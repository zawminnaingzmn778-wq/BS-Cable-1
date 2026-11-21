package com.example.domain.standards

import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CableConstruction
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.DataSourceType
import com.example.domain.model.InsulationType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.VerificationStatus
import kotlin.math.pow

/**
 * Provides a Generic Manufacturer-Independent Engineering Baseline dataset.
 *
 * IMPORTANT STANDARDS NOTICE:
 * Official BS 7671 Appendix 4 ampacity tables, correction factor tables, and k-factor tables
 * are copyrighted IET/BSI publications and are NOT reproduced verbatim here.
 *
 * Electrical resistance values at 20°C are computed from fundamental IEC 60228 conductor
 * cross-sections and standard annealed copper / aluminium resistivity physics, while
 * baseline current-carrying capacity slots are explicitly tagged as
 * [DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT] and [VerificationStatus.NOT_VERIFIED]
 * so the calculation engine can run end-to-end demonstrations and preliminary sizing
 * while prompting the engineer to import or verify authorized BS 7671 / manufacturer data.
 */
object GenericStandardsDataset {

    val standardCsaSizesMm2: List<Double> = listOf(
        1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0,
        50.0, 70.0, 95.0, 120.0, 150.0, 185.0, 240.0, 300.0
    )

    fun createGenericBaselineCatalog(
        edition: String = "BS 7671:2018+A2:2022"
    ): List<CableCatalogEntry> {
        val entries = mutableListOf<CableCatalogEntry>()

        // 1. Multicore Armoured SWA — Copper XLPE 90°C (3/4-core & 2-core generic baseline)
        for (size in standardCsaSizesMm2) {
            entries += buildGenericEntry(
                material = ConductorMaterial.COPPER,
                insulation = InsulationType.XLPE_90C,
                construction = CableConstruction.MULTICORE_ARMOURED_SWA,
                coreCount = 4,
                sizeMm2 = size,
                edition = edition
            )
            entries += buildGenericEntry(
                material = ConductorMaterial.COPPER,
                insulation = InsulationType.PVC_70C,
                construction = CableConstruction.MULTICORE_ARMOURED_SWA,
                coreCount = 4,
                sizeMm2 = size,
                edition = edition
            )
            entries += buildGenericEntry(
                material = ConductorMaterial.COPPER,
                insulation = InsulationType.PVC_70C,
                construction = CableConstruction.MULTICORE_NON_ARMOURED,
                coreCount = 3,
                sizeMm2 = size,
                edition = edition
            )
            entries += buildGenericEntry(
                material = ConductorMaterial.COPPER,
                insulation = InsulationType.XLPE_90C,
                construction = CableConstruction.SINGLE_CORE_NON_ARMOURED,
                coreCount = 1,
                sizeMm2 = size,
                edition = edition
            )
            if (size in listOf(2.5, 4.0, 6.0, 10.0, 16.0)) {
                entries += buildGenericEntry(
                    material = ConductorMaterial.COPPER,
                    insulation = InsulationType.XLPE_90C,
                    construction = CableConstruction.SOLAR_DC_DOUBLE_INSULATED,
                    coreCount = 1,
                    sizeMm2 = size,
                    edition = edition
                )
            }
            if (size >= 16.0) {
                entries += buildGenericEntry(
                    material = ConductorMaterial.ALUMINIUM,
                    insulation = InsulationType.XLPE_90C,
                    construction = CableConstruction.MULTICORE_ARMOURED_SWA,
                    coreCount = 4,
                    sizeMm2 = size,
                    edition = edition
                )
                entries += buildGenericEntry(
                    material = ConductorMaterial.ALUMINIUM,
                    insulation = InsulationType.XLPE_90C,
                    construction = CableConstruction.SINGLE_CORE_ARMOURED_AWA,
                    coreCount = 1,
                    sizeMm2 = size,
                    edition = edition
                )
            }
        }
        return entries
    }

