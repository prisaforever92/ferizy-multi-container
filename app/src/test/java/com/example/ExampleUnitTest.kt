package com.example

import com.example.domain.AccountContainerConfig
import com.example.domain.AdvancedStopMode
import com.example.domain.AssetBlockerMode
import com.example.domain.AutoKlikExtensionSettings
import com.example.domain.AutoKlikJsonSerializer
import com.example.domain.ElectronProjectGenerator
import com.example.domain.FerizyBuildVersion
import com.example.domain.GlobalRuntimeConfig
import com.example.domain.StaggeredQueueMode
import com.example.domain.TabRunnerState
import com.example.ui.state.FerizyWorkspaceViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun defaultWorkspace_startsWith5AccountsAndEnforcesHardCap3TabsPerAccount() {
    val vm = FerizyWorkspaceViewModel()
    assertEquals(5, vm.uiState.value.accounts.size)
    assertEquals(5, vm.uiState.value.totalTabs)

    vm.addTabToAccount(1)
    vm.addTabToAccount(1)
    vm.addTabToAccount(1)

    val akun1 = vm.uiState.value.accounts.first { it.accountId == 1 }
    assertEquals(3, akun1.tabs.size)
    assertFalse(akun1.canAddTab)
  }

  @Test
  fun winIsolation_andCaptchaLogout_isolateTargetTabCorrectly() {
    val vm = FerizyWorkspaceViewModel()
    vm.addTabToAccount(1) // Akun 1 now has Tab 1 & Tab 2
    vm.toggleTabRunner(1, "akun_1_tab_1")
    vm.toggleTabRunner(1, "akun_1_tab_2")
    vm.toggleTabRunner(2, "akun_2_tab_1")

    vm.simulateTabWin(1, "akun_1_tab_1")

    var akun1 = vm.uiState.value.accounts.first { it.accountId == 1 }
    val akun2 = vm.uiState.value.accounts.first { it.accountId == 2 }
    assertEquals(TabRunnerState.WON_STOPPED, akun1.tabs[0].runnerState)
    assertEquals(TabRunnerState.RUNNING, akun1.tabs[1].runnerState)
    assertEquals(TabRunnerState.RUNNING, akun2.tabs[0].runnerState)

    vm.simulateTabCaptcha(1, "akun_1_tab_2")
    akun1 = vm.uiState.value.accounts.first { it.accountId == 1 }
    assertEquals(TabRunnerState.PAUSED_CAPTCHA, akun1.tabs[1].runnerState)
    assertEquals(TabRunnerState.RUNNING, akun2.tabs[0].runnerState)

    vm.simulateTabLogout(2, "akun_2_tab_1")
    val akun2After = vm.uiState.value.accounts.first { it.accountId == 2 }
    assertEquals(TabRunnerState.LOGOUT_STOPPED, akun2After.tabs[0].runnerState)
    assertFalse(akun2After.tabs[0].isLoggedIn)
  }

  @Test
  fun electronGenerator_producesCompleteFase4ProjectWithRecommendedDefaults() {
    val accounts = (1..5).map { AccountContainerConfig(accountId = it) }
    val global = GlobalRuntimeConfig()
    assertEquals(4, global.activePhase)
    assertEquals(AssetBlockerMode.SELECTIVE, global.assetBlockerMode)
    assertEquals(350, global.memoryWatchdogLimitMb)
    assertEquals(400, global.staggeredReloadDelayMs)
    assertEquals(StaggeredQueueMode.REACTIVE_ONLY, global.staggeredQueueMode)
    assertTrue(global.lazyAntiRevertEnabled)

    // Recommended Fase 3 optional checklist defaults + penyesuaian Fase 3
    assertTrue(global.gentleKeepAliveEnabled)
    assertTrue(global.captchaAutoPopEnabled)
    assertTrue(global.smartTargetMatcherEnabled)
    assertFalse(global.latencyCompensationEnabled)
    assertEquals(AdvancedStopMode.PER_TAB_ONLY, global.advancedStopMode)
    assertEquals(com.example.domain.AutoWakeMode.SAME_ACCOUNT, global.autoWakeMode)
    assertEquals(com.example.domain.ServiceFilterType.ALL, global.smartMatcherServiceType)
    assertTrue(global.smartMatcherFallbackNearest)
    assertFalse(global.audioAlarmMuted)
    assertFalse(global.scheduledShotEnabled)
    assertEquals("https://trip.ferizy.com/", global.notifLintasanExactUrl)
    assertEquals("https://trip.ferizy.com/ferryschedule", global.notifKuotaExactUrl)

    // Default notif HP: Lintasan/Jam Muncul, Perubahan Kuota, dan Order Sukses aktif; lainnya nonaktif
    assertTrue(global.notifLintasanEnabled)
    assertTrue(global.notifKuotaEnabled)
    assertTrue(global.notifTembusEnabled)
    assertFalse(global.notifLogoutEnabled)
    assertFalse(global.notifProxyPutusEnabled)
    assertFalse(global.notifCaptchaEnabled)

    // Fase 4 defaults: 50 MB / 7 Hari Log Rotation + 350 MB Watchdog + Health Report
    assertEquals(com.example.domain.LogRotationPreset.RECOMMENDED_50MB_7D, global.logRotationPreset)
    assertEquals(50, global.logMaxSizeMb)
    assertEquals(7, global.logRetentionDays)
    assertEquals(350, global.watchdogCalibrationTargetMb)
    assertTrue(global.enduranceHealthReportEnabled)

    val files = ElectronProjectGenerator.generateProjectFiles(accounts, global)
    assertEquals(28, files.size)
    assertFalse(files.any { it.relativePath == "src/main/bookmarkStore.js" })
    assertTrue(files.any { it.relativePath == "src/main/eventBus.js" })
    assertTrue(files.any { it.relativePath == "src/main/serverClockSync.js" })
    assertTrue(files.any { it.relativePath == "src/main/notificationCenter.js" })
    assertTrue(files.any { it.relativePath == "src/main/optionalModules.js" })
    assertTrue(files.any { it.relativePath == "src/main/logRotationManager.js" })
    assertTrue(files.any { it.relativePath == "src/main/enduranceMonitor.js" })
    assertTrue(files.any { it.relativePath == "scripts/endurance-stress-test.js" })

    val notifFile = files.first { it.relativePath == "src/main/notificationCenter.js" }
    assertTrue(notifFile.content.contains("isUrlMatchLintasan"))
    assertTrue(notifFile.content.contains("https://trip.ferizy.com/ferryschedule"))
    assertTrue(notifFile.content.contains("audioAlarmMuted"))
    assertTrue(notifFile.content.contains("shouldPushToHp"))

    val optionalFile = files.first { it.relativePath == "src/main/optionalModules.js" }
    assertTrue(optionalFile.content.contains("triggerAutoWake"))
    assertTrue(optionalFile.content.contains("tickScheduledShot"))
    assertTrue(optionalFile.content.contains("resumeAllPausedCaptchaTabs"))

    val logRotFile = files.first { it.relativePath == "src/main/logRotationManager.js" }
    assertTrue(logRotFile.content.contains("pruneOldLogs"))
    assertTrue(logRotFile.content.contains("rotateIfNeeded"))

    val enduranceFile = files.first { it.relativePath == "src/main/enduranceMonitor.js" }
    assertTrue(enduranceFile.content.contains("exportHealthReport"))
    assertTrue(enduranceFile.content.contains("runSelfCheck"))
    assertTrue(enduranceFile.content.contains("sampleSystemMetrics"))
    assertTrue(enduranceFile.content.contains("startLiveMeter"))
    assertTrue(enduranceFile.content.contains("quickCleanResources"))
    assertTrue(enduranceFile.content.contains("os.cpus()"))
    assertTrue(enduranceFile.content.contains("os.freemem()"))
    assertTrue(enduranceFile.content.contains("clearCache"))

    val pkgFile = files.first { it.relativePath == "package.json" }
    assertTrue(pkgFile.content.contains("test:endurance"))

    val autoklikFile = files.first { it.relativePath == "config/autoklik-pro-v3.2-template.json" }
    assertTrue(autoklikFile.content.contains("ferizy_Transsamudra05"))
    assertTrue(autoklikFile.content.contains("\"antiRevert\": true"))

    val sessionMgrFile = files.first { it.relativePath == "src/main/sessionManager.js" }
    assertTrue(sessionMgrFile.content.contains("did-fail-load"))
    assertTrue(sessionMgrFile.content.contains("renderLoadErrorPage"))
    assertTrue(sessionMgrFile.content.contains("context-menu"))
    assertFalse(sessionMgrFile.content.contains("openAccountBookmark"))
    assertTrue(sessionMgrFile.content.contains("https://trip.ferizy.com/order"))
    assertTrue(sessionMgrFile.content.contains("flushAllSessions"))

    val rendererJsFile = files.first { it.relativePath == "src/renderer/renderer.js" }
    assertTrue(rendererJsFile.content.contains("navigateFromBar"))
    assertTrue(rendererJsFile.content.contains("document.activeElement !== urlInput"))
    assertFalse(rendererJsFile.content.contains("openBookmarkModal"))
    assertTrue(rendererJsFile.content.contains("btn-open-notif-modal"))
    assertTrue(rendererJsFile.content.contains("btn-open-autoklik-ext"))
  }

  @Test
  fun fase3Adjustments_resumeAllPausedCaptchaTabs_and_updateConfig() {
    val vm = FerizyWorkspaceViewModel()
    vm.simulateTabCaptcha(1, "akun_1_tab_1")
    vm.simulateTabCaptcha(3, "akun_3_tab_1")
    vm.resumeAllPausedCaptchaTabs()

    val akun1 = vm.uiState.value.accounts.first { it.accountId == 1 }
    val akun3 = vm.uiState.value.accounts.first { it.accountId == 3 }
    assertEquals(TabRunnerState.RUNNING, akun1.tabs[0].runnerState)
    assertEquals(TabRunnerState.RUNNING, akun3.tabs[0].runnerState)

    val updated = vm.uiState.value.globalConfig.copy(
      autoWakeMode = com.example.domain.AutoWakeMode.ALL_ACCOUNTS,
      audioAlarmMuted = true,
      scheduledShotEnabled = true,
      scheduledShotTimeWib = "00:05:00",
      smartMatcherServiceType = com.example.domain.ServiceFilterType.EXPRESS,
    )
    vm.updateFase3Config(updated)

    val runtimeCfg = vm.uiState.value.generatedFiles.first { it.relativePath == "config/runtime-config.json" }
    assertTrue(runtimeCfg.content.contains("\"autoWakeMode\": \"all_accounts\""))
    assertTrue(runtimeCfg.content.contains("\"audioAlarmMuted\": true"))
    assertTrue(runtimeCfg.content.contains("\"scheduledShotTimeWib\": \"00:05:00\""))
    assertTrue(runtimeCfg.content.contains("\"smartMatcherServiceType\": \"express\""))
  }

  @Test
  fun fase4Endurance_updatesLogRotationPresetAndRuns24hAudit() {
    val vm = FerizyWorkspaceViewModel()
    vm.updateFase4Config(
      preset = com.example.domain.LogRotationPreset.COMPACT_20MB_3D,
      watchdogTargetMb = 320,
      healthReportEnabled = true,
      snapshotIntervalMinutes = 30,
    )
    vm.runEnduranceAuditSimulation()

    val state = vm.uiState.value
    assertEquals(20, state.globalConfig.logMaxSizeMb)
    assertEquals(3, state.globalConfig.logRetentionDays)
    assertEquals(320, state.globalConfig.watchdogCalibrationTargetMb)
    assertTrue(state.enduranceAudit != null && state.enduranceAudit!!.passed)

    val runtimeCfg = state.generatedFiles.first { it.relativePath == "config/runtime-config.json" }
    assertTrue(runtimeCfg.content.contains("\"logMaxSizeMb\": 20"))
    assertTrue(runtimeCfg.content.contains("\"logRetentionDays\": 3"))
    assertTrue(runtimeCfg.content.contains("\"watchdogCalibrationTargetMb\": 320"))
  }

  @Test
  fun autoklikSerializer_keepsInputTextInsideInputPoint() {
    val accounts = (1..5).map { AccountContainerConfig(accountId = it) }
    val json = AutoKlikJsonSerializer.buildFullExportJson(accounts, GlobalRuntimeConfig())
    assertTrue(json.contains("\"type\": \"input\""))
    assertTrue(json.contains("\"inputValue\": \"PENUMPANG AKUN 1\""))
  }

  @Test
  fun dynamicAccounts_zoomDisplaySplitView_andCaptchaNotifToggle_workEndToEnd() {
    val vm = FerizyWorkspaceViewModel()
    vm.addAccountContainer()
    vm.addAccountContainer()
    assertEquals(7, vm.uiState.value.accounts.size)
    assertEquals(7, vm.uiState.value.totalTabs)

    vm.removeAccountContainer(7)
    assertEquals(6, vm.uiState.value.accounts.size)

    vm.updateDisplayAndZoomSettings(
      zoomPercent = 80,
      splitCount = 3,
      compactHeaderMode = true,
    )
    vm.updateFase3Config(vm.uiState.value.globalConfig.copy(notifCaptchaEnabled = true))

    val state = vm.uiState.value
    assertEquals(80, state.globalConfig.defaultZoomPercent)
    assertEquals(3, state.globalConfig.defaultSplitViewCount)
    assertTrue(state.globalConfig.compactHeaderMode)
    assertTrue(state.globalConfig.notifCaptchaEnabled)

    val runtimeCfg = state.generatedFiles.first { it.relativePath == "config/runtime-config.json" }
    assertTrue(runtimeCfg.content.contains("\"maxAccounts\": 6"))
    assertTrue(runtimeCfg.content.contains("\"defaultZoomPercent\": 80"))
    assertTrue(runtimeCfg.content.contains("\"defaultSplitViewCount\": 3"))
    assertTrue(runtimeCfg.content.contains("\"compactHeaderMode\": true"))
    assertTrue(runtimeCfg.content.contains("\"notifCaptchaEnabled\": true"))

    val sessionMgr = state.generatedFiles.first { it.relativePath == "src/main/sessionManager.js" }
    assertTrue(sessionMgr.content.contains("addDynamicAccount"))
    assertTrue(sessionMgr.content.contains("setGlobalZoom"))
    assertTrue(sessionMgr.content.contains("setSplitMode"))
    assertTrue(sessionMgr.content.contains("Salin Koordinat Klik"))

    val rendererJs = state.generatedFiles.first { it.relativePath == "src/renderer/renderer.js" }
    assertTrue(rendererJs.content.contains("split-slot-bar"))
    assertTrue(rendererJs.content.contains("btn-toggle-captcha-notif"))
    assertTrue(rendererJs.content.contains("applyCompactMode"))

    val notifJs = state.generatedFiles.first { it.relativePath == "src/main/notificationCenter.js" }
    assertTrue(notifJs.content.contains("Notif CAPTCHA/Challenge sedang OFF"))
  }

  @Test
  fun autoklikMandiriExtension_savesPerContainerSettingsAndGeneratesPickerUi() {
    val vm = FerizyWorkspaceViewModel()
    vm.updateAccountExtensionSettings(
      accountId = 2,
      settings =
        AutoKlikExtensionSettings(
          enabled = true,
          loopEnabled = true,
          maxLoops = 5,
          loopDelayMs = 450,
          radiusPx = 40,
          smartMatch = false,
          antiRevert = true,
        ),
    )
    vm.updateAccountExtensionSettings(
      accountId = 3,
      settings = AutoKlikExtensionSettings(enabled = false),
    )

    val state = vm.uiState.value
    val akun2Ext = state.accounts.first { it.accountId == 2 }.extensionSettings
    val akun3Ext = state.accounts.first { it.accountId == 3 }.extensionSettings
    assertTrue(akun2Ext.loopEnabled)
    assertEquals(5, akun2Ext.maxLoops)
    assertEquals(450, akun2Ext.loopDelayMs)
    assertEquals(40, akun2Ext.radiusPx)
    assertFalse(akun2Ext.smartMatch)
    assertFalse(akun3Ext.enabled)

    val autoklikJson = state.generatedFiles.first { it.relativePath == "config/autoklik-pro-v3.2-template.json" }.content
    assertTrue(autoklikJson.contains("\"extensionName\": \"AutoKlik-v3.2-Mandiri-Extension\""))
    assertTrue(autoklikJson.contains("\"maxLoops\": 5"))
    assertTrue(autoklikJson.contains("\"loopDelayMs\": 450"))
    assertTrue(autoklikJson.contains("\"radiusPx\": 40"))

    val pointStoreJs = state.generatedFiles.first { it.relativePath == "src/main/pointProfileStore.js" }.content
    assertTrue(pointStoreJs.contains("AutoKlik-v3.2-Mandiri-Extension"))
    assertTrue(pointStoreJs.contains("autoklik-containers"))
    assertTrue(pointStoreJs.contains("getAccountStorePath"))
    assertTrue(pointStoreJs.contains("persistAccount"))
    assertTrue(pointStoreJs.contains("saveAccountSettings"))
    assertTrue(pointStoreJs.contains("importAccountProfileJson"))
    assertTrue(pointStoreJs.contains("removeLastAccountPoint"))
    assertTrue(pointStoreJs.contains("clearAccountPoints"))
    assertTrue(pointStoreJs.contains("getTabEntry"))
    assertTrue(pointStoreJs.contains("normalizeTabIndex"))
    assertTrue(pointStoreJs.contains("markersHidden"))
    assertTrue(pointStoreJs.contains("pointsLocked"))
    assertTrue(pointStoreJs.contains("mainLoopStartOrder"))
    assertTrue(pointStoreJs.contains("subLoopEnabled"))
    assertTrue(pointStoreJs.contains("popupPinEnabled"))
    assertTrue(pointStoreJs.contains("popupDelayMs"))
    assertTrue(pointStoreJs.contains(": 500)"))
    assertTrue(pointStoreJs.contains("copyTabConfig"))
    assertTrue(pointStoreJs.contains("pasteTabConfig"))
    assertTrue(pointStoreJs.contains("pasteToAllTabsInAccount"))
    assertTrue(pointStoreJs.contains("isLegacyDummyPointList"))
    assertTrue(autoklikJson.contains("\"delayAfterMs\": 500"))
    assertTrue(autoklikJson.contains("\"popupDelayMs\": 300"))
    assertTrue(pointStoreJs.contains("loopEnabled: true"))
    assertTrue(pointStoreJs.contains("maxLoops: 0"))

    val mainIndexJs = state.generatedFiles.first { it.relativePath == "src/main/index.js" }.content
    assertTrue(mainIndexJs.contains("autoklik-ext:save-dialog"))
    assertTrue(mainIndexJs.contains("autoklik-ext:open-dialog"))
    assertTrue(mainIndexJs.contains("autoklik-ext:get-overlay-sync"))
    assertTrue(mainIndexJs.contains("autoklik-ext:remove-last-point"))
    assertTrue(mainIndexJs.contains("autoklik-ext:clear-points"))
    assertTrue(mainIndexJs.contains("autoklik-ext:toggle-hidden"))
    assertTrue(mainIndexJs.contains("autoklik-ext:toggle-lock"))
    assertTrue(mainIndexJs.contains("autoklik-ext:request-edit-point"))
    assertTrue(mainIndexJs.contains("shell:switch-account"))
    assertTrue(mainIndexJs.contains("account:rename"))
    assertTrue(mainIndexJs.contains("view:toggle-account-split"))
    assertTrue(mainIndexJs.contains("system:get-metrics"))
    assertTrue(mainIndexJs.contains("system:quick-clean"))
    assertTrue(mainIndexJs.contains("system:metrics"))
    assertTrue(mainIndexJs.contains("startLiveMeter"))
    assertTrue(mainIndexJs.contains("points:copy-tab"))
    assertTrue(mainIndexJs.contains("points:paste-tab"))
    assertTrue(mainIndexJs.contains("points:paste-account-tabs"))
    assertTrue(mainIndexJs.contains("points:get-clipboard"))
    assertTrue(mainIndexJs.contains("replacePointId"))
    assertFalse(mainIndexJs.contains("autoklik-ext:page-action"))
    assertFalse(mainIndexJs.contains("bookmarks:"))

    val watchdogJs = state.generatedFiles.first { it.relativePath == "src/main/memoryWatchdog.js" }.content
    assertTrue(watchdogJs.contains("enqueueStandbyManualReload"))
    assertTrue(watchdogJs.contains("includeVisible"))

    val engineJs = state.generatedFiles.first { it.relativePath == "src/main/automationEngine.js" }.content
    assertTrue(engineJs.contains("extSettings.enabled === false"))
    assertTrue(engineJs.contains("loopDelayMs"))
    assertTrue(engineJs.contains("sendSmoothWheelSwipe"))
    assertTrue(engineJs.contains("moveCursorSmoothly"))
    assertTrue(engineJs.contains("computeHumanizedDelay"))
    assertTrue(engineJs.contains("buildKeyboardEventSpec"))
    assertTrue(engineJs.contains("readAndFocusInputLength"))
    assertTrue(engineJs.contains("toDipPoint"))
    assertTrue(engineJs.contains("(Number(x) || 0) * zf"))
    assertTrue(engineJs.contains("cross.getBoundingClientRect()"))
    assertTrue(engineJs.contains("mainLoopStartOrder"))
    assertTrue(engineJs.contains("subLoopEnabled"))
    assertTrue(engineJs.contains("triggerChar"))
    assertTrue(engineJs.contains("popupWaitMs"))
    assertTrue(engineJs.contains("clearFocusedInput"))
    assertTrue(engineJs.contains("typeTextChars"))
    assertTrue(engineJs.contains("notifyRunnerOverlayState"))
    assertTrue(engineJs.contains("notifyActivePoint"))
    assertTrue(engineJs.contains("getAccountSettings(tab.accountId, tabIdx)"))
    assertTrue(engineJs.contains("getAccountPoints(tab.accountId, tabIdx)"))
    assertFalse(engineJs.contains("ensure-value"))

    val ferizyPreloadJs = state.generatedFiles.first { it.relativePath == "src/preload/ferizyPreload.js" }.content
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:start-picker"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:picked-result"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:runner-active"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:active-point"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:dom-tap"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:dom-scroll"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:input-helper"))
    assertTrue(ferizyPreloadJs.contains("focusInputCaretAtEnd"))
    assertTrue(ferizyPreloadJs.contains("is-active-step"))
    assertTrue(ferizyPreloadJs.contains("getLayerViewportOffset"))
    assertTrue(ferizyPreloadJs.contains("showClickRippleAt"))
    assertTrue(ferizyPreloadJs.contains("lastNativeClickInfo"))
    assertFalse(ferizyPreloadJs.contains("setNativeInputValue"))
    assertTrue(ferizyPreloadJs.contains("renderAutoKlikMarkers"))
    assertFalse(ferizyPreloadJs.contains("renderAutoKlikHud"))
    assertFalse(ferizyPreloadJs.contains("ak-hud"))
    assertFalse(ferizyPreloadJs.contains("ak-pin-badge"))
    assertTrue(ferizyPreloadJs.contains("markersHidden"))
    assertTrue(ferizyPreloadJs.contains("pointsLocked"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:page-drag-point"))
    assertTrue(ferizyPreloadJs.contains("ak-arrow-head"))
    assertTrue(ferizyPreloadJs.contains("type-autofill-pin"))
    assertTrue(ferizyPreloadJs.contains("computeIconDiameterPx"))
    assertTrue(ferizyPreloadJs.contains("isDraggingMarker"))
    assertTrue(ferizyPreloadJs.contains("swipe-end"))
    assertTrue(ferizyPreloadJs.contains("pointerdown"))
    assertTrue(ferizyPreloadJs.contains("detectSelectorAt"))
    assertTrue(ferizyPreloadJs.contains("tabIndex"))
    assertTrue(ferizyPreloadJs.contains("setRunnerNoSelectLock"))
    assertTrue(ferizyPreloadJs.contains("clearPageTextSelection"))
    assertTrue(ferizyPreloadJs.contains("elementFromPointClean"))

    val rendererJs = state.generatedFiles.first { it.relativePath == "src/renderer/renderer.js" }.content
    assertTrue(rendererJs.contains("openAutoklikExtModal"))
    assertTrue(rendererJs.contains("openSinglePointEditor"))
    assertTrue(rendererJs.contains("savePointConfigDialog"))
    assertTrue(rendererJs.contains("openPointConfigDialog"))
    assertTrue(rendererJs.contains("removeLastExtPoint"))
    assertTrue(rendererJs.contains("clearExtPoints"))
    assertTrue(rendererJs.contains("toggleExtHidden"))
    assertTrue(rendererJs.contains("toggleExtLock"))
    assertTrue(rendererJs.contains("switchAccount"))
    assertTrue(rendererJs.contains("renameAccount"))
    assertTrue(rendererJs.contains("toggleAccountSplit"))
    assertTrue(rendererJs.contains("ak-tab-select"))
    assertTrue(rendererJs.contains("ep-popup-trigger-preview"))
    assertTrue(rendererJs.contains("ep-popup-delay"))
    assertTrue(rendererJs.contains("ep-type-btn"))
    assertTrue(rendererJs.contains("ep-cycle-texts-list"))
    assertTrue(rendererJs.contains("ep-swipe-summary"))
    assertTrue(rendererJs.contains("acc-autoklik-row"))
    assertTrue(rendererJs.contains("btn-acc-save-cfg"))
    assertTrue(rendererJs.contains("btn-acc-load-cfg"))
    assertTrue(rendererJs.contains("openLoadCopyModal"))
    assertTrue(rendererJs.contains("applySystemMetrics"))
    assertTrue(rendererJs.contains("runQuickClean"))
    assertFalse(rendererJs.contains("acc-split-row"))
    assertFalse(rendererJs.contains("↺ Lanjut"))

    val sessionMgrJs = state.generatedFiles.first { it.relativePath == "src/main/sessionManager.js" }.content
    assertTrue(sessionMgrJs.contains("Tambah Titik AutoKlik Tab"))
    assertTrue(sessionMgrJs.contains("cssClickX"))
    assertTrue(sessionMgrJs.contains("setModalOverlayVisible"))
    assertTrue(sessionMgrJs.contains("this.modalOverlayActive"))
    assertTrue(sessionMgrJs.contains("toggleAccountSplit"))
    assertTrue(sessionMgrJs.contains("computeEffectiveSplitMode"))
    assertTrue(sessionMgrJs.contains("switchToAccount"))
    assertTrue(sessionMgrJs.contains("renameAccount"))
    assertTrue(sessionMgrJs.contains("findTabByWebContentsId"))
    assertTrue(sessionMgrJs.contains("accountSplitStates"))
    assertTrue(sessionMgrJs.contains("getAccountSplitState"))
    assertTrue(sessionMgrJs.contains("t.accountId === activeAccId"))
    assertTrue(sessionMgrJs.contains("Mencegah tab liar"))

    assertTrue(mainIndexJs.contains("ui:set-modal-open"))
    assertTrue(mainIndexJs.contains("shell:go-back-tab"))
    assertTrue(mainIndexJs.contains("mirror:set-tab1"))
    assertTrue(mainIndexJs.contains("mirror:toggle-tab1"))
    assertTrue(mainIndexJs.contains("mirror:set-tab1-collection-split"))
    assertTrue(mainIndexJs.contains("mirror:toggle-tab1-collection-split"))
    assertTrue(mainIndexJs.contains("tab1:mirror-action"))
    assertTrue(sessionMgrJs.contains("goBackTab"))
    assertTrue(sessionMgrJs.contains("canTabGoBack"))
    assertTrue(sessionMgrJs.contains("broadcastTab1MirrorAction"))
    assertTrue(sessionMgrJs.contains("enforceTab1MirrorGuardForActiveTab"))
    assertTrue(sessionMgrJs.contains("getMirrorTargetTab1List"))
    assertTrue(sessionMgrJs.contains("tab1CollectionSplitActive"))
    assertTrue(sessionMgrJs.contains("setTab1CollectionSplit"))
    assertTrue(sessionMgrJs.contains("toggleTab1CollectionSplit"))

    val shellPreloadJs = state.generatedFiles.first { it.relativePath == "src/preload/shellPreload.js" }.content
    assertTrue(shellPreloadJs.contains("setModalOpen"))
    assertTrue(shellPreloadJs.contains("goBackTab"))
    assertTrue(shellPreloadJs.contains("setTab1Mirror"))
    assertTrue(shellPreloadJs.contains("toggleTab1Mirror"))
    assertTrue(shellPreloadJs.contains("setTab1CollectionSplit"))
    assertTrue(shellPreloadJs.contains("toggleTab1CollectionSplit"))
    assertTrue(shellPreloadJs.contains("toggleAccountSplit"))
    assertTrue(shellPreloadJs.contains("renameAccount"))
    assertTrue(shellPreloadJs.contains("switchAccount"))
    assertTrue(shellPreloadJs.contains("removeLastExtPoint"))
    assertTrue(shellPreloadJs.contains("clearExtPoints"))
    assertTrue(shellPreloadJs.contains("toggleExtHidden"))
    assertTrue(shellPreloadJs.contains("toggleExtLock"))
    assertTrue(shellPreloadJs.contains("onOpenPointEditor"))
    assertTrue(shellPreloadJs.contains("getSystemMetrics"))
    assertTrue(shellPreloadJs.contains("runQuickClean"))
    assertTrue(shellPreloadJs.contains("onSystemMetrics"))
    assertTrue(shellPreloadJs.contains("copyTabConfig"))
    assertTrue(shellPreloadJs.contains("pasteTabConfig"))
    assertTrue(shellPreloadJs.contains("pasteToAllTabsInAccount"))
    assertTrue(shellPreloadJs.contains("getClipboardSummary"))
    assertFalse(shellPreloadJs.contains("addBookmark"))

    assertTrue(ferizyPreloadJs.contains("canEmitTab1MasterMirror"))
    assertTrue(ferizyPreloadJs.contains("buildFastMirrorSelector"))
    assertTrue(ferizyPreloadJs.contains("suppressMirrorEmitUntil"))
    assertTrue(ferizyPreloadJs.contains("passThroughClickToWebsiteAt"))
    assertTrue(ferizyPreloadJs.contains("buildMarkersRenderSignature"))
    assertTrue(ferizyPreloadJs.contains("findInteractiveElementInCircle"))
    assertFalse(ferizyPreloadJs.contains("kind: 'mousemove'"))
    assertTrue(ferizyPreloadJs.contains("tab1:mirror-action"))
    assertTrue(ferizyPreloadJs.contains("tab1:mirror-apply"))
    assertTrue(ferizyPreloadJs.contains("setNativeControlValue"))
    assertTrue(sessionMgrJs.contains("this.mirrorSuppressUntil"))
    assertTrue(sessionMgrJs.contains("this.attachedTabIds"))
    assertTrue(sessionMgrJs.contains("this.lastAppliedBounds"))
    assertTrue(engineJs.contains("iconDiameter"))
    assertTrue(engineJs.contains("iconRadius"))

    assertTrue(rendererJs.contains("openModalById"))
    assertTrue(rendererJs.contains("closeModalById"))
    assertTrue(rendererJs.contains("btn-back-active"))
    assertTrue(rendererJs.contains("url-preset-select"))
    assertTrue(rendererJs.contains("btn-toggle-tab1-mirror"))
    assertTrue(rendererJs.contains("btn-toggle-tab1-split"))
    assertTrue(rendererJs.contains("tab1-collection-card"))
    assertTrue(rendererJs.contains("lastSentHeaderHeight"))

    val pkgJson = state.generatedFiles.first { it.relativePath == "package.json" }.content
    assertEquals("V5.8", FerizyBuildVersion.RELEASE_TAG)
    assertTrue(pkgJson.contains("\"version\": \"${FerizyBuildVersion.SEMVER}\""))
    assertFalse(mainIndexJs.contains("getTabState("))
    assertTrue(mainIndexJs.contains("automationEngine.getRunnerState"))
    assertTrue(sessionMgrJs.contains("view.webContents.setWebRTCIPHandlingPolicy"))
    assertTrue(sessionMgrJs.contains("disconnectMirrorForAutoKlikTab"))
    assertTrue(sessionMgrJs.contains("toggleTabGroupMirror"))
    assertTrue(sessionMgrJs.contains("toggleTabCollectionSplit"))
    assertTrue(ferizyPreloadJs.contains("ak-floating-runner-pill"))
    assertTrue(ferizyPreloadJs.contains("autoklik-ext:floating-toggle-runner"))
    assertTrue(ferizyPreloadJs.contains("POPUP_OPTION_SELECTOR"))
    val builderYml = state.generatedFiles.first { it.relativePath == "electron-builder.yml" }.content
    assertTrue(builderYml.contains(FerizyBuildVersion.RELEASE_TAG))
    val stylesCss = state.generatedFiles.first { it.relativePath == "src/renderer/styles.css" }.content
    assertTrue(stylesCss.contains(".pill-danger"))
    assertTrue(stylesCss.contains("#btn-quick-clean.is-over"))
    assertTrue(stylesCss.contains(".url-preset-select"))
    assertTrue(stylesCss.contains(".tab1-collection-card"))
    val indexHtml = state.generatedFiles.first { it.relativePath == "src/renderer/index.html" }.content
    assertTrue(indexHtml.contains(FerizyBuildVersion.RELEASE_TAG))
    assertTrue(indexHtml.contains("sys-meter-pill"))
    assertTrue(indexHtml.contains("btn-quick-clean"))
    assertTrue(indexHtml.contains("btn-back-active"))
    assertTrue(indexHtml.contains("btn-toggle-tab1-mirror"))
    assertTrue(indexHtml.contains("btn-toggle-tab1-split"))
    assertTrue(indexHtml.contains("tab1-collection-card"))
    assertTrue(indexHtml.contains("tab2-collection-card"))
    assertTrue(indexHtml.contains("tab3-collection-card"))
    assertTrue(indexHtml.contains("url-preset-select"))
    assertTrue(indexHtml.contains("ferizy-url-presets"))
    assertTrue(indexHtml.contains("https://trip.ferizy.com/"))
    assertTrue(indexHtml.contains("https://trip.ferizy.com/order"))
    assertTrue(indexHtml.contains("ak-edit-point-modal"))
    assertTrue(indexHtml.contains("ak-tab-select"))
    assertTrue(indexHtml.contains("cfg-acc-name"))
    assertTrue(indexHtml.contains("ep-popup-trigger-preview"))
    assertTrue(indexHtml.contains("ep-popup-delay"))
    assertTrue(indexHtml.contains("ep-type-btn"))
    assertTrue(indexHtml.contains("ep-cycle-texts-list"))
    assertTrue(indexHtml.contains("ep-swipe-summary"))
    assertTrue(indexHtml.contains("ak-load-copy-modal"))
    assertTrue(indexHtml.contains("btn-lcm-copy-tab"))
    assertTrue(indexHtml.contains("btn-lcm-paste-tab"))
    assertFalse(indexHtml.contains("bookmark-modal"))

    // Verify per-account Save/Open JSON isolation, defaults, rename, Tab 1 Master Mirror, and Tab 2 /order URL on Android ViewModel
    assertTrue(vm.uiState.value.globalConfig.tab1MasterMirrorEnabled)
    vm.toggleTab1MasterMirror()
    assertFalse(vm.uiState.value.globalConfig.tab1MasterMirrorEnabled)
    vm.toggleTab1MasterMirror()
    assertTrue(vm.uiState.value.globalConfig.tab1MasterMirrorEnabled)

    vm.renameAccount(1, "Utama Express")
    vm.addTabToAccount(1)
    val akun1Renamed = vm.uiState.value.accounts.first { it.accountId == 1 }
    assertEquals("Utama Express", akun1Renamed.name)
    assertEquals("https://trip.ferizy.com", akun1Renamed.tabs[0].url)
    assertEquals("https://trip.ferizy.com/order", akun1Renamed.tabs[1].url)
    assertEquals(500, akun1Renamed.points.first().delayAfterMs)
    assertEquals(300, akun1Renamed.points.first().popupDelayMs)
    assertEquals(30, akun1Renamed.points[2].typingSpeedMs)

    val exportedAkun2 = AutoKlikJsonSerializer.buildSingleAccountExportJson(
      state.accounts.first { it.accountId == 2 },
      state.globalConfig,
    )
    vm.importAccountJson(4, exportedAkun2)
    val akun1After = vm.uiState.value.accounts.first { it.accountId == 1 }
    val akun4After = vm.uiState.value.accounts.first { it.accountId == 4 }
    assertTrue(akun1After.extensionSettings.loopEnabled)
    assertEquals(0, akun1After.extensionSettings.maxLoops)
    assertTrue(akun4After.extensionSettings.loopEnabled)
    assertEquals(5, akun4After.extensionSettings.maxLoops)
    assertEquals(40, akun4After.extensionSettings.radiusPx)

    // Materialize ready-to-run Electron V5.8 folder + ZIP in workspace root for AI Studio Export / GitHub Push
    val accountsDefault = (1..5).map { AccountContainerConfig(accountId = it) }
    val globalDefault = GlobalRuntimeConfig()
    val exportFiles = ElectronProjectGenerator.generateProjectFiles(accountsDefault, globalDefault)
    val cwdDir = java.io.File(System.getProperty("user.dir") ?: ".")
    val workspaceRoot = if (cwdDir.name == "app") (cwdDir.parentFile ?: cwdDir) else cwdDir
    java.io.File(workspaceRoot, "ferizy-multi-container-v4.8").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v4.8.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v4.9").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v4.9.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.0").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.0.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.1").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.1.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.2").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.2.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.3").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.3.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.4").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.4.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.5").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.5.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.6").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.6.zip").delete()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.7").deleteRecursively()
    java.io.File(workspaceRoot, "ferizy-multi-container-v5.7.zip").delete()
    val outRoot = java.io.File(workspaceRoot, FerizyBuildVersion.ZIP_BASENAME)
    exportFiles.forEach { genFile ->
      val target = java.io.File(outRoot, genFile.relativePath)
      target.parentFile?.mkdirs()
      target.writeText(genFile.content, Charsets.UTF_8)
    }
    val zipFile = java.io.File(workspaceRoot, "${FerizyBuildVersion.ZIP_BASENAME}.zip")
    java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
      exportFiles.forEach { genFile ->
        val entryPath = "${FerizyBuildVersion.ZIP_BASENAME}/${genFile.relativePath}"
        zos.putNextEntry(java.util.zip.ZipEntry(entryPath))
        zos.write(genFile.content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
      }
    }
    assertTrue(java.io.File(outRoot, "package.json").exists())
    assertTrue(zipFile.exists())
  }
}

