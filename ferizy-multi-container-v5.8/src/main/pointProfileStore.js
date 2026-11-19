const fs = require('fs');
const path = require('path');
const { app } = require('electron');

const DEFAULT_EXTENSION_SETTINGS = {
  enabled: true,
  loopEnabled: true,
  maxLoops: 0,
  loopDelayMs: 100,
  radiusPx: 28,
  smartMatch: true,
  antiRevert: false,
  humanRhythm: true,
  cursorMoveMs: 150,
  hoverSettleMs: 50,
  markersHidden: false,
  pointsLocked: false,
  mainLoopStartOrder: 1
};

class PointProfileStore {
  constructor(templatePath) {
    this.templatePath = templatePath;
    this.containersDir = path.join(app.getPath('userData'), 'autoklik-containers');
    this.legacyStorePath = path.join(app.getPath('userData'), 'ferizy-points-v3.2.json');
    this.clipboardPoints = null;
    this.clipboardSettings = null;
    this.templateData = this.readJsonSafe(this.templatePath) || {
      schema: 'AutoKlikPro',
      version: '3.2',
      extensionName: 'AutoKlik-v3.2-Mandiri-Extension',
      ntfyTopic: 'ferizy_Transsamudra05',
      containers: {}
    };
    this.legacyData = this.readJsonSafe(this.legacyStorePath);
    this.containersMap = new Map();
    this.ensureDir();
  }

  readJsonSafe(filePath) {
    try {
      return JSON.parse(fs.readFileSync(filePath, 'utf8'));
    } catch (_) {
      return null;
    }
  }

  ensureDir() {
    try {
      fs.mkdirSync(this.containersDir, { recursive: true });
    } catch (_) {}
  }

  normalizeTabIndex(tabIndex = 1) {
    const n = Math.round(Number(tabIndex) || 1);
    return Math.min(3, Math.max(1, n));
  }

  getAccountStorePath(accountId) {
    const id = Number(accountId) || 1;
    return path.join(this.containersDir, `akun_${id}.json`);
  }

  persistAccount(accountId) {
    const id = Number(accountId) || 1;
    const c = this.containersMap.get(id);
    if (!c) return;
    try {
      this.ensureDir();
      const tab1 = this.getTabEntry(id, 1);
      const payload = {
        schema: 'AutoKlikPro',
        version: '3.2',
        releaseTag: 'V5.8',
        extensionName: 'AutoKlik-v3.2-Mandiri-Extension',
        accountId: c.accountId,
        partition: c.partition,
        label: c.label,
        tabs: c.tabs,
        extensionSettings: tab1.extensionSettings,
        points: tab1.points
      };
      fs.writeFileSync(this.getAccountStorePath(id), JSON.stringify(payload, null, 2), 'utf8');
    } catch (err) {
      console.error(`[PointProfileStore] Gagal simpan profil Akun ${id}:`, err.message);
    }
  }

  persist() {
    for (const id of this.containersMap.keys()) {
      this.persistAccount(id);
    }
  }

  normalizeSettings(raw = {}) {
    const r = raw && typeof raw === 'object' ? raw : {};
    return {
      enabled: r.enabled !== false,
      loopEnabled: r.loopEnabled !== undefined ? Boolean(r.loopEnabled) : DEFAULT_EXTENSION_SETTINGS.loopEnabled,
      maxLoops: Math.min(999, Math.max(0, Number.isFinite(Number(r.maxLoops)) ? Math.round(Number(r.maxLoops)) : DEFAULT_EXTENSION_SETTINGS.maxLoops)),
      loopDelayMs: Math.min(60000, Math.max(20, Number.isFinite(Number(r.loopDelayMs)) ? Math.round(Number(r.loopDelayMs)) : DEFAULT_EXTENSION_SETTINGS.loopDelayMs)),
      radiusPx: Math.min(200, Math.max(5, Number.isFinite(Number(r.radiusPx)) ? Math.round(Number(r.radiusPx)) : DEFAULT_EXTENSION_SETTINGS.radiusPx)),
      smartMatch: r.smartMatch !== false,
      antiRevert: Boolean(r.antiRevert),
      humanRhythm: r.humanRhythm !== false,
      cursorMoveMs: Math.min(600, Math.max(0, Number.isFinite(Number(r.cursorMoveMs)) ? Math.round(Number(r.cursorMoveMs)) : DEFAULT_EXTENSION_SETTINGS.cursorMoveMs)),
      hoverSettleMs: Math.min(300, Math.max(0, Number.isFinite(Number(r.hoverSettleMs)) ? Math.round(Number(r.hoverSettleMs)) : DEFAULT_EXTENSION_SETTINGS.hoverSettleMs)),
      markersHidden: Boolean(r.markersHidden),
      pointsLocked: Boolean(r.pointsLocked),
      mainLoopStartOrder: 1
    };
  }

