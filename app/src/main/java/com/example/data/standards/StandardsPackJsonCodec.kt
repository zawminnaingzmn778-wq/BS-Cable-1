package com.example.data.standards

import com.example.domain.model.AcPhaseConfiguration
import com.example.domain.model.CableArrangement
import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CableConstruction
import com.example.domain.model.CableSpecInput
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.DataSourceType
import com.example.domain.model.DcConfiguration
import com.example.domain.model.ElectricalSystemInput
import com.example.domain.model.InsulationType
import com.example.domain.model.LoadCharacteristic
import com.example.domain.model.LoadInputMode
import com.example.domain.model.ProjectMetadata
import com.example.domain.model.ProtectionAndLimitsInput
import com.example.domain.model.ProtectiveDeviceType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.RouteSegment
import com.example.domain.model.SegmentConditions
import com.example.domain.model.SegmentFactorValues
import com.example.domain.model.SizingMode
import com.example.domain.model.VerificationStatus
import org.json.JSONArray
import org.json.JSONObject

object StandardsPackJsonCodec {

    fun encodeProject(project: CircuitDesignProject): String {
        val root = JSONObject()
        root.put("standardEdition", project.standardEdition)

        val meta = JSONObject().apply {
            put("id", project.metadata.id)
            put("projectName", project.metadata.projectName)
            put("projectReference", project.metadata.projectReference)
            put("client", project.metadata.client)
            put("siteLocation", project.metadata.siteLocation)
            put("engineerName", project.metadata.engineerName)
            put("designDate", project.metadata.designDate)
            put("circuitId", project.metadata.circuitId)
            put("circuitDescription", project.metadata.circuitDescription)
            put("designNotes", project.metadata.designNotes)
            put("createdAtEpochMs", project.metadata.createdAtEpochMs)
            put("updatedAtEpochMs", project.metadata.updatedAtEpochMs)
        }
        root.put("metadata", meta)

        val sys = JSONObject().apply {
            put("systemType", project.system.systemType.name)
            put("acPhaseConfig", project.system.acPhaseConfig.name)
            put("dcConfig", project.system.dcConfig.name)
            put("nominalVoltageV", project.system.nominalVoltageV)
            put("designFrequencyHz", project.system.designFrequencyHz)
            put("loadInputMode", project.system.loadInputMode.name)
            put("directDesignCurrentA", project.system.directDesignCurrentA)
            put("activePowerKw", project.system.activePowerKw)
            put("apparentPowerKva", project.system.apparentPowerKva)
            put("powerFactor", project.system.powerFactor)
            put("efficiencyPct", project.system.efficiencyPct)
            put("loadCharacteristic", project.system.loadCharacteristic.name)
            put("continuousOperatingFactor", project.system.continuousOperatingFactor)
        }
        root.put("system", sys)

        val cab = JSONObject().apply {
            put("sizingMode", project.cable.sizingMode.name)
            put("conductorMaterial", project.cable.conductorMaterial.name)
            put("insulationType", project.cable.insulationType.name)
            put("cableConstruction", project.cable.cableConstruction.name)
            put("coreCount", project.cable.coreCount)
            put("loadedConductors", project.cable.loadedConductors)
            put("manualCableSizeMm2", project.cable.manualCableSizeMm2)
            put("parallelRuns", project.cable.parallelRuns)
            put("arrangement", project.cable.arrangement.name)
            put("maxOperatingTempC", project.cable.maxOperatingTempC)
            put("useCustomImpedance", project.cable.useCustomImpedance)
            put("customResistanceOhmPerKm", project.cable.customResistanceOhmPerKm ?: JSONObject.NULL)
            put("customReactanceOhmPerKm", project.cable.customReactanceOhmPerKm ?: JSONObject.NULL)
        }
        root.put("cable", cab)

        val segArr = JSONArray()
        project.segments.forEach { seg ->
            val mf = seg.conditions.manualFactors
            val mfObj = JSONObject().apply {
                put("caValue", mf.caValue)
                put("caSource", mf.caSource.name)
                put("caReference", mf.caReference)
                put("cgValue", mf.cgValue)
                put("cgSource", mf.cgSource.name)
                put("cgReference", mf.cgReference)
                put("ciValue", mf.ciValue)
                put("ciSource", mf.ciSource.name)
                put("ciReference", mf.ciReference)
                put("cdValue", mf.cdValue)
                put("cdSource", mf.cdSource.name)
                put("cdReference", mf.cdReference)
                put("csValue", mf.csValue)
                put("csSource", mf.csSource.name)
                put("csReference", mf.csReference)
                put("ccValue", mf.ccValue)
                put("ccSource", mf.ccSource.name)
                put("ccReference", mf.ccReference)
            }
            val condObj = JSONObject().apply {
                put("ambientAirTempC", seg.conditions.ambientAirTempC)
                put("groundTempC", seg.conditions.groundTempC)
                put("circuitsGroupedCount", seg.conditions.circuitsGroupedCount)
                put("thermalInsulationLengthMm", seg.conditions.thermalInsulationLengthMm)
                put("buriedDepthM", seg.conditions.buriedDepthM)
                put("soilThermalResistivityKmPerW", seg.conditions.soilThermalResistivityKmPerW)
                put("installedInDuct", seg.conditions.installedInDuct)
                put("useManualFactorOverrides", seg.conditions.useManualFactorOverrides)
                put("manualFactors", mfObj)
            }
            val segObj = JSONObject().apply {
                put("id", seg.id)
                put("orderIndex", seg.orderIndex)
                put("label", seg.label)
                put("lengthMeters", seg.lengthMeters)
                put("methodCode", seg.methodCode.name)
                put("conditions", condObj)
                put("notes", seg.notes)
                put("allowTransitionOverride", seg.allowTransitionOverride)
                put("overrideCableSizeMm2", seg.overrideCableSizeMm2 ?: JSONObject.NULL)
                put("overrideParallelRuns", seg.overrideParallelRuns ?: JSONObject.NULL)
            }
            segArr.put(segObj)
        }
        root.put("segments", segArr)

        val prot = JSONObject().apply {
            put("deviceType", project.protection.deviceType.name)
            put("ratedCurrentInA", project.protection.ratedCurrentInA)
            put("deviceSettingIrA", project.protection.deviceSettingIrA ?: JSONObject.NULL)
            put("breakingCapacityKa", project.protection.breakingCapacityKa)
            put("overloadFactorI2Ratio", project.protection.overloadFactorI2Ratio)
            put("maxEarthFaultLoopImpedanceZsOhm", project.protection.maxEarthFaultLoopImpedanceZsOhm ?: JSONObject.NULL)
            put("actualZsOhm", project.protection.actualZsOhm ?: JSONObject.NULL)
            put("prospectiveFaultCurrentIkKa", project.protection.prospectiveFaultCurrentIkKa)
            put("disconnectionTimeS", project.protection.disconnectionTimeS)
            put("voltageDropLimitPct", project.protection.voltageDropLimitPct)
            put("faultDurationS", project.protection.faultDurationS)
            put("cpcSizeMm2", project.protection.cpcSizeMm2 ?: JSONObject.NULL)
            put("cpcMaterial", project.protection.cpcMaterial.name)
            put("kValueOverride", project.protection.kValueOverride ?: JSONObject.NULL)
            put("kValueSource", project.protection.kValueSource.name)
        }
        root.put("protection", prot)

        return root.toString()
    }

