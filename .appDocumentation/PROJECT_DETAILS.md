# MyBest UBSI - Project Details

Dokumentasi lengkap proyek untuk AI context.

---

## 1. Project Overview

Aplikasi Android tidak resmi untuk mahasiswa UBSI. Akses jadwal kuliah, presensi, tugas, dan notifikasi.

### Tech Stack

| Item | Value |
|------|-------|
| Package | `com.risuncode.mybest` |
| Language | Kotlin 1.9.x |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |
| UI | Material Design 3 + ViewBinding |
| Database | Room 2.6.1 |
| Theme | Navy Blue `#2C3E50` |

### Features

| Feature | Status |
|---------|--------|
| Login NIM + Auto Login | ✅ |
| Guest Mode (dummy data) | ✅ |
| Dashboard + Jadwal Hari Ini | ✅ |
| Jadwal Kuliah + Stats | ✅ |
| Presensi (Hadir/Tidak Hadir) | ✅ |
| Tugas + PDF Viewer | ✅ |
| Profil | ✅ |
| SwipeRefresh + Shimmer | ✅ |
| API Integration | 🚧 Planned |
| Push Notifications | 🚧 Planned |

### Project Structure

```
com.risuncode.mybest/
├── data/           # Room DB, DAOs, Entities, Repository
├── ui/             # Activities & Fragments
│   ├── setup/      # SetupActivity
│   ├── login/      # LoginActivity
│   ├── main/       # MainActivity (Bottom Nav + Drawer)
│   ├── dashboard/  # DashboardFragment
│   ├── jadwal/     # JadwalFragment
│   ├── profil/     # ProfilFragment
│   ├── presensi/   # PresensiActivity
│   ├── tugas/      # TugasActivity, PdfViewerActivity
│   └── notification/
├── util/           # PreferenceManager, StringUtils
├── AboutActivity.kt
├── AutoLoginActivity.kt
├── NotificationSettingsActivity.kt
└── ReplacementClassActivity.kt
```

### Navigation Flow

```
SetupActivity → LoginActivity → MainActivity
                                    ├── DashboardFragment
                                    ├── JadwalFragment → PresensiActivity → TugasActivity
                                    │                 └── TugasActivity → PdfViewerActivity
                                    └── ProfilFragment
```

---

## 2. Architecture

```
UI Layer (Activity/Fragment)
       ↓
Repository Layer (AppRepository)
       ↓
   ┌───┴───┐
Room DB   PreferenceManager
```

### Room Database

**Entities:**
- `UserEntity` - NIM, name, email, prodi, isGuest
- `ScheduleEntity` - subject, code, dosen, day, time, room, SKS, attendance
- `NotificationEntity` - title, message, type, read status

**DAOs:** `UserDao`, `ScheduleDao`, `NotificationDao`

### PreferenceManager Properties

```kotlin
// Auth
authToken, csrfToken, refreshToken, tokenExpiry

// User State
isLoggedIn, isGuestMode, userName, userEmail
savedNim, savedPassword, rememberMe

// Settings
notifScheduleEnabled, notifGradeEnabled
alarmEnabled, alarmMinutes
lastSyncTime, isSetupCompleted
```

### Repository Methods

```kotlin
// User
getCurrentUser(): Flow<UserEntity?>
insertUser(user), deleteAllUsers()

// Schedule
getAllSchedules(): Flow<List<ScheduleEntity>>
getSchedulesByDay(day), getUpcomingSchedule()
getTotalSks(), updateAttendance(id, attended, time)

// Notification
getAllNotifications(): Flow<List<NotificationEntity>>
getUnreadCount(), markAsRead(id), markAllAsRead()

// Bulk
clearAllData()
```

### Guest Mode

```kotlin
// Initialize
prefManager.isGuestMode = true
DataInitializer.initializeGuestData(context)

// Clear on logout
DataInitializer.clearGuestData(context)
repository.clearAllData()
```

### Utils

```kotlin
StringUtils.getInitials("John Doe")  // "JD"
StringUtils.formatNim("12345678")    // "NIM: 12345678"
```

