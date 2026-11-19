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
          this.tab1MirrorAutoOffReason = `Otomatis PUTUS untuk ${this.getAccountLabel(tab.accountId)} • Tab ${tIdx} yang sedang Start AutoKlik`;
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
        this.tab1MirrorAutoOffReason = `Otomatis OFF saat memilih Tab ${activeIdx} (Tab 2/3 mandiri)`;
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
            reason: `Semua Tab ${targetIdx} sedang menjalankan AutoKlik (Mirror otomatis PUTUS untuk tab yang sedang Start AutoKlik).`
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
    return this.accountNames.get(id) || `Akun ${id}`;
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

    const partition = `persist:akun_${accountId}`;
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
      return { ok: false, reason: `Maksimal ${MAX_DYNAMIC_ACCOUNTS} container akun tercapai` };
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
      return { ok: false, reason: `Akun ${accountId} belum terdaftar di container` };
    }
    const existing = this.getAccountTabs(accountId);
    if (existing.length >= MAX_TABS_PER_ACCOUNT) {
      return { ok: false, reason: `Hard Cap 3/3 tercapai untuk Akun ${accountId}` };
    }
    if (this.tabs.size >= this.accountIds.length * MAX_TABS_PER_ACCOUNT) {
      return { ok: false, reason: `Batas global ${this.accountIds.length * MAX_TABS_PER_ACCOUNT} tab tercapai` };
    }

    const ses = this.sessions.get(accountId);
    if (!ses) {
      return { ok: false, reason: 'Session container belum diinisialisasi' };
    }

    const resolvedUrl = targetUrl || (existing.length === 0 ? (this.runtimeConfig.targetUrl || 'https://trip.ferizy.com') : 'https://trip.ferizy.com/order');
    this.tabCounter += 1;
    const tabId = `akun_${accountId}_tab_${this.tabCounter}`;
    const view = new WebContentsView({
      webPreferences: {
        session: ses,
        preload: path.join(__dirname, '../preload/ferizyPreload.js'),
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        backgroundThrottling: false,
        additionalArguments: [`--ferizy-account-id=${accountId}`]
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
      title: `${accLabel} • Tab ${existing.length + 1}`,
      isLoading: true,
      lastError: null
    };
    this.tabs.set(tabId, tabMeta);

    if (this.automationEngine) {
      this.automationEngine.attachWinDetector(tabMeta);
    }

    view.webContents.setWindowOpenHandler(({ url }) => {
      // Cegah Tab Liar (popup / target="_blank" akibat klik otomatis atau iklan/link eksternal) membuka tab baru
      console.log(`[SessionManager] Mencegah tab liar (window.open) pada Akun ${accountId}:`, url);
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
      tabMeta.lastError = `${errorDescription || 'ERR_FAILED'} (${errorCode})`;
      this.renderLoadErrorPage(view, failedUrl, errorCode, errorDescription, accountId);
      this.onStateChange();
    });

    view.webContents.on('render-process-gone', (_e, details) => {
      tabMeta.isLoading = false;
      tabMeta.lastError = `Renderer berhenti (${(details && details.reason) || 'unknown'})`;
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
          label: `Buka Link di Tab Baru Akun ${accountId}`,
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
        label: `Salin Koordinat Klik (x=${cssClickX}, y=${cssClickY})`,
        click: () => clipboard.writeText(`${cssClickX},${cssClickY}`)
      });

      if (this.pointStore) {
        template.push({
          label: `+ Tambah Titik AutoKlik Tab ${tabMeta.indexInAccount} (x=${cssClickX}, y=${cssClickY})`,
          click: () => {
            const tIdx = tabMeta.indexInAccount || 1;
            const settings = this.pointStore.getAccountSettings(accountId, tIdx);
            const nextNum = this.pointStore.getAccountPoints(accountId, tIdx).length + 1;
            this.pointStore.addAccountPoint(accountId, {
              type: params.isEditable ? 'input' : 'tap',
              label: `Titik ${nextNum}`,
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
      console.warn(`[Akun ${accountId}] Gagal load URL:`, tabMeta.lastError);
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
      <h2>Gagal Memuat Halaman (Akun ${accountId})</h2>
      <div>Kode Error Chromium: <b>${safeDesc} (${errorCode})</b></div>
      <code>${safeUrl}</code>
      <ul>
        <li>Jika muncul <b>ERR_NAME_NOT_RESOLVED</b> / <b>ERR_CONNECTION_TIMED_OUT</b>: cek Wi-Fi Windows, DNS (1.1.1.1 / 8.8.8.8), atau matikan blokir/firewall sementara.</li>
        <li>Jika akun memakai Proxy: klik tombol <b>Proxy/Geo</b> pada kartu akun, pastikan IP:Port/Auth valid atau kembalikan ke <b>Direct ISP</b> untuk tes.</li>
        <li>Tekan <b>Muat Ulang</b> atau ketik ulang URL lalu tekan <b>Enter</b> / tombol <b>Buka</b>.</li>
      </ul>
    </div></body></html>`;
    view.webContents.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`).catch(() => {});
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
      this.tab1MirrorAutoOffReason = `Otomatis OFF saat memilih Tab ${nextTab.indexInAccount} (Tab 2/3 mandiri)`;
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

    const normalized = /^https?:\/\//i.test(clean) ? clean : `https://${clean}`;
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