    fun decodeProject(json: String): CircuitDesignProject {
        val root = JSONObject(json)
        val metaObj = root.getJSONObject("metadata")
        val metadata = ProjectMetadata(
            id = metaObj.getString("id"),
            projectName = metaObj.optString("projectName", ""),
            projectReference = metaObj.optString("projectReference", ""),
            client = metaObj.optString("client", ""),
            siteLocation = metaObj.optString("siteLocation", ""),
            engineerName = metaObj.optString("engineerName", ""),
            designDate = metaObj.optString("designDate", ""),
            circuitId = metaObj.optString("circuitId", "C1"),
            circuitDescription = metaObj.optString("circuitDescription", ""),
            designNotes = metaObj.optString("designNotes", ""),
            createdAtEpochMs = metaObj.optLong("createdAtEpochMs", 0L),
            updatedAtEpochMs = metaObj.optLong("updatedAtEpochMs", 0L)
        )

        val sysObj = root.getJSONObject("system")
        val system = ElectricalSystemInput(
            systemType = enumVal(sysObj.optString("systemType"), CurrentSystemType.AC),
            acPhaseConfig = enumVal(sysObj.optString("acPhaseConfig"), AcPhaseConfiguration.THREE_PHASE_3W_OR_4W),
            dcConfig = enumVal(sysObj.optString("dcConfig"), DcConfiguration.TWO_WIRE_UNGROUNDED_OR_PV),
            nominalVoltageV = sysObj.optDouble("nominalVoltageV", 400.0),
            designFrequencyHz = sysObj.optDouble("designFrequencyHz", 50.0),
            loadInputMode = enumVal(sysObj.optString("loadInputMode"), LoadInputMode.ACTIVE_POWER_KW),
            directDesignCurrentA = sysObj.optDouble("directDesignCurrentA", 45.0),
            activePowerKw = sysObj.optDouble("activePowerKw", 28.0),
            apparentPowerKva = sysObj.optDouble("apparentPowerKva", 32.0),
            powerFactor = sysObj.optDouble("powerFactor", 0.85),
            efficiencyPct = sysObj.optDouble("efficiencyPct", 100.0),
            loadCharacteristic = enumVal(sysObj.optString("loadCharacteristic"), LoadCharacteristic.GENERAL_RESISTIVE_MIXED),
            continuousOperatingFactor = sysObj.optDouble("continuousOperatingFactor", 1.0)
        )

        val cabObj = root.getJSONObject("cable")
        val cable = CableSpecInput(
            sizingMode = enumVal(cabObj.optString("sizingMode"), SizingMode.AUTOMATIC),
            conductorMaterial = enumVal(cabObj.optString("conductorMaterial"), ConductorMaterial.COPPER),
            insulationType = enumVal(cabObj.optString("insulationType"), InsulationType.XLPE_90C),
            cableConstruction = enumVal(cabObj.optString("cableConstruction"), CableConstruction.MULTICORE_ARMOURED_SWA),
            coreCount = cabObj.optInt("coreCount", 4),
            loadedConductors = cabObj.optInt("loadedConductors", 3),
            manualCableSizeMm2 = cabObj.optDouble("manualCableSizeMm2", 16.0),
            parallelRuns = cabObj.optInt("parallelRuns", 1),
            arrangement = enumVal(cabObj.optString("arrangement"), CableArrangement.TOUCHING_FLAT),
            maxOperatingTempC = cabObj.optDouble("maxOperatingTempC", 90.0),
            useCustomImpedance = cabObj.optBoolean("useCustomImpedance", false),
            customResistanceOhmPerKm = optNullableDouble(cabObj, "customResistanceOhmPerKm"),
            customReactanceOhmPerKm = optNullableDouble(cabObj, "customReactanceOhmPerKm")
        )

        val segArr = root.getJSONArray("segments")
        val segments = (0 until segArr.length()).map { idx ->
            val segObj = segArr.getJSONObject(idx)
            val condObj = segObj.getJSONObject("conditions")
            val mfObj = condObj.optJSONObject("manualFactors")
            val mf = if (mfObj != null) {
                SegmentFactorValues(
                    caValue = mfObj.optDouble("caValue", 1.0),
                    caSource = enumVal(mfObj.optString("caSource"), DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT),
                    caReference = mfObj.optString("caReference", ""),
                    cgValue = mfObj.optDouble("cgValue", 1.0),
                    cgSource = enumVal(mfObj.optString("cgSource"), DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT),
                    cgReference = mfObj.optString("cgReference", ""),
                    ciValue = mfObj.optDouble("ciValue", 1.0),
                    ciSource = enumVal(mfObj.optString("ciSource"), DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT),
                    ciReference = mfObj.optString("ciReference", ""),
                    cdValue = mfObj.optDouble("cdValue", 1.0),
                    cdSource = enumVal(mfObj.optString("cdSource"), DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT),
                    cdReference = mfObj.optString("cdReference", ""),
                    csValue = mfObj.optDouble("csValue", 1.0),
                    csSource = enumVal(mfObj.optString("csSource"), DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT),
                    csReference = mfObj.optString("csReference", ""),
                    ccValue = mfObj.optDouble("ccValue", 1.0),
                    ccSource = enumVal(mfObj.optString("ccSource"), DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT),
                    ccReference = mfObj.optString("ccReference", "")
                )
            } else {
                SegmentFactorValues()
            }
            RouteSegment(
                id = segObj.getString("id"),
                orderIndex = segObj.optInt("orderIndex", idx),
                label = segObj.optString("label", "Segment ${idx + 1}"),
                lengthMeters = segObj.optDouble("lengthMeters", 10.0),
                methodCode = enumVal(segObj.optString("methodCode"), ReferenceMethodCode.C),
                conditions = SegmentConditions(
                    ambientAirTempC = condObj.optDouble("ambientAirTempC", 30.0),
                    groundTempC = condObj.optDouble("groundTempC", 20.0),
                    circuitsGroupedCount = condObj.optInt("circuitsGroupedCount", 1),
                    thermalInsulationLengthMm = condObj.optDouble("thermalInsulationLengthMm", 0.0),
                    buriedDepthM = condObj.optDouble("buriedDepthM", 0.7),
                    soilThermalResistivityKmPerW = condObj.optDouble("soilThermalResistivityKmPerW", 2.5),
                    installedInDuct = condObj.optBoolean("installedInDuct", false),
                    useManualFactorOverrides = condObj.optBoolean("useManualFactorOverrides", false),
                    manualFactors = mf
                ),
                notes = segObj.optString("notes", ""),
                allowTransitionOverride = segObj.optBoolean("allowTransitionOverride", false),
                overrideCableSizeMm2 = optNullableDouble(segObj, "overrideCableSizeMm2"),
                overrideParallelRuns = optNullableInt(segObj, "overrideParallelRuns")
            )
        }

        val protObj = root.getJSONObject("protection")
        val protection = ProtectionAndLimitsInput(
            deviceType = enumVal(protObj.optString("deviceType"), ProtectiveDeviceType.MCCB_THERMAL_MAGNETIC),
            ratedCurrentInA = protObj.optDouble("ratedCurrentInA", 50.0),
            deviceSettingIrA = optNullableDouble(protObj, "deviceSettingIrA"),
            breakingCapacityKa = protObj.optDouble("breakingCapacityKa", 10.0),
            overloadFactorI2Ratio = protObj.optDouble("overloadFactorI2Ratio", 1.30),
            maxEarthFaultLoopImpedanceZsOhm = optNullableDouble(protObj, "maxEarthFaultLoopImpedanceZsOhm"),
            actualZsOhm = optNullableDouble(protObj, "actualZsOhm"),
            prospectiveFaultCurrentIkKa = protObj.optDouble("prospectiveFaultCurrentIkKa", 6.0),
            disconnectionTimeS = protObj.optDouble("disconnectionTimeS", 0.4),
            voltageDropLimitPct = protObj.optDouble("voltageDropLimitPct", 5.0),
            faultDurationS = protObj.optDouble("faultDurationS", 0.1),
            cpcSizeMm2 = optNullableDouble(protObj, "cpcSizeMm2"),
            cpcMaterial = enumVal(protObj.optString("cpcMaterial"), ConductorMaterial.COPPER),
            kValueOverride = optNullableDouble(protObj, "kValueOverride"),
            kValueSource = enumVal(protObj.optString("kValueSource"), DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT)
        )

        return CircuitDesignProject(
            metadata = metadata,
            system = system,
            cable = cable,
            segments = segments,
            protection = protection,
            standardEdition = root.optString("standardEdition", "BS 7671:2018+A2:2022")
        )
    }

