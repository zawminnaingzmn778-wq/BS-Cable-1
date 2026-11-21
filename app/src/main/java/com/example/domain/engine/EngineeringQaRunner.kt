package com.example.domain.engine

import com.example.domain.model.AcPhaseConfiguration
import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CableConstruction
import com.example.domain.model.CableSpecInput
import com.example.domain.model.CheckStatus
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.DcConfiguration
import com.example.domain.model.ElectricalSystemInput
import com.example.domain.model.InsulationType
import com.example.domain.model.LoadInputMode
import com.example.domain.model.ProjectMetadata
import com.example.domain.model.ProtectionAndLimitsInput
import com.example.domain.model.ProtectiveDeviceType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.RouteSegment
import com.example.domain.model.SegmentConditions
import com.example.domain.model.SizingMode
import java.util.Locale
import kotlin.math.abs

data class QaTestCaseResult(
    val testId: String,
    val title: String,
    val inputSummary: String,
    val expectedResult: String,
    val actualResult: String,
    val difference: String,
    val passed: Boolean,
    val testSource: String
)

object EngineeringQaRunner {

    fun runAll(catalog: List<CableCatalogEntry>): List<QaTestCaseResult> {
        return listOf(
            testThreePhaseDesignCurrent(),
            testSinglePhaseDesignCurrent(),
            testDcDesignCurrent(),
            testMultiSegmentGoverningSelection(catalog),
            testManualUndersizedRejection(catalog),
            testAdiabaticFormulaBoundary(catalog)
        )
    }

    private fun testThreePhaseDesignCurrent(): QaTestCaseResult {
        // P = 30 kW, V = 400 V 3-phase, PF = 0.85, eta = 95%
        // Hand calc: 30000 / (1.7320508 * 400 * 0.85 * 0.95) = 53.6317 A
        val input = ElectricalSystemInput(
            systemType = CurrentSystemType.AC,
            acPhaseConfig = AcPhaseConfiguration.THREE_PHASE_3W_OR_4W,
            nominalVoltageV = 400.0,
            loadInputMode = LoadInputMode.ACTIVE_POWER_KW,
            activePowerKw = 30.0,
            powerFactor = 0.85,
            efficiencyPct = 95.0
        )
        val out = DesignCurrentCalculator.calculate(input)
        val expected = 53.63
        val actual = out.ibAmperes ?: 0.0
        val diff = abs(actual - expected)
        return QaTestCaseResult(
            testId = "QA-01",
            title = "3-Phase AC Design Current (kW + PF + η)",
            inputSummary = "P=30 kW, V=400 V 3-Ph, PF=0.85, η=95%",
            expectedResult = "${fmt(expected)} A",
            actualResult = "${fmt(actual)} A",
            difference = "${fmt(diff)} A",
            passed = diff < 0.02,
            testSource = "Independent hand calculation: P / (√3 × V × PF × η)"
        )
    }

    private fun testSinglePhaseDesignCurrent(): QaTestCaseResult {
        // P = 7.36 kW, V = 230 V 1-phase, PF = 1.0, eta = 100% -> 32.00 A
        val input = ElectricalSystemInput(
            systemType = CurrentSystemType.AC,
            acPhaseConfig = AcPhaseConfiguration.SINGLE_PHASE_2W,
            nominalVoltageV = 230.0,
            loadInputMode = LoadInputMode.ACTIVE_POWER_KW,
            activePowerKw = 7.36,
            powerFactor = 1.0,
            efficiencyPct = 100.0
        )
        val out = DesignCurrentCalculator.calculate(input)
        val expected = 32.00
        val actual = out.ibAmperes ?: 0.0
        val diff = abs(actual - expected)
        return QaTestCaseResult(
            testId = "QA-02",
            title = "1-Phase AC Design Current",
            inputSummary = "P=7.36 kW, V=230 V 1-Ph, PF=1.0, η=100%",
            expectedResult = "${fmt(expected)} A",
            actualResult = "${fmt(actual)} A",
            difference = "${fmt(diff)} A",
            passed = diff < 0.01,
            testSource = "Independent hand calculation: P / (V × PF × η)"
        )
    }

