package com.example.narukaminews.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHighlightScreen(navController: NavHostController) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var isAdmin by remember { mutableStateOf(false) }
    var hasCheckedRole by remember { mutableStateOf(false) }
    var highlights by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var newHighlight by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF120231), Color(0xFF200B48), Color(0xFF120231))
    )

    // ✅ Kiểm tra quyền admin thật (email + role)
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val doc = firestore.collection("users").document(currentUser.uid).get().await()
                val role = doc.getString("role") ?: "user"
                isAdmin = role.equals("admin", ignoreCase = true) ||
                        currentUser.email == "admin@narukami.app"
            } catch (_: Exception) {
                isAdmin = false
            }
        }
        hasCheckedRole = true
    }

    // 🔥 Lắng nghe Firestore realtime với limit để tránh load nặng
    DisposableEffect(currentUser?.uid) {
        val listener = firestore.collection("highlights")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                highlights = snapshot?.documents?.map {
                    it.id to (it.getString("title") ?: "Không rõ tiêu đề")
                } ?: emptyList()
                loading = false
            }
        onDispose { listener.remove() }
    }

    // ⏳ Đang kiểm tra quyền
    if (!hasCheckedRole) {
        Box(Modifier.fillMaxSize().background(gradient), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFFD54F))
        }
        return
    }

    // 🚫 Không có quyền
    if (!isAdmin) {
        Box(
            Modifier.fillMaxSize().background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🚫 Bạn không có quyền truy cập trang này.",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                ) {
                    Text("Quay lại", color = Color(0xFF0D0221))
                }
            }
        }
        return
    }

    // ⚡ Giao diện chính
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🔥 Quản lý Tin nổi bật",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại Trang chính",
                            tint = Color(0xFFFFD54F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0221))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (newHighlight.isBlank() || isAdding) {
                        scope.launch {
                            snackbarHostState.showSnackbar("⚠️ Vui lòng nhập tên mục nổi bật!")
                        }
                    } else {
                        scope.launch(Dispatchers.IO) {
                            isAdding = true
                            try {
                                firestore.collection("highlights").add(
                                    mapOf(
                                        "title" to newHighlight.trim(),
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                ).await()
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar("✅ Đã thêm: $newHighlight")
                                    newHighlight = ""
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar("❌ Lỗi khi thêm: ${e.localizedMessage}")
                                }
                            } finally {
                                isAdding = false
                            }
                        }
                    }
                },
                containerColor = Color(0xFFFFD54F)
            ) {
                if (isAdding)
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF0D0221),
                        strokeWidth = 3.dp
                    )
                else
                    Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color(0xFF0D0221))
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "Danh sách mục nổi bật",
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = newHighlight,
                    onValueChange = { newHighlight = it },
                    label = { Text("Nhập tên mục nổi bật mới") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        focusedLabelColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(20.dp))

                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFFD54F))
                        }
                    }
                    highlights.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa có tin nổi bật nào 🔥", color = Color.White)
                        }
                    }
                    else -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(highlights) { item ->
                                HighlightCard(
                                    title = item.second,
                                    onDelete = { confirmDelete = item }
                                )
                            }
                        }
                    }
                }
            }

            // 🗑️ Dialog xác nhận xoá
            confirmDelete?.let { item ->
                AlertDialog(
                    onDismissRequest = { confirmDelete = null },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDelete = null
                            scope.launch(Dispatchers.IO) {
                                try {
                                    firestore.collection("highlights")
                                        .document(item.first).delete().await()
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar("🗑️ Đã xoá: ${item.second}")
                                    }
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar("❌ Lỗi khi xoá")
                                    }
                                }
                            }
                        }) { Text("Xoá", color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = null }) {
                            Text("Huỷ", color = Color.White)
                        }
                    },
                    title = { Text("Xác nhận xoá") },
                    text = { Text("Bạn có chắc muốn xoá mục: ${item.second}?") },
                    containerColor = Color(0xFF1A0846),
                    titleContentColor = Color(0xFFFFD54F),
                    textContentColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun HighlightCard(title: String, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0846)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(title, color = Color.White, fontSize = 17.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Xoá", tint = Color.Red)
            }
        }
    }
}
