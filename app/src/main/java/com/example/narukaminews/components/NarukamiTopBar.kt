package com.example.narukaminews.components

import androidx.compose.foundation.layout.RowScope // ✅ cần import này
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarukamiTopBar(
    title: String,
    navController: NavHostController,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        // ✅ luôn truyền một composable; bên trong mới quyết định vẽ hay không
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color(0xFFFFD54F)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0D0221),
            titleContentColor = Color(0xFFFFD54F),
            navigationIconContentColor = Color(0xFFFFD54F)
        )
    )
}
