package com.example.narukaminews.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.narukaminews.NewsViewModel
import com.example.narukaminews.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FavoritesScreen(
    navController: NavHostController,
    viewModel: NewsViewModel = hiltViewModel()
) {
    val favoriteSources by viewModel.favoriteSources.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmRemove by remember { mutableStateOf<String?>(null) }

    // 🌈 Lấy màu từ theme
    val colorScheme = MaterialTheme.colorScheme
    val accent = colorScheme.primary
    val textColor = colorScheme.onBackground
    val bg = colorScheme.background
    val surface = colorScheme.surface

    val gradient = Brush.verticalGradient(
        listOf(accent.copy(alpha = 0.15f), bg)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "💜 Nguồn yêu thích",
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Trở lại",
                            tint = accent
                        )
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
            AnimatedContent(
                targetState = favoriteSources.isEmpty(),
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "favorites-content"
            ) { empty ->
                if (empty) {
                    EmptyFavoritesView(
                        accentColor = accent,
                        textColor = textColor
                    ) { navController.navigate("home") }
                } else {
                    FavoritesGridView(
                        favoriteSources = favoriteSources,
                        accentColor = accent,
                        onOpen = { name -> navController.navigate("articles/$name/Thời sự") },
                        onRemove = { name -> confirmRemove = name }
                    )
                }
            }

            // 🔸 Hộp thoại xác nhận xoá
            confirmRemove?.let { source ->
                AlertDialog(
                    onDismissRequest = { confirmRemove = null },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmRemove = null
                            viewModel.toggleFavorite(source, false)
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Đã xoá $source khỏi yêu thích")
                            }
                        }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmRemove = null }) {
                            Text("Huỷ", color = textColor)
                        }
                    },
                    title = { Text("Xoá nguồn yêu thích", color = accent) },
                    text = { Text("Bạn có chắc muốn xoá $source khỏi danh sách yêu thích?", color = textColor) },
                    containerColor = surface
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 🔹 Empty View (Không có yêu thích)                                        */
/* -------------------------------------------------------------------------- */
@Composable
private fun EmptyFavoritesView(
    accentColor: Color,
    textColor: Color,
    onBackHome: () -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Bạn chưa chọn nguồn yêu thích nào.\nHãy thêm vài đầu báo nhé!",
            color = textColor,
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBackHome,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("⬅️ Quay lại trang chính", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 🔹 Grid View (hiển thị danh sách yêu thích)                               */
/* -------------------------------------------------------------------------- */
@Composable
private fun FavoritesGridView(
    favoriteSources: Set<String>,
    accentColor: Color,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val allSources = listOf(
        Triple("VNExpress", R.drawable.vnexpress, Color(0xFFB2EBF2)),
        Triple("Tuổi Trẻ", R.drawable.tuoitre, Color(0xFFFFF9C4)),
        Triple("Thanh Niên", R.drawable.thanhnien, Color(0xFFD1C4E9)),
        Triple("Zing News", R.drawable.zingnews, Color(0xFFFFCCBC)),
        Triple("Dân Trí", R.drawable.dantri, Color(0xFFB3E5FC)),
        Triple("Vietnamnet", R.drawable.vietnamnet, Color(0xFFFFECB3))
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "✨ Các nguồn yêu thích của bạn:",
            color = accentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val filtered = allSources.filter { it.first in favoriteSources }
            items(filtered) { (name, logo, color) ->
                FavoriteCard(name, logo, color, accentColor, onOpen, onRemove)
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 🔹 Card từng nguồn yêu thích                                              */
/* -------------------------------------------------------------------------- */
@Composable
private fun FavoriteCard(
    name: String,
    logo: Int,
    baseColor: Color,
    accentColor: Color,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clickable { onOpen(name) },
        colors = CardDefaults.cardColors(containerColor = baseColor.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = logo),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )

            Text(
                name,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 8.dp),
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { onRemove(name) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(30.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Xoá khỏi yêu thích",
                    tint = accentColor
                )
            }
        }
    }
}
