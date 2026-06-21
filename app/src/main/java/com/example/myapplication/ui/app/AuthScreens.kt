package com.example.myapplication.ui.app

import androidx.compose.ui.text.style.TextDecoration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.foundation.Canvas

class PointedShape(private val arrowWidth: Dp = 16.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val arrowW = with(density) { arrowWidth.toPx() }
        val path = Path().apply {
            moveTo(arrowW, 0f)
            lineTo(size.width - arrowW, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width - arrowW, size.height)
            lineTo(arrowW, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

class HexagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val h = size.height
            val w = size.width
            moveTo(w / 2f, 0f)
            lineTo(w, h * 0.25f)
            lineTo(w, h * 0.75f)
            lineTo(w / 2f, h)
            lineTo(0f, h * 0.75f)
            lineTo(0f, h * 0.25f)
            close()
        }
        return Outline.Generic(path)
    }
}

private val VisibilityIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Visibility",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 4.5f)
            curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            close()
            moveTo(12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
            close()
            moveTo(12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            close()
        }
    }.build()

private val VisibilityOffIcon: ImageVector
    get() = ImageVector.Builder(
        name = "VisibilityOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 12.71f, 16.85f, 13.38f, 16.58f, 13.99f)
            lineTo(19.26f, 16.67f)
            curveTo(20.59f, 15.39f, 21.96f, 13.82f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            curveTo(10.29f, 4.5f, 8.66f, 4.9f, 7.18f, 5.58f)
            lineTo(9.44f, 7.84f)
            curveTo(10.23f, 7.3f, 11.08f, 7f, 12f, 7f)
            close()
            moveTo(2.71f, 3.16f)
            lineTo(1.29f, 4.57f)
            lineTo(3.43f, 6.71f)
            curveTo(2.32f, 8.23f, 1.48f, 10.03f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(13.6f, 19.5f, 15.13f, 19.16f, 16.53f, 18.57f)
            lineTo(19.43f, 21.47f)
            lineTo(20.84f, 20.06f)
            lineTo(2.71f, 3.16f)
            close()
            moveTo(12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            curveTo(7f, 11.23f, 7.17f, 10.51f, 7.48f, 9.87f)
            lineTo(14.07f, 16.46f)
            curveTo(13.44f, 16.8f, 12.75f, 17f, 12f, 17f)
            close()
        }
    }.build()

@Composable
fun HexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        visualTransformation = visualTransformation,
        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontSize = 16.sp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(PointedShape())
                    .background(Color.White.copy(alpha = if (enabled) 0.9f else 0.5f))
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFCBD5E1)))
                Spacer(modifier = Modifier.width(12.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color(0xFF94A3B8), fontSize = 15.sp)
                    }
                    innerTextField()
                }
                
                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        }
    )
}

