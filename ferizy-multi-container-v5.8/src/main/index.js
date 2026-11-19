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
        accountName: accSnap.name || `Akun ${tab.accountId}`,
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
        storeFile: accSnap.storeFile || `akun_${tab.accountId}.json`
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
    title: 'Ferizy Multi-Container Browser • V5.8',
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
    if (!tabs.length) return { ok: false, reason: `Akun ${accountId} belum memiliki tab` };
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
      return { ok: false, reason: `Tab Akun ${accountId} tidak ditemukan` };
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
          label: payload.label || `Titik ${nextOrder}`,
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
      title: `Save Konfigurasi Titik Akun ${id} Tab ${tIdx}`,
      defaultPath: path.join(defaultDir, `autoklik-akun-${id}-tab-${tIdx}.json`),
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
        storeFile: `akun_${id}.json`
      };
    }
    return {
      ok: true,
      canceled: true,
      accountId: id,
      tabIndex: tIdx,
      filePath: pointStore.getAccountStorePath(id),
      storeFile: `akun_${id}.json`
    };
  });

  ipcMain.handle('autoklik-ext:open-dialog', async (_evt, accountId, tabIndex = null) => {
    const id = Number(accountId) || 1;
    const tIdx = sessionManager.resolveTabIndex(id, tabIndex);
    const defaultDir = app.getPath('documents') || app.getPath('userData');
    const res = await dialog.showOpenDialog(mainWindow, {
      title: `Open Konfigurasi Titik Akun ${id} Tab ${tIdx}`,
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
        storeFile: `akun_${id}.json`
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
      storeFile: profile.storeFile || `akun_${accId}.json`
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
