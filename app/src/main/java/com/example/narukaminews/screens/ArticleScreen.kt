@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.narukaminews.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArticleScreen(
    source: String,
    category: String,
    navController: NavHostController,
    viewModel: NewsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val articles by viewModel.articles.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("Mới nhất") }
    var isGridView by remember { mutableStateOf(false) }

    // ✅ Auth listener realtime
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { currentUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(source, category) { viewModel.loadArticles(source, category) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "$source - $category",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadArticles(source, category) }) {
                        Icon(Icons.Filled.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0221))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0221)
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> LoadingState()
                error -> ErrorState(onRetry = { viewModel.loadArticles(source, category) })
                else -> ArticleListContent(
                    articles = articles,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    sortOption = sortOption,
                    onSortChange = { sortOption = it },
                    isGridView = isGridView,
                    onToggleLayout = { isGridView = !isGridView },
                    onArticleClick = { article ->
                        navController.navigate("detail?url=${article.link}&source=$source")
                    },
                    onBookmarkClick = { article ->
                        if (currentUser == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "🔒 Vui lòng đăng nhập để lưu bài viết",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        } else {
                            viewModel.toggleBookmark(article.link, true)
                            scope.launch {
                                snackbarHostState.showSnackbar("✅ Đã lưu vào mục Đọc sau")
                            }
                        }
                    }
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                          🔹 Article List Content                           */
/* -------------------------------------------------------------------------- */
@Composable
private fun ArticleListContent(
    articles: List<Article>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortOption: String,
    onSortChange: (String) -> Unit,
    isGridView: Boolean,
    onToggleLayout: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // 🔍 Thanh tìm kiếm
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Tìm kiếm bài viết...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFD54F),
                focusedLabelColor = Color(0xFFFFD54F),
                cursorColor = Color(0xFFFFD54F)
            ),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, null, tint = Color.LightGray)
                    }
                }
            }
        )

        // 🔽 Bộ lọc & sắp xếp
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C0F3A))
                ) {
                    Text(sortOption, color = Color(0xFFFFD54F))
                    Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFFFD54F))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Mới nhất", "Cũ nhất", "A–Z", "Z–A").forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = { onSortChange(it); expanded = false }
                        )
                    }
                }
            }

            IconButton(onClick = onToggleLayout) {
                Icon(
                    if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F)
                )
            }
        }

        // Áp dụng tìm kiếm và sắp xếp
        val filtered = remember(articles, searchQuery, sortOption) {
            var list = if (searchQuery.isBlank()) articles
            else articles.filter { it.title.contains(searchQuery, ignoreCase = true) }

            list = when (sortOption) {
                "Mới nhất" -> list
                "Cũ nhất" -> list.reversed()
                "A–Z" -> list.sortedBy { it.title }
                "Z–A" -> list.sortedByDescending { it.title }
                else -> list
            }
            list
        }

        if (filtered.isEmpty()) {
            EmptyState()
        } else {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(300))
            ) {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { article ->
                            ArticleCard(
                                article = article,
                                onClick = { onArticleClick(article) },
                                onBookmarkClick = { onBookmarkClick(article) }
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { article ->
                            ArticleCard(
                                article = article,
                                onClick = { onArticleClick(article) },
                                onBookmarkClick = { onBookmarkClick(article) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                             🔹 Article Card UI                             */
/* -------------------------------------------------------------------------- */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { isPressed = true }
            )
            .border(1.dp, Color(0xFF7B1FA2), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C0F3A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(article.image)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    article.title,
                    color = Color(0xFFFFD54F),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBookmarkClick) {
                        Icon(Icons.Filled.BookmarkAdd, null, tint = Color(0xFFFFD54F))
                    }
                    AnimatedVisibility(isPressed) {
                        Icon(Icons.Filled.Share, null, tint = Color(0xFFB39DDB))
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                              🔹 Loading & Error                            */
/* -------------------------------------------------------------------------- */
@Composable private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF7B1FA2))
    }
}

@Composable private fun ErrorState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CloudOff, null, tint = Color.Gray, modifier = Modifier.size(60.dp))
            Spacer(Modifier.height(8.dp))
            Text("Không thể tải dữ liệu", color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
            ) {
                Text("Thử lại", color = Color.White)
            }
        }
    }
}

@Composable private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Filled.Feed,
                null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(60.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text("Không có kết quả phù hợp ❌", color = Color.Gray)
        }
    }
}
