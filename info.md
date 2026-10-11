# DOKUMENTASI ARSITEKTUR & SISTEM TERPADU P2KD KALISALAK 2026

## Aplikasi Native Android (E-Coklit Petugas) • Backend Next.js API • PostgreSQL Database

Dokumentasi ini adalah acuan resmi sistem bagi pengembang (AI & Developer) untuk menjaga keselarasan penuh antara **Frontend Android**, **Backend API**, dan **Basis Data**.

---

## 1. IKHTISAR SISTEM (SYSTEM OVERVIEW)

Sistem Aplikasi Petugas P2KD Kalisalak dirancang khusus untuk memfasilitasi Panitia Pemilihan Kepala Desa (P2KD) dan Petugas Pemutakhiran Data Pemilih (Pantarlih/Koordinator RW) dalam proses **Coklit Faktual**, **Pemetaan Rumah Berbasis QR-Code Stiker C6**, **Verifikasi Hak Pilih**, serta **Pelaporan Terpadu Lapangan**.

- **Entitas Wilayah**: Desa Kalisalak, Kecamatan Kebasen, Kabupaten Banyumas/Tegal.
- **Struktur Wilayah Resmi**:
  - **13 RW** (RW 01 s/d RW 13)
  - **39 RT** (Masing-masing RW tepat memiliki 3 RT: **RT 01, RT 02, dan RT 03**)
  - *Catatan Penting*: Tidak ada RT 04 atau RT 05 di Desa Kalisalak.
- **Tabung Pemilihan (TPS)**: 13 TPS yang memetakan 1-to-1 dengan 13 RW.

---

## 2. ARSITEKTUR FRONTEND (APLIKASI ANDROID NATIVE)

- **Repositori**: `D:\build.apk_p2kd`
- **Stack**: Kotlin, Jetpack Compose, Material 3, Coroutines, Flow.
- **Min SDK / Target SDK**: 24 (Android 7.0) / 35 (Android 15).
- **Tema Desain**: *Royal Sapphire & Emerald Vibrant* (Sapphire Blue `#1E3A8A` / `#2563EB`, Emerald Green `#10B981`, Soft Pearl Slate `#F8FAFC`).

### 2.1. Manajemen Izin Perangkat (Mandatory Permissions)

Aplikasi mewajibkan izin-izin berikut saat pertama kali dijalankan (`PermissionGuardWrapper`):

1. **Kamera (`CAMERA`)**:
   - Digunakan untuk pemindai barcode / QR Stiker C6 Coklit secara realtime menggunakan CameraX & ZXing.
   - Digunakan untuk pengambilan foto profil dan dokumentasi fisik lapangan.

2. **Lokasi Presisi Tinggi (`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`)**:
   - Menggunakan GPS Satelit & `FusedLocationProviderClient` dengan prioritas `PRIORITY_HIGH_ACCURACY`.
   - **Tujuan Kritis**: Merekam titik koordinat (`latitude` & `longitude`) akurat saat petugas memindai dan menempelkan QR-Code Stiker C6 di rumah warga.

