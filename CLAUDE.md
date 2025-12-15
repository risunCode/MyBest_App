# Zencoder Rules for MyBest UBSI Project

## Project Context
MyBest UBSI adalah aplikasi Android untuk sistem e-learning BSI yang menggunakan web scraping untuk mengakses data dari elearning.bsi.ac.id. Aplikasi ini dibangun dengan Kotlin, menggunakan Room Database, OkHttp untuk networking, dan Jsoup untuk HTML parsing.

---

## Critical Issues & Inconsistencies

### 🔴 **1. Security Vulnerabilities**

#### SSL Certificate Bypass (CRITICAL - MUST FIX)
**Location**: `ApiClient.kt:60-76`

```kotlin
// ❌ CURRENT: Trust all certificates
private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
})
```

**Issue**: Aplikasi menerima semua sertifikat SSL tanpa validasi. Ini membuka celah untuk Man-in-the-Middle (MITM) attacks.

**Recommended Fix**:
1. Gunakan certificate pinning untuk domain BSI
2. Hanya bypass SSL untuk development/debug build
3. Implementasikan proper SSL error handling

```kotlin
// ✅ RECOMMENDED
val certificatePinner = CertificatePinner.Builder()
    .add("elearning.bsi.ac.id", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

val client = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    // Only disable SSL verification in debug builds
    .apply {
        if (BuildConfig.DEBUG) {
            // Add custom trust manager for debug
        }
    }
    .build()
```

#### Plain Text Password Storage (CRITICAL)
**Location**: `PreferenceManager.kt:49-51`

```kotlin
// ❌ CURRENT: Password stored in plain text
var savedPassword: String
    get() = prefs.getString(KEY_SAVED_PASSWORD, "") ?: ""
    set(value) = prefs.edit().putString(KEY_SAVED_PASSWORD, value).apply()
```

**Issue**: Password disimpan dalam SharedPreferences tanpa enkripsi. Jika device di-root atau backup di-extract, password bisa dibaca.

**Recommended Fix**:
```kotlin
// ✅ RECOMMENDED: Use EncryptedSharedPreferences
implementation("androidx.security:security-crypto:1.1.0-alpha06")

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

### 🟠 **2. Architecture & Code Quality Issues**

#### No ViewModel Layer
**Location**: `LoginActivity.kt`, `MainActivity.kt`, `PresensiActivity.kt`, dll.

**Issue**: Activities langsung mengakses Repository dan melakukan business logic. Ini melanggar MVVM pattern dan membuat:
- Unit testing sangat sulit
- Code tidak reusable
- State management tidak konsisten
- Lifecycle management error-prone

**Recommended Fix**:
```kotlin
// ✅ Add ViewModel layer
class LoginViewModel(private val repository: AppRepository) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()
    
    fun login(nim: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = repository.performLogin(nim, password)
            _loginState.value = if (result.isSuccess) {
                LoginState.Success
            } else {
                LoginState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
```

#### GlobalScope Usage (DEPRECATED)
**Location**: `LoginActivity.kt:110`

```kotlin
// ❌ CURRENT: Using deprecated GlobalScope
kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
    repository.syncScheduleFromServer()
    repository.syncUserFromServer()
}
```

**Issue**: GlobalScope tidak terikat dengan lifecycle, bisa menyebabkan memory leaks dan crash.

**Recommended Fix**:
```kotlin
// ✅ Use lifecycleScope or viewModelScope
lifecycleScope.launch {
    repository.syncScheduleFromServer()
    repository.syncUserFromServer()
}

// Or better: Use WorkManager for background sync
val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
WorkManager.getInstance(context).enqueue(syncWorkRequest)
```

#### No Dependency Injection
**Location**: Semua Activity

**Issue**: Setiap Activity membuat instance Repository sendiri:
```kotlin
// ❌ CURRENT: Manual instantiation in every Activity
repository = AppRepository(AppDatabase.getDatabase(this))
```

**Recommended Fix**:
```kotlin
// ✅ Use Hilt/Dagger for DI
@HiltAndroidApp
class MyBestApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideAppRepository(database: AppDatabase): AppRepository {
        return AppRepository(database)
    }
}

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject lateinit var repository: AppRepository
    private val viewModel: LoginViewModel by viewModels()
}
```

---

### 🟡 **3. API & Networking Issues**

#### Mixed API Response Handling
**Location**: `ApiService.kt`

**Issue**: 
- Tidak ada standardized response model
- Error handling tidak konsisten
- Menggunakan `Result<T>` tapi tidak ada proper error types
- Mixing response parsing dengan networking logic

**Recommended Fix**:
```kotlin
// ✅ Create standard API response models
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val exception: Exception, val code: Int? = null) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}

