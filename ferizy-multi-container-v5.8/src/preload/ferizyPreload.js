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

  const cfg = ${JSON.stringify(geoConfig)};
  const flags = ${JSON.stringify(fase3Flags)};

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
  const key = payload.selector || `${payload.x}_${payload.y}`;
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
    return `#${el.id}`;
  }
  const tag = el.tagName.toLowerCase();
  const nameAttr = el.getAttribute('name');
  if (nameAttr && /^[a-zA-Z0-9_.\-]+$/.test(nameAttr)) {
    return `${tag}[name="${nameAttr}"]`;
  }
  const parts = [];
  let cur = el;
  while (cur && cur.nodeType === 1 && parts.length < 4) {
    let seg = cur.tagName.toLowerCase();
    if (cur.id && /^[a-zA-Z][\w:-]*$/.test(cur.id)) {
      parts.unshift(`#${cur.id}`);
      break;
    }
    const parent = cur.parentElement;
    if (parent) {
      const sameTagSiblings = Array.from(parent.children).filter((c) => c.tagName === cur.tagName);
      if (sameTagSiblings.length > 1) {
        seg += `:nth-of-type(${sameTagSiblings.indexOf(cur) + 1})`;
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
  ghostPin.innerHTML = `<span style="position:relative;z-index:2;">${nextPointNum}</span><span style="position:absolute;left:50%;top:2px;bottom:2px;width:1px;background:rgba(255,255,255,0.65);transform:translateX(-50%);"></span><span style="position:absolute;top:50%;left:2px;right:2px;height:1px;background:rgba(255,255,255,0.65);transform:translateY(-50%);"></span><span style="position:absolute;left:50%;top:50%;width:5px;height:5px;border-radius:50%;background:#fde047;border:1px solid #020617;transform:translate(-50%,-50%);z-index:3;"></span>`;

  const banner = document.createElement('div');
  banner.setAttribute('data-autoklik-overlay', '1');
  banner.style.cssText = 'position:fixed;top:8px;left:50%;transform:translateX(-50%);z-index:2147483647;background:#0b1528;color:#cffafe;border:1px solid #22d3ee;border-radius:8px;padding:6px 12px;font-family:"Segoe UI",sans-serif;font-size:12px;box-shadow:0 6px 24px rgba(0,0,0,0.45);pointer-events:none;';
  banner.textContent = replacePointId
    ? `AutoKlik v3.2 (Akun ${targetAccountId} Tab ${targetTabIndex}) • Arahkan titik tengah kuning (+) ke posisi klik baru • ESC = Batal`
    : `AutoKlik-v3.2 (Akun ${targetAccountId} Tab ${targetTabIndex}) • Arahkan titik tengah kuning (+) ke posisi target • ESC = Batal`;

  (document.body || document.documentElement).appendChild(overlay);
  (document.body || document.documentElement).appendChild(ghostPin);
  (document.body || document.documentElement).appendChild(banner);

  const onMove = (e) => {
    const cx = Math.round(e.clientX);
    const cy = Math.round(e.clientY);
    ghostPin.style.display = 'flex';
    ghostPin.style.left = `${cx}px`;
    ghostPin.style.top = `${cy}px`;

    const el = elementFromPointClean(cx, cy);
    if (!el || el.hasAttribute('data-autoklik-overlay')) return;
    const r = el.getBoundingClientRect();
    overlay.style.display = 'block';
    overlay.style.left = `${r.left}px`;
    overlay.style.top = `${r.top}px`;
    overlay.style.width = `${r.width}px`;
    overlay.style.height = `${r.height}px`;
    banner.textContent = `AutoKlik v3.2 (Akun ${targetAccountId} T${targetTabIndex}) • Titik #${nextPointNum} tepat di x=${cx}, y=${cy} (${el.tagName.toLowerCase()}) • Klik untuk kunci posisi (ESC batal)`;
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
    const label = String(rawLabel || 'Target').replace(/\s+/g, ' ').trim().slice(0, 28) || `Target (${x},${y})`;
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
  storeFile: `akun_${accountId}.json`
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
      akFloatingPill.style.right = `${floatingPillPos.right}px`;
      akFloatingPill.style.bottom = `${floatingPillPos.bottom}px`;
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
  akFloatingPill.style.right = `${floatingPillPos.right}px`;
  akFloatingPill.style.bottom = `${floatingPillPos.bottom}px`;

  // Sesuai permintaan user: saat start fitur autoklik, popup melayang hanya menampilkan STOP (ukuran kecil & rapi)
  if (isRunning) {
    akFloatingPill.innerHTML = `
      <span class="ak-floating-runner-meta" title="A${accId}•T${tIdx} (Running)">A${accId}•T${tIdx}</span>
      <button id="ak-floating-runner-btn" type="button" class="ak-floating-runner-btn is-stop" title="Hentikan AutoKlik">■ Stop</button>
    `;
  } else {
    akFloatingPill.innerHTML = `
      <span class="ak-floating-runner-meta" title="A${accId}•T${tIdx} (${ptsCount} Titik)">A${accId}•T${tIdx}</span>
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
        `Konfigurasi Titik Akun ${accId} Tab ${tIdx}:\n1. Ketik 'load' untuk Buka File JSON\n2. Ketik 'copy' untuk Salin Konfigurasi ke Clipboard\n3. Ketik 'paste' untuk Tempel Konfigurasi dari Clipboard`,
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
      pinEl.style.left = `${curX - layerOffset.left}px`;
      pinEl.style.top = `${curY - layerOffset.top}px`;
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
    .map((p) => `${p.id}:${p.x},${p.y},${p.endX ?? ''},${p.endY ?? ''},${p.type}:${p.enabled !== false ? 1 : 0}:${p.iconSizePct || 25}:${p.lineLengthPx || 350}:${p.popupPinEnabled ? 1 : 0}:${p.popupPinX ?? ''},${p.popupPinY ?? ''}`)
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
    pin.className = `ak-pin type-${type} ${isEnabled ? '' : 'disabled'} ${isLocked ? 'locked' : ''} ${isRunning && isEnabled ? 'running' : ''} ${isActiveStep ? 'is-active-step' : ''}`;
    pin.dataset.pointId = pt.id || '';
    pin.style.left = `${x - layerOffset.left}px`;
    pin.style.top = `${y - layerOffset.top}px`;
    pin.style.setProperty('--ak-size', `${diameter}px`);
    pin.style.setProperty('--ak-font', `${fontSize}px`);

    const cross = document.createElement('div');
    cross.className = 'ak-crosshair';
    const numSpan = document.createElement('span');
    numSpan.className = 'ak-pin-num';
    numSpan.textContent = String(idx + 1);
    cross.appendChild(numSpan);
    cross.title = isRunning
      ? `Titik #${idx + 1} (x=${x}, y=${y}) sedang berjalan`
      : isLocked
        ? `Titik #${idx + 1} (x=${x}, y=${y}) dikunci (Klik tembus ke website)`
        : `Titik #${idx + 1} (x=${x}, y=${y} • Diameter Bulat Penuh ${diameter}px) • Seret: Pindah Posisi • Klik: Atur Titik (Website Tetap Diam)`;

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
      endPin.className = `ak-pin type-${type} swipe-end ${isEnabled ? '' : 'disabled'} ${isLocked ? 'locked' : ''} ${isRunning && isEnabled ? 'running' : ''}`;
      endPin.style.left = `${endX - layerOffset.left}px`;
      endPin.style.top = `${endY - layerOffset.top}px`;

      const endCross = document.createElement('div');
      endCross.className = 'ak-crosshair';
      endCross.textContent = endY > y ? '▼' : '▲';
      endCross.title = `Ujung arah Swipe #${idx + 1} (x=${endX}, y=${endY})`;

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
      popPin.className = `ak-pin type-autofill-pin ${isEnabled ? '' : 'disabled'} ${isLocked ? 'locked' : ''} ${isRunning && isEnabled ? 'running' : ''}`;
      popPin.dataset.popupPinFor = pt.id || '';
      popPin.style.left = `${pinX - layerOffset.left}px`;
      popPin.style.top = `${pinY - layerOffset.top}px`;

      const popCross = document.createElement('div');
      popCross.className = 'ak-crosshair';
      const popNum = document.createElement('span');
      popNum.className = 'ak-pin-num';
      popNum.textContent = `#${idx + 1}`;
      popCross.appendChild(popNum);
      popCross.title = `Pin Ungu Autofill Titik #${idx + 1} (x=${pinX}, y=${pinY}) • Titik tengah kuning (+) = Posisi Klik Persis`;

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
    ripple.style.width = `${d}px`;
    ripple.style.height = `${d}px`;
    ripple.style.left = `${Math.round(x) - layerOffset.left}px`;
    ripple.style.top = `${Math.round(y) - layerOffset.top}px`;
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
    if (actionable.id) return `#${CSS.escape(actionable.id)}`;
    const testId = actionable.getAttribute && actionable.getAttribute('data-testid');
    if (testId) return `[data-testid="${CSS.escape(testId)}"]`;
    if (actionable.name && actionable.tagName) {
      return `${actionable.tagName.toLowerCase()}[name="${CSS.escape(actionable.name)}"]`;
    }
    const href = actionable.getAttribute && actionable.getAttribute('href');
    if (href && href.length > 1 && href.length < 90 && !href.startsWith('javascript:')) {
      return `a[href="${CSS.escape(href)}"]`;
    }
    const parts = [];
    let cur = actionable;
    let anchoredById = false;
    for (let depth = 0; depth < 4 && cur && cur.nodeType === 1 && cur !== document.body; depth++) {
      if (cur.id) {
        parts.unshift(`#${CSS.escape(cur.id)}`);
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
      parts.unshift(`${tag}:nth-of-type(${nth})`);
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
