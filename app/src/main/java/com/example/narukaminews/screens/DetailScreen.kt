@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.narukaminews.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.narukaminews.NewsViewModel
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DetailScreen(url: String, source: String, navController: NavHostController, viewModel: NewsViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF0D0221), Color(0xFF200B48), Color(0xFF0D0221))
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Đang đọc báo", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    // ⭐ Nút Lưu bài
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.toggleBookmark(url, true)
                            snackbarHostState.showSnackbar("⭐ Đã lưu bài viết vào mục Đọc sau")
                        }
                    }) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = "Lưu bài", tint = Color(0xFFFFD54F))
                    }

                    // 📤 Nút Chia sẻ
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, source)
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(intent, "Chia sẻ bài viết qua..."))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Chia sẻ", tint = Color(0xFFFFD54F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0221))
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            Box(Modifier.weight(1f)) {
                AndroidView(
                    factory = {
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ) = false

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }
                            }

                            // ✅ Kiểm tra URL hợp lệ trước khi load
                            try {
                                loadUrl(url)
                            } catch (_: Exception) {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("⚠️ Không thể tải trang này.")
                                }
                            }
                        }
                    },
                    update = { it.loadUrl(url) },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xAA0D0221)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD54F))
                    }
                }
            }

            Text(
                text = "Nguồn: $source",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0221))
                    .padding(8.dp),
                color = Color(0xFFFFD54F),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}
