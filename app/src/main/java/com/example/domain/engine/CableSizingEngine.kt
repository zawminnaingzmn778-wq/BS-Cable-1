package com.example.domain.engine

import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CalculationStepTrace
import com.example.domain.model.CandidateDecision
import com.example.domain.model.CandidateEvaluation
import com.example.domain.model.CheckItemResult
import com.example.domain.model.CheckStatus
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.DataSourceType
import com.example.domain.model.EngineeringCalculationResult
import com.example.domain.model.RejectionReason
import com.example.domain.model.SegmentEvaluationResult
import com.example.domain.model.SizingMode
import com.example.domain.model.VerificationStatus
import com.example.domain.standards.ReferenceMethodCatalog
import java.util.Locale
import kotlin.math.sqrt

object CableSizingEngine {

    fun evaluate(
        project: CircuitDesignProject,
        catalog: List<CableCatalogEntry>
    ): EngineeringCalculationResult {
        val validationErrors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val assumptions = mutableListOf<String>()

        if (project.segments.isEmpty()) {
            validationErrors += "At least one cable route segment is required."
        }
        project.segments.forEach { seg ->
            if (seg.lengthMeters <= 0.0) {
                validationErrors += "Segment '${seg.label}' length must be greater than 0 m."
            }
        }
        if (project.cable.parallelRuns < 1) {
            validationErrors += "Number of parallel runs must be at least 1."
        }
        if (project.protection.voltageDropLimitPct <= 0.0) {
            validationErrors += "Voltage drop design limit (%) must be greater than 0."
        }

        // Step 1: Design Current
        val designCurrentOut = DesignCurrentCalculator.calculate(project.system, stepNumber = 1)
        validationErrors += designCurrentOut.validationErrors

        val totalRouteLength = project.segments.sumOf { it.lengthMeters }
        val ib = designCurrentOut.ibAmperes

        if (ib == null || validationErrors.isNotEmpty()) {
            return EngineeringCalculationResult(
                designCurrentIbA = ib,
                requiredTabulatedItA = null,
                totalRouteLengthM = totalRouteLength,
                governingSegment = null,
                segmentResults = emptyList(),
                recommendedCandidate = null,
                evaluatedManualCandidate = null,
                candidateComparisons = emptyList(),
                individualChecks = emptyList(),
                calculationTraces = listOfNotNull(designCurrentOut.trace),
                overallStatus = CheckStatus.FAIL,
                dataVerificationLevel = VerificationStatus.NOT_VERIFIED,
                standardsVerificationNotice = "Fix input validation errors to run cable sizing calculations.",
                engineeringAssumptions = assumptions,
                warnings = warnings,
                validationErrors = validationErrors
            )
        }

        // Filter matching cables from database
        val matchingEntries = catalog
            .filter {
                it.material == project.cable.conductorMaterial &&
                    it.insulation == project.cable.insulationType &&
                    it.construction == project.cable.cableConstruction
            }
            .sortedBy { it.sizeMm2 }

        if (matchingEntries.isEmpty()) {
            validationErrors += "No cable database records found for ${project.cable.conductorMaterial.label} / ${project.cable.insulationType.label} / ${project.cable.cableConstruction.label}. Add or import matching entries in Cable Database."
            return EngineeringCalculationResult(
                designCurrentIbA = ib,
                requiredTabulatedItA = null,
                totalRouteLengthM = totalRouteLength,
                governingSegment = null,
                segmentResults = emptyList(),
                recommendedCandidate = null,
                evaluatedManualCandidate = null,
                candidateComparisons = emptyList(),
                individualChecks = emptyList(),
                calculationTraces = listOfNotNull(designCurrentOut.trace),
                overallStatus = CheckStatus.NOT_VERIFIED,
                dataVerificationLevel = VerificationStatus.UNSUPPORTED,
                standardsVerificationNotice = "Missing cable catalog entries for selected specification.",
                engineeringAssumptions = assumptions,
                warnings = warnings,
                validationErrors = validationErrors
            )
        }

        // Evaluate all candidate sizes across the entire multi-segment route
        val rawEvaluations = matchingEntries.map { entry ->
            evaluateCandidateAcrossRoute(
                project = project,
                entry = entry,
                catalog = matchingEntries,
                ib = ib
            )
        }

        // Decide SELECTED / ACCEPTED_ALTERNATIVE / REJECTED in Automatic mode
        var foundFirstPassing = false
        val finalizedComparisons = rawEvaluations.map { eval ->
            val passesAll = eval.ampacityCheck == CheckStatus.PASS &&
                eval.protectionCheck == CheckStatus.PASS &&
                eval.voltageDropCheck == CheckStatus.PASS &&
                eval.shortCircuitCheck == CheckStatus.PASS

            val decision = if (passesAll) {
                if (!foundFirstPassing) {
                    foundFirstPassing = true
                    CandidateDecision.SELECTED
                } else {
                    CandidateDecision.ACCEPTED_ALTERNATIVE
                }
            } else {
                CandidateDecision.REJECTED
            }
            eval.copy(decision = decision)
        }

        val recommended = finalizedComparisons.firstOrNull { it.decision == CandidateDecision.SELECTED }

        val targetManualEntry = matchingEntries.minByOrNull {
            kotlin.math.abs(it.sizeMm2 - project.cable.manualCableSizeMm2)
        }?.takeIf { kotlin.math.abs(it.sizeMm2 - project.cable.manualCableSizeMm2) < 0.01 }

        val activeCandidateEval = if (project.cable.sizingMode == SizingMode.AUTOMATIC) {
            recommended ?: finalizedComparisons.lastOrNull()
        } else {
            if (targetManualEntry != null) {
                finalizedComparisons.firstOrNull { it.cableEntry.cableId == targetManualEntry.cableId }
            } else {
                null
            }
        }

        // If Manual mode and size wasn't in catalog
        if (project.cable.sizingMode == SizingMode.MANUAL && activeCandidateEval == null) {
            warnings += "Selected manual cable size (${fmt(project.cable.manualCableSizeMm2)} mm²) is not present in the current cable database for this specification."
        }

        // Compute detailed segment-by-segment breakdown for the active/recommended candidate (or first entry for route inspection)
        val referenceEntry = activeCandidateEval?.cableEntry ?: matchingEntries.first()
        val segmentEvalPair = computeRouteSegments(project, referenceEntry, matchingEntries, ib)
        val segmentResults = segmentEvalPair.first
        val governingSegment = segmentResults.firstOrNull { it.isGoverningAmpacity }

        // Required tabulated current capacity: It >= max(Ib, In) / min(totalCorrectionFactor) / parallelRuns
        val governingFactor = governingSegment?.totalCorrectionFactor ?: 1.0
        val effectiveIn = project.protection.deviceSettingIrA?.takeIf { it > 0.0 } ?: project.protection.ratedCurrentInA
        val requiredCurrentBase = maxOf(ib, effectiveIn)
        val requiredItTotal = requiredCurrentBase / governingFactor.coerceAtLeast(0.01)
        val requiredItPerRun = requiredItTotal / project.cable.parallelRuns.toDouble()

        // Individual checks & full calculation traceability
        val traces = mutableListOf<CalculationStepTrace>()
        designCurrentOut.trace?.let { traces += it }

        val protRes = ShortCircuitAndProtectionCalculator.evaluate(
            systemType = project.system.systemType,
            protection = project.protection,
            designCurrentIbA = ib,
            correctedIzA = activeCandidateEval?.correctedIzA,
            cableEntry = referenceEntry,
            parallelRuns = project.cable.parallelRuns
        )

        // Check verification level across all data used
        val anyUnverifiedFactor = segmentResults.any { seg ->
            seg.appliedFactors.any { it.verificationStatus != VerificationStatus.VERIFIED }
        }
        val anyUnverifiedCable = referenceEntry.verificationStatus != VerificationStatus.VERIFIED
        val anyUnverifiedK = protRes.kValueVerification != VerificationStatus.VERIFIED

        val overallDataVerification = when {
            anyUnverifiedCable && anyUnverifiedFactor -> VerificationStatus.NOT_VERIFIED
            anyUnverifiedCable || anyUnverifiedFactor || anyUnverifiedK -> VerificationStatus.PARTIALLY_VERIFIED
            else -> VerificationStatus.VERIFIED
        }

        // Trace Step 2: Governing Correction Factors & Required It
        val factorSummary = governingSegment?.appliedFactors?.joinToString(" × ") {
            "${it.symbol}(${String.format(Locale.US, "%.3f", it.value)})"
        } ?: "1.000"
        traces += CalculationStepTrace(
            stepNumber = 2,
            calculationName = "Required Tabulated Ampacity (It) — Governing Segment",
            formula = "It,req = max(Ib, In) / (C_total × n_parallel)",
            variableDefinitions = listOf(
                "Ib = Design current (A)",
                "In = Protective device rating / setting (A)",
                "C_total = Product of applicable segment correction factors",
                "n_parallel = Number of parallel runs per phase"
            ),
            inputValues = "Ib = ${fmt(ib)} A, In = ${fmt(effectiveIn)} A, Governing Segment = ${governingSegment?.label ?: "-"} (${governingSegment?.methodCode?.code ?: "-"}), C_total = $factorSummary = ${String.format(Locale.US, "%.3f", governingFactor)}, n = ${project.cable.parallelRuns}",
            substitution = "${fmt(requiredCurrentBase)} / (${String.format(Locale.US, "%.3f", governingFactor)} × ${project.cable.parallelRuns})",
            intermediateResult = "Total required It = ${fmt(requiredItTotal)} A",
            finalResult = fmt(requiredItPerRun),
            unit = "A / run",
            standardReference = "${project.standardEdition} Appendix 4 — Correction Factors & Governing Segment",
            dataSource = if (anyUnverifiedFactor) DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT else DataSourceType.AUTHORIZED_BS7671,
            verificationStatus = if (anyUnverifiedFactor) VerificationStatus.NOT_VERIFIED else VerificationStatus.VERIFIED,
            outcome = CheckStatus.PASS
        )

        // Trace Step 3: Effective Cable Ampacity (Iz)
        val tabIt = activeCandidateEval?.tabulatedItA
        val effIz = activeCandidateEval?.correctedIzA
        val ampacityStatus = activeCandidateEval?.ampacityCheck ?: CheckStatus.NOT_VERIFIED
        traces += CalculationStepTrace(
            stepNumber = 3,
            calculationName = "Effective Current-Carrying Capacity (Iz)",
            formula = "Iz = It × C_total × n_parallel",
            variableDefinitions = listOf(
                "It = Tabulated current capacity for governing method (A)",
                "C_total = Combined correction factor on governing segment",
                "n_parallel = Parallel runs"
            ),
            inputValues = "Cable = ${referenceEntry.sizeMm2} mm² ${referenceEntry.material.symbol} ${referenceEntry.insulation.name}, Method = ${governingSegment?.methodCode?.code ?: "-"}, It = ${tabIt?.let { fmt(it) } ?: "N/A"} A",
            substitution = if (tabIt != null) "${fmt(tabIt)} × ${String.format(Locale.US, "%.3f", governingFactor)} × ${project.cable.parallelRuns}" else "Missing tabulated rating",
            intermediateResult = "Margin vs Ib: ${effIz?.let { fmt(it - ib) } ?: "N/A"} A",
            finalResult = effIz?.let { fmt(it) } ?: "N/A",
            unit = "A",
            standardReference = referenceEntry.referenceIdentifier,
            dataSource = referenceEntry.dataSource,
            verificationStatus = referenceEntry.verificationStatus,
            outcome = ampacityStatus
        )

        // Trace Step 4: Cumulative Route Voltage Drop
        val totalDvV = segmentResults.sumOf { it.segmentVoltageDropV ?: 0.0 }
        val totalDvPct = (totalDvV / project.system.nominalVoltageV) * 100.0
        val vdLimitPct = project.protection.voltageDropLimitPct
        val vdStatus = activeCandidateEval?.voltageDropCheck ?: CheckStatus.NOT_VERIFIED
        val segDvStr = segmentResults.joinToString(" + ") {
            "${it.label}: ${it.segmentVoltageDropV?.let { d -> fmt(d) } ?: "?"}V"
        }
        traces += CalculationStepTrace(
            stepNumber = 4,
            calculationName = "Total Route Voltage Drop (ΔV)",
            formula = "ΔV_total = Σ ΔV_segment,   ΔV% = (ΔV_total / V_n) × 100",
            variableDefinitions = listOf(
                "ΔV_segment = Voltage drop across each route segment (V)",
                "V_n = Nominal system voltage (V)"
            ),
            inputValues = "Route length = ${fmt(totalRouteLength)} m, V_n = ${fmt(project.system.nominalVoltageV)} V, Limit = ${fmt(vdLimitPct)}%",
            substitution = "$segDvStr = ${fmt(totalDvV)} V; (${fmt(totalDvV)} / ${fmt(project.system.nominalVoltageV)}) × 100",
            intermediateResult = "ΔV_total = ${fmt(totalDvV)} V",
            finalResult = fmt(totalDvPct),
            unit = "%",
            standardReference = "${project.standardEdition} Reg 525 / Appendix 4 Voltage Drop",
            dataSource = referenceEntry.dataSource,
            verificationStatus = referenceEntry.verificationStatus,
            outcome = vdStatus
        )

        // Trace Step 5: Protective Device Coordination
        traces += CalculationStepTrace(
            stepNumber = 5,
            calculationName = "Overload Protection Coordination (Ib ≤ In ≤ Iz & I2 ≤ 1.45×Iz)",
            formula = "Ib ≤ In ≤ Iz  and  I2 ≤ 1.45 × Iz",
            variableDefinitions = listOf(
                "Ib = Design current (A)",
                "In = Protective device nominal/setting current (A)",
                "Iz = Effective cable current-carrying capacity (A)",
                "I2 = Conventional operating current of protective device (A)"
            ),
            inputValues = "Ib = ${fmt(ib)} A, In = ${fmt(protRes.effectiveInOrIrA)} A, Iz = ${effIz?.let { fmt(it) } ?: "N/A"} A, I2 = ${fmt(protRes.i2OperatingCurrentA)} A",
            substitution = "${fmt(ib)} ≤ ${fmt(protRes.effectiveInOrIrA)} ≤ ${effIz?.let { fmt(it) } ?: "?"}; ${fmt(protRes.i2OperatingCurrentA)} ≤ ${protRes.overloadLimit145IzA?.let { fmt(it) } ?: "?"}",
            intermediateResult = "1.45 × Iz = ${protRes.overloadLimit145IzA?.let { fmt(it) } ?: "N/A"} A",
            finalResult = protRes.overallProtectionCheck.label,
            unit = "Status",
            standardReference = "${project.standardEdition} Reg 433.1",
            dataSource = DataSourceType.VERIFIED_REFERENCE,
            verificationStatus = VerificationStatus.VERIFIED,
            outcome = protRes.overallProtectionCheck
        )

        // Trace Step 6: Adiabatic Short-Circuit Withstand
        val ikAmps = project.protection.prospectiveFaultCurrentIkKa * 1000.0
        traces += CalculationStepTrace(
            stepNumber = 6,
            calculationName = "Adiabatic Short-Circuit Thermal Withstand",
            formula = "S_min = √(I² × t) / k",
            variableDefinitions = listOf(
                "I = Prospective fault current (A)",
                "t = Fault disconnection / clearing duration (s)",
                "k = Conductor adiabatic factor (A·s^0.5/mm²)"
            ),
            inputValues = "I = ${fmt(ikAmps)} A (${fmt(project.protection.prospectiveFaultCurrentIkKa)} kA), t = ${fmt(project.protection.faultDurationS)} s, k = ${protRes.effectiveKValue?.let { fmt(it) } ?: "N/A"}",
            substitution = if (protRes.effectiveKValue != null) "√(${fmt(ikAmps)}² × ${fmt(project.protection.faultDurationS)}) / ${fmt(protRes.effectiveKValue)}" else "Missing k factor",
            intermediateResult = "Selected total CSA = ${fmt(referenceEntry.sizeMm2 * project.cable.parallelRuns)} mm²",
            finalResult = protRes.adiabaticMinCsaMm2?.let { fmt(it) } ?: "N/A",
            unit = "mm² min",
            standardReference = "${project.standardEdition} Reg 434.5.2 (Verify k against Table 43.1)",
            dataSource = protRes.kValueSource,
            verificationStatus = protRes.kValueVerification,
            outcome = protRes.adiabaticPhaseCheck
        )

        // Individual Check Cards
        val individualChecks = listOf(
            CheckItemResult(
                id = "chk_ampacity",
                title = "Current-Carrying Capacity (Iz ≥ Ib)",
                standardRef = "${project.standardEdition} Reg 433.1 / Appendix 4",
                requirementText = "Iz ≥ ${fmt(ib)} A (Governing: ${governingSegment?.label ?: "-"})",
                calculatedText = effIz?.let { "${fmt(it)} A" } ?: "Not available",
                status = ampacityStatus,
                explanation = if (ampacityStatus == CheckStatus.PASS) {
                    "Effective ampacity ${fmt(effIz ?: 0.0)} A meets design current ${fmt(ib)} A."
                } else {
                    "Effective ampacity is insufficient or unsupported for governing segment ${governingSegment?.methodCode?.code ?: ""}."
                }
            ),
            CheckItemResult(
                id = "chk_protection",
                title = "Overload Coordination (Ib ≤ In ≤ Iz, I2 ≤ 1.45Iz)",
                standardRef = "${project.standardEdition} Reg 433.1",
                requirementText = "${fmt(ib)} ≤ ${fmt(protRes.effectiveInOrIrA)} ≤ Iz; I2 ≤ 1.45×Iz",
                calculatedText = "I2 = ${fmt(protRes.i2OperatingCurrentA)} A vs ${protRes.overloadLimit145IzA?.let { fmt(it) } ?: "?"} A",
                status = protRes.overallProtectionCheck,
                explanation = protRes.explanation
            ),
            CheckItemResult(
                id = "chk_vd",
                title = "Cumulative Route Voltage Drop",
                standardRef = "${project.standardEdition} Reg 525",
                requirementText = "ΔV ≤ ${fmt(vdLimitPct)}% (${fmt(project.system.nominalVoltageV * vdLimitPct / 100.0)} V)",
                calculatedText = "${fmt(totalDvV)} V (${fmt(totalDvPct)}%)",
                status = vdStatus,
                explanation = if (vdStatus == CheckStatus.PASS) {
                    "Cumulative drop across ${project.segments.size} segment(s) is within the ${fmt(vdLimitPct)}% design limit."
                } else {
                    "Cumulative voltage drop (${fmt(totalDvPct)}%) exceeds design limit (${fmt(vdLimitPct)}%)."
                }
            ),
            CheckItemResult(
                id = "chk_sc",
                title = "Short-Circuit Thermal Withstand & Breaking Capacity",
                standardRef = "${project.standardEdition} Reg 434.5.2",
                requirementText = "S ≥ ${protRes.adiabaticMinCsaMm2?.let { fmt(it) } ?: "?"} mm²; Icu ≥ ${fmt(project.protection.prospectiveFaultCurrentIkKa)} kA",
                calculatedText = "S = ${fmt(referenceEntry.sizeMm2 * project.cable.parallelRuns)} mm²; Icu = ${fmt(project.protection.breakingCapacityKa)} kA",
                status = protRes.overallShortCircuitCheck,
                explanation = protRes.explanation
            ),
            CheckItemResult(
                id = "chk_zs",
                title = "Earth Fault Loop Impedance (Zs)",
                standardRef = "${project.standardEdition} Chapter 41",
                requirementText = project.protection.maxEarthFaultLoopImpedanceZsOhm?.let { "Zs ≤ ${fmt(it)} Ω" } ?: "Max Zs not specified",
                calculatedText = project.protection.actualZsOhm?.let { "${fmt(it)} Ω" } ?: "Not entered",
                status = protRes.earthFaultLoopCheck,
                explanation = if (protRes.earthFaultLoopCheck == CheckStatus.NOT_VERIFIED) {
                    "Enter both actual Zs and maximum permitted Zs on Page 7 to verify disconnection impedance."
                } else if (protRes.earthFaultLoopCheck == CheckStatus.PASS) {
                    "Measured/calculated Zs complies with maximum disconnection impedance."
                } else {
                    "Actual Zs exceeds maximum allowed Zs for protective device disconnection time."
                }
            )
        )

        val anyCheckFail = individualChecks.any { it.status == CheckStatus.FAIL } ||
            (project.cable.sizingMode == SizingMode.AUTOMATIC && recommended == null)
        val anyCheckNotVerified = individualChecks.any { it.status == CheckStatus.NOT_VERIFIED }

        val overallStatus = when {
            anyCheckFail -> CheckStatus.FAIL
            anyCheckNotVerified || overallDataVerification == VerificationStatus.NOT_VERIFIED -> CheckStatus.NOT_VERIFIED
            else -> CheckStatus.PASS
        }

        if (overallDataVerification != VerificationStatus.VERIFIED) {
            warnings += "One or more tabulated cable ratings or correction factors originate from the Generic Baseline (NOT VERIFIED). Import or verify authorized BS 7671 / manufacturer data before final engineering sign-off."
        }
        if (project.segments.any { it.allowTransitionOverride }) {
            assumptions += "One or more route segments specify a cable size or parallel-run transition override; ensure transition junction/enclosure protection complies with BS 7671 Section 433/434."
        }
        assumptions += "Evaluation checks continuous thermal rating against the most restrictive (governing) segment and sums voltage drop across all ${project.segments.size} route segments."
        assumptions += "Active standard framework: ${project.standardEdition}."

        val banner = when (overallDataVerification) {
            VerificationStatus.VERIFIED -> "All tabulated data and correction factors used in this calculation are marked VERIFIED."
            VerificationStatus.PARTIALLY_VERIFIED -> "PARTIALLY VERIFIED: Mixed verified and generic/unverified values used. Review provenance before sign-off."
            VerificationStatus.NOT_VERIFIED -> "PRELIMINARY / NOT VERIFIED: Uses Generic Manufacturer-Independent Baseline values. Import authorized BS 7671 data for compliance sign-off."
            VerificationStatus.UNSUPPORTED -> "UNSUPPORTED: Missing required cable or method data."
        }

        return EngineeringCalculationResult(
            designCurrentIbA = ib,
            requiredTabulatedItA = requiredItPerRun,
            totalRouteLengthM = totalRouteLength,
            governingSegment = governingSegment,
            segmentResults = segmentResults,
            recommendedCandidate = recommended,
            evaluatedManualCandidate = if (project.cable.sizingMode == SizingMode.MANUAL) activeCandidateEval else null,
            candidateComparisons = finalizedComparisons,
            individualChecks = individualChecks,
            calculationTraces = traces,
            overallStatus = overallStatus,
            dataVerificationLevel = overallDataVerification,
            standardsVerificationNotice = banner,
            engineeringAssumptions = assumptions,
            warnings = warnings,
            validationErrors = emptyList()
        )
    }

