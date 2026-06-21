package com.example.myapplication.ui.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole

// ─── Design Tokens (matching Auth screen palette) ───────────────────────────
private val GreenDark @Composable get() = if (LocalAppThemeIsLight.current) Color(0xFF059669) else Color(0xFF064E3B)
private val GreenMid @Composable get() = if (LocalAppThemeIsLight.current) Color(0xFF10B981) else Color(0xFF0F766E)
private val GreenLight @Composable get() = if (LocalAppThemeIsLight.current) Color(0xFF6EE7B7) else Color(0xFF34D399)
private val TealAccent @Composable get() = if (LocalAppThemeIsLight.current) Color(0xFF0284C7) else Color(0xFF0369A1)
private val BgGradient @Composable get() = Brush.verticalGradient(listOf(GreenMid, GreenDark))
private val CardBg @Composable get() = Color.White.copy(alpha = if (LocalAppThemeIsLight.current) 0.25f else 0.18f)
private val CardBorder @Composable get() = Color.White.copy(alpha = if (LocalAppThemeIsLight.current) 0.5f else 0.35f)
@Composable
internal fun <T> StateContainer(
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
internal fun FullScreenLoading(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(BgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CircularProgressIndicator(color = GreenLight, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            Text(message, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

@Composable
internal fun ErrorState(message: String, onRetry: () -> Unit, canRetry: Boolean = true) {
    Box(
        modifier = Modifier.fillMaxSize().background(BgGradient).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(CutCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, CutCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("⚠️", fontSize = 36.sp)
            Text("Không tải được dữ liệu", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Text(message, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center, fontSize = 13.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CutCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(GreenMid, TealAccent)))
                    .clickable { onRetry() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (canRetry) "Thử lại" else "Đăng nhập lại", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
internal fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(BgGradient).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(CutCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, CutCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📂", fontSize = 36.sp)
            Text(message, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun AppHeader(title: String, onMenu: () -> Unit, onLogout: () -> Unit, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(GreenDark, GreenMid)))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CutCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), CutCornerShape(8.dp))
                .clickable { onMenu() }
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        
        Spacer(Modifier.width(12.dp))
        
        Text(
            title, color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.width(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CutCornerShape(8.dp))
                    .clickable { onToggleTheme() }
                    .padding(8.dp)
            ) {
                Text(if (LocalAppThemeIsLight.current) "☀️" else "🌙", fontSize = 14.sp)
            }
            
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CutCornerShape(8.dp))
                    .clickable { onLogout() }
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Thoát", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun AppBottomBar(items: List<AppScreen>, selected: AppScreen, onSelected: (AppScreen) -> Unit) {
    Surface(
        color = GreenDark,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, GreenLight.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val active = selected == item
                val accent = screenAccent(item)
                Surface(
                    onClick = { onSelected(item) },
                    modifier = Modifier.weight(1f),
                    shape = CutCornerShape(10.dp),
                    color = if (active) GreenLight.copy(alpha = 0.2f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) GreenLight.copy(alpha = 0.6f) else Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            shape = CutCornerShape(6.dp),
                            color = if (active) GreenLight else Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                item.shortCode,
                                color = if (active) GreenDark else Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            item.label,
                            color = if (active) GreenLight else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppLogo(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CutCornerShape((size * 0.28f).dp))
            .background(Brush.linearGradient(listOf(GreenMid, GreenDark)))
            .border(1.dp, GreenLight.copy(alpha = 0.5f), CutCornerShape((size * 0.28f).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Home, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
internal fun StatusPill(status: String) {
    Surface(
        color = statusColor(status).copy(alpha = 0.18f),
        shape = CutCornerShape(6.dp),
        border = BorderStroke(1.dp, statusColor(status).copy(alpha = 0.3f))
    ) {
        Text(
            text = status,
            color = statusColor(status),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = GreenLight.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.Medium, color = Color.White, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(1f, false))
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.size(width = 4.dp, height = 20.dp).clip(RoundedCornerShape(4.dp)).background(GreenLight))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

internal fun screensForRole(role: UserRole): List<AppScreen> = when (role) {
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

internal fun bottomScreens(role: UserRole): List<AppScreen> = when (role) {
    UserRole.NguoiDung -> listOf(AppScreen.Dashboard, AppScreen.Rooms, AppScreen.Invoices, AppScreen.Notices, AppScreen.Account)
    else -> listOf(AppScreen.Dashboard, AppScreen.Rooms, AppScreen.Invoices, AppScreen.Payments, AppScreen.Account)
}

internal fun canManageScreen(role: UserRole, screen: AppScreen): Boolean = when (role) {
    UserRole.Admin -> screen != AppScreen.Account
    UserRole.ChuTro -> screen in setOf(
        AppScreen.Houses, AppScreen.RoomTypes, AppScreen.Rooms, AppScreen.Tenants,
        AppScreen.Contracts, AppScreen.Invoices, AppScreen.Payments, AppScreen.Services,
        AppScreen.ServiceRegs, AppScreen.Electric, AppScreen.Water, AppScreen.RentRequests,
        AppScreen.RenewRequests, AppScreen.Incidents, AppScreen.Notices
    )
    UserRole.NguoiDung -> screen in setOf(AppScreen.Payments, AppScreen.Incidents)
}

internal fun sourceLabel(source: DataSource): String = when (source) {
    DataSource.Api -> "API"
    DataSource.Demo -> "Demo"
    DataSource.Local -> "Trong app"
}

internal fun defaultStatus(screen: AppScreen): String = when (screen) {
    AppScreen.Rooms -> "Còn trống"
    AppScreen.Invoices -> "Chưa thanh toán"
    AppScreen.Payments -> "Chờ xác nhận"
    AppScreen.RentRequests, AppScreen.RenewRequests -> "Chờ duyệt"
    AppScreen.Incidents, AppScreen.Notices -> "Mới"
    AppScreen.Users -> "Đang hoạt động"
    else -> "Đang sử dụng"
}

internal fun statusColor(status: String): Color = when {
    status.contains("Đã", true) || status.contains("Còn trống", true) || status.contains("Đang sử dụng", true) -> Color(0xFF34D399)
    status.contains("Chờ", true) || status.contains("Chưa", true) || status.contains("Một phần", true) || status.contains("Sắp", true) || status.contains("Cần", true) -> Color(0xFFFBBF24)
    status.contains("Từ chối", true) || status.contains("Hủy", true) || status.contains("Tạm dừng", true) -> Color(0xFFF87171)
    status.contains("Đang", true) || status.contains("Mới", true) -> Color(0xFF60A5FA)
    else -> Color(0xFF94A3B8)
}

internal fun screenAccent(screen: AppScreen): Color = when (screen) {
    AppScreen.Dashboard -> Color(0xFF34D399)
    AppScreen.Houses -> Color(0xFF0D9488)
    AppScreen.RoomTypes -> Color(0xFF06B6D4)
    AppScreen.Rooms -> Color(0xFF0891B2)
    AppScreen.Tenants -> Color(0xFF10B981)
    AppScreen.Contracts -> Color(0xFF818CF8)
    AppScreen.Invoices -> Color(0xFFFBBF24)
    AppScreen.Payments -> Color(0xFFFB923C)
    AppScreen.Services -> Color(0xFF2DD4BF)
    AppScreen.ServiceRegs -> Color(0xFFA3E635)
    AppScreen.Electric -> Color(0xFFFDE047)
    AppScreen.Water -> Color(0xFF38BDF8)
    AppScreen.RentRequests -> Color(0xFFF472B6)
    AppScreen.RenewRequests -> Color(0xFFC084FC)
    AppScreen.Incidents -> Color(0xFFF87171)
    AppScreen.Notices -> Color(0xFF60A5FA)
    AppScreen.Users -> Color(0xFF94A3B8)
    AppScreen.Account -> Color(0xFF34D399)
}

internal fun RentalItem.detail(key: String): String = details.firstOrNull { it.first == key }?.second.orEmpty()

internal data class ConfirmData(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)
