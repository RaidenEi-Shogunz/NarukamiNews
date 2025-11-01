@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.example.narukaminews.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.narukaminews.NewsViewModel
import com.example.narukaminews.R
import com.example.narukaminews.data.RssSources
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import androidx.compose.ui.zIndex

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: NewsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val accentColor = MaterialTheme.colorScheme.primary

    val favoriteSources by viewModel.favoriteSources.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedSource by rememberSaveable { mutableStateOf<String?>(null) }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var buttonWidth by remember { mutableIntStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var isAdmin by remember { mutableStateOf(false) }
    val firestore = remember { FirebaseFirestore.getInstance() }

    val defaultBase = remember {
        listOf(
            "VNExpress" to "https://vnexpress.net/rss/tin-moi-nhat.rss",
            "Tuổi Trẻ" to "https://tuoitre.vn/rss/tin-moi-nhat.rss",
            "Thanh Niên" to "https://thanhnien.vn/rss/home.rss",
            "Zing News" to "https://zingnews.vn/rss.html",
            "Dân Trí" to "https://dantri.com.vn/rss/home.rss",
            "Vietnamnet" to "https://vietnamnet.vn/rss/home.rss"
        )
    }

    var dynamicSources by remember { mutableStateOf<List<Triple<String, String, Boolean>>>(emptyList()) }

    // 🔐 Kiểm tra quyền admin realtime
    DisposableEffect(currentUser?.uid) {
        var registration: ListenerRegistration? = null
        if (currentUser != null) {
            val docRef = firestore.collection("users").document(currentUser.uid)
            registration = docRef.addSnapshotListener { snap, err ->
                if (err != null) {
                    isAdmin = false
                    return@addSnapshotListener
                }
                val role = snap?.getString("role")?.lowercase() ?: "user"
                isAdmin = role == "admin" || currentUser.email == "admin@narukami.app"
            }
        } else isAdmin = false
        onDispose { registration?.remove() }
    }

    // 🔄 Nguồn động từ Firestore
    LaunchedEffect(Unit) {
        firestore.collection("rss_sources").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { d ->
                    val name = d.getString("name") ?: return@mapNotNull null
                    val url = d.getString("url") ?: return@mapNotNull null
                    Triple(name, url, true)
                }
                dynamicSources = list
            }
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 🧭 MAIN UI */
    /* ---------------------------------------------------------------------- */
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NarukamiDrawer(
                navController = navController,
                drawerState = drawerState,
                snackbarHostState = snackbarHostState,
                isAdmin = isAdmin,
                currentUser = currentUser,
                auth = auth
            )
        }
    ) {
        Scaffold(
            topBar = {
                NarukamiTopBarWithAvatar(scope, drawerState, navController, accentColor)
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.3f),
                                Color(0xFF0D0221)
                            )
                        )
                    )
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // 🔹 Tiêu đề + nút Làm mới
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CHỌN NGUỒN BÁO ⚡",
                        color = accentColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = {
                        selectedSource = null
                        isGridView = false
                        expanded = false
                        scope.launch {
                            snackbarHostState.showSnackbar("⚡ Đã làm mới giao diện")
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, null, tint = accentColor)
                    }
                }

                Spacer(Modifier.height(12.dp))

                val allSources = remember(dynamicSources) {
                    (defaultBase.map { Triple(it.first, it.second, true) } + dynamicSources)
                        .distinctBy { it.first }
                }

                Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                buttonWidth = coordinates.size.width
                            },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Text(
                            selectedSource ?: "Chọn nguồn báo...",
                            color = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.ArrowDropDown, null, tint = accentColor)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .width(with(LocalDensity.current) { buttonWidth.toDp() })
                            .background(Color(0xFF1C0F3A))
                            .offset(y = 6.dp)
                            .zIndex(2f),
                        containerColor = Color(0xFF1C0F3A)
                    ) {
                        allSources.forEach { (name, _, _) ->
                            DropdownMenuItem(
                                text = {
                                    Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                                },
                                onClick = {
                                    selectedSource = name
                                    expanded = false
                                },
                                trailingIcon = {
                                    val isFav = name in favoriteSources
                                    IconButton(onClick = {
                                        viewModel.toggleFavorite(name, !isFav)
                                    }) {
                                        Icon(
                                            if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            null,
                                            tint = Color.Red
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 🔹 Hiển thị danh mục
                AnimatedVisibility(
                    visible = selectedSource != null,
                    enter = fadeIn(tween(300)) + expandVertically(),
                    exit = fadeOut(tween(300)) + shrinkVertically()
                ) {
                    val categories = RssSources[selectedSource]?.keys?.toList() ?: emptyList()
                    if (categories.isEmpty()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "Nguồn \"$selectedSource\" chưa có category trong RssSources.",
                                color = Color(0xFFFFAB91),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        if (isGridView) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(categories) { category ->
                                    CategoryCard(category, accentColor) {
                                        navController.navigate("articles/$selectedSource/$category")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(categories) { category ->
                                    CategoryCard(category, accentColor) {
                                        navController.navigate("articles/$selectedSource/$category")
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedSource == null,
                    enter = fadeIn(tween(300)) + expandVertically(),
                    exit = fadeOut(tween(300)) + shrinkVertically()
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Hãy chọn nguồn báo để xem thể loại 📚",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 🔹 Category Card                                                          */
/* -------------------------------------------------------------------------- */
@Composable
fun CategoryCard(category: String, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(getCategoryIcon(category), null, tint = Color.Black)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                category,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* 🔹 TopBar có avatar ⚡                                                    */
/* -------------------------------------------------------------------------- */
@Composable
private fun NarukamiTopBarWithAvatar(
    scope: CoroutineScope,
    drawerState: DrawerState,
    navController: NavHostController,
    accentColor: Color
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val db = FirebaseFirestore.getInstance()

    var userName by remember { mutableStateOf<String?>(null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        val name = doc.getString("name")?.trim()
                        userName = if (!name.isNullOrEmpty()) name else null
                        avatarUrl = doc.getString("avatarUrl")
                    } else {
                        userName = null
                        avatarUrl = null
                    }
                }
        } else {
            userName = null
            avatarUrl = null
        }
    }

    val scale by animateFloatAsState(targetValue = 1f, animationSpec = tween(400))

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.narukami_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Narukami News ⚡",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                Icon(Icons.Filled.Menu, contentDescription = null, tint = Color.White)
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                if (!userName.isNullOrBlank()) {
                    Text(
                        text = userName!!,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                AsyncImage(
                    model = avatarUrl ?: "https://cdn-icons-png.flaticon.com/512/847/847969.png",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .border(2.dp, accentColor, CircleShape)
                        .clickable {
                            if (currentUser == null) {
                                navController.navigate("login")
                            } else {
                                navController.navigate("profile")
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0221))
    )
}

/* -------------------------------------------------------------------------- */
/* 🔹 Icon phân loại tin tức                                                 */
/* -------------------------------------------------------------------------- */
fun getCategoryIcon(category: String): ImageVector = when {
    "thể thao" in category.lowercase() -> Icons.Outlined.SportsSoccer
    "kinh" in category.lowercase() -> Icons.Outlined.Business
    "giải" in category.lowercase() -> Icons.Outlined.Star
    else -> Icons.Outlined.Newspaper
}


/* -------------------------------------------------------------------------- */
/* 🔹 Drawer chính                                                            */
/* -------------------------------------------------------------------------- */
@Composable
private fun NarukamiDrawer(
    navController: NavHostController,
    drawerState: DrawerState,
    snackbarHostState: SnackbarHostState,
    isAdmin: Boolean,
    currentUser: FirebaseUser?,
    auth: FirebaseAuth
) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    var userName by remember { mutableStateOf("") }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        userName = doc.getString("name") ?: ""
                    }
                }
        }
    }

    val displayName = userName.ifBlank { currentUser?.email ?: "" }

    val menuItems = remember {
        listOf(
            "📰  Nguồn yêu thích" to "favorites",
            "🔖  Đọc sau" to "bookmarks",
            "🎯  Chủ đề quan tâm" to "topics",
            "🔥  Tin nổi bật" to "hotnews",
            "ℹ️  Giới thiệu & Chính sách" to "about"
        )
    }

    val adminMenu = remember {
        listOf(
            "📰  Quản lý nguồn RSS" to "admin_sources",
            "🔥  Quản lý Tin nổi bật" to "admin_highlight",
            "👥  Quản lý người dùng" to "admin_users",
            "📊  Thống kê & Giám sát" to "admin_stats"
        )
    }

    val context = LocalContext.current
    val isDarkMode by com.example.narukaminews.ui.theme.ThemePreferences
        .isDarkMode(context)
        .collectAsState(initial = true)

    ModalDrawerSheet(
        drawerContainerColor = Color.Transparent,
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(Color(0xFF14032A), Color(0xFF1C0F3A), Color(0xFF0D0221))
            )
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { drawerState.close() } }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFFFFD54F))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Trở lại",
                        color = Color(0xFFFFD54F),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                HorizontalDivider(color = Color(0xFFFFD54F).copy(alpha = 0.2f))
                Text(
                    "⚡ DÀNH CHO BẠN",
                    color = Color(0xFFFFD54F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (currentUser != null) {
                item {
                    DrawerItem("👤  Hồ sơ cá nhân") {
                        navController.navigate("profile")
                        scope.launch { drawerState.close() }
                    }
                }
            }

            menuItems.forEach { (label, route) ->
                item {
                    DrawerItem(label) {
                        navController.navigate(route)
                        scope.launch { drawerState.close() }
                    }
                }
            }

            if (isAdmin) {
                item {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        "👑 ADMIN PANEL",
                        color = Color(0xFFFFD54F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                adminMenu.forEach { (label, route) ->
                    item {
                        DrawerItem(label) {
                            navController.navigate(route)
                            scope.launch { drawerState.close() }
                        }
                    }
                }
            }

            // 🌙 Toggle Dark Mode
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌙  Chế độ tối", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = {
                            scope.launch {
                                com.example.narukaminews.ui.theme.ThemePreferences
                                    .setDarkMode(context, it)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFD54F),
                            uncheckedThumbColor = Color.Gray
                        )
                    )
                }
            }
            // 🎨 Accent color selector
            item {
                Text(
                    "🎨  Màu chủ đề",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                var expanded by remember { mutableStateOf(false) }
                var selectedLabel by remember { mutableStateOf("💜 Tím Narukami (Mặc định)") }

                val colors = listOf(
                    "#7B1FA2" to "💜 Tím Narukami (Mặc định)",
                    "#FFD54F" to "🌟 Vàng ánh kim",
                    "#1E88E5" to "💙 Xanh sấm sét",
                    "#D32F2F" to "❤️ Đỏ điện ngọc",
                    "#43A047" to "💚 Lục nguyên tố"
                )

                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(selectedLabel, color = Color.White, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1C0F3A))
                    ) {
                        colors.forEach { (hex, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(20.dp)
                                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                                .border(1.dp, Color.White, CircleShape)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(label, color = Color.White)
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    selectedLabel = label
                                    scope.launch {
                                        com.example.narukaminews.ui.theme.ThemePreferences
                                            .setAccentColor(context, hex)
                                        snackbarHostState.showSnackbar("🎨 Đã đổi chủ đề sang: $label")
                                    }
                                }
                            )
                        }

                        // ➕ Thêm lựa chọn khôi phục mặc định
                        DropdownMenuItem(
                            text = { Text("↩️ Khôi phục mặc định", color = Color.LightGray) },
                            onClick = {
                                expanded = false
                                selectedLabel = "💜 Tím Narukami (Mặc định)"
                                scope.launch {
                                    com.example.narukaminews.ui.theme.ThemePreferences
                                        .setAccentColor(context, "#7B1FA2")
                                    snackbarHostState.showSnackbar("↩️ Đã khôi phục màu mặc định 💜")
                                }
                            }
                        )
                    }
                }
            }



            // 🔐 Đăng nhập / Đăng xuất
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (currentUser == null) {
                    DrawerItem("🔑  Đăng nhập") {
                        navController.navigate("login")
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem("🆕  Đăng ký") {
                        navController.navigate("register")
                        scope.launch { drawerState.close() }
                    }
                } else {
                    DrawerItem("🚪  Đăng xuất ($displayName)") {
                        auth.signOut()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Đã đăng xuất khỏi tài khoản $displayName",
                                duration = SnackbarDuration.Short
                            )
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/* 🔹 Item trong Drawer                                                      */
/* -------------------------------------------------------------------------- */
@Composable
fun DrawerItem(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

