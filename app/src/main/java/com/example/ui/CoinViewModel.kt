package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CoinRepository
import com.example.data.UpbitCandleDto
import com.example.data.database.PriceAlert
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- UI CONFIG & SELECTIONS ---

data class CoinInfo(
    val code: String,         // "BTC"
    val nameKo: String,       // "비트코인"
    val nameEn: String,       // "Bitcoin"
    val upbitMarket: String,  // "KRW-BTC"
    val binanceSymbol: String // "BTCUSDT"
)

val SupportedCoins = listOf(
    CoinInfo("BTC", "비트코인", "Bitcoin", "KRW-BTC", "BTCUSDT"),
    CoinInfo("ETH", "이더리움", "Ethereum", "KRW-ETH", "ETHUSDT"),
    CoinInfo("XRP", "리플", "Ripple", "KRW-XRP", "XRPUSDT")
)

enum class ChartRange {
    H24, W1, M1, Y1
}

// --- UI STATE MODELS ---

sealed interface TickerState {
    object Loading : TickerState
    data class Success(
        val upbitPrice: Double,
        val upbitChangeRate24h: Double, // %
        val upbitChangePrice24h: Double,
        val upbitVolume24h: Double,
        val upbitHighPrice: Double,
        val upbitLowPrice: Double,
        
        val binancePrice: Double,
        val binanceChangeRate24h: Double, // %
        val binanceHighPrice: Double,
        val binanceLowPrice: Double,
        val binanceVolume24h: Double,
        
        val exchangeRate: Double,
        val kimchiPremiumPercent: Double
    ) : TickerState
    data class Error(val message: String) : TickerState
}

sealed interface ChartState {
    object Loading : ChartState
    data class Success(val points: List<ChartPoint>) : ChartState
    data class Error(val message: String) : ChartState
}

data class ChartPoint(
    val timestamp: Long,
    val price: Double,
    val label: String
)

// --- VIEWMODEL ---

class CoinViewModel(private val repository: CoinRepository) : ViewModel() {

    // States
    private val _selectedCoin = MutableStateFlow(SupportedCoins[0])
    val selectedCoin: StateFlow<CoinInfo> = _selectedCoin.asStateFlow()

    private val _selectedRange = MutableStateFlow(ChartRange.H24)
    val selectedRange: StateFlow<ChartRange> = _selectedRange.asStateFlow()

    private val _tickerState = MutableStateFlow<TickerState>(TickerState.Loading)
    val tickerState: StateFlow<TickerState> = _tickerState.asStateFlow()

    private val _chartState = MutableStateFlow<ChartState>(ChartState.Loading)
    val chartState: StateFlow<ChartState> = _chartState.asStateFlow()

    // Database flow exposed to UI
    val allAlerts: StateFlow<List<PriceAlert>> = repository.allAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Alert notification event
    private val _triggeredAlertEvent = MutableSharedFlow<PriceAlert>()
    val triggeredAlertEvent: SharedFlow<PriceAlert> = _triggeredAlertEvent.asSharedFlow()

    // Calculator inputs
    val calcBtcStr = MutableStateFlow("")
    val calcKrwStr = MutableStateFlow("")
    val calcUsdStr = MutableStateFlow("")

    private var pollingJob: Job? = null
    private var chartJob: Job? = null

    init {
        startPolling()
        loadChartData()
    }

    fun selectCoin(coin: CoinInfo) {
        if (_selectedCoin.value == coin) return
        _selectedCoin.value = coin
        clearCalculator()
        refreshAll()
    }

    fun selectRange(range: ChartRange) {
        if (_selectedRange.value == range) return
        _selectedRange.value = range
        loadChartData()
    }