    private fun evaluateCandidateAcrossRoute(
        project: CircuitDesignProject,
        entry: CableCatalogEntry,
        catalog: List<CableCatalogEntry>,
        ib: Double
    ): CandidateEvaluation {
        val (segmentResults, unsupportedMethodCount) = computeRouteSegments(project, entry, catalog, ib)
        val governingSeg = segmentResults.firstOrNull { it.isGoverningAmpacity } ?: segmentResults.firstOrNull()
        val governingIz = segmentResults.mapNotNull { it.effectiveTotalIzA }.minOrNull()
        val governingIt = governingSeg?.tabulatedItA
        val minCorrection = segmentResults.minOfOrNull { it.totalCorrectionFactor } ?: 1.0

        val totalDvV = if (segmentResults.all { it.segmentVoltageDropV != null }) {
            segmentResults.sumOf { it.segmentVoltageDropV!! }
        } else {
            null
        }
        val totalDvPct = totalDvV?.let { (it / project.system.nominalVoltageV) * 100.0 }

        val rejectionReasons = mutableListOf<RejectionReason>()

        val ampacityCheck = when {
            unsupportedMethodCount > 0 -> {
                rejectionReasons += RejectionReason.UNSUPPORTED_INSTALLATION_CONDITION
                CheckStatus.FAIL
            }
            governingIz == null -> {
                rejectionReasons += RejectionReason.MISSING_VERIFICATION_DATA
                CheckStatus.NOT_VERIFIED
            }
            governingIz + 1e-6 >= ib -> CheckStatus.PASS
            else -> {
                rejectionReasons += RejectionReason.INSUFFICIENT_AMPACITY
                CheckStatus.FAIL
            }
        }

        val vdCheck = when {
            totalDvPct == null -> {
                rejectionReasons += RejectionReason.MISSING_VERIFICATION_DATA
                CheckStatus.NOT_VERIFIED
            }
            totalDvPct <= project.protection.voltageDropLimitPct + 1e-6 -> CheckStatus.PASS
            else -> {
                rejectionReasons += RejectionReason.EXCESSIVE_VOLTAGE_DROP
                CheckStatus.FAIL
            }
        }

        val protRes = ShortCircuitAndProtectionCalculator.evaluate(
            systemType = project.system.systemType,
            protection = project.protection,
            designCurrentIbA = ib,
            correctedIzA = governingIz,
            cableEntry = entry,
            parallelRuns = project.cable.parallelRuns
        )

        if (protRes.overallProtectionCheck == CheckStatus.FAIL) {
            rejectionReasons += RejectionReason.PROTECTION_COORDINATION_FAILURE
        } else if (protRes.overallProtectionCheck == CheckStatus.NOT_VERIFIED) {
            rejectionReasons += RejectionReason.MISSING_VERIFICATION_DATA
        }

        if (protRes.overallShortCircuitCheck == CheckStatus.FAIL) {
            rejectionReasons += RejectionReason.INSUFFICIENT_SHORT_CIRCUIT_WITHSTAND
        } else if (protRes.overallShortCircuitCheck == CheckStatus.NOT_VERIFIED) {
            rejectionReasons += RejectionReason.MISSING_VERIFICATION_DATA
        }

        val explanation = if (rejectionReasons.isEmpty()) {
            "Satisfies ampacity (${fmt(governingIz ?: 0.0)} A ≥ ${fmt(ib)} A), voltage drop (${fmt(totalDvPct ?: 0.0)}% ≤ ${fmt(project.protection.voltageDropLimitPct)}%), protection coordination, and short-circuit thermal withstand."
        } else {
            rejectionReasons.distinct().joinToString("; ") { it.label } + "."
        }

        return CandidateEvaluation(
            cableEntry = entry,
            sizeMm2 = entry.sizeMm2,
            parallelRuns = project.cable.parallelRuns,
            tabulatedItA = governingIt,
            governingMethod = governingSeg?.methodCode,
            governingSegmentLabel = governingSeg?.label ?: "-",
            minTotalCorrectionFactor = minCorrection,
            correctedIzA = governingIz,
            designCurrentIbA = ib,
            deviceRatedInA = protRes.effectiveInOrIrA,
            voltageDropV = totalDvV,
            voltageDropPct = totalDvPct,
            adiabaticMinCsaMm2 = protRes.adiabaticMinCsaMm2,
            ampacityCheck = ampacityCheck,
            protectionCheck = protRes.overallProtectionCheck,
            voltageDropCheck = vdCheck,
            shortCircuitCheck = protRes.overallShortCircuitCheck,
            decision = CandidateDecision.REJECTED,
            rejectionReasons = rejectionReasons.distinct(),
            explanation = explanation
        )
    }