3. **Data File & Media (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`)**:
   - Untuk mengunggah foto profil, membaca berkas bukti pendukung, dan ekspor cache data pemilih.

4. **Notifikasi Sistem (`POST_NOTIFICATIONS`)**:
   - Untuk menerima siaran pengumuman internal, instruksi koordinator, dan sinyal pembaruan otomatis rilis APK baru.

### 2.2. Otentikasi & Keamanan Multi-Lapis (Triple-Lock System)

Aplikasi menerapkan 3 metode login / pembukaan kunci:

1. **Sidik Jari (Biometrik)**: Sensor otomatis menyala saat layar kunci muncul (`BiometricPrompt`).
2. **6-Digit PIN**: Akses instan cepat menggunakan keypad numerik terenkripsi SHA-256.
3. **Kata Sandi Akun**: Kredensial akun utama (wajib digunakan jika petugas belum mengatur PIN/Biometrik, atau sebagai opsi pemulihan saat lupa PIN).

### 2.3. Siklus Kunci Latar Belakang (Background App Lock)

- **Zero-Bypass Policy**: Setiap kali aplikasi diminimize, berganti aplikasi, atau ditutup dari task manager, aplikasi **WAJIB terkunci otomatis**.
- **Kondisi Buka Kunci**:
  - Jika PIN atau Biometrik sudah diatur: Tampil layar **Quick Unlock** (PIN + auto Sensor Sidik Jari) + opsi *Masuk dengan Kata Sandi*.
  - Jika BELUM diatur: Wajib memasukkan **Kata Sandi Akun**.
- **Pop-up Edukasi Keamanan di Beranda**:
  - Petugas yang belum menyetel PIN/Sidik Jari akan disambut pop-up edukasi keamanan di Beranda dengan tombol cepat `[🔒 Atur PIN Sekarang]`.

### 2.4. Arsitektur Offline-First & Sinkronisasi

- Seluruh data pemilih dan antrean coklit disimpan secara lokal di SQLite terenkripsi.
- Pekerjaan coklit di daerah tanpa sinyal internet otomatis masuk ke antrean `OfflineQueueManager` dan tersinkronisasi otomatis saat terhubung kembali ke jaringan.

---

## 3. ARSITEKTUR BACKEND (NEXT.JS 16 APP ROUTER API)

- **Repositori**: `d:\.DAFTARPEMILIH`
- **Stack**: Next.js 16, TypeScript, Node.js, PG (node-postgres), JWT Auth, Web Crypto API.
- **Base Production URL**: `https://p2kdkalisalak.my.id`

### 3.1. Rute API Aplikasi Android (`/api/app/*`)

1. **Otentikasi & Akun**:
   - `POST /api/app/auth/login`: Otentikasi kredensial (NIK / Username & Password).
   - `GET /api/app/auth/me`: Mendapatkan profil aktif petugas.
   - `POST /api/app/auth/forgot-pin`: Mengirim tautan reset PIN langsung ke DM Telegram pribadi petugas (@pantarlih_bot).
   - `POST /api/app/auth/forgot-password`: Mengirim tautan reset Password ke DM Telegram pribadi.
   - `GET /api/app/auth/telegram-status`: Cek status penautan akun Telegram petugas.
   - `POST /api/app/auth/telegram-link`: Menautkan ID Chat Telegram pribadi petugas.
   - `POST /api/app/auth/telegram-unlink`: Memutus tautan Telegram secara mandiri.

2. **Coklit Rumah & QR-Code Stiker C6**:
   - `GET /api/app/qr/lookup?token={token}`: Validasi status token QR-Code stiker.
   - `POST /api/app/rumah`: Mendaftarkan rumah baru dengan payload lengkap:
     - `qrToken`, `alamat`, `rt`, `rw`, `nomorRumah`, `keteranganLokasi`, **`koordinatLat`**, **`koordinatLng`**.
   - `POST /api/app/rumah/{id}/kk`: Menautkan nomor KK & data keluarga ke rumah ber-QR.
   - `POST /api/app/rumah/{id}/kunjungan`: Mencatat log kunjungan lapangan & status coklit.

3. **Pusat Notifikasi & In-App Banner**:
   - `GET /api/app/broadcast-banner`: Mengambil banner poster pengumuman aktif beranda (hanya dari `notifikasi_petugas` internal, terisolasi 100% dari berita publik).
   - `GET /api/app/notifications`: Riwayat pesan dan notifikasi internal untuk petugas.

4. **Pembaruan Aplikasi (Auto-Update)**:
   - `GET /api/app/version`: Menyediakan metadata rilis resmi, download URL APK, dan changelog.

---

## 4. ARSITEKTUR DATABASE (POSTGRESQL MULTI-POOL)

Sistem menggunakan PostgreSQL dengan arsitektur pool terpisah untuk performa dan keamanan tinggi:

### 4.1. Tabel-Tabel Kritis

1. **`anggota_p2kd`**:
   - Menyimpan seluruh profil panitia dan petugas lapangan.
   - Kolom Integrasi Telegram Pribadi:
     - `telegram_chat_id BIGINT`: ID Telegram personal petugas (DM).
     - `telegram_username VARCHAR(100)`: Username Telegram petugas.
     - `telegram_linked_at TIMESTAMP WITH TIME ZONE`: Waktu penautan akun.

2. **`rumah`**:
   - Menyimpan identitas fisik rumah yang telah ditempeli stiker QR C6.
   - Kolom Spasial & Geografis:
     - `koordinat_lat NUMERIC / DOUBLE PRECISION`: Titik Latitude presisi GPS.
     - `koordinat_lng NUMERIC / DOUBLE PRECISION`: Titik Longitude presisi GPS.
     - `qr_token VARCHAR(120)`: Token stiker C6 yang tertempel pada rumah.
     - `rt VARCHAR(10)`, `rw VARCHAR(10)`, `alamat TEXT`.

3. **`notifikasi_petugas`** (Database Seksi 1):
   - `is_banner BOOLEAN DEFAULT FALSE`: Menandai apakah pengumuman tampil sebagai poster in-app di Beranda.
   - `image_url TEXT`: URL gambar poster/pamflet pengumuman internal.
   - `target_role VARCHAR(50)`: Target pembaca (SEMUA, PANTARLIH, KOORDINATOR).

4. **`pemilih_kalisalak` / `pemilih`**:
   - Master data 7.787 pemilih yang tersebar di 13 RW dan 39 RT.

---

## 5. STANDAR KODE & INTEGRASI MASA DEPAN

1. **Konsistensi Wilayah**: Jangan pernah mengizinkan pembuatan data dengan opsi RT di luar RT 01, RT 02, dan RT 03.
2. **Kerahasiaan Data**: NIK dan data sensitif pemilih wajib dilindungi enkripsi AES-256 dan hanya dapat diakses melalui otentikasi resmi.
3. **Koordinat Realtime Wajib**: Setiap registrasi atau pembaruan lokasi rumah melalui aplikasi Android wajib menyertakan koordinat GPS terkini.
