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
          message: `Lintasan terbuka terdeteksi di Akun ${tab.accountId} Tab ${tab.indexInAccount}`
        });
      }

      // 2. Notif Perubahan Kuota HANYA saat konfigurasi berjalan di https://trip.ferizy.com/ferryschedule
      if (packet.kind === 'schedule' && Array.isArray(packet.slots) && isRunning) {
        const changedSlots = [];
        for (const s of packet.slots) {
          const slotQuota = Number((s && (s.quota ?? s.available ?? s.sisa)) ?? 0);
          const slotTime = String((s && (s.departureTime || s.time || s.jam || s.etd)) || 'slot');
          const slotSvc = String((s && (s.serviceType || s.service || '')) || '');
          const key = `${tab.accountId}_${slotTime}_${slotSvc}`;
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
            dedupeHint: `${jamLabel}_${firstChange.nextQuota}`,
            message: `Perubahan kuota Akun ${tab.accountId} Tab ${tab.indexInAccount} (${jamLabel}): ${firstChange.prevQuota} -> ${firstChange.nextQuota}`
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
      message: `Order Sukses / Tembus! Tab ${tabId} -> ${url} (Auto-Stop 0ms ${stopMode}).`
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
      return { ok: false, reason: `AutoKlik-v3.2-Mandiri-Extension nonaktif untuk Akun ${tab.accountId} Tab ${tabIdx}` };
    }

    const points = this.pointStore.getAccountPoints(tab.accountId, tabIdx).filter((p) => p.enabled !== false);
    if (!points.length) {
      return { ok: false, reason: `Profil titik Akun ${tab.accountId} Tab ${tabIdx} masih kosong. Klik +Titik atau Load konfigurasi terlebih dahulu.` };
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
      console.error(`[Automation] Error di ${tabId}:`, err.message);
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
            let cx = ${fallbackX};
            let cy = ${fallbackY};
            let diam = ${fallbackDiameter};
            if (host && host.shadowRoot) {
              const pid = ${JSON.stringify(pid)};
              const isPopup = ${popupFlag ? 'true' : 'false'};
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
            const px = ${xVal};
            const py = ${yVal};
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
