package com.example.domain

object ElectronFase3Templates {

  fun eventBusJs(): String =
    """
const { EventEmitter } = require('events');

// Main Process Event Bus (<1ms) untuk sinyal bangun lintas tab/akun tanpa polling
class MainEventBus extends EventEmitter {
  constructor() {
    super();
    this.setMaxListeners(64);
    this.lastEvent = null;
  }

  broadcast(channel, payload) {
    const packet = {
      channel,
      payload,
      emittedAt: Date.now()
    };
    this.lastEvent = packet;
    this.emit(channel, packet);
    this.emit('*', packet);
    return packet;
  }
}

module.exports = { MainEventBus };
    """.trimIndent() + "\n"

  fun serverClockSyncJs(): String =
    """
class ServerClockSync {
  constructor(runtimeConfig) {
    this.enabled = runtimeConfig.serverClockSyncEnabled !== false;
    this.offsetMs = 0; // serverEpochMs - localEpochMs
    this.rttMs = 0;
    this.lastSyncedAt = null;
    this.requestStarts = new Map();
  }

  attachToSession(ses) {
    ses.webRequest.onSendHeaders({ urls: ['https://trip.ferizy.com/*'] }, (details) => {
      this.requestStarts.set(details.id, Date.now());
    });

    ses.webRequest.onHeadersReceived({ urls: ['https://trip.ferizy.com/*'] }, (details, callback) => {
      const start = this.requestStarts.get(details.id);
      this.requestStarts.delete(details.id);

      const headers = details.responseHeaders || {};
      const dateKey = Object.keys(headers).find((k) => k.toLowerCase() === 'date');
      const rawDate = dateKey && Array.isArray(headers[dateKey]) ? headers[dateKey][0] : null;

      if (this.enabled && rawDate) {
        const serverMs = Date.parse(rawDate);
        const now = Date.now();
        if (Number.isFinite(serverMs)) {
          const rtt = start ? Math.max(0, now - start) : this.rttMs;
          this.rttMs = this.rttMs ? Math.round(this.rttMs * 0.6 + rtt * 0.4) : rtt;
          this.offsetMs = Math.round(serverMs + this.rttMs / 2 - now);
          this.lastSyncedAt = now;
        }
      }
      callback({ cancel: false });
    });
  }

  getServerNowMs() {
    return Date.now() + this.offsetMs;
  }

  getLatencyCompensationMs(isEnabled) {
    if (!isEnabled) return 0;
    return Math.min(350, Math.max(0, Math.round(this.rttMs / 2)));
  }

  getSummary() {
    return {
      enabled: this.enabled,
      offsetMs: this.offsetMs,
      rttMs: this.rttMs,
      lastSyncedAt: this.lastSyncedAt
    };
  }
}

module.exports = { ServerClockSync };
    """.trimIndent() + "\n"

