package com.airgorah.mobile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgorah.mobile.core.common.OperationState
import com.airgorah.mobile.core.database.AccessPointDao
import com.airgorah.mobile.core.database.AccessPointEntity
import com.airgorah.mobile.core.database.ApplicationSettingsDao
import com.airgorah.mobile.core.database.ApplicationSettingsEntity
import com.airgorah.mobile.core.database.CaptureArtifactDao
import com.airgorah.mobile.core.database.CaptureArtifactEntity
import com.airgorah.mobile.core.database.CaptureSessionDao
import com.airgorah.mobile.core.database.CaptureSessionEntity
import com.airgorah.mobile.core.database.LabDao
import com.airgorah.mobile.core.database.LabEventEntity
import com.airgorah.mobile.core.database.ReportDao
import com.airgorah.mobile.core.database.ReportEntity
import com.airgorah.mobile.core.database.SecurityFindingDao
import com.airgorah.mobile.core.database.SecurityFindingEntity
import com.airgorah.mobile.core.database.WirelessClientDao
import com.airgorah.mobile.core.database.WirelessClientEntity
import com.airgorah.mobile.core.hardware.HardwareCapabilityManager
import com.airgorah.mobile.core.hardware.HardwareCapabilityMatrix
import com.airgorah.mobile.core.network.CaptureImportRepository
import com.airgorah.mobile.core.network.SecurityLabRepository
import com.airgorah.mobile.core.network.WifiDiscoveryRepository
import com.airgorah.mobile.core.permissions.PermissionCoordinator
import com.airgorah.mobile.core.permissions.PermissionStatusSnapshot
import com.airgorah.mobile.core.reporting.ReportFormat
import com.airgorah.mobile.core.reporting.ReportGenerator
import com.airgorah.mobile.core.reporting.ReportType
import com.airgorah.mobile.core.rust.NativeBridge
import com.airgorah.mobile.core.rust.PasswordAuditDto
import com.airgorah.mobile.core.rust.PcapAnalysisDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class NetworkSortField { RSSI, ESSID, CHANNEL, LAST_SEEN }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val accessPointDao: AccessPointDao,
    private val clientDao: WirelessClientDao,
    private val artifactDao: CaptureArtifactDao,
    private val sessionDao: CaptureSessionDao,
    private val findingDao: SecurityFindingDao,
    private val labDao: LabDao,
    private val reportDao: ReportDao,
    private val settingsDao: ApplicationSettingsDao,
    private val hardwareManager: HardwareCapabilityManager,
    private val wifiRepo: WifiDiscoveryRepository,
    private val captureRepo: CaptureImportRepository,
    private val labRepo: SecurityLabRepository,
    private val reportGenerator: ReportGenerator,
    private val permissionCoordinator: PermissionCoordinator
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val permissionStatus: StateFlow<PermissionStatusSnapshot> = permissionCoordinator.status
    val scanState: StateFlow<OperationState> = wifiRepo.scanState
    val statusMessage: StateFlow<String> = wifiRepo.statusMessage
    val adapters: StateFlow<List<HardwareCapabilityMatrix>> = hardwareManager.adapters
    val selectedAdapterId: StateFlow<String> = hardwareManager.selectedAdapterId
    val lastUsbEvent: StateFlow<String?> = hardwareManager.lastUsbEvent

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _protocolFilter = MutableStateFlow("ALL")
    val protocolFilter: StateFlow<String> = _protocolFilter.asStateFlow()

    private val _sortField = MutableStateFlow(NetworkSortField.RSSI)
    val sortField: StateFlow<NetworkSortField> = _sortField.asStateFlow()

    private val _selectedAp = MutableStateFlow<AccessPointEntity?>(null)
    val selectedAp: StateFlow<AccessPointEntity?> = _selectedAp.asStateFlow()

    val settings: StateFlow<ApplicationSettingsEntity> = settingsDao.observeSettings()
        .combine(MutableStateFlow(ApplicationSettingsEntity())) { db, fallback -> db ?: fallback }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ApplicationSettingsEntity())

    val accessPoints: StateFlow<List<AccessPointEntity>> = combine(
        accessPointDao.observeAll(),
        _searchQuery,
        _protocolFilter,
        _sortField,
        settings
    ) { list, query, proto, sort, prefs ->
        list.filter { ap ->
            val hiddenOk = prefs.displayHiddenAps || !ap.isHidden
            val protoOk = proto == "ALL" || ap.securityProtocol.equals(proto, ignoreCase = true)
            val queryOk = query.isBlank() ||
                ap.essid.contains(query, ignoreCase = true) ||
                ap.bssid.contains(query, ignoreCase = true) ||
                ap.vendor.contains(query, ignoreCase = true)
            hiddenOk && protoOk && queryOk
        }.let { filtered ->
            when (sort) {
                NetworkSortField.RSSI -> filtered.sortedByDescending { it.rssiDbm }
                NetworkSortField.ESSID -> filtered.sortedBy { it.essid.lowercase() }
                NetworkSortField.CHANNEL -> filtered.sortedBy { it.channel }
                NetworkSortField.LAST_SEEN -> filtered.sortedByDescending { it.lastSeenEpochMs }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<WirelessClientEntity>> = clientDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val captureArtifacts: StateFlow<List<CaptureArtifactEntity>> = artifactDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val captureSessions: StateFlow<List<CaptureSessionEntity>> = sessionDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val findings: StateFlow<List<SecurityFindingEntity>> = findingDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labEvents: StateFlow<List<LabEventEntity>> = labDao.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = reportDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _passwordAudit = MutableStateFlow<PasswordAuditDto?>(null)
    val passwordAudit: StateFlow<PasswordAuditDto?> = _passwordAudit.asStateFlow()

    private val _channelFilterValid = MutableStateFlow(true)
    val channelFilterValid: StateFlow<Boolean> = _channelFilterValid.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    val nativeDiagnostic: String = if (NativeBridge.isLoaded) {
        NativeBridge.getVersion().fold(
            onSuccess = { "Rust Core ${it.version} (${it.arch}/${it.os})" },
            onFailure = { "Rust Core error: ${it.message}" }
        )
    } else {
        "Rust Core unavailable: ${NativeBridge.loadError ?: "unknown"}"
    }

    init {
        hardwareManager.registerUsbReceiver()
        wifiRepo.registerScanReceiver()
    }

    override fun onCleared() {
        hardwareManager.unregisterUsbReceiver()
        wifiRepo.unregisterScanReceiver()
        super.onCleared()
    }

    fun refreshPermissionsAndHardware() {
        permissionCoordinator.refresh()
        hardwareManager.refreshHardware()
    }

    fun triggerWifiScan() {
        wifiRepo.triggerScan()
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setProtocolFilter(p: String) { _protocolFilter.value = p }
    fun setSortField(s: NetworkSortField) { _sortField.value = s }
    fun selectAp(ap: AccessPointEntity?) { _selectedAp.value = ap }
    fun selectAdapter(id: String) { hardwareManager.selectAdapter(id) }
    fun clearFeedback() { _actionFeedback.value = null }

    fun requestUsbPermission(vid: Int, pid: Int) {
        val ok = hardwareManager.requestUsbPermission(vid, pid)
        _actionFeedback.value = if (ok) "Requested USB permission..." else "USB device not found."
    }

    fun importPcapUri(uri: Uri) {
        viewModelScope.launch {
            captureRepo.importPcapUri(uri)
                .onSuccess { _actionFeedback.value = "Imported ${it.fileName}: ${it.validationStatus}" }
                .onFailure { _actionFeedback.value = "Import failed: ${it.message}" }
        }
    }

    fun deleteArtifact(id: String) {
        viewModelScope.launch {
            captureRepo.deleteArtifact(id)
            _actionFeedback.value = "Capture artifact deleted."
        }
    }

    fun parseArtifactAnalysis(artifact: CaptureArtifactEntity): PcapAnalysisDto? =
        runCatching { json.decodeFromString<PcapAnalysisDto>(artifact.analysisJson) }.getOrNull()

    fun runDeauthSimulation(pmfEnabled: Boolean) {
        viewModelScope.launch {
            labRepo.runDeauthSimulation(pmfEnabled)
            _actionFeedback.value = if (pmfEnabled) {
                "Simulated PMF-protected deauth defense completed."
            } else {
                "Simulated unprotected deauth detection completed."
            }
        }
    }

    fun clearLabEvents() {
        viewModelScope.launch { labRepo.clearSimulationEvents() }
    }

    fun evaluatePassword(chars: CharArray) {
        val result = NativeBridge.auditPasswordStrength(chars)
        chars.fill('\u0000')
        result
            .onSuccess { _passwordAudit.value = it }
            .onFailure { _actionFeedback.value = "Password audit failed: ${it.message}" }
    }

    fun clearPasswordAudit() {
        _passwordAudit.value = null
    }

    fun generateReport(type: ReportType, format: ReportFormat) {
        viewModelScope.launch {
            val extra = if (type == ReportType.PASSWORD_SECURITY_ASSESSMENT) {
                _passwordAudit.value?.let {
                    "Last evaluated passphrase length=${it.length}, rating=${it.rating}, entropy=${it.estimatedEntropyBits} bits. Plaintext never stored."
                }
            } else null

            reportGenerator.generateReport(type, format, extra)
                .onSuccess { _actionFeedback.value = "Saved ${it.title} to ${it.fileUri}" }
                .onFailure { _actionFeedback.value = "Report failed: ${it.message}" }
        }
    }

    fun deleteReport(id: String) {
        viewModelScope.launch { reportDao.deleteById(id) }
    }

    fun updateSettings(updated: ApplicationSettingsEntity) {
        viewModelScope.launch {
            val valid = NativeBridge.validateChannelFilter(
                updated.channelFilter,
                updated.default24Ghz,
                updated.default5Ghz
            ).getOrDefault(true)
            _channelFilterValid.value = valid
            settingsDao.upsert(updated.copy(lastUpdatedEpochMs = System.currentTimeMillis()))
        }
    }

    fun clearAllStoredData() {
        viewModelScope.launch {
            accessPointDao.clearAll()
            clientDao.clearAll()
            artifactDao.clearAll()
            findingDao.clearAll()
            labDao.clearEvents()
            _actionFeedback.value = "All local scan, client, capture, and lab data cleared."
        }
    }
}