    private fun computeRouteSegments(
        project: CircuitDesignProject,
        baseEntry: CableCatalogEntry,
        catalog: List<CableCatalogEntry>,
        ib: Double
    ): Pair<List<SegmentEvaluationResult>, Int> {
        var unsupportedCount = 0
        val tempResults = project.segments.sortedBy { it.orderIndex }.map { seg ->
            val effectiveSize = if (seg.allowTransitionOverride && seg.overrideCableSizeMm2 != null) {
                seg.overrideCableSizeMm2
            } else {
                baseEntry.sizeMm2
            }
            val effectiveRuns = if (seg.allowTransitionOverride && seg.overrideParallelRuns != null) {
                seg.overrideParallelRuns.coerceAtLeast(1)
            } else {
                project.cable.parallelRuns.coerceAtLeast(1)
            }
            val segEntry = catalog.firstOrNull { kotlin.math.abs(it.sizeMm2 - effectiveSize) < 0.01 } ?: baseEntry

            val methodSupported = ReferenceMethodCatalog.isCompatible(seg.methodCode, segEntry.construction)
            val tabulatedIt = segEntry.ampacityByMethod[seg.methodCode]
            if (!methodSupported || tabulatedIt == null) {
                unsupportedCount++
            }

            val cfOut = CorrectionFactorCalculator.evaluateSegmentFactors(
                segment = seg,
                insulation = segEntry.insulation,
                deviceType = project.protection.deviceType
            )

            val izPerRun = tabulatedIt?.times(cfOut.totalFactor)
            val izTotal = izPerRun?.times(effectiveRuns)

            val vdOut = VoltageDropCalculator.calculateSegmentDrop(
                system = project.system,
                cableSpec = project.cable,
                entry = segEntry,
                designCurrentIbA = ib,
                lengthMeters = seg.lengthMeters,
                parallelRuns = effectiveRuns
            )

            SegmentEvaluationResult(
                segmentId = seg.id,
                label = seg.label,
                orderIndex = seg.orderIndex,
                lengthMeters = seg.lengthMeters,
                methodCode = seg.methodCode,
                effectiveCableSizeMm2 = segEntry.sizeMm2,
                effectiveParallelRuns = effectiveRuns,
                tabulatedItA = tabulatedIt,
                appliedFactors = cfOut.factors,
                totalCorrectionFactor = cfOut.totalFactor,
                effectiveIzPerRunA = izPerRun,
                effectiveTotalIzA = izTotal,
                segmentVoltageDropV = vdOut?.voltageDropV,
                segmentVoltageDropPct = vdOut?.voltageDropPct,
                isGoverningAmpacity = false,
                isSupportedMethod = methodSupported && tabulatedIt != null,
                notes = if (!methodSupported) {
                    "Method ${seg.methodCode.code} is not normally applicable to ${segEntry.construction.label}."
                } else {
                    vdOut?.formulaNote ?: ""
                }
            )
        }

        // Identify the governing (most restrictive) segment with lowest effectiveTotalIzA
        val minIz = tempResults.mapNotNull { it.effectiveTotalIzA }.minOrNull()
        var markedGoverning = false
        val finalSegments = tempResults.map { seg ->
            val isGov = !markedGoverning && minIz != null && seg.effectiveTotalIzA != null &&
                kotlin.math.abs(seg.effectiveTotalIzA - minIz) < 0.01
            if (isGov) markedGoverning = true
            seg.copy(isGoverningAmpacity = isGov)
        }

        return finalSegments to unsupportedCount
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
}
