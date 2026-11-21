package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppContainer
import com.example.domain.model.CableConstruction
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.SizingMode
import com.example.ui.screens.CableDatabaseScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.Page5ReferenceMethodScreen
import com.example.ui.screens.SavedReportsScreen
import com.example.ui.screens.SettingsAndQaScreen
import com.example.ui.screens.WizardShellScreen
import com.example.ui.theme.Bs7671CableSizerTheme
import com.example.ui.viewmodel.MainEngineeringViewModel

object AppRoutes {
    const val DASHBOARD = "dashboard"
    const val WORKSPACE = "workspace"
    const val DATABASE = "database"
    const val METHODS_GUIDE = "methods_guide"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer(applicationContext) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainEngineeringViewModel = viewModel(
                factory = MainEngineeringViewModel.Factory(appContainer)
            )
            val projects by viewModel.projects.collectAsStateWithLifecycle()
            val catalog by viewModel.catalogEntries.collectAsStateWithLifecycle()
            val reports by viewModel.savedReports.collectAsStateWithLifecycle()
            val settings by viewModel.appSettings.collectAsStateWithLifecycle()
            val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
            val activeSegmentIndex by viewModel.activeSegmentIndex.collectAsStateWithLifecycle()
            val calcResult by viewModel.calculationResult.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

            val darkTheme = when (settings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            Bs7671CableSizerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                var wizardPage by rememberSaveable { mutableIntStateOf(0) }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.DASHBOARD
                ) {
                    composable(AppRoutes.DASHBOARD) {
                        DashboardScreen(
                            projects = projects,
                            catalog = catalog,
                            settings = settings,
                            reportsCount = reports.size,
                            onNewAutoAc = {
                                viewModel.startNewProject(SizingMode.AUTOMATIC, CurrentSystemType.AC) {
                                    wizardPage = 0
                                    navController.navigate(AppRoutes.WORKSPACE)
                                }
                            },
                            onNewAutoDc = {
                                viewModel.startNewProject(SizingMode.AUTOMATIC, CurrentSystemType.DC) {
                                    wizardPage = 0
                                    navController.navigate(AppRoutes.WORKSPACE)
                                }
                            },
                            onNewManualVerification = {
                                viewModel.startNewProject(SizingMode.MANUAL, CurrentSystemType.AC) {
                                    wizardPage = 0
                                    navController.navigate(AppRoutes.WORKSPACE)
                                }
                            },
                            onOpenProject = { id ->
                                viewModel.openProject(id) {
                                    wizardPage = 7
                                    navController.navigate(AppRoutes.WORKSPACE)
                                }
                            },
                            onDeleteProject = { viewModel.deleteProject(it) },
                            onResumeActiveProject = {
                                navController.navigate(AppRoutes.WORKSPACE)
                            },
                            onOpenDatabase = { navController.navigate(AppRoutes.DATABASE) },
                            onOpenMethodsGuide = { navController.navigate(AppRoutes.METHODS_GUIDE) },
                            onOpenReports = { navController.navigate(AppRoutes.REPORTS) },
                            onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) }
                        )
                    }

                    composable(AppRoutes.WORKSPACE) {
                        WizardShellScreen(
                            currentPage = wizardPage,
                            onPageSelected = { wizardPage = it.coerceIn(0, 8) },
                            onNavigateHome = { navController.popBackStack(AppRoutes.DASHBOARD, inclusive = false) },
                            viewModel = viewModel,
                            project = activeProject,
                            calcResult = calcResult,
                            activeSegmentIndex = activeSegmentIndex,
                            statusMessage = statusMessage
                        )
                    }

                    composable(AppRoutes.DATABASE) {
                        CableDatabaseScreen(
                            entries = catalog,
                            statusMessage = statusMessage,
                            onClearMessage = { viewModel.clearStatusMessage() },
                            onUpsertEntry = { viewModel.upsertCableEntry(it) },
                            onImportJsonPack = { viewModel.importCatalogPackJson(it) },
                            onExportJsonPack = { viewModel.exportCatalogPackJson() },
                            onResetBaseline = { viewModel.resetCatalogToBaseline() },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(AppRoutes.METHODS_GUIDE) {
                        BackHandler { navController.popBackStack() }
                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            topBar = {
                                TopAppBar(
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    },
                                    title = { Text("BS Reference Methods A1–G") }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                                    Page5ReferenceMethodScreen(
                                        segments = activeProject.segments,
                                        activeSegmentIndex = activeSegmentIndex,
                                        cableConstruction = CableConstruction.MULTICORE_ARMOURED_SWA,
                                        onSelectSegment = {},
                                        onSelectMethodForSegment = { _, _ -> },
                                        standaloneGuideMode = true
                                    )
                                }
                            }
                        }
                    }

                    composable(AppRoutes.REPORTS) {
                        SavedReportsScreen(
                            reports = reports,
                            onOpenReportSnapshot = { rep ->
                                viewModel.openProjectFromJson(rep.projectSnapshotJson) {
                                    wizardPage = 8
                                    navController.navigate(AppRoutes.WORKSPACE)
                                }
                            },
                            onDeleteReport = { viewModel.deleteReport(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(AppRoutes.SETTINGS) {
                        SettingsAndQaScreen(
                            settings = settings,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onRunQaSuite = { viewModel.runEngineeringQa() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
