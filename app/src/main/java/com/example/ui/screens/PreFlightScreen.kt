package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AccountContainerConfig
import com.example.domain.AdvancedStopMode
import com.example.domain.AssetBlockerMode
import com.example.domain.AutoWakeMode
import com.example.domain.GeoProviderMode
import com.example.domain.GlobalRuntimeConfig
import com.example.domain.LogRotationPreset
import com.example.domain.ServiceFilterType
import com.example.domain.TabRunnerState
import com.example.ui.components.Fase4EnduranceCard
import com.example.ui.components.HeroWorkspaceBanner
import com.example.ui.components.PillTone
import com.example.ui.components.StatusPill
import com.example.ui.state.FerizyWorkspaceUiState
import kotlin.math.roundToInt

private enum class PreFlightSectionFilter(val label: String) {
  ALL("Semua"),
  ACCOUNTS("Akun & Proxy/Geo"),
  FASE3("Opsi Fase 3"),
  FASE4("Fase 4 & Log"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreFlightScreen(
  uiState: FerizyWorkspaceUiState,
  onAddAccount: () -> Unit,
  onRemoveAccount: (Int) -> Unit,
  onAddTab: (Int) -> Unit,
  onRemoveTab: (Int, String) -> Unit,
  onToggleTabLogin: (Int, String) -> Unit,
  onToggleTabRunner: (Int, String) -> Unit,
  onSimulateTabWin: (Int, String) -> Unit,
  onSimulateTabCaptcha: (Int, String) -> Unit,
  onSimulateTabLogout: (Int, String) -> Unit,
  onEditAccount: (Int) -> Unit,
  onUpdateGlobal: (
    staggeredBootMs: Int,
    ntfyTopic: String,
    closeToTray: Boolean,
    powerSaveBlocker: Boolean,
    assetBlockerMode: AssetBlockerMode,
    memoryWatchdogLimitMb: Int,
    staggeredReloadDelayMs: Int,
    lazyAntiRevertEnabled: Boolean,
  ) -> Unit,
  onUpdateDisplayAndZoom: (zoomPercent: Int, splitViewCount: Int, compactHeaderMode: Boolean) -> Unit,
  onUpdateFase3Config: (GlobalRuntimeConfig) -> Unit,
  onResumeAllCaptcha: () -> Unit,
  onUpdateFase4Config: (LogRotationPreset, Int, Boolean, Int) -> Unit,
  onRunEnduranceAudit: () -> Unit,
  onQuickExportZip: () -> Unit,
  onSaveAccountJson: (Int) -> Unit = {},
  onImportAccountJson: (Int, String) -> Unit = { _, _ -> },
  onNotifyMessage: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showGlobalEditor by remember { mutableStateOf(false) }
  var activeFilter by remember { mutableStateOf(PreFlightSectionFilter.ALL) }
  var loadCopyModalAccountId by remember { mutableStateOf<Int?>(null) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      HeroWorkspaceBanner(
        totalTabs = uiState.totalTabs,
        accountCount = uiState.accounts.size,
        maxTabs = uiState.accounts.size * 3,
        estimatedRamGb = uiState.estimatedRamGb,
        onQuickExportZip = onQuickExportZip,
      )
    }

    item {
      var cleanedRamBonusMb by remember { mutableStateOf(0) }
      var lastCleanNotice by remember { mutableStateOf("") }
      val rawBrowserRamMb = (uiState.totalTabs * 195 + uiState.totalRunningTabs * 45 - cleanedRamBonusMb).coerceAtLeast(180)
      val estCpuPercent = (12 + uiState.totalTabs * 4 + uiState.totalRunningTabs * 9).coerceIn(8, 96)
      val pcRamUsedGb = String.format(java.util.Locale.US, "%.1f", (2.4 + rawBrowserRamMb / 1024.0))
      val isOver = estCpuPercent >= 85 || rawBrowserRamMb >= 2400
      val isWarn = !isOver && (estCpuPercent >= 65 || rawBrowserRamMb >= 1600)

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          1.dp,
          if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        ),
        modifier = Modifier.fillMaxWidth().testTag("cpu_ram_meter_card"),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "📊 Live CPU & RAM Meter (Bar Utama Kiri Atas Browser)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Tampil permanen di bar kiri atas sebelah V5.8 (tidak pernah tersembunyi walau Mode Compact aktif).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(
              onClick = {
                cleanedRamBonusMb = (cleanedRamBonusMb + 140).coerceAtMost(420)
                lastCleanNotice = "⚡ Bersih Cepat: Cache HTTP dibersihkan (login tetap aman) & Standby di-recycle (-140 MB)."
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
              ),
              modifier = Modifier.testTag("quick_clean_meter_button"),
            ) {
              Text(if (isOver) "⚡ Bersih Cepat!" else "⚡ Bersih Cepat", style = MaterialTheme.typography.labelMedium)
            }
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StatusPill(
              text = "${if (isOver) "OVER! " else ""}CPU PC: $estCpuPercent% • RAM PC: ${pcRamUsedGb}/8.0GB",
              tone = if (isOver) PillTone.ERROR else if (isWarn) PillTone.WARNING else PillTone.SUCCESS,
            )
            StatusPill(
              text = "Browser: ${rawBrowserRamMb}MB (${(estCpuPercent * 0.65).roundToInt()}%)",
              tone = PillTone.INFO,
            )
            StatusPill(
              text = "Default Jeda: 0.5s • Ketik Keyboard: 30ms/huruf • Popup: 300ms • Siklus Urut #1..N➔#1",
              tone = PillTone.SUCCESS,
            )
            StatusPill(
              text = if (uiState.globalConfig.tab1MasterMirrorEnabled) "📦 Kumpulan Tab 1, 2 & 3 • 🔗 Mirror 0ms (AutoKlik Start = Putus Mirror)" else "📦 Kumpulan Tab 1, 2 & 3 • 🔗 Mirror: OFF",
              tone = if (uiState.globalConfig.tab1MasterMirrorEnabled) PillTone.SUCCESS else PillTone.NEUTRAL,
            )
          }
          if (lastCleanNotice.isNotBlank()) {
            Text(
              text = lastCleanNotice,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }

    item {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().testTag("preflight_section_filter_row"),
      ) {
        PreFlightSectionFilter.entries.forEach { section ->
          FilterChip(
            selected = activeFilter == section,
            onClick = { activeFilter = section },
            label = { Text(section.label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("filter_chip_${section.name.lowercase()}"),
          )
        }
      }
    }

    if (activeFilter == PreFlightSectionFilter.ALL) {
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Pre-Flight Check 1 Layar (${uiState.accounts.size} Akun × 3 Tab)",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Aktif: ${uiState.totalRunningTabs} berjalan • ${uiState.totalWonTabs} menang 0ms • Isolasi Per-Tab.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              OutlinedButton(
                onClick = { showGlobalEditor = !showGlobalEditor },
                modifier = Modifier.testTag("toggle_global_settings_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = "Pengaturan Fase 1 dan 2",
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showGlobalEditor) "Tutup" else "Engine")
              }
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              StatusPill(text = "Sniffer 0ms + Event Bus <1ms", tone = PillTone.SUCCESS)
              StatusPill(
                text = "Asset Blocker: ${uiState.globalConfig.assetBlockerMode.label}",
                tone = PillTone.INFO,
              )
              StatusPill(
                text = "Watchdog: ${uiState.globalConfig.memoryWatchdogLimitMb} MB Standby",
                tone = PillTone.INFO,
              )
              StatusPill(
                text = "Queue: ${uiState.globalConfig.staggeredReloadDelayMs}ms Reaktif (Opsi A)",
                tone = PillTone.SUCCESS,
              )
              StatusPill(
                text = if (uiState.globalConfig.lazyAntiRevertEnabled) "Lazy Anti-Revert: ON" else "Lazy Anti-Revert: OFF",
                tone = if (uiState.globalConfig.lazyAntiRevertEnabled) PillTone.SUCCESS else PillTone.WARNING,
              )
              StatusPill(
                text = "ntfy: ${uiState.globalConfig.ntfyTopic}",
                tone = PillTone.WARNING,
              )
            }

