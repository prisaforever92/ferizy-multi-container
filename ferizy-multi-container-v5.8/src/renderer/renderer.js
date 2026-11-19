let currentSnapshot = null;
let editingAccountId = null;
let renamingAccountId = null;
let renamingCollectionIndex = null;
let preflightGridVisible = true;
let extAccountId = 1;
let extTabIndex = 1;
let extEditingPointId = null;
let checklistSyncedOnce = false;
let compactModeInitialized = false;

function applyCompactMode(enabled) {
  document.body.classList.toggle('compact-mode', Boolean(enabled));
  const chk = document.getElementById('chk-compact-mode');
  if (chk) chk.checked = Boolean(enabled);
  try {
    localStorage.setItem('ferizy_compact_mode', enabled ? '1' : '0');
  } catch (_) {}
  requestAnimationFrame(syncHeaderHeight);
}

function playAlarmTone(alarm) {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const patterns = {
      lintasan: [523, 659],
      kuota: [659, 784, 988],
      menang: [523, 659, 784, 1046],
      logout: [330, 262, 220],
      proxy: [293, 293, 220],
      captcha: [880, 440, 880]
    };
    const notes = patterns[alarm] || [600];
    let t = ctx.currentTime;
    notes.forEach((freq) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = alarm === 'logout' || alarm === 'proxy' ? 'sawtooth' : 'triangle';
      osc.frequency.setValueAtTime(freq, t);
      gain.gain.setValueAtTime(0.14, t);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.14);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t);
      osc.stop(t + 0.15);
      t += 0.16;
    });
  } catch (_) {}
}

let lastSentHeaderHeight = 0;
let lastSentModalOpen = null;

function syncHeaderHeight() {
  const header = document.getElementById('shell-header');
  if (header && window.ferizyShell) {
    const h = Math.ceil(header.getBoundingClientRect().height);
    if (h >= 80 && Math.abs(h - lastSentHeaderHeight) >= 2) {
      lastSentHeaderHeight = h;
      window.ferizyShell.setHeaderHeight(h);
    }
  }
}

function syncModalOverlayState() {
  const anyOpen = Boolean(document.querySelector('.modal:not(.hidden)'));
  if (anyOpen !== lastSentModalOpen && window.ferizyShell && typeof window.ferizyShell.setModalOpen === 'function') {
    lastSentModalOpen = anyOpen;
    window.ferizyShell.setModalOpen(anyOpen);
  }
}

function openModalById(modalId) {
  document.querySelectorAll('.modal').forEach((m) => {
    if (m.id !== modalId) m.classList.add('hidden');
  });
  const el = document.getElementById(modalId);
  if (el) el.classList.remove('hidden');
  syncModalOverlayState();
}

function closeModalById(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.add('hidden');
  syncModalOverlayState();
}

function closeAllModals() {
  document.querySelectorAll('.modal').forEach((m) => m.classList.add('hidden'));
  syncModalOverlayState();
}

document.querySelectorAll('.modal').forEach((modalEl) => {
  modalEl.addEventListener('click', (e) => {
    if (e.target === modalEl) {
      closeModalById(modalEl.id);
    }
  });
});

window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && document.querySelector('.modal:not(.hidden)')) {
    closeAllModals();
  }
});