    private fun testDcDesignCurrent(): QaTestCaseResult {
        // Solar PV DC: P = 12 kW, Vdc = 600 V, eta = 100% -> 20.00 A
        val input = ElectricalSystemInput(
            systemType = CurrentSystemType.DC,
            dcConfig = DcConfiguration.TWO_WIRE_UNGROUNDED_OR_PV,
            nominalVoltageV = 600.0,
            loadInputMode = LoadInputMode.ACTIVE_POWER_KW,
            activePowerKw = 12.0,
            efficiencyPct = 100.0
        )
        val out = DesignCurrentCalculator.calculate(input)
        val expected = 20.00
        val actual = out.ibAmperes ?: 0.0
        val diff = abs(actual - expected)
        return QaTestCaseResult(
            testId = "QA-03",
            title = "DC Solar PV Design Current",
            inputSummary = "P=12 kW, Vdc=600 V, η=100%",
            expectedResult = "${fmt(expected)} A",
            actualResult = "${fmt(actual)} A",
            difference = "${fmt(diff)} A",
            passed = diff < 0.01,
            testSource = "Independent hand calculation: P / Vdc"
        )
    }

    private fun testMultiSegmentGoverningSelection(catalog: List<CableCatalogEntry>): QaTestCaseResult {
        val project = CircuitDesignProject(
            metadata = ProjectMetadata(id = "qa-04", projectName = "QA Multi-Segment"),
            system = ElectricalSystemInput(
                systemType = CurrentSystemType.AC,
                acPhaseConfig = AcPhaseConfiguration.THREE_PHASE_3W_OR_4W,
                nominalVoltageV = 400.0,
                loadInputMode = LoadInputMode.DIRECT_CURRENT_IB,
                directDesignCurrentA = 42.0,
                powerFactor = 0.85
            ),
            cable = CableSpecInput(
                sizingMode = SizingMode.AUTOMATIC,
                conductorMaterial = ConductorMaterial.COPPER,
                insulationType = InsulationType.XLPE_90C,
                cableConstruction = CableConstruction.MULTICORE_ARMOURED_SWA,
                coreCount = 4
            ),
            segments = listOf(
                RouteSegment("s1", 0, "Tray E", 40.0, ReferenceMethodCode.E, SegmentConditions()),
                RouteSegment("s2", 1, "Conduit B2", 20.0, ReferenceMethodCode.B2, SegmentConditions()),
                RouteSegment("s3", 2, "Duct D1", 15.0, ReferenceMethodCode.D1, SegmentConditions())
            ),
            protection = ProtectionAndLimitsInput(
                deviceType = ProtectiveDeviceType.MCCB_THERMAL_MAGNETIC,
                ratedCurrentInA = 50.0,
                breakingCapacityKa = 15.0,
                prospectiveFaultCurrentIkKa = 6.0,
                voltageDropLimitPct = 5.0
            )
        )
        val res = CableSizingEngine.evaluate(project, catalog)
        val govCode = res.governingSegment?.methodCode?.code ?: "NONE"
        val recSize = res.recommendedCandidate?.sizeMm2
        val passed = govCode == "D1" && recSize != null && res.candidateComparisons.isNotEmpty()
        return QaTestCaseResult(
            testId = "QA-04",
            title = "Multi-Segment Governing Segment & Auto Selection",
            inputSummary = "42 A 3-Ph, Segments: 40m E + 20m B2 + 15m D1",
            expectedResult = "Governing = D1 (most restrictive), Valid candidate selected",
            actualResult = "Governing = $govCode, Selected = ${recSize ?: "None"} mm²",
            difference = if (passed) "0" else "Mismatch",
            passed = passed,
            testSource = "Route governing rule: min(Iz,seg) governs, sum(ΔV,seg) checked"
        )
    }

