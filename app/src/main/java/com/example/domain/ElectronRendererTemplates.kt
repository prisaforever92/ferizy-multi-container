package com.example.domain

object ElectronRendererTemplates {

  fun shellPreloadJs(): String =
    """
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
    """.trimIndent() + "\n"

  fun ferizyPreloadJs(): String =
    """
const { ipcRenderer, webFrame } = require('electron');

function parseAccountId() {
  const arg = process.argv.find((item) => item.startsWith('--ferizy-account-id='));
  const parsed = arg ? Number(arg.split('=')[1]) : 1;
  return Number.isFinite(parsed) && parsed >= 1 && parsed <= 20 ? parsed : 1;
}

const accountId = parseAccountId();
const geoConfig = ipcRenderer.sendSync('geo:get-config-sync', accountId) || {
  mode: 'preset_0ms',
  effectiveLatitude: -5.9308,
  effectiveLongitude: 105.9935,
  accuracy: 18
};

const fase3Flags = ipcRenderer.sendSync('fase3:get-preload-flags-sync') || {
  latencyCompensationEnabled: false,
  fastRequestTimeoutMs: 4500,
  smartTargetMatcherEnabled: true,
  smartMatcherFallbackNearest: true,
  smartMatcherServiceType: 'all',
  priorityHours: ['08:00', '10:00', '12:00', '14:00', '20:00']
};

// Jembatan dari Main-World 0ms API Sniffer ke Main Process IPC
window.addEventListener('message', (event) => {
  if (event.source !== window || !event.data || event.data.__ferizySniffer !== true) return;
  ipcRenderer.send('sniffer:api-response', event.data.packet);
});

// Fase 1 + Fase 3: Geolocation 0ms, navigator.webdriver hygiene, & Main-World 0ms API Sniffer (Tanpa chrome.debugger)
const mainWorldPatch = `
(() => {
  try {
    Object.defineProperty(navigator, 'webdriver', { get: () => false, configurable: true });
  } catch (_) {}

  const cfg = ${'$'}{JSON.stringify(geoConfig)};
  const flags = ${'$'}{JSON.stringify(fase3Flags)};

  if (navigator.permissions && navigator.permissions.query) {
    const origQuery = navigator.permissions.query.bind(navigator.permissions);
    navigator.permissions.query = (desc) => {
      if (desc && desc.name === 'geolocation' && location.origin.startsWith('https://trip.ferizy.com')) {
        return Promise.resolve({ state: 'granted', onchange: null });
      }
      return origQuery(desc);
    };
  }

  if (cfg.mode === 'preset_0ms' && navigator.geolocation) {
    const buildCoords = () => ({
      coords: {
        latitude: cfg.effectiveLatitude,
        longitude: cfg.effectiveLongitude,
        accuracy: cfg.accuracy || 18,
        altitude: null,
        altitudeAccuracy: null,
        heading: null,
        speed: null
      },
      timestamp: Date.now()
    });

    navigator.geolocation.getCurrentPosition = (success) => {
      if (typeof success === 'function') success(buildCoords());
    };
    let watchSeq = 1;
    navigator.geolocation.watchPosition = (success) => {
      if (typeof success === 'function') success(buildCoords());
      return watchSeq++;
    };
  }

  function inspectFerizyPayload(reqUrl, data) {
    if (!data || typeof data !== 'object') return;
    const rawList = Array.isArray(data)
      ? data
      : Array.isArray(data.schedules)
        ? data.schedules
        : Array.isArray(data.data)
          ? data.data
          : Array.isArray(data.routes)
            ? data.routes
            : Array.isArray(data.lintasan)
              ? data.lintasan
              : Array.isArray(data.results)
                ? data.results
                : null;

    if (!rawList || rawList.length === 0) return;

    const urlStr = String(reqUrl || '').toLowerCase();
    const looksSchedule =
      /ferryschedule|schedule|jadwal|quota|kuota|trip/.test(urlStr) ||
      rawList.some((item) => item && (item.quota !== undefined || item.available !== undefined || item.sisa !== undefined || item.departureTime || item.jam || item.etd));

    if (looksSchedule) {
      const slots = rawList.map((s) => ({
        quota: Number((s && (s.quota ?? s.available ?? s.sisa)) ?? 0),
        departureTime: String((s && (s.departureTime || s.time || s.jam || s.etd)) || ''),
        serviceType: String((s && (s.serviceType || s.service || s.className || s.shipClass || s.type)) || ''),
        status: String((s && s.status) || '')
      }));
      window.postMessage({
        __ferizySniffer: true,
        packet: { kind: 'schedule', url: reqUrl, slots }
      }, '*');
      return;
    }

    const looksLintasan =
      /lintasan|route|harbor|pelabuhan|origin|destination/.test(urlStr) ||
      location.pathname === '/';

    if (looksLintasan) {
      const hasAvailable = rawList.some((r) => {
        if (!r || typeof r !== 'object') return false;
        if (r.isOpen === true || r.open === true || r.available === true) return true;
        if (Number(r.quota || r.sisa || 0) > 0) return true;
        return /open|buka|tersedia|active/i.test(String(r.status || ''));
      });
      if (hasAvailable) {
        window.postMessage({
          __ferizySniffer: true,
          packet: { kind: 'lintasan', url: reqUrl, hasAvailable: true }
        }, '*');
      }
    }
  }

  if (typeof window.fetch === 'function') {
    const origFetch = window.fetch.bind(window);
    window.fetch = function(input, init) {
      const reqUrl = typeof input === 'string' ? input : (input && input.url) || '';
      const isFerizy = reqUrl.startsWith('/') || reqUrl.startsWith('https://trip.ferizy.com');
      // V5.8 High-Speed 200 Mbps Fast-Path: 0ms native fetch pass-through tanpa pembungkus AbortController yang menahan/memperlambat respon halaman
      const p = origFetch(input, init);
      if (isFerizy && p && typeof p.then === 'function') {
        p.then((response) => {
          if (response && response.ok && typeof response.clone === 'function') {
            const ct = (response.headers && typeof response.headers.get === 'function' && response.headers.get('content-type')) || '';
            if (!ct || ct.includes('json')) {
              response.clone().json().then((json) => inspectFerizyPayload(reqUrl, json)).catch(() => {});
            }
          }
        }).catch(() => {});
      }
      return p;
    };
  }

  if (window.XMLHttpRequest && window.XMLHttpRequest.prototype) {
    const origOpen = window.XMLHttpRequest.prototype.open;
    const origSend = window.XMLHttpRequest.prototype.send;
    window.XMLHttpRequest.prototype.open = function(method, url) {
      this.__ferizyReqUrl = typeof url === 'string' ? url : '';
      return origOpen.apply(this, arguments);
    };
    window.XMLHttpRequest.prototype.send = function() {
      const reqUrl = this.__ferizyReqUrl || '';
      const isFerizy = reqUrl.startsWith('/') || reqUrl.startsWith('https://trip.ferizy.com');
      if (isFerizy) {
        this.addEventListener('load', function() {
          if (this.status >= 200 && this.status < 300 && typeof this.responseText === 'string') {
            try {
              const parsed = JSON.parse(this.responseText);
              inspectFerizyPayload(reqUrl, parsed);
            } catch (_) {}
          }
        }, { passive: true });
      }
      return origSend.apply(this, arguments);
    };
  }
})();
`;

webFrame.executeJavaScript(mainWorldPatch, true).catch(() => {});

// Fase 2 & 3: Direct Element Cache, Radius Scanner, Smart Target Matcher, Lazy Anti-Revert, dan Observer CAPTCHA
const elementCache = new Map();
const watchedInputs = new Map();
let lastCaptchaNoticeAt = 0;

function isVisible(el) {
  if (!el) return false;
  const r = el.getBoundingClientRect();
  return r.width > 0 && r.height > 0;
}

function findSmartPriorityElement() {
  const liveFlags = ipcRenderer.sendSync('fase3:get-preload-flags-sync') || fase3Flags;
  if (!liveFlags.smartTargetMatcherEnabled || !location.pathname.startsWith('/ferryschedule')) {
    return null;
  }
  const hours = Array.isArray(liveFlags.priorityHours) ? liveFlags.priorityHours : [];
  const svcMode = liveFlags.smartMatcherServiceType || 'all';
  const rows = Array.from(document.querySelectorAll('[class*="schedule"], [class*="card"], tr, .row')).filter((row) => {
    const txt = (row.innerText || '').toLowerCase();
    if (!txt || /habis|penuh|sold\s*out/i.test(txt)) return false;
    if (svcMode === 'express' && !/express|ekspres/.test(txt)) return false;
    if (svcMode === 'reguler' && !/reguler|regular/.test(txt)) return false;
    const btn = row.querySelector('button, [role="button"], a');
    return Boolean(btn && isVisible(btn) && !btn.disabled);
  });

  for (const h of hours) {
    const matchRow = rows.find((row) => (row.innerText || '').includes(h));
    if (matchRow) {
      const btn = matchRow.querySelector('button, [role="button"], a');
      const r = btn.getBoundingClientRect();
      return { x: r.left + r.width / 2, y: r.top + r.height / 2, found: true };
    }
  }

  if (liveFlags.smartMatcherFallbackNearest && rows.length > 0) {
    const btn = rows[0].querySelector('button, [role="button"], a');
    const r = btn.getBoundingClientRect();
    return { x: r.left + r.width / 2, y: r.top + r.height / 2, found: true };
  }
  return null;
}

function resolveWithCacheAndRadius(req) {
  if (req.selector) {
    let direct = elementCache.get(req.selector);
    if (!direct || !document.contains(direct) || !isVisible(direct)) {
      direct = document.querySelector(req.selector);
      if (direct && isVisible(direct)) {
        elementCache.set(req.selector, direct);
      } else {
        direct = null;
      }
    }
    if (direct) {
      const r = direct.getBoundingClientRect();
      const inside = req.x >= r.left && req.x <= r.right && req.y >= r.top && req.y <= r.bottom;
      if (inside) {
        return { x: req.x, y: req.y, found: true };
      }
      return { x: r.left + r.width / 2, y: r.top + r.height / 2, found: true };
    }
  }
  return { x: req.x, y: req.y, found: false };
}

ipcRenderer.on('automation:resolve-target', (_evt, req) => {
  const res = resolveWithCacheAndRadius(req);
  ipcRenderer.send('automation:target-resolved', { reqId: req.reqId, ...res });
});

ipcRenderer.on('automation:watch-input', (_evt, payload) => {
  if (!payload || !payload.expectedValue) return;
  const key = payload.selector || `${'$'}{payload.x}_${'$'}{payload.y}`;
  watchedInputs.set(key, { ...payload, expiresAt: Date.now() + 2600, retries: 0 });
});

setInterval(() => {
  const now = Date.now();
  for (const [key, item] of watchedInputs.entries()) {
    if (now > item.expiresAt || item.retries >= 2) {
      watchedInputs.delete(key);
      continue;
    }
    const el = item.selector ? document.querySelector(item.selector) : document.elementFromPoint(item.x, item.y);
    if (el && typeof el.value === 'string' && el.value !== item.expectedValue) {
      item.retries += 1;
      ipcRenderer.send('automation:anti-revert-trigger', {
        x: item.x,
        y: item.y,
        expectedValue: item.expectedValue
      });
    }
  }
}, 280);

function scanDomSignals() {
  const btns = document.querySelectorAll('button, [role="button"]');
  for (const btn of btns) {
    const txt = (btn.innerText || '').trim();
    if (/^(Pesan Sekarang|Setuju|Ya, Lanjutkan)$/i.test(txt) && isVisible(btn)) {
      ipcRenderer.send('automation:dom-trigger', { label: txt });
      break;
    }
  }

  const now = Date.now();
  if (now - lastCaptchaNoticeAt > 5000) {
    // Hanya deteksi popup challenge CAPTCHA interaktif yang benar-benar terbuka & menghalangi layar (bukan badge reCAPTCHA pasif/anchor)
    const challengeFrames = document.querySelectorAll(
      'iframe[src*="recaptcha/api2/bframe"], iframe[src*="recaptcha/enterprise/bframe"], iframe[src*="hcaptcha.com"][src*="challenge"]'
    );
    for (const frame of challengeFrames) {
      if (!isVisible(frame)) continue;
      const r = frame.getBoundingClientRect();
      if (r.width >= 280 && r.height >= 280 && r.bottom > 80 && r.right > 80) {
        lastCaptchaNoticeAt = now;
        ipcRenderer.send('automation:captcha-detected', { detail: 'Popup Challenge CAPTCHA Aktif' });
        break;
      }
    }
  }
}

window.addEventListener('DOMContentLoaded', () => {
  const observer = new MutationObserver(scanDomSignals);
  if (document.body) {
    observer.observe(document.body, { childList: true, subtree: true });
  }
});

// AutoKlik-v3.2-Mandiri-Extension: Live In-Page Target Picker
let activePickerCleanup = null;

function buildCssSelector(el) {
  if (!el || el.nodeType !== 1) return '';
  if (el.id && /^[a-zA-Z][\w:-]*$/.test(el.id)) {
    return `#${'$'}{el.id}`;
  }
  const tag = el.tagName.toLowerCase();
  const nameAttr = el.getAttribute('name');
  if (nameAttr && /^[a-zA-Z0-9_.\-]+$/.test(nameAttr)) {
    return `${'$'}{tag}[name="${'$'}{nameAttr}"]`;
  }
  const parts = [];
  let cur = el;
  while (cur && cur.nodeType === 1 && parts.length < 4) {
    let seg = cur.tagName.toLowerCase();
    if (cur.id && /^[a-zA-Z][\w:-]*$/.test(cur.id)) {
      parts.unshift(`#${'$'}{cur.id}`);
      break;
    }
    const parent = cur.parentElement;
    if (parent) {
      const sameTagSiblings = Array.from(parent.children).filter((c) => c.tagName === cur.tagName);
      if (sameTagSiblings.length > 1) {
        seg += `:nth-of-type(${'$'}{sameTagSiblings.indexOf(cur) + 1})`;
      }
    }
    parts.unshift(seg);
    cur = parent;
  }
  return parts.join(' > ');
}

function stopTargetPicker() {
  if (typeof activePickerCleanup === 'function') {
    activePickerCleanup();
    activePickerCleanup = null;
  }
}

function startTargetPicker(cfg) {
  stopTargetPicker();
  const targetAccountId = (cfg && cfg.accountId) || accountId;
  const targetTabIndex = (cfg && cfg.tabIndex) || (overlayState && overlayState.tabIndex) || 1;
  const preferredType = (cfg && cfg.pointType) || 'tap';
  const replacePointId = (cfg && cfg.replacePointId) || null;
  const nextPointNum = Array.isArray(overlayState && overlayState.points)
    ? (replacePointId
      ? Math.max(1, overlayState.points.findIndex((p) => p && p.id === replacePointId) + 1)
      : overlayState.points.length + 1)
    : 1;

  // Pertahankan marker titik yang sudah ada tetap terlihat (dengan pointer-events: none) agar user mudah menyelaraskan posisi
  renderAutoKlikMarkers();

  const overlay = document.createElement('div');
  overlay.setAttribute('data-autoklik-overlay', '1');
  overlay.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483646;border:1.5px dashed #22d3ee;background:rgba(34,211,238,0.10);border-radius:4px;display:none;transition:all 25ms linear;';

  // Ghost Pin Icon yang mengikuti ujung kursor secara presisi 1:1 sebelum diklik
  const ghostPin = document.createElement('div');
  ghostPin.setAttribute('data-autoklik-overlay', '1');
  ghostPin.style.cssText = 'position:fixed;left:-100px;top:-100px;width:35px;height:35px;transform:translate(-50%,-50%);border-radius:50%;border:2.5px solid #22d3ee;background:rgba(8,145,178,0.82);color:#fff;display:none;align-items:center;justify-content:center;font-family:"Segoe UI",sans-serif;font-size:13px;font-weight:800;z-index:2147483647;pointer-events:none;box-shadow:0 0 0 1.5px rgba(2,6,23,0.95),0 0 14px rgba(34,211,238,0.75);';
  ghostPin.innerHTML = `<span style="position:relative;z-index:2;">${'$'}{nextPointNum}</span><span style="position:absolute;left:50%;top:2px;bottom:2px;width:1px;background:rgba(255,255,255,0.65);transform:translateX(-50%);"></span><span style="position:absolute;top:50%;left:2px;right:2px;height:1px;background:rgba(255,255,255,0.65);transform:translateY(-50%);"></span><span style="position:absolute;left:50%;top:50%;width:5px;height:5px;border-radius:50%;background:#fde047;border:1px solid #020617;transform:translate(-50%,-50%);z-index:3;"></span>`;

  const banner = document.createElement('div');
  banner.setAttribute('data-autoklik-overlay', '1');
  banner.style.cssText = 'position:fixed;top:8px;left:50%;transform:translateX(-50%);z-index:2147483647;background:#0b1528;color:#cffafe;border:1px solid #22d3ee;border-radius:8px;padding:6px 12px;font-family:"Segoe UI",sans-serif;font-size:12px;box-shadow:0 6px 24px rgba(0,0,0,0.45);pointer-events:none;';
  banner.textContent = replacePointId
    ? `AutoKlik v3.2 (Akun ${'$'}{targetAccountId} Tab ${'$'}{targetTabIndex}) • Arahkan titik tengah kuning (+) ke posisi klik baru • ESC = Batal`
    : `AutoKlik-v3.2 (Akun ${'$'}{targetAccountId} Tab ${'$'}{targetTabIndex}) • Arahkan titik tengah kuning (+) ke posisi target • ESC = Batal`;

  (document.body || document.documentElement).appendChild(overlay);
  (document.body || document.documentElement).appendChild(ghostPin);
  (document.body || document.documentElement).appendChild(banner);

  const onMove = (e) => {
    const cx = Math.round(e.clientX);
    const cy = Math.round(e.clientY);
    ghostPin.style.display = 'flex';
    ghostPin.style.left = `${'$'}{cx}px`;
    ghostPin.style.top = `${'$'}{cy}px`;

    const el = elementFromPointClean(cx, cy);
    if (!el || el.hasAttribute('data-autoklik-overlay')) return;
    const r = el.getBoundingClientRect();
    overlay.style.display = 'block';
    overlay.style.left = `${'$'}{r.left}px`;
    overlay.style.top = `${'$'}{r.top}px`;
    overlay.style.width = `${'$'}{r.width}px`;
    overlay.style.height = `${'$'}{r.height}px`;
    banner.textContent = `AutoKlik v3.2 (Akun ${'$'}{targetAccountId} T${'$'}{targetTabIndex}) • Titik #${'$'}{nextPointNum} tepat di x=${'$'}{cx}, y=${'$'}{cy} (${'$'}{el.tagName.toLowerCase()}) • Klik untuk kunci posisi (ESC batal)`;
  };

  const lockScrollX = window.scrollX || 0;
  const lockScrollY = window.scrollY || 0;

  const blockPreClickEvents = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (typeof e.stopImmediatePropagation === 'function') {
      e.stopImmediatePropagation();
    }
    if ((window.scrollX || 0) !== lockScrollX || (window.scrollY || 0) !== lockScrollY) {
      window.scrollTo(lockScrollX, lockScrollY);
    }
  };

  const onClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (typeof e.stopImmediatePropagation === 'function') {
      e.stopImmediatePropagation();
    }
    if ((window.scrollX || 0) !== lockScrollX || (window.scrollY || 0) !== lockScrollY) {
      window.scrollTo(lockScrollX, lockScrollY);
    }
    const x = Math.round(e.clientX);
    const y = Math.round(e.clientY);
    const el = elementFromPointClean(x, y) || e.target;
    // V5.8 Adjacent Point Isolation: Simpan koordinat murni (selector = '') agar titik yang ditempatkan berdekatan tidak saling terhubung/mempengaruhi
    const selector = '';
    const isInputEl = el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable);
    const rawLabel = el ? (el.innerText || el.getAttribute('aria-label') || el.getAttribute('placeholder') || el.getAttribute('name') || el.tagName) : 'Target';
    const label = String(rawLabel || 'Target').replace(/\s+/g, ' ').trim().slice(0, 28) || `Target (${'$'}{x},${'$'}{y})`;
    const inputValue = isInputEl && typeof el.value === 'string' ? el.value : '';
    const detectedType = preferredType !== 'tap' ? preferredType : (isInputEl ? 'input' : 'tap');

    stopTargetPicker();
    ipcRenderer.send('autoklik-ext:picked-result', {
      accountId: targetAccountId,
      tabIndex: targetTabIndex,
      pointType: detectedType,
      replacePointId,
      x,
      y,
      selector,
      label,
      inputValue,
      cancelled: false
    });
  };

  const onKey = (e) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      stopTargetPicker();
      ipcRenderer.send('autoklik-ext:picked-result', {
        accountId: targetAccountId,
        tabIndex: targetTabIndex,
        replacePointId,
        cancelled: true
      });
    }
  };

  document.addEventListener('pointerdown', blockPreClickEvents, true);
  document.addEventListener('mousedown', blockPreClickEvents, true);
  document.addEventListener('mouseup', blockPreClickEvents, true);
  document.addEventListener('mousemove', onMove, true);
  document.addEventListener('click', onClick, true);
  document.addEventListener('keydown', onKey, true);

  activePickerCleanup = () => {
    document.removeEventListener('pointerdown', blockPreClickEvents, true);
    document.removeEventListener('mousedown', blockPreClickEvents, true);
    document.removeEventListener('mouseup', blockPreClickEvents, true);
    document.removeEventListener('mousemove', onMove, true);
    document.removeEventListener('click', onClick, true);
    document.removeEventListener('keydown', onKey, true);
    try { overlay.remove(); } catch (_) {}
    try { ghostPin.remove(); } catch (_) {}
    try { banner.remove(); } catch (_) {}
    isPickerRunning = false;
    renderAutoKlikOverlay();
  };
}

// AutoKlik-v3.2-Mandiri-Extension: Visible Point Markers (Mandiri Per-Tab, Tanpa Popup Melayang)
let overlayState = ipcRenderer.sendSync('autoklik-ext:get-overlay-sync', accountId) || {
  accountId,
  tabId: null,
  tabIndex: 1,
  runnerState: 'IDLE',
  points: [],
  extensionSettings: {
    enabled: true,
    loopEnabled: true,
    maxLoops: 0,
    loopDelayMs: 350,
    radiusPx: 28,
    smartMatch: true,
    antiRevert: true,
    markersHidden: false,
    pointsLocked: false
  },
  storeFile: `akun_${'$'}{accountId}.json`
};
let isPickerRunning = false;
let isDraggingMarker = false;
let pendingOverlayState = null;
let currentActivePointId = null;

let akShadowHost = null;
let akShadowRoot = null;
let akMarkersLayer = null;
let akSvgLayer = null;
let akFloatingPill = null;
let floatingPillPos = { right: 12, bottom: 14 };

function ensureAutoKlikHost() {
  const mountParent = document.documentElement || document.body;
  if (!mountParent) return false;
  if (akShadowHost && akShadowHost.isConnected && akShadowRoot) return true;

  const existing = document.getElementById('__autoklik_v32_host__');
  if (existing) {
    try { existing.remove(); } catch (_) {}
  }

  akShadowHost = document.createElement('div');
  akShadowHost.id = '__autoklik_v32_host__';
  akShadowHost.setAttribute('data-autoklik-overlay', '1');
  akShadowHost.style.cssText = 'position:fixed;inset:0;pointer-events:none !important;z-index:2147483645;';
  akShadowRoot = akShadowHost.attachShadow({ mode: 'open' });

  const style = document.createElement('style');
  style.textContent = `
    * { box-sizing: border-box; font-family: "Segoe UI", system-ui, sans-serif; }
    .ak-markers { position: fixed; inset: 0; pointer-events: none !important; overflow: hidden; }
    .ak-svg { position: fixed; inset: 0; width: 100%; height: 100%; pointer-events: none !important; }
    .ak-pin { position: fixed; width: 0; height: 0; pointer-events: none !important; }
    .ak-crosshair {
      position: absolute;
      left: 0;
      top: 0;
      width: var(--ak-size, 35px);
      height: var(--ak-size, 35px);
      transform: translate(-50%, -50%);
      border-radius: 50%;
      border: 2.5px solid #22d3ee;
      background: rgba(8, 145, 178, 0.78);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--ak-font, 14px);
      font-weight: 800;
      line-height: 1;
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.9);
      box-shadow: 0 0 0 1.5px rgba(2, 6, 23, 0.9), 0 3px 10px rgba(34, 211, 238, 0.45);
      pointer-events: auto;
      cursor: grab;
      touch-action: none;
      user-select: none;
    }
    .ak-crosshair:active { cursor: grabbing; transform: translate(-50%, -50%) scale(1.08); }
    .ak-crosshair::before {
      content: "";
      position: absolute;
      inset: 3px;
      background:
        linear-gradient(to right, transparent calc(50% - 0.5px), rgba(255, 255, 255, 0.52) calc(50% - 0.5px), rgba(255, 255, 255, 0.52) calc(50% + 0.5px), transparent calc(50% + 0.5px)),
        linear-gradient(to bottom, transparent calc(50% - 0.5px), rgba(255, 255, 255, 0.52) calc(50% - 0.5px), rgba(255, 255, 255, 0.52) calc(50% + 0.5px), transparent calc(50% + 0.5px));
      border-radius: 50%;
      pointer-events: none;
    }
    .ak-crosshair::after {
      content: "";
      position: absolute;
      left: 50%;
      top: 50%;
      width: 5px;
      height: 5px;
      transform: translate(-50%, -50%);
      border-radius: 50%;
      background: #fde047;
      border: 1px solid rgba(2, 6, 23, 0.95);
      box-shadow: 0 0 4px rgba(253, 224, 71, 0.95);
      pointer-events: none;
    }
    .ak-pin-num {
      position: relative;
      z-index: 2;
      pointer-events: none;
    }
    .ak-click-ripple {
      position: fixed;
      width: 18px;
      height: 18px;
      transform: translate(-50%, -50%);
      border-radius: 50%;
      border: 2px solid #fde047;
      background: rgba(250, 204, 21, 0.35);
      pointer-events: none;
      z-index: 2147483647;
      animation: akClickRippleAnim 0.36s ease-out forwards;
    }
    @keyframes akClickRippleAnim {
      0% { transform: translate(-50%, -50%) scale(0.45); opacity: 1; }
      100% { transform: translate(-50%, -50%) scale(1.9); opacity: 0; }
    }
    .ak-pin.type-input .ak-crosshair {
      border-color: #fbbf24;
      background: rgba(180, 83, 9, 0.82);
      box-shadow: 0 0 0 1.5px rgba(2, 6, 23, 0.9), 0 3px 10px rgba(245, 158, 11, 0.45);
    }
    .ak-pin.type-swipe_up .ak-crosshair,
    .ak-pin.type-swipe_down .ak-crosshair,
    .ak-pin.type-swipe .ak-crosshair {
      border-color: #34d399;
      background: rgba(5, 150, 105, 0.82);
      box-shadow: 0 0 0 1.5px rgba(2, 6, 23, 0.9), 0 3px 10px rgba(16, 185, 129, 0.45);
    }
    .ak-pin.type-autofill-pin .ak-crosshair {
      width: 28px;
      height: 28px;
      font-size: 11px;
      border-radius: 8px;
      border-color: #e879f9;
      background: rgba(126, 34, 206, 0.88);
      box-shadow: 0 0 0 1.5px rgba(2, 6, 23, 0.9), 0 3px 10px rgba(168, 85, 247, 0.55);
    }
    .ak-pin.swipe-end .ak-crosshair {
      width: 22px;
      height: 22px;
      font-size: 11px;
      border-radius: 50%;
      border-style: dashed;
      border-color: #34d399;
      background: rgba(6, 78, 59, 0.85);
    }
    .ak-pin.type-popup_key .ak-crosshair {
      border-color: #60a5fa;
      background: rgba(29, 78, 216, 0.82);
    }
    .ak-pin.disabled .ak-crosshair {
      border-style: dashed;
      border-color: #64748b;
      background: rgba(51, 65, 85, 0.65);
      box-shadow: none;
      opacity: 0.68;
    }
    .ak-pin.running .ak-crosshair,
    .ak-markers.is-running .ak-crosshair,
    .ak-markers.is-picking .ak-crosshair {
      pointer-events: none !important;
      cursor: default !important;
      border-color: #10b981;
      opacity: 0.85;
    }
    .ak-pin.is-active-step .ak-crosshair {
      border-color: #38bdf8 !important;
      background: rgba(2, 132, 199, 0.96) !important;
      transform: translate(-50%, -50%) scale(1.16) !important;
      opacity: 1 !important;
      animation: akActivePulse 0.55s ease-in-out infinite;
    }
    .ak-pin.locked .ak-crosshair {
      pointer-events: none !important;
      cursor: default !important;
      opacity: 0.78;
    }
    @keyframes akActivePulse {
      0% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.85), 0 0 12px rgba(56, 189, 248, 0.8); }
      70% { box-shadow: 0 0 0 11px rgba(56, 189, 248, 0), 0 0 16px rgba(16, 185, 129, 0.9); }
      100% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0), 0 0 12px rgba(56, 189, 248, 0.8); }
    }
    .ak-floating-runner-pill {
      position: fixed;
      right: 10px;
      bottom: 12px;
      z-index: 2147483647;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: rgba(9, 18, 36, 0.92);
      border: 1px solid #22d3ee;
      border-radius: 999px;
      padding: 3px 6px 3px 8px;
      color: #f8fafc;
      font-size: 10px;
      font-weight: 700;
      box-shadow: 0 3px 12px rgba(2, 6, 23, 0.7);
      pointer-events: auto !important;
      user-select: none;
      touch-action: none;
      cursor: grab;
    }
    .ak-floating-runner-pill.is-running {
      border-color: #f87171;
      background: rgba(30, 10, 18, 0.95);
      box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.35), 0 3px 12px rgba(2, 6, 23, 0.8);
    }
    .ak-floating-runner-meta {
      color: #bae6fd;
      font-size: 9px;
      font-weight: 700;
      letter-spacing: 0.2px;
      white-space: nowrap;
    }
    .ak-floating-runner-pill.is-running .ak-floating-runner-meta {
      color: #fecaca;
    }
    .ak-floating-runner-btn {
      border: 1px solid #10b981;
      background: #059669;
      color: #ffffff;
      border-radius: 999px;
      padding: 2px 8px;
      font-size: 10px;
      font-weight: 800;
      cursor: pointer;
      pointer-events: auto !important;
      white-space: nowrap;
      transition: transform 80ms ease, background 120ms ease;
    }
    .ak-floating-runner-btn:hover {
      transform: scale(1.03);
    }
    .ak-floating-runner-btn.is-stop {
      border-color: #f87171;
      background: #dc2626;
      color: #ffffff;
    }
    .ak-floating-sub-btn {
      border: 1px solid #38bdf8;
      background: #0284c7;
      color: #ffffff;
      border-radius: 999px;
      padding: 2px 6px;
      font-size: 9px;
      font-weight: 700;
      cursor: pointer;
      pointer-events: auto !important;
      white-space: nowrap;
    }
    .ak-floating-sub-btn:hover {
      background: #0369a1;
    }
  `;

  akSvgLayer = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  akSvgLayer.setAttribute('class', 'ak-svg');

  akMarkersLayer = document.createElement('div');
  akMarkersLayer.className = 'ak-markers';

  akFloatingPill = document.createElement('div');
  akFloatingPill.id = 'ak-floating-runner-pill';
  akFloatingPill.className = 'ak-floating-runner-pill';

  // Drag & Click handler untuk Popup Melayang AutoKlik Per-Tab
  let pillDragMoved = false;
  let pillStartX = 0;
  let pillStartY = 0;
  let pillOrigRight = 10;
  let pillOrigBottom = 12;

  const stopPillEvent = (e) => {
    const isBtn = e.target && e.target.closest && e.target.closest('button');
    if (isBtn) return;
    e.stopPropagation();
    if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
  };

  akFloatingPill.addEventListener('mousedown', (e) => {
    if (e.target && e.target.closest && e.target.closest('button')) return;
    stopPillEvent(e);
  }, true);
  akFloatingPill.addEventListener('mouseup', (e) => {
    if (e.target && e.target.closest && e.target.closest('button')) return;
    stopPillEvent(e);
  }, true);
  akFloatingPill.addEventListener('click', (e) => {
    if (e.target && e.target.closest && e.target.closest('button')) return;
    stopPillEvent(e);
  }, true);

  akFloatingPill.addEventListener('pointerdown', (e) => {
    if (e.button !== 0) return;
    const isBtn = e.target && e.target.closest && e.target.closest('button');
    stopPillEvent(e);
    if (isBtn) return;
    e.preventDefault();
    pillDragMoved = false;
    pillStartX = e.clientX;
    pillStartY = e.clientY;
    pillOrigRight = floatingPillPos.right;
    pillOrigBottom = floatingPillPos.bottom;

    const onPillMove = (me) => {
      me.preventDefault();
      stopPillEvent(me);
      const dx = me.clientX - pillStartX;
      const dy = me.clientY - pillStartY;
      if (Math.hypot(dx, dy) >= 3) pillDragMoved = true;
      if (!pillDragMoved) return;
      floatingPillPos.right = Math.max(6, Math.min((window.innerWidth || 1200) - 110, Math.round(pillOrigRight - dx)));
      floatingPillPos.bottom = Math.max(6, Math.min((window.innerHeight || 800) - 36, Math.round(pillOrigBottom - dy)));
      akFloatingPill.style.right = `${'$'}{floatingPillPos.right}px`;
      akFloatingPill.style.bottom = `${'$'}{floatingPillPos.bottom}px`;
    };

    const onPillUp = (ue) => {
      if (ue) {
        ue.preventDefault();
        stopPillEvent(ue);
      }
      window.removeEventListener('pointermove', onPillMove, true);
      window.removeEventListener('pointerup', onPillUp, true);
      window.removeEventListener('pointercancel', onPillUp, true);
    };

    window.addEventListener('pointermove', onPillMove, true);
    window.addEventListener('pointerup', onPillUp, true);
    window.addEventListener('pointercancel', onPillUp, true);
  }, true);

  akShadowRoot.appendChild(style);
  akShadowRoot.appendChild(akSvgLayer);
  akShadowRoot.appendChild(akMarkersLayer);
  akShadowRoot.appendChild(akFloatingPill);
  mountParent.appendChild(akShadowHost);
  return true;
}

function renderFloatingRunnerPill() {
  if (!ensureAutoKlikHost() || !akFloatingPill) return;
  const accId = (overlayState && overlayState.accountId) || accountId;
  const tIdx = (overlayState && overlayState.tabIndex) || 1;
  const isRunning = Boolean(overlayState && overlayState.runnerState === 'RUNNING');
  const ptsCount = Array.isArray(overlayState && overlayState.points)
    ? overlayState.points.filter((p) => p && p.enabled !== false).length
    : 0;
  const mirrorDisconnected = isRunning && Boolean(overlayState && overlayState.tabGroupMirrorEnabled);

  akFloatingPill.classList.toggle('is-running', isRunning);
  akFloatingPill.style.right = `${'$'}{floatingPillPos.right}px`;
  akFloatingPill.style.bottom = `${'$'}{floatingPillPos.bottom}px`;

  // Sesuai permintaan user: saat start fitur autoklik, popup melayang hanya menampilkan STOP (ukuran kecil & rapi)
  if (isRunning) {
    akFloatingPill.innerHTML = `
      <span class="ak-floating-runner-meta" title="A${'$'}{accId}•T${'$'}{tIdx} (Running)">A${'$'}{accId}•T${'$'}{tIdx}</span>
      <button id="ak-floating-runner-btn" type="button" class="ak-floating-runner-btn is-stop" title="Hentikan AutoKlik">■ Stop</button>
    `;
  } else {
    akFloatingPill.innerHTML = `
      <span class="ak-floating-runner-meta" title="A${'$'}{accId}•T${'$'}{tIdx} (${'$'}{ptsCount} Titik)">A${'$'}{accId}•T${'$'}{tIdx}</span>
      <button id="ak-floating-runner-btn" type="button" class="ak-floating-runner-btn" title="Jalankan AutoKlik">▶ Start</button>
      <button id="ak-floating-load-btn" type="button" class="ak-floating-sub-btn" title="Load / Copy / Paste Konfigurasi Titik">📂 Load</button>
    `;
  }

  const btn = akFloatingPill.querySelector('#ak-floating-runner-btn');
  if (btn) {
    const handleToggleRunner = (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
      ipcRenderer.send('autoklik-ext:floating-toggle-runner', {
        accountId: accId,
        tabId: (overlayState && overlayState.tabId) || null,
        tabIndex: tIdx
      });
    };
    btn.addEventListener('pointerdown', handleToggleRunner, true);
    btn.addEventListener('mousedown', handleToggleRunner, true);
    btn.addEventListener('mouseup', handleToggleRunner, true);
    btn.addEventListener('click', handleToggleRunner, true);
  }

  const loadBtn = akFloatingPill.querySelector('#ak-floating-load-btn');
  if (loadBtn) {
    const handleLoadMenu = async (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
      
      // Buat menu popover sederhana untuk Load, Copy, dan Paste di dalam tombol Load
      const choice = window.prompt(
        `Konfigurasi Titik Akun ${'$'}{accId} Tab ${'$'}{tIdx}:\n1. Ketik 'load' untuk Buka File JSON\n2. Ketik 'copy' untuk Salin Konfigurasi ke Clipboard\n3. Ketik 'paste' untuk Tempel Konfigurasi dari Clipboard`,
        'load'
      );
      if (!choice) return;
      const lower = choice.trim().toLowerCase();
      if (lower === 'load' || lower === '1') {
        const res = await ipcRenderer.invoke('autoklik-ext:open-dialog', accId, tIdx);
        if (res && res.ok) {
          window.location.reload();
        }
      } else if (lower === 'copy' || lower === '2') {
        await ipcRenderer.invoke('points:copy-tab', accId, tIdx);
        alert('Konfigurasi titik Tab ini berhasil disalin ke clipboard!');
      } else if (lower === 'paste' || lower === '3') {
        await ipcRenderer.invoke('points:paste-tab', accId, tIdx);
        window.location.reload();
      }
    };
    loadBtn.addEventListener('pointerdown', handleLoadMenu, true);
    loadBtn.addEventListener('mousedown', handleLoadMenu, true);
    loadBtn.addEventListener('mouseup', handleLoadMenu, true);
    loadBtn.addEventListener('click', handleLoadMenu, true);
  }
}

function detectSelectorAt(x, y) {
  return '';
}

function clampCoord(val, maxVal) {
  const upper = Math.max(20, (maxVal || 2000) - 4);
  return Math.max(4, Math.min(upper, Math.round(val)));
}

function getLayerViewportOffset() {
  if (!akMarkersLayer) return { left: 0, top: 0 };
  try {
    const r = akMarkersLayer.getBoundingClientRect();
    return {
      left: Number.isFinite(r.left) ? Math.round(r.left) : 0,
      top: Number.isFinite(r.top) ? Math.round(r.top) : 0
    };
  } catch (_) {
    return { left: 0, top: 0 };
  }
}

function passThroughClickToWebsiteAt(clientX, clientY) {
  const x = Math.round(Number(clientX) || 0);
  const y = Math.round(Number(clientY) || 0);
  const el = elementFromPointClean(x, y);
  if (!el || el.id === '__autoklik_v32_host__') return;
  const anchor = el.closest ? el.closest('a[target="_blank"]') : null;
  if (anchor) anchor.removeAttribute('target');
  try {
    if (typeof el.focus === 'function') el.focus({ preventScroll: true });
    const mouseInit = {
      bubbles: true,
      cancelable: true,
      composed: true,
      view: window,
      clientX: x,
      clientY: y,
      screenX: x,
      screenY: y,
      button: 0,
      buttons: 1
    };
    el.dispatchEvent(new PointerEvent('pointerdown', { ...mouseInit, pointerId: 1, isPrimary: true }));
    el.dispatchEvent(new MouseEvent('mousedown', mouseInit));
    el.dispatchEvent(new PointerEvent('pointerup', { ...mouseInit, buttons: 0, pointerId: 1, isPrimary: true }));
    el.dispatchEvent(new MouseEvent('mouseup', { ...mouseInit, buttons: 0 }));
    if (typeof el.click === 'function') {
      el.click();
    } else {
      el.dispatchEvent(new MouseEvent('click', { ...mouseInit, buttons: 0, detail: 1 }));
    }
  } catch (_) {
    try {
      if (typeof el.click === 'function') el.click();
    } catch (_2) {}
  }
  if (canEmitTab1MasterMirror({ isTrusted: true, target: el })) {
    const vw = Math.max(1, window.innerWidth || 1);
    const vh = Math.max(1, window.innerHeight || 1);
    ipcRenderer.send('tab1:mirror-action', {
      kind: 'click',
      x,
      y,
      rx: Number((x / vw).toFixed(5)),
      ry: Number((y / vh).toFixed(5)),
      selector: buildFastMirrorSelector(el) || ''
    });
  }
}

function attachDragHandlers(handleEl, pinEl, getInitialCoords, onLiveMove, onCommit, onEditPoint) {
  const stopLeak = (evt) => {
    evt.preventDefault();
    evt.stopPropagation();
    if (typeof evt.stopImmediatePropagation === 'function') {
      evt.stopImmediatePropagation();
    }
  };

  handleEl.addEventListener('mousedown', stopLeak, true);
  handleEl.addEventListener('mouseup', stopLeak, true);
  handleEl.addEventListener('click', stopLeak, true);

  handleEl.addEventListener('contextmenu', (ctxEvt) => {
    if (overlayState && overlayState.runnerState === 'RUNNING') return;
    stopLeak(ctxEvt);
    if (typeof onEditPoint === 'function') onEditPoint();
  });

  handleEl.addEventListener('pointerdown', (downEvt) => {
    if (downEvt.button !== 0 || (overlayState && overlayState.runnerState === 'RUNNING')) return;
    stopLeak(downEvt);

    try {
      if (handleEl.setPointerCapture) handleEl.setPointerCapture(downEvt.pointerId);
    } catch (_) {}

    const lockScrollX = window.scrollX || 0;
    const lockScrollY = window.scrollY || 0;
    const startClientX = downEvt.clientX;
    const startClientY = downEvt.clientY;
    const orig = getInitialCoords();
    const layerOffset = getLayerViewportOffset();
    let curX = orig.x;
    let curY = orig.y;
    let moved = false;
    isDraggingMarker = true;

    const onMove = (moveEvt) => {
      moveEvt.preventDefault();
      moveEvt.stopPropagation();
      if ((window.scrollX || 0) !== lockScrollX || (window.scrollY || 0) !== lockScrollY) {
        window.scrollTo(lockScrollX, lockScrollY);
      }
      const dx = moveEvt.clientX - startClientX;
      const dy = moveEvt.clientY - startClientY;
      if (Math.hypot(dx, dy) >= 3) moved = true;
      if (!moved) return;
      curX = clampCoord(orig.x + dx, window.innerWidth);
      curY = clampCoord(orig.y + dy, window.innerHeight);
      pinEl.style.left = `${'$'}{curX - layerOffset.left}px`;
      pinEl.style.top = `${'$'}{curY - layerOffset.top}px`;
      if (typeof onLiveMove === 'function') onLiveMove(curX, curY);
    };

    const finishDrag = (upEvt) => {
      if (upEvt) {
        upEvt.preventDefault();
        upEvt.stopPropagation();
      }
      window.removeEventListener('pointermove', onMove, true);
      window.removeEventListener('pointerup', finishDrag, true);
      window.removeEventListener('pointercancel', finishDrag, true);
      isDraggingMarker = false;
      if ((window.scrollX || 0) !== lockScrollX || (window.scrollY || 0) !== lockScrollY) {
        window.scrollTo(lockScrollX, lockScrollY);
      }
      if (moved && (curX !== orig.x || curY !== orig.y)) {
        onCommit(curX, curY);
      } else {
        // Kunci stabil tampilan website: saat Icon Titik diklik (!moved), jangan gerakkan/mengeklik website di bawahnya
        // agar konfigurasi titik klik tidak bergeser sendiri (kecuali user sengaja menahan tombol Alt).
        if (!moved && (!overlayState || overlayState.runnerState !== 'RUNNING')) {
          if (upEvt && upEvt.altKey) {
            passThroughClickToWebsiteAt(startClientX, startClientY);
          } else if (typeof onEditPoint === 'function') {
            onEditPoint();
          }
        }
        if (pendingOverlayState) {
          const queued = pendingOverlayState;
          pendingOverlayState = null;
          overlayState = queued;
          renderAutoKlikMarkers();
        }
      }
    };

    window.addEventListener('pointermove', onMove, true);
    window.addEventListener('pointerup', finishDrag, true);
    window.addEventListener('pointercancel', finishDrag, true);
  });
}

function requestEditPoint(pt) {
  if (overlayState && overlayState.runnerState === 'RUNNING') return;
  ipcRenderer.send('autoklik-ext:request-edit-point', {
    accountId: overlayState.accountId || accountId,
    tabIndex: overlayState.tabIndex || 1,
    pointId: pt.id
  });
}

function computeIconDiameterPx(pt) {
  const pct = Math.min(100, Math.max(15, Number(pt && pt.iconSizePct) || 25));
  return Math.round(20 + pct * 0.6);
}

let lastMarkersRenderSig = '';

function buildMarkersRenderSignature() {
  if (!overlayState) return 'empty';
  const ext = overlayState.extensionSettings || {};
  const pts = Array.isArray(overlayState.points) ? overlayState.points : [];
  const ptsSig = pts
    .map((p) => `${'$'}{p.id}:${'$'}{p.x},${'$'}{p.y},${'$'}{p.endX ?? ''},${'$'}{p.endY ?? ''},${'$'}{p.type}:${'$'}{p.enabled !== false ? 1 : 0}:${'$'}{p.iconSizePct || 25}:${'$'}{p.lineLengthPx || 350}:${'$'}{p.popupPinEnabled ? 1 : 0}:${'$'}{p.popupPinX ?? ''},${'$'}{p.popupPinY ?? ''}`)
    .join('|');
  return [
    overlayState.runnerState || 'IDLE',
    isPickerRunning ? 'pick1' : 'pick0',
    currentActivePointId || '',
    ext.markersHidden ? 'hid1' : 'hid0',
    ext.pointsLocked ? 'lck1' : 'lck0',
    ptsSig
  ].join('::');
}

function renderAutoKlikMarkers(force = false) {
  if (!ensureAutoKlikHost() || !akMarkersLayer || !akSvgLayer) return;
  renderFloatingRunnerPill();
  const nextSig = buildMarkersRenderSignature();
  if (!force && nextSig === lastMarkersRenderSig) {
    return;
  }
  lastMarkersRenderSig = nextSig;

  akMarkersLayer.innerHTML = '';
  akSvgLayer.innerHTML = '';

  const isRunning = overlayState.runnerState === 'RUNNING';
  akMarkersLayer.classList.toggle('is-running', isRunning);
  akMarkersLayer.classList.toggle('is-picking', isPickerRunning);

  const ext = overlayState.extensionSettings || {};
  if (ext.markersHidden === true) return;

  const layerOffset = getLayerViewportOffset();
  const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
  const marker = document.createElementNS('http://www.w3.org/2000/svg', 'marker');
  marker.setAttribute('id', 'ak-arrow-head');
  marker.setAttribute('markerWidth', '8');
  marker.setAttribute('markerHeight', '8');
  marker.setAttribute('refX', '6');
  marker.setAttribute('refY', '3');
  marker.setAttribute('orient', 'auto');
  const poly = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
  poly.setAttribute('points', '0 0, 6 3, 0 6');
  poly.setAttribute('fill', '#34d399');
  marker.appendChild(poly);
  defs.appendChild(marker);
  akSvgLayer.appendChild(defs);

  const pts = Array.isArray(overlayState.points) ? overlayState.points : [];
  const isLocked = ext.pointsLocked === true;

  pts.forEach((pt, idx) => {
    const x = Math.round(Number(pt.x) || 0);
    const y = Math.round(Number(pt.y) || 0);
    const type = pt.type || 'tap';
    const isEnabled = pt.enabled !== false;
    const isSwipe = type === 'swipe_up' || type === 'swipe_down' || type === 'swipe';
    const lineLen = Math.max(40, Number(pt.lineLengthPx) || 350);
    const defaultEndY = type === 'swipe_down' ? (y + lineLen) : Math.max(12, y - lineLen);
    const endX = isSwipe && Number.isFinite(Number(pt.endX)) ? Math.round(Number(pt.endX)) : x;
    const endY = isSwipe && Number.isFinite(Number(pt.endY)) ? Math.round(Number(pt.endY)) : defaultEndY;

    let swipeLine = null;
    if (isSwipe) {
      swipeLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      swipeLine.setAttribute('x1', String(x - layerOffset.left));
      swipeLine.setAttribute('y1', String(y - layerOffset.top));
      swipeLine.setAttribute('x2', String(endX - layerOffset.left));
      swipeLine.setAttribute('y2', String(endY - layerOffset.top));
      swipeLine.setAttribute('stroke', isEnabled ? '#34d399' : '#64748b');
      swipeLine.setAttribute('stroke-width', '2.5');
      swipeLine.setAttribute('stroke-dasharray', '5,3');
      swipeLine.setAttribute('marker-end', 'url(#ak-arrow-head)');
      akSvgLayer.appendChild(swipeLine);
    }

    const diameter = computeIconDiameterPx(pt);
    const fontSize = Math.max(10, Math.round(diameter * 0.42));

    const pin = document.createElement('div');
    const isActiveStep = isRunning && isEnabled && currentActivePointId && currentActivePointId === pt.id;
    pin.className = `ak-pin type-${'$'}{type} ${'$'}{isEnabled ? '' : 'disabled'} ${'$'}{isLocked ? 'locked' : ''} ${'$'}{isRunning && isEnabled ? 'running' : ''} ${'$'}{isActiveStep ? 'is-active-step' : ''}`;
    pin.dataset.pointId = pt.id || '';
    pin.style.left = `${'$'}{x - layerOffset.left}px`;
    pin.style.top = `${'$'}{y - layerOffset.top}px`;
    pin.style.setProperty('--ak-size', `${'$'}{diameter}px`);
    pin.style.setProperty('--ak-font', `${'$'}{fontSize}px`);

    const cross = document.createElement('div');
    cross.className = 'ak-crosshair';
    const numSpan = document.createElement('span');
    numSpan.className = 'ak-pin-num';
    numSpan.textContent = String(idx + 1);
    cross.appendChild(numSpan);
    cross.title = isRunning
      ? `Titik #${'$'}{idx + 1} (x=${'$'}{x}, y=${'$'}{y}) sedang berjalan`
      : isLocked
        ? `Titik #${'$'}{idx + 1} (x=${'$'}{x}, y=${'$'}{y}) dikunci (Klik tembus ke website)`
        : `Titik #${'$'}{idx + 1} (x=${'$'}{x}, y=${'$'}{y} • Diameter Bulat Penuh ${'$'}{diameter}px) • Seret: Pindah Posisi • Klik: Atur Titik (Website Tetap Diam)`;

    if (!isRunning && !isLocked && !isPickerRunning) {
      cross.addEventListener('dblclick', (e) => {
        e.preventDefault();
        e.stopPropagation();
        requestEditPoint(pt);
      });

      const onLiveStart = (nx, ny) => {
        if (swipeLine) {
          const curLen = Math.max(40, Math.abs(endY - y));
          const nextEy = type === 'swipe_down' ? (ny + curLen) : Math.max(12, ny - curLen);
          swipeLine.setAttribute('x1', String(nx - layerOffset.left));
          swipeLine.setAttribute('y1', String(ny - layerOffset.top));
          swipeLine.setAttribute('x2', String(nx - layerOffset.left));
          swipeLine.setAttribute('y2', String(nextEy - layerOffset.top));
        }
      };
      const onCommitStart = (nx, ny) => {
        pt.x = nx;
        pt.y = ny;
        pt.selector = '';
        ipcRenderer.send('autoklik-ext:page-drag-point', {
          accountId: overlayState.accountId || accountId,
          tabIndex: overlayState.tabIndex || 1,
          pointId: pt.id,
          x: nx,
          y: ny,
          selector: ''
        });
      };

      attachDragHandlers(
        cross,
        pin,
        () => ({ x: Number(pt.x) || 0, y: Number(pt.y) || 0 }),
        onLiveStart,
        onCommitStart,
        () => requestEditPoint(pt)
      );
    }

    pin.appendChild(cross);
    akMarkersLayer.appendChild(pin);

    // Swipe end directional handle (angka/panah saja tanpa teks tulisan)
    if (isSwipe) {
      const endPin = document.createElement('div');
      endPin.className = `ak-pin type-${'$'}{type} swipe-end ${'$'}{isEnabled ? '' : 'disabled'} ${'$'}{isLocked ? 'locked' : ''} ${'$'}{isRunning && isEnabled ? 'running' : ''}`;
      endPin.style.left = `${'$'}{endX - layerOffset.left}px`;
      endPin.style.top = `${'$'}{endY - layerOffset.top}px`;

      const endCross = document.createElement('div');
      endCross.className = 'ak-crosshair';
      endCross.textContent = endY > y ? '▼' : '▲';
      endCross.title = `Ujung arah Swipe #${'$'}{idx + 1} (x=${'$'}{endX}, y=${'$'}{endY})`;

      if (!isRunning && !isLocked && !isPickerRunning) {
        const onLiveEnd = (nx, ny) => {
          if (swipeLine) {
            swipeLine.setAttribute('x2', String(nx - layerOffset.left));
            swipeLine.setAttribute('y2', String(ny - layerOffset.top));
          }
          endCross.textContent = ny > (Number(pt.y) || y) ? '▼' : '▲';
        };
        const onCommitEnd = (nx, ny) => {
          pt.endX = nx;
          pt.endY = ny;
          ipcRenderer.send('autoklik-ext:page-drag-point', {
            accountId: overlayState.accountId || accountId,
            tabIndex: overlayState.tabIndex || 1,
            pointId: pt.id,
            isSwipeEnd: true,
            endX: nx,
            endY: ny
          });
        };
        attachDragHandlers(
          endCross,
          endPin,
          () => ({ x: Number(pt.endX ?? endX), y: Number(pt.endY ?? endY) }),
          onLiveEnd,
          onCommitEnd,
          () => requestEditPoint(pt)
        );
      }

      endPin.appendChild(endCross);
      akMarkersLayer.appendChild(endPin);
    }

    // Pin Ungu Autofill untuk Input Ketik (+Popup)
    if (type === 'input' && pt.popupPinEnabled) {
      const pinX = Number.isFinite(Number(pt.popupPinX)) ? Math.round(Number(pt.popupPinX)) : (x + 56);
      const pinY = Number.isFinite(Number(pt.popupPinY)) ? Math.round(Number(pt.popupPinY)) : (y + 42);
      const popPin = document.createElement('div');
      popPin.className = `ak-pin type-autofill-pin ${'$'}{isEnabled ? '' : 'disabled'} ${'$'}{isLocked ? 'locked' : ''} ${'$'}{isRunning && isEnabled ? 'running' : ''}`;
      popPin.dataset.popupPinFor = pt.id || '';
      popPin.style.left = `${'$'}{pinX - layerOffset.left}px`;
      popPin.style.top = `${'$'}{pinY - layerOffset.top}px`;

      const popCross = document.createElement('div');
      popCross.className = 'ak-crosshair';
      const popNum = document.createElement('span');
      popNum.className = 'ak-pin-num';
      popNum.textContent = `#${'$'}{idx + 1}`;
      popCross.appendChild(popNum);
      popCross.title = `Pin Ungu Autofill Titik #${'$'}{idx + 1} (x=${'$'}{pinX}, y=${'$'}{pinY}) • Titik tengah kuning (+) = Posisi Klik Persis`;

      if (!isRunning && !isLocked && !isPickerRunning) {
        const onCommitPop = (nx, ny) => {
          pt.popupPinX = nx;
          pt.popupPinY = ny;
          ipcRenderer.send('autoklik-ext:page-drag-point', {
            accountId: overlayState.accountId || accountId,
            tabIndex: overlayState.tabIndex || 1,
            pointId: pt.id,
            isPopupPin: true,
            popupPinX: nx,
            popupPinY: ny
          });
        };
        attachDragHandlers(
          popCross,
          popPin,
          () => ({ x: Number(pt.popupPinX ?? pinX), y: Number(pt.popupPinY ?? pinY) }),
          null,
          onCommitPop,
          () => requestEditPoint(pt)
        );
      }

      popPin.appendChild(popCross);
      akMarkersLayer.appendChild(popPin);
    }
  });
}

function renderAutoKlikOverlay() {
  renderAutoKlikMarkers();
}

let lastNativeDomClickAt = 0;
let lastNativeClickInfo = { at: 0, x: -999, y: -999 };
window.addEventListener('click', (evt) => {
  lastNativeDomClickAt = Date.now();
  lastNativeClickInfo = {
    at: lastNativeDomClickAt,
    x: Number(evt.clientX) || 0,
    y: Number(evt.clientY) || 0
  };
  // Cegah link <a target="_blank"> membuka tab liar
  const anchor = evt.target && evt.target.closest ? evt.target.closest('a[target="_blank"]') : null;
  if (anchor) {
    anchor.removeAttribute('target');
  }
}, true);

function showClickRippleAt(x, y, diameterPx = 35) {
  if (!ensureAutoKlikHost() || !akMarkersLayer) return;
  try {
    const layerOffset = getLayerViewportOffset();
    const d = Math.max(18, Math.round(Number(diameterPx) || 35));
    const ripple = document.createElement('div');
    ripple.className = 'ak-click-ripple';
    ripple.style.width = `${'$'}{d}px`;
    ripple.style.height = `${'$'}{d}px`;
    ripple.style.left = `${'$'}{Math.round(x) - layerOffset.left}px`;
    ripple.style.top = `${'$'}{Math.round(y) - layerOffset.top}px`;
    akMarkersLayer.appendChild(ripple);
    setTimeout(() => {
      try { ripple.remove(); } catch (_) {}
    }, 360);
  } catch (_) {}
}

function clearPageTextSelection() {
  try {
    const sel = window.getSelection && window.getSelection();
    if (sel && sel.rangeCount > 0) {
      sel.removeAllRanges();
    }
  } catch (_) {}
}

function setRunnerNoSelectLock(running) {
  try {
    let styleEl = document.getElementById('__autoklik_noselect_style__');
    if (running) {
      if (!styleEl && (document.head || document.documentElement)) {
        styleEl = document.createElement('style');
        styleEl.id = '__autoklik_noselect_style__';
        styleEl.textContent = `
          html.__autoklik_running__, html.__autoklik_running__ * {
            user-select: none !important;
            -webkit-user-select: none !important;
          }
          html.__autoklik_running__ ::selection {
            background: transparent !important;
          }
        `;
        (document.head || document.documentElement).appendChild(styleEl);
      }
      if (document.documentElement) {
        document.documentElement.classList.add('__autoklik_running__');
      }
      clearPageTextSelection();
    } else if (document.documentElement) {
      document.documentElement.classList.remove('__autoklik_running__');
    }
  } catch (_) {}
}

function elementFromPointClean(x, y) {
  let prevDisplay = null;
  if (akShadowHost) {
    prevDisplay = akShadowHost.style.display;
    akShadowHost.style.display = 'none';
  }
  try {
    return document.elementFromPoint(x, y);
  } finally {
    if (akShadowHost) {
      akShadowHost.style.display = prevDisplay || '';
    }
  }
}

function findInteractiveElementInCircle(x, y, radiusPx = 18) {
  const rad = Math.max(8, Math.round(Number(radiusPx) || 18));
  const interactiveSel = 'button, a, input:not([type="hidden"]), select, textarea, [role="button"], [role="option"], [role="combobox"], [role="menuitem"], label';
  const hit = elementFromPointClean(x, y);
  if (hit && hit.id !== '__autoklik_v32_host__') {
    const direct = hit.closest ? hit.closest(interactiveSel) : null;
    if (direct) return direct;
  }
  const candidates = document.querySelectorAll(interactiveSel);
  let best = null;
  let bestDist = rad + 1;
  for (const cand of candidates) {
    const r = cand.getBoundingClientRect();
    if (!r || r.width <= 0 || r.height <= 0) continue;
    const nearX = Math.max(r.left, Math.min(r.right, x));
    const nearY = Math.max(r.top, Math.min(r.bottom, y));
    const dist = Math.hypot(nearX - x, nearY - y);
    if (dist <= rad && dist < bestDist) {
      bestDist = dist;
      best = cand;
    }
  }
  return best || hit;
}

function findEditableElementAt(x, y, radiusPx = 18) {
  if (Number.isFinite(x) && Number.isFinite(y)) {
    const hit = elementFromPointClean(x, y);
    if (hit) {
      if (hit.tagName === 'INPUT' || hit.tagName === 'TEXTAREA' || hit.isContentEditable) return hit;
      const closest = hit.closest && hit.closest('input, textarea, [contenteditable="true"]');
      if (closest) return closest;
      const lbl = hit.closest && hit.closest('label');
      if (lbl && lbl.control && (lbl.control.tagName === 'INPUT' || lbl.control.tagName === 'TEXTAREA')) {
        return lbl.control;
      }
    }
    const candidates = document.querySelectorAll('input:not([type="hidden"]), textarea, [contenteditable="true"]');
    let best = null;
    let bestDist = Math.max(14, Math.round(Number(radiusPx) || 18));
    for (const cand of candidates) {
      const r = cand.getBoundingClientRect();
      if (!r || r.width <= 0 || r.height <= 0) continue;
      if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) {
        return cand;
      }
      const dx = x < r.left ? (r.left - x) : (x > r.right ? (x - r.right) : 0);
      const dy = y < r.top ? (r.top - y) : (y > r.bottom ? (y - r.bottom) : 0);
      const d = Math.hypot(dx, dy);
      if (d <= bestDist) {
        bestDist = d;
        best = cand;
      }
    }
    if (best) return best;
  }
  const active = document.activeElement;
  if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable)) {
    return active;
  }
  return null;
}

function focusInputCaretAtEnd(el) {
  if (!el) return;
  try {
    if (typeof el.focus === 'function') {
      el.focus({ preventScroll: true });
    }
    const val = el.isContentEditable ? String(el.textContent || '') : String(el.value || '');
    if (typeof el.setSelectionRange === 'function') {
      try { el.setSelectionRange(val.length, val.length); } catch (_) {}
    }
  } catch (_) {}
}

ipcRenderer.on('autoklik-ext:runner-active', (_evt, payload) => {
  const running = Boolean(payload && payload.running);
  if (!running) {
    currentActivePointId = null;
  } else if (payload && payload.activePointId) {
    currentActivePointId = payload.activePointId;
  }
  setRunnerNoSelectLock(running);
  if (overlayState) {
    overlayState.runnerState = running ? 'RUNNING' : 'IDLE';
  }
  if (akMarkersLayer) {
    akMarkersLayer.classList.toggle('is-running', running);
  }
  renderAutoKlikMarkers();
});

ipcRenderer.on('autoklik-ext:active-point', (_evt, payload) => {
  currentActivePointId = (payload && payload.pointId) || null;
  if (!akMarkersLayer) return;
  akMarkersLayer.querySelectorAll('.ak-pin').forEach((pinEl) => {
    const pid = pinEl.dataset ? pinEl.dataset.pointId : '';
    pinEl.classList.toggle('is-active-step', Boolean(currentActivePointId && pid === currentActivePointId));
  });
});

ipcRenderer.on('autoklik-ext:dom-tap', (_evt, payload) => {
  if (!payload) return;
  clearPageTextSelection();
  const x = Math.round(Number(payload.x) || 0);
  const y = Math.round(Number(payload.y) || 0);
  const iconDiameter = Math.max(18, Math.round(Number(payload.iconDiameter) || 35));
  const iconRadius = Math.max(9, Math.round(Number(payload.iconRadius) || Math.round(iconDiameter / 2)));
  showClickRippleAt(x, y, iconDiameter);
  setTimeout(() => {
    clearPageTextSelection();
    const el = findInteractiveElementInCircle(x, y, iconRadius);
    if (!el || el.id === '__autoklik_v32_host__') return;
    const anchor = el.closest ? el.closest('a[target="_blank"]') : null;
    if (anchor) anchor.removeAttribute('target');
    // Verifikasi apakah klik native sudah mengenai area bulat penuh Pin Ikon (toleransi <= iconRadius)
    const recentEnough = Date.now() - lastNativeClickInfo.at <= 45;
    const exactMatch = recentEnough && Math.hypot(lastNativeClickInfo.x - x, lastNativeClickInfo.y - y) <= 4;
    if (!exactMatch) {
      try {
        if (typeof el.focus === 'function') el.focus({ preventScroll: true });
        const mouseInit = {
          bubbles: true,
          cancelable: true,
          composed: true,
          view: window,
          clientX: x,
          clientY: y,
          screenX: x,
          screenY: y,
          button: 0,
          buttons: 1
        };
        el.dispatchEvent(new PointerEvent('pointerdown', { ...mouseInit, pointerId: 1, isPrimary: true }));
        el.dispatchEvent(new MouseEvent('mousedown', mouseInit));
        el.dispatchEvent(new PointerEvent('pointerup', { ...mouseInit, buttons: 0, pointerId: 1, isPrimary: true }));
        el.dispatchEvent(new MouseEvent('mouseup', { ...mouseInit, buttons: 0 }));
        el.dispatchEvent(new MouseEvent('click', { ...mouseInit, buttons: 0, detail: 1 }));
      } catch (_) {
        try {
          if (typeof el.click === 'function') el.click();
        } catch (_) {}
      }
    }
    clearPageTextSelection();
  }, 14);
});

ipcRenderer.on('autoklik-ext:dom-scroll', (_evt, payload) => {
  if (!payload) return;
  clearPageTextSelection();
  const x = Math.round(Number(payload.x) || 0);
  const y = Math.round(Number(payload.y) || 0);
  const dist = Math.max(40, Math.round(Number(payload.distancePx) || 350));
  const dur = Math.max(160, Math.round(Number(payload.durationMs) || 340));
  // swipe_up (direction === 'up') -> scroll halaman ke bawah (+dist)
  // swipe_down (direction === 'down') -> scroll halaman ke atas (-dist)
  const totalDeltaTop = payload.direction === 'down' ? -dist : dist;

  let scrollTarget = null;
  let cur = elementFromPointClean(x, y);
  while (cur && cur !== document.body && cur !== document.documentElement) {
    try {
      const st = window.getComputedStyle(cur);
      const oy = st ? st.overflowY : '';
      if ((oy === 'auto' || oy === 'scroll' || oy === 'overlay') && cur.scrollHeight > cur.clientHeight + 4) {
        scrollTarget = cur;
        break;
      }
    } catch (_) {}
    cur = cur.parentElement;
  }
  const rootScroller = document.scrollingElement || document.documentElement || document.body;
  const target = scrollTarget || rootScroller;
  if (!target) return;

  // Kurva Ease-In-Out Sinusoidal agar gerakan scroll mulai halus, cepat di tengah, dan melambat halus di akhir
  const steps = Math.max(8, Math.min(24, Math.round(dur / 16)));
  const weights = [];
  let weightSum = 0;
  for (let i = 0; i < steps; i++) {
    const w = Math.sin(((i + 0.5) / steps) * Math.PI);
    weights.push(w);
    weightSum += w;
  }

  let stepIdx = 0;
  const timer = setInterval(() => {
    const w = weights[stepIdx] || (1 / steps);
    const stepDelta = (totalDeltaTop * w) / (weightSum || 1);
    stepIdx += 1;
    try {
      if (target === rootScroller) {
        window.scrollBy(0, stepDelta);
      } else {
        target.scrollTop += stepDelta;
      }
    } catch (_) {}
    if (stepIdx >= steps) {
      clearInterval(timer);
    }
  }, 16);
});

ipcRenderer.on('autoklik-ext:input-helper', (_evt, payload) => {
  if (!payload || !payload.action) return;
  clearPageTextSelection();
  const el = findEditableElementAt(Number(payload.x), Number(payload.y), Number(payload.iconRadius) || 18);
  if (!el) return;
  // Hanya memfokuskan kursor ke ujung teks kotak input tanpa melakukan paste (100% murni ketikan keyboard)
  if (payload.action === 'focus') {
    focusInputCaretAtEnd(el);
  }
});

ipcRenderer.on('autoklik-ext:overlay-state', (_evt, nextState) => {
  if (!nextState) return;
  setRunnerNoSelectLock(nextState.runnerState === 'RUNNING');
  if (isDraggingMarker) {
    pendingOverlayState = nextState;
    return;
  }
  overlayState = nextState;
  renderAutoKlikOverlay();
});

ipcRenderer.on('autoklik-ext:start-picker', (_evt, cfg) => {
  isPickerRunning = true;
  renderAutoKlikOverlay();
  startTargetPicker(cfg || {});
});

ipcRenderer.on('autoklik-ext:stop-picker', () => {
  stopTargetPicker();
  isPickerRunning = false;
  renderAutoKlikOverlay();
});

// ============================================================================
// MULTI-TAB COLLECTION MIRRORING V5.8 (Kumpulan Tab 1, Tab 2 & Tab 3)
// 0ms Instant Lockstep: Klik, Popup Pilihan, Wheel/Swipe, Scroll, Ketik & Dropdown.
// ATURAN WAJIB V5.8: Otomatis PUTUS MIRROR untuk tab yang sedang Start AutoKlik!
// ============================================================================
let isApplyingMirroredEvent = false;
let suppressMirrorEmitUntil = 0;

function canEmitTab1MasterMirror(event) {
  if (isApplyingMirroredEvent || isPickerRunning || isDraggingMarker) return false;
  if (performance.now() < suppressMirrorEmitUntil) return false;
  if (event && event.isTrusted === false) return false;
  if (!overlayState) return false;
  // ATURAN WAJIB V5.8: Saat tab sedang Start AutoKlik, otomatis putus dari Mirror
  if (overlayState.runnerState === 'RUNNING' || overlayState.isMirrorDisconnectedByAutoKlik === true) {
    return false;
  }
  const isGroupMirrorActive =
    overlayState.tabGroupMirrorActive === true ||
    (Number(overlayState.tabIndex) === 1 && overlayState.tab1MirrorEnabled === true && overlayState.tab1MirrorActive === true);
  if (!isGroupMirrorActive) return false;
  const t = event && event.target;
  if (t && t.closest && (t.closest('#__autoklik_v32_host__') || t.closest('#ak-floating-runner-pill'))) return false;
  return true;
}

function buildFastMirrorSelector(el) {
  if (!el || el.nodeType !== 1) return '';
  try {
    const actionable = el.closest
      ? (el.closest('button, a, input, select, textarea, [role="button"], [role="option"], [role="combobox"], [role="menuitem"], .select2-results__option, .dropdown-item, [data-testid], label') || el)
      : el;
    if (actionable.id) return `#${'$'}{CSS.escape(actionable.id)}`;
    const testId = actionable.getAttribute && actionable.getAttribute('data-testid');
    if (testId) return `[data-testid="${'$'}{CSS.escape(testId)}"]`;
    if (actionable.name && actionable.tagName) {
      return `${'$'}{actionable.tagName.toLowerCase()}[name="${'$'}{CSS.escape(actionable.name)}"]`;
    }
    const href = actionable.getAttribute && actionable.getAttribute('href');
    if (href && href.length > 1 && href.length < 90 && !href.startsWith('javascript:')) {
      return `a[href="${'$'}{CSS.escape(href)}"]`;
    }
    const parts = [];
    let cur = actionable;
    let anchoredById = false;
    for (let depth = 0; depth < 4 && cur && cur.nodeType === 1 && cur !== document.body; depth++) {
      if (cur.id) {
        parts.unshift(`#${'$'}{CSS.escape(cur.id)}`);
        anchoredById = true;
        break;
      }
      const tag = cur.tagName.toLowerCase();
      let nth = 1;
      let sib = cur.previousElementSibling;
      while (sib) {
        if (sib.tagName === cur.tagName) nth++;
        sib = sib.previousElementSibling;
      }
      parts.unshift(`${'$'}{tag}:nth-of-type(${'$'}{nth})`);
      cur = cur.parentElement;
    }
    return anchoredById ? parts.join(' > ') : '';
  } catch (_) {
    return '';
  }
}

const POPUP_OPTION_SELECTOR = '[role="option"], [role="menuitem"], [role="listbox"] li, .select2-results__option, .dropdown-menu li, .dropdown-item, .flatpickr-day, .datepicker td, .modal button, .swal2-container button, option, li, button';

function extractOptionText(el) {
  if (!el || el.nodeType !== 1) return '';
  try {
    const opt = el.closest
      ? el.closest(POPUP_OPTION_SELECTOR)
      : null;
    const target = opt || el;
    const txt = String(target.innerText || target.textContent || target.getAttribute('aria-label') || '').replace(/\s+/g, ' ').trim();
    return txt.length > 0 && txt.length <= 80 ? txt : '';
  } catch (_) {
    return '';
  }
}

function extractPopupMeta(el) {
  if (!el || el.nodeType !== 1) return { optionText: '', optionValue: '', isPopupOption: false };
  try {
    const opt = el.closest ? el.closest(POPUP_OPTION_SELECTOR) : null;
    const target = opt || el;
    const optionText = extractOptionText(target);
    const optionValue = String(
      (target.getAttribute && (target.getAttribute('data-value') || target.getAttribute('value'))) ||
      (target.value !== undefined ? target.value : '') ||
      ''
    ).trim();
    const inPopupContainer = Boolean(
      target.closest &&
      target.closest('[role="listbox"], [role="menu"], [role="dialog"], .select2-container, .select2-dropdown, .dropdown-menu, .modal, .swal2-container, .flatpickr-calendar, .datepicker')
    );
    return {
      optionText,
      optionValue,
      isPopupOption: Boolean(opt || inPopupContainer)
    };
  } catch (_) {
    return { optionText: '', optionValue: '', isPopupOption: false };
  }
}

function findInnerScrollableContainer(startEl) {
  let cur = startEl;
  while (cur && cur.nodeType === 1 && cur !== document.body && cur !== document.documentElement) {
    try {
      const st = window.getComputedStyle(cur);
      const oy = st ? st.overflowY : '';
      if ((oy === 'auto' || oy === 'scroll' || oy === 'overlay') && cur.scrollHeight > cur.clientHeight + 2) {
        return cur;
      }
    } catch (_) {}
    cur = cur.parentElement;
  }
  return null;
}

function resolveMirroredViewportPoint(payload) {
  const vw = Math.max(1, window.innerWidth || document.documentElement.clientWidth || 1);
  const vh = Math.max(1, window.innerHeight || document.documentElement.clientHeight || 1);
  if (Number.isFinite(Number(payload.rx)) && Number.isFinite(Number(payload.ry))) {
    return {
      x: Math.max(0, Math.round(Number(payload.rx) * vw)),
      y: Math.max(0, Math.round(Number(payload.ry) * vh))
    };
  }
  return {
    x: Math.max(0, Math.round(Number(payload.x) || 0)),
    y: Math.max(0, Math.round(Number(payload.y) || 0))
  };
}

function setNativeControlValue(el, nextValue) {
  if (!el) return;
  const valStr = String(nextValue ?? '');
  try {
    if (el.isContentEditable) {
      el.textContent = valStr;
      return;
    }
    const proto = el.tagName === 'TEXTAREA'
      ? window.HTMLTextAreaElement && window.HTMLTextAreaElement.prototype
      : window.HTMLInputElement && window.HTMLInputElement.prototype;
    const desc = proto ? Object.getOwnPropertyDescriptor(proto, 'value') : null;
    if (desc && typeof desc.set === 'function') {
      desc.set.call(el, valStr);
    } else {
      el.value = valStr;
    }
  } catch (_) {
    try { el.value = valStr; } catch (_2) {}
  }
}

// 1) Mirror pointerdown instan 0ms agar dropdown / combobox / popup pilihan langsung terbuka & merespon berbarengan
window.addEventListener('pointerdown', (e) => {
  if (e.button !== 0 || !canEmitTab1MasterMirror(e)) return;
  const vw = Math.max(1, window.innerWidth || 1);
  const vh = Math.max(1, window.innerHeight || 1);
  const popMeta = extractPopupMeta(e.target);
  ipcRenderer.send('tab1:mirror-action', {
    kind: 'pointerdown',
    tabIndex: (overlayState && overlayState.tabIndex) || 1,
    x: Math.round(e.clientX),
    y: Math.round(e.clientY),
    rx: Number((e.clientX / vw).toFixed(5)),
    ry: Number((e.clientY / vh).toFixed(5)),
    selector: buildFastMirrorSelector(e.target) || '',
    optionText: popMeta.optionText,
    optionValue: popMeta.optionValue,
    isPopupOption: popMeta.isPopupOption
  });
}, true);

// 2) Mirror click instan 0ms (dengan metadata popup pilihan agar di tab lain tidak menembus popup)
window.addEventListener('click', (e) => {
  if (!canEmitTab1MasterMirror(e)) return;
  const vw = Math.max(1, window.innerWidth || 1);
  const vh = Math.max(1, window.innerHeight || 1);
  const popMeta = extractPopupMeta(e.target);
  ipcRenderer.send('tab1:mirror-action', {
    kind: 'click',
    tabIndex: (overlayState && overlayState.tabIndex) || 1,
    x: Math.round(e.clientX),
    y: Math.round(e.clientY),
    rx: Number((e.clientX / vw).toFixed(5)),
    ry: Number((e.clientY / vh).toFixed(5)),
    selector: buildFastMirrorSelector(e.target) || '',
    optionText: popMeta.optionText,
    optionValue: popMeta.optionValue,
    isPopupOption: popMeta.isPopupOption
  });
}, true);

// 3) Mirror wheel/swipe instan 0ms (menghilangkan delay swipe antar tab & menyinkronkan scroll list popup)
window.addEventListener('wheel', (e) => {
  if (!canEmitTab1MasterMirror(e)) return;
  const vw = Math.max(1, window.innerWidth || 1);
  const vh = Math.max(1, window.innerHeight || 1);
  const scroller = findInnerScrollableContainer(e.target);
  ipcRenderer.send('tab1:mirror-action', {
    kind: 'wheel',
    tabIndex: (overlayState && overlayState.tabIndex) || 1,
    x: Math.round(e.clientX),
    y: Math.round(e.clientY),
    rx: Number((e.clientX / vw).toFixed(5)),
    ry: Number((e.clientY / vh).toFixed(5)),
    deltaX: Math.round(e.deltaX || 0),
    deltaY: Math.round(e.deltaY || 0),
    scrollSelector: scroller ? buildFastMirrorSelector(scroller) : ''
  });
}, { capture: true, passive: true });

// 4) Mirror scroll instan 0ms (tanpa jeda requestAnimationFrame)
window.addEventListener('scroll', (e) => {
  if (!canEmitTab1MasterMirror(e)) return;
  const target = e.target;
  const isDoc = !target || target === document || target === document.documentElement || target === document.body;
  let scrollSelector = '';
  let elScrollTop = 0;
  let elScrollLeft = 0;
  if (!isDoc && target && target.nodeType === 1) {
    elScrollTop = Math.round(target.scrollTop || 0);
    elScrollLeft = Math.round(target.scrollLeft || 0);
    scrollSelector = buildFastMirrorSelector(target);
  }
  const vh = Math.max(1, window.innerHeight || 1);
  const docEl = document.scrollingElement || document.documentElement || document.body;
  const maxScrollY = Math.max(1, (docEl ? docEl.scrollHeight : vh) - vh);
  const curScrollY = Math.round(window.scrollY || 0);
  ipcRenderer.send('tab1:mirror-action', {
    kind: 'scroll',
    tabIndex: (overlayState && overlayState.tabIndex) || 1,
    scrollX: Math.round(window.scrollX || 0),
    scrollY: curScrollY,
    scrollRatioY: Number((curScrollY / maxScrollY).toFixed(5)),
    scrollSelector,
    scrollTop: elScrollTop,
    scrollLeft: elScrollLeft
  });
}, { capture: true, passive: true });

// 5) Mirror keydown instan 0ms
window.addEventListener('keydown', (e) => {
  if (!canEmitTab1MasterMirror(e)) return;
  const specialKeys = ['Enter', 'Escape', 'Tab', 'ArrowDown', 'ArrowUp', 'Backspace'];
  if (!specialKeys.includes(e.key)) return;
  ipcRenderer.send('tab1:mirror-action', {
    kind: 'keydown',
    tabIndex: (overlayState && overlayState.tabIndex) || 1,
    keyCode: e.key,
    selector: buildFastMirrorSelector(e.target)
  });
}, true);

function buildInputOrChangePayload(el, kindName) {
  if (!el || el.nodeType !== 1) return null;
  const rect = el.getBoundingClientRect ? el.getBoundingClientRect() : null;
  const cx = rect ? Math.round(rect.left + rect.width / 2) : 0;
  const cy = rect ? Math.round(rect.top + rect.height / 2) : 0;
  const vw = Math.max(1, window.innerWidth || 1);
  const vh = Math.max(1, window.innerHeight || 1);
  return {
    kind: kindName,
    tabIndex: (overlayState && overlayState.tabIndex) || 1,
    x: cx,
    y: cy,
    rx: Number((cx / vw).toFixed(5)),
    ry: Number((cy / vh).toFixed(5)),
    selector: buildFastMirrorSelector(el),
    tagName: el.tagName || '',
    inputType: el.type || '',
    value: el.isContentEditable ? String(el.textContent || '') : String(el.value ?? ''),
    checked: Boolean(el.checked),
    selectedIndex: typeof el.selectedIndex === 'number' ? el.selectedIndex : -1
  };
}

// 6) Mirror input & change (termasuk dropdown <select>) instan 0ms
window.addEventListener('input', (e) => {
  if (!canEmitTab1MasterMirror(e)) return;
  const payload = buildInputOrChangePayload(e.target, 'input');
  if (payload) ipcRenderer.send('tab1:mirror-action', payload);
}, true);

window.addEventListener('change', (e) => {
  if (!canEmitTab1MasterMirror(e)) return;
  const payload = buildInputOrChangePayload(e.target, 'change');
  if (payload) ipcRenderer.send('tab1:mirror-action', payload);
}, true);

function getTopmostElementAtClean(x, y) {
  let prevDisplay = null;
  if (akShadowHost) {
    prevDisplay = akShadowHost.style.display;
    akShadowHost.style.display = 'none';
  }
  try {
    if (typeof document.elementsFromPoint === 'function') {
      const stack = document.elementsFromPoint(x, y);
      for (const item of stack) {
        if (item && item.id !== '__autoklik_v32_host__' && !item.hasAttribute('data-autoklik-overlay')) {
          return item;
        }
      }
    }
    return document.elementFromPoint(x, y);
  } finally {
    if (akShadowHost) {
      akShadowHost.style.display = prevDisplay || '';
    }
  }
}

function resolveMirroredTargetElement(payload, pt) {
  // 1. Prioritas Utama: Jika klik di Master Tab mengenai opsi popup/dropdown, cari elemen opsi popup yang sedang terbuka di tab ini
  // agar klik tidak menembus popup pilihan ke elemen belakangnya!
  const topHit = getTopmostElementAtClean(pt.x, pt.y);
  if (payload.optionText) {
    const cleanOpt = String(payload.optionText).replace(/\s+/g, ' ').trim().toLowerCase();
    // Cek dulu apakah elemen teratas di (pt.x, pt.y) sudah merupakan opsi popup yang cocok
    if (topHit) {
      const topOpt = topHit.closest ? (topHit.closest(POPUP_OPTION_SELECTOR) || topHit) : topHit;
      const topTxt = String(topOpt.innerText || topOpt.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
      if (topTxt === cleanOpt || (cleanOpt.length >= 2 && topTxt.includes(cleanOpt))) {
        return topOpt;
      }
    }
    // Cari di seluruh opsi popup yang sedang tampil (visible) di layar
    const optCandidates = document.querySelectorAll(POPUP_OPTION_SELECTOR);
    let exactMatch = null;
    let partialMatch = null;
    let bestDist = Infinity;
    for (const cand of optCandidates) {
      if (!isVisible(cand)) continue;
      const t = String(cand.innerText || cand.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
      if (!t) continue;
      const r = cand.getBoundingClientRect();
      const dist = Math.hypot((r.left + r.width / 2) - pt.x, (r.top + r.height / 2) - pt.y);
      if (t === cleanOpt) {
        if (!exactMatch || dist < bestDist) {
          exactMatch = cand;
          bestDist = dist;
        }
      } else if (!exactMatch && cleanOpt.length >= 3 && t.includes(cleanOpt) && dist < 240) {
        partialMatch = cand;
      }
    }
    if (exactMatch) return exactMatch;
    if (partialMatch) return partialMatch;
  }

  // 2. Jika ada popup/modal/dropdown terbuka tepat di koordinat (pt.x, pt.y), utamakan elemen lapisan paling atas (topHit)
  // dibanding selector elemen latar belakang supaya klik tidak menembus popup!
  if (topHit) {
    const inTopOverlay = topHit.closest && topHit.closest('[role="listbox"], [role="menu"], [role="dialog"], .select2-container, .select2-dropdown, .dropdown-menu, .modal, .swal2-container, .flatpickr-calendar, .datepicker');
    if (inTopOverlay) {
      const topActionable = topHit.closest ? (topHit.closest(POPUP_OPTION_SELECTOR) || topHit) : topHit;
      return topActionable;
    }
  }

  // 3. Gunakan selector jika elemennya terlihat dan berada dekat titik klik
  let el = null;
  if (payload.selector) {
    try {
      const selEl = document.querySelector(payload.selector);
      if (selEl && isVisible(selEl)) {
        el = selEl;
      }
    } catch (_) {}
  }
  if (!el) {
    el = topHit || findInteractiveElementInCircle(pt.x, pt.y, 18);
  }
  if (el && el.closest) {
    const actionable = el.closest('button, a, input, select, textarea, [role="button"], [role="option"], [role="combobox"], [role="menuitem"], .select2-results__option, .dropdown-item, label');
    if (actionable) el = actionable;
  }
  return el;
}

function triggerFullElementSelection(el, pt, payload) {
  if (!el) return;
  try {
    if (typeof el.focus === 'function') {
      try { el.focus({ preventScroll: true }); } catch (_) {}
    }
    const r = el.getBoundingClientRect ? el.getBoundingClientRect() : null;
    const cx = r && r.width > 0 ? Math.round(r.left + r.width / 2) : pt.x;
    const cy = r && r.height > 0 ? Math.round(r.top + r.height / 2) : pt.y;
    const evInit = {
      bubbles: true,
      cancelable: true,
      composed: true,
      view: window,
      clientX: cx,
      clientY: cy,
      button: 0,
      buttons: 1
    };
    el.dispatchEvent(new PointerEvent('pointerdown', { ...evInit, pointerId: 1, isPrimary: true }));
    el.dispatchEvent(new MouseEvent('mousedown', evInit));
    el.dispatchEvent(new PointerEvent('pointerup', { ...evInit, buttons: 0, pointerId: 1, isPrimary: true }));
    el.dispatchEvent(new MouseEvent('mouseup', { ...evInit, buttons: 0 }));
    if (typeof el.click === 'function') {
      el.click();
    } else {
      el.dispatchEvent(new MouseEvent('click', { ...evInit, buttons: 0, detail: 1 }));
    }
    // Jika elemen yang diklik adalah <option> di dalam <select>, pastikan nilai <select> berubah & memicu event change
    if (el.tagName === 'OPTION' && el.parentElement && el.parentElement.tagName === 'SELECT') {
      const sel = el.parentElement;
      sel.value = el.value;
      sel.dispatchEvent(new Event('input', { bubbles: true }));
      sel.dispatchEvent(new Event('change', { bubbles: true }));
    }
  } catch (_) {}
}

ipcRenderer.on('tab1:mirror-apply', (_evt, payload) => {
  if (!payload || typeof payload !== 'object') return;
  // ATURAN WAJIB V5.8: Tab yang sedang Start AutoKlik otomatis PUTUS dari Mirror
  if (overlayState && (overlayState.runnerState === 'RUNNING' || overlayState.isMirrorDisconnectedByAutoKlik === true)) {
    return;
  }
  if (payload.tabIndex && overlayState && Number(overlayState.tabIndex) !== Number(payload.tabIndex)) {
    return;
  }

  isApplyingMirroredEvent = true;
  suppressMirrorEmitUntil = performance.now() + 360;
  try {
    const kind = String(payload.kind || '');
    const pt = resolveMirroredViewportPoint(payload);

    if (kind === 'pointerdown') {
      const el = resolveMirroredTargetElement(payload, pt);
      if (el) {
        if (typeof el.focus === 'function') {
          try { el.focus({ preventScroll: true }); } catch (_) {}
        }
        const evInit = {
          bubbles: true,
          cancelable: true,
          composed: true,
          view: window,
          clientX: pt.x,
          clientY: pt.y,
          button: 0,
          buttons: 1
        };
        try {
          el.dispatchEvent(new PointerEvent('pointerdown', { ...evInit, pointerId: 1, isPrimary: true }));
          el.dispatchEvent(new MouseEvent('mousedown', evInit));
        } catch (_) {}
      }
    } else if (kind === 'click') {
      showClickRippleAt(pt.x, pt.y, 28);
      const el = resolveMirroredTargetElement(payload, pt);
      if (el) {
        triggerFullElementSelection(el, pt, payload);
      }
    } else if (kind === 'wheel') {
      const dy = Number(payload.deltaY) || 0;
      const dx = Number(payload.deltaX) || 0;
      let scEl = null;
      if (payload.scrollSelector) {
        try { scEl = document.querySelector(payload.scrollSelector); } catch (_) {}
      }
      if (!scEl) {
        const hit = getTopmostElementAtClean(pt.x, pt.y);
        scEl = findInnerScrollableContainer(hit);
      }
      if (scEl) {
        scEl.scrollTop += dy;
        scEl.scrollLeft += dx;
      } else {
        window.scrollBy(dx, dy);
      }
    } else if (kind === 'scroll') {
      if (Number.isFinite(Number(payload.scrollX)) || Number.isFinite(Number(payload.scrollY))) {
        window.scrollTo(Number(payload.scrollX) || 0, Number(payload.scrollY) || 0);
      }
      if (payload.scrollSelector) {
        try {
          const scEl = document.querySelector(payload.scrollSelector);
          if (scEl) {
            scEl.scrollTop = Number(payload.scrollTop) || 0;
            scEl.scrollLeft = Number(payload.scrollLeft) || 0;
          }
        } catch (_) {}
      }
    } else if (kind === 'input' || kind === 'change') {
      let el = null;
      if (payload.selector) {
        try { el = document.querySelector(payload.selector); } catch (_) {}
      }
      if (!el) {
        el = findEditableElementAt(pt.x, pt.y, 20) || getTopmostElementAtClean(pt.x, pt.y);
      }
      if (!el && document.activeElement && document.activeElement !== document.body) {
        el = document.activeElement;
      }
      if (el) {
        if (typeof el.focus === 'function') {
          try { el.focus({ preventScroll: true }); } catch (_) {}
        }
        const tag = String(el.tagName || '').toUpperCase();
        const iType = String(el.type || '').toLowerCase();
        if (iType === 'checkbox' || iType === 'radio') {
          el.checked = Boolean(payload.checked);
        } else if (tag === 'SELECT') {
          if (typeof payload.selectedIndex === 'number' && payload.selectedIndex >= 0) {
            el.selectedIndex = payload.selectedIndex;
          }
          if (payload.value !== undefined) {
            el.value = String(payload.value);
          }
        } else {
          setNativeControlValue(el, payload.value);
        }
        try {
          el.dispatchEvent(new InputEvent('input', { bubbles: true, cancelable: true, data: String(payload.value ?? '') }));
        } catch (_) {
          el.dispatchEvent(new Event('input', { bubbles: true }));
        }
        if (kind === 'change' || tag === 'SELECT' || iType === 'checkbox' || iType === 'radio') {
          el.dispatchEvent(new Event('change', { bubbles: true }));
        }
      }
    } else if (kind === 'keydown') {
      let el = null;
      if (payload.selector) {
        try { el = document.querySelector(payload.selector); } catch (_) {}
      }
      if (!el) el = document.activeElement || document.body;
      if (el) {
        const key = String(payload.keyCode || 'Enter');
        const keyInit = { key, code: key, bubbles: true, cancelable: true };
        try {
          el.dispatchEvent(new KeyboardEvent('keydown', keyInit));
          el.dispatchEvent(new KeyboardEvent('keyup', keyInit));
          if (key === 'Enter' && el.form && typeof el.form.requestSubmit === 'function') {
            el.form.requestSubmit();
          }
        } catch (_) {}
      }
    }
  } catch (_) {
  } finally {
    isApplyingMirroredEvent = false;
  }
});

window.addEventListener('DOMContentLoaded', () => {
  renderAutoKlikOverlay();
});
window.addEventListener('load', () => {
  renderAutoKlikOverlay();
});
    """.trimIndent() + "\n"

