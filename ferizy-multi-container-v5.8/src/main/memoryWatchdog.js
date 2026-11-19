const { app } = require('electron');

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

class MemoryWatchdog {
  constructor(runtimeConfig, sessionManager, automationEngine, onStateChange) {
    this.limitMb = Number(runtimeConfig.watchdogCalibrationTargetMb || runtimeConfig.memoryWatchdogLimitMb) || 350;
    this.delayMs = Number(runtimeConfig.staggeredReloadDelayMs) || 400;
    this.sessionManager = sessionManager;
    this.automationEngine = automationEngine;
    this.onStateChange = onStateChange;
    this.logRotation = null;
    this.tabMemoryMb = new Map();
    this.reloadQueue = [];
    this.isProcessingQueue = false;
    this.timer = null;
    this.recycleCount = 0;
  }

  attachLogRotation(logger) {
    this.logRotation = logger;
  }

  setLimitMb(nextLimitMb) {
    if (Number.isFinite(Number(nextLimitMb))) {
      this.limitMb = Math.min(550, Math.max(250, Number(nextLimitMb)));
    }
    return this.limitMb;
  }

  start() {
    if (this.timer) return;
    this.timer = setInterval(() => this.sampleMetrics(), 15000);
  }

  stop() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  sampleMetrics() {
    const metrics = app.getAppMetrics();
    const pidMemoryMap = new Map();
    for (const m of metrics) {
      const memKb = (m.memory && m.memory.workingSetSize) || 0;
      pidMemoryMap.set(m.pid, Math.round(memKb / 1024));
    }

    for (const tab of this.sessionManager.tabs.values()) {
      if (tab.view.webContents.isDestroyed()) continue;
      const osPid = tab.view.webContents.getOSProcessId();
      const mb = pidMemoryMap.get(osPid) || 0;
      this.tabMemoryMb.set(tab.tabId, mb);

      const runner = this.automationEngine.getRunnerState(tab.tabId);
      const isActiveVisible = this.sessionManager.activeTabId === tab.tabId;

      if (
        !isActiveVisible &&
        runner.state === 'IDLE' &&
        mb > this.limitMb &&
        !this.reloadQueue.includes(tab.tabId)
      ) {
        this.reloadQueue.push(tab.tabId);
      }
    }

    if (this.reloadQueue.length > 0) {
      this.processQueue();
    }
    this.onStateChange();
  }

  enqueueStandbyManualReload(options = {}) {
    const visibleSet = new Set(Array.isArray(this.sessionManager.splitTabIds) ? this.sessionManager.splitTabIds : []);
    if (this.sessionManager.activeTabId) {
      visibleSet.add(this.sessionManager.activeTabId);
    }

    let queuedCount = 0;
    for (const tab of this.sessionManager.tabs.values()) {
      const runner = this.automationEngine.getRunnerState(tab.tabId);
      const isVisible = visibleSet.has(tab.tabId);
      if (
        runner.state === 'IDLE' &&
        (options.includeVisible === true || !isVisible) &&
        !this.reloadQueue.includes(tab.tabId)
      ) {
        this.reloadQueue.push(tab.tabId);
        queuedCount += 1;
      }
    }
    if (queuedCount > 0) {
      this.processQueue();
    }
    return { queuedCount };
  }

  async processQueue() {
    if (this.isProcessingQueue) return;
    this.isProcessingQueue = true;

    try {
      while (this.reloadQueue.length > 0) {
        const tabId = this.reloadQueue.shift();
        const tab = this.sessionManager.tabs.get(tabId);
        const runner = this.automationEngine.getRunnerState(tabId);

        if (tab && !tab.view.webContents.isDestroyed() && runner.state === 'IDLE') {
          tab.view.webContents.reloadIgnoringCache();
          this.recycleCount += 1;
          if (this.logRotation) {
            this.logRotation.write('WATCHDOG_RECYCLE', {
              tabId,
              memoryMb: this.getTabMemoryMb(tabId),
              limitMb: this.limitMb,
              recycleCount: this.recycleCount
            });
          }
          await sleep(this.delayMs);
        }
      }
    } finally {
      this.isProcessingQueue = false;
      this.onStateChange();
    }
  }

  getTabMemoryMb(tabId) {
    return this.tabMemoryMb.get(tabId) || 0;
  }
}

module.exports = { MemoryWatchdog };
