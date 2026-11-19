package com.example.domain

object ElectronFase2Templates {

  fun assetBlockerJs(): String =
    """
const CAPTCHA_ALLOW_RE = /(captcha|recaptcha|hcaptcha|turnstile|arkose|funcaptcha|challenge|verify|otp)/i;
const TRACKER_BLOCK_RE = /(google-analytics\.com|googletagmanager\.com|doubleclick\.net|facebook\.net|connect\.facebook|hotjar\.com|clarity\.ms|sentry\.io)/i;

class AssetBlocker {
  constructor(runtimeConfig) {
    this.mode = runtimeConfig.assetBlockerMode || 'selective';
    this.blockedCounts = new Map();
  }

  setMode(nextMode) {
    if (['selective', 'aggressive', 'disabled'].includes(nextMode)) {
      this.mode = nextMode;
    }
    return this.mode;
  }

  attachToSession(ses, accountId) {
    if (!this.blockedCounts.has(accountId)) {
      this.blockedCounts.set(accountId, 0);
    }
    ses.webRequest.onBeforeRequest({ urls: ['*://*/*'] }, (details, callback) => {
      if (this.mode === 'disabled') {
        callback({ cancel: false });
        return;
      }

      const url = details.url || '';
      const rType = details.resourceType || '';

      if (
        rType === 'mainFrame' ||
        rType === 'subFrame' ||
        rType === 'xhr' ||
        rType === 'fetch' ||
        rType === 'script' ||
        rType === 'stylesheet' ||
        rType === 'websocket' ||
        url.includes('trip.ferizy.com') ||
        CAPTCHA_ALLOW_RE.test(url)
      ) {
        callback({ cancel: false });
        return;
      }

      if (TRACKER_BLOCK_RE.test(url)) {
        this.incrementBlocked(accountId);
        callback({ cancel: true });
        return;
      }

      if (this.mode === 'selective') {
        if (rType === 'media' || rType === 'font') {
          this.incrementBlocked(accountId);
          callback({ cancel: true });
          return;
        }
        if (rType === 'image' && !/ferizy\.com/i.test(url) && !url.startsWith('data:')) {
          this.incrementBlocked(accountId);
          callback({ cancel: true });
          return;
        }
        callback({ cancel: false });
        return;
      }

      if (rType === 'media' || rType === 'font' || rType === 'image') {
        this.incrementBlocked(accountId);
        callback({ cancel: true });
        return;
      }

      callback({ cancel: false });
    });
  }

  incrementBlocked(accountId) {
    this.blockedCounts.set(accountId, (this.blockedCounts.get(accountId) || 0) + 1);
  }

  getBlockedCount(accountId) {
    return this.blockedCounts.get(accountId) || 0;
  }
}

module.exports = { AssetBlocker };
    """.trimIndent() + "\n"

  fun pointProfileStoreJs(): String =
    """
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
    return path.join(this.containersDir, `akun_${'$'}{id}.json`);
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
      console.error(`[PointProfileStore] Gagal simpan profil Akun ${'$'}{id}:`, err.message);
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
      id: String(raw.id || `pt_${'$'}{accountId}_${'$'}{Date.now().toString(36)}_${'$'}{order}`),
      order,
      type,
      label: String(raw.label || `Titik ${'$'}{order}`).trim() || `Titik ${'$'}{order}`,
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

    const key = `persist:akun_${'$'}{id}`;
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
          const migratedPt = { ...pt, id: pt.id || `pt_${'$'}{id}_t${'$'}{tIdx}_${'$'}{idx + 1}` };
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
      label: (seed && seed.label) || `Akun ${'$'}{id}`,
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
      storeFile: `akun_${'$'}{c.accountId}.json`,
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
    if (!seed.label) seed.label = `Titik ${'$'}{order}`;
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
      const key = `persist:akun_${'$'}{accountId}`;
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
        id: `pt_${'$'}{accountId}_t${'$'}{tIdx}_${'$'}{idx + 1}`
      })), tIdx);
      return { ok: true, count: saved.length, tabIndex: tIdx, profile: this.getAccountProfile(accountId, tIdx) };
    } catch (err) {
      return { ok: false, reason: `Gagal parse JSON: ${'$'}{err.message}` };
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
      id: `pt_${'$'}{id}_t${'$'}{tIdx}_${'$'}{idx + 1}`,
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
    """.trimIndent() + "\n"

