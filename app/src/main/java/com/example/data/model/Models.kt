package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String, // SYSTEM_ADMIN, CORPORATE_EHS, PLANT_HEAD, SAFETY_MANAGER, SAFETY_OFFICER, MAINTENANCE_ENGINEER, FIRE_TECHNICIAN, SECURITY, AUDITOR, CONTRACTOR, VIEWER
    val plantId: String,
    val certificationExpiry: Long? = null
)

@Entity(tableName = "fire_assets")
data class FireAsset(
    @PrimaryKey val id: String, // Asset ID (e.g. EXT-CH1-WH-001)
    val qrCode: String,
    val barcode: String,
    val plant: String,
    val building: String,
    val floor: String,
    val department: String,
    val area: String,
    val locationDescription: String,
    val category: String, // PORTABLE, WATER_BASED, PUMPING, DETECTION, SPECIAL_HAZARD, LIFE_SAFETY
    val assetType: String, // Portable Fire Extinguishers, Electric Fire Pump, etc.
    val manufacturer: String,
    val model: String,
    val serialNumber: String,
    val capacity: String, // e.g. "4.5 kg", "1000 LPM"
    val pressureRating: String, // e.g. "15 bar", "7.5 bar"
    val installationDate: Long,
    val manufacturingDate: Long,
    val warrantyExpiry: Long,
    val inspectionFrequencyDays: Int, // e.g. 30 (Monthly), 7 (Weekly), 365 (Annual)
    val maintenanceFrequencyDays: Int,
    val calibrationFrequencyDays: Int,
    val hydroTestFrequencyDays: Int,
    val nextInspectionDate: Long,
    val nextMaintenanceDate: Long,
    val nextHydroTestDate: Long,
    val nextCalibrationDate: Long,
    val responsibleDepartment: String,
    val responsiblePersonId: String,
    val responsiblePersonName: String,
    val currentStatus: String, // HEALTHY, DUE, OVERDUE, BREAKDOWN, OOS, REFILLING
    val photoUrl: String = "",
    val drawingUrl: String = "",
    val gpsLat: Double = 0.0,
    val gpsLng: Double = 0.0,
    val remarks: String = "",
    val isOperational: Boolean = true,
    val lastInspectionDate: Long = 0L
)

@Entity(tableName = "inspection_logs")
data class InspectionLog(
    @PrimaryKey val id: String, // Log ID
    val assetId: String,
    val userId: String,
    val userName: String,
    val timestamp: Long,
    val checklistJson: String, // JSON mapping answers (e.g. {"pressure_ok":true, "seal_intact":true})
    val photosJson: String, // JSON array of photo paths
    val digitalSignature: String, // Base64 signature image or drawing path
    val resultStatus: String, // PASS, FAIL
    val locationVerified: Boolean,
    val notes: String = ""
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: Long,
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val logType: String, // INSPECTION, MAINTENANCE, ALERT, GENERAL
    val plantId: String,
    val assetId: String
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val assetId: String,
    val isRead: Boolean = false
)
