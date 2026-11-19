package com.example.domain

object ElectronMainTemplates {

  fun packageJson(): String =
    """
{
  "name": "ferizy-multi-container-browser",
  "version": "${FerizyBuildVersion.SEMVER}",
  "description": "Browser Khusus Multi-Container Ferizy ${FerizyBuildVersion.RELEASE_TAG} (Electron + WebContentsView, Fase 1-4: Sniffer 0ms, Event Bus <1ms, Clock Sync, Notif Terpusat, Log Rotation & Uji Ketahanan 24 Jam)",
  "main": "src/main/index.js",
  "scripts": {
    "start": "electron .",
    "build:win": "electron-builder --win --x64",
    "pack": "electron-builder --dir",
    "test:endurance": "node scripts/endurance-stress-test.js"
  },
  "author": "FerizyForge Workspace",
  "license": "PRIVATE",
  "devDependencies": {
    "electron": "^33.2.0",
    "electron-builder": "^25.1.8"
  }
}
    """.trimIndent() + "\n"

  fun electronBuilderYml(): String =
    """
appId: com.ferizy.multicontainer.browser
productName: Ferizy Multi-Container
directories:
  output: dist
  buildResources: assets
files:
  - src/**/*
  - config/**/*
  - assets/**/*
  - package.json
win:
  signAndEditExecutable: false
  target:
    - target: nsis
      arch:
        - x64
    - target: portable
      arch:
        - x64
nsis:
  artifactName: "${'$'}{productName}-Setup-${FerizyBuildVersion.RELEASE_TAG}-${'$'}{arch}.${'$'}{ext}"
  oneClick: false
  allowToChangeInstallationDirectory: true
  createDesktopShortcut: true
  createStartMenuShortcut: true
portable:
  artifactName: "${'$'}{productName}-Portable-${FerizyBuildVersion.RELEASE_TAG}-${'$'}{arch}.${'$'}{ext}"
    """.trimIndent() + "\n"

  fun readmeWindows(globalConfig: GlobalRuntimeConfig): String =
    """
# Browser Khusus Multi-Container Ferizy — ${FerizyBuildVersion.RELEASE_TAG} (Fase 1–4)

Proyek Electron desktop Windows (x64) hasil generate **FerizyForge** sesuai Master Blueprint **Fase 1–4**.

## Spesifikasi Target PC Acuan
- **CPU**: Intel Core i7-4790 (4C/8T)
- **RAM**: 16 GB (kapasitas optimal 5 Akun × 3 Tab = 15 Tab)
- **Storage**: 256 GB SSD

## Fitur Aktif (Fase 1–4)
1. **Multi-Container (`persist:akun_1..5`) + Hard Cap 3/3 + Pre-Flight Check 1 Layar + Close-to-Tray + `powerSaveBlocker`**.
2. **Auto-Grant Geolocation `0ms` + Multi-Proxy + WebRTC Anti-Leak + Pembersihan `Electron/`**.
3. **Mesin `sendInputEvent` (`isTrusted: true`) + Isolasi Menang Per-Tab `0ms` + Asset Blocker (${globalConfig.assetBlockerMode.label}) + Watchdog ${globalConfig.watchdogCalibrationTargetMb}MB + Staggered Queue ${globalConfig.staggeredReloadDelayMs}ms Reaktif**.
4. **0ms API Sniffer + Main Process Event Bus (`<1ms`) + Server Clock Sync + Scheduled Shot Timer**.
5. **Notifikasi Terpusat Main Process (`ntfy.sh`: `${globalConfig.ntfyTopic}` + Windows Toast + Audio Alarm + Deduplikasi ${globalConfig.notifDedupeSeconds}s)**:
   - **Lintasan Dibuka**: Aktif **HANYA** saat konfigurasi berjalan di `https://trip.ferizy.com/`.
   - **Kuota Muncul**: Aktif **HANYA** saat konfigurasi berjalan di `https://trip.ferizy.com/ferryschedule`.
   - **Tembus Verifikasi**, **Logout (/login atau 401)**, **Proxy Putus**, dan **CAPTCHA** aktif otomatis.
6. **Ceklist Modul Opsional (Default Rekomendasi)**:
   - `[${if (globalConfig.gentleKeepAliveEnabled) "x" else " "}]` Gentle Session Keep-Alive (~${globalConfig.keepAliveIntervalMinutes} menit bergiliran, tanpa reload halaman)
   - `[${if (globalConfig.captchaAutoPopEnabled) "x" else " "}]` Detektor CAPTCHA + Auto-Pop dari Tray
   - `[${if (globalConfig.smartTargetMatcherEnabled) "x" else " "}]` Smart Target Matcher (Jam Prioritas: `${globalConfig.priorityHoursCsv}`)
   - `[${if (globalConfig.latencyCompensationEnabled) "x" else " "}]` Latency Compensation + Fast Request Timeout (`${globalConfig.fastRequestTimeoutMs} ms`)
   - `[${if (globalConfig.advancedStopMode != AdvancedStopMode.PER_TAB_ONLY) "x" else " "}]` Opsi Stop Lanjutan (`${globalConfig.advancedStopMode.label}`)
7. **Fase 4 — Uji Ketahanan 24 Jam & Log Rotation (SSD 256 GB Aman)**:
   - **Log Rotation**: Maksimal **${globalConfig.logMaxSizeMb} MB** per file, retensi otomatis **${globalConfig.logRetentionDays} hari** (`${globalConfig.logRotationPreset.label}`).
   - **Health Report JSON**: Snapshot ketahanan 15 tab setiap `${globalConfig.healthSnapshotIntervalMinutes}` menit (`health-report-24h.json`).
   - **Kalibrasi Watchdog Standby**: `${globalConfig.watchdogCalibrationTargetMb} MB` (Reaktif Opsi A — tab aktif/menang tidak pernah ter-reset).

## Cara Menjalankan, Simulasi 24 Jam & Build `.exe` di Windows
```bat
npm install
npm run test:endurance
npm start
# Build installer & portable .exe:
npm run build:win
```
    """.trimIndent() + "\n"

