package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PriceAlert
import com.example.ui.theme.BorderGrey
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TradingFall
import com.example.ui.theme.TradingRise
import kotlinx.coroutines.flow.collectLatest
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.roundToInt

// --- FORMATTING HELPERS ---

fun formatKrw(value: Double): String {
    return DecimalFormat("₩#,###").format(value)
}

fun formatUsd(value: Double): String {
    return DecimalFormat("$#,###.##").format(value)
}

fun formatPercent(value: Double): String {
    val sign = if (value > 0.0) "+" else ""
    return sign + String.format(Locale.US, "%.2f%%", value)
}

// --- MAIN DASHBOARD SCREEN ---

@Composable
fun CoinDashboard(
    viewModel: CoinViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedCoin by viewModel.selectedCoin.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val tickerState by viewModel.tickerState.collectAsState()
    val chartState by viewModel.chartState.collectAsState()
    val alerts by viewModel.allAlerts.collectAsState()

    // Trigger visual toast-like alert inside the app
    var activeTriggeredAlertMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.triggeredAlertEvent.collectLatest { alert ->
            val currencySymbol = if (alert.isKrw) "KRW" else "USD"
            val direction = if (alert.isAbove) "이상" else "이하"
            val priceStr = if (alert.isKrw) formatKrw(alert.targetPrice) else formatUsd(alert.targetPrice)
            activeTriggeredAlertMsg = "🚨 [알림] ${alert.coinCode}가 목표가 ${priceStr} ${currencySymbol} ${direction}에 도달했습니다!"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.gridDp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE MARKET",
                        color = GoldPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "BitPulse",
                        color = TextWhite,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshAll() },
                    modifier = Modifier
                        .testTag("refresh_button")
                        .background(SurfaceCard, RoundedCornerShape(50))
                        .border(1.dp, BorderGrey, RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Triggered Alert Banner
            activeTriggeredAlertMsg?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { activeTriggeredAlertMsg = null },
                    colors = CardDefaults.cardColors(containerColor = PremiumGold.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PremiumGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "알림",
                            tint = PremiumGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            color = TextWhite,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "닫기",
                            color = PremiumGold,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // --- COIN SELECTOR TABS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(24.dp))
                    .border(1.dp, BorderGrey, RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SupportedCoins.forEach { coin ->
                    val isSelected = selectedCoin.code == coin.code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) GoldSecondary else Color.Transparent)
                            .clickable { viewModel.selectCoin(coin) }
                            .padding(vertical = 10.dp)
                            .testTag("coin_tab_${coin.code.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = coin.nameKo,
                            color = if (isSelected) GoldPrimary else TextGrey,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- MAIN TICKER CONTENT ---
            when (val state = tickerState) {
                is TickerState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("실시간 시세 로딩 중...", color = TextGrey)
                    }
                }
                is TickerState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TradingRise.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = state.message,
                            color = TradingRise,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is TickerState.Success -> {
                    PriceOverviewSection(state, selectedCoin)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CHART SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Time Range Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "시황 역사 차트",
                            color = TextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ChartRange.values().forEach { range ->
                                val isRangeSelected = selectedRange == range
                                val label = when (range) {
                                    ChartRange.H24 -> "24H"
                                    ChartRange.W1 -> "1W"
                                    ChartRange.M1 -> "1M"
                                    ChartRange.Y1 -> "1Y"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isRangeSelected) GoldSecondary else Color(0xFFF3F3F7))
                                        .clickable { viewModel.selectRange(range) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("chart_range_${range.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isRangeSelected) GoldPrimary else TextGrey,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (val cState = chartState) {
                        is ChartState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("차트 데이터를 불러오는 중...", color = TextGrey)
                            }
                        }
                        is ChartState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cState.message, color = TradingRise)
                            }
                        }
                        is ChartState.Success -> {
                            BitcoinChart(points = cState.points)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CONVERTER SECTION ---
            if (tickerState is TickerState.Success) {
                ConverterCard(viewModel, selectedCoin)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- PRICE ALERTS SECTION ---
            PriceAlertsCard(viewModel, alerts, selectedCoin)
        }
    }
}

