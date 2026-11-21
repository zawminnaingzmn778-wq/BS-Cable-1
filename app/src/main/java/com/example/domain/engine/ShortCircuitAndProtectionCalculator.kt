package com.example.domain.engine

import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CheckStatus
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.DataSourceType
import com.example.domain.model.ProtectionAndLimitsInput
import com.example.domain.model.ProtectiveDeviceType
import com.example.domain.model.VerificationStatus
import java.util.Locale
import kotlin.math.sqrt

data class ProtectionAndShortCircuitResult(
    val effectiveInOrIrA: Double,
    val i2OperatingCurrentA: Double,
    val overloadLimit145IzA: Double?,
    val ibLeInCheck: CheckStatus,
    val inLeIzCheck: CheckStatus,
    val i2Le145IzCheck: CheckStatus,
    val overallProtectionCheck: CheckStatus,
    val adiabaticMinCsaMm2: Double?,
    val effectiveKValue: Double?,
    val kValueSource: DataSourceType,
    val kValueVerification: VerificationStatus,
    val adiabaticPhaseCheck: CheckStatus,
    val breakingCapacityCheck: CheckStatus,
    val earthFaultLoopCheck: CheckStatus,
    val overallShortCircuitCheck: CheckStatus,
    val explanation: String
)

object ShortCircuitAndProtectionCalculator {

