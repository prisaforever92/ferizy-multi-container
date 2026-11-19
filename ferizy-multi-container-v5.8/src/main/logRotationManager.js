const fs = require('fs');
const os = require('os');
const path = require('path');
let electronApp = null;
try {
  const el = require('electron');
  if (el && el.app) electronApp = el.app;
} catch (_) {}

class LogRotationManager {
  constructor(runtimeConfig, customLogsDir) {
    const defaultLogsDir = electronApp && typeof electronApp.getPath === 'function'
      ? path.join(electronApp.getPath('userData'), 'logs')
      : path.join(os.tmpdir(), 'ferizy-logs');
    this.logsDir = customLogsDir || defaultLogsDir;
    this.activeFile = path.join(this.logsDir, 'ferizy-runtime.jsonl');
    this.maxSizeMb = Math.min(200, Math.max(5, Number(runtimeConfig.logMaxSizeMb) || 50));
    this.retentionDays = Math.min(30, Math.max(1, Number(runtimeConfig.logRetentionDays) || 7));
    this.totalEventsWritten = 0;
    this.lastPrunedAt = null;
    this.pruneTimer = null;
    this.ensureLogsDir();
  }

  ensureLogsDir() {
    try {
      if (!fs.existsSync(this.logsDir)) {
        fs.mkdirSync(this.logsDir, { recursive: true });
      }
    } catch (err) {
      console.error('[LogRotation] Gagal buat folder log:', err.message);
    }
  }

  start() {
    this.pruneOldLogs();
    if (!this.pruneTimer) {
      this.pruneTimer = setInterval(() => this.pruneOldLogs(), 6 * 60 * 60 * 1000);
    }
  }

  stop() {
    if (this.pruneTimer) {
      clearInterval(this.pruneTimer);
      this.pruneTimer = null;
    }
  }

  updateSettings(next = {}) {
    if (Number.isFinite(Number(next.logMaxSizeMb))) {
      this.maxSizeMb = Math.min(200, Math.max(5, Number(next.logMaxSizeMb)));
    }
    if (Number.isFinite(Number(next.logRetentionDays))) {
      this.retentionDays = Math.min(30, Math.max(1, Number(next.logRetentionDays)));
    }
    this.rotateIfNeeded();
    this.pruneOldLogs();
    return this.getStats();
  }

  rotateIfNeeded() {
    try {
      if (!fs.existsSync(this.activeFile)) return false;
      const stat = fs.statSync(this.activeFile);
      const maxBytes = this.maxSizeMb * 1024 * 1024;
      if (stat.size >= maxBytes) {
        const stamp = new Date().toISOString().replace(/[:.]/g, '-');
        const archivePath = path.join(this.logsDir, `ferizy-runtime-${stamp}.jsonl`);
        fs.renameSync(this.activeFile, archivePath);
        return true;
      }
    } catch (err) {
      console.warn('[LogRotation] Gagal rotasi log:', err.message);
    }
    return false;
  }

  write(category, data = {}) {
    this.ensureLogsDir();
    this.rotateIfNeeded();
    const record = {
      ts: new Date().toISOString(),
      category,
      ...data
    };
    try {
      fs.appendFileSync(this.activeFile, `${JSON.stringify(record)}\n`, 'utf8');
      this.totalEventsWritten += 1;
    } catch (err) {
      console.warn('[LogRotation] Gagal tulis log:', err.message);
    }
  }

  pruneOldLogs() {
    this.ensureLogsDir();
    const cutoffMs = Date.now() - this.retentionDays * 24 * 60 * 60 * 1000;
    const hardCapBytes = this.maxSizeMb * 5 * 1024 * 1024;
    let deletedCount = 0;
    try {
      const files = fs
        .readdirSync(this.logsDir)
        .filter((name) => name.startsWith('ferizy-runtime-') && name.endsWith('.jsonl'))
        .map((name) => {
          const full = path.join(this.logsDir, name);
          const st = fs.statSync(full);
          return { name, full, mtimeMs: st.mtimeMs, size: st.size };
        })
        .sort((a, b) => a.mtimeMs - b.mtimeMs);

      for (const f of files) {
        if (f.mtimeMs < cutoffMs) {
          fs.unlinkSync(f.full);
          deletedCount += 1;
        }
      }

      const remaining = files.filter((f) => fs.existsSync(f.full));
      let totalSize = remaining.reduce((sum, f) => sum + f.size, 0);
      for (const f of remaining) {
        if (totalSize <= hardCapBytes) break;
        fs.unlinkSync(f.full);
        totalSize -= f.size;
        deletedCount += 1;
      }
      this.lastPrunedAt = new Date().toLocaleTimeString('id-ID');
    } catch (err) {
      console.warn('[LogRotation] Gagal prune log lama:', err.message);
    }
    return { deletedCount, stats: this.getStats() };
  }

  getStats() {
    this.ensureLogsDir();
    let currentBytes = 0;
    let totalBytes = 0;
    let archivedCount = 0;
    try {
      for (const name of fs.readdirSync(this.logsDir)) {
        if (!name.endsWith('.jsonl') && !name.endsWith('.json')) continue;
        const st = fs.statSync(path.join(this.logsDir, name));
        totalBytes += st.size;
        if (name === 'ferizy-runtime.jsonl') {
          currentBytes = st.size;
        } else if (name.startsWith('ferizy-runtime-')) {
          archivedCount += 1;
        }
      }
    } catch (_) {}

    return {
      logsDir: this.logsDir,
      maxSizeMb: this.maxSizeMb,
      retentionDays: this.retentionDays,
      currentFileKb: Math.round(currentBytes / 1024),
      archivedCount,
      totalDiskMb: Number((totalBytes / (1024 * 1024)).toFixed(2)),
      totalEventsWritten: this.totalEventsWritten,
      lastPrunedAt: this.lastPrunedAt
    };
  }
}

module.exports = { LogRotationManager };
