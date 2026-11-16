package com.airgorah.mobile.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        AccessPointEntity::class,
        WirelessClientEntity::class,
        WirelessAdapterEntity::class,
        CaptureSessionEntity::class,
        CaptureArtifactEntity::class,
        SecurityFindingEntity::class,
        AuditSessionEntity::class,
        LabScenarioEntity::class,
        LabEventEntity::class,
        ReportEntity::class,
        ApplicationSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AirgorahDatabase : RoomDatabase() {
    abstract fun accessPointDao(): AccessPointDao
    abstract fun wirelessClientDao(): WirelessClientDao
    abstract fun wirelessAdapterDao(): WirelessAdapterDao
    abstract fun captureSessionDao(): CaptureSessionDao
    abstract fun captureArtifactDao(): CaptureArtifactDao
    abstract fun securityFindingDao(): SecurityFindingDao
    abstract fun auditSessionDao(): AuditSessionDao
    abstract fun labDao(): LabDao
    abstract fun reportDao(): ReportDao
    abstract fun applicationSettingsDao(): ApplicationSettingsDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AirgorahDatabase =
        Room.databaseBuilder(
            context,
            AirgorahDatabase::class.java,
            "airgorah_mobile.db"
        ).fallbackToDestructiveMigration().build()

    @Provides fun provideAccessPointDao(db: AirgorahDatabase): AccessPointDao = db.accessPointDao()
    @Provides fun provideWirelessClientDao(db: AirgorahDatabase): WirelessClientDao = db.wirelessClientDao()
    @Provides fun provideWirelessAdapterDao(db: AirgorahDatabase): WirelessAdapterDao = db.wirelessAdapterDao()
    @Provides fun provideCaptureSessionDao(db: AirgorahDatabase): CaptureSessionDao = db.captureSessionDao()
    @Provides fun provideCaptureArtifactDao(db: AirgorahDatabase): CaptureArtifactDao = db.captureArtifactDao()
    @Provides fun provideSecurityFindingDao(db: AirgorahDatabase): SecurityFindingDao = db.securityFindingDao()
    @Provides fun provideAuditSessionDao(db: AirgorahDatabase): AuditSessionDao = db.auditSessionDao()
    @Provides fun provideLabDao(db: AirgorahDatabase): LabDao = db.labDao()
    @Provides fun provideReportDao(db: AirgorahDatabase): ReportDao = db.reportDao()
    @Provides fun provideApplicationSettingsDao(db: AirgorahDatabase): ApplicationSettingsDao = db.applicationSettingsDao()
}
