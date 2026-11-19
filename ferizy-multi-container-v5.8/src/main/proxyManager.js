class ProxyManager {
  constructor(runtimeConfig, notificationCenter) {
    this.runtimeConfig = runtimeConfig;
    this.notificationCenter = notificationCenter;
    this.accountProxies = new Map();
    this.sessions = new Map();
    this.webContentsToAccount = new Map();

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
    if (this.accountProxies.has(accountId)) return this.accountProxies.get(accountId);
    const accounts = Array.isArray(this.runtimeConfig.accounts) ? this.runtimeConfig.accounts : [];
    const cfg = found || accounts.find((a) => a.accountId === accountId);
    const p = (cfg && cfg.proxy) || {};
    const entry = {
      protocol: p.protocol || 'direct',
      host: p.host || '',
      port: Number(p.port) || 8080,
      username: p.username || '',
      password: p.password || '',
      ipLabel: p.ipLabel || (p.host ? `${p.host}:${p.port}` : 'Direct ISP'),
      lastCheckedIp: null,
      status: p.protocol && p.protocol !== 'direct' && p.host ? 'Proxy Siap' : 'Direct'
    };
    this.accountProxies.set(accountId, entry);
    return entry;
  }

  registerWebContents(webContentsId, accountId) {
    this.webContentsToAccount.set(webContentsId, accountId);
  }

  unregisterWebContents(webContentsId) {
    this.webContentsToAccount.delete(webContentsId);
  }

  getCredentialsForWebContents(webContentsId) {
    const accountId = this.webContentsToAccount.get(webContentsId);
    if (!accountId) return null;
    const cfg = this.accountProxies.get(accountId);
    if (!cfg || !cfg.username) return null;
    return { username: cfg.username, password: cfg.password };
  }

  buildProxyRules(cfg) {
    const cleanHost = cfg && cfg.host ? String(cfg.host).trim().replace(/^https?:\/\//i, '').replace(/^socks5?:\/\//i, '') : '';
    if (!cfg || cfg.protocol === 'direct' || !cleanHost) {
      return { mode: 'direct' };
    }
    const rules =
      cfg.protocol === 'socks5'
        ? `socks5://${cleanHost}:${cfg.port}`
        : `http=${cleanHost}:${cfg.port};https=${cleanHost}:${cfg.port}`;
    return {
      mode: 'fixed_servers',
      proxyRules: rules,
      proxyBypassRules: '<-loopback>'
    };
  }

  async attachToSession(ses, accountId) {
    this.sessions.set(accountId, ses);
    if (typeof ses.setWebRTCIPHandlingPolicy === 'function') {
      ses.setWebRTCIPHandlingPolicy('disable_non_proxied_udp');
    }

    const cfg = this.accountProxies.get(accountId);
    try {
      await ses.setProxy(this.buildProxyRules(cfg));
    } catch (_) {}
  }

  async updateAccountProxy(accountId, nextCfg) {
    const current = this.accountProxies.get(accountId) || {};
    const merged = {
      ...current,
      protocol: nextCfg.protocol || 'direct',
      host: (nextCfg.host || '').trim(),
      port: Number(nextCfg.port) || 8080,
      username: nextCfg.username || '',
      password: nextCfg.password || '',
      ipLabel: nextCfg.host ? `${nextCfg.host}:${nextCfg.port}` : 'Direct ISP',
      status: nextCfg.protocol && nextCfg.protocol !== 'direct' && nextCfg.host ? 'Proxy Siap' : 'Direct'
    };
    this.accountProxies.set(accountId, merged);

    const ses = this.sessions.get(accountId);
    if (ses) {
      if (typeof ses.setWebRTCIPHandlingPolicy === 'function') {
        ses.setWebRTCIPHandlingPolicy('disable_non_proxied_udp');
      }
      try {
        await ses.setProxy(this.buildProxyRules(merged));
      } catch (_) {}
    }
    return merged;
  }

  async checkExternalIp(accountId) {
    const ses = this.sessions.get(accountId);
    const cfg = this.accountProxies.get(accountId);
    if (!ses || !cfg) return { ok: false, error: 'Session tidak tersedia' };

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 5000);
    try {
      const res = await ses.fetch('https://api.ipify.org?format=json', { signal: controller.signal });
      const data = await res.json();
      cfg.lastCheckedIp = data.ip;
      cfg.status = cfg.protocol !== 'direct' && cfg.host ? `IP Berbeda (${data.ip})` : `Direct (${data.ip})`;
      return { ok: true, ip: data.ip, status: cfg.status };
    } catch (err) {
      cfg.status = 'Putus';
      if (this.notificationCenter) {
        this.notificationCenter.dispatch('PROXY_PUTUS', {
          accountId,
          dedupeHint: cfg.host || 'direct',
          message: `Proxy Akun ${accountId} (${cfg.ipLabel}) terputus: ${err.message}`
        });
      }
      return { ok: false, error: err.message, status: 'Putus' };
    } finally {
      clearTimeout(timer);
    }
  }

  getProxySummary(accountId) {
    return this.accountProxies.get(accountId);
  }
}

module.exports = { ProxyManager };
