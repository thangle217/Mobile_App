package com.example.myapplication.ui.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.RentalItem
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF7C3AED), // Indigo accent
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
            )
        }
    }
}

@Composable
internal fun ErrorState(message: String, onRetry: () -> Unit, canRetry: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚠️", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Không tải được dữ liệu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (canRetry) "Thử lại" else "Đăng nhập lại", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📂", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun AppHeader(title: String, sourceLabel: String, onMenu: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)))) // Indigo to Cyan gradient
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onMenu) {
            Text("Menu", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                sourceLabel,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
        }
        TextButton(onClick = onLogout) {
            Text("Thoát", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun AppBottomBar(items: List<AppScreen>, selected: AppScreen, onSelected: (AppScreen) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val active = selected == item
                val accent = screenAccent(item)
                Surface(
                    onClick = { onSelected(item) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) accent.copy(alpha = 0.12f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.3f) else Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) accent else accent.copy(alpha = 0.08f)
                        ) {
                            Text(
                                item.shortCode,
                                color = if (active) Color.White else accent,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            item.label,
                            color = if (active) accent else Color(0xFF64748B),
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
internal fun AppLogo(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("RT", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun StatusPill(status: String) {
    Surface(
        color = statusColor(status).copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, statusColor(status).copy(alpha = 0.18f))
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
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(Modifier.size(width = 4.dp, height = 22.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF59E0B)))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
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
    UserRole.NguoiDung -> screen in setOf(
        AppScreen.Payments,
        AppScreen.Incidents
    )
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
    status.contains("Đã", true) || status.contains("Còn trống", true) || status.contains("Đang sử dụng", true) -> Color(0xFF10B981)
    status.contains("Chờ", true) || status.contains("Chưa", true) || status.contains("Một phần", true) || status.contains("Sắp", true) || status.contains("Cần", true) -> Color(0xFFF59E0B)
    status.contains("Từ chối", true) || status.contains("Hủy", true) || status.contains("Tạm dừng", true) -> Color(0xFFEF4444)
    status.contains("Đang", true) || status.contains("Mới", true) -> Color(0xFF3B82F6)
    else -> Color(0xFF64748B)
}

internal fun screenAccent(screen: AppScreen): Color = when (screen) {
    AppScreen.Dashboard -> Color(0xFF6366F1)
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
    AppScreen.Incidents -> Color(0xFFEF4444)
    AppScreen.Notices -> Color(0xFF3B82F6)
    AppScreen.Users -> Color(0xFF64748B)
    AppScreen.Account -> Color(0xFF0F766E)
}

internal fun RentalItem.detail(key: String): String = details.firstOrNull { it.first == key }?.second.orEmpty()

internal data class ConfirmData(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)
