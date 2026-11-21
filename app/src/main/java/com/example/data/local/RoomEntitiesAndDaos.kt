package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val projectName: String,
    val projectReference: String,
    val client: String,
    val circuitId: String,
    val sizingMode: String,
    val systemType: String,
    val lastStatus: String,
    val recommendedSizeSummary: String,
    val projectJson: String,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "cable_catalog")
data class CableCatalogEntity(
    @PrimaryKey val cableId: String,
    val material: String,
    val insulation: String,
    val construction: String,
    val coreCount: Int,
    val sizeMm2: Double,
    val maxOperatingTempC: Double,
    val ampacityMapJson: String,
    val voltageDropMvPerAPerM: Double?,
    val voltageDropResistiveMvPerAPerM: Double?,
    val voltageDropReactiveMvPerAPerM: Double?,
    val resistanceOhmPerKm20C: Double?,
    val reactanceOhmPerKm50Hz: Double?,
    val kFactorAdiabatic: Double?,
    val dataSource: String,
    val referenceEdition: String,
    val referenceIdentifier: String,
    val verificationStatus: String,
    val notes: String,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "saved_reports")
data class SavedReportEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val projectName: String,
    val circuitId: String,
    val standardEdition: String,
    val overallStatus: String,
    val dataVerificationLevel: String,
    val recommendedCableSummary: String,
    val totalLengthM: Double,
    val voltageDropPct: Double?,
    val projectSnapshotJson: String,
    val createdAtEpochMs: Long
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CableCatalogDao {
    @Query("SELECT * FROM cable_catalog ORDER BY material ASC, insulation ASC, construction ASC, sizeMm2 ASC")
    fun observeAll(): Flow<List<CableCatalogEntity>>

    @Query("SELECT COUNT(*) FROM cable_catalog")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<CableCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CableCatalogEntity)

    @Query("DELETE FROM cable_catalog WHERE cableId = :cableId")
    suspend fun deleteById(cableId: String)

    @Query("DELETE FROM cable_catalog")
    suspend fun deleteAll()
}

@Dao
interface SavedReportDao {
    @Query("SELECT * FROM saved_reports ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<SavedReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedReportEntity)

    @Query("DELETE FROM saved_reports WHERE id = :id")
    suspend fun deleteById(id: String)
}
