package com.example.domain.engine

import com.example.domain.model.AcPhaseConfiguration
import com.example.domain.model.CalculationStepTrace
import com.example.domain.model.CheckStatus
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.DataSourceType
import com.example.domain.model.ElectricalSystemInput
import com.example.domain.model.LoadInputMode
import com.example.domain.model.VerificationStatus
import java.util.Locale
import kotlin.math.sqrt

data class DesignCurrentOutput(
    val ibAmperes: Double?,
    val trace: CalculationStepTrace?,
    val validationErrors: List<String>
)

object DesignCurrentCalculator {

    fun calculate(input: ElectricalSystemInput, stepNumber: Int = 1): DesignCurrentOutput {
        val errors = mutableListOf<String>()

        if (input.nominalVoltageV <= 0.0) {
            errors += "Nominal voltage must be greater than 0 V."
        }
        if (input.continuousOperatingFactor <= 0.0) {
            errors += "Operating factor must be greater than 0."
        }

        val eta = (input.efficiencyPct / 100.0)
        if (input.loadInputMode == LoadInputMode.ACTIVE_POWER_KW && (eta <= 0.0 || input.efficiencyPct > 100.0)) {
            errors += "Efficiency must be between 1% and 100%."
        }
        if (input.systemType == CurrentSystemType.AC &&
            input.loadInputMode == LoadInputMode.ACTIVE_POWER_KW &&
            (input.powerFactor <= 0.0 || input.powerFactor > 1.0)
        ) {
            errors += "AC power factor must be between 0.01 and 1.00."
        }

        if (errors.isNotEmpty()) {
            return DesignCurrentOutput(null, null, errors)
        }

        val scale = input.continuousOperatingFactor
        val ib: Double
        val formula: String
        val vars: List<String>
        val inputsStr: String
        val substitution: String
        val intermediate: String
        val ref: String

        when (input.loadInputMode) {
            LoadInputMode.DIRECT_CURRENT_IB -> {
                if (input.directDesignCurrentA <= 0.0) {
                    return DesignCurrentOutput(null, null, listOf("Design current Ib must be greater than 0 A."))
                }
                val rawIb = input.directDesignCurrentA
                ib = rawIb * scale
                formula = if (scale == 1.0) "Ib = Ib,input" else "Ib = Ib,input × k_op"
                vars = listOf(
                    "Ib,input = Directly entered load current (A)",
                    "k_op = Continuous operating / diversity factor"
                )
                inputsStr = "Ib,input = ${fmt(rawIb)} A, k_op = ${fmt(scale)}"
                substitution = if (scale == 1.0) "${fmt(rawIb)}" else "${fmt(rawIb)} × ${fmt(scale)}"
                intermediate = "${fmt(ib)} A"
                ref = "BS 7671 Part 2 / Reg 311.1 (Design Current)"
            }

            LoadInputMode.ACTIVE_POWER_KW -> {
                if (input.activePowerKw <= 0.0) {
                    return DesignCurrentOutput(null, null, listOf("Active power (kW) must be greater than 0."))
                }
                val pWatts = input.activePowerKw * 1000.0
                if (input.systemType == CurrentSystemType.DC) {
                    val rawIb = pWatts / (input.nominalVoltageV * eta)
                    ib = rawIb * scale
                    formula = "Ib = (P × 1000) / (V_dc × η) × k_op"
                    vars = listOf(
                        "P = Active DC power (kW)",
                        "V_dc = Nominal DC system voltage (V)",
                        "η = Efficiency (decimal)",
                        "k_op = Operating factor"
                    )
                    inputsStr = "P = ${fmt(input.activePowerKw)} kW, V_dc = ${fmt(input.nominalVoltageV)} V, η = ${fmt(eta)}, k_op = ${fmt(scale)}"
                    substitution = "(${fmt(input.activePowerKw)} × 1000) / (${fmt(input.nominalVoltageV)} × ${fmt(eta)}) × ${fmt(scale)}"
                    intermediate = "Raw Ib = ${fmt(rawIb)} A"
                    ref = "Electrical Engineering DC Power Law / BS 7671 Reg 311.1"
                } else if (input.acPhaseConfig == AcPhaseConfiguration.SINGLE_PHASE_2W) {
                    val rawIb = pWatts / (input.nominalVoltageV * input.powerFactor * eta)
                    ib = rawIb * scale
                    formula = "Ib = (P × 1000) / (V_n × PF × η) × k_op"
                    vars = listOf(
                        "P = Active power (kW)",
                        "V_n = Nominal single-phase L-N voltage (V)",
                        "PF = Power factor (cos φ)",
                        "η = Efficiency (decimal)",
                        "k_op = Operating factor"
                    )
                    inputsStr = "P = ${fmt(input.activePowerKw)} kW, V_n = ${fmt(input.nominalVoltageV)} V, PF = ${fmt(input.powerFactor)}, η = ${fmt(eta)}, k_op = ${fmt(scale)}"
                    substitution = "(${fmt(input.activePowerKw)} × 1000) / (${fmt(input.nominalVoltageV)} × ${fmt(input.powerFactor)} × ${fmt(eta)}) × ${fmt(scale)}"
                    intermediate = "Raw Ib = ${fmt(rawIb)} A"
                    ref = "Single-Phase AC Load Equation / BS 7671 Reg 311.1"
                } else {
                    val sqrt3 = sqrt(3.0)
                    val rawIb = pWatts / (sqrt3 * input.nominalVoltageV * input.powerFactor * eta)
                    ib = rawIb * scale
                    formula = "Ib = (P × 1000) / (√3 × V_L × PF × η) × k_op"
                    vars = listOf(
                        "P = Active 3-phase power (kW)",
                        "V_L = Nominal line-to-line voltage (V)",
                        "PF = Power factor (cos φ)",
                        "η = Efficiency (decimal)",
                        "k_op = Operating factor"
                    )
                    inputsStr = "P = ${fmt(input.activePowerKw)} kW, V_L = ${fmt(input.nominalVoltageV)} V, PF = ${fmt(input.powerFactor)}, η = ${fmt(eta)}, k_op = ${fmt(scale)}"
                    substitution = "(${fmt(input.activePowerKw)} × 1000) / (1.7321 × ${fmt(input.nominalVoltageV)} × ${fmt(input.powerFactor)} × ${fmt(eta)}) × ${fmt(scale)}"
                    intermediate = "Raw Ib = ${fmt(rawIb)} A"
                    ref = "Three-Phase AC Load Equation / BS 7671 Reg 311.1"
                }
            }

            LoadInputMode.APPARENT_POWER_KVA -> {
                if (input.apparentPowerKva <= 0.0) {
                    return DesignCurrentOutput(null, null, listOf("Apparent power (kVA) must be greater than 0."))
                }
                if (input.systemType == CurrentSystemType.DC) {
                    return DesignCurrentOutput(null, null, listOf("Apparent power (kVA) is not applicable to DC systems. Use kW or direct Ib."))
                }
                val sVa = input.apparentPowerKva * 1000.0
                if (input.acPhaseConfig == AcPhaseConfiguration.SINGLE_PHASE_2W) {
                    val rawIb = sVa / input.nominalVoltageV
                    ib = rawIb * scale
                    formula = "Ib = (S × 1000) / V_n × k_op"
                    vars = listOf(
                        "S = Apparent power (kVA)",
                        "V_n = Nominal single-phase voltage (V)",
                        "k_op = Operating factor"
                    )
                    inputsStr = "S = ${fmt(input.apparentPowerKva)} kVA, V_n = ${fmt(input.nominalVoltageV)} V, k_op = ${fmt(scale)}"
                    substitution = "(${fmt(input.apparentPowerKva)} × 1000) / ${fmt(input.nominalVoltageV)} × ${fmt(scale)}"
                    intermediate = "Raw Ib = ${fmt(rawIb)} A"
                    ref = "Single-Phase Apparent Power Equation / BS 7671 Reg 311.1"
                } else {
                    val sqrt3 = sqrt(3.0)
                    val rawIb = sVa / (sqrt3 * input.nominalVoltageV)
                    ib = rawIb * scale
                    formula = "Ib = (S × 1000) / (√3 × V_L) × k_op"
                    vars = listOf(
                        "S = Apparent 3-phase power (kVA)",
                        "V_L = Nominal line-to-line voltage (V)",
                        "k_op = Operating factor"
                    )
                    inputsStr = "S = ${fmt(input.apparentPowerKva)} kVA, V_L = ${fmt(input.nominalVoltageV)} V, k_op = ${fmt(scale)}"
                    substitution = "(${fmt(input.apparentPowerKva)} × 1000) / (1.7321 × ${fmt(input.nominalVoltageV)}) × ${fmt(scale)}"
                    intermediate = "Raw Ib = ${fmt(rawIb)} A"
                    ref = "Three-Phase Apparent Power Equation / BS 7671 Reg 311.1"
                }
            }
        }

        val trace = CalculationStepTrace(
            stepNumber = stepNumber,
            calculationName = "Design Current (Ib)",
            formula = formula,
            variableDefinitions = vars,
            inputValues = inputsStr,
            substitution = substitution,
            intermediateResult = intermediate,
            finalResult = fmt(ib),
            unit = "A",
            standardReference = ref,
            dataSource = DataSourceType.USER_INPUT,
            verificationStatus = VerificationStatus.VERIFIED,
            outcome = CheckStatus.PASS
        )

        return DesignCurrentOutput(ib, trace, emptyList())
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
}
