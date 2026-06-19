package com.example.myapplication.ui.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun LoginScreen(repository: RentalRepository, onLoggedIn: (UserSession) -> Unit) {
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF4F46E5), Color(0xFF6D28D9), Color(0xFF1E1B4B)) // Vibrant premium dark indigo/violet background
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppLogo(size = 48)
                    Column {
                        Text(
                            text = "Antigravity Rental",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1B4B)
                        )
                        Text(
                            text = "Hệ Thống Quản Lý Nhà Trọ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                
                Divider(color = Color(0xFFF1F5F9))

                Text(
                    text = authTitle(mode),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = authSubtitle(mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(AuthMode.Login, AuthMode.Register, AuthMode.Forgot, AuthMode.Reset)) { item ->
                        FilterChip(
                            selected = mode == item,
                            onClick = { mode = item; error = null; message = null },
                            label = { Text(item.label) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (mode == AuthMode.Login || mode == AuthMode.Register) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(if (mode == AuthMode.Register) listOf(UserRole.ChuTro, UserRole.NguoiDung) else UserRole.entries) { item ->
                            FilterChip(
                                selected = role == item,
                                onClick = { selectRole(item) },
                                label = { Text(item.label) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                if (mode == AuthMode.Login || mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Tên đăng nhập hoặc email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Họ tên") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    if (role == UserRole.NguoiDung) {
                        OutlinedTextField(
                            value = cccd,
                            onValueChange = { cccd = it },
                            label = { Text("CCCD/CMND") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { frontPicker.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text(if (cccdFrontUrl.isBlank()) "Ảnh CCCD Trước" else "Đã chọn mặt trước") }
                            OutlinedButton(
                                onClick = { backPicker.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text(if (cccdBackUrl.isBlank()) "Ảnh CCCD Sau" else "Đã chọn mặt sau") }
                        }
                    }
                }

                if (mode == AuthMode.Forgot || mode == AuthMode.Reset) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email nhận OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (mode == AuthMode.Reset) {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("Mã OTP/Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text(if (loading) "Đang xử lý..." else mode.action, fontWeight = FontWeight.Bold)
                }

                if (mode == AuthMode.Login) {
                    OutlinedButton(
                        enabled = !loading,
                        onClick = {
                            error = null
                            message = "Đang dùng tài khoản mẫu trong app để kiểm thử."
                            onLoggedIn(repository.demoSession(role.name))
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dùng tài khoản mẫu", fontWeight = FontWeight.SemiBold)
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
