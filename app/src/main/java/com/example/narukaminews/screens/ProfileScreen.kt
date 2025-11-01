package com.example.narukaminews.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val uid = user.uid
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 🌈 Màu từ theme
    val accentColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(
        listOf(accentColor.copy(alpha = 0.25f), bgColor)
    )

    // 🖼️ Chọn & tải ảnh
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isUploading = true
                    val ref = storage.reference.child("avatars/$uid.jpg")
                    ref.putFile(uri).await()
                    val url = ref.downloadUrl.await().toString()
                    db.collection("users").document(uid).update("avatarUrl", url).await()
                    avatarUrl = url
                    snackbarHostState.showSnackbar("✅ Ảnh đại diện đã cập nhật!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("❌ Lỗi tải ảnh: ${e.message}")
                } finally {
                    isUploading = false
                }
            }
        }
    }

    // 🔄 Lắng nghe realtime Firestore
    DisposableEffect(uid) {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    name = doc.getString("name") ?: ""
                    address = doc.getString("address") ?: ""
                    phone = doc.getString("phone") ?: ""
                    bio = doc.getString("bio") ?: ""
                    avatarUrl = doc.getString("avatarUrl")
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "⚡ Hồ sơ cá nhân",
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
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
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🖼️ Avatar
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = avatarUrl ?: "https://cdn-icons-png.flaticon.com/512/847/847969.png",
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, accentColor, CircleShape)
                            .clickable { imagePicker.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (name.isNotBlank()) "Xin chào, $name"
                    else "Xin chào, ${user.email ?: "Khách"}",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(24.dp))

                // ✏️ Trường nhập
                ProfileField("Tên hiển thị (*)", name, accentColor, textColor) { name = it }
                ProfileField("Địa chỉ", address, accentColor, textColor) { address = it }
                ProfileField("Số điện thoại", phone, accentColor, textColor) { phone = it }
                ProfileField("Tiểu sử", bio, accentColor, textColor, lines = 3) { bio = it }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = user.email ?: "",
                    onValueChange = {},
                    label = { Text("Email", color = accentColor) },
                    textStyle = TextStyle(color = textColor),
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = accentColor,
                        disabledBorderColor = accentColor,
                        disabledLabelColor = accentColor
                    )
                )

                Spacer(Modifier.height(28.dp))

                // 💾 Nút Lưu
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("⚠️ Tên hiển thị là bắt buộc!") }
                            return@Button
                        }
                        scope.launch {
                            try {
                                db.collection("users").document(uid).update(
                                    mapOf(
                                        "name" to name,
                                        "address" to address,
                                        "phone" to phone,
                                        "bio" to bio
                                    )
                                ).await()
                                snackbarHostState.showSnackbar("✅ Hồ sơ đã được lưu!")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Lỗi lưu dữ liệu: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text("💾 Lưu thay đổi", color = bgColor, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))

                // 🚪 Đăng xuất
                OutlinedButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text("🚪 Đăng xuất", color = accentColor)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileField(
    label: String,
    value: String,
    accentColor: Color,
    textColor: Color,
    lines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = accentColor) },
        textStyle = TextStyle(color = textColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = lines == 1,
        maxLines = lines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = accentColor.copy(alpha = 0.5f),
            cursorColor = accentColor
        )
    )
}
