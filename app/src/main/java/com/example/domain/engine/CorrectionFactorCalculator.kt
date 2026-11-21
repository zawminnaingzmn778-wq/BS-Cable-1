package com.example.domain.engine

import com.example.domain.model.AppliedFactorDetail
import com.example.domain.model.DataSourceType
import com.example.domain.model.InsulationType
import com.example.domain.model.ProtectiveDeviceType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.RouteSegment
import com.example.domain.model.VerificationStatus
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class CorrectionFactorOutput(
    val factors: List<AppliedFactorDetail>,
    val totalFactor: Double
)

object CorrectionFactorCalculator {

    fun evaluateSegmentFactors(
        segment: RouteSegment,
        insulation: InsulationType,
        deviceType: ProtectiveDeviceType
    ): CorrectionFactorOutput {
        val cond = segment.conditions
        val manual = cond.manualFactors
        val isUnderground = segment.methodCode == ReferenceMethodCode.D1 || segment.methodCode == ReferenceMethodCode.D2
        val maxTemp = insulation.defaultMaxTempC
        val applied = mutableListOf<AppliedFactorDetail>()

        // 1. Ambient Air or Ground Temperature Factor (Ca)
        val refTemp = if (isUnderground) 20.0 else 30.0
        val actualTemp = if (isUnderground) cond.groundTempC else cond.ambientAirTempC
        val caDetail = if (cond.useManualFactorOverrides) {
            AppliedFactorDetail(
                symbol = "Ca",
                name = if (isUnderground) "Ground Temperature Factor" else "Ambient Air Temperature Factor",
                value = manual.caValue.coerceIn(0.1, 1.5),
                tableReference = manual.caReference,
                dataSource = manual.caSource,
                verificationStatus = sourceToVerification(manual.caSource),
                calculationNote = "User-specified / imported Ca for ${fmt(actualTemp)}°C (ref ${fmt(refTemp)}°C)."
            )
        } else if (kotlin.math.abs(actualTemp - refTemp) < 0.01) {
            AppliedFactorDetail(
                symbol = "Ca",
                name = if (isUnderground) "Ground Temperature Factor" else "Ambient Air Temperature Factor",
                value = 1.0,
                tableReference = "Reference ambient ${fmt(refTemp)}°C (Unity baseline)",
                dataSource = DataSourceType.VERIFIED_REFERENCE,
                verificationStatus = VerificationStatus.VERIFIED,
                calculationNote = "Operating ambient matches reference temperature (${fmt(refTemp)}°C), so Ca = 1.00."
            )
        } else {
            val numerator = max(1.0, maxTemp - actualTemp)
            val denominator = max(1.0, maxTemp - refTemp)
            val computedCa = sqrt(numerator / denominator).coerceIn(0.25, 1.25)
            AppliedFactorDetail(
                symbol = "Ca",
                name = if (isUnderground) "Ground Temperature Factor" else "Ambient Air Temperature Factor",
                value = round3(computedCa),
                tableReference = "BS 7671 Appendix 4 Temperature Factor Slot (Generic thermal root estimate)",
                dataSource = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                verificationStatus = VerificationStatus.NOT_VERIFIED,
                calculationNote = "Physics approximation √((θmax−θa)/(θmax−θref)) for ${fmt(actualTemp)}°C; verify against authorized Appendix 4 table."
            )
        }
        applied += caDetail

        // 2. Grouping Factor (Cg)
        val groupCount = max(1, cond.circuitsGroupedCount)
        val cgDetail = if (cond.useManualFactorOverrides) {
            AppliedFactorDetail(
                symbol = "Cg",
                name = "Circuit Grouping Factor",
                value = manual.cgValue.coerceIn(0.1, 1.0),
                tableReference = manual.cgReference,
                dataSource = manual.cgSource,
                verificationStatus = sourceToVerification(manual.cgSource),
                calculationNote = "User-specified / imported Cg for $groupCount grouped circuit(s)."
            )
        } else if (groupCount == 1) {
            AppliedFactorDetail(
                symbol = "Cg",
                name = "Circuit Grouping Factor",
                value = 1.0,
                tableReference = "Single isolated circuit (Cg = 1.00)",
                dataSource = DataSourceType.VERIFIED_REFERENCE,
                verificationStatus = VerificationStatus.VERIFIED,
                calculationNote = "Single circuit in this segment; no grouping derating required."
            )
        } else {
            val approxCg = groupCount.toDouble().pow(-0.32).coerceIn(0.35, 0.95)
            AppliedFactorDetail(
                symbol = "Cg",
                name = "Circuit Grouping Factor",
                value = round3(approxCg),
                tableReference = "BS 7671 Appendix 4 Grouping Factor Slot (Verify/Import)",
                dataSource = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                verificationStatus = VerificationStatus.NOT_VERIFIED,
                calculationNote = "Generic grouping estimate for $groupCount circuits; verify against authorized Appendix 4 grouping table."
            )
        }
        applied += cgDetail

        // 3. Thermal Insulation Factor (Ci) — ONLY applied if thermalInsulationLengthMm > 0
        if (cond.thermalInsulationLengthMm > 0.0 || (cond.useManualFactorOverrides && manual.ciValue != 1.0)) {
            val ciDetail = if (cond.useManualFactorOverrides) {
                AppliedFactorDetail(
                    symbol = "Ci",
                    name = "Thermal Insulation Factor",
                    value = manual.ciValue.coerceIn(0.1, 1.0),
                    tableReference = manual.ciReference,
                    dataSource = manual.ciSource,
                    verificationStatus = sourceToVerification(manual.ciSource),
                    calculationNote = "User-specified / imported Ci for ${fmt(cond.thermalInsulationLengthMm)} mm thermal insulation."
                )
            } else {
                val len = cond.thermalInsulationLengthMm
                val approxCi = when {
                    len <= 50.0 -> 0.89
                    len <= 100.0 -> 0.81
                    len <= 200.0 -> 0.68
                    len <= 400.0 -> 0.55
                    else -> 0.50
                }
                AppliedFactorDetail(
                    symbol = "Ci",
                    name = "Thermal Insulation Factor",
                    value = approxCi,
                    tableReference = "BS 7671 Reg 523.9 / Appendix 4 Insulation Factor Slot (Verify/Import)",
                    dataSource = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                    verificationStatus = VerificationStatus.NOT_VERIFIED,
                    calculationNote = "Generic Ci estimate for ${fmt(len)} mm enclosed in thermal insulation; verify against authorized standard table."
                )
            }
            applied += ciDetail
        }

        // 4. Underground Burial Depth & Soil Thermal Resistivity Factors (Cd, Cs) — ONLY for D1 / D2
        if (isUnderground) {
            val cdDetail = if (cond.useManualFactorOverrides) {
                AppliedFactorDetail(
                    symbol = "Cd",
                    name = "Depth of Burial Factor",
                    value = manual.cdValue.coerceIn(0.5, 1.2),
                    tableReference = manual.cdReference,
                    dataSource = manual.cdSource,
                    verificationStatus = sourceToVerification(manual.cdSource),
                    calculationNote = "User-specified / imported Cd at ${fmt(cond.buriedDepthM)} m depth."
                )
            } else if (kotlin.math.abs(cond.buriedDepthM - 0.7) <= 0.1) {
                AppliedFactorDetail(
                    symbol = "Cd",
                    name = "Depth of Burial Factor",
                    value = 1.0,
                    tableReference = "Nominal reference burial depth (~0.7 m)",
                    dataSource = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                    verificationStatus = VerificationStatus.PARTIALLY_VERIFIED,
                    calculationNote = "Standard reference depth assumed (Cd = 1.00); verify exact edition reference depth."
                )
            } else {
                val approxCd = (1.0 - (cond.buriedDepthM - 0.7) * 0.04).coerceIn(0.85, 1.06)
                AppliedFactorDetail(
                    symbol = "Cd",
                    name = "Depth of Burial Factor",
                    value = round3(approxCd),
                    tableReference = "BS 7671 Appendix 4 Depth Factor Slot (Verify/Import)",
                    dataSource = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                    verificationStatus = VerificationStatus.NOT_VERIFIED,
                    calculationNote = "Generic burial depth adjustment for ${fmt(cond.buriedDepthM)} m; verify against authorized table."
                )
            }
            applied += cdDetail

            val csDetail = if (cond.useManualFactorOverrides) {
                AppliedFactorDetail(
                    symbol = "Cs",
                    name = "Soil Thermal Resistivity Factor",
                    value = manual.csValue.coerceIn(0.4, 1.3),
                    tableReference = manual.csReference,
                    dataSource = manual.csSource,
                    verificationStatus = sourceToVerification(manual.csSource),
                    calculationNote = "User-specified / imported Cs for ${fmt(cond.soilThermalResistivityKmPerW)} K·m/W."
                )
            } else if (kotlin.math.abs(cond.soilThermalResistivityKmPerW - 2.5) < 0.05) {
                AppliedFactorDetail(
                    symbol = "Cs",
                    name = "Soil Thermal Resistivity Factor",
                    value = 1.0,
                    tableReference = "Reference soil resistivity 2.5 K·m/W (Unity baseline)",
                    dataSource = DataSourceType.VERIFIED_REFERENCE,
                    verificationStatus = VerificationStatus.VERIFIED,
                    calculationNote = "Soil resistivity matches 2.5 K·m/W reference baseline."
                )
            } else {
                val approxCs = sqrt(2.5 / max(0.5, cond.soilThermalResistivityKmPerW)).coerceIn(0.60, 1.25)
                AppliedFactorDetail(
                    symbol = "Cs",
                    name = "Soil Thermal Resistivity Factor",
                    value = round3(approxCs),
                    tableReference = "BS 7671 Appendix 4 Soil Resistivity Factor Slot (Verify/Import)",
                    dataSource = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                    verificationStatus = VerificationStatus.NOT_VERIFIED,
                    calculationNote = "Generic soil factor estimate for ${fmt(cond.soilThermalResistivityKmPerW)} K·m/W; verify against authorized table."
                )
            }
            applied += csDetail
        }

        // 5. Semi-enclosed BS 3036 Fuse Factor (Cc = 0.725) — ONLY when BS 3036 fuse is selected
        if (deviceType == ProtectiveDeviceType.FUSE_BS3036) {
            val ccVal = if (cond.useManualFactorOverrides) manual.ccValue else 0.725
            applied += AppliedFactorDetail(
                symbol = "Cc",
                name = "BS 3036 Semi-Enclosed Fuse Factor",
                value = ccVal,
                tableReference = "BS 7671 Reg 433.1.202 (0.725 factor for BS 3036)",
                dataSource = if (cond.useManualFactorOverrides) manual.ccSource else DataSourceType.VERIFIED_REFERENCE,
                verificationStatus = VerificationStatus.VERIFIED,
                calculationNote = "Mandatory 0.725 derating factor applied because BS 3036 semi-enclosed fuse has fusing factor ~2.0."
            )
        }

        val total = applied.fold(1.0) { acc, item -> acc * item.value }
        return CorrectionFactorOutput(
            factors = applied,
            totalFactor = min(1.5, round3(total))
        )
    }

    private fun sourceToVerification(source: DataSourceType): VerificationStatus = when (source) {
        DataSourceType.AUTHORIZED_BS7671, DataSourceType.VERIFIED_REFERENCE -> VerificationStatus.VERIFIED
        DataSourceType.USER_INPUT -> VerificationStatus.PARTIALLY_VERIFIED
        DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT, DataSourceType.ENGINEERING_ASSUMPTION -> VerificationStatus.NOT_VERIFIED
    }

    private fun round3(v: Double): Double = kotlin.math.round(v * 1000.0) / 1000.0
    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
}
