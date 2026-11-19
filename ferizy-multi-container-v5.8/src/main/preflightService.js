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
        partition: `persist:akun_${id}`,
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
        storeFile: `akun_${id}.json`,
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
            accountName: acc.name || `Akun ${acc.accountId}`,
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
