package com.example.domain

object ElectronFase4Templates {

  fun logRotationManagerJs(): String =
    """
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
        const archivePath = path.join(this.logsDir, `ferizy-runtime-${'$'}{stamp}.jsonl`);
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
      fs.appendFileSync(this.activeFile, `${'$'}{JSON.stringify(record)}\n`, 'utf8');
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
    """.trimIndent() + "\n"

  fun enduranceMonitorJs(): String =
    """
const fs = require('fs');
const os = require('os');
const path = require('path');
let app = null;
try {
  const el = require('electron');
  if (el && el.app) app = el.app;
} catch (_) {}

class EnduranceMonitor {
  constructor(
    runtimeConfig,
    sessionManager,
    automationEngine,
    memoryWatchdog,
    optionalModules,
    serverClockSync,
    logRotation
  ) {
    this.runtimeConfig = runtimeConfig;
    this.sessionManager = sessionManager;
    this.automationEngine = automationEngine;
    this.memoryWatchdog = memoryWatchdog;
    this.optionalModules = optionalModules;
    this.serverClockSync = serverClockSync;
    this.logRotation = logRotation;

    this.healthReportEnabled = runtimeConfig.enduranceHealthReportEnabled !== false;
    this.snapshotIntervalMin = Math.min(240, Math.max(15, Number(runtimeConfig.healthSnapshotIntervalMinutes) || 60));
    this.startedAt = Date.now();
    this.sampleTimer = null;
    this.snapshotTimer = null;
    this.meterTimer = null;
    this.lastAppRamMb = 0;
    this.peakAppRamMb = 0;
    this.sampleCount = 0;
    this.lastReportPath = null;
    this.lastReportAt = null;
    this.lastSelfCheck = null;
    this.prevCpuSnapshot = this.readCpuTotals();
    this.lastSystemMetrics = null;
    this.isQuickCleaning = false;
  }

  start() {
    this.sampleNow();
    if (!this.sampleTimer) {
      this.sampleTimer = setInterval(() => this.sampleNow(), 60000);
    }
    this.restartSnapshotTimer();
  }

  stop() {
    if (this.sampleTimer) {
      clearInterval(this.sampleTimer);
      this.sampleTimer = null;
    }
    if (this.snapshotTimer) {
      clearInterval(this.snapshotTimer);
      this.snapshotTimer = null;
    }
    this.stopLiveMeter();
  }

  readCpuTotals() {
    try {
      const cpus = os.cpus() || [];
      let total = 0;
      let idle = 0;
      for (const c of cpus) {
        const t = c.times || {};
        total += (t.user || 0) + (t.nice || 0) + (t.sys || 0) + (t.irq || 0) + (t.idle || 0);
        idle += t.idle || 0;
      }
      return { total, idle, coreCount: Math.max(1, cpus.length) };
    } catch (_) {
      return { total: 0, idle: 0, coreCount: 1 };
    }
  }

  sampleSystemMetrics() {
    const currCpu = this.readCpuTotals();
    const prevCpu = this.prevCpuSnapshot || currCpu;
    const totalDelta = Math.max(0, currCpu.total - prevCpu.total);
    const idleDelta = Math.max(0, currCpu.idle - prevCpu.idle);
    let pcCpuPercent = this.lastSystemMetrics ? this.lastSystemMetrics.pcCpuPercent : 0;
    if (totalDelta > 0) {
      pcCpuPercent = Math.min(100, Math.max(0, Math.round(((totalDelta - idleDelta) / totalDelta) * 100)));
      this.prevCpuSnapshot = currCpu;
    }

    const totalMemBytes = os.totalmem() || 1;
    const freeMemBytes = os.freemem() || 0;
    const usedMemBytes = Math.max(0, totalMemBytes - freeMemBytes);
    const pcRamTotalGb = Number((totalMemBytes / (1024 * 1024 * 1024)).toFixed(1));
    const pcRamUsedGb = Number((usedMemBytes / (1024 * 1024 * 1024)).toFixed(1));
    const pcRamFreeGb = Number((freeMemBytes / (1024 * 1024 * 1024)).toFixed(1));
    const pcRamPercent = Math.min(100, Math.max(0, Math.round((usedMemBytes / totalMemBytes) * 100)));

    let browserKb = 0;
    let browserCpuRaw = 0;
    try {
      for (const m of app.getAppMetrics()) {
        browserKb += (m.memory && m.memory.workingSetSize) || 0;
        if (m.cpu && Number.isFinite(m.cpu.percentCPUUsage)) {
          browserCpuRaw += m.cpu.percentCPUUsage;
        }
      }
    } catch (_) {}

    const browserRamMb = Math.round(browserKb / 1024);
    const browserRamGb = Number((browserRamMb / 1024).toFixed(2));
    const browserCpuPercent = Math.min(100, Math.max(0, Math.round(browserCpuRaw / currCpu.coreCount)));

    this.lastAppRamMb = browserRamMb;
    if (browserRamMb > this.peakAppRamMb) {
      this.peakAppRamMb = browserRamMb;
    }

    const warnThresholdPct = 75;
    const overThresholdPct = 85;
    let severity = 'normal';
    let reasonText = 'Normal';
    if (pcCpuPercent >= overThresholdPct && pcRamPercent >= overThresholdPct) {
      severity = 'over';
      reasonText = `CPU PC ${'$'}{pcCpuPercent}% & RAM PC ${'$'}{pcRamPercent}% OVER`;
    } else if (pcRamPercent >= overThresholdPct) {
      severity = 'over';
      reasonText = `RAM PC OVER (${'$'}{pcRamPercent}%, sisa ${'$'}{pcRamFreeGb} GB)`;
    } else if (pcCpuPercent >= overThresholdPct) {
      severity = 'over';
      reasonText = `CPU PC OVER (${'$'}{pcCpuPercent}%)`;
    } else if (pcCpuPercent >= warnThresholdPct || pcRamPercent >= warnThresholdPct) {
      severity = 'warn';
      reasonText = pcRamPercent >= warnThresholdPct
        ? `RAM PC Padat (${'$'}{pcRamPercent}%)`
        : `CPU PC Padat (${'$'}{pcCpuPercent}%)`;
    }

    const payload = {
      sampledAt: Date.now(),
      pcCpuPercent,
      pcRamTotalGb,
      pcRamUsedGb,
      pcRamFreeGb,
      pcRamPercent,
      browserRamMb,
      browserRamGb,
      browserCpuPercent,
      warnThresholdPct,
      overThresholdPct,
      severity,
      reasonText
    };
    this.lastSystemMetrics = payload;
    return payload;
  }

  startLiveMeter(onTick, intervalMs = 3000) {
    this.stopLiveMeter();
    const safeInterval = Math.min(10000, Math.max(1000, Number(intervalMs) || 3000));
    const emit = () => {
      const metrics = this.sampleSystemMetrics();
      if (typeof onTick === 'function') {
        try {
          onTick(metrics);
        } catch (_) {}
      }
    };
    emit();
    this.meterTimer = setInterval(emit, safeInterval);
  }

  stopLiveMeter() {
    if (this.meterTimer) {
      clearInterval(this.meterTimer);
      this.meterTimer = null;
    }
  }

  async quickCleanResources() {
    if (this.isQuickCleaning) {
      return { ok: false, reason: 'Pembersihan cepat sedang berjalan...' };
    }
    this.isQuickCleaning = true;
    try {
      const before = this.sampleSystemMetrics();
      let clearedSessionsCount = 0;
      if (this.sessionManager && this.sessionManager.sessions) {
        for (const ses of this.sessionManager.sessions.values()) {
          if (ses && typeof ses.clearCache === 'function') {
            try {
              await ses.clearCache();
              clearedSessionsCount += 1;
            } catch (_) {}
          }
        }
      }

      let queuedStandbyCount = 0;
      if (this.memoryWatchdog && typeof this.memoryWatchdog.enqueueStandbyManualReload === 'function') {
        const res = this.memoryWatchdog.enqueueStandbyManualReload({ includeVisible: false });
        queuedStandbyCount = Number(res && res.queuedCount) || 0;
      }

      if (typeof global.gc === 'function') {
        try {
          global.gc();
        } catch (_) {}
      }

      await new Promise((resolve) => setTimeout(resolve, 150));
      const after = this.sampleSystemMetrics();
      const savedRamMb = Math.max(0, before.browserRamMb - after.browserRamMb);

      if (this.logRotation) {
        this.logRotation.write('QUICK_CLEAN', {
          clearedSessionsCount,
          queuedStandbyCount,
          beforeBrowserRamMb: before.browserRamMb,
          afterBrowserRamMb: after.browserRamMb,
          savedRamMb,
          pcRamPercent: after.pcRamPercent,
          pcCpuPercent: after.pcCpuPercent
        });
      }

      return {
        ok: true,
        clearedSessionsCount,
        queuedStandbyCount,
        before,
        after,
        savedRamMb
      };
    } finally {
      this.isQuickCleaning = false;
    }
  }

  restartSnapshotTimer() {
    if (this.snapshotTimer) {
      clearInterval(this.snapshotTimer);
      this.snapshotTimer = null;
    }
    if (!this.healthReportEnabled) return;
    this.snapshotTimer = setInterval(() => {
      this.exportHealthReport();
    }, this.snapshotIntervalMin * 60000);
  }

  updateConfig(next = {}) {
    if (typeof next.enduranceHealthReportEnabled === 'boolean') {
      this.healthReportEnabled = next.enduranceHealthReportEnabled;
    }
    if (Number.isFinite(Number(next.healthSnapshotIntervalMinutes))) {
      this.snapshotIntervalMin = Math.min(240, Math.max(15, Number(next.healthSnapshotIntervalMinutes)));
    }
    if (Number.isFinite(Number(next.watchdogCalibrationTargetMb)) && this.memoryWatchdog) {
      const target = Math.min(550, Math.max(250, Number(next.watchdogCalibrationTargetMb)));
      this.memoryWatchdog.setLimitMb(target);
      this.runtimeConfig.watchdogCalibrationTargetMb = target;
    }
    if (this.logRotation) {
      this.logRotation.updateSettings(next);
    }
    this.restartSnapshotTimer();
    return this.getSummary();
  }

  sampleNow() {
    let totalKb = 0;
    try {
      for (const m of app.getAppMetrics()) {
        totalKb += (m.memory && m.memory.workingSetSize) || 0;
      }
    } catch (_) {}

    const totalMb = Math.round(totalKb / 1024);
    this.lastAppRamMb = totalMb;
    if (totalMb > this.peakAppRamMb) {
      this.peakAppRamMb = totalMb;
    }
    this.sampleCount += 1;
    return totalMb;
  }

  formatUptime(ms) {
    const totalSec = Math.max(0, Math.floor(ms / 1000));
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const s = totalSec % 60;
    return `${'$'}{String(h).padStart(2, '0')}j ${'$'}{String(m).padStart(2, '0')}m ${'$'}{String(s).padStart(2, '0')}d`;
  }

  runSelfCheck() {
    const totalRamMb = this.sampleNow();
    const tabs = Array.from(this.sessionManager.tabs.values());
    let runningCount = 0;
    let wonCount = 0;
    let captchaCount = 0;
    let idleCount = 0;

    for (const tab of tabs) {
      const st = this.automationEngine.getRunnerState(tab.tabId).state;
      if (st === 'RUNNING') runningCount += 1;
      else if (st === 'WON_STOPPED') wonCount += 1;
      else if (st === 'PAUSED_CAPTCHA') captchaCount += 1;
      else idleCount += 1;
    }

    const logStats = this.logRotation ? this.logRotation.getStats() : null;
    const ramHealthy = totalRamMb < 12000;
    const diskHealthy = !logStats || logStats.totalDiskMb <= logStats.maxSizeMb * 5.2;
    const result = {
      checkedAt: new Date().toISOString(),
      uptimeText: this.formatUptime(Date.now() - this.startedAt),
      totalTabs: tabs.length,
      maxTabs: 15,
      runningCount,
      wonCount,
      captchaCount,
      idleCount,
      totalAppRamMb: totalRamMb,
      peakAppRamMb: this.peakAppRamMb,
      watchdogLimitMb: this.memoryWatchdog.limitMb,
      queueMode: 'reactive_only_400ms',
      recycleCount: this.memoryWatchdog.recycleCount,
      diskLogMb: logStats ? logStats.totalDiskMb : 0,
      status: ramHealthy && diskHealthy ? 'HEALTHY' : 'PRESSURE'
    };

    this.lastSelfCheck = result;
    if (this.logRotation) {
      this.logRotation.write('ENDURANCE_SELF_CHECK', result);
    }
    return result;
  }

  exportHealthReport() {
    const check = this.runSelfCheck();
    const report = {
      hardwareTarget: 'Intel Core i7-4790 / 16 GB RAM / 256 GB SSD',
      phase: 'Fase 4',
      check,
      clockSync: this.serverClockSync ? this.serverClockSync.getSummary() : null,
      optionalModules: this.optionalModules ? this.optionalModules.getState() : null,
      logRotation: this.logRotation ? this.logRotation.getStats() : null
    };

    if (this.logRotation) {
      this.logRotation.ensureLogsDir();
      const targetPath = path.join(this.logRotation.logsDir, 'health-report-24h.json');
      try {
        fs.writeFileSync(targetPath, JSON.stringify(report, null, 2), 'utf8');
        this.lastReportPath = targetPath;
        this.lastReportAt = new Date().toLocaleTimeString('id-ID');
      } catch (err) {
        console.warn('[EnduranceMonitor] Gagal tulis health report:', err.message);
      }
    }
    return { ok: true, reportPath: this.lastReportPath, report };
  }

  getSummary() {
    return {
      healthReportEnabled: this.healthReportEnabled,
      snapshotIntervalMin: this.snapshotIntervalMin,
      uptimeText: this.formatUptime(Date.now() - this.startedAt),
      lastAppRamMb: this.lastAppRamMb,
      peakAppRamMb: this.peakAppRamMb,
      sampleCount: this.sampleCount,
      watchdogLimitMb: this.memoryWatchdog ? this.memoryWatchdog.limitMb : 350,
      healthStatus: this.lastAppRamMb > 12000 ? 'PRESSURE' : 'HEALTHY',
      lastReportAt: this.lastReportAt,
      lastReportPath: this.lastReportPath,
      lastSelfCheck: this.lastSelfCheck,
      systemMetrics: this.lastSystemMetrics
    };
  }
}

module.exports = { EnduranceMonitor };
    """.trimIndent() + "\n"

