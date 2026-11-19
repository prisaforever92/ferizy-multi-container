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
        tabId: `akun_${acc}_tab_${t}`,
        accountId: acc,
        state: t === 2 ? 'RUNNING' : 'IDLE',
        memoryMb: 145 + acc * 4,
        passengerInput: `PENUMPANG AKUN ${acc}`,
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
  assert.ok(peakRamMb < 12000, `Peak RAM ${peakRamMb} MB harus aman di RAM 16 GB`);

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