  normalizePoint(raw = {}, accountId = 1, order = 1) {
    const allowedTypes = ['tap', 'swipe_up', 'swipe_down', 'swipe', 'input', 'popup_key'];
    let type = allowedTypes.includes(raw.type) ? raw.type : 'tap';
    const x = Number.isFinite(Number(raw.x)) ? Math.round(Number(raw.x)) : 540;
    const y = Number.isFinite(Number(raw.y)) ? Math.round(Number(raw.y)) : 420;
    if (type === 'swipe') {
      const legacyEndY = Number.isFinite(Number(raw.endY)) ? Number(raw.endY) : (y - 250);
      type = legacyEndY > y ? 'swipe_down' : 'swipe_up';
    }
    const base = {
      id: String(raw.id || `pt_${accountId}_${Date.now().toString(36)}_${order}`),
      order,
      type,
      label: String(raw.label || `Titik ${order}`).trim() || `Titik ${order}`,
      enabled: raw.enabled !== false,
      x,
      y,
      radiusPx: Math.min(200, Math.max(5, Number(raw.radiusPx) || 28)),
      selector: '',
      delayAfterMs: Math.min(3600000, Math.max(15, Number.isFinite(Number(raw.delayAfterMs)) ? Math.round(Number(raw.delayAfterMs)) : 500)),
      iconSizePct: Math.min(100, Math.max(15, Number.isFinite(Number(raw.iconSizePct)) ? Math.round(Number(raw.iconSizePct)) : 25)),
      subLoopEnabled: false,
      subLoopTargetOrder: 1,
      subLoopCount: 1
    };
    if (type === 'swipe_up' || type === 'swipe_down') {
      const fallbackLen = Number.isFinite(Number(raw.endY)) ? Math.max(60, Math.min(1200, Math.abs(Math.round(Number(raw.endY)) - y))) : 350;
      const lineLengthPx = Math.min(1500, Math.max(40, Number.isFinite(Number(raw.lineLengthPx)) ? Math.round(Number(raw.lineLengthPx)) : fallbackLen));
      const swipeForceScale = Math.min(20, Math.max(1, Number.isFinite(Number(raw.swipeForceScale)) ? Math.round(Number(raw.swipeForceScale)) : 2));
      base.lineLengthPx = lineLengthPx;
      base.swipeForceScale = swipeForceScale;
      base.endX = x;
      base.endY = type === 'swipe_down' ? (y + lineLengthPx) : Math.max(4, y - lineLengthPx);
      base.durationMs = Math.min(5000, Math.max(60, Number(raw.durationMs) || 260));
    } else if (type === 'input') {
      const fallbackText = String(raw.inputValue !== undefined ? raw.inputValue : (raw.text || ''));
      const rawCycles = Array.isArray(raw.cycleTexts) && raw.cycleTexts.length > 0
        ? raw.cycleTexts.map((v) => String(v ?? ''))
        : [fallbackText];
      base.cycleTexts = rawCycles;
      base.inputValue = rawCycles[0] || '';
      base.text = base.inputValue;
      base.typingSpeedMs = Math.min(1000, Math.max(1, Number.isFinite(Number(raw.typingSpeedMs)) && Number(raw.typingSpeedMs) > 0 ? Math.round(Number(raw.typingSpeedMs)) : 30));
      base.popupDelayMs = Math.min(10000, Math.max(20, Number.isFinite(Number(raw.popupDelayMs)) ? Math.round(Number(raw.popupDelayMs)) : 300));
      base.clearBeforeType = raw.clearBeforeType !== false;
      base.focusBeforeType = raw.focusBeforeType !== false;
      base.antiRevert = false;
      base.popupPinEnabled = Boolean(raw.popupPinEnabled);
      base.popupPinX = Number.isFinite(Number(raw.popupPinX)) ? Math.round(Number(raw.popupPinX)) : (x + 56);
      base.popupPinY = Number.isFinite(Number(raw.popupPinY)) ? Math.round(Number(raw.popupPinY)) : (y + 42);
    } else if (type === 'popup_key') {
      const k = String(raw.popupKey || raw.key || 'Enter').trim() || 'Enter';
      base.popupKey = k;
      base.key = k;
    }
    return base;
  }

