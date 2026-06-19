package com.example.myapplication.ui.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import com.example.myapplication.domain.util.formatCompactMoney

@Composable
internal fun DashboardScreen(
    repository: RentalRepository,
    session: UserSession?,
    onOpen: (AppScreen) -> Unit,
    onSessionExpired: () -> Unit
) {
    var state by remember { mutableStateOf<UiState<DashboardSummary>>(UiState.Loading) }

    fun load() {
        state = UiState.Loading
    }

    LaunchedEffect(session, state) {
        if (state is UiState.Loading) {
            state = repository.dashboard(session)
        }
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { summary, source ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeroCard(session, summary, source) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Phòng trọ", summary.totalRooms.toString(), "${summary.emptyRooms} phòng trống", Color(0xFF10B981), Modifier.weight(1f))
                    StatCard("Hóa đơn", summary.unpaidInvoices.toString(), "Hóa đơn chưa thu", Color(0xFFF59E0B), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Yêu cầu", summary.pendingTasks.toString(), "Yêu cầu cần xử lý", Color(0xFF3B82F6), Modifier.weight(1f))
                    StatCard("Doanh thu", formatCompactMoney(summary.revenue), "Tổng đã thanh toán", Color(0xFF8B5CF6), Modifier.weight(1f))
                }
            }
            item { SectionTitle("Tác vụ nhanh") }
            item { QuickActions(session?.role ?: UserRole.ChuTro, onOpen) }
        }
    }
}

@Composable
internal fun HeroCard(session: UserSession?, summary: DashboardSummary, source: DataSource) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4F46E5), Color(0xFF6D28D9), Color(0xFF1E1B4B))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Xin chào, ${session?.displayName ?: "bạn"} 👋",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Chào mừng bạn đến với hệ thống quản lý phòng trọ Local-First. Tất cả thay đổi được lưu trữ ngay trên thiết bị của bạn.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HeroMiniMetric("Nguồn", sourceLabel(source), Modifier.weight(1f))
                HeroMiniMetric("Phòng trống", summary.emptyRooms.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun HeroMiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        }
    }
}

@Composable
internal fun StatCard(title: String, value: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            Text(detail, color = accent, maxLines = 1, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun QuickActions(role: UserRole, onOpen: (AppScreen) -> Unit) {
    val actions = listOf(
        AppScreen.Rooms to if (role == UserRole.NguoiDung) "Tìm phòng" else "Quản lý phòng",
        AppScreen.Invoices to "Hóa đơn",
        (if (role == UserRole.NguoiDung) AppScreen.Payments else AppScreen.Electric) to if (role == UserRole.NguoiDung) "Gửi biên lai" else "Ghi điện",
        AppScreen.Incidents to "Báo sự cố"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
        items(actions) { action ->
            val accent = screenAccent(action.first)
            Surface(
                onClick = { onOpen(action.first) },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shadowElevation = 3.dp,
                modifier = Modifier.width(160.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Brush.horizontalGradient(listOf(accent, Color(0xFFF59E0B))))
                    )
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    action.first.shortCode,
                                    color = accent,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        }
                        Text(
                            action.second,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            action.first.label,
                            color = Color(0xFF64748B),
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
