# P2KD Kalisalak - Aplikasi Android Native Coklit Lapangan

[![Build Native Android APK](https://github.com/pkd-develzy/build.apk_p2kd/actions/workflows/build-apk.yml/badge.svg)](https://github.com/pkd-develzy/build.apk_p2kd/actions/workflows/build-apk.yml)

Aplikasi Android Native resmi untuk Petugas Pemutakhiran Data Pemilih (Pantarlih / Petugas Lapangan) Pemilihan Kepala Desa Kalisalak.

### 🏛️ Prinsip Arsitektur Utama
**1 QR CODE = 1 RUMAH = BANYAK KK = BANYAK ANGGOTA KELUARGA**
- QR Code fisik ditempel di rumah sebagai identitas permanen fisik rumah.
- Nama penghuni pada stiker fisik ditulis manual oleh petugas menggunakan pulpen saat Coklit lapangan.
- Pemindaian QR menggunakan kamera belakang native (**CameraX**) tanpa ketergantungan browser web.

---

## 🚀 Cara Mengunduh APK Hasil Build (Cloud CI/CD)

Proses kompilasi APK dijalankan 100% di server cloud GitHub Actions. Pengembang dan panitia tidak perlu menginstal Android Studio atau SDK di komputer lokal.

### Langkah Unduh:
1. Buka tab **[Actions](https://github.com/pkd-develzy/build.apk_p2kd/actions)** pada repositori ini.
2. Klik workflow run terbaru yang bertanda centang hijau (**Success**).
3. Scroll ke bagian bawah halaman ke tabel **Artifacts**.
4. Klik **p2kd-native-apk-debug** untuk mengunduh arsip zip yang berisi file pp-debug.apk dan berkas checksum SHA-256.
5. Ekstrak file dan pasang pp-debug.apk pada perangkat Android.

---

## 🛠️ Stack Teknologi
- **Bahasa**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose (Material 3)
- **Kamera & Scan**: AndroidX CameraX + Google ML Kit Barcode Scanning
- **HTTP Client**: Retrofit 2 + OkHttp 3 + Kotlinx Serialization
- **Keamanan**: Android Keystore + Encrypted SharedPreferences
- **Mode Offline**: Local SQLite / File Queue Manager dengan idempotency token
