package com.airgorah.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airgorah.mobile.ui.MainViewModel
import com.airgorah.mobile.ui.screens.CapturesScreen
import com.airgorah.mobile.ui.screens.ClientsScreen
import com.airgorah.mobile.ui.screens.DashboardScreen
import com.airgorah.mobile.ui.screens.HardwareScreen
import com.airgorah.mobile.ui.screens.MonitoringScreen
import com.airgorah.mobile.ui.screens.NetworksScreen
import com.airgorah.mobile.ui.screens.ReportsScreen
import com.airgorah.mobile.ui.screens.SecurityLabScreen
import com.airgorah.mobile.ui.screens.SettingsScreen
import com.airgorah.mobile.ui.theme.AirgorahTheme
import dagger.hilt.android.AndroidEntryPoint

enum class AirgorahDestination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    NETWORKS("networks", "Networks", Icons.Default.Wifi),
    CLIENTS("clients", "Clients", Icons.Default.Devices),
    MONITORING("monitoring", "Monitor", Icons.Default.Radar),
    CAPTURES("captures", "Captures", Icons.Default.FolderOpen),
    SECURITY_LAB("security_lab", "Lab", Icons.Default.Security),
    HARDWARE("hardware", "Hardware", Icons.Default.Memory),
    REPORTS("reports", "Reports", Icons.Default.Assessment),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirgorahTheme {
                AirgorahAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirgorahAppRoot(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: AirgorahDestination.DASHBOARD.route
    val snackbarHostState = remember { SnackbarHostState() }

    val scanState by viewModel.scanState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val adapters by viewModel.adapters.collectAsState()
    val selectedAdapterId by viewModel.selectedAdapterId.collectAsState()
    val lastUsbEvent by viewModel.lastUsbEvent.collectAsState()
    val accessPoints by viewModel.accessPoints.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val artifacts by viewModel.captureArtifacts.collectAsState()
    val sessions by viewModel.captureSessions.collectAsState()
    val findings by viewModel.findings.collectAsState()
    val labEvents by viewModel.labEvents.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val protocolFilter by viewModel.protocolFilter.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val selectedAp by viewModel.selectedAp.collectAsState()
    val passwordAudit by viewModel.passwordAudit.collectAsState()
    val channelFilterValid by viewModel.channelFilterValid.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionsAndHardware()
    }

    val pcapPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPcapUri(uri)
        }
    }

    LaunchedEffect(actionFeedback) {
        actionFeedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val primaryBottom = listOf(
        AirgorahDestination.DASHBOARD,
        AirgorahDestination.NETWORKS,
        AirgorahDestination.CAPTURES,
        AirgorahDestination.SECURITY_LAB,
        AirgorahDestination.HARDWARE
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        val dest = AirgorahDestination.entries.firstOrNull { it.route == currentRoute }
                        Text("Airgorah • ${dest?.label ?: "Dashboard"}")
                    }
                )
                // Compact secondary navigation strip so all 9 destinations are 1-tap accessible
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AirgorahDestination.entries.forEach { dest ->
                        FilterChip(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                primaryBottom.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = AirgorahDestination.DASHBOARD.route
            ) {
                composable(AirgorahDestination.DASHBOARD.route) {
                    DashboardScreen(
                        scanState = scanState,
                        statusMessage = statusMessage,
                        permissionStatus = permissionStatus,
                        adapters = adapters,
                        accessPoints = accessPoints,
                        clients = clients,
                        artifacts = artifacts,
                        findings = findings,
                        nativeDiagnostic = viewModel.nativeDiagnostic,
                        onRequestPermissions = {
                            permissionLauncher.launch(permissionStatus.requiredWifiScanPermissions.toTypedArray())
                        },
                        onTriggerScan = { viewModel.triggerWifiScan() },
                        onOpenSecurityLab = {
                            navController.navigate(AirgorahDestination.SECURITY_LAB.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(AirgorahDestination.NETWORKS.route) {
                    NetworksScreen(
                        accessPoints = accessPoints,
                        searchQuery = searchQuery,
                        protocolFilter = protocolFilter,
                        sortField = sortField,
                        selectedAp = selectedAp,
                        onSearchChange = viewModel::setSearchQuery,
                        onProtocolFilterChange = viewModel::setProtocolFilter,
                        onSortChange = viewModel::setSortField,
                        onRefresh = viewModel::triggerWifiScan,
                        onSelectAp = viewModel::selectAp
                    )
                }
                composable(AirgorahDestination.CLIENTS.route) {
                    ClientsScreen(clients = clients)
                }
                composable(AirgorahDestination.MONITORING.route) {
                    MonitoringScreen(
                        adapters = adapters,
                        selectedAdapterId = selectedAdapterId,
                        sessions = sessions,
                        artifacts = artifacts,
                        onSelectAdapter = viewModel::selectAdapter
                    )
                }
                composable(AirgorahDestination.CAPTURES.route) {
                    CapturesScreen(
                        artifacts = artifacts,
                        onImportPcap = { pcapPickerLauncher.launch(arrayOf("*/*")) },
                        onDeleteArtifact = viewModel::deleteArtifact
                    )
                }
                composable(AirgorahDestination.SECURITY_LAB.route) {
                    SecurityLabScreen(
                        artifacts = artifacts,
                        parseArtifact = viewModel::parseArtifactAnalysis,
                        labEvents = labEvents,
                        passwordAudit = passwordAudit,
                        onRunDeauthSim = viewModel::runDeauthSimulation,
                        onClearLabEvents = viewModel::clearLabEvents,
                        onEvaluatePassword = viewModel::evaluatePassword,
                        onClearPasswordAudit = viewModel::clearPasswordAudit
                    )
                }
                composable(AirgorahDestination.HARDWARE.route) {
                    HardwareScreen(
                        adapters = adapters,
                        lastUsbEvent = lastUsbEvent,
                        onRefresh = viewModel::refreshPermissionsAndHardware,
                        onRequestUsbPermission = viewModel::requestUsbPermission
                    )
                }
                composable(AirgorahDestination.REPORTS.route) {
                    ReportsScreen(
                        reports = reports,
                        onGenerateReport = viewModel::generateReport,
                        onDeleteReport = viewModel::deleteReport
                    )
                }
                composable(AirgorahDestination.SETTINGS.route) {
                    SettingsScreen(
                        settings = settings,
                        channelFilterValid = channelFilterValid,
                        nativeDiagnostic = viewModel.nativeDiagnostic,
                        onUpdateSettings = viewModel::updateSettings,
                        onClearAllData = viewModel::clearAllStoredData
                    )
                }
            }
        }
    }
}
