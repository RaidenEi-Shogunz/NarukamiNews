package com.example.narukaminews.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.narukaminews.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    // 🌈 Lấy màu từ theme
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val textColor = scheme.onBackground
    val bg = scheme.background
    val surface = scheme.surface

    val gradient = Brush.verticalGradient(
        colors = listOf(accent.copy(alpha = 0.25f), bg)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ℹ️ Giới thiệu & Chính sách",
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
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
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- Logo ---
                    Image(
                        painter = painterResource(id = R.drawable.raiden),
                        contentDescription = "NarukamiNews Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(30.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Text(
                        "NarukamiNews ⚡",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )

                    // --- Card Giới thiệu ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AboutItem(
                                icon = Icons.Filled.Info,
                                title = "Về ứng dụng",
                                content = """
                                    NarukamiNews là ứng dụng đọc báo thông minh, hiện đại, mang phong cách Raiden Shogun ⚡.
                                    
                                    Ứng dụng thu thập tin tức từ RSS công khai của các tờ báo Việt Nam: 
                                    VNExpress, Tuổi Trẻ, Thanh Niên, Zing News, Dân Trí, Vietnamnet...
                                """.trimIndent(),
                                accent = accent,
                                textColor = textColor
                            )

                            HorizontalDivider(color = accent.copy(alpha = 0.2f))

                            AboutItem(
                                icon = Icons.Filled.Gavel,
                                title = "Chính sách & Bản quyền",
                                content = """
                                    ⚖️ NarukamiNews tuyệt đối tôn trọng bản quyền.
                                    
                                    Mọi bài viết đều hiển thị nguyên gốc từ nguồn chính thức, có tiêu đề, mô tả, hình ảnh và liên kết về trang gốc.
                                    
                                    ❌ Không chỉnh sửa, không sao chép, không chèn quảng cáo.
                                """.trimIndent(),
                                accent = accent,
                                textColor = textColor
                            )

                            HorizontalDivider(color = accent.copy(alpha = 0.2f))

                            AboutItem(
                                icon = Icons.Filled.Public,
                                title = "Nguồn dữ liệu",
                                content = """
                                    Các nguồn RSS được công khai hợp pháp, cung cấp bởi các trang báo điện tử Việt Nam.
                                    
                                    Ứng dụng chỉ đóng vai trò tổng hợp và hiển thị tin tức cho người dùng.
                                """.trimIndent(),
                                accent = accent,
                                textColor = textColor
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Phiên bản 2.5 (Compose Stable)\n© 2025 NarukamiNews Team ⚡",
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { /* TODO: mở email liên hệ */ }) {
                        Icon(Icons.Filled.Email, null, tint = accent)
                        Spacer(Modifier.width(6.dp))
                        Text("Liên hệ: support@narukami.app", color = accent)
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun AboutItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    accent: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )
        Column {
            Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                content,
                color = textColor.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