  fun automationEngineJs(): String =
    """
const { ipcMain } = require('electron');

const WIN_URL_RE = /\/(order\/verification|payment|invoice)/i;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

class AutomationEngine {
  constructor(runtimeConfig, sessionManager, pointStore, onStateChange, eventBus, serverClockSync, notificationCenter) {
    this.runtimeConfig = runtimeConfig;
    this.sessionManager = sessionManager;
    this.pointStore = pointStore;
    this.onStateChange = onStateChange;
    this.eventBus = eventBus;
    this.serverClockSync = serverClockSync;
    this.notificationCenter = notificationCenter;
    this.optionalModules = null;
    this.runners = new Map();
    this.networkLastCompleted = new Map();
    this.lastQuotaSnapshot = new Map();
    this.registerIpc();
  }

  attachOptionalModules(optionalModules) {
    this.optionalModules = optionalModules;
  }

  registerIpc() {
    ipcMain.on('automation:dom-trigger', (event, payload) => {
      const tab = this.findTabByWebContentsId(event.sender.id);
      if (!tab) return;
      const runner = this.ensureRunner(tab.tabId);
      runner.lastDomTrigger = payload && payload.label ? payload.label : 'MutationTrigger';
    });

    ipcMain.on('automation:anti-revert-trigger', () => {
      // Dinonaktifkan agar fitur Input Ketik 100% murni mengetik menggunakan keyboard fisik tanpa penimpaan paste teks
    });

    ipcMain.on('automation:target-resolved', (event, payload) => {
      if (!payload || !payload.reqId) return;
      const resolver = this.pendingResolvers && this.pendingResolvers.get(payload.reqId);
      if (resolver) {
        this.pendingResolvers.delete(payload.reqId);
        resolver(payload);
      }
    });

    ipcMain.on('automation:captcha-detected', (event, payload) => {
      const tab = this.findTabByWebContentsId(event.sender.id);
      if (tab && this.optionalModules) {
        this.optionalModules.handleCaptchaDetected(tab, payload && payload.detail);
      }
    });

    // 0ms API Sniffer dari Preload + Notifikasi Konteks URL Spesifik
    ipcMain.on('sniffer:api-response', (event, packet) => {
      const tab = this.findTabByWebContentsId(event.sender.id);
      if (!tab || !packet) return;
      const runner = this.ensureRunner(tab.tabId);
      const isRunning = runner.state === 'RUNNING';
      const currentUrl = tab.url || '';

      // 1. Notif Lintasan Dibuka HANYA saat konfigurasi berjalan di https://trip.ferizy.com/
      if (packet.kind === 'lintasan' && packet.hasAvailable && isRunning) {
        this.eventBus.broadcast('sniffer:lintasan-open', {
          accountId: tab.accountId,
          tabId: tab.tabId,
          url: currentUrl
        });
        if (this.optionalModules && this.notificationCenter.isUrlMatchLintasan(currentUrl)) {
          this.optionalModules.triggerAutoWake(tab, 'LINTASAN_DIBUKA');
        }
        this.notificationCenter.dispatch('LINTASAN_DIBUKA', {
          accountId: tab.accountId,
          isRunning,
          url: currentUrl,
          dedupeHint: 'lintasan',
          message: `Lintasan terbuka terdeteksi di Akun ${'$'}{tab.accountId} Tab ${'$'}{tab.indexInAccount}`
        });
      }

      // 2. Notif Perubahan Kuota HANYA saat konfigurasi berjalan di https://trip.ferizy.com/ferryschedule
      if (packet.kind === 'schedule' && Array.isArray(packet.slots) && isRunning) {
        const changedSlots = [];
        for (const s of packet.slots) {
          const slotQuota = Number((s && (s.quota ?? s.available ?? s.sisa)) ?? 0);
          const slotTime = String((s && (s.departureTime || s.time || s.jam || s.etd)) || 'slot');
          const slotSvc = String((s && (s.serviceType || s.service || '')) || '');
          const key = `${'$'}{tab.accountId}_${'$'}{slotTime}_${'$'}{slotSvc}`;
          const prevQuota = this.lastQuotaSnapshot.get(key);
          this.lastQuotaSnapshot.set(key, slotQuota);
          if ((prevQuota === undefined && slotQuota > 0) || (prevQuota !== undefined && prevQuota !== slotQuota)) {
            changedSlots.push({
              slot: s,
              prevQuota: prevQuota === undefined ? 0 : prevQuota,
              nextQuota: slotQuota,
              departureTime: slotTime
            });
          }
        }

        const matched = this.optionalModules
          ? this.optionalModules.matchBestScheduleSlot(packet.slots)
          : packet.slots.find((s) => Number(s.quota || s.available || s.sisa || 0) > 0);

        if (matched) {
          this.eventBus.broadcast('sniffer:kuota-available', {
            accountId: tab.accountId,
            tabId: tab.tabId,
            slot: matched,
            url: currentUrl
          });
          if (this.optionalModules && this.notificationCenter.isUrlMatchKuota(currentUrl)) {
            this.optionalModules.triggerAutoWake(tab, 'KUOTA_MUNCUL');
          }
        }

        if (changedSlots.length > 0) {
          const firstChange = changedSlots[0];
          const jamLabel = (matched && (matched.departureTime || matched.jam)) || firstChange.departureTime;
          this.notificationCenter.dispatch('KUOTA_MUNCUL', {
            accountId: tab.accountId,
            isRunning,
            url: currentUrl,
            dedupeHint: `${'$'}{jamLabel}_${'$'}{firstChange.nextQuota}`,
            message: `Perubahan kuota Akun ${'$'}{tab.accountId} Tab ${'$'}{tab.indexInAccount} (${'$'}{jamLabel}): ${'$'}{firstChange.prevQuota} -> ${'$'}{firstChange.nextQuota}`
          });
        }
      }
    });
  }

  attachNetworkSync(ses, accountId) {
    ses.webRequest.onCompleted({ urls: ['https://trip.ferizy.com/*'] }, (details) => {
      if (details.statusCode === 401 && details.resourceType === 'mainFrame') {
        if (this.optionalModules) {
          this.optionalModules.handleLogoutDetected(accountId, null, 'HTTP 401');
        }
        return;
      }
      if (details.statusCode >= 200 && details.statusCode < 400) {
        this.networkLastCompleted.set(accountId, Date.now());
      }
    });
  }

  attachWinDetector(tabMeta) {
    this.ensureRunner(tabMeta.tabId);
    const wc = tabMeta.view.webContents;
    const checkUrl = (url) => {
      const runner = this.ensureRunner(tabMeta.tabId);
      if (typeof url === 'string' && WIN_URL_RE.test(url)) {
        if (runner.ignoredWinUrl && runner.ignoredWinUrl === url) {
          return;
        }
        this.handleTabWon(tabMeta.tabId, url);
      } else if (typeof url === 'string' && /\/login(\?|$)/i.test(url)) {
        if (runner.wasAuthenticated && runner.state !== 'RUNNING') {
          runner.wasAuthenticated = false;
          if (this.optionalModules) {
            this.optionalModules.handleLogoutDetected(tabMeta.accountId, tabMeta.tabId, 'Redirect ke /login');
          }
        }
      } else if (typeof url === 'string' && url.startsWith('https://trip.ferizy.com/') && !url.includes('/login')) {
        runner.wasAuthenticated = true;
      }
    };
    wc.on('did-navigate', (_e, url) => checkUrl(url));
    wc.on('did-navigate-in-page', (_e, url) => checkUrl(url));
    wc.on('did-redirect-navigation', (_e, url) => checkUrl(url));
  }

  ensureRunner(tabId) {
    if (!this.runners.has(tabId)) {
      this.runners.set(tabId, {
        state: 'IDLE',
        wonUrl: null,
        lastDomTrigger: null,
        wasAuthenticated: false,
        lastCursorX: null,
        lastCursorY: null,
        activePointId: null,
        runToken: 0
      });
    }
    return this.runners.get(tabId);
  }

  getRunnerState(tabId) {
    return this.ensureRunner(tabId);
  }

  findTabByWebContentsId(wcId) {
    for (const tab of this.sessionManager.tabs.values()) {
      if (!tab.view.webContents.isDestroyed() && tab.view.webContents.id === wcId) {
        return tab;
      }
    }
    return null;
  }

  notifyRunnerOverlayState(tab, running, activePointId = null) {
    try {
      if (tab && tab.view && !tab.view.webContents.isDestroyed()) {
        tab.view.webContents.send('autoklik-ext:runner-active', {
          running: Boolean(running),
          activePointId: running ? activePointId : null
        });
      }
    } catch (_) {}
  }

  notifyActivePoint(tab, pointId, order) {
    try {
      if (tab && tab.view && !tab.view.webContents.isDestroyed()) {
        tab.view.webContents.send('autoklik-ext:active-point', {
          pointId: pointId || null,
          order: order || null
        });
      }
    } catch (_) {}
  }

  handleTabWon(tabId, url) {
    const runner = this.ensureRunner(tabId);
    if (runner.state === 'WON_STOPPED') return;
    runner.state = 'WON_STOPPED';
    runner.wonUrl = url;
    runner.runToken += 1;

    const tab = this.sessionManager.tabs.get(tabId);
    this.notifyRunnerOverlayState(tab, false);
    const stopMode = this.optionalModules ? this.optionalModules.flags.advancedStopMode : 'per_tab';
    if (tab && stopMode === 'per_account') {
      for (const peer of this.sessionManager.getAccountTabs(tab.accountId)) {
        if (peer.tabId !== tabId) this.stopTab(peer.tabId);
      }
    } else if (stopMode === 'all_accounts') {
      for (const peer of this.sessionManager.tabs.values()) {
        if (peer.tabId !== tabId) this.stopTab(peer.tabId);
      }
    }

    this.eventBus.broadcast('tab:won', { tabId, accountId: tab ? tab.accountId : null, url });
    this.notificationCenter.dispatch('TEMBUS_VERIFIKASI', {
      accountId: tab ? tab.accountId : 0,
      dedupeHint: tabId,
      message: `Order Sukses / Tembus! Tab ${'$'}{tabId} -> ${'$'}{url} (Auto-Stop 0ms ${'$'}{stopMode}).`
    });
    this.onStateChange();
  }

  pauseForCaptcha(tabId) {
    const runner = this.ensureRunner(tabId);
    if (runner.state === 'WON_STOPPED') return;
    runner.state = 'PAUSED_CAPTCHA';
    runner.runToken += 1;
    const tab = this.sessionManager.tabs.get(tabId);
    this.notifyRunnerOverlayState(tab, false);
    this.onStateChange();
  }

  markLogoutStopped(tabId) {
    const runner = this.ensureRunner(tabId);
    if (runner.state === 'WON_STOPPED' || runner.state === 'RUNNING') return;
    runner.state = 'LOGOUT_STOPPED';
    runner.runToken += 1;
    const tab = this.sessionManager.tabs.get(tabId);
    this.notifyRunnerOverlayState(tab, false);
    this.onStateChange();
  }

  stopTab(tabId) {
    const runner = this.ensureRunner(tabId);
    if (runner.state === 'WON_STOPPED') return { ok: true, state: runner.state };
    runner.state = 'IDLE';
    runner.runToken += 1;
    const tab = this.sessionManager.tabs.get(tabId);
    this.notifyRunnerOverlayState(tab, false);
    this.onStateChange();
    return { ok: true, state: runner.state };
  }

  resetWonTab(tabId) {
    const runner = this.ensureRunner(tabId);
    runner.state = 'IDLE';
    runner.wonUrl = null;
    runner.ignoredWinUrl = null;
    runner.runToken += 1;
    const tab = this.sessionManager.tabs.get(tabId);
    this.notifyRunnerOverlayState(tab, false);
    this.onStateChange();
    return { ok: true };
  }

  async startTab(tabId, options = {}) {
    const tab = this.sessionManager.tabs.get(tabId);
    if (!tab) return { ok: false, reason: 'Tab tidak ditemukan' };

    const runner = this.ensureRunner(tabId);
    if (!options.force && runner.state === 'WON_STOPPED') {
      return { ok: false, reason: 'Tab sudah MENANG (Auto-Stop 0ms aktif)' };
    }

    const tabIdx = Number(tab.indexInAccount) || 1;
    const extSettings = this.pointStore.getAccountSettings(tab.accountId, tabIdx);
    if (extSettings && extSettings.enabled === false) {
      return { ok: false, reason: `AutoKlik-v3.2-Mandiri-Extension nonaktif untuk Akun ${'$'}{tab.accountId} Tab ${'$'}{tabIdx}` };
    }

    const points = this.pointStore.getAccountPoints(tab.accountId, tabIdx).filter((p) => p.enabled !== false);
    if (!points.length) {
      return { ok: false, reason: `Profil titik Akun ${'$'}{tab.accountId} Tab ${'$'}{tabIdx} masih kosong. Klik +Titik atau Load konfigurasi terlebih dahulu.` };
    }

    const initialUrl = (!tab.view.webContents.isDestroyed() && tab.view.webContents.getURL()) || '';
    runner.ignoredWinUrl = WIN_URL_RE.test(initialUrl) ? initialUrl : null;
    runner.wonUrl = null;
    runner.state = 'RUNNING';
    runner.runToken += 1;
    const myToken = runner.runToken;
    // V5.8 Mandatory Rule: Otomatis putus Mirror untuk tab yang sedang Start AutoKlik
    if (this.sessionManager && typeof this.sessionManager.disconnectMirrorForAutoKlikTab === 'function') {
      this.sessionManager.disconnectMirrorForAutoKlikTab(tabId);
    }
    this.notifyRunnerOverlayState(tab, true);
    this.onStateChange();

    this.executeLoop(tab, myToken).catch((err) => {
      console.error(`[Automation] Error di ${'$'}{tabId}:`, err.message);
    });
    return { ok: true, state: 'RUNNING' };
  }

  toDipPoint(wc, x, y) {
    let zf = 1;
    try {
      if (wc && typeof wc.getZoomFactor === 'function') {
        const raw = wc.getZoomFactor();
        if (Number.isFinite(raw) && raw > 0.1) zf = raw;
      }
    } catch (_) {}
    // Pin Ikon di halaman dirender dalam koordinat CSS viewport (clientX, clientY).
    // Electron wc.sendInputEvent memerlukan koordinat DIP (x * zoomFactor, y * zoomFactor)
    // agar titik klik hardware Chromium jatuh 100% persis tepat di tengah-tengah Pin Ikon pada level Zoom berapa pun.
    return {
      x: Math.max(0, Math.round((Number(x) || 0) * zf)),
      y: Math.max(0, Math.round((Number(y) || 0) * zf)),
      cssX: Math.max(0, Math.round(Number(x) || 0)),
      cssY: Math.max(0, Math.round(Number(y) || 0)),
      zoomFactor: zf
    };
  }

  async executeLoop(tab, myToken) {
    const runner = this.ensureRunner(tab.tabId);
    const wc = tab.view.webContents;
    let cycle = 0;

    // Pastikan pin overlay langsung masuk mode pass-through (pointer-events: none) & anti-select aktif sebelum klik/swipe pertama
    this.notifyRunnerOverlayState(tab, true);
    await sleep(22);

    while (runner.runToken === myToken && runner.state === 'RUNNING' && !wc.isDestroyed()) {
      const tabIdx = Number(tab.indexInAccount) || 1;
      const extSettings = this.pointStore.getAccountSettings(tab.accountId, tabIdx);
      if (extSettings && extSettings.enabled === false) {
        break;
      }

      // Pastikan eksekusi SELALU berurutan sesuai No Urut Titik (1, 2, 3, ...) dan hanya titik valid milik user
      const activePoints = this.pointStore
        .getAccountPoints(tab.accountId, tabIdx)
        .slice()
        .sort((a, b) => (Number(a.order) || 0) - (Number(b.order) || 0))
        .filter((p) => p && p.enabled !== false && Number.isFinite(Number(p.x)) && Number.isFinite(Number(p.y)));
      if (!activePoints.length) break;

      const latencyComp =
        this.optionalModules && this.serverClockSync
          ? this.serverClockSync.getLatencyCompensationMs(this.optionalModules.flags.latencyCompensationEnabled)
          : 0;

      // Siklus Default V5.8: Selalu mulai dari Titik #1 (mainLoopStartOrder = 1, subLoopEnabled = false)
      // dan urut ketat #1 -> #2 -> #3 -> ... -> Titik Terakhir -> kembali ke Titik #1 tanpa acak.
      let idx = 0;

      while (idx < activePoints.length) {
        if (runner.runToken !== myToken || runner.state !== 'RUNNING' || wc.isDestroyed()) {
          return;
        }
        const curUrl = wc.getURL() || '';
        if (WIN_URL_RE.test(curUrl) && curUrl !== runner.ignoredWinUrl) {
          this.handleTabWon(tab.tabId, curUrl);
          return;
        }

        const pt = activePoints[idx];
        const target = await this.resolveTarget(wc, pt, extSettings);
        runner.activePointId = pt.id;
        this.notifyActivePoint(tab, pt.id, idx + 1);

        const moveDurationMs = extSettings && Number.isFinite(Number(extSettings.cursorMoveMs))
          ? Math.max(0, Number(extSettings.cursorMoveMs))
          : 150;
        const hoverSettleMs = extSettings && Number.isFinite(Number(extSettings.hoverSettleMs))
          ? Math.max(0, Number(extSettings.hoverSettleMs))
          : 50;

        if (pt.type === 'tap') {
          await this.sendTap(wc, target.x, target.y, true, runner, moveDurationMs, hoverSettleMs, {
            pointId: pt.id,
            isPopupPin: false,
            iconDiameter: target.iconDiameter,
            iconRadius: target.iconRadius
          });
        } else if (pt.type === 'swipe_up' || pt.type === 'swipe_down' || pt.type === 'swipe') {
          const lineLen = Math.max(40, Number(pt.lineLengthPx) || Math.abs((pt.endY ?? (target.y - 350)) - target.y) || 350);
          const scale = Math.max(1, Number(pt.swipeForceScale) || 2);
          const totalDist = Math.round(lineLen * scale);
          const isDownGesture = pt.type === 'swipe_down' || (pt.type === 'swipe' && Number(pt.endY) > Number(pt.y));
          // Gerakkan kursor secara halus ke titik awal swipe lalu gulir layar dengan kurva ease-in-out
          await this.moveCursorSmoothly(wc, runner, target.x, target.y, Math.min(130, moveDurationMs));
          await this.sendSmoothWheelSwipe(wc, target.x, target.y, isDownGesture ? 'down' : 'up', totalDist, pt.durationMs || 340);
        } else if (pt.type === 'input') {
          const cycleTexts = Array.isArray(pt.cycleTexts) && pt.cycleTexts.length > 0
            ? pt.cycleTexts
            : [pt.inputValue || pt.text || ''];
          const rawText = String(cycleTexts[cycle % cycleTexts.length] || '');
          // Default kecepatan ketik manusia = 30ms/huruf
          const typingSpeed = Number.isFinite(Number(pt.typingSpeedMs)) && Number(pt.typingSpeedMs) > 0
            ? Number(pt.typingSpeedMs)
            : 30;
          const popupWaitMs = Math.min(10000, Math.max(20, Number(pt.popupDelayMs) || 300));
          const hasPopupPin = Boolean(pt.popupPinEnabled) && Number.isFinite(Number(pt.popupPinX)) && Number.isFinite(Number(pt.popupPinY));

          if (hasPopupPin) {
            // Alur Input Ketik + Popup Key (100% Murni Keyboard & Presisi Bulat Penuh Ukuran Icon Titik):
            await this.sendTap(wc, target.x, target.y, true, runner, moveDurationMs, hoverSettleMs, {
              pointId: pt.id,
              isPopupPin: false,
              iconDiameter: target.iconDiameter,
              iconRadius: target.iconRadius
            });
            await sleep(30);
            await this.clearFocusedInput(wc, target.x, target.y);
            const triggerChar = rawText.trim().charAt(0) || rawText.charAt(0);
            if (triggerChar.length > 0) {
              await this.typeTextChars(wc, triggerChar, typingSpeed, { x: target.x, y: target.y, isTrigger: true });
            }
            await sleep(popupWaitMs);
            if (runner.runToken !== myToken || runner.state !== 'RUNNING' || wc.isDestroyed()) return;
            const popupTarget = await this.resolveTarget(wc, pt, extSettings, true);
            await this.sendTap(wc, popupTarget.x, popupTarget.y, true, runner, Math.min(120, moveDurationMs), hoverSettleMs, {
              pointId: pt.id,
              isPopupPin: true,
              iconDiameter: popupTarget.iconDiameter,
              iconRadius: popupTarget.iconRadius
            });
            await sleep(60);
            if (runner.runToken !== myToken || runner.state !== 'RUNNING' || wc.isDestroyed()) return;
            const reTarget = await this.resolveTarget(wc, pt, extSettings, false);
            await this.sendTap(wc, reTarget.x, reTarget.y, true, runner, Math.min(110, moveDurationMs), 35, {
              pointId: pt.id,
              isPopupPin: false,
              iconDiameter: reTarget.iconDiameter,
              iconRadius: reTarget.iconRadius
            });
            await sleep(28);
            await this.clearFocusedInput(wc, reTarget.x, reTarget.y);
            if (rawText.length > 0) {
              await this.typeTextChars(wc, rawText, typingSpeed, { x: reTarget.x, y: reTarget.y, isFinal: true });
            }
          } else {
            // Alur Input Ketik standar (100% Murni Keyboard tanpa Popup Key):
            if (pt.focusBeforeType !== false) {
              await this.sendTap(wc, target.x, target.y, true, runner, moveDurationMs, hoverSettleMs, {
                pointId: pt.id,
                isPopupPin: false,
                iconDiameter: target.iconDiameter,
                iconRadius: target.iconRadius
              });
              await sleep(30);
            }
            if (pt.clearBeforeType !== false) {
              await this.clearFocusedInput(wc, target.x, target.y);
            }
            if (rawText.length > 0) {
              await this.typeTextChars(wc, rawText, typingSpeed, { x: target.x, y: target.y, isFinal: true });
            }
          }
        } else if (pt.type === 'popup_key') {
          await this.sendPopupKey(wc, pt.popupKey || pt.key || 'Enter');
        }

        // Waktu jeda antar titik presisi deterministik (default 500ms) tanpa drift acak
        const rawWait = Math.max(15, Number(pt.delayAfterMs) || 500);
        const humanizedWait = this.computeHumanizedDelay(rawWait, !extSettings || extSettings.humanRhythm !== false);
        const compensatedWait = Math.max(15, humanizedWait - latencyComp);
        await sleep(compensatedWait);

        idx += 1;
      }

      cycle += 1;
      const liveExt = this.pointStore.getAccountSettings(tab.accountId, Number(tab.indexInAccount) || 1);
      const loopEnabled = !liveExt || liveExt.loopEnabled !== false;
      const maxLoops = loopEnabled ? Math.max(0, Number(liveExt && liveExt.maxLoops) || 0) : 1;
      if (!loopEnabled || (maxLoops > 0 && cycle >= maxLoops)) {
        break;
      }
      const baseLoopDelay = Math.max(20, Number(liveExt && liveExt.loopDelayMs) || 100);
      await sleep(this.computeHumanizedDelay(baseLoopDelay, !liveExt || liveExt.humanRhythm !== false));
    }

    if (runner.runToken === myToken && runner.state === 'RUNNING') {
      runner.state = 'IDLE';
      runner.activePointId = null;
      this.notifyActivePoint(tab, null, null);
      this.notifyRunnerOverlayState(tab, false);
      this.onStateChange();
    }
  }

  computeHumanizedDelay(baseDelayMs, _humanRhythm = true) {
    return Math.max(15, Math.round(Number(baseDelayMs) || 500));
  }

  async resolveTarget(wc, point, _extSettings = null, isPopupPin = false) {
    const fallbackX = Math.round(Number(isPopupPin ? (point && point.popupPinX) : (point && point.x)) || 0);
    const fallbackY = Math.round(Number(isPopupPin ? (point && point.popupPinY) : (point && point.y)) || 0);
    const pct = Math.min(100, Math.max(15, Number(point && point.iconSizePct) || 25));
    const fallbackDiameter = isPopupPin ? 28 : Math.round(20 + pct * 0.6);
    const fallbackRadius = Math.max(10, Math.round(fallbackDiameter / 2));
    if (!wc || wc.isDestroyed() || !point || !point.id) {
      return { x: fallbackX, y: fallbackY, iconDiameter: fallbackDiameter, iconRadius: fallbackRadius };
    }
    const pid = String(point.id);
    const popupFlag = Boolean(isPopupPin);
    try {
      // Baca posisi visual & ukuran bulat penuh (full-circle diameter & radius) dari Pin Ikon yang tampil di layar.
      // Jika titik tengah tepat mengenai elemen interaktif, gunakan titik tengah; jika elemen interaktif berada di dalam
      // cakupan lingkaran bulat penuh Pin Ikon (dist <= iconRadius), arahkan sentuhan ke dalam lingkaran Pin Ikon tersebut!
      const liveCenter = await wc.executeJavaScript(`
        (() => {
          try {
            const host = document.getElementById('__autoklik_v32_host__');
            let cx = ${'$'}{fallbackX};
            let cy = ${'$'}{fallbackY};
            let diam = ${'$'}{fallbackDiameter};
            if (host && host.shadowRoot) {
              const pid = ${'$'}{JSON.stringify(pid)};
              const isPopup = ${'$'}{popupFlag ? 'true' : 'false'};
              const pins = host.shadowRoot.querySelectorAll('.ak-pin');
              let targetPin = null;
              for (const p of pins) {
                if (!p.dataset) continue;
                if (isPopup && p.dataset.popupPinFor === pid) {
                  targetPin = p;
                  break;
                }
                if (!isPopup && p.dataset.pointId === pid) {
                  targetPin = p;
                  break;
                }
              }
              if (targetPin) {
                const cross = targetPin.querySelector('.ak-crosshair') || targetPin;
                const r = cross.getBoundingClientRect();
                if (r && (r.width > 0 || r.height > 0)) {
                  cx = Math.round(r.left + r.width / 2);
                  cy = Math.round(r.top + r.height / 2);
                  diam = Math.max(18, Math.round(r.width || diam));
                }
              }
            }
            const rad = Math.max(9, Math.round(diam / 2));
            // Isolasi Penuh Antar Titik Berdekatan (V5.8):
            // Jangan menggeser titik ke elemen tetangga agar titik yang ditempatkan berdekatan tidak saling menarik/mempengaruhi.
            return { x: cx, y: cy, iconDiameter: diam, iconRadius: rad };
          } catch (_) {
            return null;
          }
        })()
      `, true);
      if (liveCenter && Number.isFinite(Number(liveCenter.x)) && Number.isFinite(Number(liveCenter.y)) && (liveCenter.x > 0 || liveCenter.y > 0)) {
        return {
          x: Math.max(0, Math.round(Number(liveCenter.x))),
          y: Math.max(0, Math.round(Number(liveCenter.y))),
          iconDiameter: Math.max(18, Math.round(Number(liveCenter.iconDiameter) || fallbackDiameter)),
          iconRadius: Math.max(9, Math.round(Number(liveCenter.iconRadius) || fallbackRadius))
        };
      }
    } catch (_) {}
    return { x: fallbackX, y: fallbackY, iconDiameter: fallbackDiameter, iconRadius: fallbackRadius };
  }

  async moveCursorSmoothly(wc, runner, targetCssX, targetCssY, durationMs = 150) {
    if (!wc || wc.isDestroyed()) return;
    const endX = Math.max(0, Math.round(Number(targetCssX) || 0));
    const endY = Math.max(0, Math.round(Number(targetCssY) || 0));
    const startX = runner && Number.isFinite(Number(runner.lastCursorX)) ? Number(runner.lastCursorX) : null;
    const startY = runner && Number.isFinite(Number(runner.lastCursorY)) ? Number(runner.lastCursorY) : null;

    if (startX === null || startY === null || durationMs <= 0) {
      const dip = this.toDipPoint(wc, endX, endY);
      wc.sendInputEvent({ type: 'mouseMove', x: dip.x, y: dip.y });
      if (runner) {
        runner.lastCursorX = endX;
        runner.lastCursorY = endY;
      }
      return;
    }

    const dist = Math.hypot(endX - startX, endY - startY);
    if (dist < 6) {
      const dip = this.toDipPoint(wc, endX, endY);
      wc.sendInputEvent({ type: 'mouseMove', x: dip.x, y: dip.y });
      if (runner) {
        runner.lastCursorX = endX;
        runner.lastCursorY = endY;
      }
      return;
    }

    // Hitung jumlah langkah proporsional dengan jarak (8 s/d 14 langkah halus dengan kurva Ease-In-Out + lengkungan tipis tangan manusia)
    const steps = Math.max(6, Math.min(14, Math.round(durationMs / 13)));
    const stepWait = Math.max(8, Math.round(durationMs / steps));
    const nx = -(endY - startY) / dist;
    const ny = (endX - startX) / dist;
    const arcAmp = Math.min(14, Math.max(2, dist * 0.04));

    for (let i = 1; i <= steps; i++) {
      if (wc.isDestroyed()) return;
      const t = i / steps;
      // Cubic Ease-In-Out: mulai lembut -> lancar di tengah -> melambat halus saat tiba di titik
      const eased = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
      const arc = Math.sin(Math.PI * t) * arcAmp;
      const curX = Math.max(0, Math.round(startX + (endX - startX) * eased + nx * arc));
      const curY = Math.max(0, Math.round(startY + (endY - startY) * eased + ny * arc));
      const dip = this.toDipPoint(wc, curX, curY);
      // Hanya mouseMove TANPA menekan tombol kiri mouse -> 0% risiko blok teks layar, memicu efek :hover alami
      wc.sendInputEvent({ type: 'mouseMove', x: dip.x, y: dip.y });
      await sleep(stepWait);
    }

    if (runner) {
      runner.lastCursorX = endX;
      runner.lastCursorY = endY;
    }
  }

  async sendTap(wc, cssX, cssY, triggerDomTap = true, runner = null, moveDurationMs = 150, hoverSettleMs = 50, meta = null) {
    if (wc.isDestroyed()) return;
    const cleanX = Math.max(0, Math.round(Number(cssX) || 0));
    const cleanY = Math.max(0, Math.round(Number(cssY) || 0));
    const { x, y } = this.toDipPoint(wc, cleanX, cleanY);

    // 1. Gerakkan kursor secara halus dari titik sebelumnya menuju titik Pin Ikon
    await this.moveCursorSmoothly(wc, runner, cleanX, cleanY, moveDurationMs);
    if (wc.isDestroyed()) return;

    // 2. Pastikan kursor terkunci presisi di koordinat DIP titik Pin Ikon
    wc.sendInputEvent({ type: 'mouseMove', x, y });

    // 3. Jeda Hover Singkat (Hover Settle ~50ms) setelah kursor tiba di titik sebelum menekan klik
    const settleWait = Math.max(0, Math.round((Number(hoverSettleMs) || 50) * (0.88 + Math.random() * 0.24)));
    if (settleWait > 0) {
      await sleep(settleWait);
      if (wc.isDestroyed()) return;
    }

    if (triggerDomTap) {
      try {
        wc.send('autoklik-ext:dom-tap', {
          x: cleanX,
          y: cleanY,
          iconDiameter: meta && meta.iconDiameter ? meta.iconDiameter : 35,
          iconRadius: meta && meta.iconRadius ? meta.iconRadius : 18,
          pointId: meta && meta.pointId ? meta.pointId : null,
          isPopupPin: Boolean(meta && meta.isPopupPin)
        });
      } catch (_) {}
    }
    // 4. Kirim mouseDown + mouseUp secara instan tepat pada titik Pin Ikon
    wc.sendInputEvent({ type: 'mouseDown', x, y, button: 'left', clickCount: 1 });
    wc.sendInputEvent({ type: 'mouseUp', x, y, button: 'left', clickCount: 1 });
  }

  async sendSmoothWheelSwipe(wc, cssX, cssY, direction, totalDistPx, durationMs = 340) {
    if (wc.isDestroyed()) return;
    const cleanX = Math.max(0, Math.round(Number(cssX) || 0));
    const cleanY = Math.max(0, Math.round(Number(cssY) || 0));
    const cleanDist = Math.max(40, Math.round(Number(totalDistPx) || 350));
    const cleanDur = Math.max(160, Math.round(Number(durationMs) || 340));
    const { x, y } = this.toDipPoint(wc, cleanX, cleanY);
    const totalDip = Math.max(40, cleanDist);
    // direction 'up' = swipe atas (▲) -> geser halaman ke bawah (scrollTop bertambah / deltaY negatif di Chromium)
    // direction 'down' = swipe bawah (▼) -> geser halaman ke atas (scrollTop berkurang / deltaY positif di Chromium)
    const sign = direction === 'down' ? 1 : -1;
    const steps = Math.max(8, Math.min(26, Math.round(cleanDur / 16)));

    try {
      wc.send('autoklik-ext:dom-scroll', {
        x: cleanX,
        y: cleanY,
        direction: direction === 'down' ? 'down' : 'up',
        distancePx: cleanDist,
        durationMs: cleanDur
      });
    } catch (_) {}

    // Distribusi langkah scroll menggunakan kurva Ease-In-Out Sinusoidal (mulai halus -> cepat di tengah -> melambat halus di akhir)
    const weights = [];
    let weightSum = 0;
    for (let i = 0; i < steps; i++) {
      const w = Math.sin(((i + 0.5) / steps) * Math.PI);
      weights.push(w);
      weightSum += w;
    }

    let sent = 0;
    for (let i = 0; i < steps; i++) {
      if (wc.isDestroyed()) return;
      const rawChunk = Math.max(2, Math.round((totalDip * weights[i]) / (weightSum || 1)));
      const chunk = i === steps - 1 ? Math.max(1, totalDip - sent) : rawChunk;
      sent += chunk;
      wc.sendInputEvent({
        type: 'mouseWheel',
        x,
        y,
        deltaX: 0,
        deltaY: sign * chunk,
        wheelTicksX: 0,
        wheelTicksY: sign * Math.max(0.2, Number((chunk / 100).toFixed(2))),
        canScroll: true
      });
      await sleep(16);
    }
  }

  async sendSwipe(wc, cssX1, cssY1, _cssX2, cssY2, durationMs) {
    if (wc.isDestroyed()) return;
    const dist = Math.max(60, Math.abs(Math.round((Number(cssY2) || 0) - (Number(cssY1) || 0))));
    const dir = Number(cssY2) > Number(cssY1) ? 'down' : 'up';
    await this.sendSmoothWheelSwipe(wc, cssX1, cssY1, dir, dist, durationMs || 340);
  }

  async readAndFocusInputLength(wc, cssX = null, cssY = null) {
    if (!wc || wc.isDestroyed()) return 0;
    const hasCoords = Number.isFinite(Number(cssX)) && Number.isFinite(Number(cssY));
    const xVal = hasCoords ? Math.round(Number(cssX)) : 'null';
    const yVal = hasCoords ? Math.round(Number(cssY)) : 'null';
    try {
      const len = await wc.executeJavaScript(`
        (() => {
          try {
            const host = document.getElementById('__autoklik_v32_host__');
            const prev = host ? host.style.display : null;
            if (host) host.style.display = 'none';
            let el = null;
            const px = ${'$'}{xVal};
            const py = ${'$'}{yVal};
            if (px !== null && py !== null) {
              const hit = document.elementFromPoint(px, py);
              if (hit) {
                if (hit.tagName === 'INPUT' || hit.tagName === 'TEXTAREA' || hit.isContentEditable) {
                  el = hit;
                } else if (hit.closest) {
                  const directInput = hit.closest('input, textarea, [contenteditable="true"]');
                  if (directInput) {
                    el = directInput;
                  } else {
                    const lbl = hit.closest('label');
                    if (lbl && lbl.control && (lbl.control.tagName === 'INPUT' || lbl.control.tagName === 'TEXTAREA')) {
                      el = lbl.control;
                    }
                  }
                }
                if (!el) {
                  // Cari kotak input yang area bounding-box-nya benar-benar mencakup koordinat Pin Ikon (px, py)
                  const candidates = document.querySelectorAll('input:not([type="hidden"]), textarea, [contenteditable="true"]');
                  let best = null;
                  let bestDist = 12;
                  for (const cand of candidates) {
                    const r = cand.getBoundingClientRect();
                    if (!r || r.width <= 0 || r.height <= 0) continue;
                    if (px >= r.left && px <= r.right && py >= r.top && py <= r.bottom) {
                      best = cand;
                      break;
                    }
                    const dx = px < r.left ? (r.left - px) : (px > r.right ? (px - r.right) : 0);
                    const dy = py < r.top ? (r.top - py) : (py > r.bottom ? (py - r.bottom) : 0);
                    const d = Math.hypot(dx, dy);
                    if (d < bestDist) {
                      bestDist = d;
                      best = cand;
                    }
                  }
                  if (best) el = best;
                }
              }
            }
            if (host) host.style.display = prev || '';
            if (!el) {
              const active = document.activeElement;
              if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable)) {
                el = active;
              }
            }
            if (!el) return 0;
            if (typeof el.focus === 'function') {
              el.focus({ preventScroll: true });
            }
            const val = el.isContentEditable ? String(el.textContent || '') : String(el.value || '');
            const l = val.length;
            if (typeof el.setSelectionRange === 'function') {
              try { el.setSelectionRange(l, l); } catch (_) {}
            }
            return l;
          } catch (_) {
            return 0;
          }
        })()
      `, true);
      return Math.max(0, Number(len) || 0);
    } catch (_) {
      return 0;
    }
  }

  async clearFocusedInput(wc, cssX = null, cssY = null) {
    if (wc.isDestroyed()) return;
    // 100% Murni Keyboard:
    // 1) Baca jumlah karakter yang ada di dalam kotak input & letakkan kursor di ujung kanan teks (End)
    const charCount = await this.readAndFocusInputLength(wc, cssX, cssY);
    if (wc.isDestroyed()) return;

    wc.sendInputEvent({ type: 'keyDown', keyCode: 'End' });
    wc.sendInputEvent({ type: 'keyUp', keyCode: 'End' });
    await sleep(10);

    // 2) Tekan tombol Backspace berulang sesuai panjang teks yang ada di kotak input
    const backspaceRepeats = Math.min(120, Math.max(1, charCount));
    for (let i = 0; i < backspaceRepeats; i++) {
      if (wc.isDestroyed()) return;
      wc.sendInputEvent({ type: 'keyDown', keyCode: 'Backspace' });
      await sleep(5);
      wc.sendInputEvent({ type: 'keyUp', keyCode: 'Backspace' });
      await sleep(14);
    }

    // 3) Jika masih ada sisa karakter (misal posisi caret tadi di tengah), bersihkan sisa karakter dengan Backspace/Delete murni keyboard
    if (charCount > 0) {
      const remaining = await this.readAndFocusInputLength(wc, cssX, cssY);
      const extraRepeats = Math.min(60, Math.max(0, remaining));
      for (let j = 0; j < extraRepeats; j++) {
        if (wc.isDestroyed()) return;
        wc.sendInputEvent({ type: 'keyDown', keyCode: 'Backspace' });
        wc.sendInputEvent({ type: 'keyUp', keyCode: 'Backspace' });
        wc.sendInputEvent({ type: 'keyDown', keyCode: 'Delete' });
        wc.sendInputEvent({ type: 'keyUp', keyCode: 'Delete' });
        await sleep(12);
      }
    }
    await sleep(14);
  }

  buildKeyboardEventSpec(ch) {
    if (ch === ' ') {
      return { keyCode: 'Space', charCode: ' ', modifiers: [] };
    }
    if (ch >= 'a' && ch <= 'z') {
      return { keyCode: ch.toUpperCase(), charCode: ch, modifiers: [] };
    }
    if (ch >= 'A' && ch <= 'Z') {
      return { keyCode: ch, charCode: ch, modifiers: ['shift'] };
    }
    if (ch >= '0' && ch <= '9') {
      return { keyCode: ch, charCode: ch, modifiers: [] };
    }
    const shiftSymbolMap = {
      '!': '1', '@': '2', '#': '3', '$': '4', '%': '5',
      '^': '6', '&': '7', '*': '8', '(': '9', ')': '0',
      '_': '-', '+': '=', '{': '[', '}': ']', '|': '\\',
      ':': ';', '"': "'", '<': ',', '>': '.', '?': '/', '~': '`'
    };
    if (Object.prototype.hasOwnProperty.call(shiftSymbolMap, ch)) {
      return { keyCode: shiftSymbolMap[ch], charCode: ch, modifiers: ['shift'] };
    }
    return { keyCode: ch, charCode: ch, modifiers: [] };
  }

  async typeTextChars(wc, text, typingSpeedMs = 30, meta = {}) {
    if (wc.isDestroyed()) return;
    const str = String(text ?? '');
    if (!str.length) return;
    const cleanX = meta && Number.isFinite(Number(meta.x)) ? Math.round(Number(meta.x)) : null;
    const cleanY = meta && Number.isFinite(Number(meta.y)) ? Math.round(Number(meta.y)) : null;

    // Pastikan fokus berada di dalam kotak input sebelum mulai mengetik (tanpa mengubah nilai .value)
    await this.readAndFocusInputLength(wc, cleanX, cleanY);
    await sleep(12);

    // Default kecepatan ketik = 30ms/huruf (100% murni keyboard fisik: keyDown -> char -> keyUp per karakter)
    const baseDelay = Math.max(5, Number.isFinite(Number(typingSpeedMs)) && Number(typingSpeedMs) > 0 ? Number(typingSpeedMs) : 30);
    for (const ch of str) {
      if (wc.isDestroyed()) return;
      const spec = this.buildKeyboardEventSpec(ch);
      wc.sendInputEvent({ type: 'keyDown', keyCode: spec.keyCode, modifiers: spec.modifiers });
      wc.sendInputEvent({ type: 'char', keyCode: spec.charCode, modifiers: spec.modifiers });
      const holdMs = Math.min(12, Math.max(4, Math.round(baseDelay * 0.25)));
      await sleep(holdMs);
      if (wc.isDestroyed()) return;
      wc.sendInputEvent({ type: 'keyUp', keyCode: spec.keyCode, modifiers: spec.modifiers });
      const stepDelay = Math.max(4, Math.round(baseDelay * (0.85 + Math.random() * 0.30)) - holdMs);
      await sleep(stepDelay);
    }
  }

  async sendSelectAllAndType(wc, text, opts = {}) {
    if (wc.isDestroyed()) return;
    const clearBefore = opts.clearBefore !== false;
    const charDelay = Math.max(5, Number.isFinite(Number(opts.typingSpeedMs)) && Number(opts.typingSpeedMs) > 0 ? Number(opts.typingSpeedMs) : 30);
    if (clearBefore) {
      await this.clearFocusedInput(wc, opts.x, opts.y);
    }
    await this.typeTextChars(wc, text, charDelay, { x: opts.x, y: opts.y, isFinal: true });
  }

  async sendPopupKey(wc, keyCode) {
    if (wc.isDestroyed()) return;
    wc.sendInputEvent({ type: 'keyDown', keyCode });
    await sleep(16);
    wc.sendInputEvent({ type: 'keyUp', keyCode });
  }
}

module.exports = { AutomationEngine };
    """.trimIndent() + "\n"

