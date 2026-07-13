package com.example.data

import com.example.data.database.PriceAlert
import com.example.data.database.PriceAlertDao
import kotlinx.coroutines.flow.Flow

class CoinRepository(private val priceAlertDao: PriceAlertDao) {

    // Network APIs
    suspend fun fetchUpbitTickers(markets: String): List<UpbitTickerDto> {
        return Network.upbitService.getTickers(markets)
    }

    suspend fun fetchUpbitDailyCandles(market: String, count: Int): List<UpbitCandleDto> {
        return Network.upbitService.getDailyCandles(market, count)
    }

    suspend fun fetchUpbitHourlyCandles(market: String, count: Int): List<UpbitCandleDto> {
        return Network.upbitService.getHourlyCandles(market, count)
    }

    suspend fun fetchUpbitWeeklyCandles(market: String, count: Int): List<UpbitCandleDto> {
        return Network.upbitService.getWeeklyCandles(market, count)
    }

    suspend fun fetchBinanceTicker(symbol: String): BinanceTickerDto {
        return Network.binanceService.getTicker(symbol)
    }

    suspend fun fetchExchangeRate(): ExchangeRateResponse {
        return Network.exchangeRateService.getExchangeRate()
    }

    // Local Database (Price Alerts)
    val allAlerts: Flow<List<PriceAlert>> = priceAlertDao.getAllAlerts()

    suspend fun insertAlert(alert: PriceAlert) {
        priceAlertDao.insertAlert(alert)
    }

    suspend fun deleteAlertById(id: Int) {
        priceAlertDao.deleteAlertById(id)
    }

    suspend fun updateAlertStatus(id: Int, isActive: Boolean) {
        priceAlertDao.updateAlertStatus(id, isActive)
    }
}