  fun mainIndexJs(): String =
    """
const fs = require('fs');
const path = require('path');
const { app, BrowserWindow, ipcMain, powerSaveBlocker, shell, Menu, dialog } = require('electron');
const { SessionManager } = require('./sessionManager');
const { ProxyManager } = require('./proxyManager');
const { GeolocationProvider } = require('./geolocationProvider');
const { TrayManager } = require('./trayManager');
const { PreflightService } = require('./preflightService');
const { AssetBlocker } = require('./assetBlocker');
const { PointProfileStore } = require('./pointProfileStore');
const { AutomationEngine } = require('./automationEngine');
const { MemoryWatchdog } = require('./memoryWatchdog');
const { MainEventBus } = require('./eventBus');
const { ServerClockSync } = require('./serverClockSync');
const { NotificationCenter } = require('./notificationCenter');
const { OptionalModulesManager } = require('./optionalModules');
const { LogRotationManager } = require('./logRotationManager');
const { EnduranceMonitor } = require('./enduranceMonitor');

const CONFIG_PATH = path.join(__dirname, '../../config/runtime-config.json');
const AUTOKLIK_PATH = path.join(__dirname, '../../config/autoklik-pro-v3.2-template.json');

function loadJsonSafe(filePath, fallback) {
  try {
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch (err) {
    console.error('[Config] Gagal membaca', filePath, err.message);
    return fallback;
  }
}

const runtimeConfig = loadJsonSafe(CONFIG_PATH, {
  activePhase: 4,
  staggeredBootMs: 300,
  hardCapPerAccount: 3,
  maxAccounts: 5,
  ntfyTopic: 'ferizy_Transsamudra05',
  closeToTray: true,
  powerSaveBlocker: true,
  assetBlockerMode: 'selective',
  memoryWatchdogLimitMb: 350,
  watchdogCalibrationTargetMb: 350,
  staggeredReloadDelayMs: 400,
  staggeredQueueMode: 'reactive_only',
  lazyAntiRevertEnabled: true,
  winIsolationPerTab: true,
  gentleKeepAliveEnabled: true,
  keepAliveIntervalMinutes: 12,
  captchaAutoPopEnabled: true,
  smartTargetMatcherEnabled: true,
  priorityHoursCsv: '08:00,10:00,12:00,14:00,20:00',
  latencyCompensationEnabled: false,
  fastRequestTimeoutMs: 4500,
  advancedStopMode: 'per_tab',
  notifDedupeSeconds: 10,
  notifLintasanEnabled: true,
  notifKuotaEnabled: true,
  notifTembusEnabled: true,
  notifLogoutEnabled: false,
  notifProxyPutusEnabled: false,
  notifCaptchaEnabled: false,
  logMaxSizeMb: 50,
  logRetentionDays: 7,
  enduranceHealthReportEnabled: true,
  healthSnapshotIntervalMinutes: 60,
  defaultZoomPercent: 100,
  defaultSplitViewCount: 1,
  compactHeaderMode: false,
  targetUrl: 'https://trip.ferizy.com',
  accounts: []
});

const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
}

// Optimasi Kecepatan Jaringan Chromium 200 Mbps (0ms Direct Navigation, QUIC & Parallel Socket Pooling)
try {
  app.commandLine.appendSwitch('enable-quic');
  app.commandLine.appendSwitch('enable-parallel-downloading');
  app.commandLine.appendSwitch('max-connections-per-server', '16');
  app.commandLine.appendSwitch('disable-renderer-backgrounding');
  app.commandLine.appendSwitch('disable-background-timer-throttling');
} catch (_) {}

let mainWindow = null;
let powerSaveId = null;
let headerHeight = 264;

const eventBus = new MainEventBus();
const logRotation = new LogRotationManager(runtimeConfig);
const serverClockSync = new ServerClockSync(runtimeConfig);
const notificationCenter = new NotificationCenter(runtimeConfig, () => mainWindow);
notificationCenter.attachLogRotation(logRotation);
const proxyManager = new ProxyManager(runtimeConfig, notificationCenter);
const geoProvider = new GeolocationProvider(runtimeConfig);
const assetBlocker = new AssetBlocker(runtimeConfig);
const pointStore = new PointProfileStore(AUTOKLIK_PATH);
const sessionManager = new SessionManager(runtimeConfig, proxyManager, geoProvider, assetBlocker, serverClockSync);
const automationEngine = new AutomationEngine(
  runtimeConfig,
  sessionManager,
  pointStore,
  () => broadcastState(),
  eventBus,
  serverClockSync,
  notificationCenter
);
const optionalModules = new OptionalModulesManager(
  runtimeConfig,
  sessionManager,
  automationEngine,
  notificationCenter,
  serverClockSync,
  eventBus,
  (tabId) => {
    showMainWindow();
    if (tabId) sessionManager.switchTab(tabId);
    broadcastState();
  }
);
automationEngine.attachOptionalModules(optionalModules);

const memoryWatchdog = new MemoryWatchdog(runtimeConfig, sessionManager, automationEngine, () => broadcastState());
memoryWatchdog.attachLogRotation(logRotation);
const enduranceMonitor = new EnduranceMonitor(
  runtimeConfig,
  sessionManager,
  automationEngine,
  memoryWatchdog,
  optionalModules,
  serverClockSync,
  logRotation
);
const preflightService = new PreflightService(
  runtimeConfig,
  sessionManager,
  proxyManager,
  geoProvider,
  assetBlocker,
  pointStore,
  automationEngine,
  memoryWatchdog,
  serverClockSync,
  notificationCenter,
  optionalModules,
  logRotation,
  enduranceMonitor
);
const trayManager = new TrayManager(() => showMainWindow(), () => quitAppCleanly());

sessionManager.attachAutomationEngine(automationEngine);
sessionManager.attachPointStore(pointStore);

function showMainWindow() {
  if (!mainWindow) return;
  if (mainWindow.isMinimized()) mainWindow.restore();
  mainWindow.show();
  mainWindow.focus();
  if (!mainWindow.isDestroyed() && enduranceMonitor) {
    mainWindow.webContents.send('system:metrics', enduranceMonitor.sampleSystemMetrics());
  }
}

async function quitAppCleanly() {
  app.isQuitting = true;
  memoryWatchdog.stop();
  optionalModules.stop();
  enduranceMonitor.stop();
  logRotation.stop();
  if (sessionManager && typeof sessionManager.flushAllSessions === 'function') {
    await sessionManager.flushAllSessions();
  }
  if (powerSaveId !== null && powerSaveBlocker.isStarted(powerSaveId)) {
    powerSaveBlocker.stop(powerSaveId);
  }
  trayManager.destroy();
  app.quit();
}

function pushOverlayStateToWebViews(snapshot) {
  if (!sessionManager || !sessionManager.tabs || !snapshot || !Array.isArray(snapshot.accounts)) return;
  const groupCfg = snapshot.tabGroupsMirror || {};
  for (const tab of sessionManager.tabs.values()) {
    if (!tab.view || tab.view.webContents.isDestroyed()) continue;
    const accSnap = snapshot.accounts.find((a) => a.accountId === tab.accountId);
    if (!accSnap) continue;
    const tabSnap = accSnap.tabs.find((t) => t.tabId === tab.tabId);
    const tIdx = tab.indexInAccount || (tabSnap && tabSnap.indexInAccount) || 1;
    const runnerState = (tabSnap && tabSnap.runnerState) || 'IDLE';
    const isRunningAutoKlik = runnerState === 'RUNNING';
    const grpInfo = groupCfg[tIdx] || {
      mirrorEnabled: tIdx === 1 ? Boolean(snapshot.tab1MirrorEnabled) : false,
      mirrorActive: tIdx === 1 ? Boolean(snapshot.tab1MirrorActive) : false
    };
    const isMasterForGroup = Boolean(
      !isRunningAutoKlik &&
      grpInfo.mirrorActive &&
      tab.tabId === snapshot.activeTabId &&
      Number(snapshot.activeTabIndex) === tIdx
    );
    try {
      tab.view.webContents.send('autoklik-ext:overlay-state', {
        accountId: tab.accountId,
        accountName: accSnap.name || `Akun ${'$'}{tab.accountId}`,
        tabId: tab.tabId,
        tabIndex: tIdx,
        runnerState,
        isMirrorDisconnectedByAutoKlik: isRunningAutoKlik,
        tab1MirrorEnabled: Boolean(grpInfo.mirrorEnabled && !isRunningAutoKlik),
        tab1MirrorActive: isMasterForGroup,
        tabGroupMirrorEnabled: Boolean(grpInfo.mirrorEnabled && !isRunningAutoKlik),
        tabGroupMirrorActive: isMasterForGroup,
        points: (tabSnap && Array.isArray(tabSnap.points)) ? tabSnap.points : [],
        extensionSettings: (tabSnap && tabSnap.extensionSettings) || accSnap.extensionSettings || {},
        storeFile: accSnap.storeFile || `akun_${'$'}{tab.accountId}.json`
      });
    } catch (_) {}
  }
}

function broadcastState() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  const snapshot = preflightService.getFullSnapshot();
  mainWindow.webContents.send('shell:state-updated', snapshot);
  pushOverlayStateToWebViews(snapshot);
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1480,
    height: 950,
    minWidth: 1120,
    minHeight: 760,
    title: 'Ferizy Multi-Container Browser • ${FerizyBuildVersion.RELEASE_TAG}',
    backgroundColor: '#070e1b',
    webPreferences: {
      preload: path.join(__dirname, '../preload/shellPreload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  mainWindow.setMenuBarVisibility(false);
  mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));

  mainWindow.webContents.on('context-menu', (_event, params) => {
    const canCopy = params.editFlags.canCopy || Boolean(params.selectionText);
    const canSelect = params.editFlags.canSelectAll || params.isEditable || Boolean(params.selectionText);
    if (!params.isEditable && !canCopy && !params.editFlags.canPaste) return;
    const menu = Menu.buildFromTemplate([
      { label: 'Potong (Cut)', role: 'cut', enabled: params.editFlags.canCut },
      { label: 'Salin (Copy)', role: 'copy', enabled: canCopy },
      { label: 'Tempel (Paste)', role: 'paste', enabled: params.editFlags.canPaste },
      { type: 'separator' },
      { label: 'Pilih Semua', role: 'selectAll', enabled: canSelect }
    ]);
    menu.popup({ window: mainWindow });
  });

  sessionManager.attachMainWindow(mainWindow, () => headerHeight, () => broadcastState());

  mainWindow.on('resize', () => {
    sessionManager.layoutActiveView();
  });

  mainWindow.on('close', (event) => {
    if (!app.isQuitting && runtimeConfig.closeToTray) {
      event.preventDefault();
      mainWindow.hide();
      trayManager.notifyHiddenToTray();
    }
  });
}

app.on('login', (event, webContents, details, authInfo, callback) => {
  if (authInfo && authInfo.isProxy) {
    const creds = proxyManager.getCredentialsForWebContents(webContents.id);
    if (creds && creds.username) {
      event.preventDefault();
      callback(creds.username, creds.password || '');
      return;
    }
  }
  callback();
});

app.on('second-instance', () => {
  showMainWindow();
});

function registerIpcHandlers() {
  ipcMain.handle('shell:get-state', () => preflightService.getFullSnapshot());

  ipcMain.handle('shell:set-header-height', (_evt, nextHeight) => {
    if (typeof nextHeight === 'number' && nextHeight >= 80 && nextHeight <= 680) {
      const rounded = Math.round(nextHeight);
      if (Math.abs(rounded - headerHeight) >= 2) {
        headerHeight = rounded;
        sessionManager.layoutActiveView();
      }
    }
    return headerHeight;
  });

  ipcMain.handle('shell:create-tab', (_evt, accountId, url) => {
    const res = sessionManager.createTab(accountId, url || null);
    broadcastState();
    return res;
  });

  ipcMain.handle('shell:switch-tab', (_evt, tabId) => {
    const ok = sessionManager.switchTab(tabId);
    broadcastState();
    return ok;
  });

  ipcMain.handle('shell:switch-account', (_evt, accountId) => {
    const res = sessionManager.switchToAccount(accountId);
    broadcastState();
    return res;
  });

  ipcMain.handle('shell:close-tab', (_evt, tabId) => {
    const res = sessionManager.closeTab(tabId);
    broadcastState();
    return res;
  });

  ipcMain.handle('shell:reload-tab', (_evt, tabId) => sessionManager.reloadTab(tabId));
  ipcMain.handle('shell:go-back-tab', (_evt, tabId) => {
    const res = sessionManager.goBackTab(tabId);
    broadcastState();
    return res;
  });
  ipcMain.handle('shell:navigate-tab', (_evt, tabId, targetUrl) => {
    const res = sessionManager.navigateTab(tabId, targetUrl);
    broadcastState();
    return res;
  });

  ipcMain.handle('shell:update-proxy', async (_evt, accountId, proxyCfg) => {
    const result = await proxyManager.updateAccountProxy(accountId, proxyCfg);
    broadcastState();
    return result;
  });

  ipcMain.handle('shell:check-proxy-ip', async (_evt, accountId) => {
    const result = await proxyManager.checkExternalIp(accountId);
    broadcastState();
    return result;
  });

  ipcMain.handle('shell:update-geo', (_evt, accountId, geoCfg) => {
    const result = geoProvider.updateAccountGeo(accountId, geoCfg);
    broadcastState();
    return result;
  });

  ipcMain.handle('shell:refresh-preflight', async () => {
    await preflightService.refreshLoginCookies();
    const snap = preflightService.getFullSnapshot();
    broadcastState();
    return snap;
  });

  // Fase 2 IPC
  ipcMain.handle('automation:start-tab', async (_evt, tabId) => {
    const res = await automationEngine.startTab(tabId, { force: true });
    sessionManager.disconnectMirrorForAutoKlikTab(tabId);
    broadcastState();
    return res;
  });
  ipcMain.handle('automation:stop-tab', (_evt, tabId) => {
    const res = automationEngine.stopTab(tabId);
    broadcastState();
    return res;
  });
  ipcMain.handle('automation:reset-won-tab', (_evt, tabId) => automationEngine.resetWonTab(tabId));

  ipcMain.handle('points:copy-account', (_evt, accountId, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const count = pointStore.copyFromAccount(accountId, tIdx);
    broadcastState();
    return { ok: true, count, tabIndex: tIdx };
  });

  ipcMain.handle('points:paste-all', (_evt, sourceAccountId, sourceTabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(sourceAccountId, sourceTabIndex);
    const res = pointStore.pasteToAllAccounts(sourceAccountId, sessionManager.getAccountIds(), tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('points:copy-tab', (_evt, accountId, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.copyTabConfig(accountId, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('points:paste-tab', (_evt, targetAccountId, targetTabIndex = null, sourceAccountId = null, sourceTabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(targetAccountId, targetTabIndex);
    const sIdx = sourceAccountId ? sessionManager.resolveTabIndex(sourceAccountId, sourceTabIndex) : null;
    const res = pointStore.pasteTabConfig(targetAccountId, tIdx, sourceAccountId, sIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('points:paste-account-tabs', (_evt, targetAccountId, sourceAccountId = null, sourceTabIndex = null) => {
    const sIdx = sourceAccountId ? sessionManager.resolveTabIndex(sourceAccountId, sourceTabIndex) : null;
    const res = pointStore.pasteToAllTabsInAccount(targetAccountId, sourceAccountId, sIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('points:get-clipboard', () => pointStore.getClipboardSummary());

  ipcMain.handle('watchdog:reload-standby', () => {
    memoryWatchdog.enqueueStandbyManualReload();
    return { ok: true };
  });

  ipcMain.handle('asset-blocker:set-mode', (_evt, mode) => {
    const next = assetBlocker.setMode(mode);
    broadcastState();
    return { ok: true, mode: next };
  });

  // Fase 3 IPC: Ceklist Opsional, Auto-Wake, Scheduled Shot, Notif Granular, Resume CAPTCHA
  ipcMain.handle('optional:update-flags', (_evt, nextFlags) => {
    const updated = optionalModules.updateFlags(nextFlags);
    broadcastState();
    return updated;
  });

  ipcMain.handle('clock:update-schedule', (_evt, nextSched) => {
    const updated = optionalModules.updateSchedule(nextSched);
    broadcastState();
    return updated;
  });

  ipcMain.handle('notif:update-settings', (_evt, nextNotif) => {
    const updated = notificationCenter.updateSettings(nextNotif);
    broadcastState();
    return updated;
  });

  ipcMain.handle('automation:resume-captcha-all', () => {
    const res = optionalModules.resumeAllPausedCaptchaTabs();
    broadcastState();
    return res;
  });

  ipcMain.handle('notif:test-ntfy', async () => {
    const res = await notificationCenter.dispatch('TEMBUS_VERIFIKASI', {
      accountId: 1,
      force: true,
      message: 'Tes Notifikasi ntfy + Toast + Alarm Audio dari Ferizy Multi-Container Fase 4'
    });
    broadcastState();
    return res;
  });

  // Fase 4 IPC: Log Rotation & Uji Ketahanan 24 Jam
  ipcMain.handle('fase4:update-config', (_evt, nextCfg) => {
    const res = enduranceMonitor.updateConfig(nextCfg || {});
    broadcastState();
    return res;
  });

  ipcMain.handle('endurance:run-self-check', () => {
    const res = enduranceMonitor.runSelfCheck();
    broadcastState();
    return res;
  });

  ipcMain.handle('endurance:export-health-report', () => {
    const res = enduranceMonitor.exportHealthReport();
    broadcastState();
    return res;
  });

  ipcMain.handle('system:get-metrics', () => enduranceMonitor.sampleSystemMetrics());

  ipcMain.handle('system:quick-clean', async () => {
    const res = await enduranceMonitor.quickCleanResources();
    if (res && res.after && mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('system:metrics', res.after);
    }
    broadcastState();
    return res;
  });

  ipcMain.handle('logs:prune-now', () => {
    const res = logRotation.pruneOldLogs();
    broadcastState();
    return res;
  });

  ipcMain.handle('logs:open-folder', async () => {
    logRotation.ensureLogsDir();
    await shell.openPath(logRotation.logsDir);
    return { ok: true, logsDir: logRotation.logsDir };
  });

  // Dynamic Akun (>5), Rename Akun, Global Zoom, dan Split View IPC
  ipcMain.handle('accounts:add', async () => {
    const res = await sessionManager.addDynamicAccount();
    await preflightService.refreshLoginCookies();
    optionalModules.restartKeepAliveTimer();
    broadcastState();
    return res;
  });

  ipcMain.handle('accounts:remove', async (_evt, accountId) => {
    const res = await sessionManager.removeDynamicAccount(accountId);
    optionalModules.restartKeepAliveTimer();
    broadcastState();
    return res;
  });

  ipcMain.handle('account:rename', (_evt, accountId, nextName) => {
    const res = sessionManager.renameAccount(accountId, nextName);
    broadcastState();
    return res;
  });

  ipcMain.handle('view:set-zoom', (_evt, percent) => {
    const res = sessionManager.setGlobalZoom(percent);
    broadcastState();
    return res;
  });

  ipcMain.handle('view:set-split-mode', (_evt, count) => {
    const res = sessionManager.setSplitMode(count);
    broadcastState();
    return res;
  });

  ipcMain.handle('view:set-active-slot', (_evt, slotIndex) => {
    const res = sessionManager.setActiveSlot(slotIndex);
    broadcastState();
    return res;
  });

  ipcMain.handle('view:assign-slot-tab', (_evt, slotIndex, tabId) => {
    const res = sessionManager.assignTabToSlot(slotIndex, tabId);
    broadcastState();
    return res;
  });

  ipcMain.handle('view:push-account-to-split', (_evt, accountId) => {
    const res = sessionManager.pushAccountToSplit(accountId);
    broadcastState();
    return res;
  });

  ipcMain.handle('view:set-account-split', (_evt, accountId, count) => {
    const res = sessionManager.setAccountSplitMode(accountId, count);
    broadcastState();
    return res;
  });

  ipcMain.handle('view:toggle-account-split', (_evt, accountId) => {
    const res = sessionManager.toggleAccountSplit(accountId);
    broadcastState();
    return res;
  });

  ipcMain.handle('ui:set-modal-open', (_evt, isOpen) => {
    if (!sessionManager) return { ok: false };
    return sessionManager.setModalOverlayVisible(Boolean(isOpen));
  });

  // Tab 1, Tab 2 & Tab 3 Master Mirroring & Kotak Kontainer Kumpulan Tab (Split Vertikal) IPC
  ipcMain.handle('mirror:set-tab1', (_evt, enabled) => {
    const res = sessionManager.setTab1MirrorEnabled(Boolean(enabled));
    broadcastState();
    return res;
  });

  ipcMain.handle('mirror:toggle-tab1', () => {
    const res = sessionManager.toggleTab1Mirror();
    broadcastState();
    return res;
  });

  ipcMain.handle('mirror:set-tab1-collection-split', (_evt, enabled) => {
    const res = sessionManager.setTab1CollectionSplit(Boolean(enabled));
    broadcastState();
    return res;
  });

  ipcMain.handle('mirror:toggle-tab1-collection-split', () => {
    const res = sessionManager.toggleTab1CollectionSplit();
    broadcastState();
    return res;
  });

  ipcMain.handle('mirror:set-tab-group', (_evt, tabIndex, enabled) => {
    const res = sessionManager.setTabGroupMirrorEnabled(Number(tabIndex) || 1, Boolean(enabled));
    broadcastState();
    return res;
  });

  ipcMain.handle('mirror:toggle-tab-group', (_evt, tabIndex) => {
    const res = sessionManager.toggleTabGroupMirror(Number(tabIndex) || 1);
    broadcastState();
    return res;
  });

  ipcMain.handle('mirror:set-tab-collection-split', (_evt, tabIndex, enabled) => {
    const res = sessionManager.setTabCollectionSplit(Number(tabIndex) || 1, Boolean(enabled));
    broadcastState();
    return res;
  });

  ipcMain.handle('mirror:toggle-tab-collection-split', (_evt, tabIndex) => {
    const res = sessionManager.toggleTabCollectionSplit(Number(tabIndex) || 1);
    broadcastState();
    return res;
  });

  ipcMain.handle('collection:rename', (_evt, groupIdx, nextName) => {
    const res = sessionManager.renameCollection(groupIdx, nextName);
    broadcastState();
    return res;
  });

  ipcMain.handle('collection:start-all', async (_evt, groupIdx) => {
    const res = await sessionManager.startAllCollection(groupIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('collection:reload', (_evt, groupIdx) => {
    const res = sessionManager.reloadCollection(groupIdx);
    broadcastState();
    return res;
  });

  ipcMain.on('tab1:mirror-action', (event, payload) => {
    sessionManager.broadcastTab1MirrorAction(event.sender.id, payload);
  });

  ipcMain.on('autoklik-ext:floating-toggle-runner', async (event, payload) => {
    const senderTab = sessionManager.findTabByWebContentsId(event.sender.id)
      || (payload && payload.tabId ? sessionManager.tabs.get(payload.tabId) : null);
    if (!senderTab) return;
    const curState = automationEngine.getRunnerState(senderTab.tabId);
    if (curState && curState.state === 'RUNNING') {
      automationEngine.stopTab(senderTab.tabId, 'IDLE', 'Stop dari Popup Melayang Tab');
    } else {
      await automationEngine.startTab(senderTab.tabId, { force: true });
      sessionManager.disconnectMirrorForAutoKlikTab(senderTab.tabId);
    }
    broadcastState();
  });

  // AutoKlik-v3.2-Mandiri-Extension IPC (Mandiri Per-Tab: accountId + tabIndex)
  ipcMain.handle('autoklik-ext:get-profile', (_evt, accountId, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    return pointStore.getAccountProfile(accountId, tIdx);
  });

  ipcMain.handle('autoklik-ext:save-settings', (_evt, accountId, nextSettings, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const updated = pointStore.saveAccountSettings(accountId, nextSettings || {}, tIdx);
    broadcastState();
    return { ok: true, tabIndex: tIdx, extensionSettings: updated, profile: pointStore.getAccountProfile(accountId, tIdx) };
  });

  ipcMain.handle('autoklik-ext:add-point', (_evt, accountId, payload, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.addAccountPoint(accountId, payload || {}, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:update-point', (_evt, accountId, pointId, patch, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.updateAccountPoint(accountId, pointId, patch || {}, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:remove-point', (_evt, accountId, pointId, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.removeAccountPoint(accountId, pointId, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:remove-last-point', (_evt, accountId, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.removeLastAccountPoint(accountId, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:clear-points', (_evt, accountId, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.clearAccountPoints(accountId, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:toggle-hidden', (_evt, accountId, nextHidden, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const cur = pointStore.getAccountSettings(accountId, tIdx);
    const val = typeof nextHidden === 'boolean' ? nextHidden : !Boolean(cur.markersHidden);
    const updated = pointStore.saveAccountSettings(accountId, { markersHidden: val }, tIdx);
    broadcastState();
    return { ok: true, tabIndex: tIdx, extensionSettings: updated };
  });

  ipcMain.handle('autoklik-ext:toggle-lock', (_evt, accountId, nextLocked, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const cur = pointStore.getAccountSettings(accountId, tIdx);
    const val = typeof nextLocked === 'boolean' ? nextLocked : !Boolean(cur.pointsLocked);
    const updated = pointStore.saveAccountSettings(accountId, { pointsLocked: val }, tIdx);
    broadcastState();
    return { ok: true, tabIndex: tIdx, extensionSettings: updated };
  });

  ipcMain.handle('autoklik-ext:move-point', (_evt, accountId, pointId, dir, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.moveAccountPoint(accountId, pointId, dir, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:export-json', (_evt, accountId, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    return { ok: true, tabIndex: tIdx, json: pointStore.exportAccountProfileJson(accountId, tIdx) };
  });

  ipcMain.handle('autoklik-ext:import-json', (_evt, accountId, rawJson, tabIndex = null) => {
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const res = pointStore.importAccountProfileJson(accountId, rawJson, tIdx);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:start-account', async (_evt, accountId, tabIndex = null) => {
    const tabs = sessionManager.getAccountTabs(accountId);
    if (!tabs.length) return { ok: false, reason: `Akun ${'$'}{accountId} belum memiliki tab` };
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const target = tabs.find((t) => t.indexInAccount === tIdx)
      || tabs.find((t) => t.tabId === sessionManager.activeTabId)
      || tabs[0];
    const res = await automationEngine.startTab(target.tabId, { force: true });
    sessionManager.disconnectMirrorForAutoKlikTab(target.tabId);
    broadcastState();
    return res;
  });

  ipcMain.handle('autoklik-ext:stop-account', (_evt, accountId) => {
    const tabs = sessionManager.getAccountTabs(accountId);
    tabs.forEach((t) => automationEngine.stopTab(t.tabId));
    broadcastState();
    return { ok: true };
  });

  ipcMain.handle('autoklik-ext:start-picker', (_evt, accountId, pointType, replacePointId = null, tabIndex = null) => {
    const tabs = sessionManager.getAccountTabs(accountId);
    if (!tabs.length) {
      return { ok: false, reason: `Tab Akun ${'$'}{accountId} tidak ditemukan` };
    }
    const tIdx = sessionManager.resolveTabIndex(accountId, tabIndex);
    const target = tabs.find((t) => t.indexInAccount === tIdx)
      || tabs.find((t) => t.tabId === sessionManager.activeTabId)
      || tabs[0];
    const st = sessionManager.getAccountSplitState(accountId);
    if (sessionManager.activeAccountId !== accountId || (st.splitMode === 1 && st.activeTabId !== target.tabId) || (st.splitMode > 1 && !st.splitTabIds.includes(target.tabId))) {
      sessionManager.switchTab(target.tabId);
    }
    if (!target.view.webContents.isDestroyed()) {
      target.view.webContents.send('autoklik-ext:start-picker', {
        accountId,
        tabIndex: target.indexInAccount,
        pointType: pointType || 'tap',
        replacePointId: replacePointId || null
      });
      target.view.webContents.focus();
    }
    broadcastState();
    return { ok: true, tabId: target.tabId, accountId, tabIndex: target.indexInAccount };
  });

  ipcMain.handle('autoklik-ext:stop-picker', (_evt, accountId) => {
    const tabs = sessionManager.getAccountTabs(accountId);
    tabs.forEach((t) => {
      if (!t.view.webContents.isDestroyed()) {
        t.view.webContents.send('autoklik-ext:stop-picker');
      }
    });
    return { ok: true };
  });

  ipcMain.on('autoklik-ext:picked-result', (event, payload) => {
    if (!payload || !payload.accountId) return;
    const senderTab = sessionManager.findTabByWebContentsId(event.sender.id);
    const accId = (senderTab && senderTab.accountId) || Number(payload.accountId);
    const tIdx = (senderTab && senderTab.indexInAccount) || sessionManager.resolveTabIndex(accId, payload.tabIndex);
    let addedPoint = null;
    let replaced = false;
    if (!payload.cancelled) {
      if (payload.replacePointId) {
        const patch = {
          x: Number(payload.x) || 540,
          y: Number(payload.y) || 420,
          selector: ''
        };
        const upd = pointStore.updateAccountPoint(accId, payload.replacePointId, patch, tIdx);
        if (upd && upd.ok) {
          replaced = true;
          addedPoint = (upd.points || []).find((p) => p.id === payload.replacePointId) || null;
        }
      } else {
        const currentPts = pointStore.getAccountPoints(accId, tIdx);
        const nextOrder = currentPts.length + 1;
        const settings = pointStore.getAccountSettings(accId, tIdx);
        const inputVal = payload.inputValue || '';
        const res = pointStore.addAccountPoint(accId, {
          type: payload.pointType || 'tap',
          label: payload.label || `Titik ${'$'}{nextOrder}`,
          x: Number(payload.x) || 540,
          y: Number(payload.y) || 420,
          selector: '',
          inputValue: inputVal,
          cycleTexts: inputVal ? [inputVal] : [''],
          typingSpeedMs: 30,
          popupDelayMs: 300,
          radiusPx: (settings && settings.radiusPx) || 28,
          delayAfterMs: 500,
          iconSizePct: 25
        }, tIdx);
        if (res && res.ok) addedPoint = res.point;
      }
    }
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('autoklik-ext:picker-finished', {
        accountId: accId,
        tabIndex: tIdx,
        cancelled: Boolean(payload.cancelled),
        replaced,
        replacePointId: payload.replacePointId || null,
        point: addedPoint
      });
    }
    broadcastState();
  });

  ipcMain.handle('autoklik-ext:save-dialog', async (_evt, accountId, tabIndex = null) => {
    const id = Number(accountId) || 1;
    const tIdx = sessionManager.resolveTabIndex(id, tabIndex);
    pointStore.ensureContainer(id);
    pointStore.persistAccount(id);
    const jsonContent = pointStore.exportAccountProfileJson(id, tIdx);
    const defaultDir = app.getPath('documents') || app.getPath('userData');
    const res = await dialog.showSaveDialog(mainWindow, {
      title: `Save Konfigurasi Titik Akun ${'$'}{id} Tab ${'$'}{tIdx}`,
      defaultPath: path.join(defaultDir, `autoklik-akun-${'$'}{id}-tab-${'$'}{tIdx}.json`),
      filters: [{ name: 'Konfigurasi AutoKlik JSON', extensions: ['json'] }]
    });
    if (!res.canceled && res.filePath) {
      fs.writeFileSync(res.filePath, jsonContent, 'utf8');
      return {
        ok: true,
        canceled: false,
        accountId: id,
        tabIndex: tIdx,
        filePath: res.filePath,
        storeFile: `akun_${'$'}{id}.json`
      };
    }
    return {
      ok: true,
      canceled: true,
      accountId: id,
      tabIndex: tIdx,
      filePath: pointStore.getAccountStorePath(id),
      storeFile: `akun_${'$'}{id}.json`
    };
  });

  ipcMain.handle('autoklik-ext:open-dialog', async (_evt, accountId, tabIndex = null) => {
    const id = Number(accountId) || 1;
    const tIdx = sessionManager.resolveTabIndex(id, tabIndex);
    const defaultDir = app.getPath('documents') || app.getPath('userData');
    const res = await dialog.showOpenDialog(mainWindow, {
      title: `Open Konfigurasi Titik Akun ${'$'}{id} Tab ${'$'}{tIdx}`,
      defaultPath: defaultDir,
      properties: ['openFile'],
      filters: [{ name: 'Konfigurasi AutoKlik JSON', extensions: ['json'] }]
    });
    if (res.canceled || !res.filePaths || !res.filePaths.length) {
      return { ok: false, canceled: true, accountId: id, tabIndex: tIdx };
    }
    const chosenPath = res.filePaths[0];
    try {
      const rawJson = fs.readFileSync(chosenPath, 'utf8');
      const imported = pointStore.importAccountProfileJson(id, rawJson, tIdx);
      broadcastState();
      return {
        ...imported,
        canceled: false,
        accountId: id,
        tabIndex: tIdx,
        filePath: chosenPath,
        storeFile: `akun_${'$'}{id}.json`
      };
    } catch (err) {
      return { ok: false, canceled: false, accountId: id, tabIndex: tIdx, reason: err.message };
    }
  });

  ipcMain.on('autoklik-ext:get-overlay-sync', (event, reqAccountId) => {
    const tab = sessionManager.findTabByWebContentsId(event.sender.id);
    const accId = (tab && tab.accountId) || Number(reqAccountId) || 1;
    const tIdx = (tab && tab.indexInAccount) || 1;
    const profile = pointStore.getAccountProfile(accId, tIdx);
    const runnerObj = tab && automationEngine && typeof automationEngine.getRunnerState === 'function'
      ? automationEngine.getRunnerState(tab.tabId)
      : null;
    const runnerState = (runnerObj && runnerObj.state) || 'IDLE';
    const activeAccId = sessionManager.activeAccountId || 1;
    const activeTabIdx = sessionManager.getActiveTabIndex(activeAccId);
    const isMasterActiveTab1 = Boolean(
      sessionManager.tab1MirrorEnabled &&
      tIdx === 1 &&
      activeTabIdx === 1 &&
      tab &&
      tab.tabId === sessionManager.activeTabId
    );
    event.returnValue = {
      accountId: accId,
      tabId: tab ? tab.tabId : null,
      tabIndex: tIdx,
      runnerState,
      tab1MirrorEnabled: Boolean(sessionManager.tab1MirrorEnabled),
      tab1MirrorActive: isMasterActiveTab1,
      points: profile.points || [],
      extensionSettings: profile.extensionSettings || {},
      storeFile: profile.storeFile || `akun_${'$'}{accId}.json`
    };
  });

  ipcMain.on('autoklik-ext:page-drag-point', (event, payload) => {
    if (!payload || !payload.pointId) return;
    const tab = sessionManager.findTabByWebContentsId(event.sender.id);
    const accId = (tab && tab.accountId) || Number(payload.accountId) || 1;
    const tIdx = (tab && tab.indexInAccount) || sessionManager.resolveTabIndex(accId, payload.tabIndex);
    const settings = pointStore.getAccountSettings(accId, tIdx);
    if (settings && settings.pointsLocked) return;
    const pts = pointStore.getAccountPoints(accId, tIdx);
    const curPt = pts.find((p) => p.id === payload.pointId);

    if (payload.isPopupPin) {
      const popupPinX = Math.max(4, Math.round(Number(payload.popupPinX) || 0));
      const popupPinY = Math.max(4, Math.round(Number(payload.popupPinY) || 0));
      pointStore.updateAccountPoint(accId, payload.pointId, { popupPinX, popupPinY }, tIdx);
    } else if (payload.isSwipeEnd) {
      const endX = Math.max(4, Math.round(Number(payload.endX) || 0));
      const endY = Math.max(4, Math.round(Number(payload.endY) || 0));
      const patch = { endX, endY };
      if (curPt && (curPt.type === 'swipe_up' || curPt.type === 'swipe_down')) {
        patch.lineLengthPx = Math.min(1500, Math.max(40, Math.abs(endY - (Number(curPt.y) || 0))));
        patch.type = endY > (Number(curPt.y) || 0) ? 'swipe_down' : 'swipe_up';
      }
      pointStore.updateAccountPoint(accId, payload.pointId, patch, tIdx);
    } else {
      const x = Math.max(4, Math.round(Number(payload.x) || 0));
      const y = Math.max(4, Math.round(Number(payload.y) || 0));
      const patch = {
        x,
        y,
        selector: typeof payload.selector === 'string' ? payload.selector : ''
      };
      if (curPt && (curPt.type === 'swipe_up' || curPt.type === 'swipe_down')) {
        const len = Math.max(40, Number(curPt.lineLengthPx) || 350);
        patch.endX = x;
        patch.endY = curPt.type === 'swipe_down' ? (y + len) : Math.max(4, y - len);
      }
      pointStore.updateAccountPoint(accId, payload.pointId, patch, tIdx);
    }
    broadcastState();
  });

  ipcMain.on('autoklik-ext:request-edit-point', (event, payload) => {
    if (!payload || !payload.pointId) return;
    const tab = sessionManager.findTabByWebContentsId(event.sender.id);
    const accId = (tab && tab.accountId) || Number(payload.accountId) || 1;
    const tIdx = (tab && tab.indexInAccount) || sessionManager.resolveTabIndex(accId, payload.tabIndex);
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('autoklik-ext:open-point-editor', {
        accountId: accId,
        tabIndex: tIdx,
        pointId: String(payload.pointId)
      });
      mainWindow.focus();
    }
  });

  ipcMain.on('geo:get-config-sync', (event, accountId) => {
    event.returnValue = geoProvider.getEffectiveGeo(accountId);
  });

  ipcMain.on('fase3:get-preload-flags-sync', (event) => {
    event.returnValue = {
      latencyCompensationEnabled: optionalModules.flags.latencyCompensationEnabled,
      fastRequestTimeoutMs: optionalModules.fastRequestTimeoutMs,
      smartTargetMatcherEnabled: optionalModules.flags.smartTargetMatcherEnabled,
      smartMatcherFallbackNearest: optionalModules.flags.smartMatcherFallbackNearest,
      smartMatcherServiceType: optionalModules.flags.smartMatcherServiceType,
      priorityHours: optionalModules.priorityHours
    };
  });
}

app.whenReady().then(async () => {
  app.userAgentFallback = sessionManager.stripElectronUa(app.userAgentFallback);

  if (runtimeConfig.powerSaveBlocker) {
    powerSaveId = powerSaveBlocker.start('prevent-app-suspension');
  }

  logRotation.start();
  logRotation.write('APP_BOOT', { phase: 'Fase 4', targetUrl: runtimeConfig.targetUrl });

  trayManager.init();
  registerIpcHandlers();
  createMainWindow();

  await sessionManager.initAllContainers();
  await sessionManager.staggeredBootDefaultTabs();
  await preflightService.refreshLoginCookies();
  memoryWatchdog.start();
  optionalModules.start();
  enduranceMonitor.start();
  enduranceMonitor.startLiveMeter((metrics) => {
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.isVisible()) {
      mainWindow.webContents.send('system:metrics', metrics);
    }
  }, 3000);
  broadcastState();
});

app.on('before-quit', () => {
  enduranceMonitor.stop();
  if (sessionManager && typeof sessionManager.flushAllSessions === 'function') {
    sessionManager.flushAllSessions().catch(() => {});
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin' && !runtimeConfig.closeToTray) {
    quitAppCleanly();
  }
});
    """.trimIndent() + "\n"

