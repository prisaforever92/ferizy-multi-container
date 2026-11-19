class OptionalModulesManager {
  constructor(runtimeConfig, sessionManager, automationEngine, notificationCenter, serverClockSync, eventBus, showAndFocusTab) {
    this.sessionManager = sessionManager;
    this.automationEngine = automationEngine;
    this.notificationCenter = notificationCenter;
    this.serverClockSync = serverClockSync;
    this.eventBus = eventBus;
    this.showAndFocusTab = showAndFocusTab;

    this.flags = {
      gentleKeepAliveEnabled: runtimeConfig.gentleKeepAliveEnabled !== false,
      captchaAutoPopEnabled: runtimeConfig.captchaAutoPopEnabled !== false,
      smartTargetMatcherEnabled: runtimeConfig.smartTargetMatcherEnabled !== false,
      smartMatcherFallbackNearest: runtimeConfig.smartMatcherFallbackNearest !== false,
      smartMatcherServiceType: ['all', 'express', 'reguler'].includes(runtimeConfig.smartMatcherServiceType)
        ? runtimeConfig.smartMatcherServiceType
        : 'all',
      latencyCompensationEnabled: Boolean(runtimeConfig.latencyCompensationEnabled),
      advancedStopMode: runtimeConfig.advancedStopMode || 'per_tab',
      autoWakeMode: ['monitor_only', 'same_account', 'all_accounts'].includes(runtimeConfig.autoWakeMode)
        ? runtimeConfig.autoWakeMode
        : 'same_account'
    };

    this.keepAliveIntervalMinutes = Math.min(20, Math.max(5, Number(runtimeConfig.keepAliveIntervalMinutes) || 12));
    this.fastRequestTimeoutMs = Math.min(10000, Math.max(3000, Number(runtimeConfig.fastRequestTimeoutMs) || 4500));
    this.priorityHours = String(runtimeConfig.priorityHoursCsv || '08:00,10:00,12:00,14:00,20:00')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    this.scheduledShotEnabled = Boolean(runtimeConfig.scheduledShotEnabled);
    this.scheduledShotTimeWib = this.normalizeHms(runtimeConfig.scheduledShotTimeWib || '00:00:00');
    this.lastShotStamp = null;

    this.keepAliveIndex = 0;
    this.keepAliveTimer = null;
    this.scheduleTimer = null;
    this.lastKeepAliveMap = new Map();
    this.lastMatchedSlot = null;
    this.autoWakeCount = 0;
  }

  normalizeHms(raw) {
    const m = String(raw || '').trim().match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/);
    if (!m) return '00:00:00';
    const hh = String(Math.min(23, Math.max(0, Number(m[1])))).padStart(2, '0');
    const mm = String(Math.min(59, Math.max(0, Number(m[2])))).padStart(2, '0');
    const ss = String(Math.min(59, Math.max(0, Number(m[3] || 0)))).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
  }

  start() {
    this.restartKeepAliveTimer();
    this.restartScheduleTimer();
  }

  stop() {
    if (this.keepAliveTimer) {
      clearInterval(this.keepAliveTimer);
      this.keepAliveTimer = null;
    }
    if (this.scheduleTimer) {
      clearInterval(this.scheduleTimer);
      this.scheduleTimer = null;
    }
  }

  updateFlags(next = {}) {
    if (typeof next.gentleKeepAliveEnabled === 'boolean') {
      this.flags.gentleKeepAliveEnabled = next.gentleKeepAliveEnabled;
    }
    if (Number.isFinite(Number(next.keepAliveIntervalMinutes))) {
      this.keepAliveIntervalMinutes = Math.min(20, Math.max(5, Number(next.keepAliveIntervalMinutes)));
    }
    if (typeof next.captchaAutoPopEnabled === 'boolean') {
      this.flags.captchaAutoPopEnabled = next.captchaAutoPopEnabled;
    }
    if (typeof next.smartTargetMatcherEnabled === 'boolean') {
      this.flags.smartTargetMatcherEnabled = next.smartTargetMatcherEnabled;
    }
    if (typeof next.smartMatcherFallbackNearest === 'boolean') {
      this.flags.smartMatcherFallbackNearest = next.smartMatcherFallbackNearest;
    }
    if (['all', 'express', 'reguler'].includes(next.smartMatcherServiceType)) {
      this.flags.smartMatcherServiceType = next.smartMatcherServiceType;
    }
    if (typeof next.latencyCompensationEnabled === 'boolean') {
      this.flags.latencyCompensationEnabled = next.latencyCompensationEnabled;
    }
    if (Number.isFinite(Number(next.fastRequestTimeoutMs))) {
      this.fastRequestTimeoutMs = Math.min(10000, Math.max(3000, Number(next.fastRequestTimeoutMs)));
    }
    if (['per_tab', 'per_account', 'all_accounts'].includes(next.advancedStopMode)) {
      this.flags.advancedStopMode = next.advancedStopMode;
    }
    if (['monitor_only', 'same_account', 'all_accounts'].includes(next.autoWakeMode)) {
      this.flags.autoWakeMode = next.autoWakeMode;
    }
    if (typeof next.priorityHoursCsv === 'string') {
      this.priorityHours = next.priorityHoursCsv
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
    }
    this.restartKeepAliveTimer();
    return this.getState();
  }

  updateSchedule(next = {}) {
    if (typeof next.scheduledShotEnabled === 'boolean') {
      this.scheduledShotEnabled = next.scheduledShotEnabled;
    }
    if (typeof next.scheduledShotTimeWib === 'string') {
      this.scheduledShotTimeWib = this.normalizeHms(next.scheduledShotTimeWib);
    }
    this.lastShotStamp = null;
    this.restartScheduleTimer();
    return this.getScheduleSummary();
  }

  restartKeepAliveTimer() {
    if (this.keepAliveTimer) {
      clearInterval(this.keepAliveTimer);
      this.keepAliveTimer = null;
    }
    if (!this.flags.gentleKeepAliveEnabled) return;

    const ids = (this.sessionManager && typeof this.sessionManager.getAccountIds === 'function')
      ? this.sessionManager.getAccountIds()
      : [1, 2, 3, 4, 5];
    const stepMs = Math.max(45000, Math.round((this.keepAliveIntervalMinutes * 60000) / Math.max(1, ids.length)));
    this.keepAliveTimer = setInterval(() => this.pingNextAccount(), stepMs);
  }

  restartScheduleTimer() {
    if (this.scheduleTimer) {
      clearInterval(this.scheduleTimer);
      this.scheduleTimer = null;
    }
    if (!this.scheduledShotEnabled) return;
    this.scheduleTimer = setInterval(() => this.tickScheduledShot(), 100);
  }

  getRemainingMsToTarget() {
    if (!this.scheduledShotEnabled) return null;
    const nowServerMs = this.serverClockSync ? this.serverClockSync.getServerNowMs() : Date.now();
    const d = new Date(nowServerMs);
    const [hh, mm, ss] = this.scheduledShotTimeWib.split(':').map((n) => Number(n));
    const target = new Date(nowServerMs);
    target.setHours(hh, mm, ss, 0);
    if (target.getTime() < d.getTime() - 1000) {
      target.setDate(target.getDate() + 1);
    }
    return Math.max(0, target.getTime() - d.getTime());
  }

  tickScheduledShot() {
    if (!this.scheduledShotEnabled) return;
    const rem = this.getRemainingMsToTarget();
    const rttComp =
      this.flags.latencyCompensationEnabled && this.serverClockSync
        ? this.serverClockSync.getLatencyCompensationMs(true)
        : 0;
    const stamp = `${new Date().toDateString()}_${this.scheduledShotTimeWib}`;
    if (rem !== null && rem <= rttComp && this.lastShotStamp !== stamp) {
      this.lastShotStamp = stamp;
      this.scheduledShotEnabled = false;
      this.restartScheduleTimer();

      const started = [];
      for (const tab of this.sessionManager.tabs.values()) {
        const runner = this.automationEngine.getRunnerState(tab.tabId);
        if (runner.state === 'IDLE') {
          this.automationEngine.startTab(tab.tabId);
          started.push(tab.tabId);
        }
      }
      if (this.eventBus) {
        this.eventBus.broadcast('clock:scheduled-shot-fired', {
          targetTime: this.scheduledShotTimeWib,
          rttCompMs: rttComp,
          startedTabs: started
        });
      }
      this.showAndFocusTab(null);
    }
  }

  getScheduleSummary() {
    return {
      scheduledShotEnabled: this.scheduledShotEnabled,
      scheduledShotTimeWib: this.scheduledShotTimeWib,
      remainingMs: this.getRemainingMsToTarget()
    };
  }

  async pingNextAccount() {
    if (!this.flags.gentleKeepAliveEnabled) return;
    const ids = (this.sessionManager && typeof this.sessionManager.getAccountIds === 'function')
      ? this.sessionManager.getAccountIds()
      : [1, 2, 3, 4, 5];
    if (!ids.length) return;
    const accountId = ids[this.keepAliveIndex % ids.length];
    this.keepAliveIndex = (this.keepAliveIndex + 1) % ids.length;

    const ses = this.sessionManager.getSession(accountId);
    if (!ses) return;
    try {
      const cookies = await ses.cookies.get({ domain: 'ferizy.com' });
      if (!cookies || cookies.length === 0) return;

      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 5000);
      const res = await ses.fetch('https://trip.ferizy.com/', {
        method: 'HEAD',
        signal: controller.signal
      });
      clearTimeout(t);
      this.lastKeepAliveMap.set(accountId, new Date().toLocaleTimeString('id-ID'));
      if (res && res.status === 401) {
        this.handleLogoutDetected(accountId, null, 'HTTP 401 saat Keep-Alive');
      }
    } catch (_) {}
  }

  getLastKeepAliveAt(accountId) {
    return this.lastKeepAliveMap.get(accountId) || null;
  }

  triggerAutoWake(sourceTab, triggerKind) {
    if (!sourceTab || this.flags.autoWakeMode === 'monitor_only') return [];
    const woken = [];
    const candidates =
      this.flags.autoWakeMode === 'all_accounts'
        ? Array.from(this.sessionManager.tabs.values())
        : this.sessionManager.getAccountTabs(sourceTab.accountId);

    for (const peer of candidates) {
      if (peer.tabId === sourceTab.tabId) continue;
      const runner = this.automationEngine.getRunnerState(peer.tabId);
      if (runner.state === 'IDLE') {
        this.automationEngine.startTab(peer.tabId);
        woken.push(peer.tabId);
      }
    }

    if (woken.length > 0) {
      this.autoWakeCount += woken.length;
      if (this.eventBus) {
        this.eventBus.broadcast('bus:auto-wake-strikers', {
          sourceTabId: sourceTab.tabId,
          triggerKind,
          mode: this.flags.autoWakeMode,
          wokenTabs: woken
        });
      }
    }
    return woken;
  }

  resumeAllPausedCaptchaTabs() {
    const resumed = [];
    for (const tab of this.sessionManager.tabs.values()) {
      const runner = this.automationEngine.getRunnerState(tab.tabId);
      if (runner.state === 'PAUSED_CAPTCHA') {
        this.automationEngine.startTab(tab.tabId);
        resumed.push(tab.tabId);
      }
    }
    return { ok: true, resumedCount: resumed.length, resumed };
  }

  handleCaptchaDetected(tab, detail) {
    if (!tab) return;
    this.automationEngine.pauseForCaptcha(tab.tabId);
    if (this.flags.captchaAutoPopEnabled) {
      this.showAndFocusTab(tab.tabId);
    }
    this.notificationCenter.dispatch('CAPTCHA_TERDETEKSI', {
      accountId: tab.accountId,
      dedupeHint: tab.tabId,
      message: `CAPTCHA muncul di Akun ${tab.accountId} Tab ${tab.indexInAccount} (${detail || 'Challenge'}). Runner di-pause otomatis.`
    });
  }

  handleLogoutDetected(accountId, tabId, reason) {
    if (tabId) {
      this.automationEngine.markLogoutStopped(tabId);
    } else {
      for (const t of this.sessionManager.getAccountTabs(accountId)) {
        this.automationEngine.markLogoutStopped(t.tabId);
      }
    }
    this.notificationCenter.dispatch('LOGOUT_TERDETEKSI', {
      accountId,
      dedupeHint: reason,
      message: `Akun ${accountId} terdeteksi logout (${reason}). Silakan login ulang.`
    });
  }

  matchesServiceType(slot) {
    const mode = this.flags.smartMatcherServiceType || 'all';
    if (mode === 'all') return true;
    const raw = `${(slot && (slot.serviceType || slot.service || slot.className || slot.shipClass || slot.type)) || ''}`.toLowerCase();
    if (!raw) return true;
    if (mode === 'express') return /express|ekspres/.test(raw);
    if (mode === 'reguler') return /reguler|regular/.test(raw);
    return true;
  }

  matchBestScheduleSlot(schedules) {
    if (!this.flags.smartTargetMatcherEnabled || !Array.isArray(schedules)) return null;
    const available = schedules.filter(
      (s) => Number(s.quota || s.available || s.sisa || 0) > 0 && this.matchesServiceType(s)
    );
    if (!available.length) return null;

    for (const hour of this.priorityHours) {
      const found = available.find((s) => {
        const timeStr = String(s.departureTime || s.time || s.jam || s.etd || '');
        return timeStr.includes(hour);
      });
      if (found) {
        this.lastMatchedSlot = found;
        return found;
      }
    }

    if (!this.flags.smartMatcherFallbackNearest) {
      return null;
    }
    this.lastMatchedSlot = available[0];
    return available[0];
  }

  getState() {
    return {
      ...this.flags,
      keepAliveIntervalMinutes: this.keepAliveIntervalMinutes,
      fastRequestTimeoutMs: this.fastRequestTimeoutMs,
      priorityHoursCsv: this.priorityHours.join(','),
      lastMatchedSlot: this.lastMatchedSlot,
      autoWakeCount: this.autoWakeCount,
      scheduleTimer: this.getScheduleSummary()
    };
  }
}

module.exports = { OptionalModulesManager };