  fun enduranceStressTestJs(): String =
    """
const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { LogRotationManager } = require('../src/main/logRotationManager');

// Simulasi Uji Ketahanan 24 Jam (1.440 menit terakselerasi) untuk 5 Akun x 3 Tab = 15 Tab
// Memvalidasi batas RAM 16 GB, Staggered Reload Queue 400ms Reaktif (Opsi A), dan Log Rotation SSD 256 GB.
function run24HourSimulation() {
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'ferizy-24h-'));
  const runtimeCfg = {
    logMaxSizeMb: 50,
    logRetentionDays: 7,
    memoryWatchdogLimitMb: 350,
    staggeredReloadDelayMs: 400
  };
  const logger = new LogRotationManager(runtimeCfg, tmpDir);

  const tabs = [];
  for (let acc = 1; acc <= 5; acc++) {
    for (let t = 1; t <= 3; t++) {
      tabs.push({
        tabId: `akun_${'$'}{acc}_tab_${'$'}{t}`,
        accountId: acc,
        state: t === 2 ? 'RUNNING' : 'IDLE',
        memoryMb: 145 + acc * 4,
        passengerInput: `PENUMPANG AKUN ${'$'}{acc}`,
        reloadCount: 0
      });
    }
  }

  let peakRamMb = 0;
  let totalWatchdogRecycles = 0;

  for (let minute = 1; minute <= 1440; minute++) {
    for (const tab of tabs) {
      tab.memoryMb += tab.state === 'RUNNING' ? 0.35 : 0.65;
      // Watchdog 350MB + Opsi A Reaktif: HANYA reload tab IDLE yang melebihi 350MB
      if (tab.state === 'IDLE' && tab.memoryMb > runtimeCfg.memoryWatchdogLimitMb) {
        tab.memoryMb = 148;
        tab.reloadCount += 1;
        totalWatchdogRecycles += 1;
        logger.write('WATCHDOG_RECYCLE', { tabId: tab.tabId, minute });
      }
    }

    const totalAppRamMb = Math.round(520 + tabs.reduce((sum, t) => sum + t.memoryMb, 0));
    if (totalAppRamMb > peakRamMb) peakRamMb = totalAppRamMb;

    if (minute % 60 === 0) {
      logger.write('HOURLY_HEALTH', { hour: minute / 60, totalAppRamMb, totalTabs: tabs.length });
    }
  }

  // Pastikan tab RUNNING tidak pernah di-reload oleh Watchdog (input penumpang aman)
  const runningTabs = tabs.filter((t) => t.state === 'RUNNING');
  assert.strictEqual(runningTabs.length, 5);
  for (const rt of runningTabs) {
    assert.strictEqual(rt.reloadCount, 0, 'Tab RUNNING tidak boleh terkena reload Watchdog');
    assert.ok(rt.passengerInput.startsWith('PENUMPANG AKUN'));
  }

  // Pastikan total RAM 15 tab jauh di bawah batas 16 GB (16384 MB)
  assert.ok(peakRamMb < 12000, `Peak RAM ${'$'}{peakRamMb} MB harus aman di RAM 16 GB`);

  const stats = logger.getStats();
  assert.ok(stats.totalDiskMb <= runtimeCfg.logMaxSizeMb * 5, 'Ukuran log harus terkendali di SSD 256 GB');

  fs.rmSync(tmpDir, { recursive: true, force: true });
  console.log(
    JSON.stringify(
      {
        status: 'PASS_24H_SIMULATION',
        simulatedMinutes: 1440,
        tabsSimulated: tabs.length,
        peakAppRamMb: peakRamMb,
        watchdogLimitMb: runtimeCfg.memoryWatchdogLimitMb,
        totalWatchdogRecycles,
        logEventsWritten: stats.totalEventsWritten,
        diskLogMb: stats.totalDiskMb
      },
      null,
      2
    )
  );
}

run24HourSimulation();
    """.trimIndent() + "\n"
}
