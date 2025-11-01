package com.example.narukaminews

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangNhapScreen(
    onLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    // 🌈 Hiệu ứng phát sáng quanh logo
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 🌌 Nền gradient động kiểu Raiden Shogun
    val bgAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "bgAnim"
    )

    val animatedBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D0221),
            Color(0xFF311B92).copy(alpha = 0.8f + 0.2f * kotlin.math.sin(bgAnim * Math.PI * 2).toFloat()),
            Color(0xFF4A148C)
        )
    )

    // ⚙️ Hàm lỗi Firebase thân thiện
    fun getFirebaseErrorMessage(e: Exception?): String = when {
        e?.message?.contains("password", true) == true -> "⚡ Mật khẩu không đúng!"
        e?.message?.contains("no user record", true) == true -> "⚡ Tài khoản không tồn tại!"
        e?.message?.contains("network", true) == true -> "⚡ Kiểm tra lại kết nối mạng!"
        else -> e?.localizedMessage ?: "⚡ Đăng nhập thất bại, thử lại sau."
    }

    Box(Modifier.fillMaxSize()) {
        // 🌌 Nền Raiden
        Image(
            painter = painterResource(id = R.drawable.raiden),
            contentDescription = "Raiden background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.15f)
        )
        Box(Modifier.fillMaxSize().background(animatedBrush))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ⚡ Logo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD54F).copy(alpha = glowAlpha * 0.3f))
                )
                Image(
                    painter = painterResource(id = R.drawable.narukami_icon),
                    contentDescription = "Narukami Logo",
                    modifier = Modifier.size(120.dp).clip(CircleShape)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Narukami News ⚡",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD54F)
            )
            Text(
                "Sấm sét của tri thức toàn cầu",
                fontSize = 16.sp,
                color = Color(0xFFB39DDB)
            )

            Spacer(Modifier.height(28.dp))

            // 🧩 Form nhập
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, null, tint = Color(0xFFFFD54F))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color(0xFF9575CD),
                        cursorColor = Color(0xFFFFD54F)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, null, tint = Color(0xFFFFD54F))
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color(0xFF9575CD),
                        cursorColor = Color(0xFFFFD54F)
                    )
                )
            }

            // ⚠️ Hiển thị lỗi
            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error ?: "",
                    color = Color.Red,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ⚡ Nút đăng nhập
            Button(
                onClick = {
                    error = null
                    if (email.isBlank() || password.isBlank()) {
                        error = "⚡ Vui lòng nhập đủ thông tin"
                        return@Button
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        error = "⚡ Email không hợp lệ"
                        return@Button
                    }

                    isLoading = true
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser

                                /*
                                // 📧 --- KIỂM TRA XÁC MINH EMAIL (TẠM COMMENT LẠI) ---
                                // Khi bật tính năng xác minh email, chỉ cho phép vào app nếu đã xác minh.
                                if (user != null && !user.isEmailVerified) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "⚠️ Tài khoản chưa xác minh! Vui lòng kiểm tra email để kích hoạt."
                                        )
                                    }
                                    return@addOnCompleteListener
                                }
                                // --- HẾT KIỂM TRA XÁC MINH EMAIL ---
                                */

                                if (user != null) {
                                    val firestore = FirebaseFirestore.getInstance()
                                    val userRef = firestore.collection("users").document(user.uid)
                                    scope.launch(Dispatchers.IO) {
                                        firestore.runTransaction { transaction ->
                                            val snapshot = transaction.get(userRef)
                                            val currentRole = snapshot.getString("role") ?: "user"
                                            val adminEmails = listOf("admin@narukami.app")

                                            val roleToKeep = when {
                                                currentRole == "admin" -> "admin"
                                                user.email in adminEmails -> "admin"
                                                else -> "user"
                                            }

                                            val userDoc = mapOf(
                                                "uid" to user.uid,
                                                "email" to user.email,
                                                "username" to (user.email?.substringBefore("@") ?: "unknown"),
                                                "role" to roleToKeep,
                                                "lastLoginAt" to FieldValue.serverTimestamp(),
                                                "updatedAt" to FieldValue.serverTimestamp()
                                            )
                                            transaction.set(userRef, userDoc, SetOptions.merge())
                                        }
                                    }
                                }
                                showConfirmDialog = true
                            } else {
                                error = getFirebaseErrorMessage(task.exception)
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = !isLoading,
                contentPadding = PaddingValues()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF7B1FA2), Color(0xFF512DA8), Color(0xFFFFD54F))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Đăng nhập ⚡", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("Chưa có tài khoản? Đăng ký ngay ⚡", color = Color(0xFFFFD54F), fontSize = 15.sp)
            }
        }

        // ⏳ Loading overlay
        if (isLoading) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFFD54F))
            }
        }

        // ⚡ Alert xác nhận đăng nhập
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = Color(0xFF1C0F3A),
                tonalElevation = 8.dp,
                title = {
                    Text("⚡ Xác nhận đăng nhập", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bạn có chắc chắn muốn vào Narukami News?", color = Color(0xFFEDE7F6), fontSize = 17.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Hãy sẵn sàng đón nhận sức mạnh ⚡ của tri thức!", color = Color(0xFFB388FF), fontSize = 15.sp)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            scope.launch {
                                delay(200)
                                onLogin()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFD54F))
                    ) {
                        Text("⚡ Vào ngay", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB39DDB))) {
                        Text("Hủy", fontSize = 16.sp)
                    }
                }
            )
        }
    }
}
