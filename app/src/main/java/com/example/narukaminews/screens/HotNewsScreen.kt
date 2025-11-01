package com.example.narukaminews.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.narukaminews.NewsViewModel
import com.example.narukaminews.data.Article
import kotlinx.coroutines.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotNewsScreen(navController: NavHostController, viewModel: NewsViewModel = hiltViewModel()) {
    var hotArticles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var loadedOnce by rememberSaveable { mutableStateOf(false) }

    // ⚡ Màu từ theme
    val accentColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    val gradient = Brush.verticalGradient(
        listOf(accentColor.copy(alpha = 0.25f), bgColor)
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ✅ Tải tin nóng chỉ một lần
    LaunchedEffect(Unit) {
        if (!loadedOnce) {
            loadedOnce = true
            try {
                loading = true
                error = false

                val sources = listOf("VNExpress", "Tuổi Trẻ", "Thanh Niên", "Dân Trí", "Zing News", "Vietnamnet")

                val categories = listOf("Tất cả bài báo", "Thời sự", "Thế giới", "Kinh doanh", "Giáo dục", "Công nghệ", "Sức khỏe", "Đời sống", "Thể thao", "Giải trí", "Pháp luật")


                val results = withContext(Dispatchers.IO) {
                    coroutineScope {
                        sources.flatMap { src ->
                            categories.map { cat ->
                                async {
                                    try {
                                        val fetched = com.example.narukaminews.loadRss(src, cat)
                                        fetched.map { it.copy(title = "[$src] ${it.title}") }
                                    } catch (e: Exception) {
                                        println("⚠️ Lỗi RSS: $src/$cat -> ${e.message}")
                                        emptyList()
                                    }
                                }
                            }
                        }.awaitAll().flatten()
                    }
                }

                hotArticles = results
                    .filter { !it.image.isNullOrEmpty() }
                    .distinctBy { it.link }
                    .shuffled(Random(System.currentTimeMillis()))
                    .take(60)

            } catch (e: Exception) {
                error = true
                println("🔥 Lỗi tải tin nóng: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("🔥 Tin nóng", color = accentColor, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Trang chủ",
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }

                error -> ErrorView(
                    accentColor = accentColor,
                    textColor = textColor,
                    onRetry = { loadedOnce = false }
                )

                hotArticles.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có tin nóng nào 🔥", color = textColor, fontSize = 16.sp)
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(hotArticles) { article ->
                        HotNewsItem(
                            article = article,
                            accentColor = accentColor,
                            textColor = textColor,
                            surfaceColor = surfaceColor,
                            onClick = {
                                navController.navigate(
                                    "detail?url=${Uri.encode(article.link)}&source=${Uri.encode(article.title)}"
                                )
                            },
                            onSave = {
                                viewModel.toggleBookmark(article.link, true)
                                scope.launch {
                                    snackbarHostState.showSnackbar("✅ Đã lưu vào Đọc sau")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorView(
    accentColor: Color,
    textColor: Color,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            null,
            tint = accentColor,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Không thể tải tin nóng!", color = textColor, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("Thử lại", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HotNewsItem(
    article: Article,
    accentColor: Color,
    textColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(article.image)
                    .crossfade(true)
                    .error(android.R.drawable.ic_menu_report_image)
                    .fallback(android.R.drawable.ic_menu_gallery)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    article.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    article.description.takeIf { it.isNotEmpty() } ?: "Không có mô tả",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onSave) {
                Icon(
                    Icons.Filled.BookmarkAdd,
                    contentDescription = "Lưu vào Đọc sau",
                    tint = accentColor
                )
            }
        }
    }
}