  isLegacyDummyPointList(pts) {
    if (!Array.isArray(pts) || pts.length !== 4) return false;
    const p0 = pts[0] || {};
    const p1 = pts[1] || {};
    const p2 = pts[2] || {};
    const p3 = pts[3] || {};
    return (
      Number(p0.x) === 410 && Number(p0.y) === 320 &&
      Number(p1.x) === 540 && Number(p1.y) === 620 &&
      Number(p2.x) === 480 && Number(p2.y) === 490 &&
      Number(p3.x) === 640 && Number(p3.y) === 580 &&
      /^PENUMPANG AKUN/i.test(String(p2.inputValue || p2.text || ''))
    );
  }

  ensureContainer(accountId) {
    const id = Number(accountId) || 1;
    if (this.containersMap.has(id)) {
      return this.containersMap.get(id);
    }

    const key = `persist:akun_${id}`;
    const accountFile = this.getAccountStorePath(id);
    const savedOnDisk = this.readJsonSafe(accountFile);
    const legacyEntry = this.legacyData && this.legacyData.containers && this.legacyData.containers[key];
    // Jangan pernah auto-seed titik dummy dari templateData ke kontainer agar tidak terjadi klik liar di luar konfigurasi user
    const seed = savedOnDisk || legacyEntry || null;

    const isMigratedV51 = Boolean(
      savedOnDisk &&
      (savedOnDisk.releaseTag === 'V5.1' || savedOnDisk.releaseTag === 'V5.2' || savedOnDisk.releaseTag === 'V5.3' || savedOnDisk.releaseTag === 'V5.4' || savedOnDisk.releaseTag === 'V5.5' || savedOnDisk.releaseTag === 'V5.6' || savedOnDisk.releaseTag === 'V5.7' || savedOnDisk.releaseTag === 'V5.8') &&
      savedOnDisk.tabs
    );
    const tabs = {};
    for (const tIdx of ['1', '2', '3']) {
      const savedTab = seed && seed.tabs && seed.tabs[tIdx] ? seed.tabs[tIdx] : null;
      let rawPts = [];
      let rawSettings = null;
      if (savedTab) {
        rawPts = Array.isArray(savedTab.points) ? savedTab.points : [];
        rawSettings = savedTab.extensionSettings || null;
      } else if (tIdx === '1' && seed && Array.isArray(seed.points)) {
        rawPts = seed.points;
        rawSettings = seed.extensionSettings || null;
      }
      if (this.isLegacyDummyPointList(rawPts)) {
        rawPts = [];
      }
      const normSettings = this.normalizeSettings(rawSettings);
      if (!isMigratedV51) {
        normSettings.loopEnabled = true;
        normSettings.maxLoops = 0;
        normSettings.antiRevert = false;
        normSettings.humanRhythm = true;
        if (!rawSettings || rawSettings.loopDelayMs === 350) {
          normSettings.loopDelayMs = 100;
        }
      }
      const sortedRawPts = rawPts
        .slice()
        .sort((a, b) => (Number(a && a.order) || 0) - (Number(b && b.order) || 0));
      tabs[tIdx] = {
        extensionSettings: normSettings,
        points: sortedRawPts.map((pt, idx) => {
          const migratedPt = { ...pt, id: pt.id || `pt_${id}_t${tIdx}_${idx + 1}` };
          if (!isMigratedV51) {
            if (!migratedPt.delayAfterMs) {
              migratedPt.delayAfterMs = 500;
            }
            if (!migratedPt.typingSpeedMs || Number(migratedPt.typingSpeedMs) === 15) {
              migratedPt.typingSpeedMs = 30;
            }
            migratedPt.antiRevert = false;
            if (
              migratedPt.selector === "button[data-testid='btn-cari-jadwal']" ||
              migratedPt.selector === "input[name='passenger_name']"
            ) {
              migratedPt.selector = '';
            }
          }
          return this.normalizePoint(migratedPt, id, idx + 1);
        })
      };
    }

    const container = {
      accountId: id,
      partition: key,
      label: (seed && seed.label) || `Akun ${id}`,
      tabs
    };
    this.containersMap.set(id, container);
    if (!savedOnDisk || !isMigratedV51) {
      this.persistAccount(id);
    }
    return container;
  }

