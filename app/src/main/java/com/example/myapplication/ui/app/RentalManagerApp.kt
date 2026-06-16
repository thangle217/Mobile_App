package com.example.myapplication.ui.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.SessionStore
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.AccountProfile
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import com.example.myapplication.domain.util.formatCompactMoney
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun RentalManagerApp() {
    val context = LocalContext.current
    val repository = remember(context) { RentalRepository(context) }
    val sessionStore = remember { SessionStore(context) }
    val scope = rememberCoroutineScope()
    var restored by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<UserSession?>(null) }
    var screen by remember { mutableStateOf(AppScreen.Dashboard) }

    LaunchedEffect(Unit) {
        sessionStore.session.collect {
            session = it
            restored = true
        }
    }

    if (!restored) {
        FullScreenLoading("Đang khởi động ứng dụng...")
    } else if (session == null) {
        LoginScreen(
            repository = repository,
            onLoggedIn = {
                session = it
                screen = AppScreen.Dashboard
                if (it.token.isNotBlank()) scope.launch { sessionStore.save(it) }
            }
        )
    } else {
        MainShell(
            repository = repository,
            session = session,
            screen = screen,
            onScreenChange = { screen = it },
            onLogout = {
                scope.launch { sessionStore.clear() }
                session = null
                screen = AppScreen.Dashboard
            }
        )
    }
}

