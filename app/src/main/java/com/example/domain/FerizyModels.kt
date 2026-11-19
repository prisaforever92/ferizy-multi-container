package com.example.domain

object FerizyBuildVersion {
  const val RELEASE_TAG = "V5.8"
  const val SEMVER = "5.8.0"
  const val ZIP_BASENAME = "ferizy-multi-container-v5.8"
}

enum class GeoProviderMode(val id: String, val label: String) {
  PRESET_0MS("preset_0ms", "Preset Koordinat 0ms + Micro-Jitter"),
  WINDOWS_OS("windows", "Lokasi Windows Native"),
}

enum class PortGeoPreset(
  val id: String,
  val label: String,
  val latitude: Double,
  val longitude: Double,
) {
  MERAK("merak", "Pelabuhan Merak", -5.9308, 105.9935),
  BAKAUHENI("bakauheni", "Pelabuhan Bakauheni", -5.8705, 105.7538),
  KETAPANG("ketapang", "Pelabuhan Ketapang", -8.1449, 114.4036),
  GILIMANUK("gilimanuk", "Pelabuhan Gilimanuk", -8.1636, 114.4375),
  CUSTOM("custom", "Koordinat Kustom", -6.1754, 106.8272),
}

enum class ProxyProtocol(val scheme: String, val label: String) {
  DIRECT("direct", "Direct (Tanpa Proxy)"),
  HTTP("http", "HTTP Proxy"),
  SOCKS5("socks5", "SOCKS5 / Modem Bridge"),
}

data class ProxyConfig(
  val protocol: ProxyProtocol = ProxyProtocol.DIRECT,
  val host: String = "",
  val port: Int = 8080,
  val username: String = "",
  val password: String = "",
  val ipLabel: String = "Direct ISP",
) {
  val isConfigured: Boolean
    get() = protocol != ProxyProtocol.DIRECT && host.isNotBlank() && port in 1..65535
}

enum class TabRole(val id: String, val label: String) {
  SCOUT("scout", "Scout Radar 0ms"),
  STRIKER_MAIN("striker_main", "Striker Utama"),
  STRIKER_BACKUP("striker_backup", "Striker Cadangan"),
}

enum class TabRunnerState(val id: String, val label: String) {
  IDLE("idle", "Standby"),
  RUNNING("running", "Menembak"),
  WON_STOPPED("won_stopped", "Menang • Stop 0ms"),
  PAUSED_CAPTCHA("paused_captcha", "Pause • CAPTCHA"),
  LOGOUT_STOPPED("logout_stopped", "Stop • Logout/401"),
}

enum class AssetBlockerMode(val id: String, val label: String) {
  SELECTIVE("selective", "Selektif (CSS & CAPTCHA Aman)"),
  AGGRESSIVE("aggressive", "Agresif (Hemat RAM Maksimal)"),
  DISABLED("disabled", "Nonaktif"),
}

enum class StaggeredQueueMode(val id: String, val label: String) {
  REACTIVE_ONLY("reactive_only", "Reaktif Saja (Watchdog & Manual)"),
  PERIODIC_OPTIONAL("periodic_optional", "Reaktif + Timer Opsional"),
}

enum class AdvancedStopMode(val id: String, val label: String) {
  PER_TAB_ONLY("per_tab", "Isolasi Ketat Per-Tab (Default)"),
  STOP_SAME_ACCOUNT("per_account", "Stop Semua Tab Dalam 1 Akun"),
  STOP_ALL_ACCOUNTS("all_accounts", "Stop Seluruh 5 Akun"),
}

enum class AutoWakeMode(val id: String, val label: String) {
  MONITOR_ONLY("monitor_only", "Monitor Saja"),
  SAME_ACCOUNT("same_account", "Wake Akun Sama <1ms"),
  ALL_ACCOUNTS("all_accounts", "Wake 5 Akun <1ms"),
}

enum class ServiceFilterType(val id: String, val label: String) {
  ALL("all", "Semua Layanan"),
  EXPRESS("express", "Express"),
  REGULER("reguler", "Reguler"),
}

enum class LogRotationPreset(
  val id: String,
  val label: String,
  val maxSizeMb: Int,
  val retentionDays: Int,
) {
  RECOMMENDED_50MB_7D("50mb_7d", "50 MB / 7 Hari (Rekomendasi)", 50, 7),
  COMPACT_20MB_3D("20mb_3d", "20 MB / 3 Hari (Hemat SSD)", 20, 3),
}

data class EnduranceAuditResult(
  val simulatedTabs: Int,
  val peakEstimatedRamGb: Double,
  val ramBudgetGb: Double = 16.0,
  val watchdogTriggerTabCount: Int,
  val projected24hLogMb: Int,
  val maxLogBudgetMb: Int,
  val passed: Boolean,
  val summary: String,
)

