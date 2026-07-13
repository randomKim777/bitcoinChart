package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinCode: String,      // "BTC", "ETH", "XRP"
    val targetPrice: Double,   // target price in KRW or USD
    val isKrw: Boolean,        // true = KRW alert, false = USD alert
    val isAbove: Boolean,      // true = notify when price >= target, false = notify when price <= target
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)

    @Query("UPDATE price_alerts SET isActive = :isActive WHERE id = :id")
    suspend fun updateAlertStatus(id: Int, isActive: Boolean)
}

@Database(entities = [PriceAlert::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun priceAlertDao(): PriceAlertDao
}