  fun sessionManagerJs(): String =
    """
const fs = require('fs');
const path = require('path');
const { app, WebContentsView, session, Menu, clipboard } = require('electron');

const MAX_DYNAMIC_ACCOUNTS = 12;
const MAX_TABS_PER_ACCOUNT = 3;

class SessionManager {
  constructor(runtimeConfig, proxyManager, geoProvider, assetBlocker, serverClockSync) {
    this.runtimeConfig = runtimeConfig;
    this.proxyManager = proxyManager;
    this.geoProvider = geoProvider;
    this.assetBlocker = assetBlocker;
    this.serverClockSync = serverClockSync;
    this.automationEngine = null;
    this.pointStore = null;
    this.mainWindow = null;
    this.getHeaderHeight = () => 220;
    this.onStateChange = () => {};
    this.sessions = new Map();
    this.tabs = new Map();
    this.activeTabId = null;
    this.tabCounter = 0;

    this.accountsStatePath = path.join(app.getPath('userData'), 'ferizy-accounts-state.json');
    this.accountNames = new Map();
    this.accountIds = this.loadAccountState();

    const initZoom = Number(runtimeConfig.defaultZoomPercent);
    this.globalZoomPercent = Number.isFinite(initZoom) ? Math.min(150, Math.max(50, Math.round(initZoom))) : 100;

    this.defaultSplitMode = 1;
    this.activeAccountId = this.accountIds[0] || 1;
    this.accountSplitStates = new Map();
    for (const id of this.accountIds) {
      this.accountSplitStates.set(id, {
        autoSplitEnabled: true,
        splitMode: 1,
        splitTabIds: [],
        activeSlotIndex: 0,
        activeTabId: null
      });
    }
    this.splitMode = 1;
    this.splitTabIds = [];
    this.activeSlotIndex = 0;
    this.lastPaneBounds = [];
    this.attachedTabIds = new Set();
    this.lastAppliedBounds = new Map();
    this.modalOverlayActive = false;
    this.tab1MirrorEnabled = runtimeConfig.tab1MasterMirrorEnabled !== false;
    this.tab1CollectionSplitActive = Boolean(runtimeConfig.tab1CollectionSplitActive);
    this.tabGroupMirrorEnabled = {
      1: this.tab1MirrorEnabled,
      2: false,
      3: false
    };
    this.tabCollectionSplitGroup = this.tab1CollectionSplitActive ? 1 : 0;
    this.tab1MirrorAutoOffReason = null;
    this.mirrorSuppressUntil = new Map();
    this.collectionNames = {
      1: '📦 Kumpulan Tab 1',
      2: '📦 Kumpulan Tab 2',
      3: '📦 Kumpulan Tab 3'
    };
  }

  getAccountSplitState(accountId) {
    const id = Number(accountId || this.activeAccountId || this.accountIds[0] || 1);
    if (!this.accountSplitStates.has(id)) {
      this.accountSplitStates.set(id, {
        autoSplitEnabled: true,
        splitMode: 1,
        splitTabIds: [],
        activeSlotIndex: 0,
        activeTabId: null
      });
    }
    return this.accountSplitStates.get(id);
  }

  isTabRunningAutoKlik(tabId) {
    if (!tabId || !this.automationEngine || typeof this.automationEngine.getRunnerState !== 'function') {
      return false;
    }
    const st = this.automationEngine.getRunnerState(tabId);
    return Boolean(st && st.state === 'RUNNING');
  }

  disconnectMirrorForAutoKlikTab(tabId) {
    const tab = tabId ? this.tabs.get(tabId) : null;
    if (!tab) return { ok: false };
    const tIdx = Number(tab.indexInAccount) || 1;
    // Periksa apakah masih ada tab di grup kumpulan ini yang TIDAK sedang Start AutoKlik
    const groupTabs = this.getAllTabGroupList(tIdx);
    const nonRunningTabs = groupTabs.filter((t) => t.tabId !== tabId && !this.isTabRunningAutoKlik(t.tabId));
    if (nonRunningTabs.length === 0) {
      // Semua tab di kumpulan ini sedang Start AutoKlik -> putus Mirror grup tersebut
      this.tabGroupMirrorEnabled[tIdx] = false;
      if (tIdx === 1) {
        this.tab1MirrorEnabled = false;
        this.tab1MirrorAutoOffReason = 'Otomatis PUTUS karena seluruh Tab 1 sedang Start AutoKlik';
      }
    } else if (this.activeTabId === tabId) {
      // Jika tab Master yang sedang aktif ditekan Start AutoKlik, tab ini otomatis putus dari Mirror.
      // Jika sedang dalam tampilan Split Kumpulan Tab, pindahkan Master ke tab lain yang belum Start AutoKlik;
      // jika tampilan 1 layar biasa, matikan Mirror untuk tab aktif yang sedang Start AutoKlik ini.
      if (this.tabCollectionSplitGroup === tIdx && nonRunningTabs[0]) {
        const nextMaster = nonRunningTabs[0];
        this.activeAccountId = nextMaster.accountId;
        const st = this.getAccountSplitState(nextMaster.accountId);
        st.activeTabId = nextMaster.tabId;
        this.activeTabId = nextMaster.tabId;
      } else {
        this.tabGroupMirrorEnabled[tIdx] = false;
        if (tIdx === 1) {
          this.tab1MirrorEnabled = false;
          this.tab1MirrorAutoOffReason = `Otomatis PUTUS untuk ${'$'}{this.getAccountLabel(tab.accountId)} • Tab ${'$'}{tIdx} yang sedang Start AutoKlik`;
        }
      }
    }
    return {
      ok: true,
      disconnectedTabId: tabId,
      tabIndex: tIdx,
      groupMirrorEnabled: Boolean(this.tabGroupMirrorEnabled[tIdx])
    };
  }

  ensureTabGroupExistsForAllAccounts(tabIndex = 1) {
    const targetIdx = Math.max(1, Math.min(3, Number(tabIndex) || 1));
    if (targetIdx <= 1) return;
    for (const accId of this.accountIds) {
      let accTabs = this.getAccountTabs(accId);
      while (accTabs.length < targetIdx && accTabs.length < MAX_TABS_PER_ACCOUNT) {
        const created = this.createTab(accId, null, { autoSelect: false });
        if (!created || !created.ok) break;
        accTabs = this.getAccountTabs(accId);
      }
    }
  }

  getAllTabGroupList(tabIndex = 1) {
    const targetIdx = Math.max(1, Math.min(3, Number(tabIndex) || 1));
    const list = [];
    for (const id of this.accountIds) {
      const tabs = this.getAccountTabs(id);
      const found = tabs.find((t) => t.indexInAccount === targetIdx);
      if (found) list.push(found);
    }
    return list;
  }

  getAllTab1List() {
    return this.getAllTabGroupList(1);
  }

  enforceTab1MirrorGuardForActiveTab() {
    const activeAcc = this.activeAccountId || this.accountIds[0] || 1;
    const st = this.getAccountSplitState(activeAcc);
    const activeTab = st.activeTabId ? this.tabs.get(st.activeTabId) : null;
    const activeIdx = activeTab ? (Number(activeTab.indexInAccount) || 1) : 1;
    let changed = false;
    if (activeIdx > 1) {
      if (this.tab1CollectionSplitActive) {
        this.tab1CollectionSplitActive = false;
        if (this.tabCollectionSplitGroup === 1) this.tabCollectionSplitGroup = 0;
        changed = true;
      }
      if (this.tab1MirrorEnabled) {
        this.tab1MirrorEnabled = false;
        this.tabGroupMirrorEnabled[1] = false;
        this.tab1MirrorAutoOffReason = `Otomatis OFF saat memilih Tab ${'$'}{activeIdx} (Tab 2/3 mandiri)`;
        changed = true;
      }
    }
    for (const gIdx of [1, 2, 3]) {
      if (gIdx !== activeIdx) {
        if (this.tabGroupMirrorEnabled[gIdx]) {
          this.tabGroupMirrorEnabled[gIdx] = false;
          changed = true;
        }
        if (this.tabCollectionSplitGroup === gIdx) {
          this.tabCollectionSplitGroup = 0;
          changed = true;
        }
      }
    }
    this.tab1MirrorEnabled = Boolean(this.tabGroupMirrorEnabled[1]);
    this.tab1CollectionSplitActive = this.tabCollectionSplitGroup === 1;
    return changed;
  }

  setTabGroupMirrorEnabled(tabIndex = 1, enabled = true, customReason = null) {
    const targetIdx = Math.max(1, Math.min(3, Number(tabIndex) || 1));
    const wantOn = Boolean(enabled);
    const activeAcc = this.activeAccountId || this.accountIds[0] || 1;
    let activeIdx = this.getActiveTabIndex(activeAcc);

    if (wantOn) {
      this.ensureTabGroupExistsForAllAccounts(targetIdx);
      // Matikan mirror grup tab lain agar tidak bertabrakan
      for (const gIdx of [1, 2, 3]) {
        if (gIdx !== targetIdx) {
          this.tabGroupMirrorEnabled[gIdx] = false;
        }
      }
      if (activeIdx !== targetIdx) {
        const accTabs = this.getAccountTabs(activeAcc);
        const targetTab = accTabs.find((t) => t.indexInAccount === targetIdx);
        if (targetTab) {
          const st = this.getAccountSplitState(activeAcc);
          st.activeTabId = targetTab.tabId;
          st.activeSlotIndex = 0;
          this.activeTabId = targetTab.tabId;
          activeIdx = targetIdx;
          this.layoutActiveView();
        }
      }
      // Jika tab aktif saat ini sedang Start AutoKlik, cari tab di grup ini yang belum Start AutoKlik
      if (this.activeTabId && this.isTabRunningAutoKlik(this.activeTabId)) {
        const idlePeer = this.getAllTabGroupList(targetIdx).find((t) => !this.isTabRunningAutoKlik(t.tabId));
        if (idlePeer) {
          this.activeAccountId = idlePeer.accountId;
          const st = this.getAccountSplitState(idlePeer.accountId);
          st.activeTabId = idlePeer.tabId;
          this.activeTabId = idlePeer.tabId;
        } else {
          this.tabGroupMirrorEnabled[targetIdx] = false;
          this.tab1MirrorEnabled = Boolean(this.tabGroupMirrorEnabled[1]);
          return {
            ok: false,
            tabIndex: targetIdx,
            tab1MirrorEnabled: this.tab1MirrorEnabled,
            tabGroupMirrorEnabled: false,
            reason: `Semua Tab ${'$'}{targetIdx} sedang menjalankan AutoKlik (Mirror otomatis PUTUS untuk tab yang sedang Start AutoKlik).`
          };
        }
      }
    }

    this.tabGroupMirrorEnabled[targetIdx] = wantOn;
    this.tab1MirrorEnabled = Boolean(this.tabGroupMirrorEnabled[1]);
    if (targetIdx === 1) {
      this.tab1MirrorAutoOffReason = wantOn ? null : (customReason || 'Dimatikan manual');
    }
    return {
      ok: true,
      tabIndex: targetIdx,
      tab1MirrorEnabled: this.tab1MirrorEnabled,
      tabGroupMirrorEnabled: Boolean(this.tabGroupMirrorEnabled[targetIdx]),
      tab1CollectionSplitActive: this.tab1CollectionSplitActive,
      tabCollectionSplitGroup: this.tabCollectionSplitGroup,
      activeTabIndex: activeIdx,
      reason: this.tab1MirrorAutoOffReason
    };
  }

  toggleTabGroupMirror(tabIndex = 1) {
    const targetIdx = Math.max(1, Math.min(3, Number(tabIndex) || 1));
    return this.setTabGroupMirrorEnabled(targetIdx, !Boolean(this.tabGroupMirrorEnabled[targetIdx]));
  }

  setTab1MirrorEnabled(enabled, customReason = null) {
    return this.setTabGroupMirrorEnabled(1, enabled, customReason);
  }

  toggleTab1Mirror() {
    return this.toggleTabGroupMirror(1);
  }

  renameCollection(groupIdx, nextName) {
    const gIdx = Math.max(1, Math.min(3, Number(groupIdx) || 1));
    const clean = String(nextName || '').trim();
    if (clean) {
      this.collectionNames[gIdx] = clean;
    }
    return { ok: true, groupIdx: gIdx, name: this.collectionNames[gIdx] };
  }

  async startAllCollection(groupIdx) {
    const gIdx = Math.max(1, Math.min(3, Number(groupIdx) || 1));
    const groupTabs = this.getAllTabGroupList(gIdx);
    let startedCount = 0;
    for (const t of groupTabs) {
      const profile = this.pointStore ? this.pointStore.getAccountProfile(t.accountId, gIdx) : null;
      const hasPoints = profile && Array.isArray(profile.points) && profile.points.some(p => p && p.enabled !== false);
      if (hasPoints && !this.isTabRunningAutoKlik(t.tabId)) {
        if (this.automationEngine && typeof this.automationEngine.startTab === 'function') {
          await this.automationEngine.startTab(t.tabId, { force: true });
          this.disconnectMirrorForAutoKlikTab(t.tabId);
          startedCount++;
        }
      }
    }
    return { ok: true, groupIdx: gIdx, startedCount };
  }

  reloadCollection(groupIdx) {
    const gIdx = Math.max(1, Math.min(3, Number(groupIdx) || 1));
    const groupTabs = this.getAllTabGroupList(gIdx);
    let reloadedCount = 0;
    for (const t of groupTabs) {
      const view = this.sessions.get(t.tabId);
      if (view && view.webContents && !view.webContents.isDestroyed()) {
        view.webContents.reload();
        reloadedCount++;
      }
    }
    return { ok: true, groupIdx: gIdx, reloadedCount };
  }

  setTabCollectionSplit(tabIndex = 1, enabled = true) {
    const targetIdx = Math.max(1, Math.min(3, Number(tabIndex) || 1));
    const wantSplit = Boolean(enabled);
    if (wantSplit) {
      this.ensureTabGroupExistsForAllAccounts(targetIdx);
      this.tabCollectionSplitGroup = targetIdx;
      this.tab1CollectionSplitActive = targetIdx === 1;
      const allGroupTabs = this.getAllTabGroupList(targetIdx);
      const activeAcc = this.activeAccountId || this.accountIds[0] || 1;
      const preferredTab =
        allGroupTabs.find((t) => t.accountId === activeAcc && !this.isTabRunningAutoKlik(t.tabId)) ||
        allGroupTabs.find((t) => !this.isTabRunningAutoKlik(t.tabId)) ||
        allGroupTabs.find((t) => t.accountId === activeAcc) ||
        allGroupTabs[0] ||
        null;
      if (preferredTab) {
        this.activeAccountId = preferredTab.accountId;
        const st = this.getAccountSplitState(preferredTab.accountId);
        st.activeTabId = preferredTab.tabId;
        st.activeSlotIndex = 0;
        this.activeTabId = preferredTab.tabId;
      }
      for (const gIdx of [1, 2, 3]) {
        this.tabGroupMirrorEnabled[gIdx] = gIdx === targetIdx && !(preferredTab && this.isTabRunningAutoKlik(preferredTab.tabId));
      }
      this.tab1MirrorEnabled = Boolean(this.tabGroupMirrorEnabled[1]);
      if (targetIdx === 1) this.tab1MirrorAutoOffReason = null;
    } else {
      if (this.tabCollectionSplitGroup === targetIdx) {
        this.tabCollectionSplitGroup = 0;
      }
      this.tab1CollectionSplitActive = this.tabCollectionSplitGroup === 1;
    }
    this.syncActiveSplitMirrors();
    this.layoutActiveView();
    if (!this.modalOverlayActive && this.activeTabId) {
      const activeTab = this.tabs.get(this.activeTabId);
      if (activeTab && activeTab.view && !activeTab.view.webContents.isDestroyed()) {
        try { activeTab.view.webContents.focus(); } catch (_) {}
      }
    }
    return {
      ok: true,
      tabIndex: targetIdx,
      tabCollectionSplitGroup: this.tabCollectionSplitGroup,
      tab1CollectionSplitActive: this.tab1CollectionSplitActive,
      tab1MirrorEnabled: this.tab1MirrorEnabled,
      tabGroupMirrorEnabled: Boolean(this.tabGroupMirrorEnabled[targetIdx]),
      activeTabId: this.activeTabId
    };
  }

  toggleTabCollectionSplit(tabIndex = 1) {
    const targetIdx = Math.max(1, Math.min(3, Number(tabIndex) || 1));
    const isCurrentlyActive = this.tabCollectionSplitGroup === targetIdx;
    return this.setTabCollectionSplit(targetIdx, !isCurrentlyActive);
  }

  setTab1CollectionSplit(enabled) {
    return this.setTabCollectionSplit(1, enabled);
  }

  toggleTab1CollectionSplit() {
    return this.toggleTabCollectionSplit(1);
  }

  getMirrorTargetTabList(tabIndex = 1, excludeTabId = null) {
    const targetIdx = Math.max(1, Math.min(3, Number(tabIndex) || 1));
    const list = [];
    for (const tab of this.tabs.values()) {
      if (!tab || tab.indexInAccount !== targetIdx) continue;
      if (excludeTabId && tab.tabId === excludeTabId) continue;
      if (!tab.view || !tab.view.webContents || tab.view.webContents.isDestroyed()) continue;
      // ATURAN WAJIB: Otomatis putus mirror untuk setiap tab yang sedang Start AutoKlik!
      if (this.isTabRunningAutoKlik(tab.tabId)) continue;
      list.push(tab);
    }
    return list;
  }

  getMirrorTargetTab1List(excludeTabId = null) {
    return this.getMirrorTargetTabList(1, excludeTabId);
  }

  broadcastTab1MirrorAction(senderWcId, payload) {
    if (!payload || typeof payload !== 'object') {
      return { ok: false, mirroredCount: 0 };
    }
    const senderTab = this.findTabByWebContentsId(senderWcId);
    if (!senderTab) {
      return { ok: false, mirroredCount: 0 };
    }
    const senderIdx = Number(senderTab.indexInAccount) || 1;
    // Pastikan kompatibel dengan this.tab1MirrorEnabled maupun this.tabGroupMirrorEnabled
    if (senderIdx === 1) {
      this.tabGroupMirrorEnabled[1] = Boolean(this.tab1MirrorEnabled);
    }
    const isGroupMirrorOn = Boolean(this.tabGroupMirrorEnabled[senderIdx]);
    if (!isGroupMirrorOn) {
      return { ok: false, mirroredCount: 0 };
    }

    // ATURAN WAJIB: Jika tab pengirim sedang Start AutoKlik, otomatis putus Mirror untuk tab ini!
    if (this.isTabRunningAutoKlik(senderTab.tabId)) {
      return { ok: false, mirroredCount: 0, reason: 'sender_autoklik_running' };
    }

    const now = Date.now();
    if (now < (this.mirrorSuppressUntil.get(senderTab.tabId) || 0)) {
      return { ok: false, mirroredCount: 0 };
    }

    // Jika mode Split Vertikal Kumpulan Tab (1/2/3) sedang aktif dan user berinteraksi di salah satu kolom tab tersebut,
    // jadikan kolom tab tersebut sebagai Master aktif secara instan (0ms)
    if (this.tabCollectionSplitGroup === senderIdx && senderTab.tabId !== this.activeTabId) {
      this.activeAccountId = senderTab.accountId;
      const st = this.getAccountSplitState(senderTab.accountId);
      st.activeTabId = senderTab.tabId;
      this.activeTabId = senderTab.tabId;
    }

    // Kunci Single-Master: Hanya tab yang sedang menjadi Master aktif (this.activeTabId) yang boleh memancarkan aksi mirror
    if (senderTab.tabId !== this.activeTabId) {
      return { ok: false, mirroredCount: 0 };
    }

    const targets = this.getMirrorTargetTabList(senderIdx, senderTab.tabId);
    if (!targets.length) return { ok: true, mirroredCount: 0 };

    let mirroredCount = 0;
    for (const targetTab of targets) {
      const wc = targetTab.view.webContents;
      try {
        this.mirrorSuppressUntil.set(targetTab.tabId, now + 360);
        wc.send('tab1:mirror-apply', payload);
        // Untuk swipe/wheel: kirim juga native mouseWheel ke engine Chromium agar popup/container scrollable bergerak serempak 0ms tanpa delay
        if (payload.kind === 'wheel' && typeof wc.sendInputEvent === 'function') {
          const bounds = this.lastAppliedBounds.get(targetTab.tabId) || { width: 1280, height: 720 };
          const wx = Math.max(1, Math.round((Number(payload.normX) || 0.5) * (bounds.width || 1280)));
          const wy = Math.max(1, Math.round((Number(payload.normY) || 0.5) * (bounds.height || 720)));
          wc.sendInputEvent({
            type: 'mouseWheel',
            x: wx,
            y: wy,
            deltaX: Math.round(Number(payload.deltaX) || 0),
            deltaY: Math.round(Number(payload.deltaY) || 0),
            canScroll: true
          });
        }
        mirroredCount += 1;
      } catch (_) {}
    }
    return { ok: true, mirroredCount };
  }

  syncActiveSplitMirrors() {
    const activeAcc = this.activeAccountId || this.accountIds[0] || 1;
    const st = this.getAccountSplitState(activeAcc);
    this.splitMode = st.splitMode;
    this.splitTabIds = [...st.splitTabIds];
    this.activeSlotIndex = st.activeSlotIndex;
    this.activeTabId = st.activeTabId;
    this.enforceTab1MirrorGuardForActiveTab();
  }

  loadAccountState() {
    const idSet = new Set([1, 2, 3, 4, 5]);
    const maxCfg = Math.min(MAX_DYNAMIC_ACCOUNTS, Math.max(5, Number(this.runtimeConfig.maxAccounts) || 5));
    for (let id = 1; id <= maxCfg; id++) {
      idSet.add(id);
    }
    if (Array.isArray(this.runtimeConfig.accounts)) {
      for (const acc of this.runtimeConfig.accounts) {
        const id = Number(acc && acc.accountId);
        if (Number.isInteger(id) && id >= 1 && id <= MAX_DYNAMIC_ACCOUNTS) {
          idSet.add(id);
          if (acc.name && String(acc.name).trim()) {
            this.accountNames.set(id, String(acc.name).trim().slice(0, 32));
          }
        }
      }
    }
    try {
      const saved = JSON.parse(fs.readFileSync(this.accountsStatePath, 'utf8'));
      if (saved && Array.isArray(saved.accountIds)) {
        for (const raw of saved.accountIds) {
          const id = Number(raw);
          if (Number.isInteger(id) && id >= 1 && id <= MAX_DYNAMIC_ACCOUNTS) {
            idSet.add(id);
          }
        }
      }
      if (saved && saved.accountNames && typeof saved.accountNames === 'object') {
        for (const [k, v] of Object.entries(saved.accountNames)) {
          const id = Number(k);
          if (Number.isInteger(id) && typeof v === 'string' && v.trim()) {
            this.accountNames.set(id, v.trim().slice(0, 32));
          }
        }
      }
    } catch (_) {}
    return Array.from(idSet).sort((a, b) => a - b).slice(0, MAX_DYNAMIC_ACCOUNTS);
  }

  saveAccountState() {
    try {
      fs.mkdirSync(path.dirname(this.accountsStatePath), { recursive: true });
      const namesObj = {};
      for (const [id, label] of this.accountNames.entries()) {
        namesObj[String(id)] = label;
      }
      fs.writeFileSync(
        this.accountsStatePath,
        JSON.stringify({ accountIds: this.accountIds, accountNames: namesObj }, null, 2),
        'utf8'
      );
    } catch (err) {
      console.warn('[SessionManager] Gagal simpan state akun:', err.message);
    }
  }

  saveAccountIds() {
    this.saveAccountState();
  }

  getAccountLabel(accountId) {
    const id = Number(accountId) || 1;
    return this.accountNames.get(id) || `Akun ${'$'}{id}`;
  }

  renameAccount(accountId, rawName) {
    const id = Number(accountId);
    if (!Number.isInteger(id) || !this.accountIds.includes(id)) {
      return { ok: false, reason: 'Akun tidak ditemukan' };
    }
    const cleaned = String(rawName || '').replace(/\s+/g, ' ').trim().slice(0, 32);
    if (!cleaned) {
      this.accountNames.delete(id);
    } else {
      this.accountNames.set(id, cleaned);
    }
    this.saveAccountState();
    return { ok: true, accountId: id, name: this.getAccountLabel(id) };
  }

  getAccountIds() {
    return [...this.accountIds];
  }

  findTabByWebContentsId(wcId) {
    for (const t of this.tabs.values()) {
      if (t.view && !t.view.webContents.isDestroyed() && t.view.webContents.id === wcId) {
        return t;
      }
    }
    return null;
  }

  getActiveTabIndex(accountId) {
    const id = Number(accountId || this.activeAccountId || 1);
    const tabs = this.getAccountTabs(id);
    if (!tabs.length) return 1;
    const st = this.getAccountSplitState(id);
    const active = tabs.find((t) => t.tabId === st.activeTabId)
      || tabs.find((t) => t.tabId === this.activeTabId)
      || tabs[0];
    return (active && active.indexInAccount) || 1;
  }

  resolveTabIndex(accountId, reqTabIndex = null) {
    const parsed = Number(reqTabIndex);
    if (Number.isInteger(parsed) && parsed >= 1 && parsed <= MAX_TABS_PER_ACCOUNT) {
      return parsed;
    }
    return this.getActiveTabIndex(accountId);
  }

  async flushAllSessions() {
    for (const ses of this.sessions.values()) {
      try {
        if (typeof ses.flushStorageData === 'function') ses.flushStorageData();
        if (ses.cookies && typeof ses.cookies.flushStore === 'function') {
          await ses.cookies.flushStore();
        }
      } catch (_) {}
    }
  }

  attachAutomationEngine(engine) {
    this.automationEngine = engine;
  }

  attachPointStore(pointStore) {
    this.pointStore = pointStore;
  }

  attachMainWindow(win, getHeaderHeight, onStateChange) {
    this.mainWindow = win;
    this.getHeaderHeight = getHeaderHeight;
    this.onStateChange = onStateChange;
  }

  stripElectronUa(rawUa) {
    return (rawUa || '')
      .replace(/\sElectron\/[\d.]+/gi, '')
      .replace(/\sferizy-multi-container-browser\/[\d.\-a-z]+/gi, '');
  }

  async initContainer(accountId) {
    if (this.sessions.has(accountId)) return this.sessions.get(accountId);
    if (this.proxyManager && typeof this.proxyManager.ensureAccount === 'function') {
      this.proxyManager.ensureAccount(accountId);
    }
    if (this.geoProvider && typeof this.geoProvider.ensureAccount === 'function') {
      this.geoProvider.ensureAccount(accountId);
    }

    const partition = `persist:akun_${'$'}{accountId}`;
    const ses = session.fromPartition(partition);
    this.sessions.set(accountId, ses);

    const cleanUa = this.stripElectronUa(ses.getUserAgent());
    ses.setUserAgent(cleanUa);

    ses.webRequest.onBeforeSendHeaders((details, callback) => {
      const headers = { ...details.requestHeaders };
      for (const key of Object.keys(headers)) {
        if (/^user-agent$/i.test(key)) {
          headers[key] = this.stripElectronUa(headers[key]);
        } else if (/^sec-ch-ua$/i.test(key) && typeof headers[key] === 'string') {
          headers[key] = headers[key].replace(/"Electron";v="[^"]+",?\s*/gi, '');
        }
      }
      callback({ requestHeaders: headers });
    });

    if (this.assetBlocker) {
      this.assetBlocker.attachToSession(ses, accountId);
    }
    if (this.serverClockSync) {
      this.serverClockSync.attachToSession(ses);
    }
    if (this.automationEngine) {
      this.automationEngine.attachNetworkSync(ses, accountId);
    }

    this.geoProvider.attachToSession(ses, accountId);
    await this.proxyManager.attachToSession(ses, accountId);
    return ses;
  }

  async initAllContainers() {
    for (const accountId of this.accountIds) {
      await this.initContainer(accountId);
    }
  }

  sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async staggeredBootDefaultTabs() {
    const delayMs = Math.min(400, Math.max(250, Number(this.runtimeConfig.staggeredBootMs) || 300));
    const targetUrl = this.runtimeConfig.targetUrl || 'https://trip.ferizy.com';

    for (let i = 0; i < this.accountIds.length; i++) {
      const accountId = this.accountIds[i];
      const res = this.createTab(accountId, targetUrl, { autoSelect: i === 0 });
      if (res.ok && i < this.accountIds.length - 1) {
        await this.sleep(delayMs);
      }
    }
    this.ensureSplitSlotsValid();
    this.layoutActiveView();
  }

  async addDynamicAccount() {
    if (this.accountIds.length >= MAX_DYNAMIC_ACCOUNTS) {
      return { ok: false, reason: `Maksimal ${'$'}{MAX_DYNAMIC_ACCOUNTS} container akun tercapai` };
    }
    const nextId = this.accountIds.length ? Math.max(...this.accountIds) + 1 : 1;
    this.accountIds.push(nextId);
    this.saveAccountState();

    await this.initContainer(nextId);
    const targetUrl = this.runtimeConfig.targetUrl || 'https://trip.ferizy.com';
    const res = this.createTab(nextId, targetUrl, { autoSelect: true });
    this.ensureSplitSlotsValid();
    this.layoutActiveView();
    return { ok: true, accountId: nextId, tabId: res.tabId || null };
  }

  async removeDynamicAccount(accountId) {
    const id = Number(accountId);
    if (!Number.isInteger(id) || id <= 5) {
      return { ok: false, reason: 'Akun dasar 1–5 tidak dapat dihapus' };
    }
    if (!this.accountIds.includes(id)) {
      return { ok: false, reason: 'Akun tidak ditemukan' };
    }

    const tabsToRemove = this.getAccountTabs(id);
    for (const t of tabsToRemove) {
      if (this.automationEngine) {
        this.automationEngine.stopTab(t.tabId);
      }
      try {
        if (this.mainWindow && !this.mainWindow.isDestroyed()) {
          this.mainWindow.contentView.removeChildView(t.view);
        }
      } catch (_) {}
      this.attachedTabIds.delete(t.tabId);
      this.lastAppliedBounds.delete(t.tabId);
      this.proxyManager.unregisterWebContents(t.view.webContents.id);
      if (!t.view.webContents.isDestroyed()) {
        t.view.webContents.close();
      }
      this.tabs.delete(t.tabId);
    }

    this.accountIds = this.accountIds.filter((item) => item !== id);
    this.sessions.delete(id);
    this.accountSplitStates.delete(id);
    this.accountNames.delete(id);
    this.saveAccountState();

    if (this.activeAccountId === id) {
      this.activeAccountId = this.accountIds[0] || 1;
    }
    if (this.activeTabId && !this.tabs.has(this.activeTabId)) {
      const remainingAccTabs = this.getAccountTabs(this.activeAccountId);
      const firstRemaining = remainingAccTabs[0] || Array.from(this.tabs.values())[0];
      if (firstRemaining) {
        this.activeAccountId = firstRemaining.accountId;
        this.activeTabId = firstRemaining.tabId;
      } else {
        this.activeTabId = null;
      }
    }
    this.ensureSplitSlotsValid(this.activeAccountId);
    this.layoutActiveView();
    return { ok: true, accountId: id };
  }

  getAccountTabs(accountId) {
    return Array.from(this.tabs.values()).filter((t) => t.accountId === accountId);
  }

  applyZoomToWebContents(wc) {
    if (!wc || wc.isDestroyed()) return;
    const factor = Math.max(0.5, Math.min(1.5, this.globalZoomPercent / 100));
    try {
      wc.setZoomFactor(factor);
    } catch (_) {}
  }

  setGlobalZoom(percent) {
    const parsed = Math.round(Number(percent) || 100);
    this.globalZoomPercent = Math.min(150, Math.max(50, parsed));
    for (const tab of this.tabs.values()) {
      this.applyZoomToWebContents(tab.view.webContents);
    }
    return { ok: true, globalZoomPercent: this.globalZoomPercent };
  }

  createTab(accountId, targetUrl = null, options = { autoSelect: true }) {
    if (!this.accountIds.includes(accountId)) {
      return { ok: false, reason: `Akun ${'$'}{accountId} belum terdaftar di container` };
    }
    const existing = this.getAccountTabs(accountId);
    if (existing.length >= MAX_TABS_PER_ACCOUNT) {
      return { ok: false, reason: `Hard Cap 3/3 tercapai untuk Akun ${'$'}{accountId}` };
    }
    if (this.tabs.size >= this.accountIds.length * MAX_TABS_PER_ACCOUNT) {
      return { ok: false, reason: `Batas global ${'$'}{this.accountIds.length * MAX_TABS_PER_ACCOUNT} tab tercapai` };
    }

    const ses = this.sessions.get(accountId);
    if (!ses) {
      return { ok: false, reason: 'Session container belum diinisialisasi' };
    }

    const resolvedUrl = targetUrl || (existing.length === 0 ? (this.runtimeConfig.targetUrl || 'https://trip.ferizy.com') : 'https://trip.ferizy.com/order');
    this.tabCounter += 1;
    const tabId = `akun_${'$'}{accountId}_tab_${'$'}{this.tabCounter}`;
    const view = new WebContentsView({
      webPreferences: {
        session: ses,
        preload: path.join(__dirname, '../preload/ferizyPreload.js'),
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        backgroundThrottling: false,
        additionalArguments: [`--ferizy-account-id=${'$'}{accountId}`]
      }
    });

    if (typeof view.webContents.setWebRTCIPHandlingPolicy === 'function') {
      view.webContents.setWebRTCIPHandlingPolicy('disable_non_proxied_udp');
    }
    this.proxyManager.registerWebContents(view.webContents.id, accountId);
    this.applyZoomToWebContents(view.webContents);

    const accLabel = this.getAccountLabel(accountId);
    const tabMeta = {
      tabId,
      accountId,
      indexInAccount: existing.length + 1,
      view,
      url: resolvedUrl,
      title: `${'$'}{accLabel} • Tab ${'$'}{existing.length + 1}`,
      isLoading: true,
      lastError: null
    };
    this.tabs.set(tabId, tabMeta);

    if (this.automationEngine) {
      this.automationEngine.attachWinDetector(tabMeta);
    }

    view.webContents.setWindowOpenHandler(({ url }) => {
      // Cegah Tab Liar (popup / target="_blank" akibat klik otomatis atau iklan/link eksternal) membuka tab baru
      console.log(`[SessionManager] Mencegah tab liar (window.open) pada Akun ${'$'}{accountId}:`, url);
      return { action: 'deny' };
    });

    view.webContents.on('focus', () => {
      const st = this.getAccountSplitState(accountId);
      let changed = false;
      if (this.activeAccountId !== accountId) {
        this.activeAccountId = accountId;
        changed = true;
      }
      if (st.splitMode > 1) {
        const idx = st.splitTabIds.indexOf(tabId);
        if (idx !== -1 && st.activeSlotIndex !== idx) {
          st.activeSlotIndex = idx;
          changed = true;
        }
      }
      if (st.activeTabId !== tabId || this.activeTabId !== tabId) {
        st.activeTabId = tabId;
        changed = true;
      }
      if (changed) {
        this.syncActiveSplitMirrors();
        this.onStateChange();
      }
    });

    view.webContents.on('before-input-event', (event, input) => {
      if (input.type === 'keyDown' && input.alt && !input.control && !input.meta && input.key === 'ArrowLeft') {
        event.preventDefault();
        this.goBackTab(tabId);
        this.onStateChange();
        return;
      }
      if (input.type === 'keyDown' && (input.control || input.meta) && !input.alt) {
        if (input.key === '=' || input.key === '+') {
          event.preventDefault();
          this.setGlobalZoom(this.globalZoomPercent + 10);
          this.onStateChange();
        } else if (input.key === '-') {
          event.preventDefault();
          this.setGlobalZoom(this.globalZoomPercent - 10);
          this.onStateChange();
        } else if (input.key === '0') {
          event.preventDefault();
          this.setGlobalZoom(100);
          this.onStateChange();
        }
      }
    });

    view.webContents.on('did-start-loading', () => {
      tabMeta.isLoading = true;
      tabMeta.lastError = null;
      this.onStateChange();
    });

    view.webContents.on('did-finish-load', () => {
      this.applyZoomToWebContents(view.webContents);
      this.onStateChange();
    });

    view.webContents.on('did-stop-loading', () => {
      tabMeta.isLoading = false;
      const currentUrl = view.webContents.getURL();
      if (currentUrl && !currentUrl.startsWith('data:text/html')) {
        tabMeta.url = currentUrl;
      }
      tabMeta.title = view.webContents.getTitle() || tabMeta.title;
      this.applyZoomToWebContents(view.webContents);
      this.onStateChange();
    });

    view.webContents.on('did-navigate', (_e, url) => {
      if (url && !url.startsWith('data:text/html')) {
        tabMeta.url = url;
      }
      this.applyZoomToWebContents(view.webContents);
      this.onStateChange();
    });

    view.webContents.on('did-navigate-in-page', (_e, url, isMainFrame) => {
      if (isMainFrame && url && !url.startsWith('data:text/html')) {
        tabMeta.url = url;
        this.onStateChange();
      }
    });

    view.webContents.on('page-title-updated', (_e, title) => {
      tabMeta.title = title;
      this.onStateChange();
    });

    view.webContents.on('did-fail-load', (_e, errorCode, errorDescription, validatedURL, isMainFrame) => {
      if (!isMainFrame || errorCode === -3) return;
      tabMeta.isLoading = false;
      const failedUrl = validatedURL || tabMeta.url;
      tabMeta.lastError = `${'$'}{errorDescription || 'ERR_FAILED'} (${'$'}{errorCode})`;
      this.renderLoadErrorPage(view, failedUrl, errorCode, errorDescription, accountId);
      this.onStateChange();
    });

    view.webContents.on('render-process-gone', (_e, details) => {
      tabMeta.isLoading = false;
      tabMeta.lastError = `Renderer berhenti (${'$'}{(details && details.reason) || 'unknown'})`;
      this.onStateChange();
    });

    view.webContents.on('context-menu', (_e, params) => {
      const canCopy = params.editFlags.canCopy || Boolean(params.selectionText);
      const canSelect = params.editFlags.canSelectAll || params.isEditable || Boolean(params.selectionText);
      const template = [
        { label: 'Potong (Cut)', role: 'cut', enabled: params.editFlags.canCut },
        { label: 'Salin (Copy)', role: 'copy', enabled: canCopy },
        { label: 'Tempel (Paste)', role: 'paste', enabled: params.editFlags.canPaste }
      ];
      if (params.isEditable) {
        template.push({
          label: 'Tempel Teks Biasa',
          role: 'pasteAndMatchStyle',
          enabled: params.editFlags.canPaste
        });
      }
      template.push({ label: 'Pilih Semua', role: 'selectAll', enabled: canSelect });
      template.push({ type: 'separator' });

      if (params.linkURL) {
        template.push({
          label: 'Salin Alamat Link',
          click: () => clipboard.writeText(params.linkURL)
        });
        template.push({
          label: `Buka Link di Tab Baru Akun ${'$'}{accountId}`,
          click: () => {
            this.createTab(accountId, params.linkURL, { autoSelect: true });
            this.onStateChange();
          }
        });
        template.push({ type: 'separator' });
      }

      const zf =
        (typeof view.webContents.getZoomFactor === 'function' ? view.webContents.getZoomFactor() : 1) || 1;
      const cssClickX = Math.max(0, Math.round((Number(params.x) || 0) / zf));
      const cssClickY = Math.max(0, Math.round((Number(params.y) || 0) / zf));

      template.push({
        label: `Salin Koordinat Klik (x=${'$'}{cssClickX}, y=${'$'}{cssClickY})`,
        click: () => clipboard.writeText(`${'$'}{cssClickX},${'$'}{cssClickY}`)
      });

      if (this.pointStore) {
        template.push({
          label: `+ Tambah Titik AutoKlik Tab ${'$'}{tabMeta.indexInAccount} (x=${'$'}{cssClickX}, y=${'$'}{cssClickY})`,
          click: () => {
            const tIdx = tabMeta.indexInAccount || 1;
            const settings = this.pointStore.getAccountSettings(accountId, tIdx);
            const nextNum = this.pointStore.getAccountPoints(accountId, tIdx).length + 1;
            this.pointStore.addAccountPoint(accountId, {
              type: params.isEditable ? 'input' : 'tap',
              label: `Titik ${'$'}{nextNum}`,
              x: cssClickX,
              y: cssClickY,
              radiusPx: (settings && settings.radiusPx) || 28,
              typingSpeedMs: 30,
              popupDelayMs: 300,
              delayAfterMs: 500
            }, tIdx);
            this.onStateChange();
          }
        });
      }

      template.push({
        label: 'Salin URL Tab Ini',
        click: () => {
          const currentUrl = view.webContents.getURL() || tabMeta.url || '';
          if (currentUrl) clipboard.writeText(currentUrl);
        }
      });

      template.push({
        label: '⬅ Kembali ke Halaman Sebelumnya (Back)',
        click: () => {
          this.goBackTab(tabId);
          this.onStateChange();
        }
      });

      template.push({
        label: 'Muat Ulang Tab',
        click: () => this.reloadTab(tabId)
      });

      const menu = Menu.buildFromTemplate(template);
      menu.popup({ window: this.mainWindow || undefined });
    });

    view.webContents.loadURL(resolvedUrl).catch((err) => {
      if (err && /ERR_ABORTED/i.test(String(err.message))) return;
      tabMeta.isLoading = false;
      tabMeta.lastError = (err && err.message) || 'Gagal load URL';
      console.warn(`[Akun ${'$'}{accountId}] Gagal load URL:`, tabMeta.lastError);
      this.onStateChange();
    });

    const st = this.getAccountSplitState(accountId);
    this.ensureSplitSlotsValid(accountId);
    if (st.splitMode > 1 && !st.splitTabIds.includes(tabId) && st.splitTabIds.length < st.splitMode) {
      st.splitTabIds.push(tabId);
    }

    if (options.autoSelect || !this.activeTabId) {
      this.switchTab(tabId);
    } else if (this.activeAccountId === accountId && st.splitMode > 1) {
      this.ensureSplitSlotsValid(accountId);
      this.layoutActiveView();
    }

    return { ok: true, tabId, url: resolvedUrl, indexInAccount: tabMeta.indexInAccount };
  }

  renderLoadErrorPage(view, failedUrl, errorCode, errorDescription, accountId) {
    const safeUrl = String(failedUrl || '').replace(/[<>&"']/g, (ch) => ({
      '<': '&lt;',
      '>': '&gt;',
      '&': '&amp;',
      '"': '&quot;',
      "'": '&#39;'
    }[ch]));
    const safeDesc = String(errorDescription || 'Gagal memuat halaman').replace(/[<>&"']/g, '');
    const html = `<!doctype html><html><head><meta charset="utf-8"><style>
      body{margin:0;padding:32px;background:#081121;color:#e2e8f0;font-family:"Segoe UI",sans-serif;}
      .box{max-width:640px;margin:32px auto;padding:24px;border-radius:14px;background:#0f172a;border:1px solid #ef4444;}
      h2{margin:0 0 8px 0;color:#f87171;font-size:18px;}
      code{display:block;margin:10px 0;padding:10px;border-radius:8px;background:#020617;color:#38bdf8;word-break:break-all;}
      ul{margin:12px 0 0 18px;color:#cbd5e1;font-size:13px;line-height:1.6;}
    </style></head><body><div class="box">
      <h2>Gagal Memuat Halaman (Akun ${'$'}{accountId})</h2>
      <div>Kode Error Chromium: <b>${'$'}{safeDesc} (${'$'}{errorCode})</b></div>
      <code>${'$'}{safeUrl}</code>
      <ul>
        <li>Jika muncul <b>ERR_NAME_NOT_RESOLVED</b> / <b>ERR_CONNECTION_TIMED_OUT</b>: cek Wi-Fi Windows, DNS (1.1.1.1 / 8.8.8.8), atau matikan blokir/firewall sementara.</li>
        <li>Jika akun memakai Proxy: klik tombol <b>Proxy/Geo</b> pada kartu akun, pastikan IP:Port/Auth valid atau kembalikan ke <b>Direct ISP</b> untuk tes.</li>
        <li>Tekan <b>Muat Ulang</b> atau ketik ulang URL lalu tekan <b>Enter</b> / tombol <b>Buka</b>.</li>
      </ul>
    </div></body></html>`;
    view.webContents.loadURL(`data:text/html;charset=utf-8,${'$'}{encodeURIComponent(html)}`).catch(() => {});
  }

  computeEffectiveSplitMode(accountTabs, st) {
    if (!accountTabs || accountTabs.length <= 1) return 1;
    return st && st.autoSplitEnabled === false ? 1 : Math.min(3, accountTabs.length);
  }

  ensureSplitSlotsValid(targetAccountId = null) {
    const accId = Number(targetAccountId || this.activeAccountId || this.accountIds[0] || 1);
    const st = this.getAccountSplitState(accId);
    const accountTabs = this.getAccountTabs(accId);
    st.splitMode = this.computeEffectiveSplitMode(accountTabs, st);
    const valid = [];
    const seen = new Set();
    const pushOwnTab = (id) => {
      const tab = id ? this.tabs.get(id) : null;
      if (tab && tab.accountId === accId && !seen.has(id) && valid.length < st.splitMode) {
        seen.add(id);
        valid.push(id);
      }
    };

    for (const id of st.splitTabIds) pushOwnTab(id);
    pushOwnTab(st.activeTabId);
    for (const t of accountTabs) {
      if (valid.length >= st.splitMode) break;
      pushOwnTab(t.tabId);
    }

    st.splitTabIds = valid;
    if (st.activeSlotIndex >= valid.length) {
      st.activeSlotIndex = Math.max(0, valid.length - 1);
    }
    const activeTabObj = st.activeTabId ? this.tabs.get(st.activeTabId) : null;
    if (!activeTabObj || activeTabObj.accountId !== accId) {
      st.activeTabId = valid[st.activeSlotIndex] || valid[0] || (accountTabs[0] ? accountTabs[0].tabId : null);
    }

    if (accId === this.activeAccountId) {
      this.syncActiveSplitMirrors();
    }
    return st;
  }

  setSplitMode(count) {
    const targetAccId = this.activeAccountId || this.accountIds[0] || 1;
    return this.setAccountSplitMode(targetAccId, count);
  }

  setActiveSlot(slotIndex) {
    const accId = this.activeAccountId || this.accountIds[0] || 1;
    const st = this.ensureSplitSlotsValid(accId);
    const maxSlot = Math.max(0, st.splitTabIds.length - 1);
    const idx = Math.max(0, Math.min(maxSlot, Number(slotIndex) || 0));
    st.activeSlotIndex = idx;
    if (st.splitTabIds[idx]) {
      st.activeTabId = st.splitTabIds[idx];
    }
    this.syncActiveSplitMirrors();
    this.layoutActiveView();
    const targetTab = st.activeTabId ? this.tabs.get(st.activeTabId) : null;
    if (!this.modalOverlayActive && targetTab && targetTab.view && !targetTab.view.webContents.isDestroyed()) {
      try { targetTab.view.webContents.focus(); } catch (_) {}
    }
    return { ok: true, accountId: accId, activeSlotIndex: st.activeSlotIndex, activeTabId: st.activeTabId };
  }

  assignTabToSlot(slotIndex, tabId) {
    const tab = this.tabs.get(tabId);
    if (!tab) return { ok: false, reason: 'Tab tidak ditemukan' };
    const accId = tab.accountId;
    this.activeAccountId = accId;
    const st = this.ensureSplitSlotsValid(accId);
    const idx = Math.max(0, Math.min(st.splitMode - 1, Number(slotIndex) || 0));
    const existingIdx = st.splitTabIds.indexOf(tabId);
    if (existingIdx !== -1 && existingIdx !== idx) {
      const prev = st.splitTabIds[idx];
      st.splitTabIds[idx] = tabId;
      if (prev) st.splitTabIds[existingIdx] = prev;
    } else {
      st.splitTabIds[idx] = tabId;
    }
    st.activeSlotIndex = Math.min(idx, Math.max(0, st.splitTabIds.length - 1));
    st.activeTabId = tabId;
    this.syncActiveSplitMirrors();
    this.layoutActiveView();
    return { ok: true, accountId: accId, splitTabIds: st.splitTabIds };
  }

  pushAccountToSplit(accountId) {
    const id = Number(accountId || this.activeAccountId || this.accountIds[0] || 1);
    const st = this.getAccountSplitState(id);
    st.autoSplitEnabled = true;
    return this.setAccountSplitMode(id, Math.max(2, st.splitMode || 2));
  }

  toggleAccountSplit(accountId) {
    const id = Number(accountId);
    if (!Number.isInteger(id) || !this.accountIds.includes(id)) {
      return { ok: false, reason: 'Akun tidak ditemukan' };
    }
    const accountTabs = this.getAccountTabs(id);
    if (accountTabs.length <= 1) {
      return { ok: false, reason: 'Minimal 2 tab di akun ini untuk mengaktifkan Split Layar' };
    }
    const st = this.getAccountSplitState(id);
    st.autoSplitEnabled = st.autoSplitEnabled === false;
    this.activeAccountId = id;
    this.ensureSplitSlotsValid(id);
    this.syncActiveSplitMirrors();
    this.layoutActiveView();
    return { ok: true, accountId: id, autoSplitEnabled: st.autoSplitEnabled, splitMode: st.splitMode, splitTabIds: st.splitTabIds };
  }

  setAccountSplitMode(accountId, count) {
    const id = Number(accountId);
    if (!Number.isInteger(id) || !this.accountIds.includes(id)) {
      return { ok: false, reason: 'Akun tidak ditemukan' };
    }
    const parsed = [1, 2, 3].includes(Number(count)) ? Number(count) : 1;
    let accountTabs = this.getAccountTabs(id);
    if (!accountTabs.length) {
      const created = this.createTab(id, this.runtimeConfig.targetUrl || 'https://trip.ferizy.com', { autoSelect: false });
      if (!created.ok) return created;
      accountTabs = this.getAccountTabs(id);
    }

    const st = this.getAccountSplitState(id);
    const targetTab = accountTabs.find((t) => t.tabId === st.activeTabId) || accountTabs[0];
    if (!targetTab) return { ok: false, reason: 'Tab akun tidak tersedia' };

    this.activeAccountId = id;
    st.autoSplitEnabled = parsed > 1;
    st.splitMode = this.computeEffectiveSplitMode(accountTabs, st);

    if (st.splitMode === 1) {
      st.splitTabIds = [targetTab.tabId];
      st.activeSlotIndex = 0;
      st.activeTabId = targetTab.tabId;
      this.syncActiveSplitMirrors();
      this.layoutActiveView();
      return { ok: true, accountId: id, autoSplitEnabled: st.autoSplitEnabled, splitMode: 1, splitTabIds: st.splitTabIds };
    }

    st.splitTabIds = accountTabs.slice(0, st.splitMode).map((t) => t.tabId);
    const existingIdx = st.splitTabIds.indexOf(targetTab.tabId);
    if (existingIdx !== -1) {
      st.activeSlotIndex = existingIdx;
      st.activeTabId = targetTab.tabId;
    } else {
      st.activeSlotIndex = 0;
      st.activeTabId = st.splitTabIds[0] || targetTab.tabId;
    }
    this.ensureSplitSlotsValid(id);
    this.syncActiveSplitMirrors();
    this.layoutActiveView();
    return { ok: true, accountId: id, autoSplitEnabled: st.autoSplitEnabled, splitMode: st.splitMode, splitTabIds: st.splitTabIds };
  }

  switchTab(tabId) {
    const nextTab = this.tabs.get(tabId);
    if (!nextTab || !this.mainWindow || this.mainWindow.isDestroyed()) return false;

    const accId = nextTab.accountId;
    this.activeAccountId = accId;
    if (nextTab.indexInAccount > 1) {
      this.tab1CollectionSplitActive = false;
      this.tab1MirrorEnabled = false;
      this.tab1MirrorAutoOffReason = `Otomatis OFF saat memilih Tab ${'$'}{nextTab.indexInAccount} (Tab 2/3 mandiri)`;
    }
    const st = this.ensureSplitSlotsValid(accId);

    if (st.splitMode > 1) {
      const existingSlot = st.splitTabIds.indexOf(tabId);
      if (existingSlot !== -1) {
        st.activeSlotIndex = existingSlot;
        st.activeTabId = tabId;
      } else {
        const slotIdx = Math.min(st.activeSlotIndex, Math.max(0, st.splitTabIds.length));
        if (st.splitTabIds.length < st.splitMode) {
          st.splitTabIds.push(tabId);
          st.activeSlotIndex = st.splitTabIds.length - 1;
        } else {
          st.splitTabIds[slotIdx] = tabId;
          st.activeSlotIndex = slotIdx;
        }
        st.activeTabId = tabId;
      }
    } else {
      st.splitTabIds = [tabId];
      st.activeSlotIndex = 0;
      st.activeTabId = tabId;
    }

    this.syncActiveSplitMirrors();
    this.layoutActiveView();
    if (!this.modalOverlayActive && nextTab.view && !nextTab.view.webContents.isDestroyed()) {
      try { nextTab.view.webContents.focus(); } catch (_) {}
    }
    return true;
  }

  switchToAccount(accountId) {
    const id = Number(accountId);
    if (!Number.isInteger(id) || !this.accountIds.includes(id)) {
      return { ok: false, reason: 'Akun tidak ditemukan' };
    }
    this.tab1CollectionSplitActive = false;
    this.activeAccountId = id;
    const tabs = this.getAccountTabs(id);
    if (!tabs.length) {
      const targetUrl = this.runtimeConfig.targetUrl || 'https://trip.ferizy.com';
      return this.createTab(id, targetUrl, { autoSelect: true });
    }
    const st = this.ensureSplitSlotsValid(id);
    const target = tabs.find((t) => t.tabId === st.activeTabId) || tabs[0];
    st.activeTabId = target.tabId;
    this.syncActiveSplitMirrors();
    this.layoutActiveView();
    if (!this.modalOverlayActive && target.view && !target.view.webContents.isDestroyed()) {
      try { target.view.webContents.focus(); } catch (_) {}
    }
    return { ok: true, accountId: id, tabId: target.tabId, splitMode: st.splitMode, autoSplitEnabled: st.autoSplitEnabled };
  }

  setModalOverlayVisible(visible) {
    const nextVisible = Boolean(visible);
    if (nextVisible === this.modalOverlayActive) {
      return { ok: true, hidden: this.modalOverlayActive };
    }
    this.modalOverlayActive = nextVisible;
    if (!this.mainWindow || this.mainWindow.isDestroyed()) {
      return { ok: false };
    }
    if (this.modalOverlayActive) {
      for (const tab of this.tabs.values()) {
        try {
          if (typeof tab.view.setVisible === 'function') {
            tab.view.setVisible(false);
          }
          if (this.attachedTabIds.has(tab.tabId)) {
            this.mainWindow.contentView.removeChildView(tab.view);
          }
        } catch (_) {}
      }
      this.attachedTabIds.clear();
      this.lastAppliedBounds.clear();
      return { ok: true, hidden: true };
    }
    this.layoutActiveView();
    const activeTab = this.activeTabId ? this.tabs.get(this.activeTabId) : null;
    if (activeTab && activeTab.view && !activeTab.view.webContents.isDestroyed()) {
      try { activeTab.view.webContents.focus(); } catch (_) {}
    }
    return { ok: true, hidden: false };
  }

  layoutActiveView() {
    if (!this.mainWindow || this.mainWindow.isDestroyed()) return;
    if (this.modalOverlayActive) {
      for (const tab of this.tabs.values()) {
        try {
          if (typeof tab.view.setVisible === 'function') {
            tab.view.setVisible(false);
          }
          if (this.attachedTabIds.has(tab.tabId)) {
            this.mainWindow.contentView.removeChildView(tab.view);
          }
        } catch (_) {}
      }
      this.attachedTabIds.clear();
      this.lastAppliedBounds.clear();
      return;
    }

    const activeAccId = this.activeAccountId || this.accountIds[0] || 1;
    const st = this.ensureSplitSlotsValid(activeAccId);
    this.syncActiveSplitMirrors();

    const activeSplitGroup = this.tab1CollectionSplitActive ? 1 : (this.tabCollectionSplitGroup || 0);
    const visibleTabIds = activeSplitGroup >= 1 && activeSplitGroup <= 3
      ? this.getAllTabGroupList(activeSplitGroup).map((t) => t.tabId)
      : (st.splitMode === 1
          ? (st.activeTabId && this.tabs.has(st.activeTabId) && this.tabs.get(st.activeTabId).accountId === activeAccId ? [st.activeTabId] : [])
          : st.splitTabIds.slice(0, st.splitMode).filter((id) => {
              const t = this.tabs.get(id);
              return t && t.accountId === activeAccId;
            }));
    const visibleSet = new Set(visibleTabIds);

    for (const tab of this.tabs.values()) {
      if (!visibleSet.has(tab.tabId) && this.attachedTabIds.has(tab.tabId)) {
        try {
          this.mainWindow.contentView.removeChildView(tab.view);
        } catch (_) {}
        this.attachedTabIds.delete(tab.tabId);
        this.lastAppliedBounds.delete(tab.tabId);
      }
    }

    const [width, height] = this.mainWindow.getContentSize();
    const maxOffset = Math.max(84, height - 180);
    const topOffset = Math.min(maxOffset, Math.max(84, this.getHeaderHeight()));
    const availWidth = Math.max(320, width);
    const availHeight = Math.max(180, height - topOffset);
    const count = visibleTabIds.length;
    if (count === 0) {
      this.lastPaneBounds = [];
      return;
    }

    const gap = count > 1 ? 3 : 0;
    const paneWidth = Math.floor((availWidth - gap * (count - 1)) / count);
    const boundsList = [];

    visibleTabIds.forEach((id, idx) => {
      const tab = this.tabs.get(id);
      if (!tab) return;
      const x = idx * (paneWidth + gap);
      const w = idx === count - 1 ? Math.max(120, availWidth - x) : Math.max(120, paneWidth);
      const rect = { x, y: topOffset, width: w, height: availHeight };
      try {
        if (!this.attachedTabIds.has(id)) {
          this.mainWindow.contentView.addChildView(tab.view);
          this.attachedTabIds.add(id);
        }
        if (typeof tab.view.setVisible === 'function') {
          tab.view.setVisible(true);
        }
        const prevRect = this.lastAppliedBounds.get(id);
        if (
          !prevRect ||
          prevRect.x !== rect.x ||
          prevRect.y !== rect.y ||
          prevRect.width !== rect.width ||
          prevRect.height !== rect.height
        ) {
          tab.view.setBounds(rect);
          this.lastAppliedBounds.set(id, rect);
        }
      } catch (_) {}
      boundsList.push({ slotIndex: idx, tabId: id, accountId: tab.accountId, indexInAccount: tab.indexInAccount, width: w, height: availHeight });
    });

    // Samakan dimensi viewport seluruh Tab di latar belakang (off-screen) agar koordinat klik, popup & scroll identik 1:1 (0ms)
    const refPaneWidth = boundsList[0] ? boundsList[0].width : availWidth;
    for (const tab of this.tabs.values()) {
      if (!tab || visibleSet.has(tab.tabId)) continue;
      const bgRect = { x: 0, y: topOffset, width: refPaneWidth, height: availHeight };
      const prevBg = this.lastAppliedBounds.get(tab.tabId);
      if (!prevBg || prevBg.width !== bgRect.width || prevBg.height !== bgRect.height || prevBg.y !== bgRect.y) {
        try {
          tab.view.setBounds(bgRect);
          this.lastAppliedBounds.set(tab.tabId, bgRect);
        } catch (_) {}
      }
    }

    this.lastPaneBounds = boundsList;
  }

  closeTab(tabId) {
    const target = this.tabs.get(tabId);
    if (!target) return { ok: false, reason: 'Tab tidak ditemukan' };

    const peers = this.getAccountTabs(target.accountId);
    if (peers.length <= 1) {
      return { ok: false, reason: 'Tab utama default akun tidak boleh ditutup semua' };
    }

    if (this.automationEngine) {
      this.automationEngine.stopTab(tabId);
    }

    const fallback = peers.find((t) => t.tabId !== tabId);
    const accSplit = this.getAccountSplitState(target.accountId);
    accSplit.splitTabIds = accSplit.splitTabIds.filter((id) => id !== tabId);
    if (accSplit.activeTabId === tabId && fallback) {
      accSplit.activeTabId = fallback.tabId;
    }

    try {
      this.mainWindow.contentView.removeChildView(target.view);
    } catch (_) {}
    this.attachedTabIds.delete(tabId);
    this.lastAppliedBounds.delete(tabId);
    this.proxyManager.unregisterWebContents(target.view.webContents.id);
    if (!target.view.webContents.isDestroyed()) {
      target.view.webContents.close();
    }
    this.tabs.delete(tabId);

    this.getAccountTabs(target.accountId).forEach((t, idx) => {
      t.indexInAccount = idx + 1;
    });
    this.ensureSplitSlotsValid(target.accountId);
    this.layoutActiveView();
    return { ok: true };
  }

  canTabGoBack(tabId) {
    const tab = this.tabs.get(tabId || this.activeTabId);
    if (!tab || !tab.view || !tab.view.webContents || tab.view.webContents.isDestroyed()) return false;
    const wc = tab.view.webContents;
    try {
      if (wc.navigationHistory && typeof wc.navigationHistory.canGoBack === 'function') {
        return Boolean(wc.navigationHistory.canGoBack());
      }
      if (typeof wc.canGoBack === 'function') {
        return Boolean(wc.canGoBack());
      }
    } catch (_) {}
    return false;
  }

  goBackTab(tabId, isMirrored = false) {
    const tab = this.tabs.get(tabId || this.activeTabId);
    if (!tab || !tab.view || !tab.view.webContents || tab.view.webContents.isDestroyed()) {
      return { ok: false, reason: 'Tab aktif tidak ditemukan' };
    }
    const wc = tab.view.webContents;
    tab.lastError = null;
    try {
      const canHistoryBack = this.canTabGoBack(tab.tabId);
      if (canHistoryBack) {
        tab.isLoading = true;
        if (wc.navigationHistory && typeof wc.navigationHistory.goBack === 'function') {
          wc.navigationHistory.goBack();
        } else if (typeof wc.goBack === 'function') {
          wc.goBack();
        }
        if (!isMirrored && this.tab1MirrorEnabled && tab.indexInAccount === 1) {
          for (const peer of this.getMirrorTargetTab1List(tab.tabId)) {
            this.goBackTab(peer.tabId, true);
          }
        }
        this.onStateChange();
        return { ok: true, mode: 'history_back', tabId: tab.tabId, url: wc.getURL() || tab.url };
      }
      // Fallback cerdas: jika tab dibuka langsung di sub-halaman (mis. /order atau /ferryschedule) dan belum punya riwayat back, kembali ke beranda utama https://trip.ferizy.com/
      const curUrl = String(wc.getURL() || tab.url || '').trim().replace(/\/+$/, '');
      if (curUrl && curUrl !== 'https://trip.ferizy.com') {
        const navRes = this.navigateTab(tab.tabId, 'https://trip.ferizy.com/', isMirrored);
        return { ...navRes, mode: 'fallback_home', tabId: tab.tabId };
      }
      return { ok: false, mode: 'at_root', reason: 'Sudah berada di halaman paling awal (https://trip.ferizy.com/)' };
    } catch (err) {
      return { ok: false, reason: (err && err.message) || 'Gagal kembali ke halaman sebelumnya' };
    }
  }

  reloadTab(tabId, isMirrored = false) {
    const tab = this.tabs.get(tabId);
    if (!tab) return false;
    tab.lastError = null;
    const targetUrl = tab.url || this.runtimeConfig.targetUrl || 'https://trip.ferizy.com';
    const currentUrl = tab.view.webContents.getURL();
    if (!currentUrl || currentUrl.startsWith('data:text/html')) {
      return this.navigateTab(tabId, targetUrl, isMirrored);
    }
    tab.view.webContents.reload();
    if (!isMirrored && this.tab1MirrorEnabled && tab.indexInAccount === 1) {
      for (const peer of this.getMirrorTargetTab1List(tab.tabId)) {
        this.reloadTab(peer.tabId, true);
      }
    }
    return true;
  }

  navigateTab(tabId, url, isMirrored = false) {
    const tab = this.tabs.get(tabId);
    if (!tab) return { ok: false, reason: 'Tab tidak ditemukan' };
    const clean = String(url || '').trim();
    if (!clean) return { ok: false, reason: 'URL kosong' };

    const normalized = /^https?:\/\//i.test(clean) ? clean : `https://${'$'}{clean}`;
    tab.lastError = null;
    tab.url = normalized;
    tab.isLoading = true;
    if (!isMirrored && this.activeTabId !== tabId) {
      this.switchTab(tabId);
    }
    if (!isMirrored && this.tab1MirrorEnabled && tab.indexInAccount === 1) {
      for (const peer of this.getMirrorTargetTab1List(tab.tabId)) {
        this.navigateTab(peer.tabId, normalized, true);
      }
    }
    this.onStateChange();

    tab.view.webContents.loadURL(normalized).catch((err) => {
      if (err && /ERR_ABORTED/i.test(String(err.message))) return;
      tab.isLoading = false;
      tab.lastError = (err && err.message) || 'Gagal membuka URL';
      this.onStateChange();
    });
    return { ok: true, url: normalized };
  }

  getSession(accountId) {
    return this.sessions.get(accountId);
  }
}

module.exports = { SessionManager, MAX_DYNAMIC_ACCOUNTS, MAX_TABS_PER_ACCOUNT };
    """.trimIndent() + "\n"

