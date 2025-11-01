package com.example.narukaminews

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "user",
    val createdAt: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangKyScreen(
    navController: NavHostController,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 🎨 Nền gradient động (hiệu ứng chuyển màu mượt)
    val infiniteTransition = rememberInfiniteTransition(label = "bgAnim")
    val bgShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "bgShift"
    )

    val animatedBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF0D0221),
            Color(0xFF311B92).copy(alpha = 0.8f + 0.2f * kotlin.math.sin(bgShift * Math.PI * 2).toFloat()),
            Color(0xFF4A148C)
        )
    )

    // ⚙️ Hiển thị Snackbar lỗi
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            error = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🌌 Hình nền Raiden Shogun mờ
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
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(40.dp))

            Image(
                painter = painterResource(id = R.drawable.narukami_icon),
                contentDescription = "Narukami Icon",
                modifier = Modifier.size(120.dp).clip(CircleShape)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Đăng ký NarukamiNews ⚡",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD54F)
            )
            Text(
                "Tham gia cộng đồng tri thức sấm sét",
                fontSize = 16.sp,
                color = Color(0xFFB39DDB)
            )

            Spacer(Modifier.height(28.dp))

            // 🧩 Form nhập thông tin
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Tên hiển thị", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, null, tint = Color(0xFFFFD54F))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color(0xFF9575CD),
                        cursorColor = Color(0xFFFFD54F)
                    )
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, null, tint = Color(0xFFFFD54F))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color(0xFF9575CD),
                        cursorColor = Color(0xFFFFD54F)
                    )
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color(0xFF9575CD),
                        cursorColor = Color(0xFFFFD54F)
                    )
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Xác nhận mật khẩu", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Filled.VerifiedUser, null, tint = Color(0xFFFFD54F))
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color(0xFF9575CD),
                        cursorColor = Color(0xFFFFD54F)
                    )
                )
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = Color.Red,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ⚡ Nút đăng ký
            Button(
                onClick = {
                    when {
                        username.isBlank() || email.isBlank() || pass.isBlank() || confirmPass.isBlank() ->
                            error = "⚡ Vui lòng điền đầy đủ thông tin!"
                        !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                            error = "⚡ Email không hợp lệ!"
                        pass.length < 6 ->
                            error = "⚡ Mật khẩu phải từ 6 ký tự trở lên!"
                        pass != confirmPass ->
                            error = "⚡ Mật khẩu không khớp!"
                        else -> {
                            error = null
                            showConfirmDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = !isLoading,
                contentPadding = PaddingValues()
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF7B1FA2), Color(0xFF512DA8), Color(0xFFFFD54F))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Đăng ký ⚡", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onBackToLogin) {
                Text("⬅️ Quay lại đăng nhập", color = Color(0xFFFFD54F), fontSize = 15.sp)
            }

            Spacer(Modifier.height(40.dp))
        }

        // ⏳ Overlay loading
        if (isLoading) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color(0xFFFFD54F)) }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // ⚡ Hộp thoại xác nhận đăng ký
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color(0xFF1C0F3A),
            tonalElevation = 8.dp,
            title = {
                Text("⚡ Xác nhận đăng ký", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bạn có chắc chắn muốn tạo tài khoản mới?", color = Color(0xFFEDE7F6), fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Hãy tham gia cộng đồng tri thức ⚡ Narukami!", color = Color(0xFFB388FF), fontSize = 15.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email.trim(), pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                val newUser = User(uid, username, email, "user", Timestamp.now())

                                db.collection("users").document(uid).set(newUser)
                                    .addOnSuccessListener {
                                        db.collection("users").document(uid)
                                            .update("createdAt", FieldValue.serverTimestamp())

                                        /*
                                        // 📧 --- XÁC MINH EMAIL  ---
                                        auth.currentUser?.sendEmailVerification()
                                            ?.addOnCompleteListener { verifyTask ->
                                                if (verifyTask.isSuccessful) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("📧 Đã gửi email xác minh! Vui lòng kiểm tra hộp thư.")
                                                    }
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("⚠️ Không thể gửi email xác minh: ${verifyTask.exception?.message}")
                                                    }
                                                }
                                            }
                                        */

                                        isLoading = false
                                        scope.launch {
                                            delay(200)
                                            navController.popBackStack()
                                            snackbarHostState.showSnackbar("🎉 Đăng ký thành công!")
                                        }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        error = "⚡ Lỗi lưu dữ liệu: ${it.localizedMessage}"
                                    }
                            } else {
                                isLoading = false
                                error = when (task.exception) {
                                    is FirebaseAuthUserCollisionException -> "⚡ Email này đã được đăng ký!"
                                    is FirebaseAuthInvalidCredentialsException -> "⚡ Email không hợp lệ!"
                                    else -> "⚡ ${task.exception?.localizedMessage ?: "Lỗi không xác định"}"
                                }
                            }
                        }
                }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFD54F))) {
                    Text("⚡ Tạo tài khoản", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