data class SimulatedTab(
  val tabId: String,
  val tabIndex: Int,
  val title: String,
  val url: String = if (tabIndex <= 1) "https://trip.ferizy.com" else "https://trip.ferizy.com/order",
  val role: TabRole = TabRole.STRIKER_MAIN,
  val isLoggedIn: Boolean = false,
  val runnerState: TabRunnerState = TabRunnerState.IDLE,
  val memoryMb: Int = 148,
  val blockedAssetsCount: Int = 0,
  val wonUrl: String? = null,
)

enum class AutoKlikPointType(val jsonType: String, val label: String, val badge: String) {
  TAP("tap", "Tap / Klik", "TAP"),
  SWIPE("swipe", "Swipe / Scroll", "SWP"),
  INPUT("input", "Input Ketik", "INP"),
  POPUP_KEY("popup_key", "Popup Key", "KEY"),
}

data class AutoKlikPoint(
  val id: String,
  val order: Int,
  val type: AutoKlikPointType,
  val label: String,
  val x: Int = 540,
  val y: Int = 420,
  val endX: Int? = null,
  val endY: Int? = null,
  val durationMs: Int = 120,
  val selector: String = "",
  val inputValue: String = "",
  val typingSpeedMs: Int = 30,
  val popupKey: String = "Enter",
  val popupDelayMs: Int = 300,
  val delayAfterMs: Int = 500,
  val enabled: Boolean = true,
  val antiRevert: Boolean = false,
  val radiusPx: Int = 28,
)

data class AutoKlikExtensionSettings(
  val enabled: Boolean = true,
  val loopEnabled: Boolean = true,
  val maxLoops: Int = 0,
  val loopDelayMs: Int = 100,
  val radiusPx: Int = 28,
  val smartMatch: Boolean = true,
  val antiRevert: Boolean = true,
  val markersHidden: Boolean = false,
  val pointsLocked: Boolean = false,
)

data class AccountContainerConfig(
  val accountId: Int,
  val partition: String = "persist:akun_$accountId",
  val name: String = "Akun $accountId",
  val tabs: List<SimulatedTab> =
    listOf(
      SimulatedTab(
        tabId = "akun_${accountId}_tab_1",
        tabIndex = 1,
        title = "Ferizy • Akun $accountId",
        url = "https://trip.ferizy.com",
        role = if (accountId == 1) TabRole.SCOUT else TabRole.STRIKER_MAIN,
        memoryMb = 142 + (accountId * 6),
      )
    ),
  val proxy: ProxyConfig = ProxyConfig(),
  val geoMode: GeoProviderMode = GeoProviderMode.PRESET_0MS,
  val geoPreset: PortGeoPreset = PortGeoPreset.MERAK,
  val customLat: Double = -5.9308,
  val customLng: Double = 105.9935,
  val microJitterMeters: Int = 12 + (accountId * 5),
  val points: List<AutoKlikPoint> = defaultPointsForAccount(accountId),
  val extensionSettings: AutoKlikExtensionSettings = AutoKlikExtensionSettings(),
) {
  val effectiveLat: Double
    get() {
      val base = if (geoPreset == PortGeoPreset.CUSTOM) customLat else geoPreset.latitude
      val jitter = (accountId - 3) * 0.00009
      return base + jitter
    }

  val effectiveLng: Double
    get() {
      val base = if (geoPreset == PortGeoPreset.CUSTOM) customLng else geoPreset.longitude
      val sign = if (accountId % 2 == 0) 1.0 else -1.0
      val jitter = sign * accountId * 0.00007
      return base + jitter
    }

  val canAddTab: Boolean
    get() = tabs.size < 3
}