---

## 3. UI Design System

### Colors

| Name | Hex | Usage |
|------|-----|-------|
| `primary` | `#2C3E50` | Navy - buttons, appbar, text |
| `primary_dark` | `#1A252F` | Status bar |
| `primary_light` | `#3D566E` | Highlights |
| `background` | `#F5F6FA` | Main background, input bg |
| `text_white` | `#FFFFFF` | Text on navy |
| `text_secondary` | `#7F8C8D` | Labels, hints |
| `divider` | `#E1E5EB` | Borders |

**Status Colors:**
- Success: `#27AE60` (green)
- Error: `#E74C3C` (red)
- Warning: `#F39C12` (orange)
- Info: `#3498DB` (blue)

**Schedule Card Status:**
- Active (now): `#68D391` green
- Ended: `#CBD5E0` gray
- Not started: `#FC8181` red

### Spacing & Radius

| Element | Spacing | Radius |
|---------|---------|--------|
| Screen padding | 16-20dp | - |
| Card | 8dp margin, 16-24dp padding | 16-20dp |
| Buttons | - | 12dp |
| Dialogs | - | 24dp |
| Input fields | - | 12dp |
| AppBar bottom | - | 20dp |

### App Bar Pattern

```xml
<View
    android:id="@+id/appBarBackground"
    android:layout_height="72dp"
    android:background="@drawable/bg_app_bar" />

<ImageView
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:padding="12dp"
    app:tint="@color/text_white" />

<TextView
    android:textColor="@color/text_white"
    android:textSize="20sp"
    android:textStyle="bold" />
```

### MaterialButton Fix

```xml
<MaterialButton
    android:layout_height="56dp"
    android:insetTop="0dp"
    android:insetBottom="0dp" />
```

### Common Cards

- **Info Card (Blue):** `#E3F2FD` background
- **Warning Card (Red):** `#FFEBEE` background

---

## 4. Coding Conventions

### File Naming

| Type | Pattern | Example |
|------|---------|---------|
| Activity | `NameActivity.kt` | `LoginActivity.kt` |
| Fragment | `NameFragment.kt` | `DashboardFragment.kt` |
| Layout (Activity) | `activity_name.xml` | `activity_login.xml` |
| Layout (Fragment) | `fragment_name.xml` | `fragment_dashboard.xml` |
| Layout (Item) | `item_name.xml` | `item_schedule_card.xml` |
| Drawable | `ic_name.xml`, `bg_type.xml` | `ic_back.xml` |

### Resource IDs

| Type | Prefix | Example |
|------|--------|---------|
| Button | `btn` | `btnLogin` |
| TextView | `tv` | `tvTitle` |
| EditText | `et` | `etNim` |
| ImageView | `iv` | `ivBack` |
| CardView | `card` | `cardStatus` |
| RecyclerView | `rv` | `rvSchedule` |

### Best Practices

✅ **DO:**
- Use ViewBinding (never `findViewById`)
- Use `@string/` for all text
- Use ConstraintLayout for complex UIs
- Use safe calls `?.` or elvis `?:`
- Use `ContextCompat.getColor()`
- Add `insetTop="0dp"` and `insetBottom="0dp"` for MaterialButton
- Keep Navy theme consistent

❌ **DON'T:**
- Nested ScrollViews
- Hardcoded strings/colors
- Mix dp/px units
- Use `!!` force unwrap
- Use Material3 styles (stick to MaterialComponents)

### Drawer/BottomNav Sync

```kotlin
// Page items: highlight both
return true

// Action items: no highlight
return false
```

### Fragment Loading

```kotlin
private fun loadFragment(fragment: Fragment) {
    supportFragmentManager.beginTransaction()
        .replace(R.id.fragmentContainer, fragment)
        .commit()
}
```

### Animation Standards

- Duration: 300ms
- Interpolator: DecelerateInterpolator
- Fade: 200-300ms
- Use TransitionManager for constraint changes
- Use ValueAnimator for height changes