    fun evaluate(
        systemType: CurrentSystemType,
        protection: ProtectionAndLimitsInput,
        designCurrentIbA: Double,
        correctedIzA: Double?,
        cableEntry: CableCatalogEntry,
        parallelRuns: Int
    ): ProtectionAndShortCircuitResult {
        val effectiveIn = protection.deviceSettingIrA?.takeIf { it > 0.0 } ?: protection.ratedCurrentInA
        val i2Ratio = protection.overloadFactorI2Ratio.coerceAtLeast(1.0)
        val i2Current = effectiveIn * i2Ratio
        val overloadLimit145 = correctedIzA?.let { 1.45 * it }

        // Check device compatibility with AC / DC
        val deviceCompatible = when (systemType) {
            CurrentSystemType.AC -> protection.deviceType.supportsAc
            CurrentSystemType.DC -> protection.deviceType.supportsDc
        }

        val ibLeIn = if (designCurrentIbA <= effectiveIn + 1e-6) CheckStatus.PASS else CheckStatus.FAIL

        val inLeIz = when {
            correctedIzA == null -> CheckStatus.NOT_VERIFIED
            effectiveIn <= correctedIzA + 1e-6 -> CheckStatus.PASS
            else -> CheckStatus.FAIL
        }

        val i2Le145Iz = when {
            overloadLimit145 == null -> CheckStatus.NOT_VERIFIED
            // For BS 3036, BS 7671 accounts for fusing factor via Cc = 0.725 on Iz (i.e. In <= 0.725*Iz_base)
            protection.deviceType == ProtectiveDeviceType.FUSE_BS3036 -> {
                if (effectiveIn <= correctedIzA + 1e-6) CheckStatus.PASS else CheckStatus.FAIL
            }
            i2Current <= overloadLimit145 + 1e-6 -> CheckStatus.PASS
            else -> CheckStatus.FAIL
        }

        val overallProt = when {
            !deviceCompatible -> CheckStatus.FAIL
            ibLeIn == CheckStatus.FAIL || inLeIz == CheckStatus.FAIL || i2Le145Iz == CheckStatus.FAIL -> CheckStatus.FAIL
            inLeIz == CheckStatus.NOT_VERIFIED || i2Le145Iz == CheckStatus.NOT_VERIFIED -> CheckStatus.NOT_VERIFIED
            else -> CheckStatus.PASS
        }

        // Adiabatic thermal withstand: S_min = sqrt(I_k^2 * t) / k
        val kVal = protection.kValueOverride ?: cableEntry.kFactorAdiabatic
        val kSrc = if (protection.kValueOverride != null) protection.kValueSource else cableEntry.dataSource
        val kVer = if (protection.kValueOverride != null) {
            if (kSrc == DataSourceType.AUTHORIZED_BS7671 || kSrc == DataSourceType.VERIFIED_REFERENCE) {
                VerificationStatus.VERIFIED
            } else {
                VerificationStatus.PARTIALLY_VERIFIED
            }
        } else {
            cableEntry.verificationStatus
        }

        val ikAmps = protection.prospectiveFaultCurrentIkKa * 1000.0
        val faultTimeS = protection.faultDurationS
        val adiabaticMinCsa = if (kVal != null && kVal > 0.0 && ikAmps > 0.0 && faultTimeS > 0.0) {
            sqrt(ikAmps * ikAmps * faultTimeS) / kVal
        } else {
            null
        }

        val totalPhaseCsa = cableEntry.sizeMm2 * parallelRuns
        val adiabaticPhaseCheck = when {
            adiabaticMinCsa == null -> CheckStatus.NOT_VERIFIED
            totalPhaseCsa >= adiabaticMinCsa - 1e-6 -> CheckStatus.PASS
            else -> CheckStatus.FAIL
        }

        val breakingCapacityCheck = when {
            protection.prospectiveFaultCurrentIkKa <= 0.0 || protection.breakingCapacityKa <= 0.0 -> CheckStatus.NOT_VERIFIED
            protection.breakingCapacityKa >= protection.prospectiveFaultCurrentIkKa - 1e-6 -> CheckStatus.PASS
            else -> CheckStatus.FAIL
        }

        val earthFaultLoopCheck = when {
            systemType == CurrentSystemType.DC -> CheckStatus.PASS // Zs AC loop check not applicable in same AC form
            protection.actualZsOhm == null || protection.maxEarthFaultLoopImpedanceZsOhm == null -> CheckStatus.NOT_VERIFIED
            protection.actualZsOhm <= protection.maxEarthFaultLoopImpedanceZsOhm + 1e-6 -> CheckStatus.PASS
            else -> CheckStatus.FAIL
        }

        val overallShortCircuit = when {
            adiabaticPhaseCheck == CheckStatus.FAIL || breakingCapacityCheck == CheckStatus.FAIL -> CheckStatus.FAIL
            adiabaticPhaseCheck == CheckStatus.NOT_VERIFIED || breakingCapacityCheck == CheckStatus.NOT_VERIFIED -> CheckStatus.NOT_VERIFIED
            else -> CheckStatus.PASS
        }

        val notes = buildList {
            if (!deviceCompatible) {
                add("${protection.deviceType.label} is not rated for ${systemType.label}.")
            }
            if (ibLeIn == CheckStatus.FAIL) {
                add("Design current Ib (${fmt(designCurrentIbA)} A) exceeds device setting In/Ir (${fmt(effectiveIn)} A).")
            }
            if (inLeIz == CheckStatus.FAIL && correctedIzA != null) {
                add("Device rating In (${fmt(effectiveIn)} A) exceeds effective cable ampacity Iz (${fmt(correctedIzA)} A).")
            }
            if (i2Le145Iz == CheckStatus.FAIL && overloadLimit145 != null) {
                add("Conventional operating current I2 (${fmt(i2Current)} A) exceeds 1.45 × Iz (${fmt(overloadLimit145)} A).")
            }
            if (adiabaticPhaseCheck == CheckStatus.FAIL && adiabaticMinCsa != null) {
                add("Total conductor CSA (${fmt(totalPhaseCsa)} mm²) is below adiabatic minimum (${fmt(adiabaticMinCsa)} mm²).")
            }
            if (breakingCapacityCheck == CheckStatus.FAIL) {
                add("Device breaking capacity (${fmt(protection.breakingCapacityKa)} kA) is below prospective fault current Ik (${fmt(protection.prospectiveFaultCurrentIkKa)} kA).")
            }
            if (earthFaultLoopCheck == CheckStatus.FAIL) {
                add("Actual Zs (${fmt(protection.actualZsOhm ?: 0.0)} Ω) exceeds maximum Zs (${fmt(protection.maxEarthFaultLoopImpedanceZsOhm ?: 0.0)} Ω).")
            }
        }

        return ProtectionAndShortCircuitResult(
            effectiveInOrIrA = effectiveIn,
            i2OperatingCurrentA = i2Current,
            overloadLimit145IzA = overloadLimit145,
            ibLeInCheck = ibLeIn,
            inLeIzCheck = inLeIz,
            i2Le145IzCheck = i2Le145Iz,
            overallProtectionCheck = overallProt,
            adiabaticMinCsaMm2 = adiabaticMinCsa,
            effectiveKValue = kVal,
            kValueSource = kSrc,
            kValueVerification = kVer,
            adiabaticPhaseCheck = adiabaticPhaseCheck,
            breakingCapacityCheck = breakingCapacityCheck,
            earthFaultLoopCheck = earthFaultLoopCheck,
            overallShortCircuitCheck = overallShortCircuit,
            explanation = if (notes.isEmpty()) "All protection coordination and short-circuit checks satisfied." else notes.joinToString(" ")
        )
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
}
