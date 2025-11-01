@file:Suppress("DEPRECATION", "UnusedImport", "RemoveRedundantQualifierName")
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.example.narukaminews

import com.example.narukaminews.screens.*
import com.example.narukaminews.admin.*
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.narukaminews.ui.theme.NarukamiNewsTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import androidx.room.*
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.narukaminews.data.RssSources
import com.example.narukaminews.data.Article
import com.example.narukaminews.ui.theme.ThemePreferences

// ----------------------------- DATASTORE -----------------------------
val Context.dataStore by preferencesDataStore("user_prefs")
val FAVORITE_SOURCES = stringSetPreferencesKey("favorite_sources")

suspend fun saveFavoriteSource(context: Context, source: String, add: Boolean) {
    context.dataStore.edit { prefs ->
        val current = prefs[FAVORITE_SOURCES] ?: emptySet()
        prefs[FAVORITE_SOURCES] = if (add) current + source else current - source
    }
}

fun getFavoriteSources(context: Context): Flow<Set<String>> =
    context.dataStore.data.map { it[FAVORITE_SOURCES] ?: emptySet() }

// ----------------------------- ROOM (OFFLINE CACHE) -----------------------------
@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val link: String,
    val title: String,
    val description: String,
    val image: String?,
    val source: String,
    val category: String,
    val isBookmarked: Boolean = false, // 🔖 Đọc sau
)
@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE source = :src AND category = :cat ORDER BY rowid DESC")
    suspend fun getArticles(src: String, cat: String): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: ArticleEntity)

    @Query("SELECT * FROM articles WHERE link = :link LIMIT 1")
    suspend fun getByLink(link: String): ArticleEntity?

    @Transaction
    suspend fun upsertPreserveBookmark(newArticle: ArticleEntity) {
        val existing = getByLink(newArticle.link)
        val merged = if (existing != null) {
            newArticle.copy(isBookmarked = existing.isBookmarked)
        } else newArticle
        insert(merged)
    }

    @Query("UPDATE articles SET isBookmarked = :state WHERE link = :link")
    suspend fun updateBookmark(link: String, state: Boolean)

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY rowid DESC")
    suspend fun getBookmarked(): List<ArticleEntity>

    @Query("DELETE FROM articles WHERE isBookmarked = 1")
    suspend fun clearAllBookmarks()
}

@Database(entities = [ArticleEntity::class], version = 3)
abstract class ArticleDatabase : RoomDatabase() {
    abstract fun dao(): ArticleDao
}

// ----------------------------- DATA MODEL -----------------------------
data class CachedEntry(val time: Long, val articles: List<Article>)
private val articleCache = mutableMapOf<String, CachedEntry>()

// ----------------------------- RSS LOADER -----------------------------
suspend fun loadRss(source: String, category: String): List<Article> = withContext(Dispatchers.IO) {
    val key = "$source-$category"
    val now = System.currentTimeMillis()
    val cached = articleCache[key]
    if (cached != null && now - cached.time < 10 * 60 * 1000) return@withContext cached.articles

    val rssUrl = RssSources[source]?.get(category) ?: return@withContext emptyList()
    val doc = Jsoup.connect(rssUrl).ignoreContentType(true).timeout(10_000).get()
    val result = doc.select("item").map {
        val title = it.selectFirst("title")?.text().orEmpty()
        val link = it.selectFirst("link")?.text().orEmpty()
        val desc = it.selectFirst("description")?.text()?.replace(Regex("<.*?>"), "") ?: ""
        val imgUrl = Regex("src=\"(.*?)\"").find(it.toString())?.groupValues?.get(1)
        Article(title, link, desc, imgUrl)
    }
    articleCache[key] = CachedEntry(now, result)
    result
}


