package com.airgorah.mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessPointDao {
    @Query("SELECT * FROM access_points ORDER BY lastSeenEpochMs DESC")
    fun observeAll(): Flow<List<AccessPointEntity>>

    @Query("SELECT * FROM access_points ORDER BY lastSeenEpochMs DESC")
    suspend fun getAll(): List<AccessPointEntity>

    @Query("SELECT * FROM access_points WHERE bssid = :bssid")
    suspend fun getByBssid(bssid: String): AccessPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AccessPointEntity>)

    @Query("DELETE FROM access_points")
    suspend fun clearAll()
}

@Dao
interface WirelessClientDao {
    @Query("SELECT * FROM wireless_clients ORDER BY lastSeenEpochMs DESC")
    fun observeAll(): Flow<List<WirelessClientEntity>>

    @Query("SELECT * FROM wireless_clients ORDER BY lastSeenEpochMs DESC")
    suspend fun getAll(): List<WirelessClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WirelessClientEntity>)

    @Query("DELETE FROM wireless_clients")
    suspend fun clearAll()
}

@Dao
interface WirelessAdapterDao {
    @Query("SELECT * FROM wireless_adapters ORDER BY lastSeenEpochMs DESC")
    fun observeAll(): Flow<List<WirelessAdapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WirelessAdapterEntity>)
}

@Dao
interface CaptureSessionDao {
    @Query("SELECT * FROM capture_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<CaptureSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CaptureSessionEntity)

    @Query("DELETE FROM capture_sessions")
    suspend fun clearAll()
}

@Dao
interface CaptureArtifactDao {
    @Query("SELECT * FROM capture_artifacts ORDER BY importedAtEpochMs DESC")
    fun observeAll(): Flow<List<CaptureArtifactEntity>>

    @Query("SELECT * FROM capture_artifacts ORDER BY importedAtEpochMs DESC")
    suspend fun getAll(): List<CaptureArtifactEntity>

    @Query("SELECT * FROM capture_artifacts WHERE artifactId = :id")
    suspend fun getById(id: String): CaptureArtifactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CaptureArtifactEntity)

    @Query("DELETE FROM capture_artifacts WHERE artifactId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM capture_artifacts")
    suspend fun clearAll()
}

@Dao
interface SecurityFindingDao {
    @Query("SELECT * FROM security_findings ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<SecurityFindingEntity>>

    @Query("SELECT * FROM security_findings ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<SecurityFindingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SecurityFindingEntity>)

    @Query("DELETE FROM security_findings")
    suspend fun clearAll()
}

@Dao
interface AuditSessionDao {
    @Query("SELECT * FROM audit_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<AuditSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AuditSessionEntity)
}

@Dao
interface LabDao {
    @Query("SELECT * FROM lab_scenarios ORDER BY createdAtEpochMs DESC")
    fun observeScenarios(): Flow<List<LabScenarioEntity>>

    @Query("SELECT * FROM lab_events ORDER BY timestampEpochMs DESC")
    fun observeEvents(): Flow<List<LabEventEntity>>

    @Query("SELECT * FROM lab_events ORDER BY timestampEpochMs DESC")
    suspend fun getAllEvents(): List<LabEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScenario(scenario: LabScenarioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<LabEventEntity>)

    @Query("DELETE FROM lab_events")
    suspend fun clearEvents()
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: ReportEntity)

    @Query("DELETE FROM reports WHERE reportId = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ApplicationSettingsDao {
    @Query("SELECT * FROM application_settings WHERE id = 1")
    fun observeSettings(): Flow<ApplicationSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ApplicationSettingsEntity)
}