    private fun buildGenericEntry(
        material: ConductorMaterial,
        insulation: InsulationType,
        construction: CableConstruction,
        coreCount: Int,
        sizeMm2: Double,
        edition: String
    ): CableCatalogEntry {
        // Fundamental physics: IEC 60228 nominal resistivity at 20°C
        // Cu: 0.017241 ohm*mm2/m (17.241 ohm*mm2/km) -> ~18.1 / S ohm/km practical stranded
        // Al: 0.028264 ohm*mm2/m (28.264 ohm*mm2/km) -> ~29.5 / S ohm/km practical stranded
        val rho20OhmKmPerMm2 = when (material) {
            ConductorMaterial.COPPER -> 18.1
            ConductorMaterial.ALUMINIUM -> 29.5
        }
        val r20OhmPerKm = rho20OhmKmPerMm2 / sizeMm2
        val maxTemp = insulation.defaultMaxTempC
        val alpha20 = if (material == ConductorMaterial.COPPER) 0.00393 else 0.00403
        val rOpOhmPerKm = r20OhmPerKm * (1.0 + alpha20 * (maxTemp - 20.0))

        // Generic 50 Hz reactance approximation based on typical LV cable geometry
        val reactance50Hz = when {
            sizeMm2 <= 10.0 -> 0.115
            sizeMm2 <= 35.0 -> 0.095
            sizeMm2 <= 95.0 -> 0.082
            else -> 0.075
        }

        // Resistive mV/A/m at operating temperature (single-phase 2-wire equivalent = 2 * R_op)
        val mvResistive = 2.0 * rOpOhmPerKm
        val mvReactive = 2.0 * reactance50Hz
        val mvCombined = kotlin.math.hypot(mvResistive, mvReactive)

        // Generic thermal-equilibrium empirical ampacity estimate (explicitly marked NOT_VERIFIED;
        // engineer must import/verify official BS 7671 Appendix 4 values for final sign-off).
        val baseScale = when (material) {
            ConductorMaterial.COPPER -> 1.0
            ConductorMaterial.ALUMINIUM -> 0.78
        }
        val tempScale = when {
            maxTemp >= 90.0 -> 1.22
            else -> 1.0
        }
        // Empirical power-law envelope for preliminary sizing only: I_base(S)
        val refC = 14.5 * sizeMm2.pow(0.625) * baseScale * tempScale

        val applicableMethods = ReferenceMethodCode.entries.filter {
            ReferenceMethodCatalog.isCompatible(it, construction)
        }
        val methodMap = applicableMethods.associateWith { method ->
            val factor = when (method) {
                ReferenceMethodCode.A1 -> 0.74
                ReferenceMethodCode.A2 -> 0.72
                ReferenceMethodCode.B1 -> 0.86
                ReferenceMethodCode.B2 -> 0.83
                ReferenceMethodCode.C -> 1.00
                ReferenceMethodCode.D1 -> 0.82
                ReferenceMethodCode.D2 -> 0.94
                ReferenceMethodCode.E -> 1.06
                ReferenceMethodCode.F -> 1.12
                ReferenceMethodCode.G -> 1.24
            }
            kotlin.math.round(refC * factor)
        }

        // Standard adiabatic k-factor reference slots (clearly marked unverified until standard pack imported)
        val genericK = when {
            material == ConductorMaterial.COPPER && maxTemp >= 90.0 -> 143.0
            material == ConductorMaterial.COPPER && maxTemp < 90.0 -> 115.0
            material == ConductorMaterial.ALUMINIUM && maxTemp >= 90.0 -> 94.0
            else -> 76.0
        }

        val id = "${material.symbol}_${insulation.name}_${construction.shortLabel.replace(" ", "")}_${coreCount}C_${sizeMm2}"
        return CableCatalogEntry(
            cableId = id,
            material = material,
            insulation = insulation,
            construction = construction,
            coreCount = coreCount,
            sizeMm2 = sizeMm2,
            maxOperatingTempC = maxTemp,
            ampacityByMethod = methodMap,
            voltageDropMvPerAPerM = mvCombined,
            voltageDropResistiveMvPerAPerM = mvResistive,
            voltageDropReactiveMvPerAPerM = mvReactive,
            resistanceOhmPerKm20C = r20OhmPerKm,
            reactanceOhmPerKm50Hz = reactance50Hz,
            kFactorAdiabatic = genericK,
            dataSource = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
            referenceEdition = edition,
            referenceIdentifier = "Generic Baseline / Authorized BS 7671 Appendix 4 Slot",
            verificationStatus = VerificationStatus.NOT_VERIFIED,
            notes = "Preliminary generic engineering estimate. Import or verify authorized BS 7671 / manufacturer data for sign-off."
        )
    }
}
