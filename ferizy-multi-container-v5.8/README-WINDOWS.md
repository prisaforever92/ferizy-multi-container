# Browser Khusus Multi-Container Ferizy — V5.8 (Fase 1–4)

Proyek Electron desktop Windows (x64) hasil generate **FerizyForge** sesuai Master Blueprint **Fase 1–4**.

## Spesifikasi Target PC Acuan
- **CPU**: Intel Core i7-4790 (4C/8T)
- **RAM**: 16 GB (kapasitas optimal 5 Akun × 3 Tab = 15 Tab)
- **Storage**: 256 GB SSD

## Fitur Aktif (Fase 1–4)
1. **Multi-Container (`persist:akun_1..5`) + Hard Cap 3/3 + Pre-Flight Check 1 Layar + Close-to-Tray + `powerSaveBlocker`**.
2. **Auto-Grant Geolocation `0ms` + Multi-Proxy + WebRTC Anti-Leak + Pembersihan `Electron/`**.
3. **Mesin `sendInputEvent` (`isTrusted: true`) + Isolasi Menang Per-Tab `0ms` + Asset Blocker (Selektif (CSS & CAPTCHA Aman)) + Watchdog 350MB + Staggered Queue 400ms Reaktif**.
4. **0ms API Sniffer + Main Process Event Bus (`<1ms`) + Server Clock Sync + Scheduled Shot Timer**.
5. **Notifikasi Terpusat Main Process (`ntfy.sh`: `ferizy_Transsamudra05` + Windows Toast + Audio Alarm + Deduplikasi 10s)**:
   - **Lintasan Dibuka**: Aktif **HANYA** saat konfigurasi berjalan di `https://trip.ferizy.com/`.
   - **Kuota Muncul**: Aktif **HANYA** saat konfigurasi berjalan di `https://trip.ferizy.com/ferryschedule`.
   - **Tembus Verifikasi**, **Logout (/login atau 401)**, **Proxy Putus**, dan **CAPTCHA** aktif otomatis.
6. **Ceklist Modul Opsional (Default Rekomendasi)**:
   - `[x]` Gentle Session Keep-Alive (~12 menit bergiliran, tanpa reload halaman)
   - `[x]` Detektor CAPTCHA + Auto-Pop dari Tray
   - `[x]` Smart Target Matcher (Jam Prioritas: `08:00,10:00,12:00,14:00,20:00`)
   - `[ ]` Latency Compensation + Fast Request Timeout (`4500 ms`)
   - `[ ]` Opsi Stop Lanjutan (`Isolasi Ketat Per-Tab (Default)`)
7. **Fase 4 — Uji Ketahanan 24 Jam & Log Rotation (SSD 256 GB Aman)**:
   - **Log Rotation**: Maksimal **50 MB** per file, retensi otomatis **7 hari** (`50 MB / 7 Hari (Rekomendasi)`).
   - **Health Report JSON**: Snapshot ketahanan 15 tab setiap `60` menit (`health-report-24h.json`).
   - **Kalibrasi Watchdog Standby**: `350 MB` (Reaktif Opsi A — tab aktif/menang tidak pernah ter-reset).

## Cara Menjalankan, Simulasi 24 Jam & Build `.exe` di Windows
```bat
npm install
npm run test:endurance
npm start
# Build installer & portable .exe:
npm run build:win
```