// ----------------------------- VIEWMODEL -----------------------------
@HiltViewModel
class NewsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // ------------------- DATABASE -------------------
    val db by lazy {
        Room.databaseBuilder(appContext, ArticleDatabase::class.java, "articles.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    // ------------------- STATE FLOW -------------------
    private val _favoriteSources = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSources: StateFlow<Set<String>> = _favoriteSources

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    // 🔖 Danh sách bài đã lưu
    private val _bookmarked = MutableStateFlow<List<ArticleEntity>>(emptyList())
    val bookmarked: StateFlow<List<ArticleEntity>> = _bookmarked

    init {
        // ✅ Load nguồn yêu thích khi khởi tạo
        viewModelScope.launch {
            getFavoriteSources(appContext).collect { _favoriteSources.value = it }
        }
    }

    // ------------------- FAVORITE SOURCES -------------------
    fun toggleFavorite(source: String, add: Boolean) {
        viewModelScope.launch {
            saveFavoriteSource(appContext, source, add)
        }
    }

    // ------------------- RSS LOADING -------------------
    fun loadArticles(source: String, category: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = false

            try {
                val list = loadRss(source, category)
                _articles.value = list

                // ✅ Giữ bookmark cũ (upsertPreserveBookmark)
                list.forEach { article ->
                    db.dao().upsertPreserveBookmark(
                        ArticleEntity(
                            link = article.link,
                            title = article.title,
                            description = article.description,
                            image = article.image,
                            source = source,
                            category = category
                        )
                    )
                }
            } catch (_: Exception) {
                val cached = db.dao().getArticles(source, category)
                if (cached.isNotEmpty()) {
                    _articles.value = cached.map {
                        Article(it.title, it.link, it.description, it.image)
                    }
                } else {
                    _error.value = true
                }
            }

            _loading.value = false
        }
    }

    // ------------------- BOOKMARK (Đọc sau) -------------------
    fun toggleBookmark(link: String, add: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            db.dao().updateBookmark(link, add)
            loadBookmarks() // cập nhật lại UI nếu đang ở màn bookmarks
        }
    }

    fun loadBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = db.dao().getBookmarked()
            _bookmarked.value = list
        }
    }

    fun removeBookmark(link: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.dao().updateBookmark(link, false)
            loadBookmarks()
        }
    }

    // ✅ Xoá toàn bộ bookmark
    fun clearAllBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.dao().clearAllBookmarks()
                _bookmarked.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// ----------------------------- APP ENTRY -----------------------------
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NarukamiNewsApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 🌙 Đọc trạng thái dark mode và màu chủ đề từ DataStore
    val isDarkMode by ThemePreferences.isDarkMode(context).collectAsState(initial = true)
    val accentHex by ThemePreferences.getAccentColor(context).collectAsState(initial = "#7B1FA2")

    NarukamiNewsTheme(
        darkTheme = isDarkMode,
        dynamicColor = true,
        accentHex = accentHex
    ) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedNavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { fadeIn(tween(400)) },
                    exitTransition = { fadeOut(tween(300)) }
                ) {
                    // ---------------- Đăng nhập / Đăng ký ----------------
                    composable("login") {
                        DangNhapScreen(
                            onLogin = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = { navController.navigate("register") },
                            navController = navController,
                            snackbarHostState = snackbarHostState
                        )
                    }

                    composable("register") {
                        DangKyScreen(
                            navController = navController,
                            onBackToLogin = { navController.popBackStack() }
                        )
                    }

                    // ---------------- Trang chính ----------------
                    composable("home") {
                        HomeScreen(
                            navController = navController,
                            snackbarHostState = snackbarHostState
                        )
                    }

                    // ---------------- Các trang yêu cầu đăng nhập ----------------
                    val protectedRoutes = mapOf(
                        "favorites" to "📰 Nguồn yêu thích",
                        "bookmarks" to "🔖 Đọc sau",
                        "history" to "📚 Đã đọc gần đây",
                        "topics" to "🎯 Chủ đề quan tâm",
                        "profile" to "👤 Hồ sơ cá nhân"
                    )

                    protectedRoutes.forEach { (route, _) ->
                        composable(route) {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user == null) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = "🔒 Vui lòng đăng nhập để sử dụng tính năng này.",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            } else {
                                when (route) {
                                    "favorites" -> FavoritesScreen(navController)
                                    "bookmarks" -> BookmarksScreen(navController)
                                    "topics" -> TopicsScreen(navController)
                                    "profile" -> ProfileScreen(navController)
                                }
                            }
                        }
                    }

                    // ---------------- ADMIN PANEL ----------------
                    val adminRoutes = mapOf(
                        "admin_sources" to "📰 Quản lý nguồn RSS",
                        "admin_highlight" to "🔥 Quản lý Tin nổi bật",
                        "admin_users" to "👥 Quản lý người dùng",
                        "admin_stats" to "📊 Thống kê & Giám sát"
                    )

                    adminRoutes.forEach { (route, _) ->
                        composable(route) {
                            val user = FirebaseAuth.getInstance().currentUser
                            var role by remember { mutableStateOf("user") }
                            var hasCheckedRole by remember { mutableStateOf(false) }

                            LaunchedEffect(user) {
                                if (user != null) {
                                    val firestore = FirebaseFirestore.getInstance()
                                    try {
                                        val doc = firestore.collection("users")
                                            .document(user.uid).get().await()
                                        role = doc.getString("role") ?: "user"
                                    } catch (_: Exception) {
                                        role = "user"
                                    } finally {
                                        hasCheckedRole = true
                                    }
                                } else {
                                    hasCheckedRole = true
                                }
                            }

                            when {
                                !hasCheckedRole -> {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFFFFD54F))
                                    }
                                }

                                role.lowercase() == "admin" -> when (route) {
                                    "admin_sources" -> AdminSourcesScreen(navController)
                                    "admin_highlight" -> AdminHighlightScreen(navController)
                                    "admin_users" -> AdminUsersScreen(navController)
                                    "admin_stats" -> AdminStatsScreen(navController)
                                }

                                else -> {
                                    LaunchedEffect(Unit) {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                        snackbarHostState.showSnackbar(
                                            message = "⚠️ Bạn không có quyền truy cập trang quản trị.",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---------------- Công khai ----------------
                    composable("hotnews") { HotNewsScreen(navController) }
                    composable("about") { AboutScreen(navController) }

                    // ---------------- Danh sách bài viết ----------------
                    composable(
                        "articles/{source}/{category}",
                        arguments = listOf(
                            navArgument("source") { type = NavType.StringType },
                            navArgument("category") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val source = backStackEntry.arguments?.getString("source").orEmpty()
                        val category = backStackEntry.arguments?.getString("category").orEmpty()
                        ArticleScreen(
                            source = source,
                            category = category,
                            navController = navController,
                            snackbarHostState = snackbarHostState
                        )
                    }

                    // ---------------- Màn đọc chi tiết ----------------
                    composable(
                        "detail?url={url}&source={source}",
                        arguments = listOf(
                            navArgument("url") { type = NavType.StringType },
                            navArgument("source") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val url = backStackEntry.arguments?.getString("url").orEmpty()
                        val src = backStackEntry.arguments?.getString("source").orEmpty()
                        DetailScreen(url = url, source = src, navController = navController)
                    }
                }
            }
        }
    }
}

// ----------------------------- MAIN ACTIVITY -----------------------------
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NarukamiNewsApp() }
    }
}