  fun proxyManagerJs(): String =
    """
class ProxyManager {
  constructor(runtimeConfig, notificationCenter) {
    this.runtimeConfig = runtimeConfig;
    this.notificationCenter = notificationCenter;
    this.accountProxies = new Map();
    this.sessions = new Map();
    this.webContentsToAccount = new Map();

    const accounts = Array.isArray(runtimeConfig.accounts) ? runtimeConfig.accounts : [];
    const ids = new Set([1, 2, 3, 4, 5]);
    accounts.forEach((a) => {
      if (a && Number.isInteger(a.accountId)) ids.add(a.accountId);
    });
    for (const id of ids) {
      this.ensureAccount(id, accounts.find((a) => a.accountId === id));
    }
  }

  ensureAccount(accountId, found = null) {
    if (this.accountProxies.has(accountId)) return this.accountProxies.get(accountId);
    const accounts = Array.isArray(this.runtimeConfig.accounts) ? this.runtimeConfig.accounts : [];
    const cfg = found || accounts.find((a) => a.accountId === accountId);
    const p = (cfg && cfg.proxy) || {};
    const entry = {
      protocol: p.protocol || 'direct',
      host: p.host || '',
      port: Number(p.port) || 8080,
      username: p.username || '',
      password: p.password || '',
      ipLabel: p.ipLabel || (p.host ? `${'$'}{p.host}:${'$'}{p.port}` : 'Direct ISP'),
      lastCheckedIp: null,
      status: p.protocol && p.protocol !== 'direct' && p.host ? 'Proxy Siap' : 'Direct'
    };
    this.accountProxies.set(accountId, entry);
    return entry;
  }

  registerWebContents(webContentsId, accountId) {
    this.webContentsToAccount.set(webContentsId, accountId);
  }

  unregisterWebContents(webContentsId) {
    this.webContentsToAccount.delete(webContentsId);
  }

  getCredentialsForWebContents(webContentsId) {
    const accountId = this.webContentsToAccount.get(webContentsId);
    if (!accountId) return null;
    const cfg = this.accountProxies.get(accountId);
    if (!cfg || !cfg.username) return null;
    return { username: cfg.username, password: cfg.password };
  }

  buildProxyRules(cfg) {
    const cleanHost = cfg && cfg.host ? String(cfg.host).trim().replace(/^https?:\/\//i, '').replace(/^socks5?:\/\//i, '') : '';
    if (!cfg || cfg.protocol === 'direct' || !cleanHost) {
      return { mode: 'direct' };
    }
    const rules =
      cfg.protocol === 'socks5'
        ? `socks5://${'$'}{cleanHost}:${'$'}{cfg.port}`
        : `http=${'$'}{cleanHost}:${'$'}{cfg.port};https=${'$'}{cleanHost}:${'$'}{cfg.port}`;
    return {
      mode: 'fixed_servers',
      proxyRules: rules,
      proxyBypassRules: '<-loopback>'
    };
  }

  async attachToSession(ses, accountId) {
    this.sessions.set(accountId, ses);
    if (typeof ses.setWebRTCIPHandlingPolicy === 'function') {
      ses.setWebRTCIPHandlingPolicy('disable_non_proxied_udp');
    }

    const cfg = this.accountProxies.get(accountId);
    try {
      await ses.setProxy(this.buildProxyRules(cfg));
    } catch (_) {}
  }

  async updateAccountProxy(accountId, nextCfg) {
    const current = this.accountProxies.get(accountId) || {};
    const merged = {
      ...current,
      protocol: nextCfg.protocol || 'direct',
      host: (nextCfg.host || '').trim(),
      port: Number(nextCfg.port) || 8080,
      username: nextCfg.username || '',
      password: nextCfg.password || '',
      ipLabel: nextCfg.host ? `${'$'}{nextCfg.host}:${'$'}{nextCfg.port}` : 'Direct ISP',
      status: nextCfg.protocol && nextCfg.protocol !== 'direct' && nextCfg.host ? 'Proxy Siap' : 'Direct'
    };
    this.accountProxies.set(accountId, merged);

    const ses = this.sessions.get(accountId);
    if (ses) {
      if (typeof ses.setWebRTCIPHandlingPolicy === 'function') {
        ses.setWebRTCIPHandlingPolicy('disable_non_proxied_udp');
      }
      try {
        await ses.setProxy(this.buildProxyRules(merged));
      } catch (_) {}
    }
    return merged;
  }

  async checkExternalIp(accountId) {
    const ses = this.sessions.get(accountId);
    const cfg = this.accountProxies.get(accountId);
    if (!ses || !cfg) return { ok: false, error: 'Session tidak tersedia' };

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 5000);
    try {
      const res = await ses.fetch('https://api.ipify.org?format=json', { signal: controller.signal });
      const data = await res.json();
      cfg.lastCheckedIp = data.ip;
      cfg.status = cfg.protocol !== 'direct' && cfg.host ? `IP Berbeda (${'$'}{data.ip})` : `Direct (${'$'}{data.ip})`;
      return { ok: true, ip: data.ip, status: cfg.status };
    } catch (err) {
      cfg.status = 'Putus';
      if (this.notificationCenter) {
        this.notificationCenter.dispatch('PROXY_PUTUS', {
          accountId,
          dedupeHint: cfg.host || 'direct',
          message: `Proxy Akun ${'$'}{accountId} (${'$'}{cfg.ipLabel}) terputus: ${'$'}{err.message}`
        });
      }
      return { ok: false, error: err.message, status: 'Putus' };
    } finally {
      clearTimeout(timer);
    }
  }

  getProxySummary(accountId) {
    return this.accountProxies.get(accountId);
  }
}

module.exports = { ProxyManager };
    """.trimIndent() + "\n"

