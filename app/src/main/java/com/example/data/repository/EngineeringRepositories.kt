package com.example.data.repository

import com.example.data.local.CableCatalogDao
import com.example.data.local.CableCatalogEntity
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.SavedReportDao
import com.example.data.local.SavedReportEntity
import com.example.data.standards.StandardsPackJsonCodec
import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.CableConstruction
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.DataSourceType
import com.example.domain.model.EngineeringCalculationResult
import com.example.domain.model.InsulationType
import com.example.domain.model.SizingMode
import com.example.domain.model.VerificationStatus
import com.example.domain.standards.GenericStandardsDataset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class EngineeringProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.observeAll()

    suspend fun loadProject(id: String): CircuitDesignProject? {
        val entity = projectDao.getById(id) ?: return null
        return runCatching { StandardsPackJsonCodec.decodeProject(entity.projectJson) }.getOrNull()
    }

    suspend fun saveProject(
        project: CircuitDesignProject,
        calcResult: EngineeringCalculationResult?
    ) {
        val now = System.currentTimeMillis()
        val updatedMeta = project.metadata.copy(
            createdAtEpochMs = if (project.metadata.createdAtEpochMs == 0L) now else project.metadata.createdAtEpochMs,
            updatedAtEpochMs = now
        )
        val updatedProject = project.copy(metadata = updatedMeta)
        val recSummary = if (updatedProject.cable.sizingMode == SizingMode.AUTOMATIC) {
            calcResult?.recommendedCandidate?.let {
                "${it.sizeMm2} mm² (${it.parallelRuns}×) ${updatedProject.cable.conductorMaterial.symbol}"
            } ?: "No compliant size"
        } else {
            "${updatedProject.cable.manualCableSizeMm2} mm² (${updatedProject.cable.parallelRuns}×) Manual"
        }
        val entity = ProjectEntity(
            id = updatedMeta.id,
            projectName = updatedMeta.projectName.ifBlank { "Untitled Circuit Project" },
            projectReference = updatedMeta.projectReference,
            client = updatedMeta.client,
            circuitId = updatedMeta.circuitId.ifBlank { "C1" },
            sizingMode = updatedProject.cable.sizingMode.label,
            systemType = updatedProject.system.systemType.label,
            lastStatus = calcResult?.overallStatus?.label ?: "NOT VERIFIED",
            recommendedSizeSummary = recSummary,
            projectJson = StandardsPackJsonCodec.encodeProject(updatedProject),
            updatedAtEpochMs = now
        )
        projectDao.upsert(entity)
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
    }
}

class CableCatalogRepository(private val dao: CableCatalogDao) {

    val catalogEntries: Flow<List<CableCatalogEntry>> = dao.observeAll().map { rows ->
        rows.map { it.toDomain() }
    }

    suspend fun ensureSeeded(edition: String = "BS 7671:2018+A2:2022") {
        if (dao.count() == 0) {
            val generic = GenericStandardsDataset.createGenericBaselineCatalog(edition)
            dao.upsertAll(generic.map { it.toEntity() })
        }
    }

    suspend fun resetToGenericBaseline(edition: String) {
        dao.deleteAll()
        val generic = GenericStandardsDataset.createGenericBaselineCatalog(edition)
        dao.upsertAll(generic.map { it.toEntity() })
    }

    suspend fun upsertEntry(entry: CableCatalogEntry) {
        dao.upsert(entry.toEntity())
    }

    suspend fun importJsonPack(json: String): Result<Int> = runCatching {
        val imported = StandardsPackJsonCodec.importCatalogPackJson(json)
        dao.upsertAll(imported.map { it.toEntity() })
        imported.size
    }

    suspend fun deleteEntry(cableId: String) {
        dao.deleteById(cableId)
    }