  fun notificationCenterJs(): String =
    """
const { Notification } = require('electron');

const EVENT_SPECS = {
  LINTASAN_DIBUKA: {
    title: 'Lintasan Dibuka / Jam Muncul',
    priority: 'high',
    tags: 'ship,bell',
    alarm: 'lintasan'
  },
  KUOTA_MUNCUL: {
    title: 'Perubahan Kuota Jadwal!',
    priority: 'urgent',
    tags: 'ticket,zap',
    alarm: 'kuota'
  },
  TEMBUS_VERIFIKASI: {
    title: 'ORDER SUKSES / TEMBUS PAYMENT!',
    priority: 'max',
    tags: 'trophy,white_check_mark',
    alarm: 'menang'
  },
  LOGOUT_TERDETEKSI: {
    title: 'Sesi Akun Logout / HTTP 401',
    priority: 'high',
    tags: 'warning,lock',
    alarm: 'logout'
  },
  PROXY_PUTUS: {
    title: 'Koneksi Proxy Terputus',
    priority: 'high',
    tags: 'rotating_light,electric_plug',
    alarm: 'proxy'
  },
  CAPTCHA_TERDETEKSI: {
    title: 'CAPTCHA / Challenge Muncul!',
    priority: 'urgent',
    tags: 'shield,robot',
    alarm: 'captcha'
  }
};

class NotificationCenter {
  constructor(runtimeConfig, getMainWindow) {
    this.runtimeConfig = runtimeConfig;
    this.getMainWindow = getMainWindow;
    this.dedupeWindowMs = Math.max(3000, (Number(runtimeConfig.notifDedupeSeconds) || 10) * 1000);
    this.settings = {
      ntfyServerUrl: (runtimeConfig.ntfyServerUrl || 'https://ntfy.sh').replace(/\/+$/, ''),
      audioAlarmMuted: Boolean(runtimeConfig.audioAlarmMuted),
      dedupeSeconds: Math.max(3, Number(runtimeConfig.notifDedupeSeconds) || 10),
      events: {
        LINTASAN_DIBUKA: runtimeConfig.notifLintasanEnabled !== false,
        KUOTA_MUNCUL: runtimeConfig.notifKuotaEnabled !== false,
        TEMBUS_VERIFIKASI: runtimeConfig.notifTembusEnabled !== false,
        LOGOUT_TERDETEKSI: runtimeConfig.notifLogoutEnabled === true,
        PROXY_PUTUS: runtimeConfig.notifProxyPutusEnabled === true,
        CAPTCHA_TERDETEKSI: runtimeConfig.notifCaptchaEnabled === true
      }
    };
    this.lastSentMap = new Map();
    this.recentLogs = [];
    this.logRotation = null;
  }

  attachLogRotation(logger) {
    this.logRotation = logger;
  }

  getTopic() {
    return (this.runtimeConfig.ntfyTopic || 'ferizy_Transsamudra05').trim();
  }

  getServerBaseUrl() {
    return (this.settings.ntfyServerUrl || 'https://ntfy.sh').replace(/\/+$/, '');
  }

  updateSettings(next = {}) {
    if (typeof next.ntfyServerUrl === 'string' && next.ntfyServerUrl.trim()) {
      this.settings.ntfyServerUrl = next.ntfyServerUrl.trim().replace(/\/+$/, '');
    }
    if (typeof next.ntfyTopic === 'string' && next.ntfyTopic.trim()) {
      this.runtimeConfig.ntfyTopic = next.ntfyTopic.trim();
    }
    if (typeof next.audioAlarmMuted === 'boolean') {
      this.settings.audioAlarmMuted = next.audioAlarmMuted;
    }
    if (Number.isFinite(Number(next.dedupeSeconds))) {
      const sec = Math.min(60, Math.max(3, Number(next.dedupeSeconds)));
      this.settings.dedupeSeconds = sec;
      this.dedupeWindowMs = sec * 1000;
    }
    if (next.events && typeof next.events === 'object') {
      for (const k of Object.keys(this.settings.events)) {
        if (typeof next.events[k] === 'boolean') {
          this.settings.events[k] = next.events[k];
        }
      }
    }
    return this.getSettings();
  }

  getSettings() {
    return {
      ntfyTopic: this.getTopic(),
      ntfyServerUrl: this.getServerBaseUrl(),
      audioAlarmMuted: this.settings.audioAlarmMuted,
      dedupeSeconds: this.settings.dedupeSeconds,
      events: { ...this.settings.events }
    };
  }

  isEventEnabled(eventType) {
    return Boolean(this.settings.events[eventType]);
  }

  isUrlMatchLintasan(url) {
    if (typeof url !== 'string') return false;
    try {
      const parsed = new URL(url);
      return parsed.origin === 'https://trip.ferizy.com' && (parsed.pathname === '/' || parsed.pathname === '');
    } catch (_) {
      return false;
    }
  }

  isUrlMatchKuota(url) {
    if (typeof url !== 'string') return false;
    return url.startsWith('https://trip.ferizy.com/ferryschedule');
  }

  async dispatch(eventType, details = {}) {
    const spec = EVENT_SPECS[eventType];
    if (!spec) return { ok: false, reason: 'Event tidak dikenal' };

    const isManualTest = Boolean(details.force);
    const shouldPushToHp = isManualTest || this.isEventEnabled(eventType);

    if (!isManualTest && eventType === 'CAPTCHA_TERDETEKSI' && !this.isEventEnabled('CAPTCHA_TERDETEKSI')) {
      return { ok: true, eventType, skipped: 'Notif CAPTCHA/Challenge sedang OFF', phonePushSent: false };
    }

    // Aturan Ketat Konteks URL & Status Berjalan (Dikonfirmasi User):
    if (!isManualTest && eventType === 'LINTASAN_DIBUKA') {
      if (!details.isRunning || !this.isUrlMatchLintasan(details.url)) {
        return { ok: false, skipped: 'Hanya aktif saat runner berjalan di https://trip.ferizy.com/' };
      }
    }
    if (!isManualTest && eventType === 'KUOTA_MUNCUL') {
      if (!details.isRunning || !this.isUrlMatchKuota(details.url)) {
        return { ok: false, skipped: 'Hanya aktif saat runner berjalan di https://trip.ferizy.com/ferryschedule' };
      }
    }

    const dedupeKey = `${'$'}{eventType}_${'$'}{details.accountId || 'global'}_${'$'}{details.dedupeHint || ''}`;
    const now = Date.now();
    const prev = this.lastSentMap.get(dedupeKey) || 0;
    if (now - prev < this.dedupeWindowMs && !isManualTest) {
      return { ok: false, skipped: 'Deduplikasi aktif' };
    }
    this.lastSentMap.set(dedupeKey, now);

    const bodyText = details.message || `${'$'}{spec.title} (Akun ${'$'}{details.accountId || '-'})`;
    this.recentLogs.unshift({
      eventType,
      title: spec.title,
      message: bodyText,
      phonePushSent: shouldPushToHp,
      at: new Date(now).toLocaleTimeString('id-ID')
    });
    if (this.recentLogs.length > 20) this.recentLogs.length = 20;
    if (this.logRotation) {
      this.logRotation.write('NOTIF_DISPATCH', {
        eventType,
        accountId: details.accountId || 0,
        phonePushSent: shouldPushToHp,
        message: bodyText
      });
    }

    // 1. Audio Alarm berbeda di Renderer Shell (hormati mute kecuali tes paksa)
    if (!this.settings.audioAlarmMuted || isManualTest) {
      const win = this.getMainWindow();
      if (win && !win.isDestroyed()) {
        win.webContents.send('notif:play-alarm', { alarm: spec.alarm, eventType, message: bodyText });
      }
    }

    // 2. Windows Native Toast (Desktop)
    if (Notification.isSupported()) {
      const toast = new Notification({
        title: spec.title,
        body: bodyText
      });
      toast.show();
    }

    // 3. Push HP ntfy.sh — HANYA event yang tercentang (atau tombol Tes ntfy manual)
    const topic = this.getTopic();
    const baseUrl = this.getServerBaseUrl();
    if (!shouldPushToHp) {
      return { ok: true, topic, eventType, phonePushSent: false, skippedPhonePush: 'Tidak dicentang user' };
    }

    try {
      await fetch(`${'$'}{baseUrl}/${'$'}{encodeURIComponent(topic)}`, {
        method: 'POST',
        headers: {
          Title: spec.title,
          Priority: spec.priority,
          Tags: spec.tags
        },
        body: bodyText
      });
    } catch (err) {
      console.warn('[ntfy] Gagal kirim push:', err.message);
    }

    return { ok: true, topic, eventType, phonePushSent: true };
  }
}

module.exports = { NotificationCenter };
    """.trimIndent() + "\n"

  fun optionalModulesJs(): String =
    """
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
    return `${'$'}{hh}:${'$'}{mm}:${'$'}{ss}`;
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
    const stamp = `${'$'}{new Date().toDateString()}_${'$'}{this.scheduledShotTimeWib}`;
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
      message: `CAPTCHA muncul di Akun ${'$'}{tab.accountId} Tab ${'$'}{tab.indexInAccount} (${'$'}{detail || 'Challenge'}). Runner di-pause otomatis.`
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
      message: `Akun ${'$'}{accountId} terdeteksi logout (${'$'}{reason}). Silakan login ulang.`
    });
  }

  matchesServiceType(slot) {
    const mode = this.flags.smartMatcherServiceType || 'all';
    if (mode === 'all') return true;
    const raw = `${'$'}{(slot && (slot.serviceType || slot.service || slot.className || slot.shipClass || slot.type)) || ''}`.toLowerCase();
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
    """.trimIndent() + "\n"
}
