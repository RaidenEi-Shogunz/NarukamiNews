package com.example.narukaminews.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import com.example.narukaminews.data.RssSources
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Semaphore

// 🔹 Model dữ liệu RSS
data class RssSource(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val isActive: Boolean = false,
    val fromFirestore: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSourcesScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var isAdmin by remember { mutableStateOf(false) }
    var hasCheckedRole by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var rssSources by remember { mutableStateOf(listOf<RssSource>()) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf<RssSource?>(null) }

    // ✅ Giới hạn số lượng kết nối song song
    val semaphore = remember { Semaphore(8) }

    // ✅ Kiểm tra quyền admin (Firestore + email)
    LaunchedEffect(user) {
        if (user != null) {
            try {
                val doc = firestore.collection("users").document(user.uid).get().await()
                val role = doc.getString("role") ?: "user"
                isAdmin = role.equals("admin", ignoreCase = true) || user.email == "admin@narukami.app"
            } catch (_: Exception) {
                isAdmin = false
            }
        }
        hasCheckedRole = true
    }

    // ✅ Kiểm tra RSS có hoạt động không
    suspend fun checkRssUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        semaphore.acquire()
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            val code = connection.responseCode
            connection.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        } finally {
            semaphore.release()
        }
    }

    // ✅ Nạp toàn bộ nguồn RSS theo batch (tránh overload)
    fun reloadAllSources() {
        scope.launch {
            isLoading = true
            val codeSources = mutableListOf<RssSource>()
            val firestoreSources = mutableListOf<RssSource>()

            // 🟣 Đọc từ code (RssSources.kt)
            withContext(Dispatchers.IO) {
                val batchSize = 10
                val list = RssSources.toList()
                for (batch in list.chunked(batchSize)) {
                    val checked = batch.mapNotNull { (name, categories) ->
                        val firstUrl = categories.values.firstOrNull() ?: return@mapNotNull null
                        val active = checkRssUrl(firstUrl)
                        RssSource(name = name, url = firstUrl, isActive = active, fromFirestore = false)
                    }
                    codeSources.addAll(checked)
                }
            }

            // 🟡 Đọc từ Firestore (song song + kiểm tra kết nối)
            try {
                val snapshot = firestore.collection("rss_sources").get().await()
                val temp = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    val url = doc.getString("url")
                    if (name != null && url != null)
                        RssSource(id = doc.id, name = name, url = url, fromFirestore = true)
                    else null
                }

                val checked = coroutineScope {
                    temp.map { src ->
                        async {
                            val active = checkRssUrl(src.url)
                            src.copy(isActive = active)
                        }
                    }.awaitAll()
                }
                firestoreSources.addAll(checked)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("⚠️ Lỗi tải Firestore: ${e.localizedMessage}")
            }

            rssSources = (codeSources + firestoreSources)
                .sortedWith(compareByDescending<RssSource> { it.isActive }.thenBy { it.name })
            isLoading = false
        }
    }

    // 🚀 Load lần đầu
    LaunchedEffect(key1 = firestore.hashCode()) { reloadAllSources() }

    // 🧭 Kiểm tra quyền
    if (!hasCheckedRole) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFFD54F))
        }
        return
    }

    if (!isAdmin) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚫 Bạn không có quyền truy cập trang này.", color = Color.Red, fontWeight = FontWeight.Bold)
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
    GradientBackground {
        Scaffold(
            topBar = { NarukamiTopBar("📰 Quản lý nguồn RSS", navController) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { reloadAllSources() },
                    containerColor = Color(0xFFFFD54F),
                    contentColor = Color(0xFF0D0221)
                ) {
                    if (isLoading)
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF0D0221),
                            strokeWidth = 3.dp
                        )
                    else
                        Icon(Icons.Filled.Refresh, contentDescription = "Làm mới")
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    "Nguồn RSS trong hệ thống",
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Spacer(Modifier.height(12.dp))

                // 🔹 Form thêm nguồn mới
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tên nguồn mới (Firestore)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        focusedLabelColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text("URL RSS mới") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        focusedLabelColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (newName.isBlank() || newUrl.isBlank()) {
                                snackbarHostState.showSnackbar("⚠️ Nhập đủ tên và URL!")
                                return@launch
                            }
                            isLoading = true
                            val valid = withContext(Dispatchers.IO) { checkRssUrl(newUrl) }
                            isLoading = false
                            if (!valid) {
                                snackbarHostState.showSnackbar("❌ URL không phản hồi hoặc không hợp lệ!")
                                return@launch
                            }
                            try {
                                firestore.collection("rss_sources")
                                    .add(mapOf("name" to newName.trim(), "url" to newUrl.trim()))
                                    .await()
                                snackbarHostState.showSnackbar("✅ Đã thêm: $newName")
                                newName = ""
                                newUrl = ""
                                reloadAllSources()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Lỗi khi thêm: ${e.localizedMessage}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF0D0221))
                    Spacer(Modifier.width(8.dp))
                    Text("Thêm nguồn RSS mới", color = Color(0xFF0D0221))
                }

                Spacer(Modifier.height(16.dp))

                // 🔹 Danh sách tổng hợp (code + firestore)
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFFD54F))
                        }
                    }
                    rssSources.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Không có nguồn RSS nào 📰", color = Color.White)
                        }
                    }
                    else -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rssSources) { src ->
                                RssSourceItem(source = src, onDelete = { showConfirmDialog = src })
                            }
                        }
                    }
                }

                // ⚠️ Dialog xác nhận xoá
                showConfirmDialog?.let { src ->
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = null },
                        confirmButton = {
                            TextButton(onClick = {
                                showConfirmDialog = null
                                scope.launch {
                                    try {
                                        firestore.collection("rss_sources").document(src.id).delete().await()
                                        snackbarHostState.showSnackbar("🗑️ Đã xoá: ${src.name}")
                                        reloadAllSources()
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("❌ Lỗi xoá: ${e.localizedMessage}")
                                    }
                                }
                            }) { Text("Xoá", color = Color(0xFFFF4081)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = null }) {
                                Text("Huỷ", color = Color.White)
                            }
                        },
                        title = { Text("Xác nhận xoá", color = Color(0xFFFFD54F)) },
                        text = { Text("Bạn có chắc chắn muốn xoá nguồn '${src.name}' không?", color = Color.White) },
                        containerColor = Color(0xFF1C0F3A)
                    )
                }
            }
        }
    }
}

@Composable
private fun RssSourceItem(source: RssSource, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0846))
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    source.name + if (!source.fromFirestore) " (mặc định)" else "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(source.url, color = Color(0xFFB39DDB), fontSize = 14.sp)
                Text(
                    if (source.isActive) "🟢 Hoạt động" else "🔴 Không phản hồi",
                    color = if (source.isActive) Color(0xFF81C784) else Color(0xFFFF5252),
                    fontSize = 13.sp
                )
            }
            if (source.fromFirestore) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Xoá", tint = Color(0xFFFF4081))
                }
            }
        }
    }
}
