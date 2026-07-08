package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface FireAssetDao {
    @Query("SELECT * FROM fire_assets")
    fun getAllAssets(): Flow<List<FireAsset>>

    @Query("SELECT * FROM fire_assets WHERE id = :assetId LIMIT 1")
    suspend fun getAssetById(assetId: String): FireAsset?

    @Query("SELECT * FROM fire_assets WHERE id = :assetId LIMIT 1")
    fun getAssetByIdFlow(assetId: String): Flow<FireAsset?>

    @Query("SELECT * FROM fire_assets WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getAssetByQrCode(qrCode: String): FireAsset?

    @Query("SELECT * FROM fire_assets WHERE plant = :plant")
    fun getAssetsByPlant(plant: String): Flow<List<FireAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: FireAsset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<FireAsset>)

    @Update
    suspend fun updateAsset(asset: FireAsset)

    @Query("UPDATE fire_assets SET currentStatus = :status, isOperational = :isOp WHERE id = :id")
    suspend fun updateAssetStatus(id: String, status: String, isOp: Boolean)

    @Query("DELETE FROM fire_assets WHERE id = :assetId")
    suspend fun deleteAsset(assetId: String)

    @Query("DELETE FROM fire_assets")
    suspend fun clearAssets()
}

@Dao
interface InspectionLogDao {
    @Query("SELECT * FROM inspection_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<InspectionLog>>

    @Query("SELECT * FROM inspection_logs WHERE assetId = :assetId ORDER BY timestamp DESC")
    fun getLogsForAsset(assetId: String): Flow<List<InspectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InspectionLog)

    @Query("DELETE FROM inspection_logs")
    suspend fun clearLogs()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivities(limit: Int): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivities()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE isRead = 0")
    fun getUnreadNotifications(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()
}