  fun memoryWatchdogJs(): String =
    """
const { app } = require('electron');

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

class MemoryWatchdog {
  constructor(runtimeConfig, sessionManager, automationEngine, onStateChange) {
    this.limitMb = Number(runtimeConfig.watchdogCalibrationTargetMb || runtimeConfig.memoryWatchdogLimitMb) || 350;
    this.delayMs = Number(runtimeConfig.staggeredReloadDelayMs) || 400;
    this.sessionManager = sessionManager;
    this.automationEngine = automationEngine;
    this.onStateChange = onStateChange;
    this.logRotation = null;
    this.tabMemoryMb = new Map();
    this.reloadQueue = [];
    this.isProcessingQueue = false;
    this.timer = null;
    this.recycleCount = 0;
  }

  attachLogRotation(logger) {
    this.logRotation = logger;
  }

  setLimitMb(nextLimitMb) {
    if (Number.isFinite(Number(nextLimitMb))) {
      this.limitMb = Math.min(550, Math.max(250, Number(nextLimitMb)));
    }
    return this.limitMb;
  }

  start() {
    if (this.timer) return;
    this.timer = setInterval(() => this.sampleMetrics(), 15000);
  }

  stop() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  sampleMetrics() {
    const metrics = app.getAppMetrics();
    const pidMemoryMap = new Map();
    for (const m of metrics) {
      const memKb = (m.memory && m.memory.workingSetSize) || 0;
      pidMemoryMap.set(m.pid, Math.round(memKb / 1024));
    }

    for (const tab of this.sessionManager.tabs.values()) {
      if (tab.view.webContents.isDestroyed()) continue;
      const osPid = tab.view.webContents.getOSProcessId();
      const mb = pidMemoryMap.get(osPid) || 0;
      this.tabMemoryMb.set(tab.tabId, mb);

      const runner = this.automationEngine.getRunnerState(tab.tabId);
      const isActiveVisible = this.sessionManager.activeTabId === tab.tabId;

      if (
        !isActiveVisible &&
        runner.state === 'IDLE' &&
        mb > this.limitMb &&
        !this.reloadQueue.includes(tab.tabId)
      ) {
        this.reloadQueue.push(tab.tabId);
      }
    }

    if (this.reloadQueue.length > 0) {
      this.processQueue();
    }
    this.onStateChange();
  }

  enqueueStandbyManualReload(options = {}) {
    const visibleSet = new Set(Array.isArray(this.sessionManager.splitTabIds) ? this.sessionManager.splitTabIds : []);
    if (this.sessionManager.activeTabId) {
      visibleSet.add(this.sessionManager.activeTabId);
    }

    let queuedCount = 0;
    for (const tab of this.sessionManager.tabs.values()) {
      const runner = this.automationEngine.getRunnerState(tab.tabId);
      const isVisible = visibleSet.has(tab.tabId);
      if (
        runner.state === 'IDLE' &&
        (options.includeVisible === true || !isVisible) &&
        !this.reloadQueue.includes(tab.tabId)
      ) {
        this.reloadQueue.push(tab.tabId);
        queuedCount += 1;
      }
    }
    if (queuedCount > 0) {
      this.processQueue();
    }
    return { queuedCount };
  }

  async processQueue() {
    if (this.isProcessingQueue) return;
    this.isProcessingQueue = true;

    try {
      while (this.reloadQueue.length > 0) {
        const tabId = this.reloadQueue.shift();
        const tab = this.sessionManager.tabs.get(tabId);
        const runner = this.automationEngine.getRunnerState(tabId);

        if (tab && !tab.view.webContents.isDestroyed() && runner.state === 'IDLE') {
          tab.view.webContents.reloadIgnoringCache();
          this.recycleCount += 1;
          if (this.logRotation) {
            this.logRotation.write('WATCHDOG_RECYCLE', {
              tabId,
              memoryMb: this.getTabMemoryMb(tabId),
              limitMb: this.limitMb,
              recycleCount: this.recycleCount
            });
          }
          await sleep(this.delayMs);
        }
      }
    } finally {
      this.isProcessingQueue = false;
      this.onStateChange();
    }
  }

  getTabMemoryMb(tabId) {
    return this.tabMemoryMb.get(tabId) || 0;
  }
}

module.exports = { MemoryWatchdog };
    """.trimIndent() + "\n"
}