    fun refreshAll() {
        viewModelScope.launch {
            fetchTickerData()
            loadChartData()
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchTickerData()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    private suspend fun fetchTickerData() {
        val coin = _selectedCoin.value
        try {
            // Fetch in parallel using supervisorScope so one failure doesn't cancel the others
            val (upbitList, binanceResult, exchangeResult) = kotlinx.coroutines.supervisorScope {
                val upbitDeferred = async {
                    try {
                        repository.fetchUpbitTickers(coin.upbitMarket)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val binanceDeferred = async {
                    try {
                        repository.fetchBinanceTicker(coin.binanceSymbol)
                    } catch (e: Exception) {
                        // If Binance fails (e.g. 451 blocked), try Upbit's USDT ticker as fallback
                        try {
                            val usdtMarket = "USDT-${coin.code}"
                            val tickers = repository.fetchUpbitTickers(usdtMarket)
                            if (tickers.isNotEmpty()) {
                                val usdtTicker = tickers[0]
                                com.example.data.BinanceTickerDto(
                                    symbol = coin.binanceSymbol,
                                    lastPrice = usdtTicker.tradePrice.toString(),
                                    priceChangePercent = (usdtTicker.signedChangeRate * 100.0).toString(),
                                    highPrice = usdtTicker.highPrice.toString(),
                                    lowPrice = usdtTicker.lowPrice.toString(),
                                    volume = usdtTicker.tradeVolume.toString(),
                                    quoteVolume = usdtTicker.accTradeVolume24h.toString()
                                )
                            } else {
                                null
                            }
                        } catch (fallbackEx: Exception) {
                            null
                        }
                    }
                }
                val exchangeDeferred = async {
                    try {
                        repository.fetchExchangeRate()
                    } catch (e: Exception) {
                        null
                    }
                }

                Triple(upbitDeferred.await(), binanceDeferred.await(), exchangeDeferred.await())
            }

            if (upbitList.isNotEmpty()) {
                val upbit = upbitList[0]
                val upbitPrice = upbit.tradePrice
                val binancePrice = binanceResult?.lastPrice?.toDoubleOrNull() ?: 0.0
                val exchangeRate = exchangeResult?.rates?.get("KRW") ?: 1380.0 // Default fallback

                // Calculate Kimchi Premium: Upbit (KRW) vs Binance (USD converted to KRW)
                // Kimchi Premium = ((Upbit Price) / (Binance Price * Exchange Rate) - 1) * 100
                val kimchiPremiumPercent = if (binancePrice > 0.0 && exchangeRate > 0.0) {
                    ((upbitPrice / (binancePrice * exchangeRate)) - 1.0) * 100.0
                } else {
                    0.0
                }

                val state = TickerState.Success(
                    upbitPrice = upbitPrice,
                    upbitChangeRate24h = upbit.signedChangeRate * 100.0,
                    upbitChangePrice24h = upbit.signedChangePrice,
                    upbitVolume24h = upbit.accTradeVolume24h,
                    upbitHighPrice = upbit.highPrice,
                    upbitLowPrice = upbit.lowPrice,
                    
                    binancePrice = binancePrice,
                    binanceChangeRate24h = binanceResult?.priceChangePercent?.toDoubleOrNull() ?: 0.0,
                    binanceHighPrice = binanceResult?.highPrice?.toDoubleOrNull() ?: 0.0,
                    binanceLowPrice = binanceResult?.lowPrice?.toDoubleOrNull() ?: 0.0,
                    binanceVolume24h = binanceResult?.volume?.toDoubleOrNull() ?: 0.0,
                    
                    exchangeRate = exchangeRate,
                    kimchiPremiumPercent = kimchiPremiumPercent
                )
                _tickerState.value = state

                // Check user alerts with the newly fetched prices
                checkAlerts(upbitPrice, binancePrice)
                
                // Update converter optionally if empty to provide pre-filled values
                if (calcBtcStr.value.isEmpty() && calcKrwStr.value.isEmpty() && calcUsdStr.value.isEmpty()) {
                    updateCalculatorFromBtc(1.0, upbitPrice, binancePrice, exchangeRate)
                }
            } else {
                _tickerState.value = TickerState.Error("업비트 데이터를 받아올 수 없습니다.")
            }
        } catch (e: Exception) {
            // If it's a transient connection failure, don't clear old success data if it exists
            if (_tickerState.value !is TickerState.Success) {
                _tickerState.value = TickerState.Error("네트워크 오류: ${e.localizedMessage}")
            }
        }
    }

    private fun loadChartData() {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            _chartState.value = ChartState.Loading
            val coin = _selectedCoin.value
            val range = _selectedRange.value

            try {
                val candles = when (range) {
                    ChartRange.H24 -> repository.fetchUpbitHourlyCandles(coin.upbitMarket, 24)
                    ChartRange.W1 -> repository.fetchUpbitDailyCandles(coin.upbitMarket, 7)
                    ChartRange.M1 -> repository.fetchUpbitDailyCandles(coin.upbitMarket, 30)
                    ChartRange.Y1 -> repository.fetchUpbitWeeklyCandles(coin.upbitMarket, 52)
                }

                // Reverse because Upbit returns newest candle first, but for chart we need oldest first
                val sortedCandles = candles.reversed()
                
                val points = sortedCandles.mapIndexed { index, candle ->
                    val dateLabel = when (range) {
                        ChartRange.H24 -> formatKstTime(candle.candleDateTimeKst, "HH:mm")
                        ChartRange.W1 -> formatKstTime(candle.candleDateTimeKst, "MM/dd")
                        ChartRange.M1 -> formatKstTime(candle.candleDateTimeKst, "MM/dd")
                        ChartRange.Y1 -> formatKstTime(candle.candleDateTimeKst, "yy/MM")
                    }
                    ChartPoint(
                        timestamp = candle.timestamp,
                        price = candle.tradePrice,
                        label = dateLabel
                    )
                }
                _chartState.value = ChartState.Success(points)
            } catch (e: Exception) {
                _chartState.value = ChartState.Error("차트 데이터 에러: ${e.localizedMessage}")
            }
        }
    }

    private fun formatKstTime(timeStr: String, pattern: String): String {
        return try {
            // Upbit format: yyyy-MM-dd'T'HH:mm:ss
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = parser.parse(timeStr) ?: Date()
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            timeStr
        }
    }

    // --- ALERTS CHECK LOGIC ---

    private suspend fun checkAlerts(upbitPrice: Double, binancePrice: Double) {
        val alerts = repository.allAlerts.first()
        for (alert in alerts) {
            if (!alert.isActive || alert.coinCode != _selectedCoin.value.code) continue

            val currentPrice = if (alert.isKrw) upbitPrice else binancePrice
            val isTriggered = if (alert.isAbove) {
                currentPrice >= alert.targetPrice
            } else {
                currentPrice <= alert.targetPrice
            }

            if (isTriggered) {
                repository.updateAlertStatus(alert.id, false) // Deactivate to avoid double triggers
                _triggeredAlertEvent.emit(alert)
            }
        }
    }

    // --- DATABASE ACTIONS FOR UI ---

    fun addAlert(targetPrice: Double, isKrw: Boolean, isAbove: Boolean) {
        viewModelScope.launch {
            val alert = PriceAlert(
                coinCode = _selectedCoin.value.code,
                targetPrice = targetPrice,
                isKrw = isKrw,
                isAbove = isAbove
            )
            repository.insertAlert(alert)
        }
    }

    fun deleteAlert(id: Int) {
        viewModelScope.launch {
            repository.deleteAlertById(id)
        }
    }

    fun toggleAlertStatus(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateAlertStatus(id, isActive)
        }
    }

    // --- CALCULATOR LOGIC ---

    fun onBtcAmountChanged(amountStr: String) {
        calcBtcStr.value = amountStr
        if (amountStr.isEmpty()) {
            calcKrwStr.value = ""
            calcUsdStr.value = ""
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: return
        val currentTicker = _tickerState.value as? TickerState.Success ?: return
        updateCalculatorFromBtc(
            amount,
            currentTicker.upbitPrice,
            currentTicker.binancePrice,
            currentTicker.exchangeRate
        )
    }

    fun onKrwAmountChanged(amountStr: String) {
        calcKrwStr.value = amountStr
        if (amountStr.isEmpty()) {
            calcBtcStr.value = ""
            calcUsdStr.value = ""
            return
        }

        val krwAmount = amountStr.toDoubleOrNull() ?: return
        val currentTicker = _tickerState.value as? TickerState.Success ?: return
        
        val btcAmount = if (currentTicker.upbitPrice > 0.0) krwAmount / currentTicker.upbitPrice else 0.0
        val usdAmount = if (currentTicker.exchangeRate > 0.0) krwAmount / currentTicker.exchangeRate else 0.0

        calcBtcStr.value = String.format(Locale.US, "%.6f", btcAmount)
        calcUsdStr.value = String.format(Locale.US, "%.2f", usdAmount)
    }

    fun onUsdAmountChanged(amountStr: String) {
        calcUsdStr.value = amountStr
        if (amountStr.isEmpty()) {
            calcBtcStr.value = ""
            calcKrwStr.value = ""
            return
        }

        val usdAmount = amountStr.toDoubleOrNull() ?: return
        val currentTicker = _tickerState.value as? TickerState.Success ?: return

        val btcAmount = if (currentTicker.binancePrice > 0.0) usdAmount / currentTicker.binancePrice else 0.0
        val krwAmount = usdAmount * currentTicker.exchangeRate

        calcBtcStr.value = String.format(Locale.US, "%.6f", btcAmount)
        calcKrwStr.value = String.format(Locale.US, "%.0f", krwAmount)
    }

    private fun updateCalculatorFromBtc(btcAmount: Double, upbitPrice: Double, binancePrice: Double, exchangeRate: Double) {
        val krwValue = btcAmount * upbitPrice
        val usdValue = btcAmount * binancePrice

        calcBtcStr.value = String.format(Locale.US, "%.6f", btcAmount)
        calcKrwStr.value = String.format(Locale.US, "%.0f", krwValue)
        calcUsdStr.value = String.format(Locale.US, "%.2f", usdValue)
    }

    private fun clearCalculator() {
        calcBtcStr.value = ""
        calcKrwStr.value = ""
        calcUsdStr.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
