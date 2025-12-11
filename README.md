# MyBest UBSI

Aplikasi Android tidak resmi untuk mahasiswa Universitas Bina Sarana Informatika (UBSI). Aplikasi ini mempermudah akses informasi akademik seperti jadwal kuliah, presensi, tugas, dan nilai melalui antarmuka yang modern dan mudah digunakan.

> **Disclaimer:** Ini adalah aplikasi pihak ketiga yang tidak berafiliasi dengan UBSI. Gunakan dengan risiko sendiri.

---

## Fitur

| Fitur | Status | Keterangan |
|-------|--------|------------|
| Login dengan NIM | Tersedia | Mendukung captcha otomatis |
| Guest Mode | Tersedia | Untuk mencoba aplikasi tanpa login |
| Dashboard | Tersedia | Statistik dan jadwal hari ini |
| Jadwal Kuliah | Tersedia | Daftar mata kuliah per hari |
| Presensi | Dalam pengembangan | Absensi otomatis |
| Notifikasi | Dalam pengembangan | Pengingat jadwal kuliah |
| Kuliah Pengganti | Dalam pengembangan | Jadwal pengganti |

---

## Screenshot

*Screenshot akan ditambahkan setelah build pertama.*

---

## Teknologi

| Komponen | Teknologi |
|----------|-----------|
| Bahasa | Kotlin 1.9.x |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |
| UI | Material Design 3 |
| Database | Room 2.6.1 |
| Architecture | MVVM + Repository Pattern |

---

## Struktur Proyek

```
app/src/main/
├── java/com/risuncode/mybest/
│   ├── data/
│   │   ├── dao/              # Data Access Objects
│   │   ├── entity/           # Database entities
│   │   ├── repository/       # Repository pattern
│   │   ├── AppDatabase.kt
│   │   ├── DataInitializer.kt
│   │   └── DummyDataGenerator.kt
│   ├── ui/
│   │   ├── dashboard/        # Fragment dashboard
│   │   ├── jadwal/           # Fragment jadwal
│   │   ├── login/            # Activity login
│   │   ├── main/             # Activity utama
│   │   ├── notification/     # Activity notifikasi
│   │   ├── presensi/         # Activity presensi
│   │   ├── profil/           # Fragment profil
│   │   ├── setup/            # Activity setup awal
│   │   └── tugas/            # Activity tugas & PDF viewer
│   ├── util/
│   │   ├── PreferenceManager.kt
│   │   └── StringUtils.kt
│   ├── service/              # Background services
│   ├── AboutActivity.kt
│   ├── AutoLoginActivity.kt
│   ├── NotificationSettingsActivity.kt
│   └── ReplacementClassActivity.kt
└── res/
    ├── layout/               # XML layouts
    ├── values/               # Resources
    ├── drawable/             # Icons dan graphics
    └── anim/                 # Animations
```

---

## Instalasi

### Prasyarat
- Android Studio Arctic Fox atau lebih baru
- JDK 11
- Android SDK 34

### Langkah
1. Clone repository
   ```bash
   git clone https://github.com/risunCode/MyBest-UBSI.git
   ```

2. Buka proyek di Android Studio

3. Sync Gradle dependencies

4. Build dan jalankan di device atau emulator (min API 24)

---

## Konfigurasi

### Theme
Aplikasi menggunakan tema Navy Blue dengan warna primer `#2C3E50`. Konfigurasi warna ada di `res/values/colors.xml`.

### Permissions
Aplikasi membutuhkan izin berikut:
- `POST_NOTIFICATIONS` - Untuk notifikasi jadwal
- `INTERNET` - Untuk komunikasi dengan server BSI
- `SCHEDULE_EXACT_ALARM` - Untuk alarm pengingat

---

## API

Aplikasi berkomunikasi dengan server BSI E-Learning di `https://elearning.bsi.ac.id`. Dokumentasi lengkap API tersedia di `.kiro/docs/API_DOCUMENTATION.md`.

### Endpoint Utama
| Endpoint | Fungsi |
|----------|--------|
| `/login` | Autentikasi dengan NIM dan password |
| `/sch` | Jadwal kuliah |
| `/absen-mhs/{id}` | Presensi mata kuliah |
| `/tugas/{id}` | Daftar tugas |
| `/profil` | Profil mahasiswa |

---

## Arsitektur

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│  (Activity, Fragment, ViewBinding)                      │
└───────────────────────────┬─────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    Repository Layer                     │
│  (AppRepository - Single Source of Truth)               │
└───────────────────────────┬─────────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
┌─────────────────────────┐   ┌───────────────────────────┐
│      Room Database      │   │    PreferenceManager      │
│  (User, Schedule, Notif)│   │   (Settings, Auth Token)  │
└─────────────────────────┘   └───────────────────────────┘
```

---

## Dokumentasi

| Dokumen | Lokasi | Deskripsi |
|---------|--------|-----------|
| API Documentation | `.kiro/docs/API_DOCUMENTATION.md` | Dokumentasi lengkap API BSI |
| App Design | `.kiro/docs/APP_DESIGN.md` | Panduan desain UI/UX |
| Development Rules | `.zencoder/rules/mybest-ubsi.md` | Aturan dan konvensi pengembangan |

---

## Kontribusi

Kontribusi sangat diterima. Silakan:

1. Fork repository ini
2. Buat branch fitur (`git checkout -b fitur/NamaFitur`)
3. Commit perubahan (`git commit -m 'Tambah fitur X'`)
4. Push ke branch (`git push origin fitur/NamaFitur`)
5. Buat Pull Request

### Panduan Kontribusi
- Gunakan ViewBinding, bukan findViewById
- Semua string harus di `strings.xml`
- Ikuti konvensi penamaan di `.appDocumentation/PROJECT_DETAILS.md`
- Tulis komentar dalam Bahasa Indonesia atau Inggris

---

## Roadmap

### Versi 1.0.0 (Saat ini)
- [x] Login dan autentikasi dengan auto-login
- [x] Guest mode dengan dummy data
- [x] Dashboard dengan App Status & Jadwal Hari Ini
- [x] Jadwal kuliah dengan stats & countdown
- [x] Presensi dengan expandable records
- [x] Tugas dengan PDF viewer
- [x] Profil mahasiswa
- [x] SwipeRefresh & Shimmer loading
- [x] Sidebar dengan active state highlight

### Versi 1.1.0 (Direncanakan)
- [ ] API Integration (real data dari BSI)
- [ ] Presensi otomatis
- [ ] Notifikasi push
- [ ] Alarm pengingat

---

## Lisensi

Proyek ini dilisensikan di bawah Apache License 2.0. Lihat file `LICENSE` untuk detail.

---

## Kontak

- GitHub: [@risunCode](https://github.com/risunCode)
- Repository: [MyBest-UBSI](https://github.com/risunCode/MyBest-UBSI)

---

## Acknowledgements

- [Material Design](https://material.io/) - Design system
- [Room Database](https://developer.android.com/training/data-storage/room) - Local database
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Async programming