@OptIn(ExperimentalAnimationApi::class)
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
    var passwordVisible by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize()
    ) {
        // Wavy Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topGradient = Brush.linearGradient(
                colors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)) // Light Green to Green
            )
            val bottomGradient = Brush.linearGradient(
                colors = listOf(Color(0xFF22C55E), Color(0xFF14532D)) // Green to Dark Green
            )
            
            drawRect(brush = topGradient)
            
            val path = Path().apply {
                moveTo(0f, size.height * 0.45f)
                cubicTo(
                    size.width * 0.3f, size.height * 0.65f,
                    size.width * 0.7f, size.height * 0.25f,
                    size.width, size.height * 0.5f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = path, brush = bottomGradient)
        }

        // Main Content
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Glassmorphism Card
            Column(
                modifier = Modifier
                    .padding(top = 40.dp) // Space for the top hexagon
                    .fillMaxWidth()
                    .clip(CutCornerShape(40.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CutCornerShape(40.dp))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = authTitle(mode, forgotPasswordStep),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                AnimatedContent(
                    targetState = mode,
                    label = "auth_transition"
                ) { currentMode ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Custom Role Selector
                        if (currentMode == AuthMode.Login || currentMode == AuthMode.Register) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val roles = if (currentMode == AuthMode.Register) listOf(UserRole.ChuTro, UserRole.NguoiDung) else UserRole.entries
                                roles.forEach { item ->
                                    val selected = role == item
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) Color.White.copy(alpha = 0.8f) else Color.Transparent)
                                            .clickable { selectRole(item) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.label,
                                            color = if (selected) Color(0xFF0F766E) else Color.White,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Input fields
                        if (currentMode == AuthMode.Login || currentMode == AuthMode.Register) {
                            HexTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = "Tên đăng nhập hoặc email",
                                leadingIcon = Icons.Default.Person
                            )
                        }

                        if (currentMode == AuthMode.Register) {
                            HexTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                placeholder = "Họ tên",
                                leadingIcon = Icons.Default.Info
                            )
                            HexTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = "Email",
                                leadingIcon = Icons.Default.Email
                            )
                            HexTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                placeholder = "Số điện thoại",
                                leadingIcon = Icons.Default.Phone
                            )
                            if (role == UserRole.NguoiDung) {
                                HexTextField(
                                    value = cccd,
                                    onValueChange = { cccd = it },
                                    placeholder = "CCCD/CMND",
                                    leadingIcon = Icons.Default.Info
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier.weight(1f).height(48.dp).clip(PointedShape()).background(Color.White.copy(alpha = 0.85f)).clickable { frontPicker.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) { Text(if (cccdFrontUrl.isBlank()) "Mặt Trước" else "Đã chọn", color = Color(0xFF334155), fontSize = 14.sp) }
                                    Box(
                                        modifier = Modifier.weight(1f).height(48.dp).clip(PointedShape()).background(Color.White.copy(alpha = 0.85f)).clickable { backPicker.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) { Text(if (cccdBackUrl.isBlank()) "Mặt Sau" else "Đã chọn", color = Color(0xFF334155), fontSize = 14.sp) }
                                }
                            }
                        }

                        if (currentMode == AuthMode.Forgot) {
                            HexTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = "Email đã đăng ký",
                                leadingIcon = Icons.Default.Email
                            )
                        }

                        if (currentMode != AuthMode.Forgot) {
                            HexTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = if (currentMode == AuthMode.Forgot) "Mật khẩu mới" else "Mật khẩu",
                                leadingIcon = Icons.Default.Lock,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (passwordVisible) VisibilityIcon else VisibilityOffIcon,
                                        contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(24.dp).clickable { passwordVisible = !passwordVisible }
                                    )
                                }
                            )
                        }

                        if (currentMode == AuthMode.Register) {
                            HexTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = "Nhập lại mật khẩu",
                                leadingIcon = Icons.Default.Lock,
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }

                        error?.let { Text(it, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally)) }
                        message?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally)) }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(PointedShape())
                                .background(Brush.horizontalGradient(listOf(Color(0xFF0F766E), Color(0xFF0369A1))))
                                .clickable(enabled = !loading) {
                                    loading = true
                                    error = null
                                    message = null
                                    scope.launch {
                                        when (currentMode) {
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
                                                repository.forgotPassword(email)
                                                    .onSuccess {
                                                        message = it
                                                        mode = AuthMode.Login
                                                    }
                                                    .onFailure { error = it.message ?: "Không thể gửi link khôi phục." }
                                            }
                                            AuthMode.Reset -> {} 
                                        }
                                        loading = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (if (loading) "ĐANG XỬ LÝ..." else if (currentMode == AuthMode.Forgot) "GỬI LINK KHÔI PHỤC" else currentMode.action.uppercase()).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }

                        // Footer links
                        if (currentMode == AuthMode.Login) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = true, 
                                        onCheckedChange = {}, 
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0F766E), checkmarkColor = Color.White, uncheckedColor = Color.White.copy(alpha = 0.5f)),
                                        modifier = Modifier.padding(end = 4.dp).offset(x = (-8).dp)
                                    )
                                    Text("Lưu tài khoản", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.offset(x = (-8).dp))
                                }
                                Text(
                                    "Quên mật khẩu?", 
                                    color = Color.White, 
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.clickable { mode = AuthMode.Forgot }
                                )
                            }
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (currentMode == AuthMode.Login) {
                                    Text("Chưa có tài khoản? Đăng ký ngay", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { mode = AuthMode.Register })
                                } else if (currentMode == AuthMode.Register) {
                                    Text("Đã có tài khoản? Đăng nhập", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { mode = AuthMode.Login })
                                } else if (currentMode == AuthMode.Forgot) {
                                    Text("Quay lại đăng nhập", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { mode = AuthMode.Login })
                                }
                            }
                        }
                    }
                }
            }

            // Top Hexagon Logo Eyecatcher Animation
            val infiniteTransition = rememberInfiniteTransition(label = "eyecatcher")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounce"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = offsetY.dp)
                    .clip(HexagonShape())
                    .background(Color(0xFF064E3B)) // Dark Green Hexagon
                    .border(2.dp, Color(0xFF34D399).copy(alpha = glowAlpha), HexagonShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "App Logo",
                    tint = Color.White.copy(alpha = glowAlpha),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

private enum class AuthMode(val label: String, val action: String) {
    Login("Đăng nhập", "Login"),
    Register("Đăng ký", "Đăng ký"),
    Forgot("Quên mật khẩu", "Gửi OTP"),
    Reset("Đặt lại", "Đặt lại")
}

private fun authTitle(mode: AuthMode, step: Int): String = when (mode) {
    AuthMode.Login -> "Sign In"
    AuthMode.Register -> "Sign Up"
    AuthMode.Forgot -> if (step == 1) "Forgot Password" else "Reset Password"
    AuthMode.Reset -> "Reset Password"
}