  getTabEntry(accountId, tabIndex = 1) {
    const c = this.ensureContainer(accountId);
    const tKey = String(this.normalizeTabIndex(tabIndex));
    if (!c.tabs[tKey]) {
      c.tabs[tKey] = {
        extensionSettings: this.normalizeSettings({}),
        points: []
      };
    }
    return c.tabs[tKey];
  }

  getAccountSettings(accountId, tabIndex = 1) {
    const entry = this.getTabEntry(accountId, tabIndex);
    return { ...entry.extensionSettings };
  }

  saveAccountSettings(accountId, nextSettings, tabIndex = 1) {
    const c = this.ensureContainer(accountId);
    const entry = this.getTabEntry(accountId, tabIndex);
    entry.extensionSettings = this.normalizeSettings({ ...entry.extensionSettings, ...(nextSettings || {}) });
    this.persistAccount(c.accountId);
    return { ...entry.extensionSettings };
  }

  getAccountPoints(accountId, tabIndex = 1) {
    const entry = this.getTabEntry(accountId, tabIndex);
    if (!Array.isArray(entry.points)) return [];
    entry.points.forEach((pt, idx) => {
      if (pt && typeof pt === 'object') pt.order = idx + 1;
    });
    return entry.points;
  }

  saveAccountPoints(accountId, points, tabIndex = 1) {
    const c = this.ensureContainer(accountId);
    const tIdx = this.normalizeTabIndex(tabIndex);
    const entry = this.getTabEntry(accountId, tIdx);
    const list = Array.isArray(points) ? points : [];
    entry.points = list.map((pt, idx) => this.normalizePoint(pt, c.accountId, idx + 1));
    this.persistAccount(c.accountId);
    return entry.points;
  }

  getAccountProfile(accountId, tabIndex = 1) {
    const c = this.ensureContainer(accountId);
    const tIdx = this.normalizeTabIndex(tabIndex);
    const entry = this.getTabEntry(accountId, tIdx);
    return {
      extensionName: 'AutoKlik-v3.2-Mandiri-Extension',
      accountId: c.accountId,
      tabIndex: tIdx,
      partition: c.partition,
      label: c.label,
      storeFile: `akun_${c.accountId}.json`,
      storePath: this.getAccountStorePath(c.accountId),
      extensionSettings: { ...entry.extensionSettings },
      points: JSON.parse(JSON.stringify(entry.points))
    };
  }

  addAccountPoint(accountId, payload, tabIndex = 1) {
    const tIdx = this.normalizeTabIndex(tabIndex);
    const current = [...this.getAccountPoints(accountId, tIdx)];
    const order = current.length + 1;
    const seed = payload && typeof payload === 'object' ? { ...payload } : {};
    if (!seed.label) seed.label = `Titik ${order}`;
    if (seed.delayAfterMs === undefined || seed.delayAfterMs === null) seed.delayAfterMs = 500;
    if (seed.popupDelayMs === undefined || seed.popupDelayMs === null) seed.popupDelayMs = 300;
    if (seed.typingSpeedMs === undefined || seed.typingSpeedMs === null) seed.typingSpeedMs = 30;
    const nextPoint = this.normalizePoint(seed, accountId, order);
    current.push(nextPoint);
    const saved = this.saveAccountPoints(accountId, current, tIdx);
    return { ok: true, point: saved[saved.length - 1], points: saved };
  }

  updateAccountPoint(accountId, pointId, patch, tabIndex = 1) {
    const tIdx = this.normalizeTabIndex(tabIndex);
    const current = [...this.getAccountPoints(accountId, tIdx)];
    const idx = current.findIndex((p) => p.id === pointId);
    if (idx === -1) return { ok: false, reason: 'Titik tidak ditemukan' };
    current[idx] = this.normalizePoint({ ...current[idx], ...(patch || {}), id: pointId }, accountId, idx + 1);
    const saved = this.saveAccountPoints(accountId, current, tIdx);
    return { ok: true, points: saved };
  }

