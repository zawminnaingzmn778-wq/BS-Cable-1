package com.example.domain.engine

import com.example.domain.model.AcPhaseConfiguration
import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CableSpecInput
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.ElectricalSystemInput
import kotlin.math.sqrt

data class SegmentVoltageDropOutput(
    val voltageDropV: Double,
    val voltageDropPct: Double,
    val effectiveMvPerAPerM: Double,
    val formulaNote: String
)

object VoltageDropCalculator {

    fun calculateSegmentDrop(
        system: ElectricalSystemInput,
        cableSpec: CableSpecInput,
        entry: CableCatalogEntry,
        designCurrentIbA: Double,
        lengthMeters: Double,
        parallelRuns: Int
    ): SegmentVoltageDropOutput? {
        if (system.nominalVoltageV <= 0.0 || lengthMeters <= 0.0 || parallelRuns <= 0) return null

        val currentPerRun = designCurrentIbA / parallelRuns.toDouble()
        val maxTemp = cableSpec.maxOperatingTempC
        val alpha20 = if (entry.material.symbol == "Cu") 0.00393 else 0.00403

        // Determine R_op (ohm/km) and X (ohm/km)
        val r20 = cableSpec.customResistanceOhmPerKm.takeIf { cableSpec.useCustomImpedance }
            ?: entry.resistanceOhmPerKm20C
        val x50 = cableSpec.customReactanceOhmPerKm.takeIf { cableSpec.useCustomImpedance }
            ?: (entry.reactanceOhmPerKm50Hz ?: 0.08)

        val mvPerAPerM: Double
        val note: String

        if (system.systemType == CurrentSystemType.DC) {
            // DC circuits have zero inductive reactance voltage drop; strictly resistive 2 * I * L * R_op / 1000
            val rOp = if (r20 != null) {
                r20 * (1.0 + alpha20 * (maxTemp - 20.0))
            } else {
                (entry.voltageDropResistiveMvPerAPerM ?: entry.voltageDropMvPerAPerM ?: return null) / 2.0
            }
            mvPerAPerM = 2.0 * rOp
            note = "DC resistive drop: ΔV = 2 × (Ib/n) × L × R_op / 1000"
        } else {
            val cosPhi = system.powerFactor.coerceIn(0.01, 1.0)
            val sinPhi = sqrt(maxOf(0.0, 1.0 - cosPhi * cosPhi))
            val phaseMultiplier = when (system.acPhaseConfig) {
                AcPhaseConfiguration.SINGLE_PHASE_2W -> 2.0
                AcPhaseConfiguration.THREE_PHASE_3W_OR_4W -> sqrt(3.0)
            }

            if (r20 != null) {
                val rOp = r20 * (1.0 + alpha20 * (maxTemp - 20.0))
                val freqScale = system.designFrequencyHz / 50.0
                val xOp = x50 * freqScale
                mvPerAPerM = phaseMultiplier * (rOp * cosPhi + xOp * sinPhi)
                note = if (system.acPhaseConfig == AcPhaseConfiguration.THREE_PHASE_3W_OR_4W) {
                    "3-Ph AC drop: ΔV = √3 × (Ib/n) × L × (R_op·cosφ + X·sinφ) / 1000"
                } else {
                    "1-Ph AC drop: ΔV = 2 × (Ib/n) × L × (R_op·cosφ + X·sinφ) / 1000"
                }
            } else {
                val base2WireMv = entry.voltageDropMvPerAPerM ?: return null
                mvPerAPerM = if (system.acPhaseConfig == AcPhaseConfiguration.THREE_PHASE_3W_OR_4W) {
                    base2WireMv * (sqrt(3.0) / 2.0)
                } else {
                    base2WireMv
                }
                note = "Tabulated mV/A/m drop: ΔV = (mV/A/m) × (Ib/n) × L / 1000"
            }
        }

        val dvVolts = (mvPerAPerM * currentPerRun * lengthMeters) / 1000.0
        val dvPct = (dvVolts / system.nominalVoltageV) * 100.0

        return SegmentVoltageDropOutput(
            voltageDropV = dvVolts,
            voltageDropPct = dvPct,
            effectiveMvPerAPerM = mvPerAPerM,
            formulaNote = note
        )
    }
}
