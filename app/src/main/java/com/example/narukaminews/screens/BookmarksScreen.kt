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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.narukaminews.NewsViewModel
import com.example.narukaminews.ArticleEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    navController: NavHostController,
    viewModel: NewsViewModel = hiltViewModel()
) {
    val bookmarks by viewModel.bookmarked.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var confirmDeleteAll by remember { mutableStateOf(false) }
    var confirmSingle by remember { mutableStateOf<ArticleEntity?>(null) }

    // 🌈 Lấy màu từ theme
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val textColor = scheme.onBackground
    val bg = scheme.background
    val surface = scheme.surface

    val gradient = Brush.verticalGradient(
        listOf(accent.copy(alpha = 0.2f), bg)
    )

    LaunchedEffect(Unit) { viewModel.loadBookmarks() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("🔖 Đọc sau", color = accent, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Trở lại",
                            tint = accent
                        )
                    }
                },
                actions = {
                    if (bookmarks.isNotEmpty()) {
                        IconButton(onClick = { confirmDeleteAll = true }) {
                            Icon(Icons.Filled.DeleteSweep, null, tint = accent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface)
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
            if (bookmarks.isEmpty()) {
                EmptyBookmarkView(navController, accent, textColor)
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bookmarks) { article ->
                        BookmarkItem(
                            article = article,
                            accent = accent,
                            textColor = textColor,
                            onClick = {
                                val encoded = Uri.encode(article.link)
                                navController.navigate("detail?url=$encoded&source=${article.source}")
                            },
                            onDelete = { confirmSingle = article }
                        )
                    }
                }
            }
        }

        // 🧾 Xác nhận xoá 1 bài
        confirmSingle?.let { article ->
            AlertDialog(
                onDismissRequest = { confirmSingle = null },
                confirmButton = {
                    TextButton(onClick = {
                        confirmSingle = null
                        viewModel.removeBookmark(article.link)
                        scope.launch {
                            snackbarHostState.showSnackbar("🗑️ Đã xoá: ${article.title}")
                        }
                    }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmSingle = null }) {
                        Text("Huỷ", color = textColor)
                    }
                },
                title = { Text("Xác nhận xoá", color = accent) },
                text = {
                    Text(
                        "Bạn muốn xoá “${article.title.take(60)}”?",
                        color = textColor,
                        fontSize = 14.sp
                    )
                },
                containerColor = surface
            )
        }

        // 🧹 Xác nhận xoá tất cả
        if (confirmDeleteAll) {
            AlertDialog(
                onDismissRequest = { confirmDeleteAll = false },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDeleteAll = false
                        scope.launch {
                            viewModel.clearAllBookmarks()
                            snackbarHostState.showSnackbar("🧹 Đã xoá toàn bộ danh sách đọc sau.")
                        }
                    }) { Text("Xoá hết", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteAll = false }) {
                        Text("Huỷ", color = textColor)
                    }
                },
                title = { Text("Xác nhận xoá toàn bộ", color = accent) },
                text = {
                    Text(
                        "Tất cả các bài viết lưu sẽ bị xoá. Bạn có chắc không?",
                        color = textColor
                    )
                },
                containerColor = surface
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 🔹 Empty View                                                              */
/* -------------------------------------------------------------------------- */
@Composable
fun EmptyBookmarkView(navController: NavHostController, accent: Color, textColor: Color) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Bookmark, null, tint = accent, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Danh sách bài viết bạn lưu sẽ hiển thị tại đây.",
            color = textColor,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate("home") },
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text(
                "⬅️ Quay lại trang chính",
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 🔹 Bookmark Item                                                           */
/* -------------------------------------------------------------------------- */
@Composable
fun BookmarkItem(
    article: ArticleEntity,
    accent: Color,
    textColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = article.image,
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
                    color = accent,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Nguồn: ${article.source}",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = accent)
            }
        }
    }
}
