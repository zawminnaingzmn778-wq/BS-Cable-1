package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.local.ProjectEntity
import com.example.data.local.SavedReportEntity
import com.example.data.preferences.EngineeringAppSettings
import com.example.data.standards.StandardsPackJsonCodec
import com.example.domain.engine.CableSizingEngine
import com.example.domain.engine.EngineeringQaRunner
import com.example.domain.engine.QaTestCaseResult
import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CableConstruction
import com.example.domain.model.CableSpecInput
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.ElectricalSystemInput
import com.example.domain.model.EngineeringCalculationResult
import com.example.domain.model.LoadCharacteristic
import com.example.domain.model.LoadInputMode
import com.example.domain.model.ProjectMetadata
import com.example.domain.model.ProtectionAndLimitsInput
import com.example.domain.model.ProtectiveDeviceType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.RouteSegment
import com.example.domain.model.SegmentConditions
import com.example.domain.model.SizingMode
import com.example.domain.model.defaultRouteSegments
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainEngineeringViewModel(
    private val container: AppContainer
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = container.projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val catalogEntries: StateFlow<List<CableCatalogEntry>> = container.cableRepository.catalogEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedReports: StateFlow<List<SavedReportEntity>> = container.reportRepository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<EngineeringAppSettings> = container.settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EngineeringAppSettings())

    private val _activeProject = MutableStateFlow(createDefaultProject(SizingMode.AUTOMATIC, CurrentSystemType.AC))
    val activeProject: StateFlow<CircuitDesignProject> = _activeProject.asStateFlow()

    private val _activeSegmentIndex = MutableStateFlow(0)
    val activeSegmentIndex: StateFlow<Int> = _activeSegmentIndex.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val calculationResult: StateFlow<EngineeringCalculationResult> = combine(
        _activeProject,
        catalogEntries
    ) { project, catalog ->
        CableSizingEngine.evaluate(project, catalog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CableSizingEngine.evaluate(_activeProject.value, emptyList())
    )

    init {
        viewModelScope.launch {
            val settings = container.settingsStore.settingsFlow.first()
            container.cableRepository.ensureSeeded(settings.activeStandardEdition)
            // Seed one realistic starter demo project on very first install if no projects exist
            val existing = container.projectRepository.allProjects.first()
            if (existing.isEmpty()) {
                val starter = createDefaultProject(SizingMode.AUTOMATIC, CurrentSystemType.AC).let { p ->
                    p.copy(
                        metadata = p.metadata.copy(
                            projectName = "Main Sub-Distribution Feeder",
                            projectReference = "PRJ-LV-001",
                            client = "Commercial Facility",
                            siteLocation = "Plant Room B to DB-02",
                            engineerName = settings.defaultEngineerName.ifBlank { "Lead Electrical Engineer" },
                            circuitId = "DB02-FDR",
                            circuitDescription = "400V 3-Phase 28 kW Sub-Main Feeder"
                        ),
                        standardEdition = settings.activeStandardEdition
                    )
                }
                _activeProject.value = starter
                val cat = container.cableRepository.catalogEntries.first()
                val res = CableSizingEngine.evaluate(starter, cat)
                container.projectRepository.saveProject(starter, res)
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun startNewProject(
        mode: SizingMode,
        systemType: CurrentSystemType = CurrentSystemType.AC,
        onReady: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val settings = appSettings.value
            val created = createDefaultProject(mode, systemType).copy(
                standardEdition = settings.activeStandardEdition,
                metadata = ProjectMetadata(
                    id = UUID.randomUUID().toString(),
                    projectName = if (systemType == CurrentSystemType.DC) "New Solar PV DC Circuit" else "New LV AC Circuit",
                    engineerName = settings.defaultEngineerName,
                    designDate = todayIso(),
                    circuitId = if (systemType == CurrentSystemType.DC) "PV-STR-01" else "C1"
                ),
                protection = if (systemType == CurrentSystemType.DC) {
                    ProtectionAndLimitsInput(
                        deviceType = ProtectiveDeviceType.DC_FUSE_GPV,
                        ratedCurrentInA = 25.0,
                        breakingCapacityKa = 10.0,
                        overloadFactorI2Ratio = 1.35,
                        prospectiveFaultCurrentIkKa = 1.5,
                        voltageDropLimitPct = 3.0,
                        faultDurationS = 0.1
                    )
                } else {
                    ProtectionAndLimitsInput(
                        voltageDropLimitPct = settings.defaultVoltageDropPowerPct
                    )
                }
            )
            _activeProject.value = created
            _activeSegmentIndex.value = 0
            onReady()
        }
    }

    fun openProject(id: String, onLoaded: () -> Unit = {}) {
        viewModelScope.launch {
            val loaded = container.projectRepository.loadProject(id)
            if (loaded != null) {
                _activeProject.value = loaded
                _activeSegmentIndex.value = 0
                onLoaded()
            } else {
                _statusMessage.value = "Unable to open project."
            }
        }
    }

    fun openProjectFromJson(json: String, onLoaded: () -> Unit = {}) {
        runCatching { StandardsPackJsonCodec.decodeProject(json) }
            .onSuccess {
                _activeProject.value = it
                _activeSegmentIndex.value = 0
                onLoaded()
            }
            .onFailure {
                _statusMessage.value = "Could not load report snapshot."
            }
    }

    fun saveActiveProject() {
        viewModelScope.launch {
            container.projectRepository.saveProject(_activeProject.value, calculationResult.value)
            _statusMessage.value = "Project saved locally."
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            container.projectRepository.deleteProject(id)
        }
    }

    fun updateMetadata(transform: (ProjectMetadata) -> ProjectMetadata) {
        _activeProject.update { it.copy(metadata = transform(it.metadata)) }
    }

    fun updateSystem(transform: (ElectricalSystemInput) -> ElectricalSystemInput) {
        _activeProject.update { current ->
            val updatedSys = transform(current.system)
            val updatedCable = if (updatedSys.systemType == CurrentSystemType.DC &&
                current.cable.cableConstruction == CableConstruction.MULTICORE_ARMOURED_SWA &&
                updatedSys.loadCharacteristic == LoadCharacteristic.SOLAR_PV_DC_STRING
            ) {
                current.cable.copy(
                    cableConstruction = CableConstruction.SOLAR_DC_DOUBLE_INSULATED,
                    coreCount = 1,
                    loadedConductors = 2
                )
            } else {
                current.cable
            }
            val updatedProt = if (updatedSys.systemType != current.system.systemType) {
                if (updatedSys.systemType == CurrentSystemType.DC && !current.protection.deviceType.supportsDc) {
                    current.protection.copy(
                        deviceType = ProtectiveDeviceType.DC_MCCB,
                        overloadFactorI2Ratio = ProtectiveDeviceType.DC_MCCB.defaultOverloadRatioI2ToIn
                    )
                } else if (updatedSys.systemType == CurrentSystemType.AC && !current.protection.deviceType.supportsAc) {
                    current.protection.copy(
                        deviceType = ProtectiveDeviceType.MCCB_THERMAL_MAGNETIC,
                        overloadFactorI2Ratio = ProtectiveDeviceType.MCCB_THERMAL_MAGNETIC.defaultOverloadRatioI2ToIn
                    )
                } else {
                    current.protection
                }
            } else {
                current.protection
            }
            current.copy(system = updatedSys, cable = updatedCable, protection = updatedProt)
        }
    }

    fun updateCable(transform: (CableSpecInput) -> CableSpecInput) {
        _activeProject.update { it.copy(cable = transform(it.cable)) }
    }

    fun updateProtection(transform: (ProtectionAndLimitsInput) -> ProtectionAndLimitsInput) {
        _activeProject.update { it.copy(protection = transform(it.protection)) }
    }

    fun setSizingMode(mode: SizingMode) {
        updateCable { it.copy(sizingMode = mode) }
    }

    fun selectActiveSegment(index: Int) {
        val count = _activeProject.value.segments.size
        if (count > 0) {
            _activeSegmentIndex.value = index.coerceIn(0, count - 1)
        }
    }

    fun addSegment() {
        _activeProject.update { proj ->
            val nextIdx = proj.segments.size
            val newSeg = RouteSegment(
                id = UUID.randomUUID().toString(),
                orderIndex = nextIdx,
                label = "Segment ${nextIdx + 1}",
                lengthMeters = 15.0,
                methodCode = ReferenceMethodCode.C,
                conditions = SegmentConditions()
            )
            proj.copy(segments = proj.segments + newSeg)
        }
        _activeSegmentIndex.value = (_activeProject.value.segments.size - 1).coerceAtLeast(0)
    }

    fun updateSegment(index: Int, transform: (RouteSegment) -> RouteSegment) {
        _activeProject.update { proj ->
            val list = proj.segments.toMutableList()
            if (index in list.indices) {
                list[index] = transform(list[index])
            }
            proj.copy(segments = list.mapIndexed { idx, s -> s.copy(orderIndex = idx) })
        }
    }

    fun duplicateSegment(index: Int) {
        _activeProject.update { proj ->
            val list = proj.segments.toMutableList()
            if (index in list.indices) {
                val src = list[index]
                val copy = src.copy(
                    id = UUID.randomUUID().toString(),
                    label = "${src.label} (Copy)"
                )
                list.add(index + 1, copy)
            }
            proj.copy(segments = list.mapIndexed { idx, s -> s.copy(orderIndex = idx) })
        }
    }

    fun deleteSegment(index: Int) {
        _activeProject.update { proj ->
            if (proj.segments.size <= 1) return@update proj
            val list = proj.segments.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            proj.copy(segments = list.mapIndexed { idx, s -> s.copy(orderIndex = idx) })
        }
        val maxIdx = (_activeProject.value.segments.size - 1).coerceAtLeast(0)
        if (_activeSegmentIndex.value > maxIdx) {
            _activeSegmentIndex.value = maxIdx
        }
    }

    fun moveSegment(index: Int, delta: Int) {
        _activeProject.update { proj ->
            val list = proj.segments.toMutableList()
            val target = index + delta
            if (index in list.indices && target in list.indices) {
                val item = list.removeAt(index)
                list.add(target, item)
                _activeSegmentIndex.value = target
            }
            proj.copy(segments = list.mapIndexed { idx, s -> s.copy(orderIndex = idx) })
        }
    }

    fun saveReportSnapshot() {
        viewModelScope.launch {
            container.projectRepository.saveProject(_activeProject.value, calculationResult.value)
            container.reportRepository.saveReportSnapshot(_activeProject.value, calculationResult.value)
            _statusMessage.value = "Engineering report snapshot saved."
        }
    }

    fun deleteReport(id: String) {
        viewModelScope.launch {
            container.reportRepository.deleteReport(id)
        }
    }

    fun upsertCableEntry(entry: CableCatalogEntry) {
        viewModelScope.launch {
            container.cableRepository.upsertEntry(entry)
            _statusMessage.value = "Cable record saved (${entry.sizeMm2} mm²)."
        }
    }

    fun deleteCableEntry(cableId: String) {
        viewModelScope.launch {
            container.cableRepository.deleteEntry(cableId)
        }
    }

    fun importCatalogPackJson(json: String) {
        viewModelScope.launch {
            container.cableRepository.importJsonPack(json)
                .onSuccess { count ->
                    _statusMessage.value = "Imported $count cable database record(s)."
                }
                .onFailure { err ->
                    _statusMessage.value = "Import failed: ${err.message ?: "Invalid JSON format"}"
                }
        }
    }

    fun exportCatalogPackJson(): String =
        StandardsPackJsonCodec.exportCatalogPackJson(
            entries = catalogEntries.value,
            edition = appSettings.value.activeStandardEdition
        )

    fun resetCatalogToBaseline() {
        viewModelScope.launch {
            container.cableRepository.resetToGenericBaseline(appSettings.value.activeStandardEdition)
            _statusMessage.value = "Cable database reset to Generic Manufacturer-Independent Baseline."
        }
    }

    fun updateSettings(transform: (EngineeringAppSettings) -> EngineeringAppSettings) {
        viewModelScope.launch {
            container.settingsStore.updateSettings(transform)
            val latest = container.settingsStore.settingsFlow.first()
            _activeProject.update { it.copy(standardEdition = latest.activeStandardEdition) }
        }
    }

    fun runEngineeringQa(): List<QaTestCaseResult> =
        EngineeringQaRunner.runAll(catalogEntries.value)

    private fun createDefaultProject(
        mode: SizingMode,
        systemType: CurrentSystemType
    ): CircuitDesignProject {
        val system = if (systemType == CurrentSystemType.DC) {
            ElectricalSystemInput(
                systemType = CurrentSystemType.DC,
                nominalVoltageV = 600.0,
                loadInputMode = LoadInputMode.ACTIVE_POWER_KW,
                activePowerKw = 12.0,
                efficiencyPct = 100.0,
                loadCharacteristic = LoadCharacteristic.SOLAR_PV_DC_STRING
            )
        } else {
            ElectricalSystemInput(systemType = CurrentSystemType.AC)
        }

        val cable = if (systemType == CurrentSystemType.DC) {
            CableSpecInput(
                sizingMode = mode,
                cableConstruction = CableConstruction.SOLAR_DC_DOUBLE_INSULATED,
                coreCount = 1,
                loadedConductors = 2,
                manualCableSizeMm2 = 6.0
            )
        } else {
            CableSpecInput(sizingMode = mode)
        }

        val segments = if (systemType == CurrentSystemType.DC) {
            listOf(
                RouteSegment(
                    id = UUID.randomUUID().toString(),
                    orderIndex = 0,
                    label = "Roof Array Conduit",
                    lengthMeters = 25.0,
                    methodCode = ReferenceMethodCode.B1,
                    conditions = SegmentConditions(ambientAirTempC = 45.0, circuitsGroupedCount = 2)
                ),
                RouteSegment(
                    id = UUID.randomUUID().toString(),
                    orderIndex = 1,
                    label = "Inverter Tray Run",
                    lengthMeters = 15.0,
                    methodCode = ReferenceMethodCode.E,
                    conditions = SegmentConditions(ambientAirTempC = 35.0, circuitsGroupedCount = 1)
                )
            )
        } else {
            defaultRouteSegments()
        }

        return CircuitDesignProject(
            metadata = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                designDate = todayIso()
            ),
            system = system,
            cable = cable,
            segments = segments
        )
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainEngineeringViewModel(container) as T
        }
    }
}
