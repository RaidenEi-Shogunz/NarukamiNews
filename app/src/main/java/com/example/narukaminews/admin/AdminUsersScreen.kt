package com.example.narukaminews.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 🔹 Dữ liệu người dùng
data class UserInfo(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user",
    val isOnline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf(emptyList<UserInfo>()) }
    var filteredUsers by remember { mutableStateOf(emptyList<UserInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasAccess by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showConfirmChangeRole by remember { mutableStateOf<UserInfo?>(null) }
    var showConfirmDelete by remember { mutableStateOf<UserInfo?>(null) }

    // 🔐 Kiểm tra quyền admin
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val doc = firestore.collection("users").document(currentUser.uid).get().await()
                val role = doc.getString("role") ?: "user"
                hasAccess = role.equals("admin", ignoreCase = true)
            } catch (_: Exception) {
                hasAccess = false
            }
        }
    }

    // 🔄 Lắng nghe danh sách realtime
    DisposableEffect(hasAccess) {
        if (!hasAccess) return@DisposableEffect onDispose {}
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val allUsers = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserInfo::class.java)?.copy(id = doc.id)
                    }.sortedWith(
                        compareByDescending<UserInfo> { it.role.equals("admin", true) }
                            .thenBy { it.name.lowercase() }
                    )
                    users = allUsers
                    filteredUsers = if (searchQuery.isBlank()) allUsers else allUsers.filter { user ->
                        user.name.contains(searchQuery, true) || user.email.contains(searchQuery, true)
                    }
                }
                isLoading = false
            }
        onDispose { listener.remove() }
    }

    // 🔍 Cập nhật khi tìm kiếm
    LaunchedEffect(searchQuery, users) {
        filteredUsers = if (searchQuery.isBlank()) users else users.filter { user ->
            user.name.contains(searchQuery, true) || user.email.contains(searchQuery, true)
        }
    }

    GradientBackground {
        Scaffold(
            topBar = { NarukamiTopBar("👥 Quản lý người dùng", navController) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            when {
                !hasAccess -> NoPermissionView(padding)
                isLoading -> LoadingView(padding)
                else -> {
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(16.dp)
                    ) {
                        Text(
                            "Danh sách người dùng (${filteredUsers.size})",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        // 🔍 Ô tìm kiếm
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Tìm theo tên hoặc email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD54F),
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        if (filteredUsers.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Không tìm thấy người dùng phù hợp 😶", color = Color.LightGray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredUsers, key = { it.id }) { user ->
                                    val isSelf = currentUser?.uid == user.id
                                    UserCard(
                                        user = user,
                                        isSelf = isSelf,
                                        onChangeRole = { showConfirmChangeRole = user },
                                        onDelete = { showConfirmDelete = user }
                                    )
                                }
                            }
                        }
                    }

                    // 🔸 Dialog xác nhận đổi vai trò
                    showConfirmChangeRole?.let { targetUser ->
                        ConfirmDialog(
                            title = "Xác nhận đổi vai trò",
                            message = "Bạn có chắc muốn đổi vai trò của ${targetUser.email}?",
                            confirmText = "Đổi vai trò",
                            onConfirm = {
                                showConfirmChangeRole = null
                                scope.launch {
                                    try {
                                        val newRole =
                                            if (targetUser.role.equals("admin", true)) "user" else "admin"
                                        firestore.collection("users")
                                            .document(targetUser.id)
                                            .update("role", newRole)
                                            .await()
                                        snackbarHostState.showSnackbar("✅ Đã đổi ${targetUser.email} thành $newRole")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("❌ ${e.localizedMessage}")
                                    }
                                }
                            },
                            onDismiss = { showConfirmChangeRole = null }
                        )
                    }

                    // 🔸 Dialog xác nhận xoá
                    showConfirmDelete?.let { targetUser ->
                        ConfirmDialog(
                            title = "Xoá tài khoản",
                            message = "Bạn có chắc muốn xoá tài khoản ${targetUser.email}?",
                            confirmText = "Xoá",
                            onConfirm = {
                                showConfirmDelete = null
                                scope.launch {
                                    try {
                                        firestore.collection("users")
                                            .document(targetUser.id)
                                            .delete()
                                            .await()
                                        snackbarHostState.showSnackbar("🗑️ Đã xoá ${targetUser.email}")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("❌ ${e.localizedMessage}")
                                    }
                                }
                            },
                            onDismiss = { showConfirmDelete = null }
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                               🔹 Components UI                             */
/* -------------------------------------------------------------------------- */

@Composable
private fun NoPermissionView(padding: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🚫 Bạn không có quyền truy cập trang này.",
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun LoadingView(padding: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFFFD54F))
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ", color = Color.White) }
        },
        title = { Text(title, color = Color(0xFFFFD54F)) },
        text = { Text(message, color = Color.LightGray) },
        containerColor = Color(0xFF1A0846)
    )
}

@Composable
private fun UserCard(
    user: UserInfo,
    isSelf: Boolean,
    onChangeRole: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelf) Color(0xFF4527A0) else Color(0xFF1A0846)
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (isSelf) "${user.name.ifBlank { "Không rõ" }} (Bạn)" else user.name.ifBlank { "Không rõ" },
                        color = if (isSelf) Color(0xFFFFD54F) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(user.email, color = Color.LightGray, fontSize = 13.sp)
                    Text(
                        if (user.isOnline) "🟢 Đang hoạt động" else "🔴 Ngoại tuyến",
                        color = if (user.isOnline) Color(0xFF81C784) else Color(0xFFFF5252),
                        fontSize = 12.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.role,
                        color = if (user.role.equals("admin", true)) Color(0xFFFFD54F) else Color(0xFFB39DDB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (user.role.equals("admin", ignoreCase = true)) {
                        Icon(
                            Icons.Filled.Verified,
                            null,
                            tint = Color(0xFF80DEEA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row {
                    IconButton(
                        onClick = { if (!isSelf) onChangeRole() },
                        enabled = !isSelf
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = "Đổi vai trò",
                            tint = if (isSelf) Color.Gray else Color(0xFFFFD54F)
                        )
                    }
                    IconButton(
                        onClick = { if (!isSelf) onDelete() },
                        enabled = !isSelf
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Xoá người dùng",
                            tint = if (isSelf) Color.Gray else Color(0xFFFF4081)
                        )
                    }
                }
            }
        }
    }
}
