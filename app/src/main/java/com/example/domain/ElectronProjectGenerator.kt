package com.example.domain

import org.json.JSONArray
import org.json.JSONObject

object ElectronProjectGenerator {

  fun generateProjectFiles(
    accounts: List<AccountContainerConfig>,
    globalConfig: GlobalRuntimeConfig,
  ): List<GeneratedElectronFile> {
    val runtimeJson = buildRuntimeConfigJson(accounts, globalConfig)
    val autoklikJson = AutoKlikJsonSerializer.buildFullExportJson(accounts, globalConfig)

    return listOf(
      GeneratedElectronFile(
        relativePath = "package.json",
        category = "Konfigurasi & Build",
        language = "json",
        summary = "Manifest Electron 33 & electron-builder target Windows x64 (.exe) Fase 4",
        content = ElectronMainTemplates.packageJson(),
      ),
      GeneratedElectronFile(
        relativePath = "electron-builder.yml",
        category = "Konfigurasi & Build",
        language = "yaml",
        summary = "Konfigurasi installer NSIS + Portable .EXE Windows x64",
        content = ElectronMainTemplates.electronBuilderYml(),
      ),
      GeneratedElectronFile(
        relativePath = "README-WINDOWS.md",
        category = "Panduan Build",
        language = "markdown",
        summary = "Instruksi npm install, npm run test:endurance, npm start & build:win (Fase 1–4)",
        content = ElectronMainTemplates.readmeWindows(globalConfig),
      ),
      GeneratedElectronFile(
        relativePath = "config/runtime-config.json",
        category = "Konfigurasi Dinamis",
        language = "json",
        summary = "Konfigurasi 5 akun, Asset Blocker, Watchdog 350MB, Sniffer 0ms & 5 Modul Opsional",
        content = runtimeJson,
      ),
      GeneratedElectronFile(
        relativePath = "config/autoklik-pro-v3.2-template.json",
        category = "Konfigurasi Dinamis",
        language = "json",
        summary = "Profil titik kompatibel AutoKlik Pro v3.2 + antiRevert & topik ntfy.sh",
        content = autoklikJson,
      ),
      GeneratedElectronFile(
        relativePath = "src/main/index.js",
        category = "Main Process (Node.js)",
        language = "javascript",
        summary = "Entry point Electron, powerSaveBlocker, Tray, Watchdog, EventBus & IPC Fase 3",
        content = ElectronMainTemplates.mainIndexJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/sessionManager.js",
        category = "Main Process (Node.js)",
        language = "javascript",
        summary = "Isolasi persist:akun_1..N dinamis, WebContentsView Split 1/2/3, Zoom Global & Hard Cap 3/3",
        content = ElectronMainTemplates.sessionManagerJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/automationEngine.js",
        category = "Mesin Fase 2",
        language = "javascript",
        summary = "sendInputEvent (isTrusted: true), Lazy Anti-Revert & Isolasi Menang Per-Tab 0ms",
        content = ElectronFase2Templates.automationEngineJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/assetBlocker.js",
        category = "Mesin Fase 2",
        language = "javascript",
        summary = "webRequest Asset & Media Blocker Selektif (CSS & CAPTCHA tetap diizinkan)",
        content = ElectronFase2Templates.assetBlockerJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/memoryWatchdog.js",
        category = "Mesin Fase 2",
        language = "javascript",
        summary = "app.getAppMetrics() 350MB standby + Staggered Reload Queue 400ms Reaktif (Opsi A)",
        content = ElectronFase2Templates.memoryWatchdogJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/pointProfileStore.js",
        category = "Mesin Fase 2",
        language = "javascript",
        summary = "Penyimpanan profil titik independen per tab & akun + Copy/Paste lintas akun",
        content = ElectronFase2Templates.pointProfileStoreJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/eventBus.js",
        category = "Mesin Fase 3",
        language = "javascript",
        summary = "Main Process Event Bus (<1ms) sinyal lintas tab & akun",
        content = ElectronFase3Templates.eventBusJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/serverClockSync.js",
        category = "Mesin Fase 3",
        language = "javascript",
        summary = "Sinkronisasi jam server Ferizy dari header HTTP Date + estimasi RTT",
        content = ElectronFase3Templates.serverClockSyncJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/notificationCenter.js",
        category = "Mesin Fase 3",
        language = "javascript",
        summary = "Notif terpusat (Opsi A): ntfy.sh + Toast + Audio Alarm + aturan URL Lintasan/Kuota",
        content = ElectronFase3Templates.notificationCenterJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/optionalModules.js",
        category = "Mesin Fase 3",
        language = "javascript",
        summary = "5 Modul Ceklist Opsional (Keep-Alive, CAPTCHA Auto-Pop, Smart Matcher, Latency, Stop)",
        content = ElectronFase3Templates.optionalModulesJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/logRotationManager.js",
        category = "Mesin Fase 4",
        language = "javascript",
        summary = "Log Rotation 50MB/7h atau 20MB/3h dengan pembersihan otomatis aman SSD 256 GB",
        content = ElectronFase4Templates.logRotationManagerJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/enduranceMonitor.js",
        category = "Mesin Fase 4",
        language = "javascript",
        summary = "Monitor ketahanan 24 jam, kalibrasi Watchdog & ekspor health-report-24h.json",
        content = ElectronFase4Templates.enduranceMonitorJs(),
      ),
      GeneratedElectronFile(
        relativePath = "scripts/endurance-stress-test.js",
        category = "Pengujian Fase 4",
        language = "javascript",
        summary = "Simulasi stress test 15 tab x 24 jam (1.440 menit) via npm run test:endurance",
        content = ElectronFase4Templates.enduranceStressTestJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/proxyManager.js",
        category = "Main Process (Node.js)",
        language = "javascript",
        summary = "session.setProxy per akun, cek IP via session.fetch & WebRTC Anti-Leak",
        content = ElectronMainTemplates.proxyManagerJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/geolocationProvider.js",
        category = "Main Process (Node.js)",
        language = "javascript",
        summary = "Auto-grant geolocation trip.ferizy.com + Preset 0ms Micro-Jitter",
        content = ElectronMainTemplates.geolocationProviderJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/trayManager.js",
        category = "Main Process (Node.js)",
        language = "javascript",
        summary = "System Tray Windows + ikon PNG programatik tanpa dependensi eksternal",
        content = ElectronMainTemplates.trayManagerJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/main/preflightService.js",
        category = "Main Process (Node.js)",
        language = "javascript",
        summary = "Agregator status 1 layar: Login, Proxy, Geo, RAM MB, Clock Sync & Modul Opsional",
        content = ElectronMainTemplates.preflightServiceJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/preload/shellPreload.js",
        category = "Preload Scripts",
        language = "javascript",
        summary = "ContextBridge IPC aman untuk Pre-Flight Check, Runner Fase 2 & Modul Fase 3",
        content = ElectronRendererTemplates.shellPreloadJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/preload/ferizyPreload.js",
        category = "Preload Scripts",
        language = "javascript",
        summary = "Geo 0ms + 0ms API Sniffer (fetch/XHR) + Smart Matcher + CAPTCHA Observer",
        content = ElectronRendererTemplates.ferizyPreloadJs(),
      ),
      GeneratedElectronFile(
        relativePath = "src/renderer/index.html",
        category = "Renderer UI (Pre-Flight)",
        language = "html",
        summary = "Panel default Pre-Flight 1 layar + Clock Sync + Ceklist 5 Modul Opsional",
        content = ElectronRendererTemplates.rendererIndexHtml(),
      ),
      GeneratedElectronFile(
        relativePath = "src/renderer/styles.css",
        category = "Renderer UI (Pre-Flight)",
        language = "css",
        summary = "Tema gelap maritim kompak untuk status RUNNING / MENANG / CAPTCHA / LOGOUT",
        content = ElectronRendererTemplates.rendererStylesCss(),
      ),
      GeneratedElectronFile(
        relativePath = "src/renderer/renderer.js",
        category = "Renderer UI (Pre-Flight)",
        language = "javascript",
        summary = "Render status live, Clock Sync, Web Audio Alarm 6 event & Ceklist Opsional",
        content = ElectronRendererTemplates.rendererJs(),
      ),
      GeneratedElectronFile(
        relativePath = "assets/tray-icon.svg",
        category = "Aset Ikon",
        language = "svg",
        summary = "Vektor ikon browser Ferizy Multi-Container",
        content = ElectronRendererTemplates.trayIconSvg(),
      ),
    )
  }