  fun geolocationProviderJs(): String =
    """
const TARGET_ORIGIN = 'https://trip.ferizy.com';

const PRESETS = {
  merak: { latitude: -5.9308, longitude: 105.9935 },
  bakauheni: { latitude: -5.8705, longitude: 105.7538 },
  ketapang: { latitude: -8.1449, longitude: 114.4036 },
  gilimanuk: { latitude: -8.1636, longitude: 114.4375 }
};

class GeolocationProvider {
  constructor(runtimeConfig) {
    this.runtimeConfig = runtimeConfig;
    this.accountGeo = new Map();
    const accounts = Array.isArray(runtimeConfig.accounts) ? runtimeConfig.accounts : [];
    const ids = new Set([1, 2, 3, 4, 5]);
    accounts.forEach((a) => {
      if (a && Number.isInteger(a.accountId)) ids.add(a.accountId);
    });
    for (const id of ids) {
      this.ensureAccount(id, accounts.find((a) => a.accountId === id));
    }
  }

  ensureAccount(accountId, found = null) {
    if (this.accountGeo.has(accountId)) return this.accountGeo.get(accountId);
    const accounts = Array.isArray(this.runtimeConfig.accounts) ? this.runtimeConfig.accounts : [];
    const cfg = found || accounts.find((a) => a.accountId === accountId);
    const g = (cfg && cfg.geolocation) || {};
    const entry = {
      mode: g.mode || 'preset_0ms',
      preset: g.preset || 'merak',
      latitude: typeof g.latitude === 'number' ? g.latitude : PRESETS.merak.latitude,
      longitude: typeof g.longitude === 'number' ? g.longitude : PRESETS.merak.longitude
    };
    this.accountGeo.set(accountId, entry);
    return entry;
  }

  isTrustedFerizyOrigin(originOrUrl) {
    return typeof originOrUrl === 'string' && originOrUrl.startsWith(TARGET_ORIGIN);
  }

  attachToSession(ses, accountId) {
    ses.setPermissionRequestHandler((webContents, permission, callback, details) => {
      const origin = (details && details.requestingUrl) || webContents.getURL() || '';
      if ((permission === 'geolocation' || permission === 'notifications') && this.isTrustedFerizyOrigin(origin)) {
        callback(true);
        return;
      }
      callback(false);
    });

    ses.setPermissionCheckHandler((webContents, permission, requestingOrigin) => {
      const origin = requestingOrigin || (webContents && webContents.getURL()) || '';
      if ((permission === 'geolocation' || permission === 'notifications') && this.isTrustedFerizyOrigin(origin)) {
        return true;
      }
      return false;
    });
  }

  updateAccountGeo(accountId, nextGeo) {
    const presetCoords = PRESETS[nextGeo.preset];
    const baseLat = presetCoords ? presetCoords.latitude : Number(nextGeo.latitude) || PRESETS.merak.latitude;
    const baseLng = presetCoords ? presetCoords.longitude : Number(nextGeo.longitude) || PRESETS.merak.longitude;

    const updated = {
      mode: nextGeo.mode || 'preset_0ms',
      preset: nextGeo.preset || 'merak',
      latitude: baseLat,
      longitude: baseLng
    };
    this.accountGeo.set(accountId, updated);
    return this.getEffectiveGeo(accountId);
  }

  getEffectiveGeo(accountId) {
    const cfg = this.accountGeo.get(accountId) || {
      mode: 'preset_0ms',
      preset: 'merak',
      latitude: PRESETS.merak.latitude,
      longitude: PRESETS.merak.longitude
    };

    const latJitter = (accountId - 3) * 0.00009;
    const lngJitter = (accountId % 2 === 0 ? 1 : -1) * accountId * 0.00007;

    return {
      ...cfg,
      effectiveLatitude: Number((cfg.latitude + latJitter).toFixed(6)),
      effectiveLongitude: Number((cfg.longitude + lngJitter).toFixed(6)),
      accuracy: 15 + accountId * 3
    };
  }
}

module.exports = { GeolocationProvider };
    """.trimIndent() + "\n"

