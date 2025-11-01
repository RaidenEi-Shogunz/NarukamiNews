package com.example.narukaminews.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.narukaminews.components.GradientBackground
import com.example.narukaminews.components.NarukamiTopBar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AdminStatsScreen(navController: NavHostController) {
    val firestore = remember { FirebaseFirestore.getInstance() }

    var totalUsers by remember { mutableStateOf(0) }
    var totalArticles by remember { mutableStateOf(0) }
    var activeUsers by remember { mutableStateOf(0) }
    var totalHighlights by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var lastUpdated by remember { mutableStateOf(System.currentTimeMillis()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 🕒 Clock cập nhật mỗi giây để hiển thị "vừa xong" realtime
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    // ✅ Lắng nghe Firestore realtime
    DisposableEffect(Unit) {
        var listenerCount = 0

        val userListener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorText = "⚠️ Lỗi khi tải người dùng: ${error.localizedMessage}"
                    return@addSnapshotListener
                }
                snapshot?.let {
                    totalUsers = it.size()
                    activeUsers = it.documents.count { doc -> doc.getBoolean("isOnline") == true }
                    lastUpdated = System.currentTimeMillis()
                    listenerCount++
                    if (listenerCount >= 3) isLoading = false
                }
            }

        val articleListener = firestore.collection("articles")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    totalArticles = snapshot.size()
                    lastUpdated = System.currentTimeMillis()
                    listenerCount++
                    if (listenerCount >= 3) isLoading = false
                }
            }

        val highlightListener = firestore.collection("highlights")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    totalHighlights = snapshot.size()
                    lastUpdated = System.currentTimeMillis()
                    listenerCount++
                    if (listenerCount >= 3) isLoading = false
                }
            }

        onDispose {
            userListener.remove()
            articleListener.remove()
            highlightListener.remove()
        }
    }

    GradientBackground {
        Scaffold(
            topBar = { NarukamiTopBar("📊 Thống kê & Giám sát", navController) },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    errorText != null -> {
                        Text(
                            text = errorText ?: "Đã xảy ra lỗi!",
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    else -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                "Tổng quan hệ thống",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )

                            // 🔹 Thẻ thống kê động
                            AnimatedContent(
                                targetState = totalUsers,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "users"
                            ) {
                                StatCard("👥 Tổng người dùng", "$it tài khoản", Color(0xFF9575CD))
                            }

                            AnimatedContent(
                                targetState = activeUsers,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "active"
                            ) {
                                StatCard("💡 Đang hoạt động", "$it trực tuyến", Color(0xFF64B5F6))
                            }

                            AnimatedContent(
                                targetState = totalArticles,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "articles"
                            ) {
                                StatCard("📰 Tổng bài viết (cached)", "$it bài", Color(0xFF7986CB))
                            }

                            AnimatedContent(
                                targetState = totalHighlights,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "highlights"
                            ) {
                                StatCard("🔥 Tin nổi bật", "$it mục", Color(0xFFFFA726))
                            }

                            Spacer(Modifier.weight(1f))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Analytics,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Cập nhật realtime — ${formatTimeAgo(lastUpdated, currentTime)}",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔹 Hiển thị thời gian cập nhật gần nhất
@Composable
fun formatTimeAgo(lastUpdate: Long, now: Long): String {
    val diff = now - lastUpdate
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = sdf.format(Date(lastUpdate))

    return when {
        hours >= 1 -> "$hours giờ trước ($timeStr)"
        minutes >= 1 -> "$minutes phút trước"
        else -> "Vừa xong"
    }
}

// 🔹 Thẻ thống kê có style Narukami ⚡
@Composable
fun StatCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = CardDefaults.outlinedCardBorder(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                title,
                color = Color(0xFFFFD54F),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
