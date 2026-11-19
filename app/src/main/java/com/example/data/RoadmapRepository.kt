package com.example.data

import com.example.domain.OptionalFeatureOption
import com.example.domain.PhaseStatus
import com.example.domain.RoadmapPhaseInfo

object RoadmapRepository {

  val phases: List<RoadmapPhaseInfo> =
    listOf(
      RoadmapPhaseInfo(
        phaseNumber = 1,
        title = "Fase 1: Pondasi Multi-Container & Pre-Flight",
        summary = "Kerangka Electron + WebContentsView terisolasi tanpa ekstensi luar & tanpa chrome.debugger.",
        deliverables =
          listOf(
            "Auto-open 5 tab default https://trip.ferizy.com (Tab 2 & 3 -> /order) dengan staggered boot 250–400ms",
            "Hard Cap maksimal 3 tab/akun (kunci otomatis tombol + Tab di 3/3, maks 15 tab)",
            "Panel default Pre-Flight Check 1 layar (Login, IP/Proxy, Izin Lokasi, Titik, ntfy.sh)",
            "Auto-grant geolocation khusus trip.ferizy.com + Provider 0ms & Micro-Jitter",
            "session.setProxy per akun + auto-auth app.on('login') + WebRTC disable_non_proxied_udp + hapus jejak Electron/",
            "Close-to-Tray 24 jam + powerSaveBlocker + backgroundThrottling: false",
          ),
        optionsAndImpact =
          "Status: Aktif penuh di generator Electron (.ZIP).",
        status = PhaseStatus.IMPLEMENTED,
      ),
      RoadmapPhaseInfo(
        phaseNumber = 2,
        title = "Fase 2: Mesin Otomasi Native & Pengaman Performa",
        summary = "Eksekusi ketik/klik isTrusted: true, isolasi menang per-tab, dan proteksi RAM 16 GB.",
        deliverables =
          listOf(
            "Mesin webContents.sendInputEvent (Ketik, Tap, Swipe, Popup Key, Direct Element Cache, Radius Scanner)",
            "Lazy Anti-Revert aktif + MutationObserver (Pesan Sekarang -> Setuju) & Smart Network Sync",
            "Penyimpanan profil titik independen per akun + Copy/Paste lintas 5 akun di dalam Electron",
            "Mode Menang Default — Isolasi Ketat Per-Tab (hanya tab yang tembus /order/verification|/payment|/invoice yang Auto-Stop 0ms)",
            "Asset & Media Blocker Selektif (CSS & CAPTCHA tetap aman) + Memory Watchdog 350 MB standby",
            "Staggered Reload Queue 400 ms Reaktif Saja (Opsi A: hanya aktif saat Watchdog >350 MB atau tombol manual, menjaga input penumpang tidak ter-reset)",
          ),
        optionsAndImpact =
          "Pilihan terkonfirmasi: Asset Blocker Selektif, Watchdog 350 MB, Isolasi Per-Tab + Lazy Anti-Revert aktif, Staggered Queue 400 ms Reaktif Saja.",
        status = PhaseStatus.IMPLEMENTED,
      ),
      RoadmapPhaseInfo(
        phaseNumber = 3,
        title = "Fase 3: Sniffer 0ms, Event Bus & Modul Ceklist",
        summary = "Sinyal lintas-tab <1ms, sinkronisasi jam server Ferizy, dan notifikasi terpusat Main Process (Opsi A).",
        deliverables =
          listOf(
            "0ms API Sniffer (Main-World fetch/XHR tanpa chrome.debugger) + Main Process Event Bus (<1ms)",
            "Server Clock Sync berbasis header HTTP Date server Ferizy + estimasi RTT",
            "Notifikasi terpusat Main Process (Opsi A): ntfy.sh (dedupe 10s), Windows Toast & Web Audio Alarm 6 event",
            "Aturan URL ketat: Lintasan Dibuka hanya saat berjalan di https://trip.ferizy.com/ & Kuota Muncul hanya saat berjalan di https://trip.ferizy.com/ferryschedule",
            "5 Modul Ceklist Opsional (Default Rekomendasi: Keep-Alive ~12m ON, CAPTCHA Auto-Pop ON, Smart Matcher ON, Latency OFF, Stop Per-Tab Default)",
          ),
        optionsAndImpact =
          "Status: Aktif penuh di generator Electron (.ZIP) Fase 3 sesuai opsi terkonfirmasi Anda.",
        status = PhaseStatus.IMPLEMENTED,
      ),
      RoadmapPhaseInfo(
        phaseNumber = 4,
        title = "Fase 4: Uji Ketahanan 24 Jam (i7-4790 / 16 GB / 256 GB)",
        summary = "Log Rotation aman SSD 256 GB, kalibrasi Memory Watchdog 350 MB, Health Report JSON & simulasi stress test 15 tab 24 jam.",
        deliverables =
          listOf(
            "Log Rotation otomatis (50 MB / 7 hari default rekomendasi atau 20 MB / 3 hari hemat SSD) dengan batas total folder log",
            "EnduranceMonitor 24 jam & ekspor Health Report JSON berkala (health-report-24h.json)",
            "Kalibrasi akhir Watchdog 350 MB + Staggered Queue 400 ms Reaktif (tab aktif/menang & input penumpang tetap aman)",
            "CPU & RAM Meter PC + Browser di taskbar (update 3 detik, nol beban sniffer) + peringatan merah >=85% & tombol Bersih Cepat",
            "Skrip stress test 15 tab × 1.440 menit via npm run test:endurance + tombol Uji 24J di shell Electron",
          ),
        optionsAndImpact =
          "Status: Aktif penuh di generator Electron (.ZIP) Fase 4 (Default: 50 MB / 7 Hari + Health Report JSON).",
        status = PhaseStatus.IMPLEMENTED,
      ),
    )