function renderSnapshot(snap) {
  currentSnapshot = snap;
  if (!snap) return;

  if (!compactModeInitialized) {
    compactModeInitialized = true;
    let storedCompact = null;
    try {
      storedCompact = localStorage.getItem('ferizy_compact_mode');
    } catch (_) {}
    const initialCompact = storedCompact !== null ? storedCompact === '1' : Boolean(snap.compactHeaderMode);
    applyCompactMode(initialCompact);
  }

  document.getElementById('total-tabs-badge').textContent =
    `${snap.accounts.length} Akun • ${snap.totalTabs}/${snap.maxTotalTabs} Tab`;
  document.getElementById('ntfy-badge').textContent = `ntfy: ${snap.ntfyTopic}`;
  document.getElementById('blocker-pill').textContent = `Asset Blocker: ${snap.assetBlockerMode}`;
  document.getElementById('watchdog-pill').textContent = `Watchdog: ${snap.memoryWatchdogLimitMb}MB • Queue ${snap.staggeredReloadDelayMs}ms (Recycle: ${snap.recycleCount || 0})`;

  const zoomPct = snap.globalZoomPercent || 100;
  document.getElementById('btn-zoom-reset').textContent = `${zoomPct}%`;
  const dispZoomLabel = document.getElementById('disp-zoom-label');
  const dispZoomSlider = document.getElementById('disp-zoom-slider');
  if (dispZoomLabel) dispZoomLabel.textContent = `${zoomPct}%`;
  if (dispZoomSlider) dispZoomSlider.value = String(zoomPct);

  const splitMode = snap.splitMode || 1;
  document.querySelectorAll('.disp-split-preset').forEach((btn) => {
    const val = Number(btn.getAttribute('data-split')) || 1;
    btn.className = val === splitMode ? 'btn-small btn-primary disp-split-preset' : 'btn-small disp-split-preset';
  });

  const addAccBtn = document.getElementById('btn-add-account');
  if (addAccBtn) {
    const maxAcc = snap.maxDynamicAccounts || 12;
    addAccBtn.disabled = snap.accounts.length >= maxAcc;
    addAccBtn.textContent = snap.accounts.length >= maxAcc ? `Max ${maxAcc}` : '+ Akun';
  }

  const activeAccId = snap.activeAccountId || 1;
  const activeAccObj = snap.accounts.find((a) => a.accountId === activeAccId);
  const activeAccLabel = (activeAccObj && activeAccObj.name) || `Akun ${activeAccId}`;
  const slotBar = document.getElementById('split-slot-bar');
  if (slotBar) {
    const activeCollectionGroup = Number(snap.tabCollectionSplitGroup) || (snap.tab1CollectionSplitActive ? 1 : 0);
    if (activeCollectionGroup >= 1 && activeCollectionGroup <= 3) {
      const groupListKey = activeCollectionGroup === 1
        ? 'tab1CollectionTabs'
        : activeCollectionGroup === 2
          ? 'tab2CollectionTabs'
          : 'tab3CollectionTabs';
      const groupItems = Array.isArray(snap[groupListKey]) ? snap[groupListKey] : [];
      slotBar.classList.remove('hidden');
      slotBar.innerHTML = `<span class="pill pill-ok">▥ Split Vertikal Kumpulan Tab ${activeCollectionGroup} (${groupItems.length} Akun Sejajar • Mirror 0ms • AutoKlik Putus Otomatis)</span>`;
      groupItems.forEach((tItem) => {
        const b = document.createElement('button');
        const isAutoKlikRunning = tItem.runnerState === 'RUNNING';
        b.className = `slot-btn ${tItem.isActiveMaster ? 'active-slot' : ''}`;
        b.textContent = `${tItem.isActiveMaster ? '★ Master ' : ''}A${tItem.accountId}•T${activeCollectionGroup}${isAutoKlikRunning ? ' (⚡Putus)' : ''}`;
        b.title = isAutoKlikRunning
          ? `${tItem.accountName} (Tab ${activeCollectionGroup}) sedang menjalankan AutoKlik -> Otomatis PUTUS dari Mirror`
          : `Klik untuk menjadikan ${tItem.accountName} (Tab ${activeCollectionGroup}) sebagai Master Mirror Tab ${activeCollectionGroup}`;
        b.onclick = () => window.ferizyShell.switchTab(tItem.tabId);
        slotBar.appendChild(b);
      });
      const exitSplitBtn = document.createElement('button');
      exitSplitBtn.className = 'btn-small';
      exitSplitBtn.textContent = '1 Layar Penuh';
      exitSplitBtn.title = `Tutup tampilan Split Vertikal Kumpulan Tab ${activeCollectionGroup} dan kembali ke 1 layar penuh`;
      exitSplitBtn.onclick = () => window.ferizyShell.setTabCollectionSplit(activeCollectionGroup, false);
      slotBar.appendChild(exitSplitBtn);

      const firstPane = Array.isArray(snap.paneBounds) && snap.paneBounds[0];
      const paneTxt = firstPane ? `${firstPane.width}x${firstPane.height}px/kolom` : 'Split Vertikal Rata';
      const info = document.createElement('span');
      info.className = 'active-meta';
      info.textContent = `${paneTxt} • Tab yang Start AutoKlik otomatis putus Mirror • Zoom ${zoomPct}%`;
      slotBar.appendChild(info);
    } else if (splitMode > 1) {
      slotBar.classList.remove('hidden');
      slotBar.innerHTML = `<span class="pill pill-info">Split ${activeAccLabel} (${splitMode} Layar Mandiri)</span>`;
      const slots = Array.isArray(snap.splitSlots) ? snap.splitSlots : [];
      for (let i = 0; i < splitMode; i += 1) {
        const slot = slots[i];
        const b = document.createElement('button');
        if (slot) {
          b.className = `slot-btn ${slot.slotIndex === snap.activeSlotIndex ? 'active-slot' : ''}`;
          b.textContent = `Slot ${i + 1}: Akun ${activeAccId} • T${slot.indexInAccount || (i + 1)}`;
          b.title = `Klik untuk memilih Slot ${i + 1} (Akun ${activeAccId})`;
          b.onclick = () => window.ferizyShell.setActiveSlot(i);
        } else {
          b.className = 'slot-btn';
          b.textContent = `+ Buka Tab ${i + 1} (Akun ${activeAccId})`;
          b.title = `Buka tab baru di dalam Akun ${activeAccId} untuk mengisi Slot ${i + 1}`;
          b.onclick = () => window.ferizyShell.createTab(activeAccId);
        }
        slotBar.appendChild(b);
      }
      const firstPane = Array.isArray(snap.paneBounds) && snap.paneBounds[0];
      const paneTxt = firstPane ? `${firstPane.width}x${firstPane.height}px/panel` : 'Bagi rata';
      const info = document.createElement('span');
      info.className = 'active-meta';
      info.textContent = `${paneTxt} • Hanya tab Akun ${activeAccId} • Zoom ${zoomPct}%`;
      slotBar.appendChild(info);
    } else {
      slotBar.classList.add('hidden');
      slotBar.innerHTML = '';
    }
  }

  if (snap.enduranceSummary || snap.logStats) {
    const es = snap.enduranceSummary || {};
    const ls = snap.logStats || {};
    const endPill = document.getElementById('endurance-pill');
    endPill.textContent =
      `RAM: ${es.lastAppRamMb || 0}MB (Peak ${es.peakAppRamMb || 0}MB) • Log: ${ls.totalDiskMb || 0}MB/${ls.maxSizeMb || 50}MB (${ls.retentionDays || 7}h) • ${es.uptimeText || '00j'}`;
    endPill.className = es.healthStatus === 'PRESSURE' ? 'pill pill-accent' : 'pill pill-ok';
  }

  if (snap.clockSync) {
    const sign = snap.clockSync.offsetMs >= 0 ? '+' : '';
    document.getElementById('clock-sync-pill').textContent =
      `Clock Sync: ${sign}${snap.clockSync.offsetMs}ms (RTT ${snap.clockSync.rttMs}ms)`;
  }

  if (snap.optionalModules && snap.optionalModules.scheduleTimer) {
    const st = snap.optionalModules.scheduleTimer;
    const timerPill = document.getElementById('shot-timer-pill');
    if (st.scheduledShotEnabled && typeof st.remainingMs === 'number') {
      const sec = Math.ceil(st.remainingMs / 1000);
      timerPill.textContent = `Tembak ${st.scheduledShotTimeWib} (-${sec}s)`;
      timerPill.className = 'pill pill-accent';
    } else {
      timerPill.textContent = 'Timer Tembak: OFF';
      timerPill.className = 'pill pill-info';
    }
  }

  const pausedCaptchaCount = snap.accounts.reduce(
    (sum, acc) => sum + acc.tabs.filter((t) => t.runnerState === 'PAUSED_CAPTCHA').length,
    0
  );
  const resumeCaptchaBtn = document.getElementById('btn-resume-captcha');
  resumeCaptchaBtn.textContent = `Lanjut CAPTCHA (${pausedCaptchaCount})`;
  resumeCaptchaBtn.className = pausedCaptchaCount > 0 ? 'btn-small btn-primary' : 'btn-small';

  if (snap.notificationSettings) {
    const muteBtn = document.getElementById('btn-mute-alarm');
    muteBtn.textContent = snap.notificationSettings.audioAlarmMuted ? 'Suara: MUTE' : 'Suara: ON';
    muteBtn.className = snap.notificationSettings.audioAlarmMuted ? 'btn-small btn-stop' : 'btn-small';

    const ev = snap.notificationSettings.events || {};
    const captchaOn = ev.CAPTCHA_TERDETEKSI === true;
    const capNotifBtn = document.getElementById('btn-toggle-captcha-notif');
    if (capNotifBtn) {
      capNotifBtn.textContent = captchaOn ? 'Notif CAPTCHA: ON' : 'Notif CAPTCHA: OFF';
      capNotifBtn.className = captchaOn ? 'btn-small btn-run' : 'btn-small';
    }

    const enabledCount = [
      ev.LINTASAN_DIBUKA !== false,
      ev.KUOTA_MUNCUL !== false,
      ev.TEMBUS_VERIFIKASI !== false,
      ev.LOGOUT_TERDETEKSI === true,
      ev.PROXY_PUTUS === true,
      captchaOn
    ].filter(Boolean).length;
    document.getElementById('btn-open-notif-modal').textContent = `Notif HP (${enabledCount}/6)`;
  }

  if (snap.optionalModules && !checklistSyncedOnce) {
    checklistSyncedOnce = true;
    const om = snap.optionalModules;
    document.getElementById('chk-keep-alive').checked = Boolean(om.gentleKeepAliveEnabled);
    document.getElementById('sel-keepalive-min').value = String(om.keepAliveIntervalMinutes || 12);
    document.getElementById('sel-autowake-mode').value = om.autoWakeMode || 'same_account';
    document.getElementById('chk-captcha-autopop').checked = Boolean(om.captchaAutoPopEnabled);
    document.getElementById('chk-smart-matcher').checked = Boolean(om.smartTargetMatcherEnabled);
    document.getElementById('sel-service-type').value = om.smartMatcherServiceType || 'all';
    document.getElementById('chk-matcher-fallback').checked = Boolean(om.smartMatcherFallbackNearest);
    document.getElementById('input-priority-hours').value = om.priorityHoursCsv || '08:00,10:00,12:00,14:00,20:00';
    document.getElementById('chk-latency-comp').checked = Boolean(om.latencyCompensationEnabled);
    document.getElementById('sel-fast-timeout').value = String(om.fastRequestTimeoutMs || 4500);
    document.getElementById('sel-advanced-stop').value = om.advancedStopMode || 'per_tab';
    if (om.scheduleTimer) {
      document.getElementById('chk-sched-shot').checked = Boolean(om.scheduleTimer.scheduledShotEnabled);
      document.getElementById('input-sched-time').value = om.scheduleTimer.scheduledShotTimeWib || '00:00:00';
    }
    if (snap.notificationSettings) {
      const ns = snap.notificationSettings;
      document.getElementById('input-ntfy-server').value = ns.ntfyServerUrl || 'https://ntfy.sh';
      document.getElementById('sel-notif-dedupe').value = String(ns.dedupeSeconds || 10);
      if (ns.events) {
        document.getElementById('chk-notif-lintasan').checked = ns.events.LINTASAN_DIBUKA !== false;
        document.getElementById('chk-notif-kuota').checked = ns.events.KUOTA_MUNCUL !== false;
        document.getElementById('chk-notif-tembus').checked = ns.events.TEMBUS_VERIFIKASI !== false;
        document.getElementById('chk-notif-logout').checked = ns.events.LOGOUT_TERDETEKSI === true;
        document.getElementById('chk-notif-proxy').checked = ns.events.PROXY_PUTUS === true;
        document.getElementById('chk-notif-captcha').checked = ns.events.CAPTCHA_TERDETEKSI === true;
      }
    }
    if (snap.logStats) {
      document.getElementById('sel-log-preset').value = snap.logStats.maxSizeMb <= 20 ? '20mb_3d' : '50mb_7d';
    }
    if (snap.enduranceSummary) {
      document.getElementById('sel-watchdog-cal').value = String(snap.enduranceSummary.watchdogLimitMb || 350);
      document.getElementById('chk-health-report').checked = snap.enduranceSummary.healthReportEnabled !== false;
    }
  }

  const grid = document.getElementById('preflight-grid');
  if (grid) {
    grid.style.display = preflightGridVisible ? 'grid' : 'none';
    grid.innerHTML = '';
  }

  let activeTabObj = null;
  for (const acc of snap.accounts) {
    const foundActive = acc.tabs.find((t) => t.tabId === snap.activeTabId);
    if (foundActive) {
      activeTabObj = foundActive;
      break;
    }
  }

  if (extAccountId && !document.getElementById('autoklik-ext-modal').classList.contains('hidden')) {
    refreshAutoklikExtModalFromSnapshot(snap, false);
  }

  // 3 Kotak Kontainer Khusus Kumpulan Tab 1, Kumpulan Tab 2, dan Kumpulan Tab 3 (Master Mirror + Split Vertikal + Auto-Disconnect saat AutoKlik Start)
  const activeIdxForMirror = Number(snap.activeTabIndex) || (activeTabObj && activeTabObj.indexInAccount) || 1;
  const activeCollectionSplitGroup = Number(snap.tabCollectionSplitGroup) || (snap.tab1CollectionSplitActive ? 1 : 0);
  const groupsMirrorMeta = (snap && snap.tabGroupsMirror) || {};

  [1, 2, 3].forEach((groupIdx) => {
    const listKey = groupIdx === 1 ? 'tab1CollectionTabs' : groupIdx === 2 ? 'tab2CollectionTabs' : 'tab3CollectionTabs';
    const groupMeta = groupsMirrorMeta[groupIdx] || groupsMirrorMeta[String(groupIdx)] || {};
    const collectionList = Array.isArray(snap[listKey]) && snap[listKey].length > 0
      ? snap[listKey]
      : (Array.isArray(groupMeta.tabs) && groupMeta.tabs.length > 0
          ? groupMeta.tabs
          : snap.accounts
              .map((acc) => {
                const tMatch = acc.tabs.find((t) => t.indexInAccount === groupIdx);
                if (!tMatch) return null;
                return {
                  tabId: tMatch.tabId,
                  accountId: acc.accountId,
                  accountName: acc.name || `Akun ${acc.accountId}`,
                  indexInAccount: groupIdx,
                  runnerState: tMatch.runnerState,
                  pointsCount: tMatch.pointsCount || 0,
                  isActiveMaster: tMatch.tabId === snap.activeTabId,
                  isMirrorDisconnectedByAutoKlik: tMatch.runnerState === 'RUNNING'
                };
              })
              .filter(Boolean));

    const isGroupMirrorEnabled = groupIdx === 1
      ? (groupMeta.enabled !== undefined ? Boolean(groupMeta.enabled) : Boolean(snap.tab1MirrorEnabled))
      : Boolean(groupMeta.enabled);
    const isGroupMirrorActiveNow = groupIdx === 1
      ? (groupMeta.active !== undefined ? Boolean(groupMeta.active) : Boolean(snap.tab1MirrorActive && activeIdxForMirror === 1))
      : Boolean(groupMeta.active);
    const isGroupSplitOn = activeCollectionSplitGroup === groupIdx;
    const autoKlikDisconnectedCount = collectionList.filter((item) => item && item.runnerState === 'RUNNING').length;

    const groupCard = document.createElement('div');
    groupCard.id = `tab${groupIdx}-collection-card`;
    groupCard.className = `acc-card tab1-collection-card ${isGroupSplitOn ? 'split-active' : ''} ${activeIdxForMirror === groupIdx ? 'active-acc' : ''}`;
    groupCard.title = `Kotak Kontainer Khusus Kumpulan Tab ${groupIdx}: Klik area kotak untuk membuka/menutup tampilan Split Vertikal semua Tab ${groupIdx}`;
    groupCard.addEventListener('click', (e) => {
      if (e.target && e.target.closest && e.target.closest('button, input, select, a, .tab-pill')) {
        return;
      }
      window.ferizyShell.toggleTabCollectionSplit(groupIdx);
    });

    const gHead = document.createElement('div');
    gHead.className = 'acc-head';

    const gNameWrap = document.createElement('div');
    gNameWrap.className = 'acc-name-wrap';

    const gDot = document.createElement('span');
    gDot.className = `acc-dot-login ${isGroupMirrorActiveNow ? 'ok' : ''}`;
    gDot.title = isGroupMirrorActiveNow
      ? `Mirror Kumpulan Tab ${groupIdx} Aktif (0ms Real-Time)`
      : `Mirror Kumpulan Tab ${groupIdx} Nonaktif / Mandiri`;
    gNameWrap.appendChild(gDot);

    const groupName = groupMeta.name || `📦 Kumpulan Tab ${groupIdx}`;
    if (renamingCollectionIndex === groupIdx) {
      const nameInp = document.createElement('input');
      nameInp.type = 'text';
      nameInp.maxLength = 32;
      nameInp.className = 'acc-name-input';
      nameInp.value = groupName;
      const commitCollectionRename = async () => {
        const nextVal = nameInp.value.trim();
        renamingCollectionIndex = null;
        await window.ferizyShell.renameCollection(groupIdx, nextVal);
      };
      nameInp.addEventListener('keydown', (ev) => {
        ev.stopPropagation();
        if (ev.key === 'Enter') {
          ev.preventDefault();
          commitCollectionRename();
        } else if (ev.key === 'Escape') {
          ev.preventDefault();
          renamingCollectionIndex = null;
          renderSnapshot(currentSnapshot);
        }
      });
      nameInp.addEventListener('blur', () => {
        if (renamingCollectionIndex === groupIdx) commitCollectionRename();
      });
      nameWrap.appendChild(nameInp);
      setTimeout(() => { try { nameInp.focus(); nameInp.select(); } catch (_) {} }, 10);
    } else {
      const gTitle = document.createElement('span');
      gTitle.className = 'acc-name-text';
      gTitle.style.maxWidth = '135px';
      gTitle.textContent = groupName;
      gTitle.title = `Klik dua kali untuk mengubah nama kontainer Kumpulan Tab ${groupIdx}`;
      gTitle.ondblclick = (e) => {
        e.stopPropagation();
        renamingCollectionIndex = groupIdx;
        renderSnapshot(currentSnapshot);
      };
      nameWrap.appendChild(gTitle);
    }

    const gCountBadge = document.createElement('span');
    gCountBadge.className = 'acc-tab-badge';
    gCountBadge.textContent = `(${collectionList.length}/${snap.accounts.length} Tab)`;
    gNameWrap.appendChild(gCountBadge);
    gHead.appendChild(gNameWrap);

    const gActions = document.createElement('div');
    gActions.className = 'acc-actions';

    const gReloadBtn = document.createElement('button');
    gReloadBtn.type = 'button';
    gReloadBtn.className = 'btn-small btn-primary';
    gReloadBtn.textContent = '🔄 Muat Ulang';
    gReloadBtn.title = `Muat ulang (reload) seluruh Tab ${groupIdx} lintas akun secara serempak`;
    gReloadBtn.onclick = async (e) => {
      e.stopPropagation();
      await window.ferizyShell.reloadCollection(groupIdx);
      document.getElementById('active-meta').textContent = `🔄 Seluruh Tab ${groupIdx} berhasil dimuat ulang (reload).`;
    };
    gActions.appendChild(gReloadBtn);

    const gStartAllBtn = document.createElement('button');
    gStartAllBtn.type = 'button';
    gStartAllBtn.className = 'btn-small btn-run';
    gStartAllBtn.textContent = '▶ Start All';
    gStartAllBtn.title = `Jalankan AutoKlik untuk semua Tab ${groupIdx} yang memiliki titik konfigurasi, dan otomatis putus Mirror untuk tab yang running.`;
    gStartAllBtn.onclick = async (e) => {
      e.stopPropagation();
      const res = await window.ferizyShell.startAllCollection(groupIdx);
      document.getElementById('active-meta').textContent = `▶ Start All Kumpulan Tab ${groupIdx}: Berjalan pada ${res.startedCount} tab.`;
    };
    gActions.appendChild(gStartAllBtn);

    const gSplitBtn = document.createElement('button');
    gSplitBtn.id = `btn-toggle-tab${groupIdx}-split`;
    gSplitBtn.type = 'button';
    gSplitBtn.className = isGroupSplitOn ? 'btn-small btn-run' : 'btn-small btn-primary';
    gSplitBtn.textContent = isGroupSplitOn ? '▥ Split Vertikal: ON' : '▥ Split Vertikal';
    gSplitBtn.title = isGroupSplitOn
      ? `Tampilan Split Vertikal semua Tab ${groupIdx} sedang AKTIF (Klik untuk kembali ke 1 layar penuh)`
      : `Tampilkan semua Tab ${groupIdx} lintas akun secara Split Vertikal berdampingan dalam 1 layar (Otomatis membuka Tab ${groupIdx} di semua akun jika belum terbuka)`;
    gSplitBtn.onclick = async (e) => {
      e.stopPropagation();
      const res = await window.ferizyShell.toggleTabCollectionSplit(groupIdx);
      if (res) {
        const isOn = Number(res.tabCollectionSplitGroup) === groupIdx;
        document.getElementById('active-meta').textContent = isOn
          ? `▥ Split Vertikal Kumpulan Tab ${groupIdx} AKTIF (Semua Tab ${groupIdx} tampil berdampingan real-time 0ms).`
          : `▥ Split Vertikal Kumpulan Tab ${groupIdx} ditutup (kembali ke tampilan normal).`;
      }
    };
    gActions.appendChild(gSplitBtn);
    gHead.appendChild(gActions);
    groupCard.appendChild(gHead);

    const gControlRow = document.createElement('div');
    gControlRow.className = 'acc-autoklik-row';

    const mirrorBtnEl = document.createElement('button');
    mirrorBtnEl.id = `btn-toggle-tab${groupIdx}-mirror`;
    mirrorBtnEl.type = 'button';
    if (isGroupMirrorActiveNow) {
      mirrorBtnEl.textContent = `🔗 Mirror Tab ${groupIdx}: ON`;
      mirrorBtnEl.className = 'btn-small btn-run';
      mirrorBtnEl.title = `Mirror Tab ${groupIdx} AKTIF (0ms): Klik, popup pilihan, swipe/wheel, scroll & ketik di Master Tab ${groupIdx} diikuti real-time oleh Tab ${groupIdx} lain yang TIDAK sedang Start AutoKlik.`;
    } else if (isGroupMirrorEnabled && activeIdxForMirror !== groupIdx) {
      mirrorBtnEl.textContent = `🔗 Mirror Tab ${groupIdx}: SIAGA`;
      mirrorBtnEl.className = 'btn-small btn-warn';
      mirrorBtnEl.title = `Mirror Tab ${groupIdx} dalam posisi ON (Siaga). Klik salah satu pil A1•T${groupIdx} di bawah atau aktifkan Split Vertikal Kumpulan Tab ${groupIdx} untuk menggerakkan Master Tab ${groupIdx}.`;
    } else {
      mirrorBtnEl.textContent = `🔗 Mirror Tab ${groupIdx}: OFF`;
      mirrorBtnEl.className = 'btn-small';
      mirrorBtnEl.title = `Mirror Tab ${groupIdx} sedang OFF (Mandiri). Klik untuk mengaktifkan sinkronisasi Kumpulan Tab ${groupIdx}.`;
    }
    mirrorBtnEl.onclick = async (e) => {
      e.stopPropagation();
      const res = await window.ferizyShell.toggleTabGroupMirror(groupIdx);
      if (res && !res.ok && res.reason) {
        document.getElementById('active-meta').textContent = res.reason;
      } else if (res) {
        document.getElementById('active-meta').textContent = res.enabled
          ? `🔗 Mirror Tab ${groupIdx}: ON (0ms) — Semua Tab ${groupIdx} yang tidak sedang Start AutoKlik bergerak serempak.`
          : `🔗 Mirror Tab ${groupIdx}: OFF — Semua Tab ${groupIdx} beroperasi 100% mandiri.`;
      }
    };
    gControlRow.appendChild(mirrorBtnEl);

    const gInfoBadge = document.createElement('span');
    gInfoBadge.className = `ind ${isGroupMirrorActiveNow ? 'ok' : (autoKlikDisconnectedCount > 0 ? 'warn' : '')}`;
    gInfoBadge.style.marginLeft = 'auto';
    if (autoKlikDisconnectedCount > 0 && isGroupMirrorEnabled) {
      gInfoBadge.textContent = `⚡ Putus (${autoKlikDisconnectedCount} AutoKlik)`;
      gInfoBadge.title = `${autoKlikDisconnectedCount} Tab ${groupIdx} sedang Start AutoKlik -> Otomatis PUTUS dari Mirror agar eksekusi AutoKlik tidak terganggu.`;
    } else {
      gInfoBadge.textContent = isGroupMirrorActiveNow ? '⚡ 0ms Sync' : 'Mandiri';
      gInfoBadge.title = `Saat salah satu/semua Tab ${groupIdx} Start AutoKlik, otomatis fitur Mirror putus untuk tab yang sedang Start AutoKlik.`;
    }
    gControlRow.appendChild(gInfoBadge);
    groupCard.appendChild(gControlRow);

    const gStrip = document.createElement('div');
    gStrip.className = 'tab-strip';
    if (collectionList.length === 0) {
      const openAllBtn = document.createElement('button');
      openAllBtn.type = 'button';
      openAllBtn.className = 'btn-small btn-primary';
      openAllBtn.textContent = `+ Buka Semua Tab ${groupIdx}`;
      openAllBtn.title = `Buka Tab ${groupIdx} di seluruh akun & tampilkan Split Vertikal`;
      openAllBtn.onclick = (e) => {
        e.stopPropagation();
        window.ferizyShell.setTabCollectionSplit(groupIdx, true);
      };
      gStrip.appendChild(openAllBtn);
    } else {
      collectionList.forEach((tItem) => {
        const pill = document.createElement('div');
        const isRunningAk = tItem.runnerState === 'RUNNING';
        const stateCls =
          tItem.runnerState === 'WON_STOPPED'
            ? 'won'
            : tItem.runnerState === 'PAUSED_CAPTCHA'
              ? 'captcha'
              : tItem.runnerState === 'LOGOUT_STOPPED'
                ? 'logout'
                : isRunningAk
                  ? 'running'
                  : '';
        pill.className = `tab-pill ${tItem.tabId === snap.activeTabId ? 'active' : ''} ${stateCls}`;
        pill.title = isRunningAk
          ? `${tItem.accountName} • Tab ${groupIdx} sedang START AUTOKLIK -> Otomatis PUTUS dari Mirror`
          : `${tItem.accountName} • Tab ${groupIdx} (${tItem.pointsCount || 0} titik) — Klik untuk jadikan Master Tab ${groupIdx}`;
        pill.onclick = (e) => {
          e.stopPropagation();
          window.ferizyShell.switchTab(tItem.tabId);
        };
        const ptsSuffix = tItem.pointsCount ? `(${tItem.pointsCount})` : '';
        const masterStar = tItem.tabId === snap.activeTabId && isGroupMirrorActiveNow && !isRunningAk ? '★' : '';
        const akPutusTag = isRunningAk ? '⚡Putus' : '';
        const lbl = document.createElement('span');
        lbl.textContent = `${masterStar}A${tItem.accountId}•T${groupIdx}${ptsSuffix}${akPutusTag}`;
        pill.appendChild(lbl);
        gStrip.appendChild(pill);
      });
    }
    groupCard.appendChild(gStrip);
    grid.appendChild(groupCard);
  });

  snap.accounts.forEach((acc) => {
    const accLabel = acc.name || `Akun ${acc.accountId}`;
    const isCurrentAccount = acc.accountId === activeAccId || Boolean(acc.isActiveAccount);
    const hasActiveTab = acc.tabs.some((t) => t.tabId === snap.activeTabId);
    const accActiveTabIdx = acc.activeTabIndex || 1;
    const accActiveTab = acc.tabs.find((t) => t.indexInAccount === accActiveTabIdx) || acc.tabs[0] || null;
    const activePtsCount = accActiveTab ? (accActiveTab.pointsCount ?? acc.pointsCount ?? 0) : (acc.pointsCount || 0);
    const extCfg = (accActiveTab && accActiveTab.extensionSettings) || acc.extensionSettings || { enabled: true, loopEnabled: true, markersHidden: false, pointsLocked: false };
    const isHidden = extCfg.markersHidden === true;
    const isLocked = extCfg.pointsLocked === true;

    const card = document.createElement('div');
    card.className = `acc-card ${isCurrentAccount || hasActiveTab ? 'active-acc' : ''}`;
    card.title = `Klik bagian atas/kosong kotak untuk pindah ke ${accLabel}`;
    card.addEventListener('click', (e) => {
      if (e.target && e.target.closest && e.target.closest('button, input, select, a, .tab-pill, .btn-add-tab')) {
        return;
      }
      window.ferizyShell.switchAccount(acc.accountId);
    });

    // Baris 1: Nama Akun (Bisa di-edit), Tab Count, Split Tunggal, Proxy/Geo, Hapus (>5)
    const head = document.createElement('div');
    head.className = 'acc-head';

    const nameWrap = document.createElement('div');
    nameWrap.className = 'acc-name-wrap';

    const loginDot = document.createElement('span');
    loginDot.className = `acc-dot-login ${acc.loginActive ? 'ok' : ''}`;
    loginDot.title = acc.loginActive ? 'Sesi Login Ferizy Aktif' : 'Belum Login / Standby';
    nameWrap.appendChild(loginDot);

    if (renamingAccountId === acc.accountId) {
      const nameInp = document.createElement('input');
      nameInp.type = 'text';
      nameInp.maxLength = 32;
      nameInp.className = 'acc-name-input';
      nameInp.value = accLabel;
      const commitRename = async () => {
        const nextVal = nameInp.value.trim();
        renamingAccountId = null;
        await window.ferizyShell.renameAccount(acc.accountId, nextVal);
      };
      nameInp.addEventListener('keydown', (ev) => {
        ev.stopPropagation();
        if (ev.key === 'Enter') {
          ev.preventDefault();
          commitRename();
        } else if (ev.key === 'Escape') {
          ev.preventDefault();
          renamingAccountId = null;
          renderSnapshot(currentSnapshot);
        }
      });
      nameInp.addEventListener('blur', () => {
        if (renamingAccountId === acc.accountId) commitRename();
      });
      nameWrap.appendChild(nameInp);
      setTimeout(() => { try { nameInp.focus(); nameInp.select(); } catch (_) {} }, 10);
    } else {
      const nameSpan = document.createElement('span');
      nameSpan.className = 'acc-name-text';
      nameSpan.textContent = accLabel;
      nameSpan.title = `${accLabel} (Klik ✎ untuk ubah nama)`;
      nameWrap.appendChild(nameSpan);

      const editNameBtn = document.createElement('button');
      editNameBtn.type = 'button';
      editNameBtn.className = 'btn-edit-name';
      editNameBtn.textContent = '✎';
      editNameBtn.title = `Ubah nama ${accLabel}`;
      editNameBtn.onclick = (e) => {
        e.stopPropagation();
        renamingAccountId = acc.accountId;
        renderSnapshot(currentSnapshot);
      };
      nameWrap.appendChild(editNameBtn);
    }

    const tabCountBadge = document.createElement('span');
    tabCountBadge.className = 'acc-tab-badge';
    tabCountBadge.textContent = `(${acc.tabCount}/${acc.maxTabs})`;
    nameWrap.appendChild(tabCountBadge);
    head.appendChild(nameWrap);

    const actions = document.createElement('div');
    actions.className = 'acc-actions';

    const accSplitMode = Number(acc.splitMode) || 1;
    const splitToggleBtn = document.createElement('button');
    splitToggleBtn.type = 'button';
    splitToggleBtn.disabled = acc.tabCount <= 1;
    splitToggleBtn.className = acc.tabCount > 1 && accSplitMode > 1 ? 'btn-small btn-primary' : 'btn-small';
    splitToggleBtn.textContent = acc.tabCount <= 1
      ? 'Split'
      : (accSplitMode > 1 ? `Split ${accSplitMode}` : 'Split OFF');
    splitToggleBtn.title = acc.tabCount <= 1
      ? 'Buka minimal 2 tab untuk menggunakan Split Layar'
      : (accSplitMode > 1
          ? `Split ${accSplitMode} Layar aktif (Klik untuk ubah ke 1 layar penuh)`
          : `Split OFF (Klik untuk bagi ${Math.min(3, acc.tabCount)} layar mandiri akun ini)`);
    splitToggleBtn.onclick = () => window.ferizyShell.toggleAccountSplit(acc.accountId);
    actions.appendChild(splitToggleBtn);

    const cfgBtn = document.createElement('button');
    cfgBtn.type = 'button';
    cfgBtn.className = 'btn-small';
    cfgBtn.textContent = acc.proxy && acc.proxy.protocol && acc.proxy.protocol !== 'direct' ? 'Proxy*' : 'Proxy/Geo';
    cfgBtn.title = `Pengaturan Nama, Proxy & Geo (${(acc.proxy && acc.proxy.status) || 'Direct'})`;
    cfgBtn.onclick = () => openAccountModal(acc);
    actions.appendChild(cfgBtn);

    if (acc.accountId > 5) {
      const delAccBtn = document.createElement('button');
      delAccBtn.type = 'button';
      delAccBtn.className = 'btn-small btn-stop';
      delAccBtn.textContent = '×';
      delAccBtn.title = `Hapus ${accLabel}`;
      delAccBtn.onclick = () => window.ferizyShell.removeAccount(acc.accountId);
      actions.appendChild(delAccBtn);
    }
    head.appendChild(actions);
    card.appendChild(head);

    // Baris 2: Kontrol AutoKlik Tab Aktif Akun Ini (Start/Stop saja, +Titik, -1 Titik, Hidden, Kunci)
    const akRow = document.createElement('div');
    akRow.className = 'acc-autoklik-row';

    const accRunningTab = acc.tabs.find((t) => t.runnerState === 'RUNNING');
    const runAccBtn = document.createElement('button');
    runAccBtn.type = 'button';
    if (accRunningTab) {
      runAccBtn.className = 'btn-small btn-stop';
      runAccBtn.textContent = '■ Stop';
      runAccBtn.title = `Hentikan AutoKlik ${accLabel}`;
      runAccBtn.onclick = () => window.ferizyShell.stopExtAccount(acc.accountId);
    } else {
      runAccBtn.className = 'btn-small btn-run';
      runAccBtn.textContent = '▶ Start';
      runAccBtn.title = `Jalankan AutoKlik Tab ${accActiveTabIdx} (${accLabel})`;
      runAccBtn.onclick = () => window.ferizyShell.startExtAccount(acc.accountId, accActiveTabIdx);
    }

    const pickPtBtn = document.createElement('button');
    pickPtBtn.type = 'button';
    pickPtBtn.className = 'btn-small btn-primary';
    pickPtBtn.textContent = '+Titik';
    pickPtBtn.title = `Ambil / tambah titik baru ke Tab ${accActiveTabIdx} (${accLabel})`;
    pickPtBtn.onclick = async () => {
      pendingPickerFromHeaderModal = false;
      const res = await window.ferizyShell.startExtPicker(acc.accountId, 'tap', null, accActiveTabIdx);
      if (res && res.ok) {
        document.getElementById('active-meta').textContent =
          `Mode Ambil Titik aktif untuk ${accLabel} • Tab ${res.tabIndex || accActiveTabIdx}: klik elemen di website (ESC batal).`;
      } else {
        document.getElementById('active-meta').textContent = (res && res.reason) || 'Gagal memulai picker.';
      }
    };

    const decPtBtn = document.createElement('button');
    decPtBtn.type = 'button';
    decPtBtn.className = 'btn-small';
    decPtBtn.textContent = '-1 Titik';
    decPtBtn.disabled = activePtsCount <= 0;
    decPtBtn.title = `Kurangi 1 titik terakhir pada Tab ${accActiveTabIdx} (${accLabel})`;
    decPtBtn.onclick = () => window.ferizyShell.removeLastExtPoint(acc.accountId, accActiveTabIdx);

    const hidePtsBtn = document.createElement('button');
    hidePtsBtn.type = 'button';
    hidePtsBtn.className = isHidden ? 'btn-small btn-warn' : 'btn-small';
    hidePtsBtn.textContent = isHidden ? '👁 Hidden' : '👁';
    hidePtsBtn.title = isHidden ? `Titik tersembunyi di Tab ${accActiveTabIdx} (klik untuk tampil)` : `Sembunyikan titik Tab ${accActiveTabIdx}`;
    hidePtsBtn.onclick = () => window.ferizyShell.toggleExtHidden(acc.accountId, !isHidden, accActiveTabIdx);

    const lockPtsBtn = document.createElement('button');
    lockPtsBtn.type = 'button';
    lockPtsBtn.className = isLocked ? 'btn-small btn-warn' : 'btn-small';
    lockPtsBtn.textContent = isLocked ? '🔒 Kunci' : '🔓';
    lockPtsBtn.title = isLocked ? `Titik terkunci di Tab ${accActiveTabIdx} (klik untuk bebas geser)` : `Kunci posisi titik Tab ${accActiveTabIdx}`;
    lockPtsBtn.onclick = () => window.ferizyShell.toggleExtLock(acc.accountId, !isLocked, accActiveTabIdx);

    const saveCfgBtn = document.createElement('button');
    saveCfgBtn.type = 'button';
    saveCfgBtn.className = 'btn-small btn-acc-save-cfg';
    saveCfgBtn.textContent = '💾 Save';
    saveCfgBtn.title = `Simpan konfigurasi AutoKlik Tab ${accActiveTabIdx} (${accLabel}) ke file JSON`;
    saveCfgBtn.onclick = async () => {
      const res = await window.ferizyShell.savePointConfigDialog(acc.accountId, accActiveTabIdx);
      if (res && res.ok) {
        document.getElementById('active-meta').textContent = res.canceled
          ? `Konfigurasi Tab ${accActiveTabIdx} (${accLabel}) tersimpan otomatis di kontainer (${res.storeFile}).`
          : `Konfigurasi Tab ${accActiveTabIdx} (${accLabel}) berhasil di-Save ke: ${res.filePath}`;
      }
    };

    const loadCfgBtn = document.createElement('button');
    loadCfgBtn.type = 'button';
    loadCfgBtn.className = 'btn-small btn-acc-load-cfg';
    loadCfgBtn.textContent = '📂 Load';
    loadCfgBtn.title = `Buka menu Load File & Copy-Paste Konfigurasi Antar Tab untuk ${accLabel} (Tab ${accActiveTabIdx})`;
    loadCfgBtn.onclick = () => openLoadCopyModal(acc.accountId, accActiveTabIdx);

    const ptsBadge = document.createElement('span');
    ptsBadge.className = `ind ${activePtsCount > 0 ? 'ok' : 'warn'}`;
    ptsBadge.style.marginLeft = 'auto';
    ptsBadge.textContent = `🎯 ${activePtsCount}`;
    ptsBadge.title = `Tab ${accActiveTabIdx} memiliki ${activePtsCount} titik`;

    akRow.appendChild(runAccBtn);
    akRow.appendChild(pickPtBtn);
    akRow.appendChild(decPtBtn);
    akRow.appendChild(hidePtsBtn);
    akRow.appendChild(lockPtsBtn);
    akRow.appendChild(saveCfgBtn);
    akRow.appendChild(loadCfgBtn);
    akRow.appendChild(ptsBadge);
    card.appendChild(akRow);

    // Baris 3: Tab Pills Mandiri + +Tab
    const strip = document.createElement('div');
    strip.className = 'tab-strip';

    acc.tabs.forEach((tab) => {
      const pill = document.createElement('div');
      const stateCls =
        tab.runnerState === 'WON_STOPPED'
          ? 'won'
          : tab.runnerState === 'PAUSED_CAPTCHA'
            ? 'captcha'
            : tab.runnerState === 'LOGOUT_STOPPED'
              ? 'logout'
              : tab.runnerState === 'RUNNING'
                ? 'running'
                : '';
      pill.className = `tab-pill ${tab.tabId === snap.activeTabId ? 'active' : ''} ${stateCls}`;
      pill.title = `Tab ${tab.indexInAccount}: ${tab.pointsCount || 0} titik • ${tab.url || ''}`;
      pill.onclick = () => window.ferizyShell.switchTab(tab.tabId);

      const stateIcon =
        tab.runnerState === 'WON_STOPPED'
          ? '★'
          : tab.runnerState === 'PAUSED_CAPTCHA'
            ? '⚠'
            : tab.runnerState === 'LOGOUT_STOPPED'
              ? '✖'
              : tab.runnerState === 'RUNNING'
                ? '▶'
                : '';
      const ptsSuffix = tab.pointsCount ? `(${tab.pointsCount})` : '';
      const label = document.createElement('span');
      label.textContent = `T${tab.indexInAccount}${ptsSuffix}${stateIcon}${tab.isLoading ? '…' : ''}`;
      pill.appendChild(label);

      if (acc.tabs.length > 1) {
        const close = document.createElement('span');
        close.className = 'tab-close';
        close.textContent = '×';
        close.onclick = (e) => {
          e.stopPropagation();
          window.ferizyShell.closeTab(tab.tabId);
        };
        pill.appendChild(close);
      }
      strip.appendChild(pill);
    });

    const addBtn = document.createElement('button');
    addBtn.type = 'button';
    addBtn.className = 'btn-add-tab';
    addBtn.disabled = acc.tabCount >= acc.maxTabs;
    addBtn.textContent = acc.tabCount >= acc.maxTabs ? 'Max 3/3' : '+ Tab';
    addBtn.onclick = () => window.ferizyShell.createTab(acc.accountId);
    strip.appendChild(addBtn);

    card.appendChild(strip);
    grid.appendChild(card);
  });

  const runnerBtn = document.getElementById('btn-toggle-active-runner');
  if (activeTabObj) {
    const urlInput = document.getElementById('active-url-input');
    if (document.activeElement !== urlInput) {
      urlInput.value = activeTabObj.url || '';
    }
    const urlPresetSel = document.getElementById('url-preset-select');
    if (urlPresetSel && document.activeElement !== urlPresetSel) {
      const cleanActiveUrl = String(activeTabObj.url || '').trim();
      if (cleanActiveUrl === 'https://trip.ferizy.com' || cleanActiveUrl === 'https://trip.ferizy.com/') {
        urlPresetSel.value = 'https://trip.ferizy.com/';
      } else if (/^https:\/\/trip\.ferizy\.com\/order(\/|\?|$)/i.test(cleanActiveUrl)) {
        urlPresetSel.value = 'https://trip.ferizy.com/order';
      } else {
        urlPresetSel.value = '';
      }
    }
    const backBtn = document.getElementById('btn-back-active');
    if (backBtn) {
      const cleanRoot = String(activeTabObj.url || '').trim().replace(/\/+$/, '');
      const canFallbackHome = Boolean(cleanRoot && cleanRoot !== 'https://trip.ferizy.com');
      backBtn.title = activeTabObj.canGoBack
        ? 'Kembali ke halaman website sebelumnya (Alt + Panah Kiri)'
        : canFallbackHome
          ? 'Kembali ke halaman utama https://trip.ferizy.com/ (Alt + Panah Kiri)'
          : 'Sudah berada di halaman utama https://trip.ferizy.com/';
    }
    const errSuffix = activeTabObj.lastError ? ` • ERROR: ${activeTabObj.lastError}` : '';
    const loadSuffix = activeTabObj.isLoading ? ' • Memuat...' : '';
    const ownerAcc = snap.accounts.find((a) => a.accountId === activeTabObj.accountId);
    const ownerName = (ownerAcc && ownerAcc.name) || `Akun ${activeTabObj.accountId}`;
    document.getElementById('active-meta').textContent =
      `${ownerName} • Tab ${activeTabObj.indexInAccount} • ${activeTabObj.runnerState} (${activeTabObj.memoryMb || 0} MB)${loadSuffix}${errSuffix}`;

    if (activeTabObj.runnerState === 'RUNNING') {
      runnerBtn.textContent = '■ Stop Tab';
      runnerBtn.className = 'btn-small btn-stop';
    } else {
      runnerBtn.textContent = '▶ Start Tab';
      runnerBtn.className = 'btn-small btn-run';
    }
  }

  requestAnimationFrame(syncHeaderHeight);
}

