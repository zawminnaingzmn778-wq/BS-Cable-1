package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppSettingsStore
import com.example.data.repository.CableCatalogRepository
import com.example.data.repository.EngineeringProjectRepository
import com.example.data.repository.ReportRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)

    val projectRepository = EngineeringProjectRepository(db.projectDao())
    val cableRepository = CableCatalogRepository(db.cableCatalogDao())
    val reportRepository = ReportRepository(db.savedReportDao())
    val settingsStore = AppSettingsStore(context)
}