    private fun CableCatalogEntity.toDomain(): CableCatalogEntry = CableCatalogEntry(
        cableId = cableId,
        material = enumValueOrDefault(material, ConductorMaterial.COPPER),
        insulation = enumValueOrDefault(insulation, InsulationType.XLPE_90C),
        construction = enumValueOrDefault(construction, CableConstruction.MULTICORE_ARMOURED_SWA),
        coreCount = coreCount,
        sizeMm2 = sizeMm2,
        maxOperatingTempC = maxOperatingTempC,
        ampacityByMethod = StandardsPackJsonCodec.decodeAmpacityMap(ampacityMapJson),
        voltageDropMvPerAPerM = voltageDropMvPerAPerM,
        voltageDropResistiveMvPerAPerM = voltageDropResistiveMvPerAPerM,
        voltageDropReactiveMvPerAPerM = voltageDropReactiveMvPerAPerM,
        resistanceOhmPerKm20C = resistanceOhmPerKm20C,
        reactanceOhmPerKm50Hz = reactanceOhmPerKm50Hz,
        kFactorAdiabatic = kFactorAdiabatic,
        dataSource = enumValueOrDefault(dataSource, DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT),
        referenceEdition = referenceEdition,
        referenceIdentifier = referenceIdentifier,
        verificationStatus = enumValueOrDefault(verificationStatus, VerificationStatus.NOT_VERIFIED),
        notes = notes
    )

    private fun CableCatalogEntry.toEntity(): CableCatalogEntity = CableCatalogEntity(
        cableId = cableId,
        material = material.name,
        insulation = insulation.name,
        construction = construction.name,
        coreCount = coreCount,
        sizeMm2 = sizeMm2,
        maxOperatingTempC = maxOperatingTempC,
        ampacityMapJson = StandardsPackJsonCodec.encodeAmpacityMap(ampacityByMethod),
        voltageDropMvPerAPerM = voltageDropMvPerAPerM,
        voltageDropResistiveMvPerAPerM = voltageDropResistiveMvPerAPerM,
        voltageDropReactiveMvPerAPerM = voltageDropReactiveMvPerAPerM,
        resistanceOhmPerKm20C = resistanceOhmPerKm20C,
        reactanceOhmPerKm50Hz = reactanceOhmPerKm50Hz,
        kFactorAdiabatic = kFactorAdiabatic,
        dataSource = dataSource.name,
        referenceEdition = referenceEdition,
        referenceIdentifier = referenceIdentifier,
        verificationStatus = verificationStatus.name,
        notes = notes,
        updatedAtEpochMs = System.currentTimeMillis()
    )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}

class ReportRepository(private val dao: SavedReportDao) {
    val allReports: Flow<List<SavedReportEntity>> = dao.observeAll()

    suspend fun saveReportSnapshot(
        project: CircuitDesignProject,
        result: EngineeringCalculationResult
    ): SavedReportEntity {
        val activeCandidate = result.recommendedCandidate ?: result.evaluatedManualCandidate
        val cableSummary = activeCandidate?.let {
            "${it.sizeMm2} mm² (${it.parallelRuns}×) ${project.cable.conductorMaterial.symbol} ${project.cable.insulationType.name}"
        } ?: "${project.cable.manualCableSizeMm2} mm²"

        val entity = SavedReportEntity(
            id = UUID.randomUUID().toString(),
            projectId = project.metadata.id,
            projectName = project.metadata.projectName.ifBlank { "Untitled Circuit Project" },
            circuitId = project.metadata.circuitId.ifBlank { "C1" },
            standardEdition = project.standardEdition,
            overallStatus = result.overallStatus.label,
            dataVerificationLevel = result.dataVerificationLevel.label,
            recommendedCableSummary = cableSummary,
            totalLengthM = result.totalRouteLengthM,
            voltageDropPct = activeCandidate?.voltageDropPct,
            projectSnapshotJson = StandardsPackJsonCodec.encodeProject(project),
            createdAtEpochMs = System.currentTimeMillis()
        )
        dao.insert(entity)
        return entity
    }

    suspend fun deleteReport(id: String) = dao.deleteById(id)
}