  fun trayManagerJs(): String =
    """
const zlib = require('zlib');
const { Tray, Menu, nativeImage } = require('electron');

function buildProgrammaticTrayPng() {
  const width = 32;
  const height = 32;
  const raw = Buffer.alloc((width * 4 + 1) * height);

  for (let y = 0; y < height; y++) {
    const rowStart = y * (width * 4 + 1);
    raw[rowStart] = 0;
    for (let x = 0; x < width; x++) {
      const px = rowStart + 1 + x * 4;
      const inBorder = x >= 3 && x <= 28 && y >= 3 && y <= 28;
      const inBow = y >= 10 && y <= 22 && x >= 8 && x <= 23 && (x + y > 22);
      if (inBow) {
        raw[px] = 0x22;
        raw[px + 1] = 0xd3;
        raw[px + 2] = 0xee;
        raw[px + 3] = 0xff;
      } else if (inBorder) {
        raw[px] = 0x0b;
        raw[px + 1] = 0x15;
        raw[px + 2] = 0x28;
        raw[px + 3] = 0xff;
      } else {
        raw[px] = 0x3b;
        raw[px + 1] = 0x82;
        raw[px + 2] = 0xf6;
        raw[px + 3] = 0xff;
      }
    }
  }

  function crc32(buf) {
    let c = 0xffffffff;
    for (let i = 0; i < buf.length; i++) {
      c ^= buf[i];
      for (let k = 0; k < 8; k++) {
        c = (c >>> 1) ^ (c & 1 ? 0xedb88320 : 0);
      }
    }
    return (c ^ 0xffffffff) >>> 0;
  }

  function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length, 0);
    const typeBuf = Buffer.from(type, 'ascii');
    const crcBuf = Buffer.alloc(4);
    crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
    return Buffer.concat([len, typeBuf, data, crcBuf]);
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  const idat = zlib.deflateSync(raw);

  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', ihdr),
    chunk('IDAT', idat),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

class TrayManager {
  constructor(onShow, onQuit) {
    this.onShow = onShow;
    this.onQuit = onQuit;
    this.tray = null;
    this.notifiedOnce = false;
  }

  init() {
    const icon = nativeImage.createFromBuffer(buildProgrammaticTrayPng());
    this.tray = new Tray(icon);
    this.tray.setToolTip('Ferizy Multi-Container Browser (Fase 4 • 24 Jam Siap)');

    const menu = Menu.buildFromTemplate([
      { label: 'Buka Jendela Ferizy Browser', click: () => this.onShow() },
      { type: 'separator' },
      { label: 'Mode 24 Jam (Sniffer 0ms + Notif Main Process)', enabled: false },
      { type: 'separator' },
      { label: 'Keluar Total', click: () => this.onQuit() }
    ]);
    this.tray.setContextMenu(menu);
    this.tray.on('double-click', () => this.onShow());
  }

  notifyHiddenToTray() {
    if (this.tray && !this.notifiedOnce && process.platform === 'win32') {
      this.notifiedOnce = true;
      this.tray.displayBalloon({
        title: 'Ferizy Multi-Container Berjalan di Latar',
        content: '5 container akun tetap aktif 24 jam. Klik ganda ikon tray untuk membuka kembali.'
      });
    }
  }

  destroy() {
    if (this.tray) {
      this.tray.destroy();
      this.tray = null;
    }
  }
}

module.exports = { TrayManager };
    """.trimIndent() + "\n"