    fun encodeAmpacityMap(map: Map<ReferenceMethodCode, Double>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.name, v) }
        return obj.toString()
    }

    fun decodeAmpacityMap(json: String): Map<ReferenceMethodCode, Double> {
        val obj = JSONObject(json)
        val out = mutableMapOf<ReferenceMethodCode, Double>()
        obj.keys().forEach { key ->
            val code = ReferenceMethodCode.entries.firstOrNull { it.name == key }
            if (code != null) {
                out[code] = obj.getDouble(key)
            }
        }
        return out
    }

    fun exportCatalogPackJson(entries: List<CableCatalogEntry>, edition: String): String {
        val root = JSONObject()
        root.put("packTitle", "BS 7671 Cable Standards Data Pack")
        root.put("standardEdition", edition)
        val arr = JSONArray()
        entries.forEach { e ->
            val obj = JSONObject().apply {
                put("cableId", e.cableId)
                put("material", e.material.name)
                put("insulation", e.insulation.name)
                put("construction", e.construction.name)
                put("coreCount", e.coreCount)
                put("sizeMm2", e.sizeMm2)
                put("maxOperatingTempC", e.maxOperatingTempC)
                put("ampacityByMethod", JSONObject(encodeAmpacityMap(e.ampacityByMethod)))
                put("voltageDropMvPerAPerM", e.voltageDropMvPerAPerM ?: JSONObject.NULL)
                put("resistanceOhmPerKm20C", e.resistanceOhmPerKm20C ?: JSONObject.NULL)
                put("reactanceOhmPerKm50Hz", e.reactanceOhmPerKm50Hz ?: JSONObject.NULL)
                put("kFactorAdiabatic", e.kFactorAdiabatic ?: JSONObject.NULL)
                put("dataSource", e.dataSource.name)
                put("referenceEdition", e.referenceEdition)
                put("referenceIdentifier", e.referenceIdentifier)
                put("verificationStatus", e.verificationStatus.name)
                put("notes", e.notes)
            }
            arr.put(obj)
        }
        root.put("cables", arr)
        return root.toString(2)
    }

    fun importCatalogPackJson(json: String): List<CableCatalogEntry> {
        val root = JSONObject(json)
        val edition = root.optString("standardEdition", "BS 7671:2018+A2:2022")
        val arr = root.getJSONArray("cables")
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val ampObj = obj.getJSONObject("ampacityByMethod")
            val ampMap = mutableMapOf<ReferenceMethodCode, Double>()
            ampObj.keys().forEach { key ->
                ReferenceMethodCode.entries.firstOrNull { it.name == key }?.let { code ->
                    ampMap[code] = ampObj.getDouble(key)
                }
            }
            CableCatalogEntry(
                cableId = obj.getString("cableId"),
                material = enumVal(obj.optString("material"), ConductorMaterial.COPPER),
                insulation = enumVal(obj.optString("insulation"), InsulationType.XLPE_90C),
                construction = enumVal(obj.optString("construction"), CableConstruction.MULTICORE_ARMOURED_SWA),
                coreCount = obj.optInt("coreCount", 4),
                sizeMm2 = obj.getDouble("sizeMm2"),
                maxOperatingTempC = obj.optDouble("maxOperatingTempC", 90.0),
                ampacityByMethod = ampMap,
                voltageDropMvPerAPerM = optNullableDouble(obj, "voltageDropMvPerAPerM"),
                resistanceOhmPerKm20C = optNullableDouble(obj, "resistanceOhmPerKm20C"),
                reactanceOhmPerKm50Hz = optNullableDouble(obj, "reactanceOhmPerKm50Hz"),
                kFactorAdiabatic = optNullableDouble(obj, "kFactorAdiabatic"),
                dataSource = enumVal(obj.optString("dataSource"), DataSourceType.AUTHORIZED_BS7671),
                referenceEdition = obj.optString("referenceEdition", edition),
                referenceIdentifier = obj.optString("referenceIdentifier", "Imported Standards Pack"),
                verificationStatus = enumVal(obj.optString("verificationStatus"), VerificationStatus.VERIFIED),
                notes = obj.optString("notes", "")
            )
        }
    }

    private fun optNullableDouble(obj: JSONObject, key: String): Double? =
        if (!obj.has(key) || obj.isNull(key)) null else obj.optDouble(key)

    private fun optNullableInt(obj: JSONObject, key: String): Int? =
        if (!obj.has(key) || obj.isNull(key)) null else obj.optInt(key)

    private inline fun <reified T : Enum<T>> enumVal(name: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback
}