// Create custom exception types
sealed class ApiException(message: String) : Exception(message) {
    class NetworkException(message: String) : ApiException(message)
    class ServerException(val code: Int, message: String) : ApiException(message)
    class ParseException(message: String) : ApiException(message)
    class AuthException(message: String) : ApiException(message)
}

// Separate concerns
class ApiService(private val client: OkHttpClient)
class HtmlParser // Keep separate
class ResponseMapper // New: Map HTML to domain models
```

#### No Request Retry Mechanism
**Location**: `ApiService.kt`

**Issue**: Network requests tidak di-retry saat gagal, padahal BSI server sering unstable.

**Recommended Fix**:
```kotlin
// ✅ Add retry interceptor
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response: Response? = null
        var exception: IOException? = null
        
        while (attempt < maxRetries) {
            try {
                response = chain.proceed(chain.request())
                if (response.isSuccessful) return response
            } catch (e: IOException) {
                exception = e
            }
            attempt++
            Thread.sleep(1000L * attempt) // Exponential backoff
        }
        
        throw exception ?: IOException("Max retries exceeded")
    }
}

val client = OkHttpClient.Builder()
    .addInterceptor(RetryInterceptor(maxRetries = 3))
    .build()
```

#### No Proper Caching Strategy
**Location**: `PresensiActivity.kt:124-126`

**Issue**: Manual caching di Activity level, tidak konsisten dan tidak reliable.

```kotlin
// ❌ CURRENT: Manual cache in Activity
private var cachedAttendanceRecords: List<AttendanceRecord>? = null
private var cachedEncryptedId: String = ""
```

**Recommended Fix**:
```kotlin
// ✅ Use Room Database as cache
@Entity(tableName = "attendance_cache")
data class AttendanceCache(
    @PrimaryKey val encryptedId: String,
    val records: String, // JSON string
    val timestamp: Long
)

// Repository pattern with cache-first strategy
suspend fun getAttendanceRecords(
    encryptedId: String,
    forceRefresh: Boolean = false
): Flow<Resource<List<AttendanceRecord>>> = flow {
    emit(Resource.Loading())
    
    // 1. Load from cache first
    if (!forceRefresh) {
        val cached = attendanceDao.getCache(encryptedId)
        if (cached != null && !cached.isExpired()) {
            emit(Resource.Success(cached.toRecords()))
        }
    }
    
    // 2. Fetch from network
    try {
        val records = apiService.getAttendanceRecords(encryptedId)
        attendanceDao.insertCache(records.toCache())
        emit(Resource.Success(records))
    } catch (e: Exception) {
        emit(Resource.Error(e.message ?: "Unknown error"))
    }
}.flowOn(Dispatchers.IO)
```

---

### 🟢 **4. User Experience Issues**

#### Inconsistent Error Handling UI
**Location**: Multiple Activities

**Issue**: Mix of Toast, Snackbar, dan Dialog untuk menampilkan error.

```kotlin
// ❌ CURRENT: Inconsistent
Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()  // LoginActivity
Snackbar.make(binding.root, "Error", Snackbar.LENGTH_LONG).show()  // MainActivity
AlertDialog.Builder(this).setMessage("Error").show()  // PresensiActivity
```

**Recommended Fix**:
```kotlin
// ✅ Create standardized error handling
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowDialog(val title: String, val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}

// In ViewModel
private val _eventFlow = MutableSharedFlow<UiEvent>()
val eventFlow = _eventFlow.asSharedFlow()

// In Activity/Fragment
lifecycleScope.launch {
    viewModel.eventFlow.collectLatest { event ->
        when (event) {
            is UiEvent.ShowSnackbar -> {
                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
            }
            // ...
        }
    }
}
```

#### No Proper Loading States
**Location**: Multiple Activities

**Issue**: Manual loading state management dengan button text changes.

```kotlin
// ❌ CURRENT
binding.btnLogin.isEnabled = !show
binding.btnLogin.text = if (show) getString(R.string.processing) else getString(R.string.login_button)
```

**Recommended Fix**:
```kotlin
// ✅ Use proper loading state with ViewModel
data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: Any? = null
)