  fun rendererIndexHtml(): String =
    """
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Ferizy Multi-Container Browser • ${FerizyBuildVersion.RELEASE_TAG}</title>
  <link rel="stylesheet" href="styles.css" />
</head>
<body>
  <header id="shell-header">
    <div class="top-strip">
      <div class="brand">
        <span class="brand-dot"></span>
        <strong>Ferizy Multi-Container</strong>
        <span class="badge badge-phase">FASE 4 • ${FerizyBuildVersion.RELEASE_TAG}</span>
        <span id="total-tabs-badge" class="badge">0/15 Tab</span>
        <div class="toolbar-group" title="Kotak Kontainer Akun & Kumpulan Tab">
          <button id="btn-add-account" class="btn-small btn-primary">+ Akun</button>
          <button id="btn-toggle-preflight-grid" class="btn-small btn-primary" title="Sembunyikan / Tampilkan Kotak Kontainer Tab & Akun">📦 Kotak Kontainer</button>
          <button id="btn-open-display-modal" class="btn-small">Tampilan</button>
        </div>
        <div class="toolbar-group" title="Zoom Semua Halaman (Ctrl + / - / 0)">
          <button id="btn-zoom-out" class="btn-small">-</button>
          <button id="btn-zoom-reset" class="btn-small">100%</button>
          <button id="btn-zoom-in" class="btn-small">+</button>
        </div>
        <button id="btn-toggle-captcha-notif" class="btn-small" title="ON/OFF Notif CAPTCHA/Challenge Muncul">Notif CAPTCHA: OFF</button>
        <button id="btn-open-autoklik-ext" class="btn-small btn-primary" title="Atur Titik AutoKlik, Jeda, Loop & Load/Save JSON Per-Tab">⚙ Atur Titik</button>
        <span id="sys-meter-pill" class="pill pill-ok" title="Pantau CPU & RAM PC + Total Browser tiap 3 detik dari Main Process (selalu tampil di bar utama)">CPU PC: --% • RAM PC: --/--GB | Browser: --MB (--%)</span>
        <button id="btn-quick-clean" class="btn-small" title="Bersihkan cache HTTP non-login & reload tab Standby tersembunyi tanpa mengganggu tab yang sedang berjalan">⚡ Bersih Cepat</button>
      </div>
      <div class="system-pills">
        <span class="pill pill-ok">Sniffer 0ms + Event Bus &lt;1ms</span>
        <span id="clock-sync-pill" class="pill pill-info">Clock Sync: 0ms (RTT 0ms)</span>
        <span id="shot-timer-pill" class="pill pill-info">Timer Tembak: OFF</span>
        <span id="blocker-pill" class="pill pill-info">Asset Blocker: Selektif</span>
        <span id="watchdog-pill" class="pill pill-info">Watchdog: 350MB • Queue 400ms</span>
        <span id="endurance-pill" class="pill pill-ok">RAM: 0MB • Log: 0MB (50MB/7h)</span>
        <span id="ntfy-badge" class="pill pill-accent">ntfy: ferizy_Transsamudra05</span>
        <button id="btn-mute-alarm" class="btn-small">Suara: ON</button>
        <button id="btn-resume-captcha" class="btn-small">Lanjut CAPTCHA (0)</button>
        <button id="btn-run-endurance" class="btn-small">Uji 24J</button>
        <button id="btn-export-health" class="btn-small">Health JSON</button>
        <button id="btn-open-notif-modal" class="btn-small btn-primary">Notif HP (3/6)</button>
        <button id="btn-test-ntfy" class="btn-small">Tes ntfy</button>
        <button id="btn-toggle-checklist" class="btn-small btn-primary">Panel Opsi Fase 3–4</button>
        <button id="btn-reload-standby" class="btn-small">Reload Standby (400ms)</button>
        <button id="btn-check-all-ip" class="btn-small">Cek Semua IP</button>
        <button id="btn-refresh-preflight" class="btn-small btn-primary">Refresh Pre-Flight</button>
      </div>
    </div>

    <div id="optional-checklist-panel" class="checklist-panel hidden">
      <div class="chk-row">
        <label class="chk-item"><input type="checkbox" id="chk-keep-alive" checked /> Keep-Alive</label>
        <select id="sel-keepalive-min" class="chk-select">
          <option value="10">10m</option>
          <option value="12" selected>12m</option>
          <option value="15">15m</option>
        </select>
        <select id="sel-autowake-mode" class="chk-select">
          <option value="monitor_only">Wake: Monitor Saja</option>
          <option value="same_account" selected>Wake: Akun Sama &lt;1ms</option>
          <option value="all_accounts">Wake: Semua 5 Akun &lt;1ms</option>
        </select>
        <label class="chk-item"><input type="checkbox" id="chk-captcha-autopop" checked /> CAPTCHA Auto-Pop</label>
        <label class="chk-item"><input type="checkbox" id="chk-smart-matcher" checked /> Smart Matcher</label>
        <select id="sel-service-type" class="chk-select">
          <option value="all" selected>Layanan: Semua</option>
          <option value="express">Layanan: Express</option>
          <option value="reguler">Layanan: Reguler</option>
        </select>
        <label class="chk-item"><input type="checkbox" id="chk-matcher-fallback" checked /> Fallback Terdekat</label>
        <input id="input-priority-hours" class="chk-hours" type="text" value="08:00,10:00,12:00,14:00,20:00" title="Jam Prioritas" />
        <label class="chk-item"><input type="checkbox" id="chk-latency-comp" /> Latency RTT/2</label>
        <select id="sel-fast-timeout" class="chk-select">
          <option value="3500">TO: 3.5s</option>
          <option value="4500" selected>TO: 4.5s</option>
          <option value="5500">TO: 5.5s</option>
        </select>
        <select id="sel-advanced-stop" class="chk-select">
          <option value="per_tab">Stop: Per-Tab 0ms</option>
          <option value="per_account">Stop: 1 Akun</option>
          <option value="all_accounts">Stop: Semua 5 Akun</option>
        </select>
      </div>
      <div class="chk-row">
        <label class="chk-item"><input type="checkbox" id="chk-sched-shot" /> Tembak Jam Server</label>
        <input id="input-sched-time" class="chk-time" type="text" value="00:00:00" placeholder="HH:MM:SS" />
        <span class="chk-sep">|</span>
        <input id="input-ntfy-server" class="chk-hours" type="text" value="https://ntfy.sh" title="Server ntfy" />
        <select id="sel-notif-dedupe" class="chk-select">
          <option value="5">Dedupe 5s</option>
          <option value="10" selected>Dedupe 10s</option>
          <option value="30">Dedupe 30s</option>
        </select>
        <span class="pill pill-info">Kirim ke HP:</span>
        <label class="chk-item"><input type="checkbox" id="chk-notif-lintasan" checked /> Lintasan / Jam Muncul</label>
        <label class="chk-item"><input type="checkbox" id="chk-notif-kuota" checked /> Perubahan Kuota</label>
        <label class="chk-item"><input type="checkbox" id="chk-notif-tembus" checked /> Order Sukses</label>
        <label class="chk-item"><input type="checkbox" id="chk-notif-logout" /> Logout / 401</label>
        <label class="chk-item"><input type="checkbox" id="chk-notif-proxy" /> Proxy Putus</label>
        <label class="chk-item"><input type="checkbox" id="chk-notif-captcha" /> CAPTCHA</label>
      </div>
      <div class="chk-row">
        <span class="pill pill-info">Fase 4: 24J &amp; Log</span>
        <select id="sel-log-preset" class="chk-select">
          <option value="50mb_7d" selected>Log 50 MB / 7 Hari (Rekomendasi)</option>
          <option value="20mb_3d">Log 20 MB / 3 Hari (Hemat SSD)</option>
        </select>
        <select id="sel-watchdog-cal" class="chk-select">
          <option value="320">Watchdog 320 MB</option>
          <option value="350" selected>Watchdog 350 MB</option>
          <option value="380">Watchdog 380 MB</option>
        </select>
        <label class="chk-item"><input type="checkbox" id="chk-health-report" checked /> Health Report JSON</label>
        <button id="btn-prune-logs" class="btn-small">Bersihkan Log</button>
        <button id="btn-open-logs" class="btn-small">Folder Log</button>
        <button id="btn-apply-checklist" class="btn-small btn-primary">Simpan Opsi Fase 3–4</button>
      </div>
    </div>

    <div id="preflight-grid" class="preflight-grid">
      <div id="tab1-collection-card" class="acc-card tab1-collection-card">
        <div class="acc-head">
          <div class="acc-name-wrap">
            <span class="acc-dot-login ok"></span>
            <span class="acc-name-text" style="max-width:145px;">📦 Kumpulan Tab 1</span>
            <span class="acc-tab-badge">(Master Mirror)</span>
          </div>
        </div>
        <div class="acc-autoklik-row">
          <button id="btn-toggle-tab1-mirror" type="button" class="btn-small btn-run" title="Mirror Real-Time 0ms untuk Kumpulan Tab 1. Otomatis putus untuk tab yang sedang Start AutoKlik.">🔗 Mirror Tab 1: ON</button>
          <button id="btn-toggle-tab1-split" type="button" class="btn-small btn-primary" title="Tampilkan semua Tab 1 lintas akun secara Split Vertikal berdampingan dalam 1 layar">▥ Split Vertikal</button>
        </div>
      </div>
      <div id="tab2-collection-card" class="acc-card tab1-collection-card">
        <div class="acc-head">
          <div class="acc-name-wrap">
            <span class="acc-dot-login"></span>
            <span class="acc-name-text" style="max-width:145px;">📦 Kumpulan Tab 2</span>
            <span class="acc-tab-badge">(Mirror Tab 2)</span>
          </div>
        </div>
        <div class="acc-autoklik-row">
          <button id="btn-toggle-tab2-mirror" type="button" class="btn-small" title="Mirror Real-Time 0ms untuk Kumpulan Tab 2. Otomatis putus untuk tab yang sedang Start AutoKlik.">🔗 Mirror Tab 2: OFF</button>
          <button id="btn-toggle-tab2-split" type="button" class="btn-small btn-primary" title="Tampilkan semua Tab 2 lintas akun secara Split Vertikal berdampingan dalam 1 layar">▥ Split Vertikal</button>
        </div>
      </div>
      <div id="tab3-collection-card" class="acc-card tab1-collection-card">
        <div class="acc-head">
          <div class="acc-name-wrap">
            <span class="acc-dot-login"></span>
            <span class="acc-name-text" style="max-width:145px;">📦 Kumpulan Tab 3</span>
            <span class="acc-tab-badge">(Mirror Tab 3)</span>
          </div>
        </div>
        <div class="acc-autoklik-row">
          <button id="btn-toggle-tab3-mirror" type="button" class="btn-small" title="Mirror Real-Time 0ms untuk Kumpulan Tab 3. Otomatis putus untuk tab yang sedang Start AutoKlik.">🔗 Mirror Tab 3: OFF</button>
          <button id="btn-toggle-tab3-split" type="button" class="btn-small btn-primary" title="Tampilkan semua Tab 3 lintas akun secara Split Vertikal berdampingan dalam 1 layar">▥ Split Vertikal</button>
        </div>
      </div>
    </div>

    <div id="split-slot-bar" class="split-slot-bar hidden"></div>

    <div class="nav-strip">
      <button id="btn-back-active" class="btn-small" title="Kembali ke halaman/tampilan website sebelumnya (Alt + Panah Kiri)">⬅ Kembali</button>
      <button id="btn-reload-active" class="btn-small" title="Muat ulang halaman tab aktif">↻ Muat Ulang</button>
      <div class="address-bar-wrap">
        <select id="url-preset-select" class="url-preset-select" title="Pilih Alamat Web Cepat di Address Bar">
          <option value="">🌐 Pilih Alamat Web</option>
          <option value="https://trip.ferizy.com/">https://trip.ferizy.com/</option>
          <option value="https://trip.ferizy.com/order">https://trip.ferizy.com/order</option>
        </select>
        <input id="active-url-input" type="text" list="ferizy-url-presets" placeholder="https://trip.ferizy.com/" />
        <datalist id="ferizy-url-presets">
          <option value="https://trip.ferizy.com/">Beranda / Cari Jadwal Ferizy</option>
          <option value="https://trip.ferizy.com/order">Pesanan Saya / Order Ferizy</option>
        </datalist>
      </div>
      <button id="btn-go-url" class="btn-small btn-primary">Buka URL</button>
      <button id="btn-toggle-active-runner" class="btn-small btn-run">▶ Start Tab</button>
      <span id="active-meta" class="active-meta">Siap</span>
    </div>
  </header>

  <div id="config-modal" class="modal hidden">
    <div class="modal-card">
      <div class="modal-head">
        <h3 id="modal-title">Pengaturan Akun</h3>
        <button id="btn-close-modal" class="btn-small">Tutup</button>
      </div>
      <div class="modal-body">
        <label>Nama Kotak Akun (Tersimpan Permanen)</label>
        <input id="cfg-acc-name" type="text" maxlength="32" placeholder="Contoh: Akun Merak 1" />
        <label>Mode Proxy (Multi-IP Container)</label>
        <select id="cfg-proxy-protocol">
          <option value="direct">Direct (Tanpa Proxy)</option>
          <option value="http">HTTP Proxy</option>
          <option value="socks5">SOCKS5 / Modem Bridge</option>
        </select>
        <div class="row-2">
          <div>
            <label>Host / IP Proxy</label>
            <input id="cfg-proxy-host" type="text" placeholder="127.0.0.1 atau IP proxy" />
          </div>
          <div>
            <label>Port</label>
            <input id="cfg-proxy-port" type="number" value="8080" />
          </div>
        </div>
        <div class="row-2">
          <div>
            <label>Username (Opsional)</label>
            <input id="cfg-proxy-user" type="text" />
          </div>
          <div>
            <label>Password (Opsional)</label>
            <input id="cfg-proxy-pass" type="password" />
          </div>
        </div>
        <hr />
        <label>Geolocation Provider</label>
        <select id="cfg-geo-mode">
          <option value="preset_0ms">Preset Koordinat 0ms + Micro-Jitter</option>
          <option value="windows">Lokasi Windows Native</option>
        </select>
        <label>Preset Pelabuhan</label>
        <select id="cfg-geo-preset">
          <option value="merak">Pelabuhan Merak (-5.9308, 105.9935)</option>
          <option value="bakauheni">Pelabuhan Bakauheni (-5.8705, 105.7538)</option>
          <option value="ketapang">Pelabuhan Ketapang (-8.1449, 114.4036)</option>
          <option value="gilimanuk">Pelabuhan Gilimanuk (-8.1636, 114.4375)</option>
        </select>
      </div>
      <div class="modal-foot">
        <button id="btn-save-account-cfg" class="btn-small btn-primary">Simpan &amp; Terapkan</button>
      </div>
    </div>
  </div>

  <div id="notif-modal" class="modal hidden">
    <div class="modal-card">
      <div class="modal-head">
        <h3>Pengaturan Notif Terkirim ke HP (ntfy)</h3>
        <button id="btn-close-notif-modal" class="btn-small">Tutup</button>
      </div>
      <div class="modal-body">
        <p class="modal-hint">Hanya notifikasi yang tercentang di bawah ini yang akan dikirim ke HP Anda lewat ntfy.</p>
        <label class="chk-item"><input type="checkbox" id="m-notif-lintasan" checked /> Lintasan Dibuka / Jam Muncul (trip.ferizy.com/)</label>
        <label class="chk-item"><input type="checkbox" id="m-notif-kuota" checked /> Perubahan Kuota (/ferryschedule)</label>
        <label class="chk-item"><input type="checkbox" id="m-notif-tembus" checked /> Order Sukses / Tembus Verifikasi</label>
        <hr />
        <label class="chk-item"><input type="checkbox" id="m-notif-logout" /> Sesi Akun Logout / HTTP 401</label>
        <label class="chk-item"><input type="checkbox" id="m-notif-proxy" /> Koneksi Proxy Terputus</label>
        <label class="chk-item"><input type="checkbox" id="m-notif-captcha" /> CAPTCHA / Challenge Muncul</label>
        <hr />
        <div class="row-2">
          <div>
            <label>Topik ntfy</label>
            <input id="m-ntfy-topic" type="text" placeholder="ferizy_Transsamudra05" />
          </div>
          <div>
            <label>Deduplikasi (detik)</label>
            <select id="m-notif-dedupe">
              <option value="5">5 detik</option>
              <option value="10" selected>10 detik</option>
              <option value="30">30 detik</option>
            </select>
          </div>
        </div>
        <label>Server ntfy URL</label>
        <input id="m-ntfy-server" type="text" value="https://ntfy.sh" />
      </div>
      <div class="modal-foot">
        <button id="btn-save-notif-modal" class="btn-small btn-primary">Simpan Notif HP</button>
      </div>
    </div>
  </div>

  <div id="display-modal" class="modal hidden">
    <div class="modal-card">
      <div class="modal-head">
        <h3>Pengaturan Tampilan, Zoom &amp; New Split View</h3>
        <button id="btn-close-display-modal" class="btn-small">Tutup</button>
      </div>
      <div class="modal-body">
        <label class="chk-item">
          <input type="checkbox" id="chk-compact-mode" />
          Mode Header Kompak (Sembunyikan pill status agar area web lebih tinggi)
        </label>
        <hr />
        <label>Zoom Semua Website (Global: <strong id="disp-zoom-label">100%</strong>)</label>
        <input id="disp-zoom-slider" type="range" min="50" max="150" step="5" value="100" />
        <div class="zoom-presets">
          <button class="btn-small disp-zoom-preset" data-zoom="70">70%</button>
          <button class="btn-small disp-zoom-preset" data-zoom="80">80%</button>
          <button class="btn-small disp-zoom-preset" data-zoom="90">90%</button>
          <button class="btn-small disp-zoom-preset" data-zoom="100">100%</button>
          <button class="btn-small disp-zoom-preset" data-zoom="110">110%</button>
          <button class="btn-small disp-zoom-preset" data-zoom="125">125%</button>
        </div>
        <hr />
        <label>Split Mandiri Akun Aktif (Bagi Rata Presisi untuk Titik AutoKlik)</label>
        <p class="modal-hint">Split 2 atau Split 3 berlaku mandiri hanya untuk tab di dalam akun yang sedang aktif &mdash; tidak mencampur atau mengubah pengaturan kontainer akun lain. Klik kanan web &rarr; <b>Salin Koordinat Klik (x, y)</b>.</p>
        <div class="zoom-presets">
          <button class="btn-small disp-split-preset" data-split="1">1 Layar Penuh</button>
          <button class="btn-small disp-split-preset" data-split="2">Split 2 Rata (50%)</button>
          <button class="btn-small disp-split-preset" data-split="3">Split 3 Rata (33.3%)</button>
        </div>
      </div>
      <div class="modal-foot">
        <button id="btn-done-display-modal" class="btn-small btn-primary">Selesai</button>
      </div>
    </div>
  </div>

  <div id="autoklik-ext-modal" class="modal hidden">
    <div class="modal-card modal-card-wide">
      <div class="modal-head">
        <div>
          <h3>Atur Titik • AutoKlik v3.2 (Mandiri Per-Tab)</h3>
          <small id="ak-container-badge" class="active-meta">Container: persist:akun_1 • Tab 1</small>
        </div>
        <button id="btn-close-autoklik-ext" class="btn-small">Tutup</button>
      </div>
      <div class="modal-body modal-scroll">
        <div class="ak-toolbar">
          <select id="ak-account-select" style="width:155px;"></select>
          <select id="ak-tab-select" style="width:125px;"></select>
          <button id="btn-ak-start-acc" class="btn-small btn-run">▶ Start Tab</button>
          <button id="btn-ak-stop-acc" class="btn-small btn-stop">■ Stop</button>
          <button id="btn-ak-pick-target" class="btn-small btn-primary">🎯 Ambil Titik</button>
          <button id="btn-ak-save-dialog" class="btn-small">Save .JSON</button>
          <button id="btn-ak-open-dialog" class="btn-small">Load .JSON</button>
          <button id="btn-ak-copy-all" class="btn-small">Salin ke Semua Akun</button>
        </div>
        <div id="ak-status-msg" class="modal-hint">Konfigurasi titik bersifat mandiri per tab (Tab 1 / Tab 2 / Tab 3) di dalam akun masing-masing.</div>

        <div class="ak-section">
          <strong>Pengaturan Tab Ini (<span id="ak-tab-scope-lbl">Tab 1</span>)</strong>
          <div class="chk-row">
            <label class="chk-item"><input type="checkbox" id="ak-set-enabled" checked /> Aktif di Tab Ini</label>
            <label class="chk-item"><input type="checkbox" id="ak-set-loop" checked /> Loop Kembali ke Titik #1</label>
            <label class="chk-item"><input type="checkbox" id="ak-set-smartmatch" checked /> Smart Match Jadwal</label>
            <label class="chk-item"><input type="checkbox" id="ak-set-antirevert" checked /> Lazy Anti-Revert</label>
            <label class="chk-item"><input type="checkbox" id="ak-set-hidden" /> Hidden Titik</label>
            <label class="chk-item"><input type="checkbox" id="ak-set-locked" /> Kunci Titik</label>
          </div>
          <div class="ak-grid-4">
            <div>
              <label>Maks Loop (0=∞)</label>
              <input id="ak-set-maxloops" type="number" min="0" max="999" value="0" />
            </div>
            <div>
              <label>Jeda Loop (ms)</label>
              <input id="ak-set-loopdelay" type="number" min="20" max="60000" value="100" />
            </div>
            <div>
              <label>Radius Cari (px)</label>
              <input id="ak-set-radius" type="number" min="5" max="200" value="28" />
            </div>
            <div style="display:flex;align-items:flex-end;">
              <button id="btn-ak-save-settings" class="btn-small btn-primary" style="width:100%;padding:7px;">Simpan Pengaturan</button>
            </div>
          </div>
        </div>

        <div class="ak-section">
          <div class="modal-head">
            <strong>Daftar Titik Eksekusi (<span id="ak-points-count">0</span>)</strong>
            <button id="btn-ak-clear-tab-points" class="btn-small btn-stop" title="Kosongkan semua titik di tab ini saja">Kosongkan Tab Ini</button>
          </div>
          <div id="ak-points-list" class="ak-points-list"></div>
        </div>

        <div class="ak-section">
          <strong id="ak-form-title">Tambah Titik Baru</strong>
          <div class="ak-grid-4">
            <div>
              <label>Tipe</label>
              <select id="ak-pt-type">
                <option value="tap">TAP (Klik)</option>
                <option value="swipe_up">SWIPE ATAS (▲)</option>
                <option value="swipe_down">SWIPE BWH (▼)</option>
                <option value="input">INPUT KETIK (+Popup)</option>
              </select>
            </div>
            <div>
              <label>Label Titik</label>
              <input id="ak-pt-label" type="text" placeholder="Contoh: Tombol Pesan" />
            </div>
            <div>
              <label>X</label>
              <input id="ak-pt-x" type="number" value="540" />
            </div>
            <div>
              <label>Y</label>
              <input id="ak-pt-y" type="number" value="420" />
            </div>
          </div>
          <div class="ak-grid-4">
            <div>
              <label>Jeda (ms, default 500 = 0.5s)</label>
              <input id="ak-pt-delay" type="number" min="15" value="500" />
            </div>
            <div>
              <label>Selector CSS (Opsional)</label>
              <input id="ak-pt-selector" type="text" placeholder="#btn-pesan" />
            </div>
            <div>
              <label>Teks Input / Key Popup</label>
              <input id="ak-pt-value" type="text" placeholder="Isi teks / nama" />
            </div>
            <div>
              <label>Swipe End (X,Y Opsional)</label>
              <input id="ak-pt-endxy" type="text" placeholder="540,200" />
            </div>
          </div>
          <div class="modal-foot" style="gap:6px;">
            <button id="btn-ak-cancel-edit" class="btn-small hidden">Batal Edit</button>
            <button id="btn-ak-save-point" class="btn-small btn-primary">+ Tambah Titik</button>
          </div>
        </div>

        <div class="ak-section">
          <div class="modal-head">
            <strong>Backup / Import JSON Akun Ini</strong>
            <div style="display:flex;gap:6px;">
              <button id="btn-ak-export-json" class="btn-small">Muat JSON Akun</button>
              <button id="btn-ak-import-json" class="btn-small btn-primary">Import JSON ke Akun Ini</button>
            </div>
          </div>
          <textarea id="ak-json-io" class="ak-json-box" rows="4" placeholder="Klik 'Muat JSON Akun' untuk ekspor, atau tempel JSON AutoKlik v3.2 di sini lalu klik 'Import JSON'..."></textarea>
        </div>
      </div>
    </div>
  </div>

  <div id="ak-edit-point-modal" class="modal hidden">
    <div class="modal-card ep-ext-card">
      <div class="modal-head ep-ext-head">
        <div class="ep-title-wrap">
          <h3 id="ep-modal-title">Titik #1 ( 🎯 )</h3>
          <small id="ep-modal-sub" class="active-meta">Akun 1 • persist:akun_1</small>
        </div>
        <div class="ep-order-controls">
          <span class="ep-order-lbl">Urutan:</span>
          <button id="btn-ep-order-left" type="button" class="ep-order-btn" title="Geser Urutan Lebih Awal">⬅</button>
          <button id="btn-ep-order-right" type="button" class="ep-order-btn" title="Geser Urutan Lebih Akhir">➡</button>
          <button id="btn-ep-delete" type="button" class="btn-small btn-stop" title="Hapus Titik Ini">🗑</button>
          <button id="btn-close-edit-point" type="button" class="btn-small">×</button>
        </div>
      </div>

      <div class="modal-body modal-scroll ep-ext-body">
        <div class="ep-top-meta-row">
          <label class="chk-item"><input type="checkbox" id="ep-enabled" checked /> Titik Aktif</label>
          <button id="btn-ep-repick" type="button" class="btn-small btn-primary">🎯 Pilih Ulang Lokasi</button>
        </div>

        <div class="ep-field-label">Tipe Aksi:</div>
        <div class="ep-type-grid">
          <button type="button" class="ep-type-btn active" data-type="tap">🎯 Tap</button>
          <button type="button" class="ep-type-btn" data-type="swipe_up">▲ Swipe Atas</button>
          <button type="button" class="ep-type-btn" data-type="swipe_down">▼ Swipe Bwh</button>
          <button type="button" class="ep-type-btn" data-type="input">📋 Input Ketik (+Popup)</button>
        </div>

        <div class="ep-inline-card">
          <span class="ep-inline-title">⏱ Jeda Titik (Def 0.5s):</span>
          <input id="ep-delay-val" type="number" min="0.01" step="0.1" value="0.5" class="ep-num-sm" />
          <label class="ep-radio"><input type="radio" name="ep-delay-unit" value="ms" /> ms</label>
          <label class="ep-radio"><input type="radio" name="ep-delay-unit" value="s" checked /> Detik (s)</label>
        </div>

        <div class="ep-inline-card ep-slider-row">
          <span id="ep-icon-size-label" class="ep-purple-lbl">📏 Ikon: 25% (35dp)</span>
          <input id="ep-icon-size" type="range" min="15" max="100" step="1" value="25" />
        </div>

        <div id="ep-swipe-group" class="ep-subcard ep-swipe-card hidden">
          <div class="ep-row-between">
            <span>📏 Panjang Garis (px):</span>
            <input id="ep-swipe-len" type="number" min="40" max="1500" value="350" class="ep-num-md" />
          </div>
          <div class="ep-row-between">
            <span>⚡ Skala Tenaga Swipe:</span>
            <input id="ep-swipe-scale" type="number" min="1" max="20" value="2" class="ep-num-md" />
          </div>
          <div class="ep-preset-row">
            <button type="button" class="ep-preset-btn ep-scale-preset" data-scale="1">1x</button>
            <button type="button" class="ep-preset-btn ep-scale-preset" data-scale="2">2x</button>
            <button type="button" class="ep-preset-btn ep-scale-preset" data-scale="3">3x</button>
            <button type="button" class="ep-preset-btn ep-scale-preset" data-scale="4">4x</button>
            <button type="button" class="ep-preset-btn ep-scale-preset" data-scale="5">5x</button>
            <button type="button" class="ep-preset-btn ep-scale-preset" data-scale="8">8x</button>
          </div>
          <div id="ep-swipe-summary" class="ep-swipe-summary">Total Jarak Geser Layar: 700 px (Smooth Ease-In-Out 60FPS)</div>
        </div>

        <div id="ep-input-group" class="ep-subcard ep-input-card hidden">
          <div class="ep-popup-pin-box">
            <label class="chk-item">
              <input type="checkbox" id="ep-popup-pin-enabled" />
              <strong id="ep-popup-pin-label">Aktifkan Popup Key Autofill (Pin Ungu #1)</strong>
            </label>
            <div id="ep-popup-trigger-preview" class="ep-hint-sm" style="color:#e9d5ff;margin-top:4px;">Huruf pemicu otomatis diambil dari huruf pertama teks siklus (#1).</div>
            <div class="ep-row-between" style="margin-top:6px;">
              <span>⏱ Waktu Jeda Muncul Popup (ms):</span>
              <input id="ep-popup-delay" type="number" min="20" max="10000" step="10" value="300" class="ep-num-md" />
            </div>
            <div class="ep-preset-row ep-preset-4" style="margin-top:4px;">
              <button type="button" class="ep-preset-btn ep-popup-delay-preset" data-pdelay="150">150ms</button>
              <button type="button" class="ep-preset-btn ep-popup-delay-preset active" data-pdelay="300">300ms (Def)</button>
              <button type="button" class="ep-preset-btn ep-popup-delay-preset" data-pdelay="500">500ms</button>
              <button type="button" class="ep-preset-btn ep-popup-delay-preset" data-pdelay="800">800ms</button>
            </div>
          </div>
          <div class="ep-cycle-header">
            <span>📋 Daftar Teks Input Ketik (Per Siklus • 100% Murni Keyboard)</span>
            <button id="btn-ep-add-cycle-text" type="button" class="btn-small btn-run">+ Tambah Teks</button>
          </div>
          <p class="ep-hint-sm">100% Murni Keyboard Manusia (Tanpa Paste): Klik Kolom → Hapus Teks Lama dengan Backspace Berulang Sesuai Panjang Teks → Ketik Huruf Demi Huruf (Default 30ms/huruf).</p>
          <div id="ep-cycle-texts-list" class="ep-cycle-list"></div>

          <div class="ep-row-between" style="margin-top:4px;">
            <span class="ep-amber-lbl">⚡ Kecepatan Ketik Keyboard (Def 30ms/huruf):</span>
            <input id="ep-typing-speed" type="number" min="5" max="1000" value="30" class="ep-num-md" />
          </div>
          <div class="ep-preset-row ep-preset-4">
            <button type="button" class="ep-preset-btn ep-speed-preset" data-speed="15">15ms (Cepat)</button>
            <button type="button" class="ep-preset-btn ep-speed-preset active" data-speed="30">30ms (Default)</button>
            <button type="button" class="ep-preset-btn ep-speed-preset" data-speed="45">45ms (Santai)</button>
            <button type="button" class="ep-preset-btn ep-speed-preset" data-speed="65">65ms (Halus)</button>
          </div>
          <label class="chk-item"><input type="checkbox" id="ep-clear-before" checked /> Hapus teks sebelumnya dengan Backspace berulang sesuai panjang teks lalu ketik</label>
          <label class="chk-item"><input type="checkbox" id="ep-focus-before" checked /> Gerakkan kursor halus, klik &amp; fokus kolom otomatis sebelum mengetik</label>
          <label class="chk-item hidden"><input type="checkbox" id="ep-antirevert" /> Lazy Anti-Revert untuk Input</label>
        </div>

        <div class="ep-inline-card" style="justify-content:space-between;background:#0b192e;border-color:#1e3a5f;">
          <span style="font-size:11px;color:#6ee7b7;font-weight:700;">🔄 Siklus Default Permanen: Titik #1 ➔ ... ➔ Titik Terakhir ➔ Kembali ke Titik #1</span>
        </div>

        <details class="ep-advanced-details">
          <summary>Koordinat &amp; Label Lanjutan (<span id="ep-coord-preview">x=0, y=0</span>)</summary>
          <div class="row-2" style="margin-top:6px;">
            <div>
              <label>Koordinat X</label>
              <input id="ep-x" type="number" />
            </div>
            <div>
              <label>Koordinat Y</label>
              <input id="ep-y" type="number" />
            </div>
          </div>
          <div class="row-2" style="margin-top:6px;">
            <div>
              <label>Label Titik</label>
              <input id="ep-label" type="text" placeholder="Contoh: Tombol Pesan" />
            </div>
            <div>
              <label>Radius Cari (px)</label>
              <input id="ep-radius" type="number" min="5" max="200" value="28" />
            </div>
          </div>
          <div style="margin-top:6px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <label>Selector CSS (Kosong = klik tepat di icon X,Y)</label>
              <button id="btn-ep-clear-sel" type="button" class="btn-small">Kosongkan</button>
            </div>
            <input id="ep-selector" type="text" placeholder="Kosong = klik tepat di center icon" />
          </div>
        </details>
      </div>

      <div class="modal-foot ep-ext-foot">
        <button id="btn-ep-cancel" type="button" class="ep-foot-cancel">✕ Batal</button>
        <button id="btn-ep-save" type="button" class="ep-foot-save">✔ Simpan</button>
      </div>
    </div>
  </div>

  <div id="ak-load-copy-modal" class="modal hidden">
    <div class="modal-card modal-wide">
      <div class="modal-head">
        <div>
          <h3 id="lcm-title">📂 Menu Load &amp; Copy-Paste Konfigurasi AutoKlik</h3>
          <small id="lcm-subtitle" class="active-meta">Akun 1 • Tab 1</small>
        </div>
        <button id="btn-close-load-copy-modal" type="button" class="btn-small">×</button>
      </div>
      <div class="modal-body modal-scroll">
        <div class="ak-section">
          <strong>1. Target Kontainer &amp; Tab Tujuan</strong>
          <div class="row-2" style="margin-top:6px;">
            <div>
              <label>Akun Tujuan</label>
              <select id="lcm-target-account"></select>
            </div>
            <div>
              <label>Tab Tujuan</label>
              <select id="lcm-target-tab">
                <option value="1">Tab 1</option>
                <option value="2">Tab 2</option>
                <option value="3">Tab 3</option>
              </select>
            </div>
          </div>
          <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:8px;">
            <button id="btn-lcm-load-file" type="button" class="btn-small btn-primary">📂 Load File .JSON dari PC ke Tab Tujuan</button>
            <button id="btn-lcm-save-file" type="button" class="btn-small">💾 Save Konfigurasi Tab Tujuan ke File .JSON</button>
          </div>
        </div>

        <div class="ak-section">
          <strong>2. Copy-Paste Konfigurasi Antar Tab / Antar Akun</strong>
          <small id="lcm-clipboard-badge" class="active-meta">Clipboard: Belum ada konfigurasi yang disalin.</small>
          <div class="row-2" style="margin-top:6px;">
            <div>
              <label>Salin Dari Akun Sumber</label>
              <select id="lcm-source-account"></select>
            </div>
            <div>
              <label>Salin Dari Tab Sumber</label>
              <select id="lcm-source-tab">
                <option value="1">Tab 1</option>
                <option value="2">Tab 2</option>
                <option value="3">Tab 3</option>
              </select>
            </div>
          </div>
          <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:8px;">
            <button id="btn-lcm-copy-tab" type="button" class="btn-small">📋 Copy dari Tab Sumber</button>
            <button id="btn-lcm-paste-tab" type="button" class="btn-small btn-run">📥 Paste ke Tab Tujuan</button>
            <button id="btn-lcm-paste-all-tabs-acc" type="button" class="btn-small btn-primary">📑 Paste ke Semua Tab (1-3) di Akun Tujuan</button>
            <button id="btn-lcm-paste-all-accounts" type="button" class="btn-small">🌐 Paste ke Semua Akun (Tab Sama)</button>
          </div>
        </div>

        <div class="ak-section">
          <div class="modal-head">
            <strong>3. Copy-Paste Teks JSON Konfigurasi Langsung</strong>
            <div style="display:flex;gap:6px;">
              <button id="btn-lcm-copy-json" type="button" class="btn-small">📋 Muat &amp; Copy JSON Sumber</button>
              <button id="btn-lcm-apply-json" type="button" class="btn-small btn-primary">📥 Paste / Terapkan JSON ke Tab Tujuan</button>
            </div>
          </div>
          <textarea id="lcm-json-box" class="ak-json-box" rows="4" placeholder="Klik 'Muat & Copy JSON Sumber' atau tempel (Ctrl+V) JSON konfigurasi AutoKlik di sini lalu klik 'Paste / Terapkan JSON ke Tab Tujuan'..."></textarea>
        </div>

        <small id="lcm-status-msg" class="active-meta">Pilih sumber dan tujuan untuk menyalin konfigurasi antar tab dengan cepat.</small>
      </div>
    </div>
  </div>

  <script src="renderer.js"></script>
</body>
</html>
    """.trimIndent() + "\n"

