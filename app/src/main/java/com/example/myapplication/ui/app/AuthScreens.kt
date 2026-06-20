package com.example.myapplication.ui.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    var forgotPasswordStep by remember { mutableStateOf(1) }

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
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE0F2FE), Color(0xFFF1F5F9), Color(0xFFFAE8FF))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape = RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Logo and App Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    AppLogo(size = 48)
                    Column {
                        Text(
                            text = "Rental Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Hệ Thống Quản Lý Nhà Trọ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Divider(color = Color(0xFFE2E8F0))

                // Page Title (Without the subtitle description below it)
                Text(
                    text = authTitle(mode, forgotPasswordStep),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Custom segmented tab controller for AuthMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(AuthMode.Login, AuthMode.Register, AuthMode.Forgot).forEach { item ->
                        val selected = mode == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable {
                                    mode = item
                                    forgotPasswordStep = 1
                                    error = null
                                    message = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label,
                                color = if (selected) Color(0xFF0F172A) else Color(0xFF64748B),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Custom segmented control for UserRole
                if (mode == AuthMode.Login || mode == AuthMode.Register) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val roles = if (mode == AuthMode.Register) listOf(UserRole.ChuTro, UserRole.NguoiDung) else UserRole.entries
                        roles.forEach { item ->
                            val selected = role == item
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0xFF0284C7).copy(alpha = 0.08f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selected) Color(0xFF0284C7) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectRole(item) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.label,
                                    color = if (selected) Color(0xFF0284C7) else Color(0xFF64748B),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Input fields
                if (mode == AuthMode.Login || mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Tên đăng nhập hoặc email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                if (mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Họ tên") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                    if (role == UserRole.NguoiDung) {
                        OutlinedTextField(
                            value = cccd,
                            onValueChange = { cccd = it },
                            label = { Text("CCCD/CMND") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = Color(0xFF0284C7),
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { frontPicker.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF0284C7))
                            ) { Text(if (cccdFrontUrl.isBlank()) "CCCD Mặt Trước" else "Đã chọn mặt trước", style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                            OutlinedButton(
                                onClick = { backPicker.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF0284C7))
                            ) { Text(if (cccdBackUrl.isBlank()) "CCCD Mặt Sau" else "Đã chọn mặt sau", style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                        }
                    }
                }

                if (mode == AuthMode.Forgot) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        enabled = forgotPasswordStep == 1,
                        label = { Text("Email đã đăng ký") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                if (mode == AuthMode.Forgot && forgotPasswordStep == 2) {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("Mã OTP/Token (nhập 123456)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                if (mode != AuthMode.Forgot || forgotPasswordStep == 2) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (mode == AuthMode.Forgot) "Mật khẩu mới" else "Mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                if (mode == AuthMode.Register || (mode == AuthMode.Forgot && forgotPasswordStep == 2)) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Nhập lại mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // Error / Success message handling
                error?.let { Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall) }
                message?.let { Text(it, color = Color(0xFF047857), style = MaterialTheme.typography.bodySmall) }

                // Main Gradient Submit Button
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
                                AuthMode.Forgot -> {
                                    if (forgotPasswordStep == 1) {
                                        repository.forgotPassword(email)
                                            .onSuccess {
                                                message = it
                                                forgotPasswordStep = 2
                                            }
                                            .onFailure { error = it.message ?: "Không thể gửi OTP." }
                                    } else {
                                        repository.resetPassword(email, otp, password, confirmPassword)
                                            .onSuccess {
                                                message = it
                                                mode = AuthMode.Login
                                                forgotPasswordStep = 1
                                            }
                                            .onFailure { error = it.message ?: "Không thể đặt lại mật khẩu." }
                                    }
                                }
                                AuthMode.Reset -> {} // Unused in combined flow
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (loading) "Đang xử lý..." else if (mode == AuthMode.Forgot && forgotPasswordStep == 1) "Gửi OTP" else if (mode == AuthMode.Forgot && forgotPasswordStep == 2) "Đặt lại mật khẩu" else mode.action,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Demo session button (Only shows during Login)
                if (mode == AuthMode.Login) {
                    OutlinedButton(
                        enabled = !loading,
                        onClick = {
                            error = null
                            message = "Đang dùng tài khoản mẫu trong app để kiểm thử."
                            onLoggedIn(repository.demoSession(role.name))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6))
                    ) {
                        Text("Dùng tài khoản mẫu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Option to go back to email entry step during Reset process
                if (mode == AuthMode.Forgot && forgotPasswordStep == 2) {
                    TextButton(
                        onClick = {
                            forgotPasswordStep = 1
                            error = null
                            message = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Quay lại nhập email", color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
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

private fun authTitle(mode: AuthMode, step: Int): String = when (mode) {
    AuthMode.Login -> "Đăng nhập"
    AuthMode.Register -> "Đăng ký tài khoản"
    AuthMode.Forgot -> if (step == 1) "Quên mật khẩu" else "Đặt lại mật khẩu"
    AuthMode.Reset -> "Đặt lại mật khẩu"
}