            if (showGlobalEditor) {
              GlobalRuntimeEditor(
                staggeredBootMs = uiState.globalConfig.staggeredBootMs,
                ntfyTopic = uiState.globalConfig.ntfyTopic,
                closeToTray = uiState.globalConfig.closeToTray,
                powerSaveBlocker = uiState.globalConfig.powerSaveBlocker,
                assetBlockerMode = uiState.globalConfig.assetBlockerMode,
                memoryWatchdogLimitMb = uiState.globalConfig.memoryWatchdogLimitMb,
                staggeredReloadDelayMs = uiState.globalConfig.staggeredReloadDelayMs,
                lazyAntiRevertEnabled = uiState.globalConfig.lazyAntiRevertEnabled,
                onSave = onUpdateGlobal,
              )
            }
          }
        }
      }
    }

    if (activeFilter == PreFlightSectionFilter.ALL) {
      item {
        DisplayZoomSplitCard(
          globalConfig = uiState.globalConfig,
          onUpdateDisplayAndZoom = onUpdateDisplayAndZoom,
        )
      }
    }

    if (activeFilter == PreFlightSectionFilter.ALL || activeFilter == PreFlightSectionFilter.FASE3) {
      item {
        Fase3OptionalModulesCard(
          globalConfig = uiState.globalConfig,
          pausedCaptchaCount =
            uiState.accounts.sumOf { acc ->
              acc.tabs.count { it.runnerState == TabRunnerState.PAUSED_CAPTCHA }
            },
          onSaveConfig = onUpdateFase3Config,
          onResumeAllCaptcha = onResumeAllCaptcha,
        )
      }
    }

    if (activeFilter == PreFlightSectionFilter.ALL || activeFilter == PreFlightSectionFilter.FASE4) {
      item {
        Fase4EnduranceCard(
          globalConfig = uiState.globalConfig,
          enduranceAudit = uiState.enduranceAudit,
          onUpdateFase4Config = onUpdateFase4Config,
          onRunEnduranceAudit = onRunEnduranceAudit,
        )
      }
    }

    if (activeFilter == PreFlightSectionFilter.ALL || activeFilter == PreFlightSectionFilter.ACCOUNTS) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Daftar Container Akun (${uiState.accounts.size}/12 Akun)",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Bisa tambah >5 akun untuk login lebih banyak akun sekaligus.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = onAddAccount,
            enabled = uiState.accounts.size < 12,
            modifier = Modifier.testTag("add_account_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (uiState.accounts.size < 12) "Tambah Akun" else "Maks 12 Akun")
          }
        }
      }

      items(uiState.accounts, key = { it.accountId }) { account ->
        AccountPreFlightCard(
          account = account,
          ntfyTopic = uiState.globalConfig.ntfyTopic,
          onAddTab = { onAddTab(account.accountId) },
          onRemoveTab = { tabId -> onRemoveTab(account.accountId, tabId) },
          onToggleTabLogin = { tabId -> onToggleTabLogin(account.accountId, tabId) },
          onToggleTabRunner = { tabId -> onToggleTabRunner(account.accountId, tabId) },
          onSimulateTabWin = { tabId -> onSimulateTabWin(account.accountId, tabId) },
          onSimulateTabCaptcha = { tabId -> onSimulateTabCaptcha(account.accountId, tabId) },
          onSimulateTabLogout = { tabId -> onSimulateTabLogout(account.accountId, tabId) },
          onEditAccount = { onEditAccount(account.accountId) },
          onSaveConfig = { onSaveAccountJson(account.accountId) },
          onOpenLoadCopyModal = { loadCopyModalAccountId = account.accountId },
          onRemoveAccount = { onRemoveAccount(account.accountId) },
        )
      }
    }
  }

  val targetAccountForModal = uiState.accounts.find { it.accountId == loadCopyModalAccountId }
  if (targetAccountForModal != null) {
    AccountLoadCopyConfigDialog(
      targetAccount = targetAccountForModal,
      allAccounts = uiState.accounts,
      globalConfig = uiState.globalConfig,
      onDismiss = { loadCopyModalAccountId = null },
      onSaveToFile = {
        onSaveAccountJson(targetAccountForModal.accountId)
      },
      onApplyJsonToAccount = { targetAccId, rawJson ->
        onImportAccountJson(targetAccId, rawJson)
      },
      onNotifyMessage = onNotifyMessage,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisplayZoomSplitCard(
  globalConfig: GlobalRuntimeConfig,
  onUpdateDisplayAndZoom: (zoomPercent: Int, splitViewCount: Int, compactHeaderMode: Boolean) -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth().testTag("display_zoom_split_card"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Tampilan, Zoom Halaman & New Split View",
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = "Zoom berlaku untuk semua tab & web. Split View membagi layar 2 atau 3 kolom secara mandiri di dalam masing-masing akun (tidak mencampur tab antar akun).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Text(
        text = "Zoom Website Global: ${globalConfig.defaultZoomPercent}%",
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = globalConfig.defaultZoomPercent.toFloat(),
        onValueChange = {
          onUpdateDisplayAndZoom(
            it.roundToInt(),
            globalConfig.defaultSplitViewCount,
            globalConfig.compactHeaderMode,
          )
        },
        valueRange = 50f..150f,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(70, 80, 90, 100, 110, 125).forEach { pct ->
          FilterChip(
            selected = globalConfig.defaultZoomPercent == pct,
            onClick = {
              onUpdateDisplayAndZoom(
                pct,
                globalConfig.defaultSplitViewCount,
                globalConfig.compactHeaderMode,
              )
            },
            label = { Text("${pct}%", style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Text(
        text = "New Split View Default:",
        style = MaterialTheme.typography.labelLarge,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1 to "1 Layar Penuh", 2 to "Split 2 Rata", 3 to "Split 3 Rata").forEach { (cnt, label) ->
          FilterChip(
            selected = globalConfig.defaultSplitViewCount == cnt,
            onClick = {
              onUpdateDisplayAndZoom(
                globalConfig.defaultZoomPercent,
                cnt,
                globalConfig.compactHeaderMode,
              )
            },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Mode Header Kompak (Maksimalkan area web)",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = globalConfig.compactHeaderMode,
          onCheckedChange = {
            onUpdateDisplayAndZoom(
              globalConfig.defaultZoomPercent,
              globalConfig.defaultSplitViewCount,
              it,
            )
          },
          modifier = Modifier.testTag("switch_compact_header"),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Fase3OptionalModulesCard(
  globalConfig: GlobalRuntimeConfig,
  pausedCaptchaCount: Int,
  onSaveConfig: (GlobalRuntimeConfig) -> Unit,
  onResumeAllCaptcha: () -> Unit,
) {
  var priorityHours by remember(globalConfig.priorityHoursCsv) { mutableStateOf(globalConfig.priorityHoursCsv) }
  var schedTime by remember(globalConfig.scheduledShotTimeWib) { mutableStateOf(globalConfig.scheduledShotTimeWib) }
  var ntfyServer by remember(globalConfig.ntfyServerUrl) { mutableStateOf(globalConfig.ntfyServerUrl) }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth().testTag("fase3_optional_card"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Penyesuaian Fase 3 & Modul Opsional",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        if (pausedCaptchaCount > 0) {
          OutlinedButton(
            onClick = onResumeAllCaptcha,
            modifier = Modifier.testTag("resume_all_captcha_button"),
          ) {
            Text("Lanjut CAPTCHA ($pausedCaptchaCount)")
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        StatusPill(text = "Lintasan: trip.ferizy.com/", tone = PillTone.INFO)
        StatusPill(text = "Kuota: trip.ferizy.com/ferryschedule", tone = PillTone.SUCCESS)
        StatusPill(
          text = if (globalConfig.audioAlarmMuted) "Audio: MUTE" else "Audio Alarm: ON",
          tone = if (globalConfig.audioAlarmMuted) PillTone.ERROR else PillTone.WARNING,
        )
      }

      Text(
        text = "Auto-Wake Striker via Event Bus (<1ms):",
        style = MaterialTheme.typography.labelLarge,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        AutoWakeMode.entries.forEach { mode ->
          FilterChip(
            selected = globalConfig.autoWakeMode == mode,
            onClick = { onSaveConfig(globalConfig.copy(autoWakeMode = mode)) },
            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "1. Gentle Session Keep-Alive (${globalConfig.keepAliveIntervalMinutes}m)",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = globalConfig.gentleKeepAliveEnabled,
          onCheckedChange = { onSaveConfig(globalConfig.copy(gentleKeepAliveEnabled = it)) },
          modifier = Modifier.testTag("switch_keep_alive"),
        )
      }
      if (globalConfig.gentleKeepAliveEnabled) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(10, 12, 15).forEach { mins ->
            FilterChip(
              selected = globalConfig.keepAliveIntervalMinutes == mins,
              onClick = { onSaveConfig(globalConfig.copy(keepAliveIntervalMinutes = mins)) },
              label = { Text("${mins} menit", style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "2. Detektor CAPTCHA + Auto-Pop Tray",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = globalConfig.captchaAutoPopEnabled,
          onCheckedChange = { onSaveConfig(globalConfig.copy(captchaAutoPopEnabled = it)) },
          modifier = Modifier.testTag("switch_captcha_autopop"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "3. Smart Target Matcher (Kuota > 0)",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = globalConfig.smartTargetMatcherEnabled,
          onCheckedChange = { onSaveConfig(globalConfig.copy(smartTargetMatcherEnabled = it)) },
          modifier = Modifier.testTag("switch_smart_matcher"),
        )
      }

      if (globalConfig.smartTargetMatcherEnabled) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ServiceFilterType.entries.forEach { svc ->
            FilterChip(
              selected = globalConfig.smartMatcherServiceType == svc,
              onClick = { onSaveConfig(globalConfig.copy(smartMatcherServiceType = svc)) },
              label = { Text(svc.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
          FilterChip(
            selected = globalConfig.smartMatcherFallbackNearest,
            onClick = {
              onSaveConfig(
                globalConfig.copy(smartMatcherFallbackNearest = !globalConfig.smartMatcherFallbackNearest)
              )
            },
            label = { Text("Fallback Jam Terdekat", style = MaterialTheme.typography.labelSmall) },
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = priorityHours,
            onValueChange = { priorityHours = it },
            label = { Text("Jam Prioritas Jadwal") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("priority_hours_input"),
          )
          OutlinedButton(
            onClick = { onSaveConfig(globalConfig.copy(priorityHoursCsv = priorityHours)) },
          ) {
            Text("Simpan")
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "4. Latency Compensation + Timeout ${globalConfig.fastRequestTimeoutMs / 1000.0}s",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = globalConfig.latencyCompensationEnabled,
          onCheckedChange = { onSaveConfig(globalConfig.copy(latencyCompensationEnabled = it)) },
          modifier = Modifier.testTag("switch_latency_comp"),
        )
      }
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(3500, 4500, 5500).forEach { ms ->
          FilterChip(
            selected = globalConfig.fastRequestTimeoutMs == ms,
            onClick = { onSaveConfig(globalConfig.copy(fastRequestTimeoutMs = ms)) },
            label = { Text("Timeout ${ms / 1000.0}s", style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Text(
        text = "5. Mode Stop Setelah Menang:",
        style = MaterialTheme.typography.labelLarge,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        AdvancedStopMode.entries.forEach { mode ->
          FilterChip(
            selected = globalConfig.advancedStopMode == mode,
            onClick = { onSaveConfig(globalConfig.copy(advancedStopMode = mode)) },
            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "6. Scheduled Shot (Jam Server Ferizy)",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = globalConfig.scheduledShotEnabled,
          onCheckedChange = { onSaveConfig(globalConfig.copy(scheduledShotEnabled = it)) },
          modifier = Modifier.testTag("switch_scheduled_shot"),
        )
      }
      if (globalConfig.scheduledShotEnabled) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = schedTime,
            onValueChange = { schedTime = it },
            label = { Text("Target Jam Server (HH:MM:SS)") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("scheduled_shot_input"),
          )
          OutlinedButton(
            onClick = { onSaveConfig(globalConfig.copy(scheduledShotTimeWib = schedTime)) },
          ) {
            Text("Set")
          }
        }
      }

      Text(
        text = "7. Pilihan Notifikasi Terkirim ke HP (ntfy) — hanya yang tercentang:",
        style = MaterialTheme.typography.labelLarge,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Notif CAPTCHA / Challenge Muncul (ON/OFF)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
          checked = globalConfig.notifCaptchaEnabled,
          onCheckedChange = { onSaveConfig(globalConfig.copy(notifCaptchaEnabled = it)) },
          modifier = Modifier.testTag("switch_notif_captcha"),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Bisukan Audio Alarm Shell (Mute)", style = MaterialTheme.typography.bodyMedium)
        Switch(
          checked = globalConfig.audioAlarmMuted,
          onCheckedChange = { onSaveConfig(globalConfig.copy(audioAlarmMuted = it)) },
          modifier = Modifier.testTag("switch_mute_alarm"),
        )
      }
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(5, 10, 30).forEach { sec ->
          FilterChip(
            selected = globalConfig.notifDedupeSeconds == sec,
            onClick = { onSaveConfig(globalConfig.copy(notifDedupeSeconds = sec)) },
            label = { Text("Dedupe ${sec}s", style = MaterialTheme.typography.labelSmall) },
          )
        }
        FilterChip(
          selected = globalConfig.notifLintasanEnabled,
          onClick = { onSaveConfig(globalConfig.copy(notifLintasanEnabled = !globalConfig.notifLintasanEnabled)) },
          label = { Text("Lintasan / Jam Muncul", style = MaterialTheme.typography.labelSmall) },
        )
        FilterChip(
          selected = globalConfig.notifKuotaEnabled,
          onClick = { onSaveConfig(globalConfig.copy(notifKuotaEnabled = !globalConfig.notifKuotaEnabled)) },
          label = { Text("Perubahan Kuota", style = MaterialTheme.typography.labelSmall) },
        )
        FilterChip(
          selected = globalConfig.notifTembusEnabled,
          onClick = { onSaveConfig(globalConfig.copy(notifTembusEnabled = !globalConfig.notifTembusEnabled)) },
          label = { Text("Order Sukses", style = MaterialTheme.typography.labelSmall) },
        )
        FilterChip(
          selected = globalConfig.notifLogoutEnabled,
          onClick = { onSaveConfig(globalConfig.copy(notifLogoutEnabled = !globalConfig.notifLogoutEnabled)) },
          label = { Text("Logout / 401", style = MaterialTheme.typography.labelSmall) },
        )
        FilterChip(
          selected = globalConfig.notifProxyPutusEnabled,
          onClick = { onSaveConfig(globalConfig.copy(notifProxyPutusEnabled = !globalConfig.notifProxyPutusEnabled)) },
          label = { Text("Proxy Putus", style = MaterialTheme.typography.labelSmall) },
        )
        FilterChip(
          selected = globalConfig.notifCaptchaEnabled,
          onClick = { onSaveConfig(globalConfig.copy(notifCaptchaEnabled = !globalConfig.notifCaptchaEnabled)) },
          label = { Text("CAPTCHA", style = MaterialTheme.typography.labelSmall) },
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = ntfyServer,
          onValueChange = { ntfyServer = it },
          label = { Text("Server ntfy URL") },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("ntfy_server_url_input"),
        )
        OutlinedButton(
          onClick = { onSaveConfig(globalConfig.copy(ntfyServerUrl = ntfyServer)) },
        ) {
          Text("Simpan")
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlobalRuntimeEditor(
  staggeredBootMs: Int,
  ntfyTopic: String,
  closeToTray: Boolean,
  powerSaveBlocker: Boolean,
  assetBlockerMode: AssetBlockerMode,
  memoryWatchdogLimitMb: Int,
  staggeredReloadDelayMs: Int,
  lazyAntiRevertEnabled: Boolean,
  onSave: (Int, String, Boolean, Boolean, AssetBlockerMode, Int, Int, Boolean) -> Unit,
) {
  var bootSlider by remember(staggeredBootMs) { mutableStateOf(staggeredBootMs.toFloat()) }
  var topic by remember(ntfyTopic) { mutableStateOf(ntfyTopic) }
  var trayEnabled by remember(closeToTray) { mutableStateOf(closeToTray) }
  var powerEnabled by remember(powerSaveBlocker) { mutableStateOf(powerSaveBlocker) }
  var blockerMode by remember(assetBlockerMode) { mutableStateOf(assetBlockerMode) }
  var watchdogSlider by remember(memoryWatchdogLimitMb) { mutableStateOf(memoryWatchdogLimitMb.toFloat()) }
  var queueSlider by remember(staggeredReloadDelayMs) { mutableStateOf(staggeredReloadDelayMs.toFloat()) }
  var antiRevert by remember(lazyAntiRevertEnabled) { mutableStateOf(lazyAntiRevertEnabled) }

  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Asset & Media Blocker (Fase 2)",
        style = MaterialTheme.typography.labelLarge,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        AssetBlockerMode.entries.forEach { mode ->
          FilterChip(
            selected = blockerMode == mode,
            onClick = { blockerMode = mode },
            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Text(
        text = "Memory Watchdog Tab Standby: ${watchdogSlider.roundToInt()} MB",
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = watchdogSlider,
        onValueChange = { watchdogSlider = it },
        valueRange = 250f..550f,
      )

      Text(
        text = "Staggered Reload Queue (Reaktif Opsi A): ${queueSlider.roundToInt()} ms/tab",
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = queueSlider,
        onValueChange = { queueSlider = it },
        valueRange = 250f..800f,
      )

      Text(
        text = "Staggered Boot Startup: ${bootSlider.roundToInt()} ms/tab",
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = bootSlider,
        onValueChange = { bootSlider = it },
        valueRange = 250f..400f,
      )

      OutlinedTextField(
        value = topic,
        onValueChange = { topic = it },
        label = { Text("Topik ntfy.sh Default") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("global_ntfy_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Lazy Anti-Revert (Jaga Input Penumpang)", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = antiRevert, onCheckedChange = { antiRevert = it })
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Close-to-Tray 24 Jam", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = trayEnabled, onCheckedChange = { trayEnabled = it })
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("powerSaveBlocker + Anti-Throttle", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = powerEnabled, onCheckedChange = { powerEnabled = it })
      }

      Button(
        onClick = {
          onSave(
            bootSlider.roundToInt(),
            topic,
            trayEnabled,
            powerEnabled,
            blockerMode,
            watchdogSlider.roundToInt(),
            queueSlider.roundToInt(),
            antiRevert,
          )
        },
        modifier = Modifier.align(Alignment.End).testTag("save_global_runtime_button"),
      ) {
        Text("Terapkan ke Paket Electron")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountPreFlightCard(
  account: AccountContainerConfig,
  ntfyTopic: String,
  onAddTab: () -> Unit,
  onRemoveTab: (String) -> Unit,
  onToggleTabLogin: (String) -> Unit,
  onToggleTabRunner: (String) -> Unit,
  onSimulateTabWin: (String) -> Unit,
  onSimulateTabCaptcha: (String) -> Unit,
  onSimulateTabLogout: (String) -> Unit,
  onEditAccount: () -> Unit,
  onSaveConfig: () -> Unit,
  onOpenLoadCopyModal: () -> Unit,
  onRemoveAccount: () -> Unit,
) {
  val anyLoggedIn = account.tabs.any { it.isLoggedIn }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth().testTag("account_card_${account.accountId}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = account.name,
              style = MaterialTheme.typography.titleMedium,
            )
            StatusPill(text = account.partition, tone = PillTone.NEUTRAL)
            StatusPill(
              text = "${account.tabs.size}/3 Tab",
              tone = if (account.canAddTab) PillTone.INFO else PillTone.WARNING,
            )
          }
          Text(
            text = "https://trip.ferizy.com • Jeda Default 0.5s (500ms) • Micro-Jitter ±${account.microJitterMeters}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          OutlinedButton(
            onClick = onEditAccount,
            modifier = Modifier.testTag("edit_account_${account.accountId}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Router,
              contentDescription = "Atur Proxy dan Lokasi ${account.name}",
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Proxy & Geo")
          }
          if (account.accountId > 5) {
            IconButton(
              onClick = onRemoveAccount,
              modifier = Modifier.size(40.dp).testTag("remove_account_${account.accountId}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Hapus ${account.name}",
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OutlinedButton(
          onClick = onSaveConfig,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("save_autoklik_account_${account.accountId}"),
        ) {
          Text("💾 Save Konfigurasi", style = MaterialTheme.typography.labelSmall)
        }
        Button(
          onClick = onOpenLoadCopyModal,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("load_autoklik_account_${account.accountId}"),
        ) {
          Text("📂 Load & Copy-Paste Antar Tab", style = MaterialTheme.typography.labelSmall)
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        StatusPill(
          text = if (anyLoggedIn) "Login: Aktif" else "Login: Logout (/login)",
          tone = if (anyLoggedIn) PillTone.SUCCESS else PillTone.WARNING,
        )
        StatusPill(
          text = if (account.proxy.isConfigured) "Proxy: ${account.proxy.ipLabel}" else "IP: Direct ISP",
          tone = if (account.proxy.isConfigured) PillTone.SUCCESS else PillTone.INFO,
        )
        StatusPill(
          text =
            if (account.geoMode == GeoProviderMode.PRESET_0MS) {
              "Geo: Granted 0ms (${account.geoPreset.label})"
            } else {
              "Geo: Granted Windows"
            },
          tone = PillTone.SUCCESS,
        )
        StatusPill(
          text = if (account.points.isNotEmpty()) "Titik: Siap (${account.points.size})" else "Titik: Kosong",
          tone = if (account.points.isNotEmpty()) PillTone.SUCCESS else PillTone.ERROR,
        )
        StatusPill(text = "ntfy: $ntfyTopic", tone = PillTone.INFO)
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = String.format("%.5f, %.5f", account.effectiveLat, account.effectiveLng),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "WebRTC Anti-Leak • Mouse Copas Aktif",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        account.tabs.forEach { tab ->
          val runnerTone =
            when (tab.runnerState) {
              TabRunnerState.IDLE -> PillTone.NEUTRAL
              TabRunnerState.RUNNING -> PillTone.WARNING
              TabRunnerState.WON_STOPPED -> PillTone.SUCCESS
              TabRunnerState.PAUSED_CAPTCHA -> PillTone.WARNING
              TabRunnerState.LOGOUT_STOPPED -> PillTone.ERROR
            }

          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("tab_chip_${tab.tabId}"),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(
                  modifier =
                    Modifier.weight(1f).clickable { onToggleTabLogin(tab.tabId) },
                ) {
                  Text(
                    text = "Tab ${tab.tabIndex} • ${tab.role.label}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "${tab.memoryMb} MB • ${if (tab.isLoggedIn) "Sesi Aktif" else "/login (tap simulasi login)"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                StatusPill(text = tab.runnerState.label, tone = runnerTone)
                if (account.tabs.size > 1) {
                  IconButton(
                    onClick = { onRemoveTab(tab.tabId) },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Close,
                      contentDescription = "Tutup Tab ${tab.tabIndex}",
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                OutlinedButton(
                  onClick = { onToggleTabRunner(tab.tabId) },
                  modifier = Modifier.testTag("toggle_runner_${tab.tabId}"),
                ) {
                  Icon(
                    imageVector =
                      if (tab.runnerState == TabRunnerState.RUNNING) Icons.Rounded.Stop
                      else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text =
                      when (tab.runnerState) {
                        TabRunnerState.IDLE -> "Start"
                        TabRunnerState.RUNNING -> "Stop"
                        TabRunnerState.WON_STOPPED,
                        TabRunnerState.PAUSED_CAPTCHA,
                        TabRunnerState.LOGOUT_STOPPED -> "Reset"
                      },
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                if (tab.runnerState != TabRunnerState.WON_STOPPED) {
                  TextButton(
                    onClick = { onSimulateTabWin(tab.tabId) },
                    modifier = Modifier.testTag("simulate_win_${tab.tabId}"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.EmojiEvents,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Menang 0ms", style = MaterialTheme.typography.labelSmall)
                  }
                  TextButton(
                    onClick = { onSimulateTabCaptcha(tab.tabId) },
                    modifier = Modifier.testTag("simulate_captcha_${tab.tabId}"),
                  ) {
                    Text("Tes CAPTCHA", style = MaterialTheme.typography.labelSmall)
                  }
                  TextButton(
                    onClick = { onSimulateTabLogout(tab.tabId) },
                    modifier = Modifier.testTag("simulate_logout_${tab.tabId}"),
                  ) {
                    Text("Tes 401", style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }

        Button(
          onClick = onAddTab,
          enabled = account.canAddTab,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          modifier = Modifier.testTag("add_tab_account_${account.accountId}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (account.canAddTab) "+ Tab (${account.tabs.size}/3)" else "Terkunci 3/3")
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountLoadCopyConfigDialog(
  targetAccount: AccountContainerConfig,
  allAccounts: List<AccountContainerConfig>,
  globalConfig: GlobalRuntimeConfig,
  onDismiss: () -> Unit,
  onSaveToFile: () -> Unit,
  onApplyJsonToAccount: (Int, String) -> Unit,
  onNotifyMessage: (String) -> Unit,
) {
  var sourceAccountId by remember(targetAccount.accountId) { mutableStateOf(targetAccount.accountId) }
  var sourceTabIndex by remember { mutableStateOf(1) }
  var targetTabIndex by remember { mutableStateOf(1) }
  val sourceAccount = allAccounts.find { it.accountId == sourceAccountId } ?: targetAccount
  var jsonText by remember(sourceAccountId) {
    mutableStateOf(
      com.example.domain.AutoKlikJsonSerializer.buildSingleAccountExportJson(sourceAccount, globalConfig)
    )
  }

  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "📂 Load & Copy-Paste Konfigurasi (${targetAccount.name})",
        style = MaterialTheme.typography.titleMedium,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Salin konfigurasi titik AutoKlik (Jeda Default 0.5 detik) antar Tab (Tab 1/2/3) atau antar Akun Kontainer:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Pilih Akun Sumber:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          allAccounts.forEach { acc ->
            FilterChip(
              selected = sourceAccountId == acc.accountId,
              onClick = {
                sourceAccountId = acc.accountId
                jsonText = com.example.domain.AutoKlikJsonSerializer.buildSingleAccountExportJson(acc, globalConfig)
              },
              label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        Text("Tab Sumber → Tab Tujuan (${targetAccount.name}):", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          (1..3).forEach { tIdx ->
            FilterChip(
              selected = sourceTabIndex == tIdx,
              onClick = { sourceTabIndex = tIdx },
              label = { Text("Dari Tab $tIdx", style = MaterialTheme.typography.labelSmall) },
            )
          }
          (1..3).forEach { tIdx ->
            FilterChip(
              selected = targetTabIndex == tIdx,
              onClick = { targetTabIndex = tIdx },
              label = { Text("Ke Tab $tIdx", style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(
            onClick = {
              onApplyJsonToAccount(targetAccount.accountId, jsonText)
              onNotifyMessage(
                "Konfigurasi dari ${sourceAccount.name} (Tab $sourceTabIndex) berhasil di-Copy-Paste ke ${targetAccount.name} (Tab $targetTabIndex)."
              )
              onDismiss()
            },
            modifier = Modifier.testTag("btn_dialog_copy_paste_tab"),
          ) {
            Text("📥 Copy-Paste ke Tab $targetTabIndex", style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = {
              onSaveToFile()
              onDismiss()
            },
            modifier = Modifier.testTag("btn_dialog_save_file"),
          ) {
            Text("💾 Save .JSON", style = MaterialTheme.typography.labelSmall)
          }
        }
        OutlinedTextField(
          value = jsonText,
          onValueChange = { jsonText = it },
          label = { Text("Copy-Paste JSON Konfigurasi AutoKlik") },
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("dialog_load_copy_json_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onApplyJsonToAccount(targetAccount.accountId, jsonText)
          onDismiss()
        },
      ) {
        Text("Terapkan JSON")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Tutup")
      }
    },
  )
}