  fun rendererStylesCss(): String =
    """
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  background: #070e1b;
  color: #e2e8f0;
  overflow: hidden;
  user-select: none;
}
input, textarea, .active-meta, .ind, .modal-card {
  user-select: text;
}
#shell-header {
  background: #0b1528;
  border-bottom: 1px solid #1e3154;
  padding: 5px 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
}
.top-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  flex-wrap: wrap;
}
.brand { display: flex; align-items: center; gap: 5px; font-size: 12px; flex-wrap: wrap; }
.brand-dot { width: 9px; height: 9px; border-radius: 50%; background: #22d3ee; box-shadow: 0 0 8px #22d3ee; }
.toolbar-group {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: #101d35;
  border: 1px solid #23375c;
  padding: 2px 4px;
  border-radius: 7px;
}
body.compact-mode #shell-header { padding: 4px 8px; gap: 4px; }
body.compact-mode .system-pills { display: none; }
body.compact-mode .preflight-grid { max-height: 175px; }
.badge {
  background: #172745;
  border: 1px solid #2b4470;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 10px;
  color: #cbd5e1;
}
.badge-phase { background: #0e4a5c; border-color: #22d3ee; color: #cffafe; font-weight: 700; }
.system-pills { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; }
.pill {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 6px;
  border: 1px solid transparent;
}
.pill-ok { background: #064e3b; color: #a7f3d0; border-color: #10b981; }
.pill-info { background: #172745; color: #93c5fd; border-color: #3b82f6; }
.pill-accent { background: #3b2806; color: #fde68a; border-color: #f59e0b; }
.pill-warn { background: #422006; color: #fde68a; border-color: #f59e0b; font-weight: 600; }
.pill-danger {
  background: #450a0a;
  color: #fecaca;
  border-color: #ef4444;
  font-weight: 700;
  box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.35);
}
#btn-quick-clean.is-warn { background: #92400e; border-color: #f59e0b; color: #fef3c7; font-weight: 600; }
#btn-quick-clean.is-over { background: #dc2626; border-color: #f87171; color: #ffffff; font-weight: 700; }

.checklist-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #101d35;
  border: 1px solid #23375c;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 11px;
}
.checklist-panel.hidden { display: none; }
.chk-row { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.chk-item { display: inline-flex; align-items: center; gap: 5px; cursor: pointer; color: #e2e8f0; }
.chk-hours, .chk-time {
  width: 155px;
  background: #070e1b;
  border: 1px solid #293b5f;
  color: #f1f5f9;
  padding: 3px 6px;
  border-radius: 5px;
  font-size: 11px;
}
.chk-time { width: 82px; text-align: center; }
.chk-sep { color: #475569; }
.chk-select {
  background: #070e1b;
  border: 1px solid #293b5f;
  color: #f1f5f9;
  padding: 3px 6px;
  border-radius: 5px;
  font-size: 11px;
}

.preflight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(238px, 1fr));
  gap: 5px;
  max-height: 215px;
  overflow-y: auto;
  padding-right: 2px;
}
.split-slot-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  background: #0d1a30;
  border: 1px solid #1e3a5f;
  border-radius: 7px;
  padding: 3px 8px;
  font-size: 11px;
}
.split-slot-bar.hidden { display: none; }
.slot-btn {
  background: #162847;
  border: 1px solid #2e4873;
  color: #cbd5e1;
  padding: 2px 7px;
  border-radius: 5px;
  font-size: 10px;
  cursor: pointer;
}
.slot-btn.active-slot {
  background: #0e7490;
  border-color: #22d3ee;
  color: #ffffff;
  font-weight: 700;
}
.zoom-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.acc-card {
  background: #101d35;
  border: 1px solid #23375c;
  border-radius: 8px;
  padding: 5px 7px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
  transition: border-color 120ms ease, background 120ms ease;
}
.acc-card:hover { border-color: #38bdf8; background: #132240; }
.acc-card.active-acc { border-color: #22d3ee; box-shadow: 0 0 0 1px rgba(34,211,238,0.28); }
.acc-card.tab1-collection-card {
  background: linear-gradient(135deg, #0b2239 0%, #102a45 100%);
  border: 1px solid #22d3ee;
  box-shadow: 0 0 0 1px rgba(34,211,238,0.24);
}
.acc-card.tab1-collection-card.split-active {
  border-color: #10b981;
  box-shadow: 0 0 0 1px rgba(16,185,129,0.45);
}
.acc-head { display: flex; align-items: center; justify-content: space-between; font-size: 11px; font-weight: 600; gap: 4px; cursor: pointer; }
.acc-name-wrap { display: inline-flex; align-items: center; gap: 3px; min-width: 0; flex: 1; }
.acc-name-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 110px; color: #f8fafc; }
.acc-name-input {
  width: 98px;
  background: #070e1b;
  border: 1px solid #22d3ee;
  color: #f8fafc;
  border-radius: 4px;
  padding: 1px 4px;
  font-size: 11px;
  font-weight: 600;
}
.btn-edit-name {
  background: transparent;
  border: none;
  color: #93c5fd;
  font-size: 11px;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
}
.btn-edit-name:hover { color: #22d3ee; }
.acc-tab-badge { font-size: 10px; color: #94a3b8; font-weight: 500; }
.acc-dot-login { width: 7px; height: 7px; border-radius: 50%; background: #f59e0b; flex-shrink: 0; }
.acc-dot-login.ok { background: #10b981; }
.acc-actions { display: flex; gap: 3px; flex-shrink: 0; align-items: center; }
.acc-autoklik-row {
  display: flex;
  align-items: center;
  gap: 3px;
  flex-wrap: wrap;
  font-size: 10px;
  background: #091326;
  border: 1px solid #1e3a5f;
  border-radius: 6px;
  padding: 3px 5px;
}
.acc-autoklik-row .btn-small {
  padding: 2px 5px;
  font-size: 10px;
}
.btn-small.btn-warn {
  background: #92400e;
  border-color: #f59e0b;
  color: #fef3c7;
  font-weight: 600;
}
.btn-small:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.ind { padding: 1px 5px; border-radius: 4px; background: #1a2c4e; color: #cbd5e1; font-size: 10px; }
.ind.ok { background: #064e3b; color: #6ee7b7; }
.ind.warn { background: #451a03; color: #fcd34d; }
.ind.err { background: #450a0a; color: #fca5a5; }
.tab-strip { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.tab-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: #182947;
  border: 1px solid #2b4470;
  color: #cbd5e1;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 5px;
  cursor: pointer;
}
.tab-pill.active { background: #0e7490; border-color: #22d3ee; color: #ffffff; font-weight: 600; }
.tab-pill.won { background: #065f46; border-color: #10b981; color: #ecfdf5; font-weight: 700; }
.tab-pill.running { border-color: #fbbf24; }
.tab-pill.captcha { background: #78350f; border-color: #f59e0b; color: #fef3c7; font-weight: 700; }
.tab-pill.logout { background: #7f1d1d; border-color: #f87171; color: #fee2e2; font-weight: 700; }
.tab-close { opacity: 0.7; font-weight: bold; margin-left: 2px; }
.tab-close:hover { opacity: 1; color: #fda4af; }
.btn-add-tab {
  margin-left: auto;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 5px;
  border: 1px solid #22d3ee;
  background: #0e4a5c;
  color: #cffafe;
  cursor: pointer;
}
.btn-add-tab:disabled {
  border-color: #475569;
  background: #1e293b;
  color: #94a3b8;
  cursor: not-allowed;
}
.modal-hint { color: #93c5fd; font-size: 11px; line-height: 1.4; margin-bottom: 4px; }
.nav-strip { display: flex; align-items: center; gap: 6px; flex-wrap: nowrap; overflow: hidden; min-height: 28px; }
.address-bar-wrap {
  flex: 1;
  min-width: 180px;
  display: flex;
  align-items: center;
  background: #070e1b;
  border: 1px solid #293b5f;
  border-radius: 6px;
  overflow: hidden;
}
.address-bar-wrap:focus-within {
  border-color: #22d3ee;
  box-shadow: 0 0 0 1px rgba(34, 211, 238, 0.25);
}
.url-preset-select {
  background: #101d35;
  border: none;
  border-right: 1px solid #293b5f;
  color: #cffafe;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  outline: none;
  max-width: 215px;
}
.url-preset-select:hover {
  background: #162847;
  color: #22d3ee;
}
.nav-strip input {
  flex: 1;
  background: transparent;
  border: none;
  color: #f1f5f9;
  padding: 4px 8px;
  font-size: 12px;
  outline: none;
}
.btn-small {
  background: #182947;
  border: 1px solid #304b7a;
  color: #e2e8f0;
  padding: 3px 7px;
  border-radius: 6px;
  font-size: 10px;
  cursor: pointer;
  flex-shrink: 0;
}
.btn-small.btn-primary { background: #0891b2; border-color: #22d3ee; color: #fff; font-weight: 600; }
.btn-small.btn-run { background: #059669; border-color: #10b981; color: #fff; font-weight: 600; }
.btn-small.btn-stop { background: #b91c1c; border-color: #f87171; color: #fff; font-weight: 600; }
.active-meta {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
  flex-shrink: 0;
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.78);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.modal.hidden { display: none; }
.modal-card {
  width: 440px;
  background: #0e182b;
  border: 1px solid #2b4470;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.modal-head { display: flex; justify-content: space-between; align-items: center; }
.modal-body { display: flex; flex-direction: column; gap: 6px; font-size: 12px; }
.modal-body input[type="text"],
.modal-body input[type="number"],
.modal-body input[type="password"],
.modal-body select {
  width: 100%;
  background: #070e1b;
  border: 1px solid #293b5f;
  color: #f1f5f9;
  padding: 6px 8px;
  border-radius: 6px;
}
.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.modal-foot { display: flex; justify-content: flex-end; }
.modal-card-wide { width: min(760px, 95vw); max-height: 92vh; }
.modal-scroll { overflow-y: auto; max-height: 78vh; padding-right: 4px; gap: 10px; }
.ak-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.ak-section {
  background: #091224;
  border: 1px solid #1f3458;
  border-radius: 8px;
  padding: 9px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.ak-grid-4 { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 7px; }
.ak-points-list { display: flex; flex-direction: column; gap: 5px; max-height: 170px; overflow-y: auto; }
.ak-point-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  background: #101d35;
  border: 1px solid #23375c;
  border-radius: 6px;
  padding: 5px 8px;
  font-size: 11px;
}
.ak-point-info { display: flex; align-items: center; gap: 6px; min-width: 0; flex: 1; flex-wrap: wrap; }
.ak-point-actions { display: flex; align-items: center; gap: 4px; }
.ep-ext-card { width: min(440px, 95vw); max-height: 92vh; background: #141c2b; border: 1px solid #2e4266; }
.ep-ext-head { border-bottom: 1px solid #22324f; padding-bottom: 8px; }
.ep-title-wrap h3 { font-size: 15px; color: #f8fafc; }
.ep-order-controls { display: flex; align-items: center; gap: 5px; }
.ep-order-lbl { font-size: 11px; color: #94a3b8; }
.ep-order-btn {
  background: #1d4ed8;
  border: 1px solid #3b82f6;
  color: #ffffff;
  border-radius: 5px;
  padding: 3px 7px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.ep-order-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.ep-ext-body { gap: 8px; }
.ep-top-meta-row { display: flex; justify-content: space-between; align-items: center; }
.ep-field-label { font-size: 11px; color: #cbd5e1; font-weight: 600; }
.ep-type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.ep-type-btn {
  background: #1c283d;
  border: 1px solid #33476b;
  color: #cbd5e1;
  border-radius: 7px;
  padding: 8px 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
}
.ep-type-btn:hover { border-color: #38bdf8; }
.ep-type-btn.active { background: #0284c7; border-color: #38bdf8; color: #ffffff; }
.ep-inline-card {
  background: #182336;
  border: 1px solid #293b5f;
  border-radius: 8px;
  padding: 7px 9px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ep-inline-title { font-size: 12px; font-weight: 600; color: #f1f5f9; }
.ep-num-sm { width: 64px !important; text-align: center; padding: 4px 6px !important; }
.ep-num-md { width: 84px !important; text-align: center; padding: 4px 6px !important; }
.ep-radio { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; cursor: pointer; color: #e2e8f0; }
.ep-slider-row input[type="range"] { flex: 1; accent-color: #38bdf8; }
.ep-purple-lbl { font-size: 11px; font-weight: 700; color: #c084fc; min-width: 115px; }
.ep-amber-lbl { font-size: 11px; font-weight: 700; color: #fbbf24; }
.ep-subcard {
  background: #0d192c;
  border: 1px solid #243b61;
  border-radius: 8px;
  padding: 9px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.ep-swipe-card { border-color: #0e7490; }
.ep-input-card { border-color: #3b4b6b; }
.ep-loop-card { border-color: #1e40af; }
.ep-row-between { display: flex; align-items: center; justify-content: space-between; gap: 8px; font-size: 11px; font-weight: 600; }
.ep-preset-row { display: grid; grid-template-columns: repeat(6, 1fr); gap: 5px; }
.ep-preset-row.ep-preset-4 { grid-template-columns: repeat(4, 1fr); }
.ep-preset-btn {
  background: #1b2840;
  border: 1px solid #30476e;
  color: #cbd5e1;
  border-radius: 5px;
  padding: 5px 4px;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
}
.ep-preset-btn.active { background: #0284c7; border-color: #38bdf8; color: #ffffff; }
.ep-swipe-summary { font-size: 11px; font-weight: 700; color: #34d399; text-align: center; }
.ep-popup-pin-box {
  background: #4c1d95;
  border: 1px solid #a855f7;
  border-radius: 7px;
  padding: 7px 9px;
  color: #f3e8ff;
  font-size: 11px;
}
.ep-cycle-header { display: flex; align-items: center; justify-content: space-between; font-size: 11px; font-weight: 700; color: #bae6fd; }
.ep-hint-sm { font-size: 10px; color: #94a3b8; line-height: 1.35; }
.ep-cycle-list { display: flex; flex-direction: column; gap: 5px; max-height: 130px; overflow-y: auto; }
.ep-cycle-row { display: flex; align-items: center; gap: 6px; }
.ep-cycle-idx { font-size: 11px; font-weight: 700; color: #34d399; min-width: 22px; }
.ep-loop-banner-btn {
  width: 100%;
  background: #065f46;
  border: 1px solid #10b981;
  color: #ecfdf5;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.ep-loop-banner-btn.inactive {
  background: #172745;
  border-color: #334e7d;
  color: #93c5fd;
}
.ep-advanced-details {
  background: #0a1324;
  border: 1px dashed #253b61;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 11px;
  color: #94a3b8;
}
.ep-advanced-details summary { cursor: pointer; font-weight: 600; color: #93c5fd; }
.ep-ext-foot { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 2px; }
.ep-foot-cancel {
  background: #2b3446;
  border: 1px solid #475569;
  color: #e2e8f0;
  border-radius: 7px;
  padding: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.ep-foot-save {
  background: #059669;
  border: 1px solid #10b981;
  color: #ffffff;
  border-radius: 7px;
  padding: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.ak-json-box {
  width: 100%;
  background: #070e1b;
  border: 1px solid #293b5f;
  color: #cbd5e1;
  border-radius: 6px;
  padding: 6px 8px;
  font-family: Consolas, monospace;
  font-size: 11px;
  resize: vertical;
}
    """.trimIndent() + "\n"