// Custom Grid dp to ensure direct clean layout safely
private val Int.gridDp get() = this.dp

// --- PRICE OVERVIEW PANEL ---

@Composable
fun PriceOverviewSection(
    state: TickerState.Success,
    coin: CoinInfo
) {
    val isRise = state.upbitChangeRate24h >= 0.0
    val trendColor = if (isRise) TradingRise else TradingFall

    Column {
        // Upbit Card: E0E2ED background, 32dp rounded corners, shadow-sm
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E2ED)),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header of Upbit Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulse Red dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(TradingRise)
                        )
                        Text(
                            text = "업비트 (국내)",
                            color = Color(0xFF44474E),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${coin.code}/KRW",
                            color = Color(0xFF44474E),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Upbit Huge Price
                Text(
                    text = formatKrw(state.upbitPrice),
                    color = Color(0xFF001945), // Deep navy from mockup
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Rate change detail
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isRise) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "변동",
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatPercent(state.upbitChangeRate24h),
                        color = trendColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "▲ 24h 시세 추이",
                        color = Color(0xFF44474E),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Binance & Kimchi Premium Card: White background, 32dp rounded corners, border E1E2EC, shadow-sm
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Binance Left Pane
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "바이낸스 (해외)",
                        color = TextGrey,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatUsd(state.binancePrice),
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(BorderGrey)
                )

                // Kimchi Premium Right Pane
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "김치 프리미엄",
                        color = TextGrey,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%+2.2f%%", state.kimchiPremiumPercent),
                        color = GoldPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- INTERACTIVE PRICE CHART WITH DRAG CROSSHAIR ---