  fun preflightServiceJs(): String =
    """
class PreflightService {
  constructor(
    runtimeConfig,
    sessionManager,
    proxyManager,
    geoProvider,
    assetBlocker,
    pointStore,
    automationEngine,
    memoryWatchdog,
    serverClockSync,
    notificationCenter,
    optionalModules,
    logRotation,
    enduranceMonitor
  ) {
    this.runtimeConfig = runtimeConfig;
    this.sessionManager = sessionManager;
    this.proxyManager = proxyManager;
    this.geoProvider = geoProvider;
    this.assetBlocker = assetBlocker;
    this.pointStore = pointStore;
    this.automationEngine = automationEngine;
    this.memoryWatchdog = memoryWatchdog;
    this.serverClockSync = serverClockSync;
    this.notificationCenter = notificationCenter;
    this.optionalModules = optionalModules;
    this.logRotation = logRotation;
    this.enduranceMonitor = enduranceMonitor;
    this.loginStates = new Map();
  }

  async refreshLoginCookies() {
    const ids = this.sessionManager.getAccountIds();
    for (const id of ids) {
      const ses = this.sessionManager.getSession(id);
      if (!ses) continue;
      try {
        const cookies = await ses.cookies.get({ domain: 'ferizy.com' });
        const tabs = this.sessionManager.getAccountTabs(id);
        const onLoginPage = tabs.length > 0 && tabs.every((t) => /\/login(\?|$)/i.test(t.url || ''));
        const hasAuthCookie = cookies.some((c) => /token|session|auth|ferizy/i.test(c.name));
        this.loginStates.set(id, hasAuthCookie && !onLoginPage);
      } catch (_) {
        this.loginStates.set(id, false);
      }
    }
  }

  getFullSnapshot() {
    const ntfyTopic = this.notificationCenter.getTopic();
    const accountIds = this.sessionManager.getAccountIds();

    const accounts = [];
    for (const id of accountIds) {
      const accSplitState = this.sessionManager.getAccountSplitState(id);
      const tabs = this.sessionManager.getAccountTabs(id).map((t) => {
        const runner = this.automationEngine.getRunnerState(t.tabId);
        const tIdx = t.indexInAccount || 1;
        const tabPts = this.pointStore.getAccountPoints(id, tIdx);
        const tabExt = this.pointStore.getAccountSettings(id, tIdx);
        return {
          tabId: t.tabId,
          accountId: t.accountId,
          indexInAccount: tIdx,
          title: t.title,
          url: t.url,
          canGoBack: this.sessionManager.canTabGoBack(t.tabId),
          isLoading: t.isLoading,
          lastError: t.lastError || null,
          runnerState: runner.state,
          wonUrl: runner.wonUrl,
          memoryMb: this.memoryWatchdog.getTabMemoryMb(t.tabId),
          points: tabPts,
          pointsCount: tabPts.length,
          extensionSettings: tabExt
        };
      });

      const activeTabInAcc = tabs.find((t) => t.tabId === accSplitState.activeTabId) || tabs[0] || null;
      const activeTabIndex = activeTabInAcc ? activeTabInAcc.indexInAccount : 1;
      const proxy = this.proxyManager.getProxySummary(id);
      const geo = this.geoProvider.getEffectiveGeo(id);
      const pts = this.pointStore.getAccountPoints(id, activeTabIndex);
      const extensionSettings = this.pointStore.getAccountSettings(id, activeTabIndex);
      const totalAccountPoints = tabs.reduce((sum, t) => sum + (t.pointsCount || 0), 0);
      const accSplitSlots = (accSplitState.splitTabIds || []).map((tabId, idx) => {
        const t = this.sessionManager.tabs.get(tabId);
        return {
          slotIndex: idx,
          tabId,
          accountId: t ? t.accountId : id,
          indexInAccount: t ? t.indexInAccount : (idx + 1),
          title: t ? t.title : ''
        };
      });

      accounts.push({
        accountId: id,
        name: this.sessionManager.getAccountLabel(id),
        partition: `persist:akun_${'$'}{id}`,
        tabs,
        tabCount: tabs.length,
        maxTabs: 3,
        activeTabId: activeTabInAcc ? activeTabInAcc.tabId : null,
        activeTabIndex,
        loginActive: Boolean(this.loginStates.get(id)),
        proxy,
        geo,
        pointsCount: pts.length,
        totalAccountPoints,
        pointsReady: pts.length > 0,
        points: pts,
        extensionSettings,
        storeFile: `akun_${'$'}{id}.json`,
        blockedAssets: this.assetBlocker.getBlockedCount(id),
        lastKeepAliveAt: this.optionalModules.getLastKeepAliveAt(id),
        ntfyTopic,
        isActiveAccount: id === this.sessionManager.activeAccountId,
        autoSplitEnabled: accSplitState.autoSplitEnabled !== false,
        splitMode: accSplitState.splitMode || 1,
        activeSlotIndex: accSplitState.activeSlotIndex || 0,
        splitSlots: accSplitSlots
      });
    }

    const activeAccId = this.sessionManager.activeAccountId || accountIds[0] || 1;
    const activeAccSplit = this.sessionManager.getAccountSplitState(activeAccId);
    const activeAccTabs = this.sessionManager.getAccountTabs(activeAccId);
    const activeTabObj = activeAccTabs.find((t) => t.tabId === (activeAccSplit.activeTabId || this.sessionManager.activeTabId)) || activeAccTabs[0] || null;
    const splitSlots = (activeAccSplit.splitTabIds || []).map((tabId, idx) => {
      const t = this.sessionManager.tabs.get(tabId);
      return {
        slotIndex: idx,
        tabId,
        accountId: t ? t.accountId : activeAccId,
        indexInAccount: t ? t.indexInAccount : (idx + 1),
        title: t ? t.title : ''
      };
    });

    const activeTabIdxVal = activeTabObj ? activeTabObj.indexInAccount : 1;
    const buildCollectionForGroup = (gIdx) =>
      accounts
        .map((acc) => {
          const foundTab = Array.isArray(acc.tabs) ? acc.tabs.find((t) => t.indexInAccount === gIdx) : null;
          if (!foundTab) return null;
          const isRunningAk = foundTab.runnerState === 'RUNNING';
          return {
            tabId: foundTab.tabId,
            accountId: acc.accountId,
            accountName: acc.name || `Akun ${'$'}{acc.accountId}`,
            indexInAccount: gIdx,
            title: foundTab.title,
            url: foundTab.url,
            isLoading: foundTab.isLoading,
            runnerState: foundTab.runnerState,
            isMirrorDisconnectedByAutoKlik: isRunningAk,
            pointsCount: foundTab.pointsCount || 0,
            isActiveMaster: !isRunningAk && foundTab.tabId === this.sessionManager.activeTabId
          };
        })
        .filter(Boolean);

    const tab1CollectionTabs = buildCollectionForGroup(1);
    const tab2CollectionTabs = buildCollectionForGroup(2);
    const tab3CollectionTabs = buildCollectionForGroup(3);

    const activeTabRunner = activeTabObj ? this.automationEngine.getRunnerState(activeTabObj.tabId) : null;
    const isActiveTabRunningAk = Boolean(activeTabRunner && activeTabRunner.state === 'RUNNING');

    const grpMirrorMap = this.sessionManager.tabGroupMirrorEnabled || { 1: Boolean(this.sessionManager.tab1MirrorEnabled), 2: false, 3: false };
    const colNames = this.sessionManager.collectionNames || {};
    const tabGroupsMirror = {
      1: {
        groupIndex: 1,
        name: colNames[1] || '📦 Kumpulan Tab 1',
        mirrorEnabled: Boolean(grpMirrorMap[1]),
        mirrorActive: Boolean(grpMirrorMap[1] && activeTabIdxVal === 1 && !isActiveTabRunningAk),
        splitActive: this.sessionManager.tabCollectionSplitGroup === 1 || Boolean(this.sessionManager.tab1CollectionSplitActive),
        tabs: tab1CollectionTabs
      },
      2: {
        groupIndex: 2,
        name: colNames[2] || '📦 Kumpulan Tab 2',
        mirrorEnabled: Boolean(grpMirrorMap[2]),
        mirrorActive: Boolean(grpMirrorMap[2] && activeTabIdxVal === 2 && !isActiveTabRunningAk),
        splitActive: this.sessionManager.tabCollectionSplitGroup === 2,
        tabs: tab2CollectionTabs
      },
      3: {
        groupIndex: 3,
        name: colNames[3] || '📦 Kumpulan Tab 3',
        mirrorEnabled: Boolean(grpMirrorMap[3]),
        mirrorActive: Boolean(grpMirrorMap[3] && activeTabIdxVal === 3 && !isActiveTabRunningAk),
        splitActive: this.sessionManager.tabCollectionSplitGroup === 3,
        tabs: tab3CollectionTabs
      }
    };

    const tab1MirrorEnabled = tabGroupsMirror[1].mirrorEnabled;
    const tab1MirrorActive = tabGroupsMirror[1].mirrorActive;
    const tab1CollectionSplitActive = tabGroupsMirror[1].splitActive;

    return {
      phase: 'Fase 4',
      accountIds,
      maxDynamicAccounts: 12,
      activeAccountId: activeAccId,
      activeTabId: this.sessionManager.activeTabId,
      activeTabIndex: activeTabIdxVal,
      tab1MirrorEnabled,
      tab1MirrorActive,
      tab1CollectionSplitActive,
      tab1CollectionTabs,
      tab2CollectionTabs,
      tab3CollectionTabs,
      tabCollectionSplitGroup: this.sessionManager.tabCollectionSplitGroup || (tab1CollectionSplitActive ? 1 : 0),
      tabGroupsMirror,
      tab1MirrorAutoOffReason: this.sessionManager.tab1MirrorAutoOffReason || null,
      totalTabs: this.sessionManager.tabs.size,
      maxTotalTabs: accountIds.length * 3,
      globalZoomPercent: this.sessionManager.globalZoomPercent,
      splitMode: activeAccSplit.splitMode || 1,
      activeSlotIndex: activeAccSplit.activeSlotIndex || 0,
      splitSlots,
      paneBounds: this.sessionManager.lastPaneBounds || [],
      ntfyTopic,
      closeToTray: Boolean(this.runtimeConfig.closeToTray),
      powerSaveBlocker: Boolean(this.runtimeConfig.powerSaveBlocker),
      assetBlockerMode: this.assetBlocker.mode,
      memoryWatchdogLimitMb: this.memoryWatchdog.limitMb,
      staggeredReloadDelayMs: this.memoryWatchdog.delayMs,
      recycleCount: this.memoryWatchdog.recycleCount,
      lazyAntiRevertEnabled: Boolean(this.runtimeConfig.lazyAntiRevertEnabled),
      clockSync: this.serverClockSync.getSummary(),
      optionalModules: this.optionalModules.getState(),
      notificationSettings: this.notificationCenter.getSettings(),
      recentNotifications: this.notificationCenter.recentLogs.slice(0, 5),
      logStats: this.logRotation ? this.logRotation.getStats() : null,
      enduranceSummary: this.enduranceMonitor ? this.enduranceMonitor.getSummary() : null,
      accounts
    };
  }
}

module.exports = { PreflightService };
    """.trimIndent() + "\n"
}
