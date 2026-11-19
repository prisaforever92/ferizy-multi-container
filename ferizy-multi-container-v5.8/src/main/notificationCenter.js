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

    const dedupeKey = `${eventType}_${details.accountId || 'global'}_${details.dedupeHint || ''}`;
    const now = Date.now();
    const prev = this.lastSentMap.get(dedupeKey) || 0;
    if (now - prev < this.dedupeWindowMs && !isManualTest) {
      return { ok: false, skipped: 'Deduplikasi aktif' };
    }
    this.lastSentMap.set(dedupeKey, now);

    const bodyText = details.message || `${spec.title} (Akun ${details.accountId || '-'})`;
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
      await fetch(`${baseUrl}/${encodeURIComponent(topic)}`, {
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
