package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- DATA TRANSFER OBJECTS (DTOs) ---

@JsonClass(generateAdapter = true)
data class UpbitTickerDto(
    @Json(name = "market") val market: String,
    @Json(name = "trade_price") val tradePrice: Double,
    @Json(name = "opening_price") val openingPrice: Double,
    @Json(name = "high_price") val highPrice: Double,
    @Json(name = "low_price") val lowPrice: Double,
    @Json(name = "prev_closing_price") val prevClosingPrice: Double,
    @Json(name = "change") val change: String,
    @Json(name = "change_price") val changePrice: Double,
    @Json(name = "change_rate") val changeRate: Double,
    @Json(name = "signed_change_price") val signedChangePrice: Double,
    @Json(name = "signed_change_rate") val signedChangeRate: Double,
    @Json(name = "trade_volume") val tradeVolume: Double,
    @Json(name = "acc_trade_price_24h") val accTradePrice24h: Double,
    @Json(name = "acc_trade_volume_24h") val accTradeVolume24h: Double,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class UpbitCandleDto(
    @Json(name = "market") val market: String,
    @Json(name = "candle_date_time_utc") val candleDateTimeUtc: String,
    @Json(name = "candle_date_time_kst") val candleDateTimeKst: String,
    @Json(name = "opening_price") val openingPrice: Double,
    @Json(name = "high_price") val highPrice: Double,
    @Json(name = "low_price") val lowPrice: Double,
    @Json(name = "trade_price") val tradePrice: Double,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "candle_acc_trade_price") val candleAccTradePrice: Double,
    @Json(name = "candle_acc_trade_volume") val candleAccTradeVolume: Double
)

@JsonClass(generateAdapter = true)
data class BinanceTickerDto(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "lastPrice") val lastPrice: String,
    @Json(name = "priceChangePercent") val priceChangePercent: String,
    @Json(name = "highPrice") val highPrice: String,
    @Json(name = "lowPrice") val lowPrice: String,
    @Json(name = "volume") val volume: String,
    @Json(name = "quoteVolume") val quoteVolume: String
)

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
    @Json(name = "result") val result: String,
    @Json(name = "rates") val rates: Map<String, Double>
)

// --- RETROFIT INTERFACES ---

interface UpbitService {
    @GET("v1/ticker")
    suspend fun getTickers(@Query("markets") markets: String): List<UpbitTickerDto>

    @GET("v1/candles/days")
    suspend fun getDailyCandles(
        @Query("market") market: String,
        @Query("count") count: Int
    ): List<UpbitCandleDto>

    @GET("v1/candles/minutes/60")
    suspend fun getHourlyCandles(
        @Query("market") market: String,
        @Query("count") count: Int
    ): List<UpbitCandleDto>

    @GET("v1/candles/weeks")
    suspend fun getWeeklyCandles(
        @Query("market") market: String,
        @Query("count") count: Int
    ): List<UpbitCandleDto>
}

interface BinanceService {
    @GET("api/v3/ticker/24hr")
    suspend fun getTicker(@Query("symbol") symbol: String): BinanceTickerDto
}

interface ExchangeRateService {
    @GET("v6/latest/USD")
    suspend fun getExchangeRate(): ExchangeRateResponse
}

// --- NETWORK CLIENTS PROVIDER ---

object Network {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val upbitService: UpbitService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.upbit.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UpbitService::class.java)
    }

    val binanceService: BinanceService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.binance.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BinanceService::class.java)
    }

    val exchangeRateService: ExchangeRateService by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRateService::class.java)
    }
}