@Composable
private fun LoginScreen(repository: RentalRepository, onLoggedIn: (UserSession) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var role by remember { mutableStateOf(UserRole.ChuTro) }
    var username by remember { mutableStateOf("chutro") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var cccdFrontUrl by remember { mutableStateOf("") }
    var cccdBackUrl by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun uploadFromUri(uri: Uri, onDone: (String) -> Unit) {
        loading = true
        error = null
        scope.launch {
            val result = runCatching {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Không đọc được ảnh.")
                val name = "cccd_${System.currentTimeMillis()}.jpg"
                repository.uploadCccdImage(name, mime, bytes).getOrThrow()
            }
            result.onSuccess {
                onDone(it)
                message = "Upload ảnh CCCD thành công."
            }.onFailure {
                error = it.message ?: "Upload ảnh thất bại."
            }
            loading = false
        }
    }

    val frontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadFromUri(it) { url -> cccdFrontUrl = url } }
    }
    val backPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadFromUri(it) { url -> cccdBackUrl = url } }
    }

    fun selectRole(next: UserRole) {
        role = next
        username = when (next) {
            UserRole.Admin -> "Admin"
            UserRole.ChuTro -> "chutro"
            UserRole.NguoiDung -> "nguoithue"
        }
        password = if (next == UserRole.Admin) "Admin123" else "123456"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0891B2), Color(0xFF065F46))))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppLogo(size = 54)
                Text(authTitle(mode), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A))
                Text(authSubtitle(mode), color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(AuthMode.Login, AuthMode.Register, AuthMode.Forgot, AuthMode.Reset)) { item ->
                        FilterChip(selected = mode == item, onClick = { mode = item; error = null; message = null }, label = { Text(item.label) })
                    }
                }
                if (mode == AuthMode.Login || mode == AuthMode.Register) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(if (mode == AuthMode.Register) listOf(UserRole.ChuTro, UserRole.NguoiDung) else UserRole.entries) { item ->
                            FilterChip(selected = role == item, onClick = { selectRole(item) }, label = { Text(item.label) })
                        }
                    }
                }
                if (mode == AuthMode.Login || mode == AuthMode.Register) {
                    OutlinedTextField(username, { username = it }, label = { Text("Tên đăng nhập hoặc email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                }
                if (mode == AuthMode.Register) {
                    OutlinedTextField(fullName, { fullName = it }, label = { Text("Họ tên") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                    if (role == UserRole.NguoiDung) {
                        OutlinedTextField(cccd, { cccd = it }, label = { Text("CCCD/CMND") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { frontPicker.launch("image/*") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(if (cccdFrontUrl.isBlank()) "Ảnh mặt trước" else "Đã có mặt trước") }
                            OutlinedButton(onClick = { backPicker.launch("image/*") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(if (cccdBackUrl.isBlank()) "Ảnh mặt sau" else "Đã có mặt sau") }
                        }
                    }
                }
                if (mode == AuthMode.Forgot || mode == AuthMode.Reset) {
                    OutlinedTextField(email, { email = it }, label = { Text("Email nhận OTP") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                }
                if (mode == AuthMode.Reset) {
                    OutlinedTextField(otp, { otp = it }, label = { Text("Mã OTP/Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                }
                if (mode != AuthMode.Forgot) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (mode == AuthMode.Reset) "Mật khẩu mới" else "Mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                if (mode == AuthMode.Register || mode == AuthMode.Reset) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Nhập lại mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                error?.let { Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall) }
                message?.let { Text(it, color = Color(0xFF047857), style = MaterialTheme.typography.bodySmall) }
                Button(
                    enabled = !loading,
                    onClick = {
                        loading = true
                        error = null
                        message = null
                        scope.launch {
                            when (mode) {
                                AuthMode.Login -> repository.login(username, password, role)
                                    .onSuccess(onLoggedIn)
                                    .onFailure { error = it.message ?: "Không thể đăng nhập." }
                                AuthMode.Register -> repository.register(
                                    JSONObject()
                                        .put("tenDangNhap", username)
                                        .put("matKhau", password)
                                        .put("xacNhanMatKhau", confirmPassword)
                                        .put("email", email)
                                        .put("hoTen", fullName)
                                        .put("soDienThoai", phone)
                                        .put("cccd", cccd)
                                        .put("anhCccdMatTruoc", cccdFrontUrl)
                                        .put("anhCccdMatSau", cccdBackUrl)
                                        .put("vaiTro", role.name)
                                ).onSuccess {
                                    message = it
                                    mode = AuthMode.Login
                                }.onFailure { error = it.message ?: "Không thể đăng ký." }
                                AuthMode.Forgot -> repository.forgotPassword(email)
                                    .onSuccess { message = it; mode = AuthMode.Reset }
                                    .onFailure { error = it.message ?: "Không thể gửi OTP." }
                                AuthMode.Reset -> repository.resetPassword(email, otp, password, confirmPassword)
                                    .onSuccess { message = it; mode = AuthMode.Login }
                                    .onFailure { error = it.message ?: "Không thể đặt lại mật khẩu." }
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Text(if (loading) "Đang xử lý..." else mode.action)
                }
                if (mode == AuthMode.Login) {
                    OutlinedButton(
                        enabled = !loading,
                        onClick = {
                            error = null
                            message = "Đang dùng tài khoản mẫu trong app để kiểm thử."
                            onLoggedIn(repository.demoSession(role.name))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Dùng tài khoản mẫu")
                    }
                }
            }
        }
    }
}

private enum class AuthMode(val label: String, val action: String) {
    Login("Đăng nhập", "Đăng nhập"),
    Register("Đăng ký", "Tạo tài khoản"),
    Forgot("Quên mật khẩu", "Gửi OTP"),
    Reset("Đặt lại", "Đặt lại mật khẩu")
}

private fun authTitle(mode: AuthMode): String = when (mode) {
    AuthMode.Login -> "Đăng nhập"
    AuthMode.Register -> "Đăng ký tài khoản"
    AuthMode.Forgot -> "Quên mật khẩu"
    AuthMode.Reset -> "Đặt lại mật khẩu"
}

private fun authSubtitle(mode: AuthMode): String = when (mode) {
    AuthMode.Login -> "Đăng nhập bằng tài khoản đã lưu trong app hoặc tài khoản mẫu."
    AuthMode.Register -> "Tạo tài khoản Chủ trọ hoặc Người thuê và dùng ngay trong app."
    AuthMode.Forgot -> "Nhập email để lấy mã đặt lại mật khẩu trong bản local."
    AuthMode.Reset -> "Nhập mã 123456 hoặc mã đã được cấp và mật khẩu mới."
}

@Composable
private fun MainShell(
    repository: RentalRepository,
    session: UserSession?,
    screen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    val role = session?.role ?: UserRole.ChuTro
    val screens = screensForRole(role)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(size = 42)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(session?.displayName ?: role.label, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A))
                            Text(role.label, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    screens.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label) },
                            selected = item == screen,
                            onClick = {
                                onScreenChange(item)
                                scope.launch { drawerState.close() }
                            },
                            badge = { Text(item.shortCode) }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppHeader(
                    title = screen.label,
                    sourceLabel = "Dữ liệu trong app",
                    onMenu = { scope.launch { drawerState.open() } },
                    onLogout = onLogout
                )
            },
            bottomBar = {
                AppBottomBar(
                    items = bottomScreens(role),
                    selected = screen,
                    onSelected = onScreenChange
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (screen) {
                    AppScreen.Dashboard -> DashboardScreen(repository, session, onScreenChange, onLogout)
                    AppScreen.Account -> AccountScreen(repository, session, onLogout)
                    else -> ModuleScreen(repository, session, screen, onLogout)
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(repository: RentalRepository, session: UserSession?, onOpen: (AppScreen) -> Unit, onSessionExpired: () -> Unit) {
    var state by remember { mutableStateOf<UiState<DashboardSummary>>(UiState.Loading) }

    fun load() {
        state = UiState.Loading
    }

    LaunchedEffect(session, state) {
        if (state is UiState.Loading) state = repository.dashboard(session)
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { summary, source ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF0FDFA), Color(0xFFF8FAFC))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HeroCard(session, summary, source) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Phòng", summary.totalRooms.toString(), "${summary.emptyRooms} còn trống", Color(0xFF0D9488), Modifier.weight(1f))
                    StatCard("Hóa đơn", summary.unpaidInvoices.toString(), "Cần thu/xử lý", Color(0xFFF59E0B), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Cần xử lý", summary.pendingTasks.toString(), "Yêu cầu và biên lai", Color(0xFF0891B2), Modifier.weight(1f))
                    StatCard("Đã thu", formatCompactMoney(summary.revenue), "Theo dữ liệu hiện tại", Color(0xFF10B981), Modifier.weight(1f))
                }
            }
            item { SectionTitle("Tác vụ nhanh") }
            item { QuickActions(session?.role ?: UserRole.ChuTro, onOpen) }
        }
    }
}

@Composable
private fun ModuleScreen(repository: RentalRepository, session: UserSession?, screen: AppScreen, onSessionExpired: () -> Unit) {
    val scope = rememberCoroutineScope()
    var state by remember(screen, session) { mutableStateOf<UiState<List<RentalItem>>>(UiState.Loading) }
    var query by remember(screen) { mutableStateOf("") }
    var status by remember(screen) { mutableStateOf("Tất cả") }
    var selected by remember { mutableStateOf<RentalItem?>(null) }
    var editing by remember { mutableStateOf<RentalItem?>(null) }
    var rentRequestRoom by remember { mutableStateOf<RentalItem?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val role = session?.role ?: UserRole.NguoiDung
    val canManage = canManageScreen(role, screen)

    fun load() {
        state = UiState.Loading
    }

    LaunchedEffect(screen, session, state) {
        if (state is UiState.Loading) state = repository.list(screen, session)
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { items, source ->
        val statuses = listOf("Tất cả") + items.map { it.status }.distinct()
        val filtered = items.filter {
            val textMatch = it.title.contains(query, true) || it.id.contains(query, true) || it.note.contains(query, true) || it.value.contains(query, true)
            val statusMatch = status == "Tất cả" || it.status == status
            textMatch && statusMatch
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF0FDFA), Color(0xFFF8FAFC))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ModuleHeader(screen, items.size, source) }
            if (canManage) item {
                Button(
                    onClick = {
                        actionError = null
                        editing = RentalItem(
                            id = "",
                            title = "",
                            status = defaultStatus(screen),
                            value = "",
                            note = ""
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = screenAccent(screen)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
                ) {
                    Text("Thêm ${screen.label.lowercase()}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            actionError?.let {
                item { Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall) }
            }
            item {
                SearchPanel(query, { query = it }, statuses, status, { status = it })
            }
            if (filtered.isEmpty()) {
                item { EmptyState("Không tìm thấy dữ liệu phù hợp.") }
            } else {
                items(filtered, key = { it.id }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RentalListCard(
                            item = item,
                            canManage = canManage,
                            onOpen = { selected = item },
                            onEdit = {
                                actionError = null
                                editing = item
                            },
                            onDelete = {
                                actionError = null
                                scope.launch {
                                    repository.deleteItem(screen, item.id, session)
                                        .onSuccess { load() }
                                        .onFailure { actionError = it.message ?: "Không thể xóa dữ liệu." }
                                }
                            }
                        )
                        ModuleActionBar(
                            screen = screen,
                            item = item,
                            role = role,
                            onRentRoom = { rentRequestRoom = item },
                            onApproveRequest = {
                                scope.launch {
                                    repository.decideRentRequest(item.id, approve = true, session = session)
                                        .onSuccess { load() }
                                        .onFailure { actionError = it.message ?: "Không thể duyệt yêu cầu." }
                                }
                            },
                            onRejectRequest = {
                                scope.launch {
                                    repository.decideRentRequest(item.id, approve = false, session = session)
                                        .onSuccess { load() }
                                        .onFailure { actionError = it.message ?: "Không thể từ chối yêu cầu." }
                                }
                            },
                            onConfirmContract = {
                                scope.launch {
                                    repository.confirmContract(item.id, approve = true, session = session)
                                        .onSuccess { load() }
                                        .onFailure { actionError = it.message ?: "Không thể xác nhận hợp đồng." }
                                }
                            },
                            onRejectContract = {
                                scope.launch {
                                    repository.confirmContract(item.id, approve = false, session = session)
                                        .onSuccess { load() }
                                        .onFailure { actionError = it.message ?: "Không thể từ chối hợp đồng." }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    selected?.let {
        DetailDialog(item = it, screen = screen, onDismiss = { selected = null })
    }

    editing?.let { item ->
        EditItemDialog(
            screen = screen,
            item = item,
            onDismiss = { editing = null },
            onSave = { next ->
                actionError = null
                scope.launch {
                    repository.saveItem(screen, next, session)
                        .onSuccess {
                            editing = null
                            load()
                        }
                        .onFailure { actionError = it.message ?: "Không thể lưu dữ liệu." }
                }
            }
        )
    }

    rentRequestRoom?.let { room ->
        RentRequestDialog(
            room = room,
            onDismiss = { rentRequestRoom = null },
            onSubmit = { duration, note ->
                actionError = null
                scope.launch {
                    repository.requestRoom(room.id, session, duration, note)
                        .onSuccess {
                            rentRequestRoom = null
                            load()
                        }
                        .onFailure { actionError = it.message ?: "Không thể gửi yêu cầu thuê phòng." }
                }
            }
        )
    }
}

@Composable
private fun AccountScreen(repository: RentalRepository, session: UserSession?, onSessionExpired: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var state by remember(session) { mutableStateOf<UiState<AccountProfile>>(UiState.Loading) }
    var editing by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(AccountProfile()) }

    fun load() {
        state = UiState.Loading
    }

    fun uploadFromUri(uri: Uri, front: Boolean) {
        val currentSession = session ?: return
        saving = true
        error = null
        scope.launch {
            val result = runCatching {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Không đọc được ảnh.")
                repository.uploadCccdImage("cccd_${System.currentTimeMillis()}.jpg", mime, bytes, currentSession).getOrThrow()
            }
            result.onSuccess {
                draft = if (front) draft.copy(cccdFrontUrl = it) else draft.copy(cccdBackUrl = it)
                message = "Upload ảnh CCCD thành công. Bấm Lưu hồ sơ để cập nhật."
            }.onFailure {
                error = it.message ?: "Upload ảnh thất bại."
            }
            saving = false
        }
    }

    val frontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadFromUri(it, front = true) }
    }
    val backPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadFromUri(it, front = false) }
    }

    LaunchedEffect(session, state) {
        if (state is UiState.Loading && session != null) {
            state = repository.account(session)
            (state as? UiState.Content<AccountProfile>)?.let { draft = it.data }
        }
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { profile, _ ->
        if (!editing) draft = profile
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF0FDFA), Color(0xFFF8FAFC))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD1FAE5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppLogo(size = 44)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tài khoản của tôi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A))
                                Text(profile.role.label, color = Color(0xFF64748B))
                            }
                        }
                        message?.let { Text(it, color = Color(0xFF047857)) }
                        error?.let { Text(it, color = Color(0xFFDC2626)) }
                        if (editing) {
                            ProfileEditor(draft, onChange = { draft = it }, onPickFront = { frontPicker.launch("image/*") }, onPickBack = { backPicker.launch("image/*") })
                        } else {
                            listOf(
                                "Họ tên" to profile.fullName,
                                "Tên đăng nhập" to profile.username,
                                "Email" to profile.email,
                                "Số điện thoại" to profile.phone,
                                "CCCD" to profile.cccd,
                                "Địa chỉ" to profile.address,
                                "Ngân hàng" to profile.bankName,
                                "Số tài khoản" to profile.bankAccount
                            ).forEach { DetailRow(it.first, it.second.ifBlank { "Chưa cập nhật" }) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                enabled = !saving,
                                onClick = {
                                    if (editing) {
                                        val currentSession = session ?: return@Button
                                        saving = true
                                        scope.launch {
                                            repository.updateAccount(currentSession, draft)
                                                .onSuccess {
                                                    draft = it
                                                    state = UiState.Content(it, DataSource.Local)
                                                    editing = false
                                                    message = "Cập nhật hồ sơ thành công."
                                                }
                                                .onFailure { error = it.message ?: "Không thể cập nhật hồ sơ." }
                                            saving = false
                                        }
                                    } else {
                                        editing = true
                                        message = null
                                        error = null
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier.weight(1f)
                            ) { Text(if (editing) "Lưu hồ sơ" else "Cập nhật") }
                            OutlinedButton(onClick = { changingPassword = true }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                Text("Đổi mật khẩu")
                            }
                        }
                        if (editing) {
                            TextButton(onClick = { editing = false; draft = profile }) { Text("Hủy chỉnh sửa") }
                        }
                    }
                }
            }
        }
    }

    if (changingPassword) {
        ChangePasswordDialog(
            saving = saving,
            onDismiss = { changingPassword = false },
            onSubmit = { oldPassword, newPassword, confirmPassword ->
                val currentSession = session ?: return@ChangePasswordDialog
                saving = true
                scope.launch {
                    repository.changePassword(currentSession, oldPassword, newPassword, confirmPassword)
                        .onSuccess {
                            message = it
                            changingPassword = false
                        }
                        .onFailure { error = it.message ?: "Không thể đổi mật khẩu." }
                    saving = false
                }
            }
        )
    }
}

@Composable
private fun <T> StateContainer(
    state: UiState<T>,
    onRetry: () -> Unit,
    onSessionExpired: (() -> Unit)? = null,
    content: @Composable (T, DataSource) -> Unit
) {
    when (state) {
        UiState.Loading -> FullScreenLoading("Đang tải dữ liệu...")
        is UiState.Empty -> EmptyState(state.message)
        is UiState.Error -> {
            val action: () -> Unit = if (state.canRetry) onRetry else ({ onSessionExpired?.invoke() })
            ErrorState(state.message, action, state.canRetry)
        }
        is UiState.Content -> content(state.data, state.source)
    }
}

@Composable
private fun FullScreenLoading(message: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0FDFA)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = Color(0xFF0F766E))
            Text(message, color = Color(0xFF134E4A))
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, canRetry: Boolean = true) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFFECACA))) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Không tải được dữ liệu", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                Text(message, color = Color(0xFF64748B))
                Button(onClick = onRetry, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))) {
                    Text(if (canRetry) "Thử lại" else "Đăng nhập lại")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFD1FAE5))) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = CircleShape, color = Color(0xFFCCFBF1), modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("0", color = Color(0xFF0F766E), fontWeight = FontWeight.Bold) }
                }
                Text(message, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    profile: AccountProfile,
    onChange: (AccountProfile) -> Unit,
    onPickFront: () -> Unit,
    onPickBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(profile.fullName, { onChange(profile.copy(fullName = it)) }, label = { Text("Họ tên") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.email, { onChange(profile.copy(email = it)) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.phone, { onChange(profile.copy(phone = it)) }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.cccd, { onChange(profile.copy(cccd = it)) }, label = { Text("CCCD/CMND") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(profile.dateOfBirth.take(10), { onChange(profile.copy(dateOfBirth = it)) }, label = { Text("Ngày sinh yyyy-MM-dd") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(profile.gender, { onChange(profile.copy(gender = it)) }, label = { Text("Giới tính") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
        }
        OutlinedTextField(profile.address, { onChange(profile.copy(address = it)) }, label = { Text("Địa chỉ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(profile.workplace, { onChange(profile.copy(workplace = it)) }, label = { Text("Nơi công tác") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPickFront, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text(if (profile.cccdFrontUrl.isBlank()) "Ảnh CCCD trước" else "Đổi ảnh trước")
            }
            OutlinedButton(onClick = onPickBack, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text(if (profile.cccdBackUrl.isBlank()) "Ảnh CCCD sau" else "Đổi ảnh sau")
            }
        }
        OutlinedTextField(profile.bankName, { onChange(profile.copy(bankName = it)) }, label = { Text("Tên ngân hàng") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(profile.bankAccount, { onChange(profile.copy(bankAccount = it)) }, label = { Text("Số tài khoản") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(profile.bankOwner, { onChange(profile.copy(bankOwner = it)) }, label = { Text("Chủ tài khoản") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
        }
        OutlinedTextField(profile.transferContent, { onChange(profile.copy(transferContent = it)) }, label = { Text("Nội dung chuyển khoản mặc định") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
    }
}

@Composable
private fun ChangePasswordDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(oldPassword, { oldPassword = it }, label = { Text("Mật khẩu cũ") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(newPassword, { newPassword = it }, label = { Text("Mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Nhập lại mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = { onSubmit(oldPassword, newPassword, confirmPassword) }) {
                Text(if (saving) "Đang lưu..." else "Đổi mật khẩu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun AppHeader(title: String, sourceLabel: String, onMenu: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF0891B2), Color(0xFF065F46))))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onMenu) { Text("Menu", color = Color.White) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sourceLabel, color = Color(0xFFCCFBF1), style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = onLogout) { Text("Thoát", color = Color.White) }
    }
}

@Composable
private fun AppBottomBar(items: List<AppScreen>, selected: AppScreen, onSelected: (AppScreen) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 10.dp,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFFE0F2F1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val active = selected == item
                val accent = screenAccent(item)
                Surface(
                    onClick = { onSelected(item) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) accent.copy(alpha = 0.16f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                    shadowElevation = if (active) 4.dp else 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) accent else accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                item.shortCode,
                                color = if (active) Color.White else accent,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            item.label,
                            color = if (active) Color(0xFF134E4A) else Color(0xFF475569),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLogo(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0891B2))))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("RT", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeroCard(session: UserSession?, summary: DashboardSummary, source: DataSource) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0891B2), Color(0xFF065F46))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Xin chào, ${session?.displayName ?: "bạn"}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Dữ liệu được lưu trực tiếp trong app để bạn thao tác và kiểm thử ngay.", color = Color(0xFFCCFBF1))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HeroMiniMetric("Nguồn", sourceLabel(source), Modifier.weight(1f))
                HeroMiniMetric("Phòng trống", summary.emptyRooms.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.14f)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Color(0xFFCCFBF1), style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFD1FAE5))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(7.dp))
                Text(title, color = Color(0xFF64748B), maxLines = 1)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A))
            Text(detail, color = accent, maxLines = 2, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickActions(role: UserRole, onOpen: (AppScreen) -> Unit) {
    val actions = listOf(
        AppScreen.Rooms to if (role == UserRole.NguoiDung) "Tìm phòng" else "Quản lý phòng",
        AppScreen.Invoices to "Hóa đơn",
        (if (role == UserRole.NguoiDung) AppScreen.Payments else AppScreen.Electric) to if (role == UserRole.NguoiDung) "Gửi biên lai" else "Ghi điện",
        AppScreen.Incidents to "Báo sự cố"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(actions) { action ->
            val accent = screenAccent(action.first)
            Surface(
                onClick = { onOpen(action.first) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
                shadowElevation = 4.dp,
                modifier = Modifier.width(164.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(Brush.horizontalGradient(listOf(accent, Color(0xFFF59E0B))))
                    )
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.14f)) {
                                Text(
                                    action.first.shortCode,
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        }
                        Text(action.second, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(action.first.label, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleHeader(screen: AppScreen, count: Int, source: DataSource) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF0F766E), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(screen.shortCode, color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(screen.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A))
            Text("$count bản ghi • ${sourceLabel(source)}", color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun SearchPanel(query: String, onQueryChange: (String) -> Unit, statuses: List<String>, status: String, onStatusChange: (String) -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFD1FAE5))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(query, onQueryChange, label = { Text("Tìm mã, tên, ghi chú hoặc số tiền") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statuses) { item ->
                    FilterChip(selected = status == item, onClick = { onStatusChange(item) }, label = { Text(item) })
                }
            }
        }
    }
}

@Composable
private fun RentalListCard(item: RentalItem, canManage: Boolean, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 5.dp, height = 52.dp).clip(RoundedCornerShape(8.dp)).background(statusColor(item.status)))
                Spacer(Modifier.width(10.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor(item.status).copy(alpha = 0.12f), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(item.id.takeLast(2), color = statusColor(item.status), fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.id, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                }
                StatusPill(item.status)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.value, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                    Text(item.note, color = Color(0xFF64748B), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor(item.status)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Chi tiết", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (canManage) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Sửa", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Xóa", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    Surface(color = statusColor(status).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, statusColor(status).copy(alpha = 0.18f))) {
        Text(status, color = statusColor(status), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModuleActionBar(
    screen: AppScreen,
    item: RentalItem,
    role: UserRole,
    onRentRoom: () -> Unit,
    onApproveRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onConfirmContract: () -> Unit,
    onRejectContract: () -> Unit
) {
    val showRent = role == UserRole.NguoiDung && screen == AppScreen.Rooms && item.status.equals("Còn trống", true)
    val showDecision = role != UserRole.NguoiDung && screen == AppScreen.RentRequests && item.status.contains("Chờ", true)
    val showConfirm = role == UserRole.NguoiDung && screen == AppScreen.Contracts && item.status == "Chờ người thuê xác nhận"
    if (!showRent && !showDecision && !showConfirm) return

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD1FAE5)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showRent) {
                Button(
                    onClick = onRentRoom,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Gửi yêu cầu thuê", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (showDecision) {
                Button(
                    onClick = onApproveRequest,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Duyệt", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRejectRequest,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Từ chối", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
            if (showConfirm) {
                Button(
                    onClick = onConfirmContract,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Xác nhận", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRejectContract,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Từ chối", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RentRequestDialog(room: RentalItem, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var duration by remember(room) { mutableStateOf("6 tháng") }
    var note by remember(room) { mutableStateOf("Mình muốn thuê phòng này.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gửi yêu cầu thuê") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("Phòng", room.title)
                DetailRow("Giá", room.value)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Thời hạn mong muốn") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(duration, note) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text("Gửi yêu cầu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun EditItemDialog(screen: AppScreen, item: RentalItem, onDismiss: () -> Unit, onSave: (RentalItem) -> Unit) {
    var title by remember(item) { mutableStateOf(item.title) }
    var status by remember(item) { mutableStateOf(item.status.ifBlank { defaultStatus(screen) }) }
    var value by remember(item) { mutableStateOf(item.value) }
    var note by remember(item) { mutableStateOf(item.note) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id.isBlank()) "Thêm ${screen.label.lowercase()}" else "Sửa ${screen.label.lowercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên / nội dung") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Trạng thái") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Giá trị / số tiền / thời hạn") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
                localError?.let { Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        localError = "Vui lòng nhập tên hoặc nội dung."
                    } else {
                        onSave(
                            item.copy(
                                title = title.trim(),
                                status = status.trim().ifBlank { defaultStatus(screen) },
                                value = value.trim(),
                                note = note.trim()
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = screenAccent(screen))
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun DetailDialog(item: RentalItem, screen: AppScreen, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DetailRow("Module", screen.label) }
                item { DetailRow("Mã", item.id) }
                item { DetailRow("Trạng thái", item.status) }
                item { DetailRow("Giá trị", item.value) }
                item { DetailRow("Ghi chú", item.note) }
                items(item.details) { DetailRow(it.first, it.second) }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Đóng") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.Medium, color = Color(0xFF134E4A))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 22.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF59E0B)))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF134E4A))
    }
}

private fun screensForRole(role: UserRole): List<AppScreen> = when (role) {
    UserRole.Admin -> AppScreen.entries
    UserRole.NguoiDung -> listOf(
        AppScreen.Dashboard, AppScreen.Rooms, AppScreen.Contracts, AppScreen.Invoices,
        AppScreen.Payments, AppScreen.Services, AppScreen.ServiceRegs, AppScreen.Electric,
        AppScreen.Water, AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents,
        AppScreen.Notices, AppScreen.Account
    )
    UserRole.ChuTro -> listOf(
        AppScreen.Dashboard, AppScreen.Houses, AppScreen.RoomTypes, AppScreen.Rooms,
        AppScreen.Tenants, AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments,
        AppScreen.Services, AppScreen.ServiceRegs, AppScreen.Electric, AppScreen.Water,
        AppScreen.RentRequests, AppScreen.RenewRequests, AppScreen.Incidents,
        AppScreen.Notices, AppScreen.Account
    )
}

private fun bottomScreens(role: UserRole): List<AppScreen> = when (role) {
    UserRole.NguoiDung -> listOf(AppScreen.Dashboard, AppScreen.Rooms, AppScreen.Invoices, AppScreen.Notices, AppScreen.Account)
    else -> listOf(AppScreen.Dashboard, AppScreen.Rooms, AppScreen.Invoices, AppScreen.Payments, AppScreen.Account)
}

private fun canManageScreen(role: UserRole, screen: AppScreen): Boolean = when (role) {
    UserRole.Admin -> screen != AppScreen.Account
    UserRole.ChuTro -> screen in setOf(
        AppScreen.Houses,
        AppScreen.RoomTypes,
        AppScreen.Rooms,
        AppScreen.Tenants,
        AppScreen.Contracts,
        AppScreen.Invoices,
        AppScreen.Payments,
        AppScreen.Services,
        AppScreen.ServiceRegs,
        AppScreen.Electric,
        AppScreen.Water,
        AppScreen.RentRequests,
        AppScreen.RenewRequests,
        AppScreen.Incidents,
        AppScreen.Notices
    )
    UserRole.NguoiDung -> false
}

private fun sourceLabel(source: DataSource): String = when (source) {
    DataSource.Api -> "API"
    DataSource.Demo -> "Demo"
    DataSource.Local -> "Trong app"
}

private fun defaultStatus(screen: AppScreen): String = when (screen) {
    AppScreen.Rooms -> "Còn trống"
    AppScreen.Invoices -> "Chưa thanh toán"
    AppScreen.Payments -> "Chờ xác nhận"
    AppScreen.RentRequests, AppScreen.RenewRequests -> "Chờ duyệt"
    AppScreen.Incidents, AppScreen.Notices -> "Mới"
    AppScreen.Users -> "Đang hoạt động"
    else -> "Đang sử dụng"
}

private fun statusColor(status: String): Color = when {
    status.contains("Đã", true) || status.contains("Còn trống", true) || status.contains("Đang sử dụng", true) -> Color(0xFF059669)
    status.contains("Chờ", true) || status.contains("Chưa", true) || status.contains("Một phần", true) || status.contains("Sắp", true) || status.contains("Cần", true) -> Color(0xFFD97706)
    status.contains("Từ chối", true) || status.contains("Hủy", true) || status.contains("Tạm dừng", true) -> Color(0xFFDC2626)
    status.contains("Đang", true) || status.contains("Mới", true) -> Color(0xFF2563EB)
    else -> Color(0xFF475569)
}

private fun screenAccent(screen: AppScreen): Color = when (screen) {
    AppScreen.Dashboard -> Color(0xFF7C3AED)
    AppScreen.Houses -> Color(0xFF0D9488)
    AppScreen.RoomTypes -> Color(0xFF06B6D4)
    AppScreen.Rooms -> Color(0xFF0891B2)
    AppScreen.Tenants -> Color(0xFF10B981)
    AppScreen.Contracts -> Color(0xFF6366F1)
    AppScreen.Invoices -> Color(0xFFF59E0B)
    AppScreen.Payments -> Color(0xFFEA580C)
    AppScreen.Services -> Color(0xFF14B8A6)
    AppScreen.ServiceRegs -> Color(0xFF84CC16)
    AppScreen.Electric -> Color(0xFFEAB308)
    AppScreen.Water -> Color(0xFF0284C7)
    AppScreen.RentRequests -> Color(0xFFEC4899)
    AppScreen.RenewRequests -> Color(0xFFA855F7)
    AppScreen.Incidents -> Color(0xFFDC2626)
    AppScreen.Notices -> Color(0xFF2563EB)
    AppScreen.Users -> Color(0xFF475569)
    AppScreen.Account -> Color(0xFF0F766E)
}
