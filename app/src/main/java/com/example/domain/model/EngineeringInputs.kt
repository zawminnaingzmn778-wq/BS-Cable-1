package com.example.domain.model

data class ProjectMetadata(
    val id: String,
    val projectName: String = "",
    val projectReference: String = "",
    val client: String = "",
    val siteLocation: String = "",
    val engineerName: String = "",
    val designDate: String = "",
    val circuitId: String = "C1",
    val circuitDescription: String = "",
    val designNotes: String = "",
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class ElectricalSystemInput(
    val systemType: CurrentSystemType = CurrentSystemType.AC,
    val acPhaseConfig: AcPhaseConfiguration = AcPhaseConfiguration.THREE_PHASE_3W_OR_4W,
    val dcConfig: DcConfiguration = DcConfiguration.TWO_WIRE_UNGROUNDED_OR_PV,
    val nominalVoltageV: Double = 400.0,
    val designFrequencyHz: Double = 50.0,
    val loadInputMode: LoadInputMode = LoadInputMode.ACTIVE_POWER_KW,
    val directDesignCurrentA: Double = 45.0,
    val activePowerKw: Double = 28.0,
    val apparentPowerKva: Double = 32.0,
    val powerFactor: Double = 0.85,
    val efficiencyPct: Double = 100.0,
    val loadCharacteristic: LoadCharacteristic = LoadCharacteristic.GENERAL_RESISTIVE_MIXED,
    val continuousOperatingFactor: Double = 1.0
)

data class CableSpecInput(
    val sizingMode: SizingMode = SizingMode.AUTOMATIC,
    val conductorMaterial: ConductorMaterial = ConductorMaterial.COPPER,
    val insulationType: InsulationType = InsulationType.XLPE_90C,
    val cableConstruction: CableConstruction = CableConstruction.MULTICORE_ARMOURED_SWA,
    val coreCount: Int = 4,
    val loadedConductors: Int = 3,
    val manualCableSizeMm2: Double = 16.0,
    val parallelRuns: Int = 1,
    val arrangement: CableArrangement = CableArrangement.TOUCHING_FLAT,
    val maxOperatingTempC: Double = 90.0,
    val useCustomImpedance: Boolean = false,
    val customResistanceOhmPerKm: Double? = null,
    val customReactanceOhmPerKm: Double? = null
)

data class SegmentFactorValues(
    val caValue: Double = 1.0,
    val caSource: DataSourceType = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
    val caReference: String = "BS 7671 Appendix 4 Ambient Temp Factor (Verify/Import)",
    val cgValue: Double = 1.0,
    val cgSource: DataSourceType = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
    val cgReference: String = "BS 7671 Appendix 4 Grouping Factor (Verify/Import)",
    val ciValue: Double = 1.0,
    val ciSource: DataSourceType = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
    val ciReference: String = "BS 7671 Thermal Insulation Factor (Verify/Import)",
    val cdValue: Double = 1.0,
    val cdSource: DataSourceType = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
    val cdReference: String = "Buried Depth Factor (Verify/Import)",
    val csValue: Double = 1.0,
    val csSource: DataSourceType = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
    val csReference: String = "Soil Thermal Resistivity Factor (Verify/Import)",
    val ccValue: Double = 1.0,
    val ccSource: DataSourceType = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
    val ccReference: String = "BS 3036 Semi-Enclosed Fuse Factor (0.725 if applicable)"
)

data class SegmentConditions(
    val ambientAirTempC: Double = 30.0,
    val groundTempC: Double = 20.0,
    val circuitsGroupedCount: Int = 1,
    val thermalInsulationLengthMm: Double = 0.0,
    val buriedDepthM: Double = 0.7,
    val soilThermalResistivityKmPerW: Double = 2.5,
    val installedInDuct: Boolean = false,
    val useManualFactorOverrides: Boolean = false,
    val manualFactors: SegmentFactorValues = SegmentFactorValues()
)

data class RouteSegment(
    val id: String,
    val orderIndex: Int,
    val label: String,
    val lengthMeters: Double,
    val methodCode: ReferenceMethodCode,
    val conditions: SegmentConditions = SegmentConditions(),
    val notes: String = "",
    val allowTransitionOverride: Boolean = false,
    val overrideCableSizeMm2: Double? = null,
    val overrideParallelRuns: Int? = null
)

data class ProtectionAndLimitsInput(
    val deviceType: ProtectiveDeviceType = ProtectiveDeviceType.MCCB_THERMAL_MAGNETIC,
    val ratedCurrentInA: Double = 50.0,
    val deviceSettingIrA: Double? = null,
    val breakingCapacityKa: Double = 10.0,
    val overloadFactorI2Ratio: Double = 1.30,
    val maxEarthFaultLoopImpedanceZsOhm: Double? = 0.85,
    val actualZsOhm: Double? = 0.42,
    val prospectiveFaultCurrentIkKa: Double = 6.0,
    val disconnectionTimeS: Double = 0.4,
    val voltageDropLimitPct: Double = 5.0,
    val faultDurationS: Double = 0.1,
    val cpcSizeMm2: Double? = null,
    val cpcMaterial: ConductorMaterial = ConductorMaterial.COPPER,
    val kValueOverride: Double? = null,
    val kValueSource: DataSourceType = DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT
)

data class CircuitDesignProject(
    val metadata: ProjectMetadata,
    val system: ElectricalSystemInput = ElectricalSystemInput(),
    val cable: CableSpecInput = CableSpecInput(),
    val segments: List<RouteSegment> = defaultRouteSegments(),
    val protection: ProtectionAndLimitsInput = ProtectionAndLimitsInput(),
    val standardEdition: String = "BS 7671:2018+A2:2022"
)

fun defaultRouteSegments(): List<RouteSegment> = listOf(
    RouteSegment(
        id = "seg-1",
        orderIndex = 0,
        label = "Riser / Cable Tray",
        lengthMeters = 35.0,
        methodCode = ReferenceMethodCode.E,
        conditions = SegmentConditions(ambientAirTempC = 30.0, circuitsGroupedCount = 2)
    ),
    RouteSegment(
        id = "seg-2",
        orderIndex = 1,
        label = "Conduit Partition Run",
        lengthMeters = 18.0,
        methodCode = ReferenceMethodCode.B2,
        conditions = SegmentConditions(ambientAirTempC = 30.0, circuitsGroupedCount = 1)
    )
)