@Composable
fun BitcoinChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("데이터가 부족합니다", color = TextGrey)
        }
        return
    }

    var activeIndex by remember(points) { mutableStateOf(-1) }

    val prices = points.map { it.price }
    val maxPrice = prices.maxOrNull() ?: 1.0
    val minPrice = prices.minOrNull() ?: 0.0
    val priceRange = if (maxPrice == minPrice) 1.0 else maxPrice - minPrice

    Column(modifier = modifier) {
        // Tracker details above chart
        if (activeIndex != -1 && activeIndex < points.size) {
            val pt = points[activeIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "선택: ${formatKrw(pt.price)}",
                    color = GoldSecondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = pt.label,
                    color = TextGrey,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "차트를 스크롤하여 상세 지점을 확인하세요",
                    color = TextGrey,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "최고: ${formatKrw(maxPrice)}",
                    color = TradingRise,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = (offset.x / (size.width / (points.size - 1)))
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            activeIndex = index
                        },
                        onDrag = { change, _ ->
                            val index = (change.position.x / (size.width / (points.size - 1)))
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            activeIndex = index
                        },
                        onDragEnd = { activeIndex = -1 },
                        onDragCancel = { activeIndex = -1 }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (points.size - 1)

            val coords = points.mapIndexed { i, pt ->
                val x = i * stepX
                val y = height - (((pt.price - minPrice) / priceRange) * height).toFloat()
                Offset(x, y)
            }

            // Fill transparent glow
            val fillPath = Path().apply {
                moveTo(0f, height)
                coords.forEach { lineTo(it.x, it.y) }
                lineTo(width, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(GoldPrimary.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line path
            val linePath = Path().apply {
                coords.forEachIndexed { index, offset ->
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
            }
            drawPath(
                path = linePath,
                color = GoldPrimary,
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )

            // Active dragging indicators
            if (activeIndex != -1 && activeIndex < coords.size) {
                val pt = coords[activeIndex]
                // Crosshair line
                drawLine(
                    color = TextGrey.copy(alpha = 0.5f),
                    start = Offset(pt.x, 0f),
                    end = Offset(pt.x, height),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 12.dp.toPx()), 0f)
                )

                // Spot glowing dots
                drawCircle(
                    color = GoldSecondary.copy(alpha = 0.4f),
                    radius = 16.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = GoldPrimary,
                    radius = 6.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

// --- CONVERTER CARD ---

@Composable
fun ConverterCard(
    viewModel: CoinViewModel,
    coin: CoinInfo
) {
    val btcVal by viewModel.calcBtcStr.collectAsState()
    val krwVal by viewModel.calcKrwStr.collectAsState()
    val usdVal by viewModel.calcUsdStr.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "실시간 계산기 (BTC/KRW/USD)",
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "한 곳을 수정하면 연동되어 실시간으로 변환 계산됩니다.",
                color = TextGrey,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Crypto Amount Input
            OutlinedTextField(
                value = btcVal,
                onValueChange = { viewModel.onBtcAmountChanged(it) },
                label = { Text("${coin.code} 수량") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_btc_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderGrey,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = TextGrey,
                    focusedContainerColor = Color(0xFFF3F3F7),
                    unfocusedContainerColor = Color(0xFFF3F3F7)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // KRW Input
            OutlinedTextField(
                value = krwVal,
                onValueChange = { viewModel.onKrwAmountChanged(it) },
                label = { Text("원화 금액 (KRW)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_krw_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderGrey,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = TextGrey,
                    focusedContainerColor = Color(0xFFF3F3F7),
                    unfocusedContainerColor = Color(0xFFF3F3F7)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // USD Input
            OutlinedTextField(
                value = usdVal,
                onValueChange = { viewModel.onUsdAmountChanged(it) },
                label = { Text("달러 금액 (USD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_usd_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderGrey,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = TextGrey,
                    focusedContainerColor = Color(0xFFF3F3F7),
                    unfocusedContainerColor = Color(0xFFF3F3F7)
                )
            )
        }
    }
}

// --- PRICE ALERTS LIST AND MAKER ---

@Composable
fun PriceAlertsCard(
    viewModel: CoinViewModel,
    alerts: List<PriceAlert>,
    coin: CoinInfo
) {
    var priceInput by remember { mutableStateOf("") }
    var isKrwAlert by remember { mutableStateOf(true) }
    var isAboveAlert by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "시세 알림 설정 (Price Alert)",
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "목표 시세 도달 시 실시간으로 앱 상단에서 알림을 제공합니다.",
                color = TextGrey,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target price input
            OutlinedTextField(
                value = priceInput,
                onValueChange = { priceInput = it },
                label = { Text("알림 목표가 입력") },
                placeholder = { Text(if (isKrwAlert) "예: 90000000" else "예: 62500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert_price_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderGrey,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = TextGrey,
                    focusedContainerColor = Color(0xFFF3F3F7),
                    unfocusedContainerColor = Color(0xFFF3F3F7)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mode switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isKrwAlert) "원화 (KRW) 기준" else "달러 (USD) 기준",
                        color = TextWhite,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isKrwAlert,
                        onCheckedChange = { isKrwAlert = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GoldPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = BorderGrey
                        )
                    )
                }

                // Direction Switch (Above / Below)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAboveAlert) "이상 (↑)" else "이하 (↓)",
                        color = TextWhite,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isAboveAlert,
                        onCheckedChange = { isAboveAlert = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GoldPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = BorderGrey
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add button
            Button(
                onClick = {
                    val p = priceInput.toDoubleOrNull()
                    if (p != null && p > 0.0) {
                        viewModel.addAlert(p, isKrwAlert, isAboveAlert)
                        priceInput = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("alert_add_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "추가", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "시세 알림 등록", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alerts list
            if (alerts.isEmpty()) {
                Text(
                    text = "등록된 알림이 없습니다.",
                    color = TextGrey,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "등록된 알림 목록",
                    color = TextWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                alerts.forEach { alert ->
                    val alertPrice = if (alert.isKrw) formatKrw(alert.targetPrice) else formatUsd(alert.targetPrice)
                    val dirText = if (alert.isAbove) "이상" else "이하"
                    val unit = if (alert.isKrw) "KRW" else "USD"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFFF3F3F7), RoundedCornerShape(16.dp))
                            .border(1.dp, BorderGrey, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (alert.isActive) GoldPrimary else TextGrey)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = alert.coinCode,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (alert.isActive) "대기 중" else "도달 완료",
                                    color = if (alert.isActive) GoldSecondary else TextGrey,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "목표가: ${alertPrice} (${unit}) ${dirText}",
                                color = TextWhite.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteAlert(alert.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = TradingRise,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
