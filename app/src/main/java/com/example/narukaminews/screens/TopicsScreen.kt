package com.example.narukaminews.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Star
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(navController: NavHostController, viewModel: NewsViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var topics by rememberSaveable { mutableStateOf(listOf<String>()) }
    var articles by rememberSaveable { mutableStateOf<List<Article>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    // 🎨 Màu từ MaterialTheme
    val accentColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    val gradient = Brush.verticalGradient(
        colors = listOf(accentColor.copy(alpha = 0.25f), bgColor)
    )

    // ✅ Tải dữ liệu
    LaunchedEffect(Unit) {
        try {
            loading = true
            error = false
            topics = listOf("Tất cả bài báo", "Thời sự", "Thế giới", "Kinh doanh", "Giáo dục", "Công nghệ", "Sức khỏe", "Đời sống", "Thể thao", "Giải trí", "Pháp luật")

            val fetched = withContext(Dispatchers.IO) {
                com.example.narukaminews.loadRss("VNExpress", "Thời sự")
            }
            articles = fetched.take(10)
        } catch (e: Exception) {
            error = true
            println("⚠️ Lỗi TopicsScreen: ${e.message}")
        } finally {
            loading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("🎯 Chủ đề quan tâm", color = accentColor, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Về trang chính",
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
                .padding(horizontal = 16.dp)
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }

                error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không thể tải dữ liệu chủ đề 😢", color = textColor)
                }

                else -> AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // --- Danh sách chủ đề ---
                        item {
                            Text(
                                "📚 Các chủ đề phổ biến",
                                color = accentColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(topics) { topic ->
                            TopicItem(topic = topic, accentColor = accentColor) {
                                navController.navigate("articles/VNExpress/$topic")
                            }
                        }

                        // --- Gợi ý bài viết ---
                        item {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "✨ Gợi ý cho bạn",
                                color = accentColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(articles) { article ->
                            SuggestedArticleItem(
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
                                        snackbarHostState.showSnackbar("✅ Đã lưu vào mục Đọc sau!")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicItem(topic: String, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Star, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Text(topic, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SuggestedArticleItem(
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
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
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
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    article.title,
                    fontSize = 16.sp,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
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
                    contentDescription = "Lưu vào đọc sau",
                    tint = accentColor
                )
            }
        }
    }
}
