package com.example.myapplication.ui.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.repository.RentalRepository
import com.example.myapplication.domain.model.AppScreen
import com.example.myapplication.domain.model.DashboardSummary
import com.example.myapplication.domain.model.UiState
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import com.example.myapplication.domain.util.formatCompactMoney

val CardBgColor = Color(0xFF1E293B)
val BrownAlert = Color(0xFF92400E)
val BlueAlert = Color(0xFF1D4ED8)
val RedAlert = Color(0xFFB91C1C)
val PurpleAlert = Color(0xFF6D28D9)

@Composable
internal fun DashboardScreen(
    repository: RentalRepository,
    session: UserSession?,
    onOpen: (AppScreen) -> Unit,
    onSessionExpired: () -> Unit
) {
    var state by remember(session) { mutableStateOf<UiState<DashboardSummary>>(UiState.Loading) }

    fun load() { state = UiState.Loading }

    LaunchedEffect(session, state) {
        if (state is UiState.Loading) state = repository.dashboard(session)
    }

    StateContainer(state = state, onRetry = { load() }, onSessionExpired = onSessionExpired) { summary, _ ->
        val isLandlord = session?.role == UserRole.ChuTro || session?.role == UserRole.Admin
        if (isLandlord) {
            LandlordDashboard(session, summary, onOpen)
        } else {
            TenantDashboard(session, summary, onOpen)
        }
    }
}

@Composable
internal fun LandlordDashboard(session: UserSession?, summary: DashboardSummary, onOpen: (AppScreen) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgGradient).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AlertShortcut(
                    icon = Icons.Default.Description, title = "Hóa đơn chưa thu", count = summary.unpaidInvoices,
                    bgColor = BrownAlert, modifier = Modifier.weight(1f)
                ) { onOpen(AppScreen.Invoices) }
                AlertShortcut(
                    icon = Icons.Default.PriorityHigh, title = "Yêu cầu chờ duyệt", count = summary.pendingRentRequests,
                    bgColor = BlueAlert, modifier = Modifier.weight(1f)
                ) { onOpen(AppScreen.RentRequests) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AlertShortcut(
                    icon = Icons.Default.Warning, title = "Sự cố chưa xử lý", count = summary.unresolvedIncidents,
                    bgColor = RedAlert, modifier = Modifier.weight(1f)
                ) { onOpen(AppScreen.Incidents) }
                AlertShortcut(
                    icon = Icons.Default.Notifications, title = "Thông báo mới", count = summary.unreadNotices,
                    bgColor = PurpleAlert, modifier = Modifier.weight(1f)
                ) { onOpen(AppScreen.Notices) }
            }
        }
        
        item { SimpleHeroCard(session, summary, true) }
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DonutChartCard(
                    title = "Tình trạng phòng",
                    total = summary.totalRooms,
                    value1 = summary.emptyRooms, label1 = "Còn trống", color1 = Color(0xFF10B981),
                    value2 = summary.rentedRooms, label2 = "Đã thuê", color2 = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                DonutChartCard(
                    title = "Tình trạng hóa đơn",
                    total = summary.paidInvoices + summary.unpaidInvoices,
                    value1 = summary.paidInvoices, label1 = "Đã thu", color1 = Color(0xFF10B981),
                    value2 = summary.unpaidInvoices, label2 = "Chưa thu", color2 = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun TenantDashboard(session: UserSession?, summary: DashboardSummary, onOpen: (AppScreen) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgGradient).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TenantStatCard(icon = Icons.Default.MeetingRoom, title = "Phòng đang thuê", value = summary.rentedRooms.toString(), onOpen = { onOpen(AppScreen.Rooms) }, modifier = Modifier.weight(1f))
                TenantStatCard(icon = Icons.Default.Description, title = "Hóa đơn chưa trả", value = summary.unpaidInvoices.toString(), onOpen = { onOpen(AppScreen.Invoices) }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TenantStatCard(icon = Icons.Default.Notifications, title = "Thông báo", value = "${summary.unreadNotices} chưa đọc", onOpen = { onOpen(AppScreen.Notices) }, modifier = Modifier.weight(1f))
                TenantStatCard(icon = Icons.Default.PriorityHigh, title = "Yêu cầu thuê", value = "${summary.pendingRentRequests} chờ", onOpen = { onOpen(AppScreen.RentRequests) }, modifier = Modifier.weight(1f))
            }
        }
        item {
            TenantStatCard(icon = Icons.Default.Warning, title = "Sự cố", value = "${summary.unresolvedIncidents} cần xử lý", onOpen = { onOpen(AppScreen.Incidents) }, modifier = Modifier.fillMaxWidth())
        }

        item { SimpleHeroCard(session, summary, false) }
    }
}

@Composable
internal fun AlertShortcut(icon: ImageVector, title: String, count: Int, bgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val alphaBg = bgColor.copy(alpha = 0.85f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(alphaBg)
            .border(1.dp, bgColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(title, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (count == 0) "Ổn định" else "Cần xử lý", fontSize = 10.sp, color = if (count == 0) Color(0xFF34D399) else Color(0xFFFBBF24))
            }
        }
    }
}

@Composable
internal fun SimpleHeroCard(session: UserSession?, summary: DashboardSummary, isLandlord: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppGreenDark)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(
                        "Xin chào, ${session?.displayName ?: "bạn"} 👋",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
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
            
            if (isLandlord) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Tổng doanh thu: ${formatCompactMoney(summary.revenue)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(if(summary.revenue > 0) 1f else 0f).fillMaxHeight().background(AppGreenLight))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    val totalInv = summary.paidInvoices + summary.unpaidInvoices
                    val prog = if (totalInv > 0) (summary.paidInvoices.toFloat() / totalInv) else 1f
                    Text("Tiến độ thanh toán hóa đơn: ${(prog * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(prog).fillMaxHeight().background(AppGreenLight))
                    }
                }
            }
        }
    }
}

@Composable
internal fun DonutChartCard(title: String, total: Int, value1: Int, label1: String, color1: Color, value2: Int, label2: String, color2: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBgColor)
            .padding(16.dp)
            .height(180.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 20f
                    if (total == 0) {
                        drawArc(color = Color.DarkGray, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    } else {
                        val angle1 = (value1.toFloat() / total) * 360f
                        drawArc(color = color1, startAngle = -90f, sweepAngle = angle1, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        drawArc(color = color2, startAngle = -90f + angle1, sweepAngle = 360f - angle1, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(total.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Tổng", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color1))
                    Text(label1, color = Color.White.copy(alpha=0.8f), fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color2))
                    Text(label2, color = Color.White.copy(alpha=0.8f), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
internal fun TenantStatCard(icon: ImageVector, title: String, value: String, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBgColor)
            .clickable { onOpen() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = AppGreenLight, modifier = Modifier.size(18.dp))
            }
            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