function openAccountModal(acc) {
  editingAccountId = acc.accountId;
  const accLabel = acc.name || `Akun ${acc.accountId}`;
  document.getElementById('modal-title').textContent = `Pengaturan ${accLabel} (${acc.partition})`;
  document.getElementById('cfg-acc-name').value = accLabel;
  document.getElementById('cfg-proxy-protocol').value = (acc.proxy && acc.proxy.protocol) || 'direct';
  document.getElementById('cfg-proxy-host').value = (acc.proxy && acc.proxy.host) || '';
  document.getElementById('cfg-proxy-port').value = (acc.proxy && acc.proxy.port) || 8080;
  document.getElementById('cfg-proxy-user').value = (acc.proxy && acc.proxy.username) || '';
  document.getElementById('cfg-proxy-pass').value = (acc.proxy && acc.proxy.password) || '';
  document.getElementById('cfg-geo-mode').value = (acc.geo && acc.geo.mode) || 'preset_0ms';
  document.getElementById('cfg-geo-preset').value = (acc.geo && acc.geo.preset) || 'merak';
  openModalById('config-modal');
}

document.getElementById('btn-close-modal').onclick = () => {
  closeModalById('config-modal');
};

document.getElementById('btn-save-account-cfg').onclick = async () => {
  if (!editingAccountId) return;
  const nextName = document.getElementById('cfg-acc-name').value.trim();
  await window.ferizyShell.renameAccount(editingAccountId, nextName);
  await window.ferizyShell.updateProxy(editingAccountId, {
    protocol: document.getElementById('cfg-proxy-protocol').value,
    host: document.getElementById('cfg-proxy-host').value,
    port: Number(document.getElementById('cfg-proxy-port').value) || 8080,
    username: document.getElementById('cfg-proxy-user').value,
    password: document.getElementById('cfg-proxy-pass').value
  });
  await window.ferizyShell.updateGeo(editingAccountId, {
    mode: document.getElementById('cfg-geo-mode').value,
    preset: document.getElementById('cfg-geo-preset').value
  });
  closeModalById('config-modal');
};