  val optionalModules: List<OptionalFeatureOption> =
    listOf(
      OptionalFeatureOption(
        id = "keep_alive",
        title = "Gentle Session Keep-Alive (~12 Menit)",
        targetPhase = "Fase 3 (Aktif Default)",
        description = "Ping sesi login HEAD ringan bergiliran per akun tiap ~12 menit agar token Ferizy tidak kedaluwarsa saat standby 24 jam tanpa me-reload halaman.",
        optionsDetail = "Rekomendasi aktif: ping 1 akun tiap ~2,4 menit (siklus 12 menit/akun) & langsung deteksi HTTP 401.",
        impactRamCpu = "+0 MB RAM, <0.5% CPU (tanpa membuka tab atau mereset input penumpang).",
        impactRiskSpeed = "Menurunkan risiko silent logout di tengah malam tanpa mengganggu data penumpang.",
        defaultChecked = true,
      ),
      OptionalFeatureOption(
        id = "captcha_autopop",
        title = "Detektor CAPTCHA / Challenge + Auto-Pop Tray",
        targetPhase = "Fase 3 (Aktif Default)",
        description = "Saat widget CAPTCHA/Challenge muncul, tab terkait langsung pause (PAUSED_CAPTCHA), jendela browser muncul ke depan dari Tray, dan alarm + ntfy.sh berbunyi.",
        optionsDetail = "Rekomendasi aktif: Auto-Pop jendela + fokus otomatis tab bermasalah.",
        impactRamCpu = "+1–2 MB RAM (observer DOM ringan pada tab aktif).",
        impactRiskSpeed = "Mencegah klik berulang saat CAPTCHA aktif sehingga menghindari risiko blokir/ban IP.",
        defaultChecked = true,
      ),
      OptionalFeatureOption(
        id = "smart_matcher",
        title = "Smart Target Matcher (Jam Prioritas & Kuota > 0)",
        targetPhase = "Fase 3 (Aktif Default)",
        description = "Memilih slot tombol jadwal berdasarkan daftar jam prioritas & kuota > 0 dari API Sniffer 0ms / DOM jika urutan baris kapal berubah.",
        optionsDetail = "Rekomendasi aktif: 08:00,10:00,12:00,14:00,20:00 (dapat diubah langsung).",
        impactRamCpu = "+2 MB RAM untuk cache indeks jadwal.",
        impactRiskSpeed = "Akurasi tembak lebih presisi dibanding koordinat statis saat daftar kapal bergeser.",
        defaultChecked = true,
      ),
      OptionalFeatureOption(
        id = "latency_timeout",
        title = "Latency Compensation + Fast Request Timeout (4.5 Detik)",
        targetPhase = "Fase 3 (Nonaktif Default — Opsional)",
        description = "Kompensasi ping RTT/2 saat eksekusi aksi & membatalkan fetch/XHR Ferizy yang menggantung >4.500ms agar tab tidak macet.",
        optionsDetail = "Default OFF untuk keamanan koneksi lambat; aktifkan saat detik pembukaan kuota padat.",
        impactRamCpu = "Hemat CPU & mencegah antrean socket menumpuk saat server sibuk.",
        impactRiskSpeed = "Lebih agresif saat jam war, namun koneksi sangat lambat >4.5s bisa terputus lebih cepat.",
        defaultChecked = false,
      ),
      OptionalFeatureOption(
        id = "advanced_stop",
        title = "Opsi Stop Lanjutan (Di Luar Isolasi Per-Tab)",
        targetPhase = "Fase 3 (Default: Isolasi Per-Tab)",
        description = "Pilihan untuk menghentikan tab lain dalam 1 akun atau seluruh 5 akun saat 1 tiket berhasil tembus verifikasi.",
        optionsDetail = "Default: Isolasi Ketat Per-Tab (hanya tab yang tembus yang berhenti 0ms). Opsional: Stop 1 Akun / Stop 5 Akun.",
        impactRamCpu = "Membebaskan CPU/RAM seketika jika mode Stop 1 Akun atau 5 Akun dipilih.",
        impactRiskSpeed = "Jika diubah ke stop multi-tab/akun, striker lain yang berburu tiket tambahan ikut berhenti.",
        defaultChecked = false,
      ),
    )
}