data class GlobalRuntimeConfig(
  val activePhase: Int = 4,
  val staggeredBootMs: Int = 300,
  val hardCapPerAccount: Int = 3,
  val maxAccounts: Int = 5,
  val ntfyTopic: String = "ferizy_Transsamudra05",
  val closeToTray: Boolean = true,
  val powerSaveBlocker: Boolean = true,
  val webrtcDisableNonProxiedUdp: Boolean = true,
  val targetUrl: String = "https://trip.ferizy.com",
  val defaultZoomPercent: Int = 100,
  val defaultSplitViewCount: Int = 1,
  val compactHeaderMode: Boolean = false,
  val tab1MasterMirrorEnabled: Boolean = true,
  val tab1CollectionSplitActive: Boolean = false,
  // Fase 2 controls
  val assetBlockerMode: AssetBlockerMode = AssetBlockerMode.SELECTIVE,
  val memoryWatchdogLimitMb: Int = 350,
  val staggeredReloadDelayMs: Int = 400,
  val staggeredQueueMode: StaggeredQueueMode = StaggeredQueueMode.REACTIVE_ONLY,
  val lazyAntiRevertEnabled: Boolean = true,
  val winIsolationPerTab: Boolean = true,
  // Fase 3 controls (confirmed defaults + penyesuaian Fase 3)
  val apiSniffer0msEnabled: Boolean = true,
  val serverClockSyncEnabled: Boolean = true,
  val autoWakeMode: AutoWakeMode = AutoWakeMode.SAME_ACCOUNT,
  val notifLintasanExactUrl: String = "https://trip.ferizy.com/",
  val notifKuotaExactUrl: String = "https://trip.ferizy.com/ferryschedule",
  val ntfyServerUrl: String = "https://ntfy.sh",
  val notifDedupeSeconds: Int = 10,
  val audioAlarmMuted: Boolean = false,
  val notifLintasanEnabled: Boolean = true,
  val notifKuotaEnabled: Boolean = true,
  val notifTembusEnabled: Boolean = true,
  val notifLogoutEnabled: Boolean = false,
  val notifProxyPutusEnabled: Boolean = false,
  val notifCaptchaEnabled: Boolean = false,
  val gentleKeepAliveEnabled: Boolean = true,
  val keepAliveIntervalMinutes: Int = 12,
  val captchaAutoPopEnabled: Boolean = true,
  val smartTargetMatcherEnabled: Boolean = true,
  val priorityHoursCsv: String = "08:00,10:00,12:00,14:00,20:00",
  val smartMatcherServiceType: ServiceFilterType = ServiceFilterType.ALL,
  val smartMatcherFallbackNearest: Boolean = true,
  val latencyCompensationEnabled: Boolean = false,
  val fastRequestTimeoutMs: Int = 4500,
  val advancedStopMode: AdvancedStopMode = AdvancedStopMode.PER_TAB_ONLY,
  val scheduledShotEnabled: Boolean = false,
  val scheduledShotTimeWib: String = "00:00:00",
  // Fase 4 controls (Log Rotation & 24h Endurance)
  val logRotationPreset: LogRotationPreset = LogRotationPreset.RECOMMENDED_50MB_7D,
  val logMaxSizeMb: Int = 50,
  val logRetentionDays: Int = 7,
  val enduranceHealthReportEnabled: Boolean = true,
  val healthSnapshotIntervalMinutes: Int = 60,
  val watchdogCalibrationTargetMb: Int = 350,
)

enum class PhaseStatus(val label: String) {
  IMPLEMENTED("Aktif di Generator"),
  AWAITING_CONFIRMATION("Menunggu Konfirmasi User"),
  PLANNED("Terjadwal"),
}

data class RoadmapPhaseInfo(
  val phaseNumber: Int,
  val title: String,
  val summary: String,
  val deliverables: List<String>,
  val optionsAndImpact: String,
  val status: PhaseStatus,
)

data class OptionalFeatureOption(
  val id: String,
  val title: String,
  val targetPhase: String,
  val description: String,
  val optionsDetail: String,
  val impactRamCpu: String,
  val impactRiskSpeed: String,
  val defaultChecked: Boolean = false,
)

data class GeneratedElectronFile(
  val relativePath: String,
  val category: String,
  val language: String,
  val summary: String,
  val content: String,
)

fun defaultPointsForAccount(accountId: Int): List<AutoKlikPoint> =
  listOf(
    AutoKlikPoint(
      id = "pt_${accountId}_1",
      order = 1,
      type = AutoKlikPointType.TAP,
      label = "Titik 1",
      x = 410,
      y = 320,
      selector = "",
      delayAfterMs = 500,
      antiRevert = false,
      radiusPx = 28,
    ),
    AutoKlikPoint(
      id = "pt_${accountId}_2",
      order = 2,
      type = AutoKlikPointType.SWIPE,
      label = "Titik 2",
      x = 540,
      y = 620,
      endX = 540,
      endY = 260,
      durationMs = 180,
      delayAfterMs = 500,
      antiRevert = false,
      radiusPx = 24,
    ),
    AutoKlikPoint(
      id = "pt_${accountId}_3",
      order = 3,
      type = AutoKlikPointType.INPUT,
      label = "Titik 3",
      x = 480,
      y = 490,
      selector = "",
      inputValue = "PENUMPANG AKUN $accountId",
      typingSpeedMs = 30,
      popupDelayMs = 300,
      delayAfterMs = 500,
      antiRevert = true,
      radiusPx = 32,
    ),
    AutoKlikPoint(
      id = "pt_${accountId}_4",
      order = 4,
      type = AutoKlikPointType.POPUP_KEY,
      label = "Titik 4",
      x = 640,
      y = 580,
      popupKey = "Enter",
      delayAfterMs = 500,
      antiRevert = false,
      radiusPx = 24,
    ),
  )