document.getElementById('btn-toggle-active-runner').onclick = async () => {
  if (!currentSnapshot || !currentSnapshot.activeTabId) return;
  let activeTab = null;
  for (const acc of currentSnapshot.accounts) {
    const found = acc.tabs.find((t) => t.tabId === currentSnapshot.activeTabId);
    if (found) activeTab = found;
  }
  if (!activeTab) return;
  if (activeTab.runnerState === 'RUNNING') {
    await window.ferizyShell.stopTabAutomation(activeTab.tabId);
  } else {
    await window.ferizyShell.startTabAutomation(activeTab.tabId);
  }
};

document.getElementById('btn-toggle-checklist').onclick = () => {
  document.getElementById('optional-checklist-panel').classList.toggle('hidden');
  requestAnimationFrame(syncHeaderHeight);
};

document.getElementById('btn-apply-checklist').onclick = async () => {
  await window.ferizyShell.updateOptionalFlags({
    gentleKeepAliveEnabled: document.getElementById('chk-keep-alive').checked,
    keepAliveIntervalMinutes: Number(document.getElementById('sel-keepalive-min').value) || 12,
    autoWakeMode: document.getElementById('sel-autowake-mode').value,
    captchaAutoPopEnabled: document.getElementById('chk-captcha-autopop').checked,
    smartTargetMatcherEnabled: document.getElementById('chk-smart-matcher').checked,
    smartMatcherServiceType: document.getElementById('sel-service-type').value,
    smartMatcherFallbackNearest: document.getElementById('chk-matcher-fallback').checked,
    latencyCompensationEnabled: document.getElementById('chk-latency-comp').checked,
    fastRequestTimeoutMs: Number(document.getElementById('sel-fast-timeout').value) || 4500,
    advancedStopMode: document.getElementById('sel-advanced-stop').value,
    priorityHoursCsv: document.getElementById('input-priority-hours').value
  });
  await window.ferizyShell.updateSchedule({
    scheduledShotEnabled: document.getElementById('chk-sched-shot').checked,
    scheduledShotTimeWib: document.getElementById('input-sched-time').value
  });
  await window.ferizyShell.updateNotifSettings({
    ntfyServerUrl: document.getElementById('input-ntfy-server').value,
    dedupeSeconds: Number(document.getElementById('sel-notif-dedupe').value) || 10,
    events: {
      LINTASAN_DIBUKA: document.getElementById('chk-notif-lintasan').checked,
      KUOTA_MUNCUL: document.getElementById('chk-notif-kuota').checked,
      TEMBUS_VERIFIKASI: document.getElementById('chk-notif-tembus').checked,
      LOGOUT_TERDETEKSI: document.getElementById('chk-notif-logout').checked,
      PROXY_PUTUS: document.getElementById('chk-notif-proxy').checked,
      CAPTCHA_TERDETEKSI: document.getElementById('chk-notif-captcha').checked
    }
  });
  const preset = document.getElementById('sel-log-preset').value;
  await window.ferizyShell.updateFase4Config({
    logMaxSizeMb: preset === '20mb_3d' ? 20 : 50,
    logRetentionDays: preset === '20mb_3d' ? 3 : 7,
    watchdogCalibrationTargetMb: Number(document.getElementById('sel-watchdog-cal').value) || 350,
    enduranceHealthReportEnabled: document.getElementById('chk-health-report').checked
  });
  document.getElementById('optional-checklist-panel').classList.add('hidden');
  requestAnimationFrame(syncHeaderHeight);
};

document.getElementById('btn-mute-alarm').onclick = async () => {
  const muted = Boolean(currentSnapshot && currentSnapshot.notificationSettings && currentSnapshot.notificationSettings.audioAlarmMuted);
  await window.ferizyShell.updateNotifSettings({ audioAlarmMuted: !muted });
};

document.getElementById('btn-resume-captcha').onclick = () => {
  window.ferizyShell.resumeCaptchaAll();
};

document.getElementById('btn-run-endurance').onclick = async () => {
  const res = await window.ferizyShell.runEnduranceSelfCheck();
  if (res) {
    document.getElementById('active-meta').textContent =
      `Uji 24J: ${res.status} • RAM ${res.totalAppRamMb}MB • Log ${res.diskLogMb}MB`;
  }
};

document.getElementById('btn-export-health').onclick = async () => {
  const res = await window.ferizyShell.exportHealthReport();
  if (res && res.reportPath) {
    document.getElementById('active-meta').textContent = `Health Report disimpan: ${res.reportPath}`;
  }
};

document.getElementById('btn-prune-logs').onclick = () => {
  window.ferizyShell.pruneLogsNow();
};

document.getElementById('btn-open-logs').onclick = () => {
  window.ferizyShell.openLogsFolder();
};

document.getElementById('btn-open-notif-modal').onclick = () => {
  const ns = (currentSnapshot && currentSnapshot.notificationSettings) || {};
  const ev = ns.events || {};
  document.getElementById('m-notif-lintasan').checked = ev.LINTASAN_DIBUKA !== false;
  document.getElementById('m-notif-kuota').checked = ev.KUOTA_MUNCUL !== false;
  document.getElementById('m-notif-tembus').checked = ev.TEMBUS_VERIFIKASI !== false;
  document.getElementById('m-notif-logout').checked = ev.LOGOUT_TERDETEKSI === true;
  document.getElementById('m-notif-proxy').checked = ev.PROXY_PUTUS === true;
  document.getElementById('m-notif-captcha').checked = ev.CAPTCHA_TERDETEKSI === true;
  document.getElementById('m-ntfy-topic').value = ns.ntfyTopic || (currentSnapshot && currentSnapshot.ntfyTopic) || 'ferizy_Transsamudra05';
  document.getElementById('m-ntfy-server').value = ns.ntfyServerUrl || 'https://ntfy.sh';
  document.getElementById('m-notif-dedupe').value = String(ns.dedupeSeconds || 10);
  openModalById('notif-modal');
};

document.getElementById('btn-close-notif-modal').onclick = () => {
  closeModalById('notif-modal');
};

document.getElementById('btn-save-notif-modal').onclick = async () => {
  const events = {
    LINTASAN_DIBUKA: document.getElementById('m-notif-lintasan').checked,
    KUOTA_MUNCUL: document.getElementById('m-notif-kuota').checked,
    TEMBUS_VERIFIKASI: document.getElementById('m-notif-tembus').checked,
    LOGOUT_TERDETEKSI: document.getElementById('m-notif-logout').checked,
    PROXY_PUTUS: document.getElementById('m-notif-proxy').checked,
    CAPTCHA_TERDETEKSI: document.getElementById('m-notif-captcha').checked
  };
  const topic = document.getElementById('m-ntfy-topic').value.trim();
  const serverUrl = document.getElementById('m-ntfy-server').value.trim();
  const dedupeSeconds = Number(document.getElementById('m-notif-dedupe').value) || 10;

  document.getElementById('chk-notif-lintasan').checked = events.LINTASAN_DIBUKA;
  document.getElementById('chk-notif-kuota').checked = events.KUOTA_MUNCUL;
  document.getElementById('chk-notif-tembus').checked = events.TEMBUS_VERIFIKASI;
  document.getElementById('chk-notif-logout').checked = events.LOGOUT_TERDETEKSI;
  document.getElementById('chk-notif-proxy').checked = events.PROXY_PUTUS;
  document.getElementById('chk-notif-captcha').checked = events.CAPTCHA_TERDETEKSI;
  document.getElementById('input-ntfy-server').value = serverUrl;
  document.getElementById('sel-notif-dedupe').value = String(dedupeSeconds);

  await window.ferizyShell.updateNotifSettings({
    ntfyTopic: topic,
    ntfyServerUrl: serverUrl,
    dedupeSeconds,
    events
  });
  closeModalById('notif-modal');
};

document.getElementById('btn-test-ntfy').onclick = () => {
  window.ferizyShell.testNtfy();
};

document.getElementById('btn-reload-standby').onclick = () => {
  window.ferizyShell.reloadStandbyQueue();
};