// In Activity
viewModel.uiState.observe(this) { state ->
    binding.progressBar.isVisible = state.isLoading
    binding.btnLogin.isEnabled = !state.isLoading
    state.error?.let { showError(it) }
    state.data?.let { handleSuccess(it) }
}
```

---

## Development Guidelines

### Must Follow Rules

1. **NEVER bypass SSL certificate validation in production**
   - Only allow for debug builds
   - Use certificate pinning for production

2. **NEVER store passwords in plain text**
   - Use EncryptedSharedPreferences
   - Consider using Android Keystore for sensitive data

3. **ALWAYS use ViewModel for business logic**
   - Activities/Fragments should only handle UI
   - Use StateFlow/LiveData for reactive UI updates

4. **ALWAYS use Dependency Injection**
   - Use Hilt/Koin for DI
   - No manual instantiation of repositories/use cases

5. **ALWAYS use proper coroutine scopes**
   - viewModelScope for ViewModels
   - lifecycleScope for Activities/Fragments
   - NEVER use GlobalScope

6. **ALWAYS implement proper error handling**
   - Create custom exception types
   - Use sealed classes for UI states
   - Consistent error messaging

7. **ALWAYS implement offline-first architecture**
   - Cache data in Room Database
   - Show cached data while fetching fresh data
   - Handle network errors gracefully

8. **ALWAYS separate concerns**
   - Networking layer (ApiService)
   - Data layer (Repository)
   - Domain layer (Use Cases)
   - Presentation layer (ViewModel)
   - UI layer (Activity/Fragment)

---

## Testing Strategy

### Unit Tests
```kotlin
// Test ViewModels
@Test
fun `login success updates state correctly`() = runTest {
    val repository = mockk<AppRepository>()
    coEvery { repository.performLogin(any(), any()) } returns Result.success(Unit)
    
    val viewModel = LoginViewModel(repository)
    viewModel.login("12345", "password")
    
    assertEquals(LoginState.Success, viewModel.loginState.value)
}
```

### Integration Tests
```kotlin
// Test Repository with fake database
@Test
fun `syncSchedule updates database`() = runTest {
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository = AppRepository(database)
    
    repository.syncScheduleFromServer()
    
    val schedules = database.scheduleDao().getAllSchedules().first()
    assertTrue(schedules.isNotEmpty())
}
```

---

## Performance Optimization

### 1. Use Paging for Large Lists
```kotlin
// For attendance records, assignment lists, etc.
implementation("androidx.paging:paging-runtime:3.2.1")

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAttendanceRecordsPaged(): PagingSource<Int, AttendanceRecord>
}
```

### 2. Use Coil for Image Loading
```kotlin
// If app loads images in the future
implementation("io.coil-kt:coil:2.5.0")

imageView.load(imageUrl) {
    crossfade(true)
    placeholder(R.drawable.placeholder)
    error(R.drawable.error)
}
```

### 3. Use WorkManager for Background Sync
```kotlin
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val repository = getRepository()
            repository.syncScheduleFromServer()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Schedule periodic sync
val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "sync_schedule",
    ExistingPeriodicWorkPolicy.KEEP,
    syncWork
)
```

---

## Migration Plan (Priority Order)

### Phase 1: Critical Security Fixes
1. ✅ Implement EncryptedSharedPreferences for passwords
2. ✅ Add certificate pinning or proper SSL handling
3. ✅ Remove GlobalScope usage

### Phase 2: Architecture Improvements
4. ✅ Add Hilt dependency injection
5. ✅ Create ViewModel layer for all screens
6. ✅ Implement proper state management with StateFlow

### Phase 3: API & Networking
7. ✅ Add retry mechanism
8. ✅ Standardize API responses
9. ✅ Implement proper caching strategy

### Phase 4: Testing & Quality
10. ✅ Add unit tests for ViewModels
11. ✅ Add integration tests for Repository
12. ✅ Set up CI/CD pipeline

---

## Code Review Checklist

Before committing code, ensure:

- [ ] No SSL certificate bypass in production code
- [ ] No plain text password storage
- [ ] No GlobalScope usage
- [ ] No direct repository calls from Activities
- [ ] All coroutines use proper scopes
- [ ] Error handling is consistent
- [ ] Loading states are properly managed
- [ ] All strings are in resources (not hardcoded)
- [ ] No memory leaks (check with LeakCanary)
- [ ] Unit tests pass
- [ ] Code follows Kotlin coding conventions

---

## Build & Test Commands

```bash
# Run lint checks
./gradlew lint

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Build release APK
./gradlew assembleRelease

# Build debug APK
./gradlew assembleDebug
```

---

## Resources

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Kotlin Coroutines Best Practices](https://kotlinlang.org/docs/coroutines-guide.html)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

---

*Last Updated: 2025-12-11*
*Reviewed by: Zencoder AI Assistant*
