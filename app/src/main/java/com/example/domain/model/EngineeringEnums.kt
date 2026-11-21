package com.example.domain.model

enum class SizingMode(val label: String) {
    AUTOMATIC("Automatic Sizing"),
    MANUAL("Manual Verification")
}

enum class CurrentSystemType(val label: String) {
    AC("AC System"),
    DC("DC System")
}

enum class AcPhaseConfiguration(val label: String, val shortLabel: String) {
    SINGLE_PHASE_2W("Single-Phase (2-Wire L+N)", "1-Ph"),
    THREE_PHASE_3W_OR_4W("Three-Phase (3-Wire / 4-Wire)", "3-Ph")
}

enum class DcConfiguration(val label: String, val shortLabel: String) {
    TWO_WIRE_UNGROUNDED_OR_PV("2-Wire DC (Unearthed / Solar PV)", "2W Floating"),
    TWO_WIRE_EARTHED("2-Wire DC (Earthed Pole)", "2W Earthed")
}

enum class LoadInputMode(val label: String) {
    DIRECT_CURRENT_IB("Direct Design Current (Ib)"),
    ACTIVE_POWER_KW("Active Power (kW)"),
    APPARENT_POWER_KVA("Apparent Power (kVA)")
}

enum class LoadCharacteristic(val label: String) {
    GENERAL_RESISTIVE_MIXED("General / Mixed Distribution"),
    MOTOR_INDUCTIVE("Motor / Inductive Load"),
    SOLAR_PV_DC_STRING("Solar PV DC String / Array"),
    SOLAR_PV_AC_INVERTER("Solar PV AC Inverter Output"),
    LIGHTING("Lighting Circuit"),
    EV_CHARGER("EV Charging Equipment")
}

enum class ConductorMaterial(val label: String, val symbol: String) {
    COPPER("Copper", "Cu"),
    ALUMINIUM("Aluminium", "Al")
}

enum class InsulationType(val label: String, val defaultMaxTempC: Double) {
    PVC_70C("Thermoplastic (PVC 70°C)", 70.0),
    XLPE_90C("Thermosetting (XLPE 90°C)", 90.0),
    EPR_90C("Thermosetting (EPR 90°C)", 90.0),
    LSF_70C("Low Smoke Fume (LSF 70°C)", 70.0)
}

enum class CableConstruction(val label: String, val shortLabel: String) {
    SINGLE_CORE_NON_ARMOURED("Single-Core Non-Armoured", "1C Non-Arm"),
    MULTICORE_NON_ARMOURED("Multicore Non-Armoured", "MC Non-Arm"),
    MULTICORE_ARMOURED_SWA("Multicore Armoured (SWA)", "MC SWA"),
    SINGLE_CORE_ARMOURED_AWA("Single-Core Armoured (AWA)", "1C AWA"),
    SOLAR_DC_DOUBLE_INSULATED("Solar PV DC Double-Insulated", "PV DC")
}

enum class CableArrangement(val label: String) {
    TOUCHING_FLAT("Touching (Flat Formation)"),
    TREFOIL("Trefoil Formation"),
    SPACED_1D("Spaced (≥ 1 Cable Diameter)"),
    BUNDLED_IN_CONDUIT("Enclosed / Bundled in Conduit or Trunking"),
    BURIED_DIRECT("Buried Direct in Ground"),
    BURIED_IN_DUCT("Buried in Underground Duct")
}

enum class ReferenceMethodCode(val code: String, val shortTitle: String) {
    A1("A1", "Insulated conductors in conduit in thermally insulated wall"),
    A2("A2", "Multicore cable in conduit in thermally insulated wall"),
    B1("B1", "Insulated conductors in conduit on wall / trunking"),
    B2("B2", "Multicore cable in conduit on wall / trunking"),
    C("C", "Clipped direct to non-metallic surface / masonry"),
    D1("D1", "Multicore cable in underground ducts"),
    D2("D2", "Cable buried direct in ground"),
    E("E", "Multicore cable in free air / perforated tray"),
    F("F", "Single-core cables in free air / tray (touching)"),
    G("G", "Single-core cables in free air (spaced ≥ 1 De)")
}

enum class ProtectiveDeviceType(
    val label: String,
    val defaultOverloadRatioI2ToIn: Double,
    val supportsAc: Boolean,
    val supportsDc: Boolean
) {
    MCB_TYPE_B("MCB Type B (BS EN 60898)", 1.45, true, false),
    MCB_TYPE_C("MCB Type C (BS EN 60898)", 1.45, true, false),
    MCB_TYPE_D("MCB Type D (BS EN 60898)", 1.45, true, false),
    MCCB_THERMAL_MAGNETIC("MCCB (BS EN 60947-2)", 1.30, true, true),
    FUSE_BS88_GG("HRC Fuse gG (BS 88)", 1.60, true, true),
    FUSE_BS3036("Semi-Enclosed Fuse (BS 3036)", 2.00, true, false),
    DC_FUSE_GPV("DC PV Fuse (gPV)", 1.35, false, true),
    DC_MCCB("DC Moulded Case Breaker", 1.30, false, true)
}

enum class DataSourceType(val label: String) {
    AUTHORIZED_BS7671("Authorized BS 7671 Source"),
    VERIFIED_REFERENCE("Verified Engineering Reference"),
    GENERIC_MANUFACTURER_INDEPENDENT("Generic Manufacturer-Independent Baseline"),
    USER_INPUT("User Input"),
    ENGINEERING_ASSUMPTION("Engineering Assumption")
}

enum class VerificationStatus(val label: String) {
    VERIFIED("Verified"),
    PARTIALLY_VERIFIED("Partially Verified"),
    NOT_VERIFIED("Not Verified"),
    UNSUPPORTED("Unsupported")
}

enum class CheckStatus(val label: String) {
    PASS("PASS"),
    FAIL("FAIL"),
    NOT_VERIFIED("NOT VERIFIED")
}

enum class CandidateDecision(val label: String) {
    SELECTED("Selected"),
    ACCEPTED_ALTERNATIVE("Accepted Alternative"),
    REJECTED("Rejected")
}

enum class RejectionReason(val label: String) {
    INSUFFICIENT_AMPACITY("Insufficient current-carrying capacity"),
    EXCESSIVE_VOLTAGE_DROP("Excessive voltage drop"),
    INSUFFICIENT_SHORT_CIRCUIT_WITHSTAND("Insufficient short-circuit withstand"),
    PROTECTION_COORDINATION_FAILURE("Protection coordination failure"),
    UNSUPPORTED_INSTALLATION_CONDITION("Unsupported installation condition"),
    MISSING_VERIFICATION_DATA("Missing verification data")
}