document.getElementById('btn-back-active').onclick = async () => {
  if (!currentSnapshot || !currentSnapshot.activeTabId) {
    document.getElementById('active-meta').textContent = 'Tab aktif belum siap, tunggu sebentar...';
    return;
  }
  const res = await window.ferizyShell.goBackTab(currentSnapshot.activeTabId);
  if (res && res.ok) {
    document.getElementById('active-meta').textContent =
      res.mode === 'fallback_home'
        ? 'Kembali ke beranda https://trip.ferizy.com/...'
        : 'Kembali ke halaman website sebelumnya...';
  } else if (res && res.reason) {
    document.getElementById('active-meta').textContent = res.reason;
  }
};

document.getElementById('btn-reload-active').onclick = () => {
  if (currentSnapshot && currentSnapshot.activeTabId) {
    window.ferizyShell.reloadTab(currentSnapshot.activeTabId);
  }
};

// Initial fallback bindings untuk Kumpulan Tab 1, Tab 2, dan Tab 3 (sebelum snapshot pertama tiba)
// IDs: tab1-collection-card, tab2-collection-card, tab3-collection-card, btn-toggle-tab1-mirror, btn-toggle-tab1-split
[1, 2, 3].forEach((gIdx) => {
  const mBtn = document.getElementById(`btn-toggle-tab${gIdx}-mirror`);
  if (mBtn) {
    mBtn.onclick = () => window.ferizyShell.toggleTabGroupMirror(gIdx);
  }
  const sBtn = document.getElementById(`btn-toggle-tab${gIdx}-split`);
  if (sBtn) {
    sBtn.onclick = () => window.ferizyShell.toggleTabCollectionSplit(gIdx);
  }
});

const togglePreflightBtn = document.getElementById('btn-toggle-preflight-grid');
if (togglePreflightBtn) {
  togglePreflightBtn.onclick = () => {
    preflightGridVisible = !preflightGridVisible;
    const gridEl = document.getElementById('preflight-grid');
    if (gridEl) {
      gridEl.style.display = preflightGridVisible ? 'grid' : 'none';
    }
    togglePreflightBtn.textContent = preflightGridVisible ? '📦 Kotak Kontainer' : '📦 Kotak Kontainer (OFF)';
    togglePreflightBtn.className = preflightGridVisible ? 'btn-small btn-primary' : 'btn-small';
    requestAnimationFrame(syncHeaderHeight);
  };
}

async function navigateFromBar(overrideUrl = null) {
  const urlInput = document.getElementById('active-url-input');
  if (overrideUrl) {
    urlInput.value = overrideUrl;
  }
  const url = urlInput.value.trim();
  if (!url) return;
  if (!currentSnapshot || !currentSnapshot.activeTabId) {
    document.getElementById('active-meta').textContent = 'Tab aktif belum siap, tunggu sebentar...';
    return;
  }
  urlInput.blur();
  document.getElementById('active-meta').textContent = `Memuat ${url}...`;
  await window.ferizyShell.navigateTab(currentSnapshot.activeTabId, url);
}

const urlPresetSelectEl = document.getElementById('url-preset-select');
if (urlPresetSelectEl) {
  urlPresetSelectEl.onchange = () => {
    const selectedUrl = urlPresetSelectEl.value;
    if (selectedUrl) {
      navigateFromBar(selectedUrl);
    }
  };
}

document.getElementById('btn-go-url').onclick = () => {
  navigateFromBar();
};

document.getElementById('active-url-input').addEventListener('change', (e) => {
  const val = (e.target.value || '').trim();
  if (val === 'https://trip.ferizy.com/' || val === 'https://trip.ferizy.com/order') {
    navigateFromBar(val);
  }
});

document.getElementById('active-url-input').addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    navigateFromBar();
  } else if (e.key === 'Escape' && currentSnapshot && currentSnapshot.activeTabId) {
    e.target.blur();
    renderSnapshot(currentSnapshot);
  }
});

document.getElementById('btn-refresh-preflight').onclick = () => {
  window.ferizyShell.refreshPreflight();
};

document.getElementById('btn-check-all-ip').onclick = async () => {
  const ids = (currentSnapshot && Array.isArray(currentSnapshot.accountIds))
    ? currentSnapshot.accountIds
    : [1, 2, 3, 4, 5];
  for (const id of ids) {
    await window.ferizyShell.checkProxyIp(id);
  }
};

document.getElementById('btn-add-account').onclick = () => {
  window.ferizyShell.addAccount();
};

document.getElementById('btn-zoom-out').onclick = () => {
  const cur = (currentSnapshot && currentSnapshot.globalZoomPercent) || 100;
  window.ferizyShell.setGlobalZoom(Math.max(50, cur - 10));
};
document.getElementById('btn-zoom-reset').onclick = () => {
  window.ferizyShell.setGlobalZoom(100);
};
document.getElementById('btn-zoom-in').onclick = () => {
  const cur = (currentSnapshot && currentSnapshot.globalZoomPercent) || 100;
  window.ferizyShell.setGlobalZoom(Math.min(150, cur + 10));
};

document.getElementById('btn-toggle-captcha-notif').onclick = async () => {
  const ev = (currentSnapshot && currentSnapshot.notificationSettings && currentSnapshot.notificationSettings.events) || {};
  const nextVal = !(ev.CAPTCHA_TERDETEKSI === true);
  const chkMain = document.getElementById('chk-notif-captcha');
  const chkModal = document.getElementById('m-notif-captcha');
  if (chkMain) chkMain.checked = nextVal;
  if (chkModal) chkModal.checked = nextVal;
  await window.ferizyShell.updateNotifSettings({
    events: { CAPTCHA_TERDETEKSI: nextVal }
  });
};

document.getElementById('btn-open-display-modal').onclick = () => {
  const zoomPct = (currentSnapshot && currentSnapshot.globalZoomPercent) || 100;
  document.getElementById('disp-zoom-label').textContent = `${zoomPct}%`;
  document.getElementById('disp-zoom-slider').value = String(zoomPct);
  document.getElementById('chk-compact-mode').checked = document.getElementById('shell-header').classList.contains('compact-mode');
  openModalById('display-modal');
};
document.getElementById('btn-close-display-modal').onclick = () => {
  closeModalById('display-modal');
};
document.getElementById('btn-done-display-modal').onclick = () => {
  closeModalById('display-modal');
};

document.getElementById('chk-compact-mode').onchange = (e) => {
  applyCompactMode(e.target.checked);
};

document.getElementById('disp-zoom-slider').oninput = (e) => {
  const val = Number(e.target.value) || 100;
  document.getElementById('disp-zoom-label').textContent = `${val}%`;
  window.ferizyShell.setGlobalZoom(val);
};

document.querySelectorAll('.disp-zoom-preset').forEach((btn) => {
  btn.onclick = () => window.ferizyShell.setGlobalZoom(Number(btn.getAttribute('data-zoom')) || 100);
});

document.querySelectorAll('.disp-split-preset').forEach((btn) => {
  btn.onclick = () => window.ferizyShell.setSplitMode(Number(btn.getAttribute('data-split')) || 1);
});

// AutoKlik-v3.2-Mandiri-Extension UI Controller (Mandiri Per-Tab: accountId + tabIndex)
function setAkStatus(text) {
  const el = document.getElementById('ak-status-msg');
  if (el) el.textContent = text;
}

function resetAkPointForm() {
  extEditingPointId = null;
  document.getElementById('ak-form-title').textContent = 'Tambah Titik Baru';
  document.getElementById('btn-ak-save-point').textContent = '+ Tambah Titik';
  document.getElementById('btn-ak-cancel-edit').classList.add('hidden');
  document.getElementById('ak-pt-type').value = 'tap';
  document.getElementById('ak-pt-label').value = '';
  document.getElementById('ak-pt-x').value = '540';
  document.getElementById('ak-pt-y').value = '420';
  document.getElementById('ak-pt-delay').value = '500';
  document.getElementById('ak-pt-selector').value = '';
  document.getElementById('ak-pt-value').value = '';
  document.getElementById('ak-pt-endxy').value = '';
}

function fillAkPointForm(pt) {
  extEditingPointId = pt.id;
  document.getElementById('ak-form-title').textContent = `Edit Titik #${pt.order}: ${pt.label}`;
  document.getElementById('btn-ak-save-point').textContent = 'Simpan Perubahan Titik';
  document.getElementById('btn-ak-cancel-edit').classList.remove('hidden');
  document.getElementById('ak-pt-type').value = pt.type || 'tap';
  document.getElementById('ak-pt-label').value = pt.label || '';
  document.getElementById('ak-pt-x').value = String(pt.x ?? 540);
  document.getElementById('ak-pt-y').value = String(pt.y ?? 420);
  document.getElementById('ak-pt-delay').value = String(pt.delayAfterMs ?? 500);
  document.getElementById('ak-pt-selector').value = pt.selector || '';
  document.getElementById('ak-pt-value').value = pt.type === 'popup_key' ? (pt.popupKey || 'Enter') : (pt.inputValue || pt.text || '');
  document.getElementById('ak-pt-endxy').value = pt.type === 'swipe' ? `${pt.endX || pt.x},${pt.endY || 200}` : '';
}

function getTabPointsAndSettings(acc, tabIdx) {
  if (!acc) return { points: [], extensionSettings: {}, isOpen: false };
  const tabObj = Array.isArray(acc.tabs) ? acc.tabs.find((t) => t.indexInAccount === tabIdx) : null;
  if (tabObj) {
    return {
      points: Array.isArray(tabObj.points) ? tabObj.points : [],
      extensionSettings: tabObj.extensionSettings || acc.extensionSettings || {},
      isOpen: true
    };
  }
  return {
    points: tabIdx === 1 && Array.isArray(acc.points) ? acc.points : [],
    extensionSettings: acc.extensionSettings || {},
    isOpen: false
  };
}

function refreshAutoklikExtModalFromSnapshot(snap, syncSettingsInputs = true) {
  if (!snap || !Array.isArray(snap.accounts)) return;
  const sel = document.getElementById('ak-account-select');
  if (sel) {
    sel.innerHTML = '';
    snap.accounts.forEach((a) => {
      const opt = document.createElement('option');
      opt.value = String(a.accountId);
      const lbl = a.name || `Akun ${a.accountId}`;
      opt.textContent = `${lbl} (${a.totalAccountPoints ?? a.pointsCount ?? 0} titik)`;
      if (a.accountId === extAccountId) opt.selected = true;
      sel.appendChild(opt);
    });
  }

  const acc = snap.accounts.find((a) => a.accountId === extAccountId) || snap.accounts[0];
  if (!acc) return;
  extAccountId = acc.accountId;
  extTabIndex = Math.min(3, Math.max(1, Number(extTabIndex) || 1));

  const tabSel = document.getElementById('ak-tab-select');
  if (tabSel) {
    tabSel.innerHTML = '';
    for (let t = 1; t <= 3; t++) {
      const tabObj = Array.isArray(acc.tabs) ? acc.tabs.find((item) => item.indexInAccount === t) : null;
      const cnt = tabObj ? (tabObj.pointsCount || 0) : 0;
      const opt = document.createElement('option');
      opt.value = String(t);
      opt.textContent = tabObj ? `Tab ${t} (${cnt} titik)` : `Tab ${t} (Standby)`;
      if (t === extTabIndex) opt.selected = true;
      tabSel.appendChild(opt);
    }
  }

  const tabData = getTabPointsAndSettings(acc, extTabIndex);
  const accLabel = acc.name || `Akun ${acc.accountId}`;
  document.getElementById('ak-container-badge').textContent =
    `${accLabel} (${acc.partition}) • Tab ${extTabIndex} • File: ${acc.storeFile || `akun_${acc.accountId}.json`}`;
  const scopeLbl = document.getElementById('ak-tab-scope-lbl');
  if (scopeLbl) scopeLbl.textContent = `Tab ${extTabIndex}`;

  if (syncSettingsInputs) {
    const s = tabData.extensionSettings || {};
    document.getElementById('ak-set-enabled').checked = s.enabled !== false;
    document.getElementById('ak-set-loop').checked = s.loopEnabled !== false;
    document.getElementById('ak-set-smartmatch').checked = s.smartMatch !== false;
    document.getElementById('ak-set-antirevert').checked = s.antiRevert !== false;
    document.getElementById('ak-set-hidden').checked = Boolean(s.markersHidden);
    document.getElementById('ak-set-locked').checked = Boolean(s.pointsLocked);
    document.getElementById('ak-set-maxloops').value = String(s.maxLoops ?? 0);
    document.getElementById('ak-set-loopdelay').value = String(s.loopDelayMs ?? 100);
    document.getElementById('ak-set-radius').value = String(s.radiusPx ?? 28);
  }

  const pts = tabData.points;
  document.getElementById('ak-points-count').textContent = String(pts.length);
  const listEl = document.getElementById('ak-points-list');
  listEl.innerHTML = '';

  if (!pts.length) {
    listEl.innerHTML = `<div class="active-meta">Belum ada titik di Tab ${extTabIndex} (${accLabel}). Klik "Ambil Titik" atau "+ Tambah Titik".</div>`;
    return;
  }

  pts.forEach((pt, idx) => {
    const row = document.createElement('div');
    row.className = 'ak-point-row';

    const info = document.createElement('div');
    info.className = 'ak-point-info';

    const chk = document.createElement('input');
    chk.type = 'checkbox';
    chk.checked = pt.enabled !== false;
    chk.title = 'Aktif / Nonaktifkan titik ini';
    chk.onchange = () => window.ferizyShell.updateExtPoint(acc.accountId, pt.id, { enabled: chk.checked }, extTabIndex);
    info.appendChild(chk);

    const extra = pt.type === 'input'
      ? ` = "${pt.inputValue || ''}"${pt.popupPinEnabled ? ` [PopupKey:${pt.popupDelayMs || 300}ms]` : ''}`
      : pt.type === 'popup_key'
        ? ` [${pt.popupKey || 'Enter'}]`
        : '';
    const txt = document.createElement('span');
    txt.textContent = `#${idx + 1} [${(pt.type || 'tap').toUpperCase()}] ${pt.label} (${pt.x},${pt.y}) • ${pt.delayAfterMs || 500}ms${extra}`;
    info.appendChild(txt);

    const acts = document.createElement('div');
    acts.className = 'ak-point-actions';

    const upBtn = document.createElement('button');
    upBtn.className = 'btn-small';
    upBtn.textContent = '▲';
    upBtn.disabled = idx === 0;
    upBtn.onclick = () => window.ferizyShell.moveExtPoint(acc.accountId, pt.id, 'up', extTabIndex);

    const downBtn = document.createElement('button');
    downBtn.className = 'btn-small';
    downBtn.textContent = '▼';
    downBtn.disabled = idx === pts.length - 1;
    downBtn.onclick = () => window.ferizyShell.moveExtPoint(acc.accountId, pt.id, 'down', extTabIndex);

    const editBtn = document.createElement('button');
    editBtn.className = 'btn-small';
    editBtn.textContent = 'Edit';
    editBtn.onclick = () => openSinglePointEditor(acc.accountId, pt.id, extTabIndex);

    const delBtn = document.createElement('button');
    delBtn.className = 'btn-small btn-stop';
    delBtn.textContent = '×';
    delBtn.onclick = () => window.ferizyShell.removeExtPoint(acc.accountId, pt.id, extTabIndex);

    acts.appendChild(upBtn);
    acts.appendChild(downBtn);
    acts.appendChild(editBtn);
    acts.appendChild(delBtn);

    row.appendChild(info);
    row.appendChild(acts);
    listEl.appendChild(row);
  });
}

