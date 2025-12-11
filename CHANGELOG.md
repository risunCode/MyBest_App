# Changelog

All notable changes to the MyBest UBSI app will be documented in this file.

## [Planned] v1.0.0

### Package Refactoring
- ✅ Changed package name from `com.ubsi.mybest` to `com.risuncode.mybest`
- ✅ Updated namespace and applicationId in `build.gradle.kts`
- ✅ Updated all Kotlin files with new package imports
- ✅ Updated AndroidManifest.xml activity references

### Auto-Login Feature
- ✅ Added auto-login functionality with "Ingat Saya" checkbox
- ✅ Saves NIM and password when checked
- ✅ Auto-processes login on app restart if credentials exist
- ✅ Toast messages: "Auto Login, silahkan tunggu..." and "Login berhasil, selamat datang [NIM]"
- ✅ Logout disables auto-login but keeps saved credentials for autofill

### Dashboard
- ✅ Replaced statistics cards with **App Status** card (Login Status, Web Connected)
- ✅ Added dynamic "Kelas Hari Ini" section with compact schedule cards
- ✅ Added **SwipeRefreshLayout** for pull-to-refresh functionality
- ✅ Created `item_dashboard_schedule.xml` - compact card for dashboard
- ✅ Added **skeleton loading** with shimmer effect
- ✅ Added **random motivational quotes** (25 quotes - Indonesian & International)

### Jadwal Kuliah
- ✅ Fixed **double app bar** issue by removing fragment's custom app bar
- ✅ Redesigned stats cards: 2 compact side-by-side cards (Mata Kuliah + Total SKS)
- ✅ Enhanced stats labels with bold + primary color
- ✅ Added SwipeRefreshLayout for pull-to-refresh
- ✅ **Kuliah Mendatang** card now navigates to Presensi page on click
- ✅ Improved countdown format: "X hari Y jam", "X jam Y menit", etc.

### Schedule Card (`item_schedule_card.xml`)
- ✅ Complete redesign: colored header at top, compact 2-column info grid
- ✅ All info labels (Dosen, SKS, Kode, Ruang, etc.) now bold + primary color
- ✅ Smaller icons (14dp) and font sizes (11sp) for compact look
- ✅ Added new icons: `ic_group.xml`, `ic_link.xml`

### Presensi
- ✅ Simplified to only **Hadir** and **Tidak Hadir** (removed Izin/Alpha)
- ✅ Updated stats card to compact vertical layout with 3 rows:
  - Hadir count, Tidak Hadir count, Respon Server Terakhir
- ✅ Added **expandable attendance list** with Rangkuman and Berita Acara
  - Click row to expand/collapse
  - Text is selectable/copyable
- ✅ Added **server response dialog** when submitting attendance:
  - 200 OK → Success, hadir count +1
  - 419 Expired → Toast + redirect to login
  - 500/Other → Error dialog
- ✅ Added **holiday detection** based on schedule day format (lowercase = possible holiday)
- ✅ Added SwipeRefreshLayout wrapper

### Tugas Feature (NEW)
- ✅ Created **TugasListFragment** with assignment cards:
  - Pertemuan number, title, description, deadline
  - Status badge (Aktif/Berakhir)
  - "LIHAT TUGAS" button
- ✅ Created **detail modal** (BottomSheetDialog):
  - File info with PDF icon
  - LIHAT (open viewer) and UNDUH buttons
  - Google Drive link input
  - BATAL and KIRIM TUGAS buttons
- ✅ Created **PdfViewerActivity** (light theme):
  - App bar with title
  - White background for content
  - "File berhasil diunduh" notification card

### Profil
- ✅ Made layout more compact:
  - Avatar: 80dp → 56dp
  - Padding: 20dp → 16dp
  - Icons: 32dp → 24dp
  - Button height: 56dp → 48dp

### Sidebar Navigation
- ✅ Added **active state highlight** for current page
- ✅ Created `drawer_item_background.xml` - background selector
- ✅ Created `drawer_icon_tint.xml` - icon color selector
- ✅ Created `drawer_text_color.xml` - text color selector

### Code Quality Improvements
- ✅ Moved all hardcoded colors to `colors.xml`
- ✅ Replaced deprecated `resources.getColor()` with `ContextCompat.getColor()`
- ✅ Moved all hardcoded strings to `strings.xml`
- ✅ Removed unused imports
- ✅ Fixed skeleton loading - only shows on first load, not when returning from other fragments
- ✅ Added `ACCESS_NETWORK_STATE` permission for network connectivity check
- ✅ Removed duplicate `processing` string resource

### UI Animations
- ✅ Created **Theme.MyBestUBSI.Animated** with smooth activity transitions
- ✅ Added slide animations: `slide_in_right`, `slide_out_left`, `slide_in_left`, `slide_out_right`
- ✅ Added `slide_up`, `slide_down` for bottom sheets
- ✅ Added `scale_up` animation for dialogs
- ✅ Smooth **expand/collapse** animation for attendance list (fade + rotate)
- ✅ Applied animated theme to Presensi, Tugas, and PdfViewer activities

### Release Preparation
- ✅ **Forced light theme only** - `android:forceDarkAllowed="false"`
- ✅ No dark mode override from system settings
- ✅ Created **GitHub Actions workflow** (`.github/workflows/android-build.yml`):
  - Manual trigger with `workflow_dispatch`
  - Choose between **debug** or **release** build
  - Input version name (e.g., 1.0.0)
  - Gradle caching for faster builds
  - Uploads APK as artifact

### Dependencies
- ✅ Added `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
- ✅ Added `com.facebook.shimmer:shimmer:0.5.0`

### New Resources
- `ic_wifi.xml` - WiFi icon for web status
- `ic_calendar.xml` - Calendar icon for holiday status
- `ic_expand_more.xml` - Expand arrow icon
- `ic_visibility.xml` - Eye icon for view action
- `ic_download.xml` - Download icon
- `ic_send.xml` - Send icon
- `ic_pdf.xml` - PDF file icon
- `item_tugas_card.xml` - Tugas card layout
- `dialog_tugas_detail.xml` - Tugas detail modal
- `activity_pdf_viewer.xml` - PDF viewer layout
- `item_attendance_expandable.xml` - Expandable attendance item
- `skeleton_dashboard.xml` - Skeleton loading layout

### Strings Added
- Auto-login: `processing`, `processing_login`, `auto_login_wait`, `login_success_welcome`
- App status: `app_status`, `login_status`, `web_connected`, `is_today_holiday`
- Presensi: `status_hadir`, `status_tidak_hadir`, `last_server_response`, `response_200_ok`, `response_error`
- Tugas: `lihat_tugas`, `tugas_summary`, `pertemuan_n`, `deadline_format`, `kirim_tugas`
- 25 motivational quotes in string-array

---

## Initial Structure

- Basic app structure with Dashboard, Jadwal, Profil
- Guest mode login
- Schedule display with presensi tracking
- Notification system
- About page