  private fun buildRuntimeConfigJson(
    accounts: List<AccountContainerConfig>,
    globalConfig: GlobalRuntimeConfig,
  ): String {
    val root = JSONObject()
    root.put("phase", globalConfig.activePhase)
    root.put("staggeredBootMs", globalConfig.staggeredBootMs)
    root.put("hardCapPerAccount", globalConfig.hardCapPerAccount)
    root.put("maxAccounts", accounts.size)
    root.put("ntfyTopic", globalConfig.ntfyTopic.ifBlank { "ferizy_Transsamudra05" })
    root.put("closeToTray", globalConfig.closeToTray)
    root.put("powerSaveBlocker", globalConfig.powerSaveBlocker)
    root.put("webrtcDisableNonProxiedUdp", globalConfig.webrtcDisableNonProxiedUdp)
    root.put("targetUrl", globalConfig.targetUrl)
    root.put("defaultZoomPercent", globalConfig.defaultZoomPercent)
    root.put("defaultSplitViewCount", globalConfig.defaultSplitViewCount)
    root.put("compactHeaderMode", globalConfig.compactHeaderMode)
    root.put("tab1MasterMirrorEnabled", globalConfig.tab1MasterMirrorEnabled)
    // Fase 2 confirmed parameters
    root.put("assetBlockerMode", globalConfig.assetBlockerMode.id)
    root.put("memoryWatchdogLimitMb", globalConfig.memoryWatchdogLimitMb)
    root.put("staggeredReloadDelayMs", globalConfig.staggeredReloadDelayMs)
    root.put("staggeredQueueMode", globalConfig.staggeredQueueMode.id)
    root.put("lazyAntiRevertEnabled", globalConfig.lazyAntiRevertEnabled)
    root.put("winIsolationPerTab", globalConfig.winIsolationPerTab)
    // Fase 3 confirmed parameters, optional modules & penyesuaian Fase 3
    root.put("apiSniffer0msEnabled", globalConfig.apiSniffer0msEnabled)
    root.put("serverClockSyncEnabled", globalConfig.serverClockSyncEnabled)
    root.put("autoWakeMode", globalConfig.autoWakeMode.id)
    root.put("notifLintasanExactUrl", globalConfig.notifLintasanExactUrl)
    root.put("notifKuotaExactUrl", globalConfig.notifKuotaExactUrl)
    root.put("ntfyServerUrl", globalConfig.ntfyServerUrl)
    root.put("notifDedupeSeconds", globalConfig.notifDedupeSeconds)
    root.put("audioAlarmMuted", globalConfig.audioAlarmMuted)
    root.put("notifLintasanEnabled", globalConfig.notifLintasanEnabled)
    root.put("notifKuotaEnabled", globalConfig.notifKuotaEnabled)
    root.put("notifTembusEnabled", globalConfig.notifTembusEnabled)
    root.put("notifLogoutEnabled", globalConfig.notifLogoutEnabled)
    root.put("notifProxyPutusEnabled", globalConfig.notifProxyPutusEnabled)
    root.put("notifCaptchaEnabled", globalConfig.notifCaptchaEnabled)
    root.put("gentleKeepAliveEnabled", globalConfig.gentleKeepAliveEnabled)
    root.put("keepAliveIntervalMinutes", globalConfig.keepAliveIntervalMinutes)
    root.put("captchaAutoPopEnabled", globalConfig.captchaAutoPopEnabled)
    root.put("smartTargetMatcherEnabled", globalConfig.smartTargetMatcherEnabled)
    root.put("priorityHoursCsv", globalConfig.priorityHoursCsv)
    root.put("smartMatcherServiceType", globalConfig.smartMatcherServiceType.id)
    root.put("smartMatcherFallbackNearest", globalConfig.smartMatcherFallbackNearest)
    root.put("latencyCompensationEnabled", globalConfig.latencyCompensationEnabled)
    root.put("fastRequestTimeoutMs", globalConfig.fastRequestTimeoutMs)
    root.put("advancedStopMode", globalConfig.advancedStopMode.id)
    root.put("scheduledShotEnabled", globalConfig.scheduledShotEnabled)
    root.put("scheduledShotTimeWib", globalConfig.scheduledShotTimeWib)
    // Fase 4 confirmed parameters (Log Rotation & 24h Endurance)
    root.put("logRotationPreset", globalConfig.logRotationPreset.id)
    root.put("logMaxSizeMb", globalConfig.logMaxSizeMb)
    root.put("logRetentionDays", globalConfig.logRetentionDays)
    root.put("enduranceHealthReportEnabled", globalConfig.enduranceHealthReportEnabled)
    root.put("healthSnapshotIntervalMinutes", globalConfig.healthSnapshotIntervalMinutes)
    root.put("watchdogCalibrationTargetMb", globalConfig.watchdogCalibrationTargetMb)

    val arr = JSONArray()
    accounts.forEach { acc ->
      val obj = JSONObject()
      obj.put("accountId", acc.accountId)
      obj.put("name", acc.name)
      obj.put("partition", acc.partition)
      obj.put(
        "proxy",
        JSONObject().apply {
          put("protocol", acc.proxy.protocol.scheme)
          put("host", acc.proxy.host)
          put("port", acc.proxy.port)
          put("username", acc.proxy.username)
          put("password", acc.proxy.password)
          put("ipLabel", acc.proxy.ipLabel)
        },
      )
      obj.put(
        "geolocation",
        JSONObject().apply {
          put("mode", acc.geoMode.id)
          put("preset", acc.geoPreset.id)
          put("latitude", acc.effectiveLat)
          put("longitude", acc.effectiveLng)
        },
      )
      arr.put(obj)
    }
    root.put("accounts", arr)
    return root.toString(2) + "\n"
  }
}