function openAutoklikExtModal(accountId = null, tabIndex = null) {
  if (accountId) {
    extAccountId = Number(accountId) || 1;
    const acc = currentSnapshot && currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === extAccountId);
    extTabIndex = Number(tabIndex) || (acc && acc.activeTabIndex) || 1;
  } else if (currentSnapshot) {
    extAccountId = currentSnapshot.activeAccountId || 1;
    const acc = currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === extAccountId);
    extTabIndex = Number(tabIndex) || currentSnapshot.activeTabIndex || (acc && acc.activeTabIndex) || 1;
  }
  resetAkPointForm();
  document.getElementById('ak-json-io').value = '';
  refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
  openModalById('autoklik-ext-modal');
}

document.getElementById('btn-open-autoklik-ext').onclick = () => openAutoklikExtModal(null, null);
document.getElementById('btn-close-autoklik-ext').onclick = () => {
  closeModalById('autoklik-ext-modal');
};

document.getElementById('ak-account-select').onchange = (e) => {
  extAccountId = Number(e.target.value) || 1;
  const acc = currentSnapshot && currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === extAccountId);
  extTabIndex = (acc && acc.activeTabIndex) || 1;
  resetAkPointForm();
  refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
  setAkStatus(`Menampilkan konfigurasi Akun ${extAccountId} • Tab ${extTabIndex}.`);
};

document.getElementById('ak-tab-select').onchange = (e) => {
  extTabIndex = Math.min(3, Math.max(1, Number(e.target.value) || 1));
  resetAkPointForm();
  refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
  setAkStatus(`Menampilkan konfigurasi Akun ${extAccountId} • Tab ${extTabIndex}.`);
};

document.getElementById('btn-ak-clear-tab-points').onclick = async () => {
  await window.ferizyShell.clearExtPoints(extAccountId, extTabIndex);
  setAkStatus(`Semua titik Tab ${extTabIndex} Akun ${extAccountId} dikosongkan.`);
};

document.getElementById('btn-ak-save-settings').onclick = async () => {
  const nextSettings = {
    enabled: document.getElementById('ak-set-enabled').checked,
    loopEnabled: document.getElementById('ak-set-loop').checked,
    smartMatch: document.getElementById('ak-set-smartmatch').checked,
    antiRevert: document.getElementById('ak-set-antirevert').checked,
    markersHidden: document.getElementById('ak-set-hidden').checked,
    pointsLocked: document.getElementById('ak-set-locked').checked,
    maxLoops: Math.max(0, Number(document.getElementById('ak-set-maxloops').value) || 0),
    loopDelayMs: Number(document.getElementById('ak-set-loopdelay').value) || 100,
    radiusPx: Number(document.getElementById('ak-set-radius').value) || 28
  };
  await window.ferizyShell.saveExtSettings(extAccountId, nextSettings, extTabIndex);
  setAkStatus(`Pengaturan Tab ${extTabIndex} Akun ${extAccountId} tersimpan.`);
};

document.getElementById('btn-ak-cancel-edit').onclick = resetAkPointForm;

document.getElementById('btn-ak-save-point').onclick = async () => {
  const type = document.getElementById('ak-pt-type').value;
  const label = document.getElementById('ak-pt-label').value.trim();
  const x = Number(document.getElementById('ak-pt-x').value) || 540;
  const y = Number(document.getElementById('ak-pt-y').value) || 420;
  const delayAfterMs = Number(document.getElementById('ak-pt-delay').value) || 500;
  const selector = document.getElementById('ak-pt-selector').value.trim();
  const rawVal = document.getElementById('ak-pt-value').value;
  const radiusPx = Number(document.getElementById('ak-set-radius').value) || 28;

  const payload = {
    type,
    label: label || undefined,
    x,
    y,
    delayAfterMs,
    selector,
    radiusPx,
    iconSizePct: 25,
    inputValue: type === 'input' ? rawVal : '',
    cycleTexts: type === 'input' ? [rawVal] : undefined,
    typingSpeedMs: type === 'input' ? 30 : undefined,
    popupDelayMs: type === 'input' ? 300 : undefined,
    lineLengthPx: (type === 'swipe_up' || type === 'swipe_down') ? 350 : undefined,
    swipeForceScale: (type === 'swipe_up' || type === 'swipe_down') ? 2 : undefined,
    endX: x,
    endY: type === 'swipe_down' ? (y + 350) : Math.max(20, y - 350)
  };

  if (extEditingPointId) {
    await window.ferizyShell.updateExtPoint(extAccountId, extEditingPointId, payload, extTabIndex);
    setAkStatus(`Titik diperbarui pada Tab ${extTabIndex} Akun ${extAccountId}.`);
    resetAkPointForm();
  } else {
    await window.ferizyShell.addExtPoint(extAccountId, payload, extTabIndex);
    setAkStatus(`Titik baru ditambahkan ke Tab ${extTabIndex} Akun ${extAccountId}.`);
    resetAkPointForm();
  }
};

document.getElementById('btn-ak-start-acc').onclick = async () => {
  const res = await window.ferizyShell.startExtAccount(extAccountId, extTabIndex);
  setAkStatus(res && res.ok ? `AutoKlik Tab ${extTabIndex} Akun ${extAccountId} berjalan.` : ((res && res.reason) || 'Gagal memulai AutoKlik.'));
};

document.getElementById('btn-ak-stop-acc').onclick = async () => {
  await window.ferizyShell.stopExtAccount(extAccountId);
  setAkStatus(`AutoKlik Akun ${extAccountId} dihentikan.`);
};

document.getElementById('btn-ak-save-dialog').onclick = async () => {
  const res = await window.ferizyShell.savePointConfigDialog(extAccountId, extTabIndex);
  if (res && res.ok) {
    setAkStatus(res.canceled
      ? `Profil Tab ${extTabIndex} Akun ${extAccountId} tersimpan di ${res.storeFile}.`
      : `Konfigurasi Tab ${extTabIndex} Akun ${extAccountId} disimpan ke ${res.filePath}`);
  }
};

document.getElementById('btn-ak-open-dialog').onclick = async () => {
  const res = await window.ferizyShell.openPointConfigDialog(extAccountId, extTabIndex);
  if (res && res.ok) {
    refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
    setAkStatus(`Berhasil load ${res.count} titik ke Tab ${extTabIndex} Akun ${extAccountId} (${res.storeFile}).`);
  } else if (res && !res.canceled && res.reason) {
    setAkStatus(`Gagal load file: ${res.reason}`);
  }
};

document.getElementById('btn-ak-copy-all').onclick = async () => {
  const res = await window.ferizyShell.pastePointsToAll(extAccountId, extTabIndex);
  setAkStatus(res && res.ok ? `Profil titik Tab ${extTabIndex} Akun ${extAccountId} disalin ke semua akun.` : 'Gagal menyalin.');
};

let pendingPickerFromHeaderModal = false;
let singleEditAccountId = null;
let singleEditTabIndex = 1;
let singleEditPointId = null;
let singleEditType = 'tap';
let singleEditOrder = 1;
let singleEditCycleTexts = [''];
let pendingRepickTarget = null;

function mapLegacyPointType(pt) {
  const rawType = (pt && pt.type) || 'tap';
  if (rawType === 'swipe') {
    const endY = Number(pt.endY);
    const y = Number(pt.y) || 0;
    return Number.isFinite(endY) && endY > y ? 'swipe_down' : 'swipe_up';
  }
  if (rawType === 'swipe_up' || rawType === 'swipe_down' || rawType === 'input') return rawType;
  return 'tap';
}

function getTypeSymbolForTitle(type) {
  if (type === 'swipe_up') return '▲';
  if (type === 'swipe_down') return '▼';
  if (type === 'input') return '📋';
  return '🎯';
}

function syncExtensionTypeButtons() {
  document.querySelectorAll('.ep-type-btn').forEach((btn) => {
    const btnType = btn.getAttribute('data-type');
    btn.classList.toggle('active', btnType === singleEditType);
  });
  document.getElementById('ep-modal-title').textContent =
    `Titik #${singleEditOrder} ( ${getTypeSymbolForTitle(singleEditType)} )`;
}

function updatePopupTriggerPreview() {
  const el = document.getElementById('ep-popup-trigger-preview');
  if (!el) return;
  const enabled = document.getElementById('ep-popup-pin-enabled').checked;
  const popupDelayEl = document.getElementById('ep-popup-delay');
  const popupDelay = Math.min(10000, Math.max(20, Number(popupDelayEl ? popupDelayEl.value : 300) || 300));
  const firstSample = (singleEditCycleTexts || []).find((s) => String(s || '').trim().length > 0) || '';
  const triggerChar = firstSample.trim().charAt(0) || '';
  if (!enabled) {
    el.textContent = 'Alur Tanpa Popup Key: Klik Kolom → Backspace Berulang Sesuai Panjang Teks → Ketik Murni Keyboard (30ms/huruf).';
  } else if (triggerChar) {
    el.textContent = `Pemicu Popup Key Otomatis: Klik Kolom + ketik "${triggerChar}" → Tunggu ${popupDelay}ms → Klik Pin Ungu → Backspace Berulang → Ketik Murni Keyboard "${firstSample.trim()}".`;
  } else {
    el.textContent = `Isi minimal 1 teks di daftar bawah: huruf pertama akan otomatis diketik sebagai pemicu → tunggu ${popupDelay}ms → klik Pin Ungu → Backspace berulang → ketik lengkap (30ms/huruf).`;
  }
}

function updatePopupDelayPresetsDisplay() {
  const popupDelayEl = document.getElementById('ep-popup-delay');
  const dly = Math.min(10000, Math.max(20, Number(popupDelayEl ? popupDelayEl.value : 300) || 300));
  document.querySelectorAll('.ep-popup-delay-preset').forEach((btn) => {
    btn.classList.toggle('active', Number(btn.getAttribute('data-popup-delay')) === dly);
  });
}

function renderCycleTextRows() {
  const listEl = document.getElementById('ep-cycle-texts-list');
  listEl.innerHTML = '';
  const items = Array.isArray(singleEditCycleTexts) && singleEditCycleTexts.length > 0 ? singleEditCycleTexts : [''];
  singleEditCycleTexts = items;

  items.forEach((val, idx) => {
    const row = document.createElement('div');
    row.className = 'ep-cycle-row';

    const idxLbl = document.createElement('span');
    idxLbl.className = 'ep-cycle-idx';
    idxLbl.textContent = `#${idx + 1}`;

    const inp = document.createElement('input');
    inp.type = 'text';
    inp.placeholder = `Teks / Angka Siklus #${idx + 1}...`;
    inp.value = val || '';
    inp.oninput = () => {
      singleEditCycleTexts[idx] = inp.value;
      updatePopupTriggerPreview();
    };

    row.appendChild(idxLbl);
    row.appendChild(inp);

    if (items.length > 1) {
      const remBtn = document.createElement('button');
      remBtn.type = 'button';
      remBtn.className = 'btn-small btn-stop';
      remBtn.textContent = '×';
      remBtn.onclick = () => {
        singleEditCycleTexts.splice(idx, 1);
        renderCycleTextRows();
        updatePopupTriggerPreview();
      };
      row.appendChild(remBtn);
    }
    listEl.appendChild(row);
  });
  updatePopupTriggerPreview();
}

function updateSwipeSummaryDisplay() {
  const len = Math.max(40, Number(document.getElementById('ep-swipe-len').value) || 350);
  const scale = Math.max(1, Number(document.getElementById('ep-swipe-scale').value) || 2);
  const total = Math.round(len * scale);
  document.getElementById('ep-swipe-summary').textContent =
    `Total Jarak Geser Layar: ${total} px (Smooth 60FPS)`;
  document.querySelectorAll('.ep-scale-preset').forEach((btn) => {
    btn.classList.toggle('active', Number(btn.getAttribute('data-scale')) === scale);
  });
}

function updateTypingSpeedPresetsDisplay() {
  const spd = Math.max(5, Number(document.getElementById('ep-typing-speed').value) || 30);
  document.querySelectorAll('.ep-speed-preset').forEach((btn) => {
    btn.classList.toggle('active', Number(btn.getAttribute('data-speed')) === spd);
  });
}

function updateIconSizeDisplay() {
  const pct = Math.min(100, Math.max(15, Number(document.getElementById('ep-icon-size').value) || 25));
  const dp = Math.round(20 + pct * 0.6);
  document.getElementById('ep-icon-size-label').textContent = `📏 Ikon: ${pct}% (${dp}dp)`;
}

function updateSinglePointEditorFieldsVisibility() {
  syncExtensionTypeButtons();
  const isSwipe = singleEditType === 'swipe_up' || singleEditType === 'swipe_down';
  const isInput = singleEditType === 'input';
  const swipeGroup = document.getElementById('ep-swipe-group');
  const inputGroup = document.getElementById('ep-input-group');
  // V5.8 Menu Titik Klik Berdasarkan Tipe Aksi:
  // - TAP: Sembunyikan Panjang Garis & Skala Tenaga (ep-swipe-group) DAN sembunyikan seluruh form Input Ketik & Popup Key (ep-input-group)
  // - SWIPE ATAS & BAWAH: Tampilkan hanya Panjang Garis & Skala Tenaga (ep-swipe-group), sembunyikan form Input Ketik & Popup Key (ep-input-group)
  // - INPUT KETIK (+Popup): Tampilkan hanya form Input Ketik & Popup Key (ep-input-group), sembunyikan Panjang Garis & Skala Tenaga (ep-swipe-group)
  if (swipeGroup) {
    swipeGroup.classList.toggle('hidden', !isSwipe);
    swipeGroup.style.display = isSwipe ? 'flex' : 'none';
  }
  if (inputGroup) {
    inputGroup.classList.toggle('hidden', !isInput);
    inputGroup.style.display = isInput ? 'flex' : 'none';
  }
}

