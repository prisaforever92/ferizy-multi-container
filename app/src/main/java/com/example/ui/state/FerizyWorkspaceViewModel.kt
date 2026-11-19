package com.example.ui.state

import androidx.lifecycle.ViewModel
import com.example.data.RoadmapRepository
import com.example.domain.AccountContainerConfig
import com.example.domain.AdvancedStopMode
import com.example.domain.AssetBlockerMode
import com.example.domain.AutoKlikExtensionSettings
import com.example.domain.AutoKlikJsonSerializer
import com.example.domain.AutoKlikPoint
import com.example.domain.AutoKlikPointType
import com.example.domain.AutoWakeMode
import com.example.domain.ElectronProjectGenerator
import com.example.domain.EnduranceAuditResult
import com.example.domain.GeneratedElectronFile
import com.example.domain.GeoProviderMode
import com.example.domain.GlobalRuntimeConfig
import com.example.domain.LogRotationPreset
import com.example.domain.OptionalFeatureOption
import com.example.domain.PortGeoPreset
import com.example.domain.ProxyConfig
import com.example.domain.ProxyProtocol
import com.example.domain.RoadmapPhaseInfo
import com.example.domain.ServiceFilterType
import com.example.domain.SimulatedTab
import com.example.domain.TabRole
import com.example.domain.TabRunnerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class WorkspaceTab {
  PREFLIGHT,
  CODE_ZIP,
  AUTOKLIK,
  ROADMAP,
}

data class FerizyWorkspaceUiState(
  val currentTab: WorkspaceTab = WorkspaceTab.PREFLIGHT,
  val accounts: List<AccountContainerConfig> = (1..5).map { AccountContainerConfig(accountId = it) },
  val globalConfig: GlobalRuntimeConfig = GlobalRuntimeConfig(),
  val selectedAccountIdForPoints: Int = 1,
  val copiedPointsBuffer: List<AutoKlikPoint>? = null,
  val generatedFiles: List<GeneratedElectronFile> = emptyList(),
  val selectedFilePath: String = "src/main/notificationCenter.js",
  val editingAccountId: Int? = null,
  val statusMessage: String? = null,
  val roadmapPhases: List<RoadmapPhaseInfo> = RoadmapRepository.phases,
  val optionalModules: List<OptionalFeatureOption> = RoadmapRepository.optionalModules,
  val reviewedOptionalIds: Set<String> = setOf("keep_alive", "captcha_autopop", "smart_matcher"),
  val enduranceAudit: EnduranceAuditResult? = null,
) {
  val totalTabs: Int
    get() = accounts.sumOf { it.tabs.size }

  val totalWonTabs: Int
    get() = accounts.sumOf { acc -> acc.tabs.count { it.runnerState == TabRunnerState.WON_STOPPED } }

  val totalRunningTabs: Int
    get() = accounts.sumOf { acc -> acc.tabs.count { it.runnerState == TabRunnerState.RUNNING } }

  val estimatedRamGb: Double
    get() {
      val factor =
        when (globalConfig.assetBlockerMode) {
          AssetBlockerMode.AGGRESSIVE -> 0.12
          AssetBlockerMode.SELECTIVE -> 0.14
          AssetBlockerMode.DISABLED -> 0.21
        }
      return 0.55 + (totalTabs * factor)
    }

  val selectedFile: GeneratedElectronFile?
    get() = generatedFiles.find { it.relativePath == selectedFilePath } ?: generatedFiles.firstOrNull()

  val selectedPointsAccount: AccountContainerConfig
    get() = accounts.find { it.accountId == selectedAccountIdForPoints } ?: accounts.first()

  val autoklikPreviewJson: String
    get() = AutoKlikJsonSerializer.buildFullExportJson(accounts, globalConfig)
}

class FerizyWorkspaceViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(createInitialState())
  val uiState: StateFlow<FerizyWorkspaceUiState> = _uiState.asStateFlow()

  private fun createInitialState(): FerizyWorkspaceUiState {
    val accounts = (1..5).map { AccountContainerConfig(accountId = it) }
    val global = GlobalRuntimeConfig()
    val files = ElectronProjectGenerator.generateProjectFiles(accounts, global)
    val initial = FerizyWorkspaceUiState(
      accounts = accounts,
      globalConfig = global,
      generatedFiles = files,
    )
    return initial.copy(enduranceAudit = computeEnduranceAudit(accounts, global))
  }

  private fun rebuildFiles(state: FerizyWorkspaceUiState): FerizyWorkspaceUiState {
    val files = ElectronProjectGenerator.generateProjectFiles(state.accounts, state.globalConfig)
    return state.copy(
      generatedFiles = files,
      enduranceAudit = computeEnduranceAudit(state.accounts, state.globalConfig),
    )
  }

  private fun computeEnduranceAudit(
    accounts: List<AccountContainerConfig>,
    globalConfig: GlobalRuntimeConfig,
  ): EnduranceAuditResult {
    val simulatedTabs = maxOf(15, accounts.sumOf { it.tabs.size })
    val perTabCapMb =
      when (globalConfig.assetBlockerMode) {
        AssetBlockerMode.AGGRESSIVE -> globalConfig.watchdogCalibrationTargetMb - 30
        AssetBlockerMode.SELECTIVE -> globalConfig.watchdogCalibrationTargetMb
        AssetBlockerMode.DISABLED -> globalConfig.watchdogCalibrationTargetMb + 90
      }
    val peakRamGb = ((550.0 + (simulatedTabs * perTabCapMb)) / 1024.0)
    val roundedPeakGb = (Math.round(peakRamGb * 100.0) / 100.0)
    val triggerTabs =
      accounts.sumOf { acc ->
        acc.tabs.count { it.runnerState == TabRunnerState.IDLE && it.memoryMb > globalConfig.watchdogCalibrationTargetMb }
      }
    val maxLogBudgetMb = globalConfig.logMaxSizeMb * 5
    val projected24hLogMb = minOf(maxLogBudgetMb, if (globalConfig.enduranceHealthReportEnabled) 18 else 10)
    val passed = roundedPeakGb < 12.0 && maxLogBudgetMb <= 250
    return EnduranceAuditResult(
      simulatedTabs = simulatedTabs,
      peakEstimatedRamGb = roundedPeakGb,
      ramBudgetGb = 16.0,
      watchdogTriggerTabCount = triggerTabs,
      projected24hLogMb = projected24hLogMb,
      maxLogBudgetMb = maxLogBudgetMb,
      passed = passed,
      summary =
        "Simulasi 24J ($simulatedTabs tab): Puncak ${roundedPeakGb} GB / 16 GB RAM • Log ~${projected24hLogMb} MB (Batas ${globalConfig.logMaxSizeMb} MB / ${globalConfig.logRetentionDays} hari)",
    )
  }

  fun selectTab(tab: WorkspaceTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  fun clearStatusMessage() {
    _uiState.update { it.copy(statusMessage = null) }
  }

  fun showStatusMessage(msg: String) {
    _uiState.update { it.copy(statusMessage = msg) }
  }

  fun addAccountContainer() {
    _uiState.update { state ->
      if (state.accounts.size >= 12) {
        return@update state.copy(statusMessage = "Batas aman maksimal 12 akun (36 tab) untuk menjaga stabilitas RAM.")
      }
      val nextId = (state.accounts.maxOfOrNull { it.accountId } ?: 0) + 1
      val updatedAccounts = state.accounts + AccountContainerConfig(accountId = nextId)
      val nextGlobal = state.globalConfig.copy(maxAccounts = updatedAccounts.size)
      rebuildFiles(
        state.copy(
          accounts = updatedAccounts,
          globalConfig = nextGlobal,
          statusMessage = "Akun $nextId (persist:akun_$nextId) ditambahkan. Total ${updatedAccounts.size} akun siap login.",
        )
      )
    }
  }

  fun removeAccountContainer(accountId: Int) {
    _uiState.update { state ->
      if (state.accounts.size <= 1) return@update state
      val updatedAccounts = state.accounts.filterNot { it.accountId == accountId }
      val nextGlobal = state.globalConfig.copy(maxAccounts = updatedAccounts.size)
      val nextSelectedPoints =
        if (state.selectedAccountIdForPoints == accountId) updatedAccounts.first().accountId
        else state.selectedAccountIdForPoints
      rebuildFiles(
        state.copy(
          accounts = updatedAccounts,
          globalConfig = nextGlobal,
          selectedAccountIdForPoints = nextSelectedPoints,
          statusMessage = "Akun $accountId dihapus dari workspace (${updatedAccounts.size} akun aktif).",
        )
      )
    }
  }

  fun updateDisplayAndZoomSettings(
    zoomPercent: Int,
    splitCount: Int,
    compactHeaderMode: Boolean,
  ) {
    _uiState.update { state ->
      val nextGlobal =
        state.globalConfig.copy(
          defaultZoomPercent = zoomPercent.coerceIn(50, 150),
          defaultSplitViewCount = splitCount.coerceIn(1, 3),
          compactHeaderMode = compactHeaderMode,
        )
      rebuildFiles(
        state.copy(
          globalConfig = nextGlobal,
          statusMessage = "Tampilan diperbarui: Zoom ${nextGlobal.defaultZoomPercent}% • Split ${nextGlobal.defaultSplitViewCount} Layar.",
        )
      )
    }
  }

  fun toggleTab1MasterMirror() {
    _uiState.update { state ->
      val nextVal = !state.globalConfig.tab1MasterMirrorEnabled
      val nextGlobal = state.globalConfig.copy(tab1MasterMirrorEnabled = nextVal)
      rebuildFiles(
        state.copy(
          globalConfig = nextGlobal,
          statusMessage =
            if (nextVal) "Mirror Tab 1: ON (Semua Tab 1 mengikuti gerakan Master Tab 1; otomatis OFF saat klik Tab 2/3)."
            else "Mirror Tab 1: OFF (Seluruh Tab 1/2/3 beroperasi mandiri).",
        )
      )
    }
  }

  fun addTabToAccount(accountId: Int) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) {
            acc
          } else if (!acc.canAddTab) {
            return@update state.copy(
              statusMessage = "Hard Cap 3/3 tercapai untuk ${acc.name}. Tombol + Tab dikunci."
            )
          } else {
            val nextIdx = acc.tabs.size + 1
            val role =
              when (nextIdx) {
                2 -> TabRole.STRIKER_MAIN
                else -> TabRole.STRIKER_BACKUP
              }
            acc.copy(
              tabs =
                acc.tabs +
                  SimulatedTab(
                    tabId = "akun_${accountId}_tab_$nextIdx",
                    tabIndex = nextIdx,
                    title = "${role.label} • Akun $accountId",
                    url = if (nextIdx <= 1) "https://trip.ferizy.com" else "https://trip.ferizy.com/order",
                    role = role,
                    memoryMb = 140 + (accountId * 5) + (nextIdx * 8),
                  )
            )
          }
        }
      rebuildFiles(state.copy(accounts = updated))
    }
  }

  fun removeTabFromAccount(accountId: Int, tabId: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId || acc.tabs.size <= 1) {
            acc
          } else {
            val remaining =
              acc.tabs.filterNot { it.tabId == tabId }.mapIndexed { index, tab ->
                tab.copy(tabIndex = index + 1)
              }
            acc.copy(tabs = remaining)
          }
        }
      rebuildFiles(state.copy(accounts = updated))
    }
  }

  fun toggleTabLoginSimulated(accountId: Int, tabId: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            acc.copy(
              tabs =
                acc.tabs.map { tab ->
                  if (tab.tabId == tabId) tab.copy(isLoggedIn = !tab.isLoggedIn) else tab
                }
            )
          }
        }
      state.copy(accounts = updated)
    }
  }

  fun toggleTabRunner(accountId: Int, tabId: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            acc.copy(
              tabs =
                acc.tabs.map { tab ->
                  if (tab.tabId != tabId) tab
                  else {
                    val nextState =
                      when (tab.runnerState) {
                        TabRunnerState.IDLE -> TabRunnerState.RUNNING
                        TabRunnerState.RUNNING -> TabRunnerState.IDLE
                        TabRunnerState.WON_STOPPED,
                        TabRunnerState.PAUSED_CAPTCHA,
                        TabRunnerState.LOGOUT_STOPPED -> TabRunnerState.IDLE
                      }
                    tab.copy(
                      runnerState = nextState,
                      wonUrl = if (nextState == TabRunnerState.IDLE) null else tab.wonUrl,
                      url = if (nextState == TabRunnerState.IDLE && tab.wonUrl != null) "https://trip.ferizy.com" else tab.url,
                    )
                  }
                }
            )
          }
        }
      state.copy(accounts = updated)
    }
  }

  // Isolasi Ketat Per-Tab (default) atau Stop Lanjutan sesuai konfigurasi Fase 3
  fun simulateTabWin(accountId: Int, tabId: String) {
    _uiState.update { state ->
      val winPath = "https://trip.ferizy.com/order/verification"
      val stopMode = state.globalConfig.advancedStopMode
      val updated =
        state.accounts.map { acc ->
          acc.copy(
            tabs =
              acc.tabs.map { tab ->
                when {
                  acc.accountId == accountId && tab.tabId == tabId -> {
                    tab.copy(
                      runnerState = TabRunnerState.WON_STOPPED,
                      wonUrl = winPath,
                      url = winPath,
                    )
                  }
                  stopMode == AdvancedStopMode.STOP_ALL_ACCOUNTS &&
                    tab.runnerState == TabRunnerState.RUNNING -> {
                    tab.copy(runnerState = TabRunnerState.IDLE)
                  }
                  stopMode == AdvancedStopMode.STOP_SAME_ACCOUNT &&
                    acc.accountId == accountId &&
                    tab.runnerState == TabRunnerState.RUNNING -> {
                    tab.copy(runnerState = TabRunnerState.IDLE)
                  }
                  else -> tab
                }
              }
          )
        }
      state.copy(
        accounts = updated,
        statusMessage = "Tembus /order/verification ($tabId) • Mode: ${stopMode.label}",
      )
    }
  }

  fun simulateTabCaptcha(accountId: Int, tabId: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            acc.copy(
              tabs =
                acc.tabs.map { tab ->
                  if (tab.tabId == tabId) {
                    tab.copy(runnerState = TabRunnerState.PAUSED_CAPTCHA)
                  } else {
                    tab
                  }
                }
            )
          }
        }
      val autoPopText = if (state.globalConfig.captchaAutoPopEnabled) " + Auto-Pop Tray" else ""
      state.copy(
        accounts = updated,
        statusMessage = "CAPTCHA di $tabId: tab di-pause$autoPopText + notif ntfy.sh.",
      )
    }
  }

  fun simulateTabLogout(accountId: Int, tabId: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            acc.copy(
              tabs =
                acc.tabs.map { tab ->
                  if (tab.tabId == tabId) {
                    tab.copy(
                      runnerState = TabRunnerState.LOGOUT_STOPPED,
                      isLoggedIn = false,
                      url = "https://trip.ferizy.com/login",
                    )
                  } else {
                    tab
                  }
                }
            )
          }
        }
      state.copy(
        accounts = updated,
        statusMessage = "Logout / HTTP 401 di $tabId: runner berhenti & alarm notif aktif.",
      )
    }
  }

  fun openAccountEditor(accountId: Int) {
    _uiState.update { it.copy(editingAccountId = accountId) }
  }

  fun closeAccountEditor() {
    _uiState.update { it.copy(editingAccountId = null) }
  }

  fun updateAccountProxyAndGeo(
    accountId: Int,
    protocol: ProxyProtocol,
    host: String,
    port: Int,
    username: String,
    password: String,
    geoMode: GeoProviderMode,
    geoPreset: PortGeoPreset,
    customLat: Double,
    customLng: Double,
  ) {
    _uiState.update { state ->
      val cleanHost = host.trim()
      val ipLabel =
        if (protocol == ProxyProtocol.DIRECT || cleanHost.isBlank()) "Direct ISP"
        else "$cleanHost:$port"
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            acc.copy(
              proxy =
                ProxyConfig(
                  protocol = protocol,
                  host = cleanHost,
                  port = port.coerceIn(1, 65535),
                  username = username.trim(),
                  password = password,
                  ipLabel = ipLabel,
                ),
              geoMode = geoMode,
              geoPreset = geoPreset,
              customLat = customLat,
              customLng = customLng,
            )
          }
        }
      rebuildFiles(
        state.copy(
          accounts = updated,
          editingAccountId = null,
          statusMessage = "Konfigurasi Akun $accountId diperbarui & disinkronkan ke Electron ZIP.",
        )
      )
    }
  }

  fun updateGlobalSettings(
    staggeredBootMs: Int,
    ntfyTopic: String,
    closeToTray: Boolean,
    powerSaveBlocker: Boolean,
    assetBlockerMode: AssetBlockerMode,
    memoryWatchdogLimitMb: Int,
    staggeredReloadDelayMs: Int,
    lazyAntiRevertEnabled: Boolean,
  ) {
    _uiState.update { state ->
      val nextGlobal =
        state.globalConfig.copy(
          staggeredBootMs = staggeredBootMs.coerceIn(250, 400),
          ntfyTopic = ntfyTopic.trim().ifBlank { "ferizy_Transsamudra05" },
          closeToTray = closeToTray,
          powerSaveBlocker = powerSaveBlocker,
          assetBlockerMode = assetBlockerMode,
          memoryWatchdogLimitMb = memoryWatchdogLimitMb.coerceIn(250, 600),
          staggeredReloadDelayMs = staggeredReloadDelayMs.coerceIn(250, 1000),
          lazyAntiRevertEnabled = lazyAntiRevertEnabled,
        )
      rebuildFiles(
        state.copy(
          globalConfig = nextGlobal,
          statusMessage = "Parameter Fase 1–3 diperbarui di paket Electron.",
        )
      )
    }
  }

  fun updateFase3Checklist(
    gentleKeepAliveEnabled: Boolean,
    captchaAutoPopEnabled: Boolean,
    smartTargetMatcherEnabled: Boolean,
    priorityHoursCsv: String,
    latencyCompensationEnabled: Boolean,
    advancedStopMode: AdvancedStopMode,
  ) {
    _uiState.update { state ->
      val cleanHours = priorityHoursCsv.trim().ifBlank { "08:00,10:00,12:00,14:00,20:00" }
      val nextGlobal =
        state.globalConfig.copy(
          gentleKeepAliveEnabled = gentleKeepAliveEnabled,
          captchaAutoPopEnabled = captchaAutoPopEnabled,
          smartTargetMatcherEnabled = smartTargetMatcherEnabled,
          priorityHoursCsv = cleanHours,
          latencyCompensationEnabled = latencyCompensationEnabled,
          advancedStopMode = advancedStopMode,
        )
      rebuildFiles(
        state.copy(
          globalConfig = nextGlobal,
          statusMessage = "Ceklist 5 Modul Opsional Fase 3 disinkronkan ke Electron ZIP.",
        )
      )
    }
  }

  fun updateFase3Config(nextConfig: GlobalRuntimeConfig) {
    _uiState.update { state ->
      val sanitized =
        nextConfig.copy(
          priorityHoursCsv = nextConfig.priorityHoursCsv.trim().ifBlank { "08:00,10:00,12:00,14:00,20:00" },
          ntfyServerUrl = nextConfig.ntfyServerUrl.trim().ifBlank { "https://ntfy.sh" },
          scheduledShotTimeWib = nextConfig.scheduledShotTimeWib.trim().ifBlank { "00:00:00" },
        )
      rebuildFiles(
        state.copy(
          globalConfig = sanitized,
          statusMessage = "Penyesuaian opsi Fase 3 disinkronkan ke Electron ZIP.",
        )
      )
    }
  }

  fun resumeAllPausedCaptchaTabs() {
    _uiState.update { state ->
      var count = 0
      val updated =
        state.accounts.map { acc ->
          acc.copy(
            tabs =
              acc.tabs.map { tab ->
                if (tab.runnerState == TabRunnerState.PAUSED_CAPTCHA) {
                  count += 1
                  tab.copy(runnerState = TabRunnerState.RUNNING)
                } else {
                  tab
                }
              }
          )
        }
      state.copy(
        accounts = updated,
        statusMessage =
          if (count > 0) "$count tab CAPTCHA dilanjutkan kembali ke mode Menembak."
          else "Tidak ada tab yang sedang di-pause karena CAPTCHA.",
      )
    }
  }

  fun updateFase4Config(
    preset: LogRotationPreset,
    watchdogTargetMb: Int,
    healthReportEnabled: Boolean,
    snapshotIntervalMinutes: Int,
  ) {
    _uiState.update { state ->
      val clampedWatchdog = watchdogTargetMb.coerceIn(250, 550)
      val clampedInterval = snapshotIntervalMinutes.coerceIn(15, 240)
      val nextGlobal =
        state.globalConfig.copy(
          logRotationPreset = preset,
          logMaxSizeMb = preset.maxSizeMb,
          logRetentionDays = preset.retentionDays,
          watchdogCalibrationTargetMb = clampedWatchdog,
          memoryWatchdogLimitMb = clampedWatchdog,
          enduranceHealthReportEnabled = healthReportEnabled,
          healthSnapshotIntervalMinutes = clampedInterval,
        )
      rebuildFiles(
        state.copy(
          globalConfig = nextGlobal,
          statusMessage = "Konfigurasi Fase 4 (${preset.label}, Watchdog ${clampedWatchdog}MB) disinkronkan.",
        )
      )
    }
  }

  fun runEnduranceAuditSimulation() {
    _uiState.update { state ->
      val audit = computeEnduranceAudit(state.accounts, state.globalConfig)
      state.copy(
        enduranceAudit = audit,
        statusMessage =
          if (audit.passed) "LULUS Uji Ketahanan 24J: ${audit.peakEstimatedRamGb} GB / 16 GB RAM • Log terkendali ${audit.projected24hLogMb} MB."
          else "Peringatan beban RAM ${audit.peakEstimatedRamGb} GB mendekati batas.",
      )
    }
  }

  fun selectGeneratedFile(relativePath: String) {
    _uiState.update { it.copy(selectedFilePath = relativePath) }
  }

  fun selectPointsAccount(accountId: Int) {
    _uiState.update { it.copy(selectedAccountIdForPoints = accountId) }
  }

  fun addAutoKlikPoint(
    accountId: Int,
    type: AutoKlikPointType,
    label: String,
    x: Int,
    y: Int,
    endX: Int?,
    endY: Int?,
    selector: String,
    inputValue: String,
    popupKey: String,
    delayAfterMs: Int,
  ) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            val nextOrder = acc.points.size + 1
            val newPoint =
              AutoKlikPoint(
                id = "pt_${accountId}_${System.currentTimeMillis() % 100000}",
                order = nextOrder,
                type = type,
                label = label.ifBlank { "${type.label} #$nextOrder" },
                x = x,
                y = y,
                endX = endX,
                endY = endY,
                selector = selector.trim(),
                inputValue = inputValue,
                popupKey = popupKey.ifBlank { "Enter" },
                delayAfterMs = delayAfterMs.coerceAtLeast(0),
                antiRevert = (type == AutoKlikPointType.INPUT),
              )
            acc.copy(points = acc.points + newPoint)
          }
        }
      rebuildFiles(
        state.copy(
          accounts = updated,
          statusMessage = "Titik ${type.label} ditambahkan ke Akun $accountId.",
        )
      )
    }
  }

  fun updateInputPointText(accountId: Int, pointId: String, newText: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            acc.copy(
              points =
                acc.points.map { pt ->
                  if (pt.id == pointId) pt.copy(inputValue = newText) else pt
                }
            )
          }
        }
      rebuildFiles(state.copy(accounts = updated))
    }
  }

  fun togglePointAntiRevert(accountId: Int, pointId: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            acc.copy(
              points =
                acc.points.map { pt ->
                  if (pt.id == pointId) pt.copy(antiRevert = !pt.antiRevert) else pt
                }
            )
          }
        }
      rebuildFiles(state.copy(accounts = updated))
    }
  }

  fun deleteAutoKlikPoint(accountId: Int, pointId: String) {
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId != accountId) acc
          else {
            val reordered =
              acc.points.filterNot { it.id == pointId }.mapIndexed { idx, pt ->
                pt.copy(order = idx + 1)
              }
            acc.copy(points = reordered)
          }
        }
      rebuildFiles(state.copy(accounts = updated))
    }
  }

  fun copyPointsFromAccount(accountId: Int) {
    _uiState.update { state ->
      val source = state.accounts.find { it.accountId == accountId } ?: return@update state
      state.copy(
        copiedPointsBuffer = source.points,
        statusMessage = "${source.points.size} titik dari Akun $accountId disalin ke clipboard profil.",
      )
    }
  }

  fun updateAccountExtensionSettings(
    accountId: Int,
    settings: AutoKlikExtensionSettings,
  ) {
    val sanitized =
      settings.copy(
        maxLoops = settings.maxLoops.coerceIn(0, 999),
        loopDelayMs = settings.loopDelayMs.coerceIn(50, 60000),
        radiusPx = settings.radiusPx.coerceIn(5, 200),
      )
    _uiState.update { state ->
      val updated =
        state.accounts.map { acc ->
          if (acc.accountId == accountId) acc.copy(extensionSettings = sanitized) else acc
        }
      rebuildFiles(
        state.copy(
          accounts = updated,
          statusMessage = "Pengaturan AutoKlik-v3.2-Mandiri-Extension Akun $accountId disimpan ke container.",
        )
      )
    }
  }

  fun importAccountJson(accountId: Int, rawJson: String) {
    val parsed = AutoKlikJsonSerializer.parseAccountProfileJson(rawJson, accountId)
    parsed.fold(
      onSuccess = { profile ->
        _uiState.update { state ->
          val updated =
            state.accounts.map { acc ->
              if (acc.accountId != accountId) acc
              else {
                acc.copy(
                  extensionSettings = profile.extensionSettings ?: acc.extensionSettings,
                  points = profile.points,
                )
              }
            }
          rebuildFiles(
            state.copy(
              accounts = updated,
              statusMessage = "${profile.points.size} titik berhasil dibuka ke penyimpanan Akun $accountId.",
            )
          )
        }
      },
      onFailure = { err ->
        showStatusMessage("Gagal membuka JSON Akun $accountId: ${err.message}")
      },
    )
  }

  fun pastePointsToAllAccounts() {
    _uiState.update { state ->
      val sourceAcc = state.selectedPointsAccount
      val buffer = state.copiedPointsBuffer ?: sourceAcc.points
      val sourceExt = sourceAcc.extensionSettings
      val updated =
        state.accounts.map { acc ->
          acc.copy(
            extensionSettings = sourceExt,
            points =
              buffer.mapIndexed { idx, pt ->
                pt.copy(id = "pt_${acc.accountId}_${idx + 1}", order = idx + 1)
              }
          )
        }
      rebuildFiles(
        state.copy(
          accounts = updated,
          statusMessage = "${buffer.size} titik & pengaturan Ext v3.2 berhasil di-Paste ke seluruh ${updated.size} akun.",
        )
      )
    }
  }

  fun toggleReviewedOptionalModule(id: String) {
    _uiState.update { state ->
      val next =
        if (id in state.reviewedOptionalIds) state.reviewedOptionalIds - id
        else state.reviewedOptionalIds + id
      state.copy(reviewedOptionalIds = next)
    }
  }

  fun renameAccount(accountId: Int, newName: String) {
    val clean = newName.trim().ifBlank { "Akun $accountId" }
    _uiState.update { state ->
      val updated = state.accounts.map { acc ->
        if (acc.accountId == accountId) acc.copy(name = clean) else acc
      }
      rebuildFiles(
        state.copy(
          accounts = updated,
          statusMessage = "Nama Akun $accountId diubah menjadi \"$clean\".",
        )
      )
    }
  }
}
