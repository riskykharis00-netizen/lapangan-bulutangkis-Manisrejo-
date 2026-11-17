# Lapangan Bulutangkis Manisrejo

Aplikasi dan website pemesanan serta pengelolaan jadwal sewa **Lapangan Bulutangkis Manisrejo** (Senin – Minggu, pukul **08:00 – 00:00**, **Lapangan A & Lapangan B**, tarif **Rp 15.000/jam**).

Codingan website sudah dipecah rapi **per bagian file** dan dipisahkan antara halaman **Orang Pesan (Penyewa)** dan **Editor Website (Pengelola)** agar mudah di-upload ke GitHub dan dipublikasikan lewat **GitHub Pages**.

---

## Struktur File Per Bagian

### Bagian Website (Siap GitHub Pages)
```text
├── index.html          # [Bagian 1] Halaman HTML khusus Orang Pesan (Penyewa)
├── editor.html         # [Bagian 2] Halaman HTML khusus Editor Website (PIN default: 1234)
├── css/
│   └── styles.css      # [Bagian 3] Tampilan & layout website (Emerald & Lime)
└── js/
    ├── storage.js      # [Bagian 4] Penyimpanan data, format jam 08:00–00:00 & validasi bentrok
    ├── renter.js       # [Bagian 5] Logika jadwal, pesan 1/2 lapangan & riwayat penyewa
    └── editor.js       # [Bagian 6] Logika PIN, tagihan Lunas/DP, pengingat WA & editor lapangan
```

### Bagian Aplikasi Android & Konfigurasi Repository
```text
├── app/                # Source code Android (Kotlin + Jetpack Compose + Room)
├── gradle/             # Konfigurasi Gradle Version Catalog
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore          # Mengabaikan folder build, APK, dan file lokal sensitif
└── README.md           # Dokumentasi & tutorial upload GitHub
```

---

## Tutorial Lengkap Upload Codingan ke GitHub

### Cara 1: Upload Lewat Browser GitHub (Tanpa Terminal / Paling Mudah)
1. Download/Export proyek ini sebagai file **ZIP**, lalu **Extract** ke folder di komputer Anda.
2. Login ke akun **GitHub** Anda di browser, lalu klik tombol **`+`** di pojok kanan atas → pilih **New repository**.
3. Isi **Repository name** (contoh: `lapangan-bulutangkis-manisrejo`), pilih **Public**, lalu klik **Create repository**.
4. Di halaman repository baru, klik tulisan **uploading an existing file**.
5. **Upload file per bagian**:
   - Drag & drop file di root (`index.html`, `editor.html`, `README.md`, `.gitignore`) beserta folder `css/` dan `js/` (atau seluruh folder jika ingin sekaligus dengan `app/`).
   - Pastikan struktur folder `css/styles.css` dan `js/storage.js`, `js/renter.js`, `js/editor.js` tetap berada di dalam foldernya masing-masing.
6. Di bagian bawah, isi pesan commit (contoh: `Upload website Lapangan Bulutangkis Manisrejo`), lalu klik **Commit changes**.

---

### Cara 2: Upload Lewat Terminal / Git Bash
Buka terminal di dalam folder proyek ini lalu jalankan berurutan:

```bash
git init
git add .
git commit -m "Initial commit: Lapangan Bulutangkis Manisrejo"
git branch -M main
git remote add origin https://github.com/USERNAME_ANDA/lapangan-bulutangkis-manisrejo.git
git push -u origin main
```
*(Ganti `USERNAME_ANDA` dengan username GitHub Anda. Jika diminta login, gunakan Personal Access Token GitHub sebagai password).*

---

## Cara Mengaktifkan Website Online (GitHub Pages)

1. Buka repository Anda di GitHub → klik tab **Settings** → pilih menu **Pages** di sebelah kiri.
2. Pada bagian **Build and deployment**:
   - **Source**: `Deploy from a branch`
   - **Branch**: pilih **`main`** dan folder **`/ (root)`**
3. Klik **Save**, lalu tunggu 1–2 menit hingga tautan website muncul.
4. Website Anda siap diakses secara terpisah:
   - **Link Orang Pesan (Penyewa)**: `https://USERNAME_ANDA.github.io/lapangan-bulutangkis-manisrejo/`
   - **Link Editor Website (Pengelola)**: `https://USERNAME_ANDA.github.io/lapangan-bulutangkis-manisrejo/editor.html` (PIN bawaan: `1234`)

---

## Kontak & Lokasi

- **WhatsApp Admin**: `082142272367`
- **Jam Operasional**: Senin – Minggu, `08:00 – 00:00`
- **Google Maps**: [Lokasi Lapangan Bulutangkis Manisrejo](https://maps.app.goo.gl/xEdtA2gNiUx9pz9f9)
