const TARGET_ORIGIN = 'https://trip.ferizy.com';

const PRESETS = {
  merak: { latitude: -5.9308, longitude: 105.9935 },
  bakauheni: { latitude: -5.8705, longitude: 105.7538 },
  ketapang: { latitude: -8.1449, longitude: 114.4036 },
  gilimanuk: { latitude: -8.1636, longitude: 114.4375 }
};

class GeolocationProvider {
  constructor(runtimeConfig) {
    this.runtimeConfig = runtimeConfig;
    this.accountGeo = new Map();
    const accounts = Array.isArray(runtimeConfig.accounts) ? runtimeConfig.accounts : [];
    const ids = new Set([1, 2, 3, 4, 5]);
    accounts.forEach((a) => {
      if (a && Number.isInteger(a.accountId)) ids.add(a.accountId);
    });
    for (const id of ids) {
      this.ensureAccount(id, accounts.find((a) => a.accountId === id));
    }
  }

  ensureAccount(accountId, found = null) {
    if (this.accountGeo.has(accountId)) return this.accountGeo.get(accountId);
    const accounts = Array.isArray(this.runtimeConfig.accounts) ? this.runtimeConfig.accounts : [];
    const cfg = found || accounts.find((a) => a.accountId === accountId);
    const g = (cfg && cfg.geolocation) || {};
    const entry = {
      mode: g.mode || 'preset_0ms',
      preset: g.preset || 'merak',
      latitude: typeof g.latitude === 'number' ? g.latitude : PRESETS.merak.latitude,
      longitude: typeof g.longitude === 'number' ? g.longitude : PRESETS.merak.longitude
    };
    this.accountGeo.set(accountId, entry);
    return entry;
  }

  isTrustedFerizyOrigin(originOrUrl) {
    return typeof originOrUrl === 'string' && originOrUrl.startsWith(TARGET_ORIGIN);
  }

  attachToSession(ses, accountId) {
    ses.setPermissionRequestHandler((webContents, permission, callback, details) => {
      const origin = (details && details.requestingUrl) || webContents.getURL() || '';
      if ((permission === 'geolocation' || permission === 'notifications') && this.isTrustedFerizyOrigin(origin)) {
        callback(true);
        return;
      }
      callback(false);
    });

    ses.setPermissionCheckHandler((webContents, permission, requestingOrigin) => {
      const origin = requestingOrigin || (webContents && webContents.getURL()) || '';
      if ((permission === 'geolocation' || permission === 'notifications') && this.isTrustedFerizyOrigin(origin)) {
        return true;
      }
      return false;
    });
  }

  updateAccountGeo(accountId, nextGeo) {
    const presetCoords = PRESETS[nextGeo.preset];
    const baseLat = presetCoords ? presetCoords.latitude : Number(nextGeo.latitude) || PRESETS.merak.latitude;
    const baseLng = presetCoords ? presetCoords.longitude : Number(nextGeo.longitude) || PRESETS.merak.longitude;

    const updated = {
      mode: nextGeo.mode || 'preset_0ms',
      preset: nextGeo.preset || 'merak',
      latitude: baseLat,
      longitude: baseLng
    };
    this.accountGeo.set(accountId, updated);
    return this.getEffectiveGeo(accountId);
  }

  getEffectiveGeo(accountId) {
    const cfg = this.accountGeo.get(accountId) || {
      mode: 'preset_0ms',
      preset: 'merak',
      latitude: PRESETS.merak.latitude,
      longitude: PRESETS.merak.longitude
    };

    const latJitter = (accountId - 3) * 0.00009;
    const lngJitter = (accountId % 2 === 0 ? 1 : -1) * accountId * 0.00007;

    return {
      ...cfg,
      effectiveLatitude: Number((cfg.latitude + latJitter).toFixed(6)),
      effectiveLongitude: Number((cfg.longitude + lngJitter).toFixed(6)),
      accuracy: 15 + accountId * 3
    };
  }
}

module.exports = { GeolocationProvider };
