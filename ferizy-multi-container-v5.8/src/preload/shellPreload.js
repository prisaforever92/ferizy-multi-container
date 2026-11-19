const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('ferizyShell', {
  getState: () => ipcRenderer.invoke('shell:get-state'),
  setHeaderHeight: (height) => ipcRenderer.invoke('shell:set-header-height', height),
  createTab: (accountId, url) => ipcRenderer.invoke('shell:create-tab', accountId, url),
  switchTab: (tabId) => ipcRenderer.invoke('shell:switch-tab', tabId),
  switchAccount: (accountId) => ipcRenderer.invoke('shell:switch-account', accountId),
  closeTab: (tabId) => ipcRenderer.invoke('shell:close-tab', tabId),
  reloadTab: (tabId) => ipcRenderer.invoke('shell:reload-tab', tabId),
  goBackTab: (tabId) => ipcRenderer.invoke('shell:go-back-tab', tabId),
  navigateTab: (tabId, url) => ipcRenderer.invoke('shell:navigate-tab', tabId, url),
  updateProxy: (accountId, proxyCfg) => ipcRenderer.invoke('shell:update-proxy', accountId, proxyCfg),
  checkProxyIp: (accountId) => ipcRenderer.invoke('shell:check-proxy-ip', accountId),
  updateGeo: (accountId, geoCfg) => ipcRenderer.invoke('shell:update-geo', accountId, geoCfg),
  refreshPreflight: () => ipcRenderer.invoke('shell:refresh-preflight'),
  // Fase 2 APIs
  startTabAutomation: (tabId) => ipcRenderer.invoke('automation:start-tab', tabId),
  stopTabAutomation: (tabId) => ipcRenderer.invoke('automation:stop-tab', tabId),
  resetWonTab: (tabId) => ipcRenderer.invoke('automation:reset-won-tab', tabId),
  copyAccountPoints: (accountId) => ipcRenderer.invoke('points:copy-account', accountId),
  pastePointsToAll: (accountId) => ipcRenderer.invoke('points:paste-all', accountId),
  reloadStandbyQueue: () => ipcRenderer.invoke('watchdog:reload-standby'),
  setAssetBlockerMode: (mode) => ipcRenderer.invoke('asset-blocker:set-mode', mode),
  // Fase 3 APIs
  updateOptionalFlags: (flags) => ipcRenderer.invoke('optional:update-flags', flags),
  updateSchedule: (sched) => ipcRenderer.invoke('clock:update-schedule', sched),
  updateNotifSettings: (cfg) => ipcRenderer.invoke('notif:update-settings', cfg),
  resumeCaptchaAll: () => ipcRenderer.invoke('automation:resume-captcha-all'),
  testNtfy: () => ipcRenderer.invoke('notif:test-ntfy'),
  // Fase 4 APIs
  updateFase4Config: (cfg) => ipcRenderer.invoke('fase4:update-config', cfg),
  runEnduranceSelfCheck: () => ipcRenderer.invoke('endurance:run-self-check'),
  exportHealthReport: () => ipcRenderer.invoke('endurance:export-health-report'),
  pruneLogsNow: () => ipcRenderer.invoke('logs:prune-now'),
  openLogsFolder: () => ipcRenderer.invoke('logs:open-folder'),
  // Dynamic Akun (>5), Rename Akun, Global Zoom, dan Split View APIs
  addAccount: () => ipcRenderer.invoke('accounts:add'),
  removeAccount: (accountId) => ipcRenderer.invoke('accounts:remove', accountId),
  renameAccount: (accountId, nextName) => ipcRenderer.invoke('account:rename', accountId, nextName),
  renameCollection: (groupIdx, nextName) => ipcRenderer.invoke('collection:rename', groupIdx, nextName),
  startAllCollection: (groupIdx) => ipcRenderer.invoke('collection:start-all', groupIdx),
  reloadCollection: (groupIdx) => ipcRenderer.invoke('collection:reload', groupIdx),
  setGlobalZoom: (percent) => ipcRenderer.invoke('view:set-zoom', percent),
  setSplitMode: (count) => ipcRenderer.invoke('view:set-split-mode', count),
  setActiveSlot: (slotIndex) => ipcRenderer.invoke('view:set-active-slot', slotIndex),
  assignSlotTab: (slotIndex, tabId) => ipcRenderer.invoke('view:assign-slot-tab', slotIndex, tabId),
  pushAccountToSplit: (accountId) => ipcRenderer.invoke('view:push-account-to-split', accountId),
  setAccountSplit: (accountId, count) => ipcRenderer.invoke('view:set-account-split', accountId, count),
  toggleAccountSplit: (accountId) => ipcRenderer.invoke('view:toggle-account-split', accountId),
  setModalOpen: (isOpen) => ipcRenderer.invoke('ui:set-modal-open', isOpen),
  setTab1Mirror: (enabled) => ipcRenderer.invoke('mirror:set-tab1', enabled),
  toggleTab1Mirror: () => ipcRenderer.invoke('mirror:toggle-tab1'),
  setTab1CollectionSplit: (enabled) => ipcRenderer.invoke('mirror:set-tab1-collection-split', enabled),
  toggleTab1CollectionSplit: () => ipcRenderer.invoke('mirror:toggle-tab1-collection-split'),
  setTabGroupMirror: (tabIndex, enabled) => ipcRenderer.invoke('mirror:set-tab-group', tabIndex, enabled),
  toggleTabGroupMirror: (tabIndex) => ipcRenderer.invoke('mirror:toggle-tab-group', tabIndex),
  setTabCollectionSplit: (tabIndex, enabled) => ipcRenderer.invoke('mirror:set-tab-collection-split', tabIndex, enabled),
  toggleTabCollectionSplit: (tabIndex) => ipcRenderer.invoke('mirror:toggle-tab-collection-split', tabIndex),
  copyAccountPoints: (accountId, tabIndex = null) => ipcRenderer.invoke('points:copy-account', accountId, tabIndex),
  pastePointsToAll: (accountId, tabIndex = null) => ipcRenderer.invoke('points:paste-all', accountId, tabIndex),
  copyTabConfig: (accountId, tabIndex = null) => ipcRenderer.invoke('points:copy-tab', accountId, tabIndex),
  pasteTabConfig: (targetAccountId, targetTabIndex = null, sourceAccountId = null, sourceTabIndex = null) =>
    ipcRenderer.invoke('points:paste-tab', targetAccountId, targetTabIndex, sourceAccountId, sourceTabIndex),
  pasteToAllTabsInAccount: (targetAccountId, sourceAccountId = null, sourceTabIndex = null) =>
    ipcRenderer.invoke('points:paste-account-tabs', targetAccountId, sourceAccountId, sourceTabIndex),
  getClipboardSummary: () => ipcRenderer.invoke('points:get-clipboard'),
  // AutoKlik-v3.2-Mandiri-Extension Per-Tab APIs
  getExtProfile: (accountId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:get-profile', accountId, tabIndex),
  saveExtSettings: (accountId, nextSettings, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:save-settings', accountId, nextSettings, tabIndex),
  addExtPoint: (accountId, payload, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:add-point', accountId, payload, tabIndex),
  updateExtPoint: (accountId, pointId, patch, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:update-point', accountId, pointId, patch, tabIndex),
  removeExtPoint: (accountId, pointId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:remove-point', accountId, pointId, tabIndex),
  removeLastExtPoint: (accountId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:remove-last-point', accountId, tabIndex),
  clearExtPoints: (accountId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:clear-points', accountId, tabIndex),
  toggleExtHidden: (accountId, hidden, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:toggle-hidden', accountId, hidden, tabIndex),
  toggleExtLock: (accountId, locked, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:toggle-lock', accountId, locked, tabIndex),
  moveExtPoint: (accountId, pointId, dir, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:move-point', accountId, pointId, dir, tabIndex),
  exportExtJson: (accountId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:export-json', accountId, tabIndex),
  importExtJson: (accountId, rawJson, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:import-json', accountId, rawJson, tabIndex),
  startExtAccount: (accountId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:start-account', accountId, tabIndex),
  stopExtAccount: (accountId) => ipcRenderer.invoke('autoklik-ext:stop-account', accountId),
  startExtPicker: (accountId, pointType, replacePointId = null, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:start-picker', accountId, pointType, replacePointId, tabIndex),
  stopExtPicker: (accountId) => ipcRenderer.invoke('autoklik-ext:stop-picker', accountId),
  savePointConfigDialog: (accountId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:save-dialog', accountId, tabIndex),
  openPointConfigDialog: (accountId, tabIndex = null) => ipcRenderer.invoke('autoklik-ext:open-dialog', accountId, tabIndex),
  onExtPickerFinished: (callback) => {
    const handler = (_event, payload) => callback(payload);
    ipcRenderer.on('autoklik-ext:picker-finished', handler);
    return () => ipcRenderer.removeListener('autoklik-ext:picker-finished', handler);
  },
  onOpenPointEditor: (callback) => {
    const handler = (_event, payload) => callback(payload);
    ipcRenderer.on('autoklik-ext:open-point-editor', handler);
    return () => ipcRenderer.removeListener('autoklik-ext:open-point-editor', handler);
  },
  onStateUpdated: (callback) => {
    const handler = (_event, snapshot) => callback(snapshot);
    ipcRenderer.on('shell:state-updated', handler);
    return () => ipcRenderer.removeListener('shell:state-updated', handler);
  },
  onPlayAlarm: (callback) => {
    const handler = (_event, payload) => callback(payload);
    ipcRenderer.on('notif:play-alarm', handler);
    return () => ipcRenderer.removeListener('notif:play-alarm', handler);
  },
  getSystemMetrics: () => ipcRenderer.invoke('system:get-metrics'),
  runQuickClean: () => ipcRenderer.invoke('system:quick-clean'),
  onSystemMetrics: (callback) => {
    const handler = (_event, payload) => callback(payload);
    ipcRenderer.on('system:metrics', handler);
    return () => ipcRenderer.removeListener('system:metrics', handler);
  }
});
