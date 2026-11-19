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
      reasonText = `CPU PC ${pcCpuPercent}% & RAM PC ${pcRamPercent}% OVER`;
    } else if (pcRamPercent >= overThresholdPct) {
      severity = 'over';
      reasonText = `RAM PC OVER (${pcRamPercent}%, sisa ${pcRamFreeGb} GB)`;
    } else if (pcCpuPercent >= overThresholdPct) {
      severity = 'over';
      reasonText = `CPU PC OVER (${pcCpuPercent}%)`;
    } else if (pcCpuPercent >= warnThresholdPct || pcRamPercent >= warnThresholdPct) {
      severity = 'warn';
      reasonText = pcRamPercent >= warnThresholdPct
        ? `RAM PC Padat (${pcRamPercent}%)`
        : `CPU PC Padat (${pcCpuPercent}%)`;
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
    return `${String(h).padStart(2, '0')}j ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}d`;
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