function openSinglePointEditor(accountId, pointId, tabIndex = null) {
  if (!currentSnapshot || !Array.isArray(currentSnapshot.accounts)) return;
  const acc = currentSnapshot.accounts.find((a) => a.accountId === Number(accountId));
  if (!acc) return;
  const resolvedTabIdx = Math.min(3, Math.max(1, Number(tabIndex) || acc.activeTabIndex || 1));
  const tabData = getTabPointsAndSettings(acc, resolvedTabIdx);
  const pts = Array.isArray(tabData.points) ? tabData.points : [];
  const idx = pts.findIndex((p) => p.id === pointId);
  if (idx === -1) return;
  const pt = pts[idx];

  singleEditAccountId = acc.accountId;
  singleEditTabIndex = resolvedTabIdx;
  singleEditPointId = pt.id;
  singleEditOrder = idx + 1;
  singleEditType = mapLegacyPointType(pt);

  const accLabel = acc.name || `Akun ${acc.accountId}`;
  document.getElementById('ep-modal-sub').textContent =
    `${accLabel} • Tab ${resolvedTabIdx} • Titik ${idx + 1}/${pts.length}`;
  document.getElementById('btn-ep-order-left').disabled = idx === 0;
  document.getElementById('btn-ep-order-right').disabled = idx >= pts.length - 1;

  document.getElementById('ep-enabled').checked = pt.enabled !== false;
  document.getElementById('ep-x').value = String(pt.x ?? 540);
  document.getElementById('ep-y').value = String(pt.y ?? 420);
  document.getElementById('ep-label').value = pt.label || `Titik ${idx + 1}`;
  document.getElementById('ep-radius').value = String(pt.radiusPx ?? 28);
  document.getElementById('ep-selector').value = pt.selector || '';
  document.getElementById('ep-coord-preview').textContent = `x=${pt.x ?? 540}, y=${pt.y ?? 420}`;

  // Delay default 0.5 detik (500ms)/titik
  const delayMs = Math.max(15, Number(pt.delayAfterMs) || 500);
  const useSec = delayMs >= 100 && delayMs % 100 === 0;
  document.querySelectorAll('input[name="ep-delay-unit"]').forEach((r) => {
    r.checked = (r.value === 's') === useSec;
  });
  document.getElementById('ep-delay-val').value = String(useSec ? (delayMs / 1000) : delayMs);

  // Icon size
  document.getElementById('ep-icon-size').value = String(Math.min(100, Math.max(15, Number(pt.iconSizePct) || 25)));
  updateIconSizeDisplay();

  // Swipe fields
  const lineLen = Math.max(40, Number(pt.lineLengthPx) || Math.abs((pt.endY ?? ((pt.y || 420) - 350)) - (pt.y || 420)) || 350);
  document.getElementById('ep-swipe-len').value = String(lineLen);
  document.getElementById('ep-swipe-scale').value = String(Math.max(1, Number(pt.swipeForceScale) || 2));
  updateSwipeSummaryDisplay();

  // Input (+Popup Pin Ungu) fields
  document.getElementById('ep-popup-pin-enabled').checked = Boolean(pt.popupPinEnabled);
  document.getElementById('ep-popup-pin-label').textContent =
    `Aktifkan Popup Key Autofill (Pin Ungu #${singleEditOrder})`;
  const popupDelayEl = document.getElementById('ep-popup-delay');
  if (popupDelayEl) {
    popupDelayEl.value = String(Math.min(10000, Math.max(20, Number(pt.popupDelayMs) || 300)));
  }
  updatePopupDelayPresetsDisplay();
  singleEditCycleTexts = Array.isArray(pt.cycleTexts) && pt.cycleTexts.length > 0
    ? pt.cycleTexts.slice()
    : [pt.inputValue || pt.text || ''];
  renderCycleTextRows();
  document.getElementById('ep-typing-speed').value = String(Number.isFinite(Number(pt.typingSpeedMs)) && Number(pt.typingSpeedMs) > 0 ? Number(pt.typingSpeedMs) : 30);
  updateTypingSpeedPresetsDisplay();
  document.getElementById('ep-clear-before').checked = pt.clearBeforeType !== false;
  document.getElementById('ep-focus-before').checked = pt.focusBeforeType !== false;
  document.getElementById('ep-antirevert').checked = false;

  updateSinglePointEditorFieldsVisibility();
  openModalById('ak-edit-point-modal');
}

document.querySelectorAll('.ep-type-btn').forEach((btn) => {
  btn.onclick = () => {
    singleEditType = btn.getAttribute('data-type') || 'tap';
    updateSinglePointEditorFieldsVisibility();
  };
});

document.getElementById('ep-popup-pin-enabled').onchange = updatePopupTriggerPreview;
const epPopupDelayEl = document.getElementById('ep-popup-delay');
if (epPopupDelayEl) {
  epPopupDelayEl.oninput = () => {
    updatePopupDelayPresetsDisplay();
    updatePopupTriggerPreview();
  };
}
document.querySelectorAll('.ep-popup-delay-preset').forEach((btn) => {
  btn.onclick = () => {
    if (epPopupDelayEl) epPopupDelayEl.value = btn.getAttribute('data-popup-delay') || btn.getAttribute('data-pdelay') || '300';
    updatePopupDelayPresetsDisplay();
    updatePopupTriggerPreview();
  };
});
document.getElementById('ep-icon-size').oninput = updateIconSizeDisplay;
document.getElementById('ep-swipe-len').oninput = updateSwipeSummaryDisplay;
document.getElementById('ep-swipe-scale').oninput = updateSwipeSummaryDisplay;
document.querySelectorAll('.ep-scale-preset').forEach((btn) => {
  btn.onclick = () => {
    document.getElementById('ep-swipe-scale').value = btn.getAttribute('data-scale') || '2';
    updateSwipeSummaryDisplay();
  };
});

document.getElementById('btn-ep-add-cycle-text').onclick = () => {
  singleEditCycleTexts.push('');
  renderCycleTextRows();
};

document.getElementById('ep-typing-speed').oninput = updateTypingSpeedPresetsDisplay;
document.querySelectorAll('.ep-speed-preset').forEach((btn) => {
  btn.onclick = () => {
    document.getElementById('ep-typing-speed').value = btn.getAttribute('data-speed') || '30';
    updateTypingSpeedPresetsDisplay();
  };
});

document.querySelectorAll('input[name="ep-delay-unit"]').forEach((radio) => {
  radio.onchange = () => {
    const valEl = document.getElementById('ep-delay-val');
    const num = Number(valEl.value) || 0.1;
    if (radio.value === 's' && radio.checked && num >= 15) {
      valEl.value = String(Math.max(0.05, Math.round(num / 10) / 100));
    } else if (radio.value === 'ms' && radio.checked && num < 15) {
      valEl.value = String(Math.round(num * 1000));
    }
  };
});

document.getElementById('btn-ep-order-left').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  await window.ferizyShell.moveExtPoint(singleEditAccountId, singleEditPointId, 'up', singleEditTabIndex);
  currentSnapshot = await window.ferizyShell.getState();
  openSinglePointEditor(singleEditAccountId, singleEditPointId, singleEditTabIndex);
};

document.getElementById('btn-ep-order-right').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  await window.ferizyShell.moveExtPoint(singleEditAccountId, singleEditPointId, 'down', singleEditTabIndex);
  currentSnapshot = await window.ferizyShell.getState();
  openSinglePointEditor(singleEditAccountId, singleEditPointId, singleEditTabIndex);
};

document.getElementById('btn-close-edit-point').onclick = () => closeModalById('ak-edit-point-modal');
document.getElementById('btn-ep-cancel').onclick = () => closeModalById('ak-edit-point-modal');
document.getElementById('btn-ep-clear-sel').onclick = () => {
  document.getElementById('ep-selector').value = '';
};

document.getElementById('btn-ep-repick').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  pendingRepickTarget = { accountId: singleEditAccountId, tabIndex: singleEditTabIndex, pointId: singleEditPointId };
  closeModalById('ak-edit-point-modal');
  const res = await window.ferizyShell.startExtPicker(singleEditAccountId, singleEditType, singleEditPointId, singleEditTabIndex);
  if (!res || !res.ok) {
    pendingRepickTarget = null;
    openModalById('ak-edit-point-modal');
  } else {
    document.getElementById('active-meta').textContent =
      `Klik posisi baru di website Tab ${singleEditTabIndex} untuk memperbarui titik ini (ESC batal).`;
  }
};

document.getElementById('btn-ep-delete').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  await window.ferizyShell.removeExtPoint(singleEditAccountId, singleEditPointId, singleEditTabIndex);
  closeModalById('ak-edit-point-modal');
};

document.getElementById('btn-ep-save').onclick = async () => {
  if (!singleEditAccountId || !singleEditPointId) return;
  const type = singleEditType || 'tap';
  const x = Math.max(4, Math.round(Number(document.getElementById('ep-x').value) || 0));
  const y = Math.max(4, Math.round(Number(document.getElementById('ep-y').value) || 0));

  const unitEl = document.querySelector('input[name="ep-delay-unit"]:checked');
  const unit = unitEl ? unitEl.value : 'ms';
  const parsedDelay = Number(document.getElementById('ep-delay-val').value);
  const rawDelay = Number.isFinite(parsedDelay) && parsedDelay > 0 ? parsedDelay : (unit === 's' ? 0.5 : 500);
  const delayAfterMs = Math.max(15, Math.round(unit === 's' ? rawDelay * 1000 : rawDelay));

  const iconSizePct = Math.min(100, Math.max(15, Math.round(Number(document.getElementById('ep-icon-size').value) || 25)));
  const lineLengthPx = Math.min(1500, Math.max(40, Math.round(Number(document.getElementById('ep-swipe-len').value) || 350)));
  const swipeForceScale = Math.min(20, Math.max(1, Math.round(Number(document.getElementById('ep-swipe-scale').value) || 2)));
  const cycleTexts = Array.isArray(singleEditCycleTexts) && singleEditCycleTexts.length > 0
    ? singleEditCycleTexts.map((s) => String(s ?? ''))
    : [''];

  const accObj = currentSnapshot && currentSnapshot.accounts && currentSnapshot.accounts.find((a) => a.accountId === singleEditAccountId);
  const tabData = getTabPointsAndSettings(accObj, singleEditTabIndex);
  const oldPt = Array.isArray(tabData.points) ? tabData.points.find((p) => p.id === singleEditPointId) : null;

  const patch = {
    enabled: document.getElementById('ep-enabled').checked,
    type,
    label: document.getElementById('ep-label').value.trim() || `Titik ${singleEditOrder}`,
    x,
    y,
    delayAfterMs,
    iconSizePct,
    radiusPx: Math.min(200, Math.max(5, Number(document.getElementById('ep-radius').value) || 28)),
    selector: document.getElementById('ep-selector').value.trim(),
    subLoopEnabled: false,
    subLoopTargetOrder: 1,
    subLoopCount: 1
  };

  if (type === 'swipe_up' || type === 'swipe_down') {
    patch.lineLengthPx = lineLengthPx;
    patch.swipeForceScale = swipeForceScale;
    patch.endX = x;
    patch.endY = type === 'swipe_down' ? (y + lineLengthPx) : Math.max(4, y - lineLengthPx);
    patch.durationMs = 280;
  } else if (type === 'input') {
    patch.cycleTexts = cycleTexts;
    patch.inputValue = cycleTexts[0] || '';
    patch.text = patch.inputValue;
    patch.typingSpeedMs = Math.min(1000, Math.max(5, Math.round(Number(document.getElementById('ep-typing-speed').value) || 30)));
    patch.popupDelayMs = Math.min(10000, Math.max(20, Math.round(Number(document.getElementById('ep-popup-delay').value) || 300)));
    patch.clearBeforeType = document.getElementById('ep-clear-before').checked;
    patch.focusBeforeType = document.getElementById('ep-focus-before').checked;
    patch.antiRevert = false;
    patch.popupPinEnabled = document.getElementById('ep-popup-pin-enabled').checked;
    if (patch.popupPinEnabled) {
      patch.popupPinX = (oldPt && Number.isFinite(Number(oldPt.popupPinX))) ? Number(oldPt.popupPinX) : (x + 56);
      patch.popupPinY = (oldPt && Number.isFinite(Number(oldPt.popupPinY))) ? Number(oldPt.popupPinY) : (y + 42);
    }
  }

  // Siklus default permanen V5.8: dari titik terakhir selalu mengulang kembali ke Titik #1 (mainLoopStartOrder: 1)
  await window.ferizyShell.saveExtSettings(
    singleEditAccountId,
    { loopEnabled: true, maxLoops: 0, mainLoopStartOrder: 1 },
    singleEditTabIndex
  );
  await window.ferizyShell.updateExtPoint(singleEditAccountId, singleEditPointId, patch, singleEditTabIndex);
  closeModalById('ak-edit-point-modal');
  document.getElementById('active-meta').textContent =
    `Titik #${singleEditOrder} (${type}) diperbarui di Akun ${singleEditAccountId} Tab ${singleEditTabIndex}.`;
};

document.getElementById('btn-ak-pick-target').onclick = async () => {
  const type = document.getElementById('ak-pt-type').value || 'tap';
  pendingPickerFromHeaderModal = true;
  closeModalById('autoklik-ext-modal');
  const res = await window.ferizyShell.startExtPicker(extAccountId, type, null, extTabIndex);
  if (!res || !res.ok) {
    pendingPickerFromHeaderModal = false;
    openModalById('autoklik-ext-modal');
    setAkStatus((res && res.reason) || 'Gagal membuka mode ambil titik.');
  }
};

document.getElementById('btn-ak-export-json').onclick = async () => {
  const res = await window.ferizyShell.exportExtJson(extAccountId, extTabIndex);
  if (res && res.json) {
    document.getElementById('ak-json-io').value = res.json;
    setAkStatus(`JSON AutoKlik v3.2 Tab ${extTabIndex} Akun ${extAccountId} dimuat ke kotak teks.`);
  }
};

document.getElementById('btn-ak-import-json').onclick = async () => {
  const raw = document.getElementById('ak-json-io').value.trim();
  if (!raw) {
    setAkStatus('Tempel JSON terlebih dahulu sebelum import.');
    return;
  }
  const res = await window.ferizyShell.importExtJson(extAccountId, raw, extTabIndex);
  if (res && res.ok) {
    refreshAutoklikExtModalFromSnapshot(currentSnapshot, true);
    setAkStatus(`Berhasil import ${res.count} titik ke Tab ${extTabIndex} Akun ${extAccountId}.`);
  } else {
    setAkStatus((res && res.reason) || 'Import JSON gagal.');
  }
};

function setLcmStatus(text) {
  const el = document.getElementById('lcm-status-msg');
  if (el) el.textContent = text;
}