    private fun testManualUndersizedRejection(catalog: List<CableCatalogEntry>): QaTestCaseResult {
        val project = CircuitDesignProject(
            metadata = ProjectMetadata(id = "qa-05", projectName = "QA Manual Failure"),
            system = ElectricalSystemInput(
                systemType = CurrentSystemType.AC,
                acPhaseConfig = AcPhaseConfiguration.THREE_PHASE_3W_OR_4W,
                nominalVoltageV = 400.0,
                loadInputMode = LoadInputMode.DIRECT_CURRENT_IB,
                directDesignCurrentA = 80.0
            ),
            cable = CableSpecInput(
                sizingMode = SizingMode.MANUAL,
                conductorMaterial = ConductorMaterial.COPPER,
                insulationType = InsulationType.XLPE_90C,
                cableConstruction = CableConstruction.MULTICORE_ARMOURED_SWA,
                manualCableSizeMm2 = 2.5
            ),
            segments = listOf(RouteSegment("s1", 0, "Clipped C", 25.0, ReferenceMethodCode.C)),
            protection = ProtectionAndLimitsInput(
                deviceType = ProtectiveDeviceType.MCCB_THERMAL_MAGNETIC,
                ratedCurrentInA = 100.0
            )
        )
        val res = CableSizingEngine.evaluate(project, catalog)
        val passed = res.overallStatus == CheckStatus.FAIL &&
            res.evaluatedManualCandidate?.ampacityCheck == CheckStatus.FAIL
        return QaTestCaseResult(
            testId = "QA-05",
            title = "Manual Verification Fail Detection (2.5 mm² @ 80 A)",
            inputSummary = "Ib=80 A, Manual=2.5 mm² Cu XLPE, In=100 A",
            expectedResult = "FAIL (Insufficient Ampacity)",
            actualResult = "${res.overallStatus.label} (Ampacity=${res.evaluatedManualCandidate?.ampacityCheck?.label})",
            difference = if (passed) "0" else "Mismatch",
            passed = passed,
            testSource = "BS 7671 Reg 433.1 boundary check"
        )
    }

    private fun testAdiabaticFormulaBoundary(catalog: List<CableCatalogEntry>): QaTestCaseResult {
        // Ik = 10 kA = 10000 A, t = 0.1 s, k = 143 -> S_min = sqrt(10000^2 * 0.1) / 143 = 3162.277 / 143 = 22.11 mm²
        val project = CircuitDesignProject(
            metadata = ProjectMetadata(id = "qa-06", projectName = "QA Adiabatic"),
            system = ElectricalSystemInput(loadInputMode = LoadInputMode.DIRECT_CURRENT_IB, directDesignCurrentA = 40.0),
            cable = CableSpecInput(
                sizingMode = SizingMode.MANUAL,
                conductorMaterial = ConductorMaterial.COPPER,
                insulationType = InsulationType.XLPE_90C,
                cableConstruction = CableConstruction.MULTICORE_ARMOURED_SWA,
                manualCableSizeMm2 = 16.0
            ),
            segments = listOf(RouteSegment("s1", 0, "Method E", 10.0, ReferenceMethodCode.E)),
            protection = ProtectionAndLimitsInput(
                ratedCurrentInA = 50.0,
                prospectiveFaultCurrentIkKa = 10.0,
                faultDurationS = 0.1,
                breakingCapacityKa = 15.0,
                kValueOverride = 143.0
            )
        )
        val res = CableSizingEngine.evaluate(project, catalog)
        val minCsa = res.evaluatedManualCandidate?.adiabaticMinCsaMm2 ?: 0.0
        val expected = 22.11
        val diff = abs(minCsa - expected)
        val scFail = res.evaluatedManualCandidate?.shortCircuitCheck == CheckStatus.FAIL
        val passed = diff < 0.05 && scFail
        return QaTestCaseResult(
            testId = "QA-06",
            title = "Adiabatic Short-Circuit Boundary (16 mm² < 22.11 mm²)",
            inputSummary = "Ik=10 kA, t=0.1 s, k=143, Manual=16 mm²",
            expectedResult = "S_min=${fmt(expected)} mm², Status=FAIL",
            actualResult = "S_min=${fmt(minCsa)} mm², Status=${res.evaluatedManualCandidate?.shortCircuitCheck?.label}",
            difference = "${fmt(diff)} mm²",
            passed = passed,
            testSource = "Independent hand calculation: √(I²t)/k"
        )
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
}