  removeAccountPoint(accountId, pointId, tabIndex = 1) {
    const tIdx = this.normalizeTabIndex(tabIndex);
    const filtered = this.getAccountPoints(accountId, tIdx).filter((p) => p.id !== pointId);
    const saved = this.saveAccountPoints(accountId, filtered, tIdx);
    return { ok: true, points: saved };
  }

  removeLastAccountPoint(accountId, tabIndex = 1) {
    const tIdx = this.normalizeTabIndex(tabIndex);
    const current = [...this.getAccountPoints(accountId, tIdx)];
    if (!current.length) {
      return { ok: true, points: [], removed: null };
    }
    const removed = current.pop();
    const saved = this.saveAccountPoints(accountId, current, tIdx);
    return { ok: true, points: saved, removed };
  }

  clearAccountPoints(accountId, tabIndex = 1) {
    const tIdx = this.normalizeTabIndex(tabIndex);
    const saved = this.saveAccountPoints(accountId, [], tIdx);
    return { ok: true, points: saved };
  }

  moveAccountPoint(accountId, pointId, dir, tabIndex = 1) {
    const tIdx = this.normalizeTabIndex(tabIndex);
    const current = [...this.getAccountPoints(accountId, tIdx)];
    const idx = current.findIndex((p) => p.id === pointId);
    if (idx === -1) return { ok: false, reason: 'Titik tidak ditemukan' };
    const targetIdx = dir === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= current.length) return { ok: true, points: current };
    const tmp = current[idx];
    current[idx] = current[targetIdx];
    current[targetIdx] = tmp;
    const saved = this.saveAccountPoints(accountId, current, tIdx);
    return { ok: true, points: saved };
  }

  exportAccountProfileJson(accountId, tabIndex = 1) {
    const profile = this.getAccountProfile(accountId, tabIndex);
    return JSON.stringify({
      schema: 'AutoKlikPro',
      version: '3.2',
      releaseTag: 'V5.8',
      extensionName: 'AutoKlik-v3.2-Mandiri-Extension',
      accountId: profile.accountId,
      tabIndex: profile.tabIndex,
      partition: profile.partition,
      label: profile.label,
      extensionSettings: profile.extensionSettings,
      points: profile.points
    }, null, 2);
  }

  importAccountProfileJson(accountId, rawJson, tabIndex = 1) {
    try {
      const tIdx = this.normalizeTabIndex(tabIndex);
      const parsed = typeof rawJson === 'string' ? JSON.parse(rawJson) : rawJson;
      if (!parsed || (typeof parsed !== 'object' && !Array.isArray(parsed))) {
        return { ok: false, reason: 'Format JSON tidak valid' };
      }
      const key = `persist:akun_${accountId}`;
      let sourceObj = parsed;
      if (!Array.isArray(parsed) && parsed.containers && typeof parsed.containers === 'object') {
        sourceObj = parsed.containers[key] || Object.values(parsed.containers)[0] || parsed;
      }
      if (!Array.isArray(sourceObj) && sourceObj.tabs && sourceObj.tabs[String(tIdx)]) {
        sourceObj = sourceObj.tabs[String(tIdx)];
      }
      const rawPoints = Array.isArray(sourceObj) ? sourceObj : (Array.isArray(sourceObj.points) ? sourceObj.points : null);
      if (!rawPoints) {
        return { ok: false, reason: 'JSON tidak memiliki daftar points' };
      }
      if (!Array.isArray(sourceObj) && sourceObj.extensionSettings) {
        this.saveAccountSettings(accountId, sourceObj.extensionSettings, tIdx);
      }
      const saved = this.saveAccountPoints(accountId, rawPoints.map((pt, idx) => ({
        ...pt,
        id: `pt_${accountId}_t${tIdx}_${idx + 1}`
      })), tIdx);
      return { ok: true, count: saved.length, tabIndex: tIdx, profile: this.getAccountProfile(accountId, tIdx) };
    } catch (err) {
      return { ok: false, reason: `Gagal parse JSON: ${err.message}` };
    }
  }

  copyFromAccount(accountId, tabIndex = 1) {
    const id = Number(accountId) || 1;
    const tIdx = this.normalizeTabIndex(tabIndex);
    const pts = this.getAccountPoints(id, tIdx);
    const settings = this.getAccountSettings(id, tIdx);
    this.clipboardPoints = JSON.parse(JSON.stringify(pts));
    this.clipboardSettings = { ...settings };
    this.clipboardMeta = {
      sourceAccountId: id,
      sourceTabIndex: tIdx,
      count: this.clipboardPoints.length,
      copiedAt: Date.now()
    };
    return this.clipboardPoints.length;
  }

  copyTabConfig(sourceAccountId, sourceTabIndex = 1) {
    const id = Number(sourceAccountId) || 1;
    const sIdx = this.normalizeTabIndex(sourceTabIndex);
    const count = this.copyFromAccount(id, sIdx);
    return {
      ok: true,
      count,
      sourceAccountId: id,
      sourceTabIndex: sIdx,
      json: this.exportAccountProfileJson(id, sIdx),
      clipboard: this.getClipboardSummary()
    };
  }

  getClipboardSummary() {
    const count = Array.isArray(this.clipboardPoints) ? this.clipboardPoints.length : 0;
    return {
      hasClipboard: count > 0,
      count,
      meta: this.clipboardMeta || null
    };
  }

  pasteToAccount(targetAccountId, tabIndex = 1) {
    if (!this.clipboardPoints) return { ok: false, reason: 'Clipboard titik masih kosong. Klik Copy terlebih dahulu.' };
    const id = Number(targetAccountId) || 1;
    const tIdx = this.normalizeTabIndex(tabIndex);
    const cloned = this.clipboardPoints.map((pt, idx) => ({
      ...pt,
      id: `pt_${id}_t${tIdx}_${idx + 1}`,
      order: idx + 1
    }));
    this.saveAccountPoints(id, cloned, tIdx);
    if (this.clipboardSettings) {
      this.saveAccountSettings(id, this.clipboardSettings, tIdx);
    }
    return {
      ok: true,
      count: cloned.length,
      targetAccountId: id,
      targetTabIndex: tIdx,
      profile: this.getAccountProfile(id, tIdx)
    };
  }

  pasteTabConfig(targetAccountId, targetTabIndex = 1, sourceAccountId = null, sourceTabIndex = null) {
    if (sourceAccountId !== null && sourceAccountId !== undefined) {
      this.copyFromAccount(Number(sourceAccountId) || 1, this.normalizeTabIndex(sourceTabIndex || 1));
    }
    return this.pasteToAccount(Number(targetAccountId) || 1, this.normalizeTabIndex(targetTabIndex || 1));
  }

  pasteToAllTabsInAccount(targetAccountId, sourceAccountId = null, sourceTabIndex = null) {
    const id = Number(targetAccountId) || 1;
    if (sourceAccountId !== null && sourceAccountId !== undefined) {
      this.copyFromAccount(Number(sourceAccountId) || 1, this.normalizeTabIndex(sourceTabIndex || 1));
    }
    if (!this.clipboardPoints) {
      return { ok: false, reason: 'Clipboard titik masih kosong. Pilih Tab Sumber lalu klik Copy terlebih dahulu.' };
    }
    for (const tIdx of [1, 2, 3]) {
      this.pasteToAccount(id, tIdx);
    }
    return {
      ok: true,
      count: this.clipboardPoints.length,
      targetAccountId: id
    };
  }

  pasteToAllAccounts(sourceAccountId, accountIds = null, sourceTabIndex = 1) {
    const sIdx = this.normalizeTabIndex(sourceTabIndex);
    if (sourceAccountId) {
      this.copyFromAccount(sourceAccountId, sIdx);
    }
    if (!this.clipboardPoints) return { ok: false, reason: 'Tidak ada titik untuk di-paste' };
    const targetIds = new Set(Array.isArray(accountIds) && accountIds.length > 0 ? accountIds : [1, 2, 3, 4, 5]);
    for (const id of this.containersMap.keys()) {
      targetIds.add(Number(id));
    }
    for (const id of targetIds) {
      this.pasteToAccount(id, sIdx);
    }
    return { ok: true, count: this.clipboardPoints.length };
  }
}

module.exports = { PointProfileStore, DEFAULT_EXTENSION_SETTINGS };
