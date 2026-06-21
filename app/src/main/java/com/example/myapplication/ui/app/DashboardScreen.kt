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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.DataSource
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import com.example.myapplication.domain.util.formatCompactMoney

// Colors are now imported from CommonComponents.kt

@Composable
internal fun DashboardScreen(
    repository: RentalRepository,
    session: UserSession?,
    onOpen: (AppScreen) -> Unit,
    onSessionExpired: () -> Unit
) {
    var state by remember { mutableStateOf<UiState<DashboardSummary>>(UiState.Loading) }

    fun load() { state = UiState.Loading }

    LaunchedEffect(session, state) {
        if (state is UiState.Loading) state = repository.dashboard(session)
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { summary, source ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(BgGradient).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeroCard(session, summary, source) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Phòng trọ", summary.totalRooms.toString(), "${summary.emptyRooms} phòng trống", AppGreenLight, Modifier.weight(1f))
                    StatCard("Hóa đơn", summary.unpaidInvoices.toString(), "Hóa đơn chưa thu", Color(0xFFFBBF24), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Yêu cầu", summary.pendingTasks.toString(), "Yêu cầu cần xử lý", Color(0xFF60A5FA), Modifier.weight(1f))
                    StatCard("Doanh thu", formatCompactMoney(summary.revenue), "Tổng đã thanh toán", Color(0xFFC084FC), Modifier.weight(1f))
                }
            }
            item { SectionTitle("Tác vụ nhanh") }
            item { QuickActions(session?.role ?: UserRole.ChuTro, onOpen) }
        }
    }
}

@Composable
internal fun HeroCard(session: UserSession?, summary: DashboardSummary, source: DataSource) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(24.dp))
            .background(AppCardBg)
            .border(2.dp, AppGreenLight.copy(alpha = shimmer * 0.6f), CutCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppLogo(size = 44)
                Column {
                    Text(
                        "Xin chào, ${session?.displayName ?: "bạn"} 👋",
                        color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                    )
                    Text(session?.role?.label ?: "", color = AppGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                "Giải pháp quản lý nhà trọ đơn giản và hiệu quả. Dữ liệu của bạn luôn được bảo mật và đồng bộ.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp
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
    Box(
        modifier = modifier
            .clip(CutCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CutCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = AppGreenLight.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun StatCard(title: String, value: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CutCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.4f), CutCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CutCornerShape(2.dp)).background(accent))
                Text(title, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1)
            }
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(detail, color = accent, maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .clip(CutCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.45f), CutCornerShape(16.dp))
                    .clickable { onOpen(action.first) }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Brush.horizontalGradient(listOf(accent, AppGreenLight)))
                    )
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CutCornerShape(6.dp), color = accent.copy(alpha = 0.2f)) {
                            Icon(
                                imageVector = action.first.getIcon(),
                                contentDescription = action.second,
                                tint = accent,
                                modifier = Modifier.padding(8.dp).size(24.dp)
                            )
                        }
                        Text(action.second, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(action.first.label, color = AppGreenLight.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
