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
