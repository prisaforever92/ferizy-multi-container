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
