package com.example.narukaminews.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0221),  // Đen tím sâu
                        Color(0xFF1A0846),  // Tím đậm
                        Color(0xFF311B92)   // Tím sáng Raiden
                    )
                )
            )
    ) {
        content()
    }
}
