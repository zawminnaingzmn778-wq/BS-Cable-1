package com.example.domain.model

data class CableCatalogEntry(
    val cableId: String,
    val material: ConductorMaterial,
    val insulation: InsulationType,
    val construction: CableConstruction,
    val coreCount: Int,
    val sizeMm2: Double,
    val maxOperatingTempC: Double,
    val ampacityByMethod: Map<ReferenceMethodCode, Double>,
    val voltageDropMvPerAPerM: Double? = null,
    val voltageDropResistiveMvPerAPerM: Double? = null,
    val voltageDropReactiveMvPerAPerM: Double? = null,
    val resistanceOhmPerKm20C: Double? = null,
    val reactanceOhmPerKm50Hz: Double? = null,
    val kFactorAdiabatic: Double? = null,
    val dataSource: DataSourceType,
    val referenceEdition: String,
    val referenceIdentifier: String,
    val verificationStatus: VerificationStatus,
    val notes: String = ""
)

data class StandardsPackMetadata(
    val packId: String,
    val standardEdition: String,
    val title: String,
    val sourceType: DataSourceType,
    val verificationStatus: VerificationStatus,
    val importedAtEpochMs: Long,
    val notes: String
)

data class ReferenceMethodSpec(
    val code: ReferenceMethodCode,
    val title: String,
    val shortSummary: String,
    val detailedDescription: String,
    val applicableConstructions: List<CableConstruction>,
    val conductorArrangements: List<String>,
    val limitations: List<String>,
    val standardReference: String
)