async function refreshLoadCopyModalClipboard() {
  const badge = document.getElementById('lcm-clipboard-badge');
  if (!badge || !window.ferizyShell.getClipboardSummary) return;
  const clip = await window.ferizyShell.getClipboardSummary();
  if (clip && clip.hasClipboard) {
    badge.textContent = `Clipboard Siap: ${clip.count} titik dari Akun ${clip.sourceAccountId} • Tab ${clip.sourceTabIndex}`;
  } else {
    badge.textContent = 'Clipboard: Belum ada konfigurasi yang disalin (atau pilih Sumber di bawah lalu langsung klik Paste).';
  }
}

function populateLoadCopySelects(targetAccId, targetTabIdx, sourceAccId, sourceTabIdx) {
  const accounts = (currentSnapshot && Array.isArray(currentSnapshot.accounts)) ? currentSnapshot.accounts : [];
  const tAccSel = document.getElementById('lcm-target-account');
  const sAccSel = document.getElementById('lcm-source-account');
  const tTabSel = document.getElementById('lcm-target-tab');
  const sTabSel = document.getElementById('lcm-source-tab');
  if (!tAccSel || !sAccSel || !tTabSel || !sTabSel) return;

  tAccSel.innerHTML = '';
  sAccSel.innerHTML = '';
  accounts.forEach((a) => {
    const lbl = `${a.name || `Akun ${a.accountId}`} (${a.totalAccountPoints ?? a.pointsCount ?? 0} titik)`;
    const optT = document.createElement('option');
    optT.value = String(a.accountId);
    optT.textContent = lbl;
    if (a.accountId === targetAccId) optT.selected = true;
    tAccSel.appendChild(optT);

    const optS = document.createElement('option');
    optS.value = String(a.accountId);
    optS.textContent = lbl;
    if (a.accountId === sourceAccId) optS.selected = true;
    sAccSel.appendChild(optS);
  });

  tTabSel.value = String(targetTabIdx || 1);
  sTabSel.value = String(sourceTabIdx || 1);

  const accObj = accounts.find((a) => a.accountId === Number(tAccSel.value));
  const sub = document.getElementById('lcm-subtitle');
  if (sub && accObj) {
    sub.textContent = `${accObj.name || `Akun ${accObj.accountId}`} • Tab ${tTabSel.value} (${accObj.partition})`;
  }
}

async function openLoadCopyModal(accountId = null, tabIndex = null) {
  const tAcc = Number(accountId) || (currentSnapshot && currentSnapshot.activeAccountId) || 1;
  const accObj = currentSnapshot && Array.isArray(currentSnapshot.accounts)
    ? currentSnapshot.accounts.find((a) => a.accountId === tAcc)
    : null;
  const tTab = Math.min(3, Math.max(1, Number(tabIndex) || (accObj && accObj.activeTabIndex) || 1));

  let sAcc = tAcc;
  let sTab = tTab;
  if (window.ferizyShell.getClipboardSummary) {
    const clip = await window.ferizyShell.getClipboardSummary();
    if (clip && clip.hasClipboard) {
      sAcc = clip.sourceAccountId || tAcc;
      sTab = clip.sourceTabIndex || tTab;
    }
  }

  populateLoadCopySelects(tAcc, tTab, sAcc, sTab);
  await refreshLoadCopyModalClipboard();
  setLcmStatus(`Siap Load file JSON atau Copy-Paste konfigurasi untuk Akun ${tAcc} • Tab ${tTab}.`);
  openModalById('ak-load-copy-modal');
}

const closeLcmBtn = document.getElementById('btn-close-load-copy-modal');
if (closeLcmBtn) {
  closeLcmBtn.onclick = () => closeModalById('ak-load-copy-modal');
}

['lcm-target-account', 'lcm-target-tab'].forEach((id) => {
  const el = document.getElementById(id);
  if (el) {
    el.onchange = () => {
      const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
      const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
      const accounts = (currentSnapshot && Array.isArray(currentSnapshot.accounts)) ? currentSnapshot.accounts : [];
      const accObj = accounts.find((a) => a.accountId === tAcc);
      const sub = document.getElementById('lcm-subtitle');
      if (sub && accObj) {
        sub.textContent = `${accObj.name || `Akun ${accObj.accountId}`} • Tab ${tTab} (${accObj.partition})`;
      }
    };
  }
});

const lcmCopyTabBtn = document.getElementById('btn-lcm-copy-tab');
if (lcmCopyTabBtn) {
  lcmCopyTabBtn.onclick = async () => {
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.copyTabConfig(sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Copy ${res.count} titik dari Akun ${sAcc} • Tab ${sTab} ke Clipboard.`);
      document.getElementById('active-meta').textContent =
        `Copy konfigurasi: ${res.count} titik dari Akun ${sAcc} Tab ${sTab} siap di-Paste.`;
    }
  };
}

const lcmPasteTabBtn = document.getElementById('btn-lcm-paste-tab');
if (lcmPasteTabBtn) {
  lcmPasteTabBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.pasteTabConfig(tAcc, tTab, sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Paste ${res.count} titik dari Akun ${res.sourceAccountId} Tab ${res.sourceTabIndex} ke Akun ${tAcc} Tab ${tTab}.`);
      document.getElementById('active-meta').textContent =
        `Konfigurasi (${res.count} titik) berhasil di-Paste ke Akun ${tAcc} • Tab ${tTab}.`;
    } else {
      setLcmStatus((res && res.reason) || 'Gagal melakukan Paste konfigurasi.');
    }
  };
}

const lcmPasteAllTabsAccBtn = document.getElementById('btn-lcm-paste-all-tabs-acc');
if (lcmPasteAllTabsAccBtn) {
  lcmPasteAllTabsAccBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.pasteToAllTabsInAccount(tAcc, sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Paste ${res.count} titik dari Akun ${sAcc} Tab ${sTab} ke SEMUA Tab (Tab 1, 2, 3) di Akun ${tAcc}.`);
      document.getElementById('active-meta').textContent =
        `Konfigurasi (${res.count} titik) disalin merata ke Tab 1, Tab 2 & Tab 3 pada Akun ${tAcc}.`;
    }
  };
}

const lcmPasteAllAccountsBtn = document.getElementById('btn-lcm-paste-all-accounts');
if (lcmPasteAllAccountsBtn) {
  lcmPasteAllAccountsBtn.onclick = async () => {
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.pastePointsToAll(sAcc, sTab);
    await refreshLoadCopyModalClipboard();
    if (res && res.ok) {
      setLcmStatus(`Berhasil Paste ${res.count} titik dari Akun ${sAcc} Tab ${sTab} ke semua akun (Tab ${sTab}).`);
      document.getElementById('active-meta').textContent =
        `Konfigurasi Akun ${sAcc} Tab ${sTab} (${res.count} titik) berhasil di-Paste ke seluruh akun.`;
    }
  };
}

const lcmLoadFileBtn = document.getElementById('btn-lcm-load-file');
if (lcmLoadFileBtn) {
  lcmLoadFileBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const res = await window.ferizyShell.openPointConfigDialog(tAcc, tTab);
    if (res && res.ok) {
      setLcmStatus(`Berhasil Load ${res.count} titik dari file ke Akun ${tAcc} • Tab ${tTab}.`);
      document.getElementById('active-meta').textContent =
        `Load konfigurasi berhasil: ${res.count} titik dimuat ke Akun ${tAcc} Tab ${tTab}.`;
    } else if (res && !res.canceled && res.reason) {
      setLcmStatus(`Gagal Load file: ${res.reason}`);
    }
  };
}

const lcmSaveFileBtn = document.getElementById('btn-lcm-save-file');
if (lcmSaveFileBtn) {
  lcmSaveFileBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const res = await window.ferizyShell.savePointConfigDialog(tAcc, tTab);
    if (res && res.ok) {
      setLcmStatus(res.canceled
        ? `Konfigurasi Akun ${tAcc} Tab ${tTab} tersimpan di ${res.storeFile}.`
        : `Konfigurasi Akun ${tAcc} Tab ${tTab} disimpan ke ${res.filePath}`);
    }
  };
}

const lcmCopyJsonBtn = document.getElementById('btn-lcm-copy-json');
if (lcmCopyJsonBtn) {
  lcmCopyJsonBtn.onclick = async () => {
    const sAcc = Number(document.getElementById('lcm-source-account').value) || 1;
    const sTab = Number(document.getElementById('lcm-source-tab').value) || 1;
    const res = await window.ferizyShell.exportExtJson(sAcc, sTab);
    if (res && res.json) {
      const box = document.getElementById('lcm-json-box');
      if (box) box.value = res.json;
      try {
        await navigator.clipboard.writeText(res.json);
      } catch (_) {}
      setLcmStatus(`JSON konfigurasi Akun ${sAcc} Tab ${sTab} dimuat & disalin ke clipboard.`);
    }
  };
}

const lcmApplyJsonBtn = document.getElementById('btn-lcm-apply-json');
if (lcmApplyJsonBtn) {
  lcmApplyJsonBtn.onclick = async () => {
    const tAcc = Number(document.getElementById('lcm-target-account').value) || 1;
    const tTab = Number(document.getElementById('lcm-target-tab').value) || 1;
    const box = document.getElementById('lcm-json-box');
    const raw = box ? box.value.trim() : '';
    if (!raw) {
      setLcmStatus('Kotak JSON masih kosong. Klik "Muat & Copy JSON Sumber" atau tempel JSON terlebih dahulu.');
      return;
    }
    const res = await window.ferizyShell.importExtJson(tAcc, raw, tTab);
    if (res && res.ok) {
      setLcmStatus(`Berhasil menerapkan ${res.count} titik dari JSON ke Akun ${tAcc} • Tab ${tTab}.`);
      document.getElementById('active-meta').textContent =
        `JSON konfigurasi (${res.count} titik) diterapkan ke Akun ${tAcc} Tab ${tTab}.`;
    } else {
      setLcmStatus((res && res.reason) || 'Format JSON tidak valid.');
    }
  };
}

function applySystemMetrics(m) {
  if (!m) return;
  const pill = document.getElementById('sys-meter-pill');
  const cleanBtn = document.getElementById('btn-quick-clean');
  if (!pill) return;

  const browserRamLabel = m.browserRamMb >= 1024
    ? `${Number(m.browserRamGb || (m.browserRamMb / 1024)).toFixed(2)}GB`
    : `${m.browserRamMb || 0}MB`;
  const coreText = `CPU PC: ${m.pcCpuPercent ?? 0}% • RAM PC: ${m.pcRamUsedGb ?? 0}/${m.pcRamTotalGb ?? 0}GB (${m.pcRamPercent ?? 0}%) | Browser: ${browserRamLabel} (${m.browserCpuPercent ?? 0}%)`;

  if (m.severity === 'over') {
    pill.textContent = `OVER! ${coreText}`;
    pill.className = 'pill pill-danger';
    pill.title = `${m.reasonText || 'Beban OVER'} — Klik ⚡ Bersih Cepat untuk membebaskan RAM secara aman tanpa menyentuh tab aktif/menembak.`;
    if (cleanBtn) {
      cleanBtn.className = 'btn-small is-over';
      if (!cleanBtn.disabled) cleanBtn.textContent = '⚡ Bersih Cepat!';
    }
  } else if (m.severity === 'warn') {
    pill.textContent = coreText;
    pill.className = 'pill pill-warn';
    pill.title = `${m.reasonText || 'Beban Mulai Padat'} — Sisa RAM PC: ${m.pcRamFreeGb ?? 0} GB`;
    if (cleanBtn) {
      cleanBtn.className = 'btn-small is-warn';
      if (!cleanBtn.disabled) cleanBtn.textContent = '⚡ Bersih Cepat';
    }
  } else {
    pill.textContent = coreText;
    pill.className = 'pill pill-ok';
    pill.title = `Kinerja PC Normal • Sisa RAM fisik: ${m.pcRamFreeGb ?? 0} GB`;
    if (cleanBtn) {
      cleanBtn.className = 'btn-small';
      if (!cleanBtn.disabled) cleanBtn.textContent = '⚡ Bersih Cepat';
    }
  }
}

const quickCleanBtnEl = document.getElementById('btn-quick-clean');
if (quickCleanBtnEl) {
  quickCleanBtnEl.onclick = async () => {
    quickCleanBtnEl.disabled = true;
    quickCleanBtnEl.textContent = 'Membersihkan...';
    try {
      const res = await window.ferizyShell.runQuickClean();
      if (res && res.ok) {
        if (res.after) applySystemMetrics(res.after);
        document.getElementById('active-meta').textContent =
          `Bersih Cepat selesai: ${res.clearedSessionsCount || 0} sesi dibersihkan cache HTTP (login tetap aman), ${res.queuedStandbyCount || 0} tab Standby tersembunyi di-recycle, hemat ${res.savedRamMb || 0}MB RAM.`;
      } else {
        document.getElementById('active-meta').textContent = (res && res.reason) || 'Pembersihan cepat selesai.';
      }
    } finally {
      quickCleanBtnEl.disabled = false;
      quickCleanBtnEl.textContent = quickCleanBtnEl.classList.contains('is-over') ? '⚡ Bersih Cepat!' : '⚡ Bersih Cepat';
    }
  };
}

window.addEventListener('resize', syncHeaderHeight);
window.ferizyShell.onStateUpdated(renderSnapshot);
window.ferizyShell.onSystemMetrics(applySystemMetrics);
window.ferizyShell.onPlayAlarm((payload) => playAlarmTone(payload && payload.alarm));
window.ferizyShell.onOpenPointEditor((payload) => {
  if (!payload || !payload.accountId || !payload.pointId) return;
  openSinglePointEditor(payload.accountId, payload.pointId, payload.tabIndex || null);
});
window.ferizyShell.onExtPickerFinished((payload) => {
  if (!payload) return;
  const repick = pendingRepickTarget;
  pendingRepickTarget = null;
  if (repick) {
    setTimeout(() => openSinglePointEditor(repick.accountId, repick.pointId, repick.tabIndex || null), 40);
    return;
  }
  const shouldOpenModal = pendingPickerFromHeaderModal;
  pendingPickerFromHeaderModal = false;
  if (shouldOpenModal) {
    openAutoklikExtModal(payload.accountId || extAccountId, payload.tabIndex || extTabIndex);
  }
  if (payload.cancelled) {
    setAkStatus('Ambil titik dibatalkan (ESC).');
  } else if (payload.point) {
    setAkStatus(`Titik "${payload.point.label}" (${payload.point.x},${payload.point.y}) tersimpan ke Tab ${payload.tabIndex || extTabIndex} Akun ${payload.accountId}.`);
    document.getElementById('active-meta').textContent =
      `Titik #${payload.point.order || ''} "${payload.point.label}" (${payload.point.x},${payload.point.y}) tampil di Tab ${payload.tabIndex || extTabIndex} Akun ${payload.accountId}.`;
  }
});
window.ferizyShell.getState().then(renderSnapshot);
window.ferizyShell.getSystemMetrics().then(applySystemMetrics);