  fun rendererJs(): String =
    """
let currentSnapshot = null;
let editingAccountId = null;
let renamingAccountId = null;
let renamingCollectionIndex = null;
let preflightGridVisible = true;
let extAccountId = 1;
let extTabIndex = 1;
let extEditingPointId = null;
let checklistSyncedOnce = false;
let compactModeInitialized = false;

function applyCompactMode(enabled) {
  document.body.classList.toggle('compact-mode', Boolean(enabled));
  const chk = document.getElementById('chk-compact-mode');
  if (chk) chk.checked = Boolean(enabled);
  try {
    localStorage.setItem('ferizy_compact_mode', enabled ? '1' : '0');
  } catch (_) {}
  requestAnimationFrame(syncHeaderHeight);
}

function playAlarmTone(alarm) {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const patterns = {
      lintasan: [523, 659],
      kuota: [659, 784, 988],
      menang: [523, 659, 784, 1046],
      logout: [330, 262, 220],
      proxy: [293, 293, 220],
      captcha: [880, 440, 880]
    };
    const notes = patterns[alarm] || [600];
    let t = ctx.currentTime;
    notes.forEach((freq) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = alarm === 'logout' || alarm === 'proxy' ? 'sawtooth' : 'triangle';
      osc.frequency.setValueAtTime(freq, t);
      gain.gain.setValueAtTime(0.14, t);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.14);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t);
      osc.stop(t + 0.15);
      t += 0.16;
    });
  } catch (_) {}
}

let lastSentHeaderHeight = 0;
let lastSentModalOpen = null;

function syncHeaderHeight() {
  const header = document.getElementById('shell-header');
  if (header && window.ferizyShell) {
    const h = Math.ceil(header.getBoundingClientRect().height);
    if (h >= 80 && Math.abs(h - lastSentHeaderHeight) >= 2) {
      lastSentHeaderHeight = h;
      window.ferizyShell.setHeaderHeight(h);
    }
  }
}

function syncModalOverlayState() {
  const anyOpen = Boolean(document.querySelector('.modal:not(.hidden)'));
  if (anyOpen !== lastSentModalOpen && window.ferizyShell && typeof window.ferizyShell.setModalOpen === 'function') {
    lastSentModalOpen = anyOpen;
    window.ferizyShell.setModalOpen(anyOpen);
  }
}

function openModalById(modalId) {
  document.querySelectorAll('.modal').forEach((m) => {
    if (m.id !== modalId) m.classList.add('hidden');
  });
  const el = document.getElementById(modalId);
  if (el) el.classList.remove('hidden');
  syncModalOverlayState();
}

function closeModalById(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.add('hidden');
  syncModalOverlayState();
}

function closeAllModals() {
  document.querySelectorAll('.modal').forEach((m) => m.classList.add('hidden'));
  syncModalOverlayState();
}

document.querySelectorAll('.modal').forEach((modalEl) => {
  modalEl.addEventListener('click', (e) => {
    if (e.target === modalEl) {
      closeModalById(modalEl.id);
    }
  });
});

window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && document.querySelector('.modal:not(.hidden)')) {
    closeAllModals();
  }
});

function renderSnapshot(snap) {
  currentSnapshot = snap;
  if (!snap) return;

  if (!compactModeInitialized) {
    compactModeInitialized = true;
    let storedCompact = null;
    try {
      storedCompact = localStorage.getItem('ferizy_compact_mode');
    } catch (_) {}
    const initialCompact = storedCompact !== null ? storedCompact === '1' : Boolean(snap.compactHeaderMode);
    applyCompactMode(initialCompact);
  }

  document.getElementById('total-tabs-badge').textContent =
    `${'$'}{snap.accounts.length} Akun • ${'$'}{snap.totalTabs}/${'$'}{snap.maxTotalTabs} Tab`;
  document.getElementById('ntfy-badge').textContent = `ntfy: ${'$'}{snap.ntfyTopic}`;
  document.getElementById('blocker-pill').textContent = `Asset Blocker: ${'$'}{snap.assetBlockerMode}`;
  document.getElementById('watchdog-pill').textContent = `Watchdog: ${'$'}{snap.memoryWatchdogLimitMb}MB • Queue ${'$'}{snap.staggeredReloadDelayMs}ms (Recycle: ${'$'}{snap.recycleCount || 0})`;

  const zoomPct = snap.globalZoomPercent || 100;
  document.getElementById('btn-zoom-reset').textContent = `${'$'}{zoomPct}%`;
  const dispZoomLabel = document.getElementById('disp-zoom-label');
  const dispZoomSlider = document.getElementById('disp-zoom-slider');
  if (dispZoomLabel) dispZoomLabel.textContent = `${'$'}{zoomPct}%`;
  if (dispZoomSlider) dispZoomSlider.value = String(zoomPct);

  const splitMode = snap.splitMode || 1;
  document.querySelectorAll('.disp-split-preset').forEach((btn) => {
    const val = Number(btn.getAttribute('data-split')) || 1;
    btn.className = val === splitMode ? 'btn-small btn-primary disp-split-preset' : 'btn-small disp-split-preset';
  });

  const addAccBtn = document.getElementById('btn-add-account');
  if (addAccBtn) {
    const maxAcc = snap.maxDynamicAccounts || 12;
    addAccBtn.disabled = snap.accounts.length >= maxAcc;
    addAccBtn.textContent = snap.accounts.length >= maxAcc ? `Max ${'$'}{maxAcc}` : '+ Akun';
  }

  const activeAccId = snap.activeAccountId || 1;
  const activeAccObj = snap.accounts.find((a) => a.accountId === activeAccId);
  const activeAccLabel = (activeAccObj && activeAccObj.name) || `Akun ${'$'}{activeAccId}`;
  const slotBar = document.getElementById('split-slot-bar');
  if (slotBar) {
    const activeCollectionGroup = Number(snap.tabCollectionSplitGroup) || (snap.tab1CollectionSplitActive ? 1 : 0);
    if (activeCollectionGroup >= 1 && activeCollectionGroup <= 3) {
      const groupListKey = activeCollectionGroup === 1
        ? 'tab1CollectionTabs'
        : activeCollectionGroup === 2
          ? 'tab2CollectionTabs'
          : 'tab3CollectionTabs';
      const groupItems = Array.isArray(snap[groupListKey]) ? snap[groupListKey] : [];
      slotBar.classList.remove('hidden');
      slotBar.innerHTML = `<span class="pill pill-ok">▥ Split Vertikal Kumpulan Tab ${'$'}{activeCollectionGroup} (${'$'}{groupItems.length} Akun Sejajar • Mirror 0ms • AutoKlik Putus Otomatis)</span>`;
      groupItems.forEach((tItem) => {
        const b = document.createElement('button');
        const isAutoKlikRunning = tItem.runnerState === 'RUNNING';
        b.className = `slot-btn ${'$'}{tItem.isActiveMaster ? 'active-slot' : ''}`;
        b.textContent = `${'$'}{tItem.isActiveMaster ? '★ Master ' : ''}A${'$'}{tItem.accountId}•T${'$'}{activeCollectionGroup}${'$'}{isAutoKlikRunning ? ' (⚡Putus)' : ''}`;
        b.title = isAutoKlikRunning
          ? `${'$'}{tItem.accountName} (Tab ${'$'}{activeCollectionGroup}) sedang menjalankan AutoKlik -> Otomatis PUTUS dari Mirror`
          : `Klik untuk menjadikan ${'$'}{tItem.accountName} (Tab ${'$'}{activeCollectionGroup}) sebagai Master Mirror Tab ${'$'}{activeCollectionGroup}`;
        b.onclick = () => window.ferizyShell.switchTab(tItem.tabId);
        slotBar.appendChild(b);
      });
      const exitSplitBtn = document.createElement('button');
      exitSplitBtn.className = 'btn-small';
      exitSplitBtn.textContent = '1 Layar Penuh';
      exitSplitBtn.title = `Tutup tampilan Split Vertikal Kumpulan Tab ${'$'}{activeCollectionGroup} dan kembali ke 1 layar penuh`;
      exitSplitBtn.onclick = () => window.ferizyShell.setTabCollectionSplit(activeCollectionGroup, false);
      slotBar.appendChild(exitSplitBtn);

      const firstPane = Array.isArray(snap.paneBounds) && snap.paneBounds[0];
      const paneTxt = firstPane ? `${'$'}{firstPane.width}x${'$'}{firstPane.height}px/kolom` : 'Split Vertikal Rata';
      const info = document.createElement('span');
      info.className = 'active-meta';
      info.textContent = `${'$'}{paneTxt} • Tab yang Start AutoKlik otomatis putus Mirror • Zoom ${'$'}{zoomPct}%`;
      slotBar.appendChild(info);
    } else if (splitMode > 1) {
      slotBar.classList.remove('hidden');
      slotBar.innerHTML = `<span class="pill pill-info">Split ${'$'}{activeAccLabel} (${'$'}{splitMode} Layar Mandiri)</span>`;
      const slots = Array.isArray(snap.splitSlots) ? snap.splitSlots : [];
      for (let i = 0; i < splitMode; i += 1) {
        const slot = slots[i];
        const b = document.createElement('button');
        if (slot) {
          b.className = `slot-btn ${'$'}{slot.slotIndex === snap.activeSlotIndex ? 'active-slot' : ''}`;
          b.textContent = `Slot ${'$'}{i + 1}: Akun ${'$'}{activeAccId} • T${'$'}{slot.indexInAccount || (i + 1)}`;
          b.title = `Klik untuk memilih Slot ${'$'}{i + 1} (Akun ${'$'}{activeAccId})`;
          b.onclick = () => window.ferizyShell.setActiveSlot(i);
        } else {
          b.className = 'slot-btn';
          b.textContent = `+ Buka Tab ${'$'}{i + 1} (Akun ${'$'}{activeAccId})`;
          b.title = `Buka tab baru di dalam Akun ${'$'}{activeAccId} untuk mengisi Slot ${'$'}{i + 1}`;
          b.onclick = () => window.ferizyShell.createTab(activeAccId);
        }
        slotBar.appendChild(b);
      }
      const firstPane = Array.isArray(snap.paneBounds) && snap.paneBounds[0];
      const paneTxt = firstPane ? `${'$'}{firstPane.width}x${'$'}{firstPane.height}px/panel` : 'Bagi rata';
      const info = document.createElement('span');
      info.className = 'active-meta';
      info.textContent = `${'$'}{paneTxt} • Hanya tab Akun ${'$'}{activeAccId} • Zoom ${'$'}{zoomPct}%`;
      slotBar.appendChild(info);
    } else {
      slotBar.classList.add('hidden');
      slotBar.innerHTML = '';
    }
  }

  if (snap.enduranceSummary || snap.logStats) {
    const es = snap.enduranceSummary || {};
    const ls = snap.logStats || {};
    const endPill = document.getElementById('endurance-pill');
    endPill.textContent =
      `RAM: ${'$'}{es.lastAppRamMb || 0}MB (Peak ${'$'}{es.peakAppRamMb || 0}MB) • Log: ${'$'}{ls.totalDiskMb || 0}MB/${'$'}{ls.maxSizeMb || 50}MB (${'$'}{ls.retentionDays || 7}h) • ${'$'}{es.uptimeText || '00j'}`;
    endPill.className = es.healthStatus === 'PRESSURE' ? 'pill pill-accent' : 'pill pill-ok';
  }

  if (snap.clockSync) {
    const sign = snap.clockSync.offsetMs >= 0 ? '+' : '';
    document.getElementById('clock-sync-pill').textContent =
      `Clock Sync: ${'$'}{sign}${'$'}{snap.clockSync.offsetMs}ms (RTT ${'$'}{snap.clockSync.rttMs}ms)`;
  }

  if (snap.optionalModules && snap.optionalModules.scheduleTimer) {
    const st = snap.optionalModules.scheduleTimer;
    const timerPill = document.getElementById('shot-timer-pill');
    if (st.scheduledShotEnabled && typeof st.remainingMs === 'number') {
      const sec = Math.ceil(st.remainingMs / 1000);
      timerPill.textContent = `Tembak ${'$'}{st.scheduledShotTimeWib} (-${'$'}{sec}s)`;
      timerPill.className = 'pill pill-accent';
    } else {
      timerPill.textContent = 'Timer Tembak: OFF';
      timerPill.className = 'pill pill-info';
    }
  }

  const pausedCaptchaCount = snap.accounts.reduce(
    (sum, acc) => sum + acc.tabs.filter((t) => t.runnerState === 'PAUSED_CAPTCHA').length,
    0
  );
  const resumeCaptchaBtn = document.getElementById('btn-resume-captcha');
  resumeCaptchaBtn.textContent = `Lanjut CAPTCHA (${'$'}{pausedCaptchaCount})`;
  resumeCaptchaBtn.className = pausedCaptchaCount > 0 ? 'btn-small btn-primary' : 'btn-small';

  if (snap.notificationSettings) {
    const muteBtn = document.getElementById('btn-mute-alarm');
    muteBtn.textContent = snap.notificationSettings.audioAlarmMuted ? 'Suara: MUTE' : 'Suara: ON';
    muteBtn.className = snap.notificationSettings.audioAlarmMuted ? 'btn-small btn-stop' : 'btn-small';

    const ev = snap.notificationSettings.events || {};
    const captchaOn = ev.CAPTCHA_TERDETEKSI === true;
    const capNotifBtn = document.getElementById('btn-toggle-captcha-notif');
    if (capNotifBtn) {
      capNotifBtn.textContent = captchaOn ? 'Notif CAPTCHA: ON' : 'Notif CAPTCHA: OFF';
      capNotifBtn.className = captchaOn ? 'btn-small btn-run' : 'btn-small';
    }

    const enabledCount = [
      ev.LINTASAN_DIBUKA !== false,
      ev.KUOTA_MUNCUL !== false,
      ev.TEMBUS_VERIFIKASI !== false,
      ev.LOGOUT_TERDETEKSI === true,
      ev.PROXY_PUTUS === true,
      captchaOn
    ].filter(Boolean).length;
    document.getElementById('btn-open-notif-modal').textContent = `Notif HP (${'$'}{enabledCount}/6)`;
  }

  if (snap.optionalModules && !checklistSyncedOnce) {
    checklistSyncedOnce = true;
    const om = snap.optionalModules;
    document.getElementById('chk-keep-alive').checked = Boolean(om.gentleKeepAliveEnabled);
    document.getElementById('sel-keepalive-min').value = String(om.keepAliveIntervalMinutes || 12);
    document.getElementById('sel-autowake-mode').value = om.autoWakeMode || 'same_account';
    document.getElementById('chk-captcha-autopop').checked = Boolean(om.captchaAutoPopEnabled);
    document.getElementById('chk-smart-matcher').checked = Boolean(om.smartTargetMatcherEnabled);
    document.getElementById('sel-service-type').value = om.smartMatcherServiceType || 'all';
    document.getElementById('chk-matcher-fallback').checked = Boolean(om.smartMatcherFallbackNearest);
    document.getElementById('input-priority-hours').value = om.priorityHoursCsv || '08:00,10:00,12:00,14:00,20:00';
    document.getElementById('chk-latency-comp').checked = Boolean(om.latencyCompensationEnabled);
    document.getElementById('sel-fast-timeout').value = String(om.fastRequestTimeoutMs || 4500);
    document.getElementById('sel-advanced-stop').value = om.advancedStopMode || 'per_tab';
    if (om.scheduleTimer) {
      document.getElementById('chk-sched-shot').checked = Boolean(om.scheduleTimer.scheduledShotEnabled);
      document.getElementById('input-sched-time').value = om.scheduleTimer.scheduledShotTimeWib || '00:00:00';
    }
    if (snap.notificationSettings) {
      const ns = snap.notificationSettings;
      document.getElementById('input-ntfy-server').value = ns.ntfyServerUrl || 'https://ntfy.sh';
      document.getElementById('sel-notif-dedupe').value = String(ns.dedupeSeconds || 10);
      if (ns.events) {
        document.getElementById('chk-notif-lintasan').checked = ns.events.LINTASAN_DIBUKA !== false;
        document.getElementById('chk-notif-kuota').checked = ns.events.KUOTA_MUNCUL !== false;
        document.getElementById('chk-notif-tembus').checked = ns.events.TEMBUS_VERIFIKASI !== false;
        document.getElementById('chk-notif-logout').checked = ns.events.LOGOUT_TERDETEKSI === true;
        document.getElementById('chk-notif-proxy').checked = ns.events.PROXY_PUTUS === true;
        document.getElementById('chk-notif-captcha').checked = ns.events.CAPTCHA_TERDETEKSI === true;
      }
    }
    if (snap.logStats) {
      document.getElementById('sel-log-preset').value = snap.logStats.maxSizeMb <= 20 ? '20mb_3d' : '50mb_7d';
    }
    if (snap.enduranceSummary) {
      document.getElementById('sel-watchdog-cal').value = String(snap.enduranceSummary.watchdogLimitMb || 350);
      document.getElementById('chk-health-report').checked = snap.enduranceSummary.healthReportEnabled !== false;
    }
  }

  const grid = document.getElementById('preflight-grid');
  if (grid) {
    grid.style.display = preflightGridVisible ? 'grid' : 'none';
    grid.innerHTML = '';
  }

  let activeTabObj = null;
  for (const acc of snap.accounts) {
    const foundActive = acc.tabs.find((t) => t.tabId === snap.activeTabId);
    if (foundActive) {
      activeTabObj = foundActive;
      break;
    }
  }

  if (extAccountId && !document.getElementById('autoklik-ext-modal').classList.contains('hidden')) {
    refreshAutoklikExtModalFromSnapshot(snap, false);
  }

  // 3 Kotak Kontainer Khusus Kumpulan Tab 1, Kumpulan Tab 2, dan Kumpulan Tab 3 (Master Mirror + Split Vertikal + Auto-Disconnect saat AutoKlik Start)
  const activeIdxForMirror = Number(snap.activeTabIndex) || (activeTabObj && activeTabObj.indexInAccount) || 1;
  const activeCollectionSplitGroup = Number(snap.tabCollectionSplitGroup) || (snap.tab1CollectionSplitActive ? 1 : 0);
  const groupsMirrorMeta = (snap && snap.tabGroupsMirror) || {};

  [1, 2, 3].forEach((groupIdx) => {
    const listKey = groupIdx === 1 ? 'tab1CollectionTabs' : groupIdx === 2 ? 'tab2CollectionTabs' : 'tab3CollectionTabs';
    const groupMeta = groupsMirrorMeta[groupIdx] || groupsMirrorMeta[String(groupIdx)] || {};
    const collectionList = Array.isArray(snap[listKey]) && snap[listKey].length > 0
      ? snap[listKey]
      : (Array.isArray(groupMeta.tabs) && groupMeta.tabs.length > 0
          ? groupMeta.tabs
          : snap.accounts
              .map((acc) => {
                const tMatch = acc.tabs.find((t) => t.indexInAccount === groupIdx);
                if (!tMatch) return null;
                return {
                  tabId: tMatch.tabId,
                  accountId: acc.accountId,
                  accountName: acc.name || `Akun ${'$'}{acc.accountId}`,
                  indexInAccount: groupIdx,
                  runnerState: tMatch.runnerState,
                  pointsCount: tMatch.pointsCount || 0,
                  isActiveMaster: tMatch.tabId === snap.activeTabId,
                  isMirrorDisconnectedByAutoKlik: tMatch.runnerState === 'RUNNING'
                };
              })
              .filter(Boolean));

    const isGroupMirrorEnabled = groupIdx === 1
      ? (groupMeta.enabled !== undefined ? Boolean(groupMeta.enabled) : Boolean(snap.tab1MirrorEnabled))
      : Boolean(groupMeta.enabled);
    const isGroupMirrorActiveNow = groupIdx === 1
      ? (groupMeta.active !== undefined ? Boolean(groupMeta.active) : Boolean(snap.tab1MirrorActive && activeIdxForMirror === 1))
      : Boolean(groupMeta.active);
    const isGroupSplitOn = activeCollectionSplitGroup === groupIdx;
    const autoKlikDisconnectedCount = collectionList.filter((item) => item && item.runnerState === 'RUNNING').length;

    const groupCard = document.createElement('div');
    groupCard.id = `tab${'$'}{groupIdx}-collection-card`;
    groupCard.className = `acc-card tab1-collection-card ${'$'}{isGroupSplitOn ? 'split-active' : ''} ${'$'}{activeIdxForMirror === groupIdx ? 'active-acc' : ''}`;
    groupCard.title = `Kotak Kontainer Khusus Kumpulan Tab ${'$'}{groupIdx}: Klik area kotak untuk membuka/menutup tampilan Split Vertikal semua Tab ${'$'}{groupIdx}`;
    groupCard.addEventListener('click', (e) => {
      if (e.target && e.target.closest && e.target.closest('button, input, select, a, .tab-pill')) {
        return;
      }
      window.ferizyShell.toggleTabCollectionSplit(groupIdx);
    });

    const gHead = document.createElement('div');
    gHead.className = 'acc-head';

    const gNameWrap = document.createElement('div');
    gNameWrap.className = 'acc-name-wrap';

    const gDot = document.createElement('span');
    gDot.className = `acc-dot-login ${'$'}{isGroupMirrorActiveNow ? 'ok' : ''}`;
    gDot.title = isGroupMirrorActiveNow
      ? `Mirror Kumpulan Tab ${'$'}{groupIdx} Aktif (0ms Real-Time)`
      : `Mirror Kumpulan Tab ${'$'}{groupIdx} Nonaktif / Mandiri`;
    gNameWrap.appendChild(gDot);

    const groupName = groupMeta.name || `📦 Kumpulan Tab ${'$'}{groupIdx}`;
    if (renamingCollectionIndex === groupIdx) {
      const nameInp = document.createElement('input');
      nameInp.type = 'text';
      nameInp.maxLength = 32;
      nameInp.className = 'acc-name-input';
      nameInp.value = groupName;
      const commitCollectionRename = async () => {
        const nextVal = nameInp.value.trim();
        renamingCollectionIndex = null;
        await window.ferizyShell.renameCollection(groupIdx, nextVal);
      };
      nameInp.addEventListener('keydown', (ev) => {
        ev.stopPropagation();
        if (ev.key === 'Enter') {
          ev.preventDefault();
          commitCollectionRename();
        } else if (ev.key === 'Escape') {
          ev.preventDefault();
          renamingCollectionIndex = null;
          renderSnapshot(currentSnapshot);
        }
      });
      nameInp.addEventListener('blur', () => {
        if (renamingCollectionIndex === groupIdx) commitCollectionRename();
      });
      nameWrap.appendChild(nameInp);
      setTimeout(() => { try { nameInp.focus(); nameInp.select(); } catch (_) {} }, 10);
    } else {
      const gTitle = document.createElement('span');
      gTitle.className = 'acc-name-text';
      gTitle.style.maxWidth = '135px';
      gTitle.textContent = groupName;
      gTitle.title = `Klik dua kali untuk mengubah nama kontainer Kumpulan Tab ${'$'}{groupIdx}`;
      gTitle.ondblclick = (e) => {
        e.stopPropagation();
        renamingCollectionIndex = groupIdx;
        renderSnapshot(currentSnapshot);
      };
      nameWrap.appendChild(gTitle);
    }

    const gCountBadge = document.createElement('span');
    gCountBadge.className = 'acc-tab-badge';
    gCountBadge.textContent = `(${'$'}{collectionList.length}/${'$'}{snap.accounts.length} Tab)`;
    gNameWrap.appendChild(gCountBadge);
    gHead.appendChild(gNameWrap);

    const gActions = document.createElement('div');
    gActions.className = 'acc-actions';

    const gReloadBtn = document.createElement('button');
    gReloadBtn.type = 'button';
    gReloadBtn.className = 'btn-small btn-primary';
    gReloadBtn.textContent = '🔄 Muat Ulang';
    gReloadBtn.title = `Muat ulang (reload) seluruh Tab ${'$'}{groupIdx} lintas akun secara serempak`;
    gReloadBtn.onclick = async (e) => {
      e.stopPropagation();
      await window.ferizyShell.reloadCollection(groupIdx);
      document.getElementById('active-meta').textContent = `🔄 Seluruh Tab ${'$'}{groupIdx} berhasil dimuat ulang (reload).`;
    };
    gActions.appendChild(gReloadBtn);

    const gStartAllBtn = document.createElement('button');
    gStartAllBtn.type = 'button';
    gStartAllBtn.className = 'btn-small btn-run';
    gStartAllBtn.textContent = '▶ Start All';
    gStartAllBtn.title = `Jalankan AutoKlik untuk semua Tab ${'$'}{groupIdx} yang memiliki titik konfigurasi, dan otomatis putus Mirror untuk tab yang running.`;
    gStartAllBtn.onclick = async (e) => {
      e.stopPropagation();
      const res = await window.ferizyShell.startAllCollection(groupIdx);
      document.getElementById('active-meta').textContent = `▶ Start All Kumpulan Tab ${'$'}{groupIdx}: Berjalan pada ${'$'}{res.startedCount} tab.`;
    };
    gActions.appendChild(gStartAllBtn);

    const gSplitBtn = document.createElement('button');
    gSplitBtn.id = `btn-toggle-tab${'$'}{groupIdx}-split`;
    gSplitBtn.type = 'button';
    gSplitBtn.className = isGroupSplitOn ? 'btn-small btn-run' : 'btn-small btn-primary';
    gSplitBtn.textContent = isGroupSplitOn ? '▥ Split Vertikal: ON' : '▥ Split Vertikal';
    gSplitBtn.title = isGroupSplitOn
      ? `Tampilan Split Vertikal semua Tab ${'$'}{groupIdx} sedang AKTIF (Klik untuk kembali ke 1 layar penuh)`
      : `Tampilkan semua Tab ${'$'}{groupIdx} lintas akun secara Split Vertikal berdampingan dalam 1 layar (Otomatis membuka Tab ${'$'}{groupIdx} di semua akun jika belum terbuka)`;
    gSplitBtn.onclick = async (e) => {
      e.stopPropagation();
      const res = await window.ferizyShell.toggleTabCollectionSplit(groupIdx);
      if (res) {
        const isOn = Number(res.tabCollectionSplitGroup) === groupIdx;
        document.getElementById('active-meta').textContent = isOn
          ? `▥ Split Vertikal Kumpulan Tab ${'$'}{groupIdx} AKTIF (Semua Tab ${'$'}{groupIdx} tampil berdampingan real-time 0ms).`
          : `▥ Split Vertikal Kumpulan Tab ${'$'}{groupIdx} ditutup (kembali ke tampilan normal).`;
      }
    };
    gActions.appendChild(gSplitBtn);
    gHead.appendChild(gActions);
    groupCard.appendChild(gHead);

    const gControlRow = document.createElement('div');
    gControlRow.className = 'acc-autoklik-row';

    const mirrorBtnEl = document.createElement('button');
    mirrorBtnEl.id = `btn-toggle-tab${'$'}{groupIdx}-mirror`;
    mirrorBtnEl.type = 'button';
    if (isGroupMirrorActiveNow) {
      mirrorBtnEl.textContent = `🔗 Mirror Tab ${'$'}{groupIdx}: ON`;
      mirrorBtnEl.className = 'btn-small btn-run';
      mirrorBtnEl.title = `Mirror Tab ${'$'}{groupIdx} AKTIF (0ms): Klik, popup pilihan, swipe/wheel, scroll & ketik di Master Tab ${'$'}{groupIdx} diikuti real-time oleh Tab ${'$'}{groupIdx} lain yang TIDAK sedang Start AutoKlik.`;
    } else if (isGroupMirrorEnabled && activeIdxForMirror !== groupIdx) {
      mirrorBtnEl.textContent = `🔗 Mirror Tab ${'$'}{groupIdx}: SIAGA`;
      mirrorBtnEl.className = 'btn-small btn-warn';
      mirrorBtnEl.title = `Mirror Tab ${'$'}{groupIdx} dalam posisi ON (Siaga). Klik salah satu pil A1•T${'$'}{groupIdx} di bawah atau aktifkan Split Vertikal Kumpulan Tab ${'$'}{groupIdx} untuk menggerakkan Master Tab ${'$'}{groupIdx}.`;
    } else {
      mirrorBtnEl.textContent = `🔗 Mirror Tab ${'$'}{groupIdx}: OFF`;
      mirrorBtnEl.className = 'btn-small';
      mirrorBtnEl.title = `Mirror Tab ${'$'}{groupIdx} sedang OFF (Mandiri). Klik untuk mengaktifkan sinkronisasi Kumpulan Tab ${'$'}{groupIdx}.`;
    }
    mirrorBtnEl.onclick = async (e) => {
      e.stopPropagation();
      const res = await window.ferizyShell.toggleTabGroupMirror(groupIdx);
      if (res && !res.ok && res.reason) {
        document.getElementById('active-meta').textContent = res.reason;
      } else if (res) {
        document.getElementById('active-meta').textContent = res.enabled
          ? `🔗 Mirror Tab ${'$'}{groupIdx}: ON (0ms) — Semua Tab ${'$'}{groupIdx} yang tidak sedang Start AutoKlik bergerak serempak.`
          : `🔗 Mirror Tab ${'$'}{groupIdx}: OFF — Semua Tab ${'$'}{groupIdx} beroperasi 100% mandiri.`;
      }
    };
    gControlRow.appendChild(mirrorBtnEl);

    const gInfoBadge = document.createElement('span');
    gInfoBadge.className = `ind ${'$'}{isGroupMirrorActiveNow ? 'ok' : (autoKlikDisconnectedCount > 0 ? 'warn' : '')}`;
    gInfoBadge.style.marginLeft = 'auto';
    if (autoKlikDisconnectedCount > 0 && isGroupMirrorEnabled) {
      gInfoBadge.textContent = `⚡ Putus (${'$'}{autoKlikDisconnectedCount} AutoKlik)`;
      gInfoBadge.title = `${'$'}{autoKlikDisconnectedCount} Tab ${'$'}{groupIdx} sedang Start AutoKlik -> Otomatis PUTUS dari Mirror agar eksekusi AutoKlik tidak terganggu.`;
    } else {
      gInfoBadge.textContent = isGroupMirrorActiveNow ? '⚡ 0ms Sync' : 'Mandiri';
      gInfoBadge.title = `Saat salah satu/semua Tab ${'$'}{groupIdx} Start AutoKlik, otomatis fitur Mirror putus untuk tab yang sedang Start AutoKlik.`;
    }
    gControlRow.appendChild(gInfoBadge);
    groupCard.appendChild(gControlRow);

    const gStrip = document.createElement('div');
    gStrip.className = 'tab-strip';
    if (collectionList.length === 0) {
      const openAllBtn = document.createElement('button');
      openAllBtn.type = 'button';
      openAllBtn.className = 'btn-small btn-primary';
      openAllBtn.textContent = `+ Buka Semua Tab ${'$'}{groupIdx}`;
      openAllBtn.title = `Buka Tab ${'$'}{groupIdx} di seluruh akun & tampilkan Split Vertikal`;
      openAllBtn.onclick = (e) => {
        e.stopPropagation();
        window.ferizyShell.setTabCollectionSplit(groupIdx, true);
      };
      gStrip.appendChild(openAllBtn);
    } else {
      collectionList.forEach((tItem) => {
        const pill = document.createElement('div');
        const isRunningAk = tItem.runnerState === 'RUNNING';
        const stateCls =
          tItem.runnerState === 'WON_STOPPED'
            ? 'won'
            : tItem.runnerState === 'PAUSED_CAPTCHA'
              ? 'captcha'
              : tItem.runnerState === 'LOGOUT_STOPPED'
                ? 'logout'
                : isRunningAk
                  ? 'running'
                  : '';
        pill.className = `tab-pill ${'$'}{tItem.tabId === snap.activeTabId ? 'active' : ''} ${'$'}{stateCls}`;
        pill.title = isRunningAk
          ? `${'$'}{tItem.accountName} • Tab ${'$'}{groupIdx} sedang START AUTOKLIK -> Otomatis PUTUS dari Mirror`
          : `${'$'}{tItem.accountName} • Tab ${'$'}{groupIdx} (${'$'}{tItem.pointsCount || 0} titik) — Klik untuk jadikan Master Tab ${'$'}{groupIdx}`;
        pill.onclick = (e) => {
          e.stopPropagation();
          window.ferizyShell.switchTab(tItem.tabId);
        };
        const ptsSuffix = tItem.pointsCount ? `(${'$'}{tItem.pointsCount})` : '';
        const masterStar = tItem.tabId === snap.activeTabId && isGroupMirrorActiveNow && !isRunningAk ? '★' : '';
        const akPutusTag = isRunningAk ? '⚡Putus' : '';
        const lbl = document.createElement('span');
        lbl.textContent = `${'$'}{masterStar}A${'$'}{tItem.accountId}•T${'$'}{groupIdx}${'$'}{ptsSuffix}${'$'}{akPutusTag}`;
        pill.appendChild(lbl);
        gStrip.appendChild(pill);
      });
    }
    groupCard.appendChild(gStrip);
    grid.appendChild(groupCard);
  });

  snap.accounts.forEach((acc) => {
    const accLabel = acc.name || `Akun ${'$'}{acc.accountId}`;
    const isCurrentAccount = acc.accountId === activeAccId || Boolean(acc.isActiveAccount);
    const hasActiveTab = acc.tabs.some((t) => t.tabId === snap.activeTabId);
    const accActiveTabIdx = acc.activeTabIndex || 1;
    const accActiveTab = acc.tabs.find((t) => t.indexInAccount === accActiveTabIdx) || acc.tabs[0] || null;
    const activePtsCount = accActiveTab ? (accActiveTab.pointsCount ?? acc.pointsCount ?? 0) : (acc.pointsCount || 0);
    const extCfg = (accActiveTab && accActiveTab.extensionSettings) || acc.extensionSettings || { enabled: true, loopEnabled: true, markersHidden: false, pointsLocked: false };
    const isHidden = extCfg.markersHidden === true;
    const isLocked = extCfg.pointsLocked === true;

    const card = document.createElement('div');
    card.className = `acc-card ${'$'}{isCurrentAccount || hasActiveTab ? 'active-acc' : ''}`;
    card.title = `Klik bagian atas/kosong kotak untuk pindah ke ${'$'}{accLabel}`;
    card.addEventListener('click', (e) => {
      if (e.target && e.target.closest && e.target.closest('button, input, select, a, .tab-pill, .btn-add-tab')) {
        return;
      }
      window.ferizyShell.switchAccount(acc.accountId);
    });

    // Baris 1: Nama Akun (Bisa di-edit), Tab Count, Split Tunggal, Proxy/Geo, Hapus (>5)
    const head = document.createElement('div');
    head.className = 'acc-head';

    const nameWrap = document.createElement('div');
    nameWrap.className = 'acc-name-wrap';

    const loginDot = document.createElement('span');
    loginDot.className = `acc-dot-login ${'$'}{acc.loginActive ? 'ok' : ''}`;
    loginDot.title = acc.loginActive ? 'Sesi Login Ferizy Aktif' : 'Belum Login / Standby';
    nameWrap.appendChild(loginDot);

    if (renamingAccountId === acc.accountId) {
      const nameInp = document.createElement('input');
      nameInp.type = 'text';
      nameInp.maxLength = 32;
      nameInp.className = 'acc-name-input';
      nameInp.value = accLabel;
      const commitRename = async () => {
        const nextVal = nameInp.value.trim();
        renamingAccountId = null;
        await window.ferizyShell.renameAccount(acc.accountId, nextVal);
      };
      nameInp.addEventListener('keydown', (ev) => {
        ev.stopPropagation();
        if (ev.key === 'Enter') {
          ev.preventDefault();
          commitRename();
        } else if (ev.key === 'Escape') {
          ev.preventDefault();
          renamingAccountId = null;
          renderSnapshot(currentSnapshot);
        }
      });
      nameInp.addEventListener('blur', () => {
        if (renamingAccountId === acc.accountId) commitRename();
      });
      nameWrap.appendChild(nameInp);
      setTimeout(() => { try { nameInp.focus(); nameInp.select(); } catch (_) {} }, 10);
    } else {
      const nameSpan = document.createElement('span');
      nameSpan.className = 'acc-name-text';
      nameSpan.textContent = accLabel;
      nameSpan.title = `${'$'}{accLabel} (Klik ✎ untuk ubah nama)`;
      nameWrap.appendChild(nameSpan);

      const editNameBtn = document.createElement('button');
      editNameBtn.type = 'button';
      editNameBtn.className = 'btn-edit-name';
      editNameBtn.textContent = '✎';
      editNameBtn.title = `Ubah nama ${'$'}{accLabel}`;
      editNameBtn.onclick = (e) => {
        e.stopPropagation();
        renamingAccountId = acc.accountId;
        renderSnapshot(currentSnapshot);
      };
      nameWrap.appendChild(editNameBtn);
    }

    const tabCountBadge = document.createElement('span');
    tabCountBadge.className = 'acc-tab-badge';
    tabCountBadge.textContent = `(${'$'}{acc.tabCount}/${'$'}{acc.maxTabs})`;
    nameWrap.appendChild(tabCountBadge);
    head.appendChild(nameWrap);

    const actions = document.createElement('div');
    actions.className = 'acc-actions';

    const accSplitMode = Number(acc.splitMode) || 1;
    const splitToggleBtn = document.createElement('button');
    splitToggleBtn.type = 'button';
    splitToggleBtn.disabled = acc.tabCount <= 1;
    splitToggleBtn.className = acc.tabCount > 1 && accSplitMode > 1 ? 'btn-small btn-primary' : 'btn-small';
    splitToggleBtn.textContent = acc.tabCount <= 1
      ? 'Split'
      : (accSplitMode > 1 ? `Split ${'$'}{accSplitMode}` : 'Split OFF');
    splitToggleBtn.title = acc.tabCount <= 1
      ? 'Buka minimal 2 tab untuk menggunakan Split Layar'
      : (accSplitMode > 1
          ? `Split ${'$'}{accSplitMode} Layar aktif (Klik untuk ubah ke 1 layar penuh)`
          : `Split OFF (Klik untuk bagi ${'$'}{Math.min(3, acc.tabCount)} layar mandiri akun ini)`);
    splitToggleBtn.onclick = () => window.ferizyShell.toggleAccountSplit(acc.accountId);
    actions.appendChild(splitToggleBtn);

    const cfgBtn = document.createElement('button');
    cfgBtn.type = 'button';
    cfgBtn.className = 'btn-small';
    cfgBtn.textContent = acc.proxy && acc.proxy.protocol && acc.proxy.protocol !== 'direct' ? 'Proxy*' : 'Proxy/Geo';
    cfgBtn.title = `Pengaturan Nama, Proxy & Geo (${'$'}{(acc.proxy && acc.proxy.status) || 'Direct'})`;
    cfgBtn.onclick = () => openAccountModal(acc);
    actions.appendChild(cfgBtn);

    if (acc.accountId > 5) {
      const delAccBtn = document.createElement('button');
      delAccBtn.type = 'button';
      delAccBtn.className = 'btn-small btn-stop';
      delAccBtn.textContent = '×';
      delAccBtn.title = `Hapus ${'$'}{accLabel}`;
      delAccBtn.onclick = () => window.ferizyShell.removeAccount(acc.accountId);
      actions.appendChild(delAccBtn);
    }
    head.appendChild(actions);
    card.appendChild(head);

    // Baris 2: Kontrol AutoKlik Tab Aktif Akun Ini (Start/Stop saja, +Titik, -1 Titik, Hidden, Kunci)
    const akRow = document.createElement('div');
    akRow.className = 'acc-autoklik-row';

    const accRunningTab = acc.tabs.find((t) => t.runnerState === 'RUNNING');
    const runAccBtn = document.createElement('button');
    runAccBtn.type = 'button';
    if (accRunningTab) {
      runAccBtn.className = 'btn-small btn-stop';
      runAccBtn.textContent = '■ Stop';
      runAccBtn.title = `Hentikan AutoKlik ${'$'}{accLabel}`;
      runAccBtn.onclick = () => window.ferizyShell.stopExtAccount(acc.accountId);
    } else {
      runAccBtn.className = 'btn-small btn-run';
      runAccBtn.textContent = '▶ Start';
      runAccBtn.title = `Jalankan AutoKlik Tab ${'$'}{accActiveTabIdx} (${'$'}{accLabel})`;
      runAccBtn.onclick = () => window.ferizyShell.startExtAccount(acc.accountId, accActiveTabIdx);
    }

    const pickPtBtn = document.createElement('button');
    pickPtBtn.type = 'button';
    pickPtBtn.className = 'btn-small btn-primary';
    pickPtBtn.textContent = '+Titik';
    pickPtBtn.title = `Ambil / tambah titik baru ke Tab ${'$'}{accActiveTabIdx} (${'$'}{accLabel})`;
    pickPtBtn.onclick = async () => {
      pendingPickerFromHeaderModal = false;
      const res = await window.ferizyShell.startExtPicker(acc.accountId, 'tap', null, accActiveTabIdx);
      if (res && res.ok) {
        document.getElementById('active-meta').textContent =
          `Mode Ambil Titik aktif untuk ${'$'}{accLabel} • Tab ${'$'}{res.tabIndex || accActiveTabIdx}: klik elemen di website (ESC batal).`;
      } else {
        document.getElementById('active-meta').textContent = (res && res.reason) || 'Gagal memulai picker.';
      }
    };

    const decPtBtn = document.createElement('button');
    decPtBtn.type = 'button';
    decPtBtn.className = 'btn-small';
    decPtBtn.textContent = '-1 Titik';
    decPtBtn.disabled = activePtsCount <= 0;
    decPtBtn.title = `Kurangi 1 titik terakhir pada Tab ${'$'}{accActiveTabIdx} (${'$'}{accLabel})`;
    decPtBtn.onclick = () => window.ferizyShell.removeLastExtPoint(acc.accountId, accActiveTabIdx);

    const hidePtsBtn = document.createElement('button');
    hidePtsBtn.type = 'button';
    hidePtsBtn.className = isHidden ? 'btn-small btn-warn' : 'btn-small';
    hidePtsBtn.textContent = isHidden ? '👁 Hidden' : '👁';
    hidePtsBtn.title = isHidden ? `Titik tersembunyi di Tab ${'$'}{accActiveTabIdx} (klik untuk tampil)` : `Sembunyikan titik Tab ${'$'}{accActiveTabIdx}`;
    hidePtsBtn.onclick = () => window.ferizyShell.toggleExtHidden(acc.accountId, !isHidden, accActiveTabIdx);

    const lockPtsBtn = document.createElement('button');
    lockPtsBtn.type = 'button';
    lockPtsBtn.className = isLocked ? 'btn-small btn-warn' : 'btn-small';
    lockPtsBtn.textContent = isLocked ? '🔒 Kunci' : '🔓';
    lockPtsBtn.title = isLocked ? `Titik terkunci di Tab ${'$'}{accActiveTabIdx} (klik untuk bebas geser)` : `Kunci posisi titik Tab ${'$'}{accActiveTabIdx}`;
    lockPtsBtn.onclick = () => window.ferizyShell.toggleExtLock(acc.accountId, !isLocked, accActiveTabIdx);

    const saveCfgBtn = document.createElement('button');
    saveCfgBtn.type = 'button';
    saveCfgBtn.className = 'btn-small btn-acc-save-cfg';
    saveCfgBtn.textContent = '💾 Save';
    saveCfgBtn.title = `Simpan konfigurasi AutoKlik Tab ${'$'}{accActiveTabIdx} (${'$'}{accLabel}) ke file JSON`;
    saveCfgBtn.onclick = async () => {
      const res = await window.ferizyShell.savePointConfigDialog(acc.accountId, accActiveTabIdx);
      if (res && res.ok) {
        document.getElementById('active-meta').textContent = res.canceled
          ? `Konfigurasi Tab ${'$'}{accActiveTabIdx} (${'$'}{accLabel}) tersimpan otomatis di kontainer (${'$'}{res.storeFile}).`
          : `Konfigurasi Tab ${'$'}{accActiveTabIdx} (${'$'}{accLabel}) berhasil di-Save ke: ${'$'}{res.filePath}`;
      }
    };

    const loadCfgBtn = document.createElement('button');
    loadCfgBtn.type = 'button';
    loadCfgBtn.className = 'btn-small btn-acc-load-cfg';
    loadCfgBtn.textContent = '📂 Load';
    loadCfgBtn.title = `Buka menu Load File & Copy-Paste Konfigurasi Antar Tab untuk ${'$'}{accLabel} (Tab ${'$'}{accActiveTabIdx})`;
    loadCfgBtn.onclick = () => openLoadCopyModal(acc.accountId, accActiveTabIdx);

    const ptsBadge = document.createElement('span');
    ptsBadge.className = `ind ${'$'}{activePtsCount > 0 ? 'ok' : 'warn'}`;
    ptsBadge.style.marginLeft = 'auto';
    ptsBadge.textContent = `🎯 ${'$'}{activePtsCount}`;
    ptsBadge.title = `Tab ${'$'}{accActiveTabIdx} memiliki ${'$'}{activePtsCount} titik`;

    akRow.appendChild(runAccBtn);
    akRow.appendChild(pickPtBtn);
    akRow.appendChild(decPtBtn);
    akRow.appendChild(hidePtsBtn);
    akRow.appendChild(lockPtsBtn);
    akRow.appendChild(saveCfgBtn);
    akRow.appendChild(loadCfgBtn);
    akRow.appendChild(ptsBadge);
    card.appendChild(akRow);

    // Baris 3: Tab Pills Mandiri + +Tab
    const strip = document.createElement('div');
    strip.className = 'tab-strip';

    acc.tabs.forEach((tab) => {
      const pill = document.createElement('div');
      const stateCls =
        tab.runnerState === 'WON_STOPPED'
          ? 'won'
          : tab.runnerState === 'PAUSED_CAPTCHA'
            ? 'captcha'
            : tab.runnerState === 'LOGOUT_STOPPED'
              ? 'logout'
              : tab.runnerState === 'RUNNING'
                ? 'running'
                : '';
      pill.className = `tab-pill ${'$'}{tab.tabId === snap.activeTabId ? 'active' : ''} ${'$'}{stateCls}`;
      pill.title = `Tab ${'$'}{tab.indexInAccount}: ${'$'}{tab.pointsCount || 0} titik • ${'$'}{tab.url || ''}`;
      pill.onclick = () => window.ferizyShell.switchTab(tab.tabId);

      const stateIcon =
        tab.runnerState === 'WON_STOPPED'
          ? '★'
          : tab.runnerState === 'PAUSED_CAPTCHA'
            ? '⚠'
            : tab.runnerState === 'LOGOUT_STOPPED'
              ? '✖'
              : tab.runnerState === 'RUNNING'
                ? '▶'
                : '';
      const ptsSuffix = tab.pointsCount ? `(${'$'}{tab.pointsCount})` : '';
      const label = document.createElement('span');
      label.textContent = `T${'$'}{tab.indexInAccount}${'$'}{ptsSuffix}${'$'}{stateIcon}${'$'}{tab.isLoading ? '…' : ''}`;
      pill.appendChild(label);

      if (acc.tabs.length > 1) {
        const close = document.createElement('span');
        close.className = 'tab-close';
        close.textContent = '×';
        close.onclick = (e) => {
          e.stopPropagation();
          window.ferizyShell.closeTab(tab.tabId);
        };
        pill.appendChild(close);
      }
      strip.appendChild(pill);
    });

    const addBtn = document.createElement('button');
    addBtn.type = 'button';
    addBtn.className = 'btn-add-tab';
    addBtn.disabled = acc.tabCount >= acc.maxTabs;
    addBtn.textContent = acc.tabCount >= acc.maxTabs ? 'Max 3/3' : '+ Tab';
    addBtn.onclick = () => window.ferizyShell.createTab(acc.accountId);
    strip.appendChild(addBtn);

    card.appendChild(strip);
    grid.appendChild(card);
  });

  const runnerBtn = document.getElementById('btn-toggle-active-runner');
  if (activeTabObj) {
    const urlInput = document.getElementById('active-url-input');
    if (document.activeElement !== urlInput) {
      urlInput.value = activeTabObj.url || '';
    }
    const urlPresetSel = document.getElementById('url-preset-select');
    if (urlPresetSel && document.activeElement !== urlPresetSel) {
      const cleanActiveUrl = String(activeTabObj.url || '').trim();
      if (cleanActiveUrl === 'https://trip.ferizy.com' || cleanActiveUrl === 'https://trip.ferizy.com/') {
        urlPresetSel.value = 'https://trip.ferizy.com/';
      } else if (/^https:\/\/trip\.ferizy\.com\/order(\/|\?|$)/i.test(cleanActiveUrl)) {
        urlPresetSel.value = 'https://trip.ferizy.com/order';
      } else {
        urlPresetSel.value = '';
      }
    }
    const backBtn = document.getElementById('btn-back-active');
    if (backBtn) {
      const cleanRoot = String(activeTabObj.url || '').trim().replace(/\/+$/, '');
      const canFallbackHome = Boolean(cleanRoot && cleanRoot !== 'https://trip.ferizy.com');
      backBtn.title = activeTabObj.canGoBack
        ? 'Kembali ke halaman website sebelumnya (Alt + Panah Kiri)'
        : canFallbackHome
          ? 'Kembali ke halaman utama https://trip.ferizy.com/ (Alt + Panah Kiri)'
          : 'Sudah berada di halaman utama https://trip.ferizy.com/';
    }
    const errSuffix = activeTabObj.lastError ? ` • ERROR: ${'$'}{activeTabObj.lastError}` : '';
    const loadSuffix = activeTabObj.isLoading ? ' • Memuat...' : '';
    const ownerAcc = snap.accounts.find((a) => a.accountId === activeTabObj.accountId);
    const ownerName = (ownerAcc && ownerAcc.name) || `Akun ${'$'}{activeTabObj.accountId}`;
    document.getElementById('active-meta').textContent =
      `${'$'}{ownerName} • Tab ${'$'}{activeTabObj.indexInAccount} • ${'$'}{activeTabObj.runnerState} (${'$'}{activeTabObj.memoryMb || 0} MB)${'$'}{loadSuffix}${'$'}{errSuffix}`;

    if (activeTabObj.runnerState === 'RUNNING') {
      runnerBtn.textContent = '■ Stop Tab';
      runnerBtn.className = 'btn-small btn-stop';
    } else {
      runnerBtn.textContent = '▶ Start Tab';
      runnerBtn.className = 'btn-small btn-run';
    }
  }

  requestAnimationFrame(syncHeaderHeight);
}

function openAccountModal(acc) {
  editingAccountId = acc.accountId;
  const accLabel = acc.name || `Akun ${'$'}{acc.accountId}`;
  document.getElementById('modal-title').textContent = `Pengaturan ${'$'}{accLabel} (${'$'}{acc.partition})`;
  document.getElementById('cfg-acc-name').value = accLabel;
  document.getElementById('cfg-proxy-protocol').value = (acc.proxy && acc.proxy.protocol) || 'direct';
  document.getElementById('cfg-proxy-host').value = (acc.proxy && acc.proxy.host) || '';
  document.getElementById('cfg-proxy-port').value = (acc.proxy && acc.proxy.port) || 8080;
  document.getElementById('cfg-proxy-user').value = (acc.proxy && acc.proxy.username) || '';
  document.getElementById('cfg-proxy-pass').value = (acc.proxy && acc.proxy.password) || '';
  document.getElementById('cfg-geo-mode').value = (acc.geo && acc.geo.mode) || 'preset_0ms';
  document.getElementById('cfg-geo-preset').value = (acc.geo && acc.geo.preset) || 'merak';
  openModalById('config-modal');
}

document.getElementById('btn-close-modal').onclick = () => {
  closeModalById('config-modal');
};

document.getElementById('btn-save-account-cfg').onclick = async () => {
  if (!editingAccountId) return;
  const nextName = document.getElementById('cfg-acc-name').value.trim();
  await window.ferizyShell.renameAccount(editingAccountId, nextName);
  await window.ferizyShell.updateProxy(editingAccountId, {
    protocol: document.getElementById('cfg-proxy-protocol').value,
    host: document.getElementById('cfg-proxy-host').value,
    port: Number(document.getElementById('cfg-proxy-port').value) || 8080,
    username: document.getElementById('cfg-proxy-user').value,
    password: document.getElementById('cfg-proxy-pass').value
  });
  await window.ferizyShell.updateGeo(editingAccountId, {
    mode: document.getElementById('cfg-geo-mode').value,
    preset: document.getElementById('cfg-geo-preset').value
  });
  closeModalById('config-modal');
};

document.getElementById('btn-toggle-active-runner').onclick = async () => {
  if (!currentSnapshot || !currentSnapshot.activeTabId) return;
  let activeTab = null;
  for (const acc of currentSnapshot.accounts) {
    const found = acc.tabs.find((t) => t.tabId === currentSnapshot.activeTabId);
    if (found) activeTab = found;
  }
  if (!activeTab) return;
  if (activeTab.runnerState === 'RUNNING') {
    await window.ferizyShell.stopTabAutomation(activeTab.tabId);
  } else {
    await window.ferizyShell.startTabAutomation(activeTab.tabId);
  }
};

document.getElementById('btn-toggle-checklist').onclick = () => {
  document.getElementById('optional-checklist-panel').classList.toggle('hidden');
  requestAnimationFrame(syncHeaderHeight);
};

document.getElementById('btn-apply-checklist').onclick = async () => {
  await window.ferizyShell.updateOptionalFlags({
    gentleKeepAliveEnabled: document.getElementById('chk-keep-alive').checked,
    keepAliveIntervalMinutes: Number(document.getElementById('sel-keepalive-min').value) || 12,
    autoWakeMode: document.getElementById('sel-autowake-mode').value,
    captchaAutoPopEnabled: document.getElementById('chk-captcha-autopop').checked,
    smartTargetMatcherEnabled: document.getElementById('chk-smart-matcher').checked,
    smartMatcherServiceType: document.getElementById('sel-service-type').value,
    smartMatcherFallbackNearest: document.getElementById('chk-matcher-fallback').checked,
    latencyCompensationEnabled: document.getElementById('chk-latency-comp').checked,
    fastRequestTimeoutMs: Number(document.getElementById('sel-fast-timeout').value) || 4500,
    advancedStopMode: document.getElementById('sel-advanced-stop').value,
    priorityHoursCsv: document.getElementById('input-priority-hours').value
  });
  await window.ferizyShell.updateSchedule({
    scheduledShotEnabled: document.getElementById('chk-sched-shot').checked,
    scheduledShotTimeWib: document.getElementById('input-sched-time').value
  });
  await window.ferizyShell.updateNotifSettings({
    ntfyServerUrl: document.getElementById('input-ntfy-server').value,
    dedupeSeconds: Number(document.getElementById('sel-notif-dedupe').value) || 10,
    events: {
      LINTASAN_DIBUKA: document.getElementById('chk-notif-lintasan').checked,
      KUOTA_MUNCUL: document.getElementById('chk-notif-kuota').checked,
      TEMBUS_VERIFIKASI: document.getElementById('chk-notif-tembus').checked,
      LOGOUT_TERDETEKSI: document.getElementById('chk-notif-logout').checked,
      PROXY_PUTUS: document.getElementById('chk-notif-proxy').checked,
      CAPTCHA_TERDETEKSI: document.getElementById('chk-notif-captcha').checked
    }
  });
  const preset = document.getElementById('sel-log-preset').value;
  await window.ferizyShell.updateFase4Config({
    logMaxSizeMb: preset === '20mb_3d' ? 20 : 50,
    logRetentionDays: preset === '20mb_3d' ? 3 : 7,
    watchdogCalibrationTargetMb: Number(document.getElementById('sel-watchdog-cal').value) || 350,
    enduranceHealthReportEnabled: document.getElementById('chk-health-report').checked
  });
  document.getElementById('optional-checklist-panel').classList.add('hidden');
  requestAnimationFrame(syncHeaderHeight);
};

document.getElementById('btn-mute-alarm').onclick = async () => {
  const muted = Boolean(currentSnapshot && currentSnapshot.notificationSettings && currentSnapshot.notificationSettings.audioAlarmMuted);
  await window.ferizyShell.updateNotifSettings({ audioAlarmMuted: !muted });
};

document.getElementById('btn-resume-captcha').onclick = () => {
  window.ferizyShell.resumeCaptchaAll();
};

document.getElementById('btn-run-endurance').onclick = async () => {
  const res = await window.ferizyShell.runEnduranceSelfCheck();
  if (res) {
    document.getElementById('active-meta').textContent =
      `Uji 24J: ${'$'}{res.status} • RAM ${'$'}{res.totalAppRamMb}MB • Log ${'$'}{res.diskLogMb}MB`;
  }
};

document.getElementById('btn-export-health').onclick = async () => {
  const res = await window.ferizyShell.exportHealthReport();
  if (res && res.reportPath) {
    document.getElementById('active-meta').textContent = `Health Report disimpan: ${'$'}{res.reportPath}`;
  }
};

document.getElementById('btn-prune-logs').onclick = () => {
  window.ferizyShell.pruneLogsNow();
};

document.getElementById('btn-open-logs').onclick = () => {
  window.ferizyShell.openLogsFolder();
};

document.getElementById('btn-open-notif-modal').onclick = () => {
  const ns = (currentSnapshot && currentSnapshot.notificationSettings) || {};
  const ev = ns.events || {};
  document.getElementById('m-notif-lintasan').checked = ev.LINTASAN_DIBUKA !== false;
  document.getElementById('m-notif-kuota').checked = ev.KUOTA_MUNCUL !== false;
  document.getElementById('m-notif-tembus').checked = ev.TEMBUS_VERIFIKASI !== false;
  document.getElementById('m-notif-logout').checked = ev.LOGOUT_TERDETEKSI === true;
  document.getElementById('m-notif-proxy').checked = ev.PROXY_PUTUS === true;
  document.getElementById('m-notif-captcha').checked = ev.CAPTCHA_TERDETEKSI === true;
  document.getElementById('m-ntfy-topic').value = ns.ntfyTopic || (currentSnapshot && currentSnapshot.ntfyTopic) || 'ferizy_Transsamudra05';
  document.getElementById('m-ntfy-server').value = ns.ntfyServerUrl || 'https://ntfy.sh';
  document.getElementById('m-notif-dedupe').value = String(ns.dedupeSeconds || 10);
  openModalById('notif-modal');
};

document.getElementById('btn-close-notif-modal').onclick = () => {
  closeModalById('notif-modal');
};

document.getElementById('btn-save-notif-modal').onclick = async () => {
  const events = {
    LINTASAN_DIBUKA: document.getElementById('m-notif-lintasan').checked,
    KUOTA_MUNCUL: document.getElementById('m-notif-kuota').checked,
    TEMBUS_VERIFIKASI: document.getElementById('m-notif-tembus').checked,
    LOGOUT_TERDETEKSI: document.getElementById('m-notif-logout').checked,
    PROXY_PUTUS: document.getElementById('m-notif-proxy').checked,
    CAPTCHA_TERDETEKSI: document.getElementById('m-notif-captcha').checked
  };
  const topic = document.getElementById('m-ntfy-topic').value.trim();
  const serverUrl = document.getElementById('m-ntfy-server').value.trim();
  const dedupeSeconds = Number(document.getElementById('m-notif-dedupe').value) || 10;

  document.getElementById('chk-notif-lintasan').checked = events.LINTASAN_DIBUKA;
  document.getElementById('chk-notif-kuota').checked = events.KUOTA_MUNCUL;
  document.getElementById('chk-notif-tembus').checked = events.TEMBUS_VERIFIKASI;
  document.getElementById('chk-notif-logout').checked = events.LOGOUT_TERDETEKSI;
  document.getElementById('chk-notif-proxy').checked = events.PROXY_PUTUS;
  document.getElementById('chk-notif-captcha').checked = events.CAPTCHA_TERDETEKSI;
  document.getElementById('input-ntfy-server').value = serverUrl;
  document.getElementById('sel-notif-dedupe').value = String(dedupeSeconds);

  await window.ferizyShell.updateNotifSettings({
    ntfyTopic: topic,
    ntfyServerUrl: serverUrl,
    dedupeSeconds,
    events
  });
  closeModalById('notif-modal');
};

document.getElementById('btn-test-ntfy').onclick = () => {
  window.ferizyShell.testNtfy();
};

document.getElementById('btn-reload-standby').onclick = () => {
  window.ferizyShell.reloadStandbyQueue();
};

document.getElementById('btn-back-active').onclick = async () => {
  if (!currentSnapshot || !currentSnapshot.activeTabId) {
    document.getElementById('active-meta').textContent = 'Tab aktif belum siap, tunggu sebentar...';
    return;
  }
  const res = await window.ferizyShell.goBackTab(currentSnapshot.activeTabId);
  if (res && res.ok) {
    document.getElementById('active-meta').textContent =
      res.mode === 'fallback_home'
        ? 'Kembali ke beranda https://trip.ferizy.com/...'
        : 'Kembali ke halaman website sebelumnya...';
  } else if (res && res.reason) {
    document.getElementById('active-meta').textContent = res.reason;
  }
};

document.getElementById('btn-reload-active').onclick = () => {
  if (currentSnapshot && currentSnapshot.activeTabId) {
    window.ferizyShell.reloadTab(currentSnapshot.activeTabId);
  }
};

// Initial fallback bindings untuk Kumpulan Tab 1, Tab 2, dan Tab 3 (sebelum snapshot pertama tiba)
// IDs: tab1-collection-card, tab2-collection-card, tab3-collection-card, btn-toggle-tab1-mirror, btn-toggle-tab1-split
[1, 2, 3].forEach((gIdx) => {
  const mBtn = document.getElementById(`btn-toggle-tab${'$'}{gIdx}-mirror`);
  if (mBtn) {
    mBtn.onclick = () => window.ferizyShell.toggleTabGroupMirror(gIdx);
  }
  const sBtn = document.getElementById(`btn-toggle-tab${'$'}{gIdx}-split`);
  if (sBtn) {
    sBtn.onclick = () => window.ferizyShell.toggleTabCollectionSplit(gIdx);
  }
});

const togglePreflightBtn = document.getElementById('btn-toggle-preflight-grid');
if (togglePreflightBtn) {
  togglePreflightBtn.onclick = () => {
    preflightGridVisible = !preflightGridVisible;
    const gridEl = document.getElementById('preflight-grid');
    if (gridEl) {
      gridEl.style.display = preflightGridVisible ? 'grid' : 'none';
    }
    togglePreflightBtn.textContent = preflightGridVisible ? '📦 Kotak Kontainer' : '📦 Kotak Kontainer (OFF)';
    togglePreflightBtn.className = preflightGridVisible ? 'btn-small btn-primary' : 'btn-small';
    requestAnimationFrame(syncHeaderHeight);
  };
}

async function navigateFromBar(overrideUrl = null) {
  const urlInput = document.getElementById('active-url-input');
  if (overrideUrl) {
    urlInput.value = overrideUrl;
  }
  const url = urlInput.value.trim();
  if (!url) return;
  if (!currentSnapshot || !currentSnapshot.activeTabId) {
    document.getElementById('active-meta').textContent = 'Tab aktif belum siap, tunggu sebentar...';
    return;
  }
  urlInput.blur();
  document.getElementById('active-meta').textContent = `Memuat ${'$'}{url}...`;
  await window.ferizyShell.navigateTab(currentSnapshot.activeTabId, url);
}

const urlPresetSelectEl = document.getElementById('url-preset-select');
if (urlPresetSelectEl) {
  urlPresetSelectEl.onchange = () => {
    const selectedUrl = urlPresetSelectEl.value;
    if (selectedUrl) {
      navigateFromBar(selectedUrl);
    }
  };
}

document.getElementById('btn-go-url').onclick = () => {
  navigateFromBar();
};

document.getElementById('active-url-input').addEventListener('change', (e) => {
  const val = (e.target.value || '').trim();
  if (val === 'https://trip.ferizy.com/' || val === 'https://trip.ferizy.com/order') {
    navigateFromBar(val);
  }
});

document.getElementById('active-url-input').addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    navigateFromBar();
  } else if (e.key === 'Escape' && currentSnapshot && currentSnapshot.activeTabId) {
    e.target.blur();
    renderSnapshot(currentSnapshot);
  }
});

document.getElementById('btn-refresh-preflight').onclick = () => {
  window.ferizyShell.refreshPreflight();
};

document.getElementById('btn-check-all-ip').onclick = async () => {
  const ids = (currentSnapshot && Array.isArray(currentSnapshot.accountIds))
    ? currentSnapshot.accountIds
    : [1, 2, 3, 4, 5];
  for (const id of ids) {
    await window.ferizyShell.checkProxyIp(id);
  }
};

document.getElementById('btn-add-account').onclick = () => {
  window.ferizyShell.addAccount();
};

document.getElementById('btn-zoom-out').onclick = () => {
  const cur = (currentSnapshot && currentSnapshot.globalZoomPercent) || 100;
  window.ferizyShell.setGlobalZoom(Math.max(50, cur - 10));
};
document.getElementById('btn-zoom-reset').onclick = () => {
  window.ferizyShell.setGlobalZoom(100);
};
document.getElementById('btn-zoom-in').onclick = () => {
  const cur = (currentSnapshot && currentSnapshot.globalZoomPercent) || 100;
  window.ferizyShell.setGlobalZoom(Math.min(150, cur + 10));
};

document.getElementById('btn-toggle-captcha-notif').onclick = async () => {
  const ev = (currentSnapshot && currentSnapshot.notificationSettings && currentSnapshot.notificationSettings.events) || {};
  const nextVal = !(ev.CAPTCHA_TERDETEKSI === true);
  const chkMain = document.getElementById('chk-notif-captcha');
  const chkModal = document.getElementById('m-notif-captcha');
  if (chkMain) chkMain.checked = nextVal;
  if (chkModal) chkModal.checked = nextVal;
  await window.ferizyShell.updateNotifSettings({
    events: { CAPTCHA_TERDETEKSI: nextVal }
  });
};

document.getElementById('btn-open-display-modal').onclick = () => {
  const zoomPct = (currentSnapshot && currentSnapshot.globalZoomPercent) || 100;
  document.getElementById('disp-zoom-label').textContent = `${'$'}{zoomPct}%`;
  document.getElementById('disp-zoom-slider').value = String(zoomPct);
  document.getElementById('chk-compact-mode').checked = document.getElementById('shell-header').classList.contains('compact-mode');
  openModalById('display-modal');
};
document.getElementById('btn-close-display-modal').onclick = () => {
  closeModalById('display-modal');
};
document.getElementById('btn-done-display-modal').onclick = () => {
  closeModalById('display-modal');
};

document.getElementById('chk-compact-mode').onchange = (e) => {
  applyCompactMode(e.target.checked);
};

document.getElementById('disp-zoom-slider').oninput = (e) => {
  const val = Number(e.target.value) || 100;
  document.getElementById('disp-zoom-label').textContent = `${'$'}{val}%`;
  window.ferizyShell.setGlobalZoom(val);
};

document.querySelectorAll('.disp-zoom-preset').forEach((btn) => {
  btn.onclick = () => window.ferizyShell.setGlobalZoom(Number(btn.getAttribute('data-zoom')) || 100);
});

document.querySelectorAll('.disp-split-preset').forEach((btn) => {
  btn.onclick = () => window.ferizyShell.setSplitMode(Number(btn.getAttribute('data-split')) || 1);
});

// AutoKlik-v3.2-Mandiri-Extension UI Controller (Mandiri Per-Tab: accountId + tabIndex)
function setAkStatus(text) {
  const el = document.getElementById('ak-status-msg');
  if (el) el.textContent = text;
}

function resetAkPointForm() {
  extEditingPointId = null;
  document.getElementById('ak-form-title').textContent = 'Tambah Titik Baru';
  document.getElementById('btn-ak-save-point').textContent = '+ Tambah Titik';
  document.getElementById('btn-ak-cancel-edit').classList.add('hidden');
  document.getElementById('ak-pt-type').value = 'tap';
  document.getElementById('ak-pt-label').value = '';
  document.getElementById('ak-pt-x').value = '540';
  document.getElementById('ak-pt-y').value = '420';
  document.getElementById('ak-pt-delay').value = '500';
  document.getElementById('ak-pt-selector').value = '';
  document.getElementById('ak-pt-value').value = '';
  document.getElementById('ak-pt-endxy').value = '';
}

function fillAkPointForm(pt) {
  extEditingPointId = pt.id;
  document.getElementById('ak-form-title').textContent = `Edit Titik #${'$'}{pt.order}: ${'$'}{pt.label}`;
  document.getElementById('btn-ak-save-point').textContent = 'Simpan Perubahan Titik';
  document.getElementById('btn-ak-cancel-edit').classList.remove('hidden');
  document.getElementById('ak-pt-type').value = pt.type || 'tap';
  document.getElementById('ak-pt-label').value = pt.label || '';
  document.getElementById('ak-pt-x').value = String(pt.x ?? 540);
  document.getElementById('ak-pt-y').value = String(pt.y ?? 420);
  document.getElementById('ak-pt-delay').value = String(pt.delayAfterMs ?? 500);
  document.getElementById('ak-pt-selector').value = pt.selector || '';
  document.getElementById('ak-pt-value').value = pt.type === 'popup_key' ? (pt.popupKey || 'Enter') : (pt.inputValue || pt.text || '');
  document.getElementById('ak-pt-endxy').value = pt.type === 'swipe' ? `${'$'}{pt.endX || pt.x},${'$'}{pt.endY || 200}` : '';
}

function getTabPointsAndSettings(acc, tabIdx) {
  if (!acc) return { points: [], extensionSettings: {}, isOpen: false };
  const tabObj = Array.isArray(acc.tabs) ? acc.tabs.find((t) => t.indexInAccount === tabIdx) : null;
  if (tabObj) {
    return {
      points: Array.isArray(tabObj.points) ? tabObj.points : [],
      extensionSettings: tabObj.extensionSettings || acc.extensionSettings || {},
      isOpen: true
    };
  }
  return {
    points: tabIdx === 1 && Array.isArray(acc.points) ? acc.points : [],
    extensionSettings: acc.extensionSettings || {},
    isOpen: false
  };
}

function refreshAutoklikExtModalFromSnapshot(snap, syncSettingsInputs = true) {
  if (!snap || !Array.isArray(snap.accounts)) return;
  const sel = document.getElementById('ak-account-select');
  if (sel) {
    sel.innerHTML = '';
    snap.accounts.forEach((a) => {
      const opt = document.createElement('option');
      opt.value = String(a.accountId);
      const lbl = a.name || `Akun ${'$'}{a.accountId}`;
      opt.textContent = `${'$'}{lbl} (${'$'}{a.totalAccountPoints ?? a.pointsCount ?? 0} titik)`;
      if (a.accountId === extAccountId) opt.selected = true;
      sel.appendChild(opt);
    });
  }

  const acc = snap.accounts.find((a) => a.accountId === extAccountId) || snap.accounts[0];
  if (!acc) return;
  extAccountId = acc.accountId;
  extTabIndex = Math.min(3, Math.max(1, Number(extTabIndex) || 1));

  const tabSel = document.getElementById('ak-tab-select');
  if (tabSel) {
    tabSel.innerHTML = '';
    for (let t = 1; t <= 3; t++) {
      const tabObj = Array.isArray(acc.tabs) ? acc.tabs.find((item) => item.indexInAccount === t) : null;
      const cnt = tabObj ? (tabObj.pointsCount || 0) : 0;
      const opt = document.createElement('option');
      opt.value = String(t);
      opt.textContent = tabObj ? `Tab ${'$'}{t} (${'$'}{cnt} titik)` : `Tab ${'$'}{t} (Standby)`;
      if (t === extTabIndex) opt.selected = true;
      tabSel.appendChild(opt);
    }
  }

  const tabData = getTabPointsAndSettings(acc, extTabIndex);
  const accLabel = acc.name || `Akun ${'$'}{acc.accountId}`;
  document.getElementById('ak-container-badge').textContent =
    `${'$'}{accLabel} (${'$'}{acc.partition}) • Tab ${'$'}{extTabIndex} • File: ${'$'}{acc.storeFile || `akun_${'$'}{acc.accountId}.json`}`;
  const scopeLbl = document.getElementById('ak-tab-scope-lbl');
  if (scopeLbl) scopeLbl.textContent = `Tab ${'$'}{extTabIndex}`;

  if (syncSettingsInputs) {
    const s = tabData.extensionSettings || {};
    document.getElementById('ak-set-enabled').checked = s.enabled !== false;
    document.getElementById('ak-set-loop').checked = s.loopEnabled !== false;
    document.getElementById('ak-set-smartmatch').checked = s.smartMatch !== false;
    document.getElementById('ak-set-antirevert').checked = s.antiRevert !== false;
    document.getElementById('ak-set-hidden').checked = Boolean(s.markersHidden);
    document.getElementById('ak-set-locked').checked = Boolean(s.pointsLocked);
    document.getElementById('ak-set-maxloops').value = String(s.maxLoops ?? 0);
    document.getElementById('ak-set-loopdelay').value = String(s.loopDelayMs ?? 100);
    document.getElementById('ak-set-radius').value = String(s.radiusPx ?? 28);
  }

  const pts = tabData.points;
  document.getElementById('ak-points-count').textContent = String(pts.length);
  const listEl = document.getElementById('ak-points-list');
  listEl.innerHTML = '';

  if (!pts.length) {
    listEl.innerHTML = `<div class="active-meta">Belum ada titik di Tab ${'$'}{extTabIndex} (${'$'}{accLabel}). Klik "Ambil Titik" atau "+ Tambah Titik".</div>`;
    return;
  }

  pts.forEach((pt, idx) => {
    const row = document.createElement('div');
    row.className = 'ak-point-row';

    const info = document.createElement('div');
    info.className = 'ak-point-info';

    const chk = document.createElement('input');
    chk.type = 'checkbox';
    chk.checked = pt.enabled !== false;
    chk.title = 'Aktif / Nonaktifkan titik ini';
    chk.onchange = () => window.ferizyShell.updateExtPoint(acc.accountId, pt.id, { enabled: chk.checked }, extTabIndex);
    info.appendChild(chk);

    const extra = pt.type === 'input'
      ? ` = "${'$'}{pt.inputValue || ''}"${'$'}{pt.popupPinEnabled ? ` [PopupKey:${'$'}{pt.popupDelayMs || 300}ms]` : ''}`
      : pt.type === 'popup_key'
        ? ` [${'$'}{pt.popupKey || 'Enter'}]`
        : '';
    const txt = document.createElement('span');
    txt.textContent = `#${'$'}{idx + 1} [${'$'}{(pt.type || 'tap').toUpperCase()}] ${'$'}{pt.label} (${'$'}{pt.x},${'$'}{pt.y}) • ${'$'}{pt.delayAfterMs || 500}ms${'$'}{extra}`;
    info.appendChild(txt);

    const acts = document.createElement('div');
    acts.className = 'ak-point-actions';

    const upBtn = document.createElement('button');
    upBtn.className = 'btn-small';
    upBtn.textContent = '▲';
    upBtn.disabled = idx === 0;
    upBtn.onclick = () => window.ferizyShell.moveExtPoint(acc.accountId, pt.id, 'up', extTabIndex);

    const downBtn = document.createElement('button');
    downBtn.className = 'btn-small';
    downBtn.textContent = '▼';
    downBtn.disabled = idx === pts.length - 1;
    downBtn.onclick = () => window.ferizyShell.moveExtPoint(acc.accountId, pt.id, 'down', extTabIndex);

    const editBtn = document.createElement('button');
    editBtn.className = 'btn-small';
    editBtn.textContent = 'Edit';
    editBtn.onclick = () => openSinglePointEditor(acc.accountId, pt.id, extTabIndex);

    const delBtn = document.createElement('button');
    delBtn.className = 'btn-small btn-stop';
    delBtn.textContent = '×';
    delBtn.onclick = () => window.ferizyShell.removeExtPoint(acc.accountId, pt.id, extTabIndex);

    acts.appendChild(upBtn);
    acts.appendChild(downBtn);
    acts.appendChild(editBtn);
    acts.appendChild(delBtn);

    row.appendChild(info);
    row.appendChild(acts);
    listEl.appendChild(row);
  });
}

function openAutoklikExtModal(accountId = null, tabIndex = null) {
  if (accountId) {
    extAccountId = Number(accountId) || 1;
    const acc = currentSnapshot && currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === extAccountId);
    extTabIndex = Number(tabIndex) || (acc && acc.activeTabIndex) || 1;
  } else if (currentSnapshot) {
    extAccountId = currentSnapshot.activeAccountId || 1;
    const acc = currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === extAccountId);
    extTabIndex = Number(tabIndex) || currentSnapshot.activeTabIndex || (acc && acc.activeTabIndex) || 1;
  }
  resetAkPointForm();
  document.getElementById('ak-json-io').value = '';
  refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
  openModalById('autoklik-ext-modal');
}

document.getElementById('btn-open-autoklik-ext').onclick = () => openAutoklikExtModal(null, null);
document.getElementById('btn-close-autoklik-ext').onclick = () => {
  closeModalById('autoklik-ext-modal');
};

document.getElementById('ak-account-select').onchange = (e) => {
  extAccountId = Number(e.target.value) || 1;
  const acc = currentSnapshot && currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === extAccountId);
  extTabIndex = (acc && acc.activeTabIndex) || 1;
  resetAkPointForm();
  refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
  setAkStatus(`Menampilkan konfigurasi Akun ${'$'}{extAccountId} • Tab ${'$'}{extTabIndex}.`);
};

document.getElementById('ak-tab-select').onchange = (e) => {
  extTabIndex = Math.min(3, Math.max(1, Number(e.target.value) || 1));
  resetAkPointForm();
  refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
  setAkStatus(`Menampilkan konfigurasi Akun ${'$'}{extAccountId} • Tab ${'$'}{extTabIndex}.`);
};

document.getElementById('btn-ak-clear-tab-points').onclick = async () => {
  await window.ferizyShell.clearExtPoints(extAccountId, extTabIndex);
  setAkStatus(`Semua titik Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} dikosongkan.`);
};

document.getElementById('btn-ak-save-settings').onclick = async () => {
  const nextSettings = {
    enabled: document.getElementById('ak-set-enabled').checked,
    loopEnabled: document.getElementById('ak-set-loop').checked,
    smartMatch: document.getElementById('ak-set-smartmatch').checked,
    antiRevert: document.getElementById('ak-set-antirevert').checked,
    markersHidden: document.getElementById('ak-set-hidden').checked,
    pointsLocked: document.getElementById('ak-set-locked').checked,
    maxLoops: Math.max(0, Number(document.getElementById('ak-set-maxloops').value) || 0),
    loopDelayMs: Number(document.getElementById('ak-set-loopdelay').value) || 100,
    radiusPx: Number(document.getElementById('ak-set-radius').value) || 28
  };
  await window.ferizyShell.saveExtSettings(extAccountId, nextSettings, extTabIndex);
  setAkStatus(`Pengaturan Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} tersimpan.`);
};

document.getElementById('btn-ak-cancel-edit').onclick = resetAkPointForm;

document.getElementById('btn-ak-save-point').onclick = async () => {
  const type = document.getElementById('ak-pt-type').value;
  const label = document.getElementById('ak-pt-label').value.trim();
  const x = Number(document.getElementById('ak-pt-x').value) || 540;
  const y = Number(document.getElementById('ak-pt-y').value) || 420;
  const delayAfterMs = Number(document.getElementById('ak-pt-delay').value) || 500;
  const selector = document.getElementById('ak-pt-selector').value.trim();
  const rawVal = document.getElementById('ak-pt-value').value;
  const radiusPx = Number(document.getElementById('ak-set-radius').value) || 28;

  const payload = {
    type,
    label: label || undefined,
    x,
    y,
    delayAfterMs,
    selector,
    radiusPx,
    iconSizePct: 25,
    inputValue: type === 'input' ? rawVal : '',
    cycleTexts: type === 'input' ? [rawVal] : undefined,
    typingSpeedMs: type === 'input' ? 30 : undefined,
    popupDelayMs: type === 'input' ? 300 : undefined,
    lineLengthPx: (type === 'swipe_up' || type === 'swipe_down') ? 350 : undefined,
    swipeForceScale: (type === 'swipe_up' || type === 'swipe_down') ? 2 : undefined,
    endX: x,
    endY: type === 'swipe_down' ? (y + 350) : Math.max(20, y - 350)
  };

  if (extEditingPointId) {
    await window.ferizyShell.updateExtPoint(extAccountId, extEditingPointId, payload, extTabIndex);
    setAkStatus(`Titik diperbarui pada Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId}.`);
    resetAkPointForm();
  } else {
    await window.ferizyShell.addExtPoint(extAccountId, payload, extTabIndex);
    setAkStatus(`Titik baru ditambahkan ke Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId}.`);
    resetAkPointForm();
  }
};

document.getElementById('btn-ak-start-acc').onclick = async () => {
  const res = await window.ferizyShell.startExtAccount(extAccountId, extTabIndex);
  setAkStatus(res && res.ok ? `AutoKlik Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} berjalan.` : ((res && res.reason) || 'Gagal memulai AutoKlik.'));
};

document.getElementById('btn-ak-stop-acc').onclick = async () => {
  await window.ferizyShell.stopExtAccount(extAccountId);
  setAkStatus(`AutoKlik Akun ${'$'}{extAccountId} dihentikan.`);
};

document.getElementById('btn-ak-save-dialog').onclick = async () => {
  const res = await window.ferizyShell.savePointConfigDialog(extAccountId, extTabIndex);
  if (res && res.ok) {
    setAkStatus(res.canceled
      ? `Profil Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} tersimpan di ${'$'}{res.storeFile}.`
      : `Konfigurasi Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} disimpan ke ${'$'}{res.filePath}`);
  }
};

document.getElementById('btn-ak-open-dialog').onclick = async () => {
  const res = await window.ferizyShell.openPointConfigDialog(extAccountId, extTabIndex);
  if (res && res.ok) {
    refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
    setAkStatus(`Berhasil load ${'$'}{res.count} titik ke Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} (${'$'}{res.storeFile}).`);
  } else if (res && !res.canceled && res.reason) {
    setAkStatus(`Gagal load file: ${'$'}{res.reason}`);
  }
};

document.getElementById('btn-ak-copy-all').onclick = async () => {
  const res = await window.ferizyShell.pastePointsToAll(extAccountId, extTabIndex);
  setAkStatus(res && res.ok ? `Profil titik Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} disalin ke semua akun.` : 'Gagal menyalin.');
};

let pendingPickerFromHeaderModal = false;
let singleEditAccountId = null;
let singleEditTabIndex = 1;
let singleEditPointId = null;
let singleEditType = 'tap';
let singleEditOrder = 1;
let singleEditCycleTexts = [''];
let pendingRepickTarget = null;

function mapLegacyPointType(pt) {
  const rawType = (pt && pt.type) || 'tap';
  if (rawType === 'swipe') {
    const endY = Number(pt.endY);
    const y = Number(pt.y) || 0;
    return Number.isFinite(endY) && endY > y ? 'swipe_down' : 'swipe_up';
  }
  if (rawType === 'swipe_up' || rawType === 'swipe_down' || rawType === 'input') return rawType;
  return 'tap';
}

function getTypeSymbolForTitle(type) {
  if (type === 'swipe_up') return '▲';
  if (type === 'swipe_down') return '▼';
  if (type === 'input') return '📋';
  return '🎯';
}

function syncExtensionTypeButtons() {
  document.querySelectorAll('.ep-type-btn').forEach((btn) => {
    const btnType = btn.getAttribute('data-type');
    btn.classList.toggle('active', btnType === singleEditType);
  });
  document.getElementById('ep-modal-title').textContent =
    `Titik #${'$'}{singleEditOrder} ( ${'$'}{getTypeSymbolForTitle(singleEditType)} )`;
}

function updatePopupTriggerPreview() {
  const el = document.getElementById('ep-popup-trigger-preview');
  if (!el) return;
  const enabled = document.getElementById('ep-popup-pin-enabled').checked;
  const popupDelayEl = document.getElementById('ep-popup-delay');
  const popupDelay = Math.min(10000, Math.max(20, Number(popupDelayEl ? popupDelayEl.value : 300) || 300));
  const firstSample = (singleEditCycleTexts || []).find((s) => String(s || '').trim().length > 0) || '';
  const triggerChar = firstSample.trim().charAt(0) || '';
  if (!enabled) {
    el.textContent = 'Alur Tanpa Popup Key: Klik Kolom → Backspace Berulang Sesuai Panjang Teks → Ketik Murni Keyboard (30ms/huruf).';
  } else if (triggerChar) {
    el.textContent = `Pemicu Popup Key Otomatis: Klik Kolom + ketik "${'$'}{triggerChar}" → Tunggu ${'$'}{popupDelay}ms → Klik Pin Ungu → Backspace Berulang → Ketik Murni Keyboard "${'$'}{firstSample.trim()}".`;
  } else {
    el.textContent = `Isi minimal 1 teks di daftar bawah: huruf pertama akan otomatis diketik sebagai pemicu → tunggu ${'$'}{popupDelay}ms → klik Pin Ungu → Backspace berulang → ketik lengkap (30ms/huruf).`;
  }
}

function updatePopupDelayPresetsDisplay() {
  const popupDelayEl = document.getElementById('ep-popup-delay');
  const dly = Math.min(10000, Math.max(20, Number(popupDelayEl ? popupDelayEl.value : 300) || 300));
  document.querySelectorAll('.ep-popup-delay-preset').forEach((btn) => {
    btn.classList.toggle('active', Number(btn.getAttribute('data-popup-delay')) === dly);
  });
}

function renderCycleTextRows() {
  const listEl = document.getElementById('ep-cycle-texts-list');
  listEl.innerHTML = '';
  const items = Array.isArray(singleEditCycleTexts) && singleEditCycleTexts.length > 0 ? singleEditCycleTexts : [''];
  singleEditCycleTexts = items;

  items.forEach((val, idx) => {
    const row = document.createElement('div');
    row.className = 'ep-cycle-row';

    const idxLbl = document.createElement('span');
    idxLbl.className = 'ep-cycle-idx';
    idxLbl.textContent = `#${'$'}{idx + 1}`;

    const inp = document.createElement('input');
    inp.type = 'text';
    inp.placeholder = `Teks / Angka Siklus #${'$'}{idx + 1}...`;
    inp.value = val || '';
    inp.oninput = () => {
      singleEditCycleTexts[idx] = inp.value;
      updatePopupTriggerPreview();
    };

    row.appendChild(idxLbl);
    row.appendChild(inp);

    if (items.length > 1) {
      const remBtn = document.createElement('button');
      remBtn.type = 'button';
      remBtn.className = 'btn-small btn-stop';
      remBtn.textContent = '×';
      remBtn.onclick = () => {
        singleEditCycleTexts.splice(idx, 1);
        renderCycleTextRows();
        updatePopupTriggerPreview();
      };
      row.appendChild(remBtn);
    }
    listEl.appendChild(row);
  });
  updatePopupTriggerPreview();
}

function updateSwipeSummaryDisplay() {
  const len = Math.max(40, Number(document.getElementById('ep-swipe-len').value) || 350);
  const scale = Math.max(1, Number(document.getElementById('ep-swipe-scale').value) || 2);
  const total = Math.round(len * scale);
  document.getElementById('ep-swipe-summary').textContent =
    `Total Jarak Geser Layar: ${'$'}{total} px (Smooth 60FPS)`;
  document.querySelectorAll('.ep-scale-preset').forEach((btn) => {
    btn.classList.toggle('active', Number(btn.getAttribute('data-scale')) === scale);
  });
}

function updateTypingSpeedPresetsDisplay() {
  const spd = Math.max(5, Number(document.getElementById('ep-typing-speed').value) || 30);
  document.querySelectorAll('.ep-speed-preset').forEach((btn) => {
    btn.classList.toggle('active', Number(btn.getAttribute('data-speed')) === spd);
  });
}

function updateIconSizeDisplay() {
  const pct = Math.min(100, Math.max(15, Number(document.getElementById('ep-icon-size').value) || 25));
  const dp = Math.round(20 + pct * 0.6);
  document.getElementById('ep-icon-size-label').textContent = `📏 Ikon: ${'$'}{pct}% (${'$'}{dp}dp)`;
}

function updateSinglePointEditorFieldsVisibility() {
  syncExtensionTypeButtons();
  const isSwipe = singleEditType === 'swipe_up' || singleEditType === 'swipe_down';
  const isInput = singleEditType === 'input';
  const swipeGroup = document.getElementById('ep-swipe-group');
  const inputGroup = document.getElementById('ep-input-group');
  // V5.8 Menu Titik Klik Berdasarkan Tipe Aksi:
  // - TAP: Sembunyikan Panjang Garis & Skala Tenaga (ep-swipe-group) DAN sembunyikan seluruh form Input Ketik & Popup Key (ep-input-group)
  // - SWIPE ATAS & BAWAH: Tampilkan hanya Panjang Garis & Skala Tenaga (ep-swipe-group), sembunyikan form Input Ketik & Popup Key (ep-input-group)
  // - INPUT KETIK (+Popup): Tampilkan hanya form Input Ketik & Popup Key (ep-input-group), sembunyikan Panjang Garis & Skala Tenaga (ep-swipe-group)
  if (swipeGroup) {
    swipeGroup.classList.toggle('hidden', !isSwipe);
    swipeGroup.style.display = isSwipe ? 'flex' : 'none';
  }
  if (inputGroup) {
    inputGroup.classList.toggle('hidden', !isInput);
    inputGroup.style.display = isInput ? 'flex' : 'none';
  }
}

function openSinglePointEditor(accountId, pointId, tabIndex = null) {
  if (!currentSnapshot || !Array.isArray(currentSnapshot.accounts)) return;
  const acc = currentSnapshot.accounts.find((a) => a.accountId === Number(accountId));
  if (!acc) return;
  const resolvedTabIdx = Math.min(3, Math.max(1, Number(tabIndex) || acc.activeTabIndex || 1));
  const tabData = getTabPointsAndSettings(acc, resolvedTabIdx);
  const pts = Array.isArray(tabData.points) ? tabData.points : [];
  const idx = pts.findIndex((p) => p.id === pointId);
  if (idx === -1) return;
  const pt = pts[idx];

  singleEditAccountId = acc.accountId;
  singleEditTabIndex = resolvedTabIdx;
  singleEditPointId = pt.id;
  singleEditOrder = idx + 1;
  singleEditType = mapLegacyPointType(pt);

  const accLabel = acc.name || `Akun ${'$'}{acc.accountId}`;
  document.getElementById('ep-modal-sub').textContent =
    `${'$'}{accLabel} • Tab ${'$'}{resolvedTabIdx} • Titik ${'$'}{idx + 1}/${'$'}{pts.length}`;
  document.getElementById('btn-ep-order-left').disabled = idx === 0;
  document.getElementById('btn-ep-order-right').disabled = idx >= pts.length - 1;

  document.getElementById('ep-enabled').checked = pt.enabled !== false;
  document.getElementById('ep-x').value = String(pt.x ?? 540);
  document.getElementById('ep-y').value = String(pt.y ?? 420);
  document.getElementById('ep-label').value = pt.label || `Titik ${'$'}{idx + 1}`;
  document.getElementById('ep-radius').value = String(pt.radiusPx ?? 28);
  document.getElementById('ep-selector').value = pt.selector || '';
  document.getElementById('ep-coord-preview').textContent = `x=${'$'}{pt.x ?? 540}, y=${'$'}{pt.y ?? 420}`;

  // Delay default 0.5 detik (500ms)/titik
  const delayMs = Math.max(15, Number(pt.delayAfterMs) || 500);
  const useSec = delayMs >= 100 && delayMs % 100 === 0;
  document.querySelectorAll('input[name="ep-delay-unit"]').forEach((r) => {
    r.checked = (r.value === 's') === useSec;
  });
  document.getElementById('ep-delay-val').value = String(useSec ? (delayMs / 1000) : delayMs);

  // Icon size
  document.getElementById('ep-icon-size').value = String(Math.min(100, Math.max(15, Number(pt.iconSizePct) || 25)));
  updateIconSizeDisplay();

  // Swipe fields
  const lineLen = Math.max(40, Number(pt.lineLengthPx) || Math.abs((pt.endY ?? ((pt.y || 420) - 350)) - (pt.y || 420)) || 350);
  document.getElementById('ep-swipe-len').value = String(lineLen);
  document.getElementById('ep-swipe-scale').value = String(Math.max(1, Number(pt.swipeForceScale) || 2));
  updateSwipeSummaryDisplay();

  // Input (+Popup Pin Ungu) fields
  document.getElementById('ep-popup-pin-enabled').checked = Boolean(pt.popupPinEnabled);
  document.getElementById('ep-popup-pin-label').textContent =
    `Aktifkan Popup Key Autofill (Pin Ungu #${'$'}{singleEditOrder})`;
  const popupDelayEl = document.getElementById('ep-popup-delay');
  if (popupDelayEl) {
    popupDelayEl.value = String(Math.min(10000, Math.max(20, Number(pt.popupDelayMs) || 300)));
  }
  updatePopupDelayPresetsDisplay();
  singleEditCycleTexts = Array.isArray(pt.cycleTexts) && pt.cycleTexts.length > 0
    ? pt.cycleTexts.slice()
    : [pt.inputValue || pt.text || ''];
  renderCycleTextRows();
  document.getElementById('ep-typing-speed').value = String(Number.isFinite(Number(pt.typingSpeedMs)) && Number(pt.typingSpeedMs) > 0 ? Number(pt.typingSpeedMs) : 30);
  updateTypingSpeedPresetsDisplay();
  document.getElementById('ep-clear-before').checked = pt.clearBeforeType !== false;
  document.getElementById('ep-focus-before').checked = pt.focusBeforeType !== false;
  document.getElementById('ep-antirevert').checked = false;

  updateSinglePointEditorFieldsVisibility();
  openModalById('ak-edit-point-modal');
}

document.querySelectorAll('.ep-type-btn').forEach((btn) => {
  btn.onclick = () => {
    singleEditType = btn.getAttribute('data-type') || 'tap';
    updateSinglePointEditorFieldsVisibility();
  };
});

document.getElementById('ep-popup-pin-enabled').onchange = updatePopupTriggerPreview;
const epPopupDelayEl = document.getElementById('ep-popup-delay');
if (epPopupDelayEl) {
  epPopupDelayEl.oninput = () => {
    updatePopupDelayPresetsDisplay();
    updatePopupTriggerPreview();
  };
}
document.querySelectorAll('.ep-popup-delay-preset').forEach((btn) => {
  btn.onclick = () => {
    if (epPopupDelayEl) epPopupDelayEl.value = btn.getAttribute('data-popup-delay') || btn.getAttribute('data-pdelay') || '300';
    updatePopupDelayPresetsDisplay();
    updatePopupTriggerPreview();
  };
});
document.getElementById('ep-icon-size').oninput = updateIconSizeDisplay;
document.getElementById('ep-swipe-len').oninput = updateSwipeSummaryDisplay;
document.getElementById('ep-swipe-scale').oninput = updateSwipeSummaryDisplay;
document.querySelectorAll('.ep-scale-preset').forEach((btn) => {
  btn.onclick = () => {
    document.getElementById('ep-swipe-scale').value = btn.getAttribute('data-scale') || '2';
    updateSwipeSummaryDisplay();
  };
});

document.getElementById('btn-ep-add-cycle-text').onclick = () => {
  singleEditCycleTexts.push('');
  renderCycleTextRows();
};

document.getElementById('ep-typing-speed').oninput = updateTypingSpeedPresetsDisplay;
document.querySelectorAll('.ep-speed-preset').forEach((btn) => {
  btn.onclick = () => {
    document.getElementById('ep-typing-speed').value = btn.getAttribute('data-speed') || '30';
    updateTypingSpeedPresetsDisplay();
  };
});

document.querySelectorAll('input[name="ep-delay-unit"]').forEach((radio) => {
  radio.onchange = () => {
    const valEl = document.getElementById('ep-delay-val');
    const num = Number(valEl.value) || 0.1;
    if (radio.value === 's' && radio.checked && num >= 15) {
      valEl.value = String(Math.max(0.05, Math.round(num / 10) / 100));
    } else if (radio.value === 'ms' && radio.checked && num < 15) {
      valEl.value = String(Math.round(num * 1000));
    }
  };
});

document.getElementById('btn-ep-order-left').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  await window.ferizyShell.moveExtPoint(singleEditAccountId, singleEditPointId, 'up', singleEditTabIndex);
  currentSnapshot = await window.ferizyShell.getState();
  openSinglePointEditor(singleEditAccountId, singleEditPointId, singleEditTabIndex);
};

document.getElementById('btn-ep-order-right').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  await window.ferizyShell.moveExtPoint(singleEditAccountId, singleEditPointId, 'down', singleEditTabIndex);
  currentSnapshot = await window.ferizyShell.getState();
  openSinglePointEditor(singleEditAccountId, singleEditPointId, singleEditTabIndex);
};

document.getElementById('btn-close-edit-point').onclick = () => closeModalById('ak-edit-point-modal');
document.getElementById('btn-ep-cancel').onclick = () => closeModalById('ak-edit-point-modal');
document.getElementById('btn-ep-clear-sel').onclick = () => {
  document.getElementById('ep-selector').value = '';
};

document.getElementById('btn-ep-repick').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  pendingRepickTarget = { accountId: singleEditAccountId, tabIndex: singleEditTabIndex, pointId: singleEditPointId };
  closeModalById('ak-edit-point-modal');
  const res = await window.ferizyShell.startExtPicker(singleEditAccountId, singleEditType, singleEditPointId, singleEditTabIndex);
  if (!res || !res.ok) {
    pendingRepickTarget = null;
    openModalById('ak-edit-point-modal');
  } else {
    document.getElementById('active-meta').textContent =
      `Klik posisi baru di website Tab ${'$'}{singleEditTabIndex} untuk memperbarui titik ini (ESC batal).`;
  }
};

document.getElementById('btn-ep-delete').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  await window.ferizyShell.removeExtPoint(singleEditAccountId, singleEditPointId, singleEditTabIndex);
  closeModalById('ak-edit-point-modal');
};

document.getElementById('btn-ep-save').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  const type = singleEditType || 'tap';
  const x = Math.max(4, Math.round(Number(document.getElementById('ep-x').value) || 0));
  const y = Math.max(4, Math.round(Number(document.getElementById('ep-y').value) || 0));

  const unitEl = document.querySelector('input[name="ep-delay-unit"]:checked');
  const unit = unitEl ? unitEl.value : 'ms';
  const parsedDelay = Number(document.getElementById('ep-delay-val').value);
  const rawDelay = Number.isFinite(parsedDelay) && parsedDelay > 0 ? parsedDelay : (unit === 's' ? 0.5 : 500);
  const delayAfterMs = Math.max(15, Math.round(unit === 's' ? rawDelay * 1000 : rawDelay));

  const iconSizePct = Math.min(100, Math.max(15, Math.round(Number(document.getElementById('ep-icon-size').value) || 25)));
  const lineLengthPx = Math.min(1500, Math.max(40, Math.round(Number(document.getElementById('ep-swipe-len').value) || 350)));
  const swipeForceScale = Math.min(20, Math.max(1, Math.round(Number(document.getElementById('ep-swipe-scale').value) || 2)));
  const cycleTexts = Array.isArray(singleEditCycleTexts) && singleEditCycleTexts.length > 0
    ? singleEditCycleTexts.map((s) => String(s ?? ''))
    : [''];

  const accObj = currentSnapshot && currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === singleEditAccountId);
  const tabData = getTabPointsAndSettings(accObj, singleEditTabIndex);
  const oldPt = Array.isArray(tabData.points) ? tabData.points.find((p) => p.id === singleEditPointId) : null;

  const patch = {
    enabled: document.getElementById('ep-enabled').checked,
    type,
    label: document.getElementById('ep-label').value.trim() || `Titik ${'$'}{singleEditOrder}`,
    x,
    y,
    delayAfterMs,
    iconSizePct,
    radiusPx: Math.min(200, Math.max(5, Number(document.getElementById('ep-radius').value) || 28)),
    selector: document.getElementById('ep-selector').value.trim(),
    subLoopEnabled: false,
    subLoopTargetOrder: 1,
    subLoopCount: 1
  };

  if (type === 'swipe_up' || type === 'swipe_down') {
    patch.lineLengthPx = lineLengthPx;
    patch.swipeForceScale = swipeForceScale;
    patch.endX = x;
    patch.endY = type === 'swipe_down' ? (y + lineLengthPx) : Math.max(4, y - lineLengthPx);
    patch.durationMs = 280;
  } else if (type === 'input') {
    patch.cycleTexts = cycleTexts;
    patch.inputValue = cycleTexts[0] || '';
    patch.text = patch.inputValue;
    patch.typingSpeedMs = Math.min(1000, Math.max(5, Math.round(Number(document.getElementById('ep-typing-speed').value) || 30)));
    patch.popupDelayMs = Math.min(10000, Math.max(20, Math.round(Number(document.getElementById('ep-popup-delay').value) || 300)));
    patch.clearBeforeType = document.getElementById('ep-clear-before').checked;
    patch.focusBeforeType = document.getElementById('ep-focus-before').checked;
    patch.antiRevert = false;
    patch.popupPinEnabled = document.getElementById('ep-popup-pin-enabled').checked;
    if (patch.popupPinEnabled) {
      patch.popupPinX = (oldPt && Number.isFinite(Number(oldPt.popupPinX))) ? Number(oldPt.popupPinX) : (x + 56);
      patch.popupPinY = (oldPt && Number.isFinite(Number(oldPt.popupPinY))) ? Number(oldPt.popupPinY) : (y + 42);
    }
  }

  // Siklus default permanen V5.8: dari titik terakhir selalu mengulang kembali ke Titik #1 (mainLoopStartOrder: 1)
  await window.ferizyShell.saveExtSettings(
    singleEditAccountId,
    { loopEnabled: true, maxLoops: 0, mainLoopStartOrder: 1 },
    singleEditTabIndex
  );
  await window.ferizyShell.updateExtPoint(singleEditAccountId, singleEditPointId, patch, singleEditTabIndex);
  closeModalById('ak-edit-point-modal');
  document.getElementById('active-meta').textContent =
    `Titik #${'$'}{singleEditOrder} (${'$'}{type}) diperbarui di Akun ${'$'}{singleEditAccountId} Tab ${'$'}{singleEditTabIndex}.`;
};

document.getElementById('btn-ak-pick-target').onclick = async () => {
  const type = document.getElementById('ak-pt-type').value || 'tap';
  pendingPickerFromHeaderModal = true;
  closeModalById('autoklik-ext-modal');
  const res = await window.ferizyShell.startExtPicker(extAccountId, type, null, extTabIndex);
  if (!res || !res.ok) {
    pendingPickerFromHeaderModal = false;
    openModalById('autoklik-ext-modal');
    setAkStatus((res && res.reason) || 'Gagal membuka mode ambil titik.');
  }
};

document.getElementById('btn-ak-export-json').onclick = async () => {
  const res = await window.ferizyShell.exportExtJson(extAccountId, extTabIndex);
  if (res && res.json) {
    document.getElementById('ak-json-io').value = res.json;
    setAkStatus(`JSON AutoKlik v3.2 Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId} dimuat ke kotak teks.`);
  }
};

document.getElementById('btn-ak-import-json').onclick = async () => {
  const raw = document.getElementById('ak-json-io').value.trim();
  if (!raw) {
    setAkStatus('Tempel JSON terlebih dahulu sebelum import.');
    return;
  }
  const res = await window.ferizyShell.importExtJson(extAccountId, raw, extTabIndex);
  if (res && res.ok) {
    refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
    setAkStatus(`Berhasil import ${'$'}{res.count} titik ke Tab ${'$'}{extTabIndex} Akun ${'$'}{extAccountId}.`);
  } else {
    setAkStatus((res && res.reason) || 'Import JSON gagal.');
  }
};

function setLcmStatus(text) {
  const el = document.getElementById('lcm-status-msg');
  if (el) el.textContent = text;
}

async function refreshLoadCopyModalClipboard() {
  const badge = document.getElementById('lcm-clipboard-badge');
  if (!badge || !window.ferizyShell.getClipboardSummary) return;
  const clip = await window.ferizyShell.getClipboardSummary();
  if (clip && clip.hasClipboard) {
    badge.textContent = `Clipboard Siap: ${'$'}{clip.count} titik dari Akun ${'$'}{clip.sourceAccountId} • Tab ${'$'}{clip.sourceTabIndex}`;
  } else {
    badge.textContent = 'Clipboard: Belum ada konfigurasi yang disalin (atau pilih Sumber di bawah lalu langsung klik Paste).';
  }
}

function populateLoadCopySelects(targetAccId, targetTabIdx, sourceAccId, sourceTabIdx) {
  const accounts = (currentSnapshot && Array.isArray(currentSnapshot.accounts)) ? currentSnapshot.accounts : [];
  const tAccSel = document.getElementById('lcm-target-account');
  const sAccSel = document.getElementById('lcm-source-account');
  const tTabSel = document.getElementById('lcm-target-tab');
  const sTabSel = document.getElementById('lcm-source-tab');
  if (!tAccSel || !sAccSel || !tTabSel || !sTabSel) return;

  tAccSel.innerHTML = '';
  sAccSel.innerHTML = '';
  accounts.forEach((a) => {
    const lbl = `${'$'}{a.name || `Akun ${'$'}{a.accountId}`} (${'$'}{a.totalAccountPoints ?? a.pointsCount ?? 0} titik)`;
    const optT = document.createElement('option');
    optT.value = String(a.accountId);
    optT.textContent = lbl;
    if (a.accountId === targetAccId) optT.selected = true;
    tAccSel.appendChild(optT);

    const optS = document.createElement('option');
    optS.value = String(a.accountId);
    optS.textContent = lbl;
    if (a.accountId === sourceAccId) optS.selected = true;
    sAccSel.appendChild(optS);
  });

  tTabSel.value = String(targetTabIdx || 1);
  sTabSel.value = String(sourceTabIdx || 1);

  const accObj = accounts.find((a) => a.accountId === Number(tAccSel.value));
  const sub = document.getElementById('lcm-subtitle');
  if (sub && accObj) {
    sub.textContent = `${'$'}{accObj.name || `Akun ${'$'}{accObj.accountId}`} • Tab ${'$'}{tTabSel.value} (${'$'}{accObj.partition})`;
  }
}

async function openLoadCopyModal(accountId = null, tabIndex = null) {
  const tAcc = Number(accountId) || (currentSnapshot && currentSnapshot.activeAccountId) || 1;
  const accObj = currentSnapshot && Array.isArray(currentSnapshot.accounts)
    ? currentSnapshot.accounts.find((a) => a.accountId === tAcc)
    : null;
  const tTab = Math.min(3, Math.max(1, Number(tabIndex) || (accObj && accObj.activeTabIndex) || 1));

  let sAcc = tAcc;
  let sTab = tTab;
  if (window.ferizyShell.getClipboardSummary) {
    const clip = await window.ferizyShell.getClipboardSummary();
    if (clip && clip.hasClipboard) {
      sAcc = clip.sourceAccountId || tAcc;
      sTab = clip.sourceTabIndex || tTab;
    }
  }

  populateLoadCopySelects(tAcc, tTab, sAcc, sTab);
  await refreshLoadCopyModalClipboard();
  setLcmStatus(`Siap Load file JSON atau Copy-Paste konfigurasi untuk Akun ${'$'}{tAcc} • Tab ${'$'}{tTab}.`);
  openModalById('ak-load-copy-modal');
}

const closeLcmBtn = document.getElementById('btn-close-load-copy-modal');
if (closeLcmBtn) {
  closeLcmBtn.onclick = () => closeModalById('ak-load-copy-modal');
}

['lcm-target-account', 'lcm-target-tab'].forEach((id) => {
  const el = document.getElementById(id);
  if (el) {
    el.onchange = () => {
      const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
      const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
      const accounts = (currentSnapshot && Array.isArray(currentSnapshot.accounts)) ? currentSnapshot.accounts : [];
      const accObj = accounts.find((a) => a.accountId === tAcc);
      const sub = document.getElementById('lcm-subtitle');
      if (sub && accObj) {
        sub.textContent = `${'$'}{accObj.name || `Akun ${'$'}{accObj.accountId}`} • Tab ${'$'}{tTab} (${'$'}{accObj.partition})`;
      }
    };
  }
});

const lcmCopyTabBtn = document.getElementById('btn-lcm-copy-tab');
if (lcmCopyTabBtn) {
  lcmCopyTabBtn.onclick = async () => {
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.copyTabConfig(sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Copy ${'$'}{res.count} titik dari Akun ${'$'}{sAcc} • Tab ${'$'}{sTab} ke Clipboard.`);
      document.getElementById('active-meta').textContent =
        `Copy konfigurasi: ${'$'}{res.count} titik dari Akun ${'$'}{sAcc} Tab ${'$'}{sTab} siap di-Paste.`;
    }
  };
}

const lcmPasteTabBtn = document.getElementById('btn-lcm-paste-tab');
if (lcmPasteTabBtn) {
  lcmPasteTabBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.pasteTabConfig(tAcc, tTab, sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Paste ${'$'}{res.count} titik dari Akun ${'$'}{res.sourceAccountId} Tab ${'$'}{res.sourceTabIndex} ke Akun ${'$'}{tAcc} Tab ${'$'}{tTab}.`);
      document.getElementById('active-meta').textContent =
        `Konfigurasi (${'$'}{res.count} titik) berhasil di-Paste ke Akun ${'$'}{tAcc} • Tab ${'$'}{tTab}.`;
    } else {
      setLcmStatus((res && res.reason) || 'Gagal melakukan Paste konfigurasi.');
    }
  };
}

const lcmPasteAllTabsAccBtn = document.getElementById('btn-lcm-paste-all-tabs-acc');
if (lcmPasteAllTabsAccBtn) {
  lcmPasteAllTabsAccBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.pasteToAllTabsInAccount(tAcc, sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Paste ${'$'}{res.count} titik dari Akun ${'$'}{sAcc} Tab ${'$'}{sTab} ke SEMUA Tab (Tab 1, 2, 3) di Akun ${'$'}{tAcc}.`);
      document.getElementById('active-meta').textContent =
        `Konfigurasi (${'$'}{res.count} titik) disalin merata ke Tab 1, Tab 2 & Tab 3 pada Akun ${'$'}{tAcc}.`;
    }
  };
}

const lcmPasteAllAccountsBtn = document.getElementById('btn-lcm-paste-all-accounts');
if (lcmPasteAllAccountsBtn) {
  lcmPasteAllAccountsBtn.onclick = async () => {
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.pastePointsToAll(sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Paste ${'$'}{res.count} titik dari Akun ${'$'}{sAcc} Tab ${'$'}{sTab} ke semua akun (Tab ${'$'}{sTab}).`);
      document.getElementById('active-meta').textContent =
        `Konfigurasi Akun ${'$'}{sAcc} Tab ${'$'}{sTab} (${'$'}{res.count} titik) berhasil di-Paste ke seluruh akun.`;
    }
  };
}

const lcmLoadFileBtn = document.getElementById('btn-lcm-load-file');
if (lcmLoadFileBtn) {
  lcmLoadFileBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const res = await window.ferizyShell.openPointConfigDialog(tAcc, tTab);
    if (res && res.ok) {
      setLcmStatus(`Berhasil Load ${'$'}{res.count} titik dari file ke Akun ${'$'}{tAcc} • Tab ${'$'}{tTab}.`);
      document.getElementById('active-meta').textContent =
        `Load konfigurasi berhasil: ${'$'}{res.count} titik dimuat ke Akun ${'$'}{tAcc} Tab ${'$'}{tTab}.`;
    } else if (res && !res.canceled && res.reason) {
      setLcmStatus(`Gagal Load file: ${'$'}{res.reason}`);
    }
  };
}

const lcmSaveFileBtn = document.getElementById('btn-lcm-save-file');
if (lcmSaveFileBtn) {
  lcmSaveFileBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const res = await window.ferizyShell.savePointConfigDialog(tAcc, tTab);
    if (res && res.ok) {
      setLcmStatus(res.canceled
        ? `Konfigurasi Akun ${'$'}{tAcc} Tab ${'$'}{tTab} tersimpan di ${'$'}{res.storeFile}.`
        : `Konfigurasi Akun ${'$'}{tAcc} Tab ${'$'}{tTab} disimpan ke ${'$'}{res.filePath}`);
    }
  };
}

const lcmCopyJsonBtn = document.getElementById('btn-lcm-copy-json');
if (lcmCopyJsonBtn) {
  lcmCopyJsonBtn.onclick = async () => {
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.exportExtJson(sAcc, sTab);
    if (res && res.json) {
      const box = document.getElementById('lcm-json-box');
      if (box) box.value = res.json;
      try {
        await navigator.clipboard.writeText(res.json);
      } catch (_) {}
      setLcmStatus(`JSON konfigurasi Akun ${'$'}{sAcc} Tab ${'$'}{sTab} dimuat & disalin ke clipboard.`);
    }
  };
}

const lcmApplyJsonBtn = document.getElementById('btn-lcm-apply-json');
if (lcmApplyJsonBtn) {
  lcmApplyJsonBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const box = document.getElementById('lcm-json-box');
    const raw = box ? box.value.trim() : '';
    if (!raw) {
      setLcmStatus('Kotak JSON masih kosong. Klik "Muat & Copy JSON Sumber" atau tempel JSON terlebih dahulu.');
      return;
    }
    const res = await window.ferizyShell.importExtJson(tAcc, raw, tTab);
    if (res && res.ok) {
      setLcmStatus(`Berhasil menerapkan ${'$'}{res.count} titik dari JSON ke Akun ${'$'}{tAcc} • Tab ${'$'}{tTab}.`);
      document.getElementById('active-meta').textContent =
        `JSON konfigurasi (${'$'}{res.count} titik) diterapkan ke Akun ${'$'}{tAcc} Tab ${'$'}{tTab}.`;
    } else {
      setLcmStatus((res && res.reason) || 'Format JSON tidak valid.');
    }
  };
}

function applySystemMetrics(m) {
  if (!m) return;
  const pill = document.getElementById('sys-meter-pill');
  const cleanBtn = document.getElementById('btn-quick-clean');
  if (!pill) return;

  const browserRamLabel = m.browserRamMb >= 1024
    ? `${'$'}{Number(m.browserRamGb || (m.browserRamMb / 1024)).toFixed(2)}GB`
    : `${'$'}{m.browserRamMb || 0}MB`;
  const coreText = `CPU PC: ${'$'}{m.pcCpuPercent ?? 0}% • RAM PC: ${'$'}{m.pcRamUsedGb ?? 0}/${'$'}{m.pcRamTotalGb ?? 0}GB (${'$'}{m.pcRamPercent ?? 0}%) | Browser: ${'$'}{browserRamLabel} (${'$'}{m.browserCpuPercent ?? 0}%)`;

  if (m.severity === 'over') {
    pill.textContent = `OVER! ${'$'}{coreText}`;
    pill.className = 'pill pill-danger';
    pill.title = `${'$'}{m.reasonText || 'Beban OVER'} — Klik ⚡ Bersih Cepat untuk membebaskan RAM secara aman tanpa menyentuh tab aktif/menembak.`;
    if (cleanBtn) {
      cleanBtn.className = 'btn-small is-over';
      if (!cleanBtn.disabled) cleanBtn.textContent = '⚡ Bersih Cepat!';
    }
  } else if (m.severity === 'warn') {
    pill.textContent = coreText;
    pill.className = 'pill pill-warn';
    pill.title = `${'$'}{m.reasonText || 'Beban Mulai Padat'} — Sisa RAM PC: ${'$'}{m.pcRamFreeGb ?? 0} GB`;
    if (cleanBtn) {
      cleanBtn.className = 'btn-small is-warn';
      if (!cleanBtn.disabled) cleanBtn.textContent = '⚡ Bersih Cepat';
    }
  } else {
    pill.textContent = coreText;
    pill.className = 'pill pill-ok';
    pill.title = `Kinerja PC Normal • Sisa RAM fisik: ${'$'}{m.pcRamFreeGb ?? 0} GB`;
    if (cleanBtn) {
      cleanBtn.className = 'btn-small';
      if (!cleanBtn.disabled) cleanBtn.textContent = '⚡ Bersih Cepat';
    }
  }
}

const quickCleanBtnEl = document.getElementById('btn-quick-clean');
if (quickCleanBtnEl) {
  quickCleanBtnEl.onclick = async () => {
    quickCleanBtnEl.disabled = true;
    quickCleanBtnEl.textContent = 'Membersihkan...';
    try {
      const res = await window.ferizyShell.runQuickClean();
      if (res && res.ok) {
        if (res.after) applySystemMetrics(res.after);
        document.getElementById('active-meta').textContent =
          `Bersih Cepat selesai: ${'$'}{res.clearedSessionsCount || 0} sesi dibersihkan cache HTTP (login tetap aman), ${'$'}{res.queuedStandbyCount || 0} tab Standby tersembunyi di-recycle, hemat ${'$'}{res.savedRamMb || 0}MB RAM.`;
      } else {
        document.getElementById('active-meta').textContent = (res && res.reason) || 'Pembersihan cepat selesai.';
      }
    } finally {
      quickCleanBtnEl.disabled = false;
      quickCleanBtnEl.textContent = quickCleanBtnEl.classList.contains('is-over') ? '⚡ Bersih Cepat!' : '⚡ Bersih Cepat';
    }
  };
}

window.addEventListener('resize', syncHeaderHeight);
window.ferizyShell.onStateUpdated(renderSnapshot);
window.ferizyShell.onSystemMetrics(applySystemMetrics);
window.ferizyShell.onPlayAlarm((payload) => playAlarmTone(payload && payload.alarm));
window.ferizyShell.onOpenPointEditor((payload) => {
  if (!payload || !payload.accountId || !payload.pointId) return;
  openSinglePointEditor(payload.accountId, payload.pointId, payload.tabIndex || null);
});
window.ferizyShell.onExtPickerFinished((payload) => {
  if (!payload) return;
  const repick = pendingRepickTarget;
  pendingRepickTarget = null;
  if (repick) {
    setTimeout(() => openSinglePointEditor(repick.accountId, repick.pointId, repick.tabIndex || null), 40);
    return;
  }
  const shouldOpenModal = pendingPickerFromHeaderModal;
  pendingPickerFromHeaderModal = false;
  if (shouldOpenModal) {
    openAutoklikExtModal(payload.accountId || extAccountId, payload.tabIndex || extTabIndex);
  }
  if (payload.cancelled) {
    setAkStatus('Ambil titik dibatalkan (ESC).');
  } else if (payload.point) {
    setAkStatus(`Titik "${'$'}{payload.point.label}" (${'$'}{payload.point.x},${'$'}{payload.point.y}) tersimpan ke Tab ${'$'}{payload.tabIndex || extTabIndex} Akun ${'$'}{payload.accountId}.`);
    document.getElementById('active-meta').textContent =
      `Titik #${'$'}{payload.point.order || ''} "${'$'}{payload.point.label}" (${'$'}{payload.point.x},${'$'}{payload.point.y}) tampil di Tab ${'$'}{payload.tabIndex || extTabIndex} Akun ${'$'}{payload.accountId}.`;
  }
});
window.ferizyShell.getState().then(renderSnapshot);
window.ferizyShell.getSystemMetrics().then(applySystemMetrics);
    """.trimIndent() + "\n"

  fun trayIconSvg(): String =
    """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128">
  <rect width="128" height="128" rx="28" fill="#0B1528"/>
  <rect x="20" y="20" width="88" height="64" rx="12" fill="none" stroke="#3B82F6" stroke-width="6"/>
  <path d="M26 92 L102 92 L92 108 L36 108 Z" fill="#22D3EE"/>
  <path d="M44 46 L84 46 L94 74 L34 74 Z" fill="#22D3EE" opacity="0.9"/>
</svg>
    """.trimIndent() + "\n"
}
