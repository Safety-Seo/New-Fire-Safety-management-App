package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class FireAssetRepository(
    private val userDao: UserDao,
    private val fireAssetDao: FireAssetDao,
    private val inspectionLogDao: InspectionLogDao,
    private val activityLogDao: ActivityLogDao,
    private val notificationDao: NotificationDao
) {
    val allAssets: Flow<List<FireAsset>> = fireAssetDao.getAllAssets()
    val allLogs: Flow<List<InspectionLog>> = inspectionLogDao.getAllLogs()
    val allActivities: Flow<List<ActivityLog>> = activityLogDao.getAllActivities()
    val unreadNotifications: Flow<List<Notification>> = notificationDao.getUnreadNotifications()
    val allNotifications: Flow<List<Notification>> = notificationDao.getAllNotifications()

    fun getRecentActivities(limit: Int): Flow<List<ActivityLog>> = activityLogDao.getRecentActivities(limit)

    suspend fun getAssetById(id: String): FireAsset? = fireAssetDao.getAssetById(id)
    fun getAssetByIdFlow(id: String): Flow<FireAsset?> = fireAssetDao.getAssetByIdFlow(id)
    suspend fun getAssetByQrCode(qrCode: String): FireAsset? = fireAssetDao.getAssetByQrCode(qrCode)

    suspend fun insertAsset(asset: FireAsset) = fireAssetDao.insertAsset(asset)
    suspend fun updateAsset(asset: FireAsset) = fireAssetDao.updateAsset(asset)
    suspend fun deleteAsset(id: String) = fireAssetDao.deleteAsset(id)

    suspend fun markAllNotificationsAsRead() = notificationDao.markAllAsRead()
    suspend fun markNotificationAsRead(id: String) = notificationDao.markAsRead(id)

    suspend fun submitInspection(log: InspectionLog, asset: FireAsset) {
        // Save inspection log
        inspectionLogDao.insertLog(log)

        // Calculate next dates
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = log.timestamp

        // Add frequency to calculate next due dates
        calendar.add(Calendar.DAY_OF_YEAR, asset.inspectionFrequencyDays)
        val nextInsp = calendar.timeInMillis

        // If inspection passes, status is HEALTHY or DUE, otherwise BREAKDOWN
        val updatedStatus = if (log.resultStatus == "PASS") {
            // Check if upcoming due for refilling or hydro test
            "HEALTHY"
        } else {
            "BREAKDOWN"
        }

        val updatedAsset = asset.copy(
            lastInspectionDate = log.timestamp,
            nextInspectionDate = nextInsp,
            currentStatus = updatedStatus,
            isOperational = log.resultStatus == "PASS"
        )

        fireAssetDao.updateAsset(updatedAsset)

        // Log this activity
        val severity = if (log.resultStatus == "PASS") "LOW" else "CRITICAL"
        activityLogDao.insertActivity(
            ActivityLog(
                title = "Inspection Submitted",
                description = "Inspection of ${asset.assetType} (${asset.id}) by ${log.userName}. Result: ${log.resultStatus}.",
                timestamp = log.timestamp,
                severity = severity,
                logType = "INSPECTION",
                plantId = asset.plant,
                assetId = asset.id
            )
        )

        // If failed, trigger Critical Failure Alert notification
        if (log.resultStatus == "FAIL") {
            val alertId = "AL-%d".format(System.currentTimeMillis())
            notificationDao.insertNotification(
                Notification(
                    id = alertId,
                    title = "CRITICAL FAILURE: ${asset.assetType}",
                    description = "Asset ${asset.id} at ${asset.plant} - ${asset.building} failed inspection. Reason: ${log.notes}",
                    timestamp = log.timestamp,
                    severity = "CRITICAL",
                    assetId = asset.id
                )
            )
        }
    }

    suspend fun triggerAssetBreakdown(assetId: String, reason: String, userName: String) {
        val asset = fireAssetDao.getAssetById(assetId) ?: return
        val updatedAsset = asset.copy(currentStatus = "BREAKDOWN", isOperational = false)
        fireAssetDao.updateAsset(updatedAsset)

        val timestamp = System.currentTimeMillis()
        activityLogDao.insertActivity(
            ActivityLog(
                title = "Asset Breakdown Reported",
                description = "Asset ${asset.id} reported as broken by $userName. Reason: $reason",
                timestamp = timestamp,
                severity = "HIGH",
                logType = "ALERT",
                plantId = asset.plant,
                assetId = asset.id
            )
        )

        notificationDao.insertNotification(
            Notification(
                id = "NOTIF-${System.currentTimeMillis()}",
                title = "Breakdown: ${asset.id}",
                description = "Asset ${asset.id} (${asset.assetType}) in ${asset.plant} is non-functional: $reason",
                timestamp = timestamp,
                severity = "HIGH",
                assetId = asset.id
            )
        )
    }

    suspend fun resolveBreakdown(assetId: String, remarks: String, userName: String) {
        val asset = fireAssetDao.getAssetById(assetId) ?: return
        val updatedAsset = asset.copy(currentStatus = "HEALTHY", isOperational = true, remarks = remarks)
        fireAssetDao.updateAsset(updatedAsset)

        val timestamp = System.currentTimeMillis()
        activityLogDao.insertActivity(
            ActivityLog(
                title = "Asset Repaired",
                description = "Breakdown on ${asset.id} resolved by $userName. Remarks: $remarks",
                timestamp = timestamp,
                severity = "LOW",
                logType = "MAINTENANCE",
                plantId = asset.plant,
                assetId = asset.id
            )
        )
    }

    suspend fun prepopulateDatabase() {
        val assets = fireAssetDao.getAllAssets().firstOrNull()
        if (assets.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            val initialUsers = listOf(
                User("USR-001", "Vikram Singh", "vikram.singh@ignisguard.com", "SAFETY_MANAGER", "Chennai Plant A"),
                User("USR-002", "Amit Sharma", "amit.sharma@ignisguard.com", "FIRE_TECHNICIAN", "Chennai Plant A"),
                User("USR-003", "Rajesh Kumar", "rajesh.kumar@ignisguard.com", "PLANT_HEAD", "Pune Plant B"),
                User("USR-004", "Suresh Nair", "suresh.nair@ignisguard.com", "AUDITOR", "All Plants")
            )
            userDao.insertUsers(initialUsers)

            // Dynamic date setups
            val pastDate = now - (15L * 24 * 3600 * 1000) // 15 days ago
            val future3Days = now + (3L * 24 * 3600 * 1000) // 3 days from now
            val future30Days = now + (30L * 24 * 3600 * 1000) // 30 days from now
            val overdueDate = now - (2L * 24 * 3600 * 1000) // 2 days ago (meaning inspection is overdue)

            val initialAssets = listOf(
                FireAsset(
                    id = "EXT-CH1-WH-101",
                    qrCode = "EXT-CH1-WH-101",
                    barcode = "100200300101",
                    plant = "Chennai Plant A",
                    building = "Warehouse 1",
                    floor = "Ground Floor",
                    department = "Logistics",
                    area = "Loading Bay 2",
                    locationDescription = "Mounted next to south emergency fire exit.",
                    category = "PORTABLE",
                    assetType = "CO₂ Fire Extinguisher",
                    manufacturer = "Ceasefire",
                    model = "CF-CO2-4.5",
                    serialNumber = "SN-28312A",
                    capacity = "4.5 kg",
                    pressureRating = "150 bar",
                    installationDate = pastDate - (365L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (400L * 24 * 3600 * 1000),
                    warrantyExpiry = now + (180L * 24 * 3600 * 1000),
                    inspectionFrequencyDays = 30, // Monthly
                    maintenanceFrequencyDays = 365, // Annual
                    calibrationFrequencyDays = 0, // N/A
                    hydroTestFrequencyDays = 1825, // 5-yearly
                    nextInspectionDate = future3Days,
                    nextMaintenanceDate = now + (90L * 24 * 3600 * 1000),
                    nextHydroTestDate = now + (730L * 24 * 3600 * 1000),
                    nextCalibrationDate = 0,
                    responsibleDepartment = "EHS Dept",
                    responsiblePersonId = "USR-002",
                    responsiblePersonName = "Amit Sharma",
                    currentStatus = "HEALTHY",
                    photoUrl = "extinguisher_co2",
                    isOperational = true
                ),
                FireAsset(
                    id = "EXT-CH1-SR-102",
                    qrCode = "EXT-CH1-SR-102",
                    barcode = "100200300102",
                    plant = "Chennai Plant A",
                    building = "Server Building",
                    floor = "2nd Floor",
                    department = "IT Infrastructure",
                    area = "Server Room 1",
                    locationDescription = "Beside the main electrical control room entrance.",
                    category = "PORTABLE",
                    assetType = "Clean Agent Extinguisher",
                    manufacturer = "Kanex",
                    model = "CF-CA-2.0",
                    serialNumber = "SN-99482B",
                    capacity = "2.0 kg",
                    pressureRating = "15 bar",
                    installationDate = pastDate - (200L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (220L * 24 * 3600 * 1000),
                    warrantyExpiry = now + (300L * 24 * 3600 * 1000),
                    inspectionFrequencyDays = 30,
                    maintenanceFrequencyDays = 365,
                    calibrationFrequencyDays = 0,
                    hydroTestFrequencyDays = 1825,
                    nextInspectionDate = overdueDate, // OVERDUE INSPECTION
                    nextMaintenanceDate = now + (165L * 24 * 3600 * 1000),
                    nextHydroTestDate = now + (800L * 24 * 3600 * 1000),
                    nextCalibrationDate = 0,
                    responsibleDepartment = "EHS Dept",
                    responsiblePersonId = "USR-002",
                    responsiblePersonName = "Amit Sharma",
                    currentStatus = "OVERDUE",
                    photoUrl = "extinguisher_clean_agent",
                    isOperational = true
                ),
                FireAsset(
                    id = "HYD-CH1-PR-201",
                    qrCode = "HYD-CH1-PR-201",
                    barcode = "100200300201",
                    plant = "Chennai Plant A",
                    building = "Production Block A",
                    floor = "Ground Floor",
                    department = "Operations",
                    area = "External Perimeter North",
                    locationDescription = "Hydrant post near the chemical storage storage warehouse.",
                    category = "WATER_BASED",
                    assetType = "Fire Hydrant System",
                    manufacturer = "Newage Industries",
                    model = "POST-HYD-100",
                    serialNumber = "SN-HYD-5510",
                    capacity = "1500 LPM @ 7 Bar",
                    pressureRating = "16 bar",
                    installationDate = pastDate - (500L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (520L * 24 * 3600 * 1000),
                    warrantyExpiry = pastDate + (100L * 24 * 3600 * 1000),
                    inspectionFrequencyDays = 30, // Monthly
                    maintenanceFrequencyDays = 90, // Quarterly
                    calibrationFrequencyDays = 0,
                    hydroTestFrequencyDays = 1095, // 3-yearly
                    nextInspectionDate = future30Days,
                    nextMaintenanceDate = overdueDate, // OVERDUE MAINTENANCE
                    nextHydroTestDate = now + (300L * 24 * 3600 * 1000),
                    nextCalibrationDate = 0,
                    responsibleDepartment = "Utility Maintenance",
                    responsiblePersonId = "USR-001",
                    responsiblePersonName = "Vikram Singh",
                    currentStatus = "DUE",
                    photoUrl = "hydrant_post",
                    isOperational = true
                ),
                FireAsset(
                    id = "PMP-CH1-PH-301",
                    qrCode = "PMP-CH1-PH-301",
                    barcode = "100200300301",
                    plant = "Chennai Plant A",
                    building = "Pump House",
                    floor = "Ground Floor",
                    department = "Utility Maintenance",
                    area = "Main Pump Chamber",
                    locationDescription = "Primary pumping system for industrial hydrant and sprinkler feed.",
                    category = "PUMPING",
                    assetType = "Electric Fire Pump",
                    manufacturer = "Kirloskar Brothers Ltd",
                    model = "KBL-FP-3000",
                    serialNumber = "SN-PMP-8201",
                    capacity = "3000 GPM",
                    pressureRating = "12 bar",
                    installationDate = pastDate - (1000L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (1050L * 24 * 3600 * 1000),
                    warrantyExpiry = pastDate,
                    inspectionFrequencyDays = 7, // Weekly Churn Test (NFPA 25)
                    maintenanceFrequencyDays = 180, // Half-Yearly
                    calibrationFrequencyDays = 365, // Annual sensor calibration
                    hydroTestFrequencyDays = 0,
                    nextInspectionDate = future3Days,
                    nextMaintenanceDate = now + (15L * 24 * 3600 * 1000),
                    nextHydroTestDate = 0,
                    nextCalibrationDate = now + (45L * 24 * 3600 * 1000),
                    responsibleDepartment = "Utility Maintenance",
                    responsiblePersonId = "USR-001",
                    responsiblePersonName = "Vikram Singh",
                    currentStatus = "HEALTHY",
                    photoUrl = "pump_electric",
                    isOperational = true
                ),
                FireAsset(
                    id = "PMP-CH1-PH-302",
                    qrCode = "PMP-CH1-PH-302",
                    barcode = "100200300302",
                    plant = "Chennai Plant A",
                    building = "Pump House",
                    floor = "Ground Floor",
                    department = "Utility Maintenance",
                    area = "Secondary Pump Standby",
                    locationDescription = "Diesel engine powered backup pump, NFPA 25 standby line.",
                    category = "PUMPING",
                    assetType = "Diesel Fire Pump",
                    manufacturer = "Cummins / Kirloskar",
                    model = "DSL-FP-3000",
                    serialNumber = "SN-DSL-8921",
                    capacity = "3000 GPM",
                    pressureRating = "12.5 bar",
                    installationDate = pastDate - (1000L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (1050L * 24 * 3600 * 1000),
                    warrantyExpiry = pastDate,
                    inspectionFrequencyDays = 7, // Weekly Churn
                    maintenanceFrequencyDays = 90, // Quarterly maintenance
                    calibrationFrequencyDays = 365,
                    hydroTestFrequencyDays = 0,
                    nextInspectionDate = now + (1L * 24 * 3600 * 1000),
                    nextMaintenanceDate = now + (10L * 24 * 3600 * 1000),
                    nextHydroTestDate = 0,
                    nextCalibrationDate = now + (60L * 24 * 3600 * 1000),
                    responsibleDepartment = "Utility Maintenance",
                    responsiblePersonId = "USR-001",
                    responsiblePersonName = "Vikram Singh",
                    currentStatus = "BREAKDOWN", // DOWN!
                    photoUrl = "pump_diesel",
                    isOperational = false,
                    remarks = "Starter battery charging circuit fault detected. Diesel pump fails to kick off in auto churn test."
                ),
                FireAsset(
                    id = "ALM-CH1-CN-401",
                    qrCode = "ALM-CH1-CN-401",
                    barcode = "100200300401",
                    plant = "Chennai Plant A",
                    building = "Control Room",
                    floor = "1st Floor",
                    department = "Security",
                    area = "Main Fire Control Desk",
                    locationDescription = "Main addressable fire alarm panel linked to 400 detector loops across Chennai plant.",
                    category = "DETECTION",
                    assetType = "Fire Alarm Control Panel",
                    manufacturer = "Honeywell Notifier",
                    model = "NFS2-3030",
                    serialNumber = "SN-FACP-99120",
                    capacity = "10 Loop Addressable",
                    pressureRating = "N/A",
                    installationDate = pastDate - (300L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (310L * 24 * 3600 * 1000),
                    warrantyExpiry = now + (65L * 24 * 3600 * 1000),
                    inspectionFrequencyDays = 1, // Daily status check
                    maintenanceFrequencyDays = 90, // Quarterly loop test
                    calibrationFrequencyDays = 365,
                    hydroTestFrequencyDays = 0,
                    nextInspectionDate = now + (8L * 3600 * 1000), // Due in 8 hours
                    nextMaintenanceDate = now + (35L * 24 * 3600 * 1000),
                    nextHydroTestDate = 0,
                    nextCalibrationDate = now + (65L * 24 * 3600 * 1000),
                    responsibleDepartment = "Instrumentation & Security",
                    responsiblePersonId = "USR-001",
                    responsiblePersonName = "Vikram Singh",
                    currentStatus = "HEALTHY",
                    photoUrl = "panel_notifier",
                    isOperational = true
                ),
                FireAsset(
                    id = "EXT-PN2-OF-105",
                    qrCode = "EXT-PN2-OF-105",
                    barcode = "100200300105",
                    plant = "Pune Plant B",
                    building = "Admin HQ Block",
                    floor = "Ground Floor",
                    department = "Administration",
                    area = "Pantry Entrance Lobby",
                    locationDescription = "Wall mounted next to the main electrical pantry circuit panel.",
                    category = "PORTABLE",
                    assetType = "Wet Chemical Extinguisher",
                    manufacturer = "Ceasefire",
                    model = "CF-WC-6",
                    serialNumber = "SN-WCT-212",
                    capacity = "6.0 Litres",
                    pressureRating = "12 bar",
                    installationDate = pastDate - (120L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (130L * 24 * 3600 * 1000),
                    warrantyExpiry = now + (245L * 24 * 3600 * 1000),
                    inspectionFrequencyDays = 30,
                    maintenanceFrequencyDays = 365,
                    calibrationFrequencyDays = 0,
                    hydroTestFrequencyDays = 1825,
                    nextInspectionDate = future30Days,
                    nextMaintenanceDate = now + (245L * 24 * 3600 * 1000),
                    nextHydroTestDate = now + (1700L * 24 * 3600 * 1000),
                    nextCalibrationDate = 0,
                    responsibleDepartment = "Safety Dept Pune",
                    responsiblePersonId = "USR-003",
                    responsiblePersonName = "Rajesh Kumar",
                    currentStatus = "HEALTHY",
                    photoUrl = "extinguisher_wet_chemical",
                    isOperational = true
                ),
                FireAsset(
                    id = "GAS-PN2-SV-501",
                    qrCode = "GAS-PN2-SV-501",
                    barcode = "100200300501",
                    plant = "Pune Plant B",
                    building = "Engineering R&D Center",
                    floor = "Basement",
                    department = "Research & Development",
                    area = "Novec 1230 Gas Cylinders Room",
                    locationDescription = "Piped gas suppression storage cylinders designed to protect high-density server banks.",
                    category = "SPECIAL_HAZARD",
                    assetType = "Gas Suppression System",
                    manufacturer = "Kidde Fenwal",
                    model = "NOV-1230-180",
                    serialNumber = "SN-KD-8812A",
                    capacity = "180 Litre Cylinder",
                    pressureRating = "25 bar",
                    installationDate = pastDate - (180L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (190L * 24 * 3600 * 1000),
                    warrantyExpiry = now + (185L * 24 * 3600 * 1000),
                    inspectionFrequencyDays = 30, // Monthly
                    maintenanceFrequencyDays = 180, // Bi-annual weights check
                    calibrationFrequencyDays = 365,
                    hydroTestFrequencyDays = 3650, // 10-yearly
                    nextInspectionDate = now - (5L * 24 * 3600 * 1000), // OVERDUE INSPECTION
                    nextMaintenanceDate = now + (5L * 24 * 3600 * 1000),
                    nextHydroTestDate = now + (3000L * 24 * 3600 * 1000),
                    nextCalibrationDate = now + (185L * 24 * 3600 * 1000),
                    responsibleDepartment = "R&D Operations Pune",
                    responsiblePersonId = "USR-003",
                    responsiblePersonName = "Rajesh Kumar",
                    currentStatus = "OVERDUE",
                    photoUrl = "novec_system",
                    isOperational = true
                ),
                FireAsset(
                    id = "LIF-BL1-DR-601",
                    qrCode = "LIF-BL1-DR-601",
                    barcode = "100200300601",
                    plant = "Bengaluru Tech Park",
                    building = "Block C R&D lab",
                    floor = "Ground Floor",
                    department = "Facility Management",
                    area = "Main Laboratory Exit",
                    locationDescription = "Heavy duty double-leaf fire doors rated for 120-minute flame integrity with electromagnetic hold-open locks.",
                    category = "LIFE_SAFETY",
                    assetType = "Fire Doors",
                    manufacturer = "Shakti Met-Dor",
                    model = "SH-120-MIN",
                    serialNumber = "SN-DR-092A",
                    capacity = "120 Min Integrity",
                    pressureRating = "N/A",
                    installationDate = pastDate - (400L * 24 * 3600 * 1000),
                    manufacturingDate = pastDate - (420L * 24 * 3600 * 1000),
                    warrantyExpiry = now + (330L * 24 * 3600 * 1000),
                    inspectionFrequencyDays = 90, // Quarterly drop check
                    maintenanceFrequencyDays = 365, // Annual lock lubrication
                    calibrationFrequencyDays = 0,
                    hydroTestFrequencyDays = 0,
                    nextInspectionDate = future30Days,
                    nextMaintenanceDate = now + (120L * 24 * 3600 * 1000),
                    nextHydroTestDate = 0,
                    nextCalibrationDate = 0,
                    responsibleDepartment = "Facility Operations",
                    responsiblePersonId = "USR-004",
                    responsiblePersonName = "Suresh Nair",
                    currentStatus = "HEALTHY",
                    photoUrl = "fire_door",
                    isOperational = true
                )
            )

            fireAssetDao.insertAssets(initialAssets)

            // Seed some initial activities to make the audit trail look real
            val initialActivities = listOf(
                ActivityLog(
                    title = "Database Seeded",
                    description = "Enterprise Fire Asset Management System initialized. Multi-plant assets registered.",
                    timestamp = pastDate,
                    severity = "LOW",
                    logType = "GENERAL",
                    plantId = "System",
                    assetId = ""
                ),
                ActivityLog(
                    title = "Battery Fault Alert",
                    description = "FACP reported starter battery voltage drop (19.8V) on Diesel Pump (PMP-CH1-PH-302).",
                    timestamp = pastDate + (5L * 24 * 3600 * 1000),
                    severity = "CRITICAL",
                    logType = "ALERT",
                    plantId = "Chennai Plant A",
                    assetId = "PMP-CH1-PH-302"
                ),
                ActivityLog(
                    title = "Inspection Completed",
                    description = "Successful routine monthly check of Extinguisher (EXT-CH1-WH-101) by Amit Sharma. Seal intact and pressure in green range.",
                    timestamp = pastDate + (10L * 24 * 3600 * 1000),
                    severity = "LOW",
                    logType = "INSPECTION",
                    plantId = "Chennai Plant A",
                    assetId = "EXT-CH1-WH-101"
                )
            )

            for (act in initialActivities) {
                activityLogDao.insertActivity(act)
            }

            // Seed initial notifications
            val initialNotifications = listOf(
                Notification(
                    id = "N1",
                    title = "Diesel Pump Battery Fault",
                    description = "Standby Diesel Fire Pump PMP-CH1-PH-302 battery charge circuit failed. Battery voltage critical (19.8V). Urgent inspection required.",
                    timestamp = pastDate + (5L * 24 * 3600 * 1000),
                    severity = "CRITICAL",
                    assetId = "PMP-CH1-PH-302"
                ),
                Notification(
                    id = "N2",
                    title = "2 Inspections Overdue",
                    description = "Assets in Server Room and Gas Cylinder Storage have missed their monthly inspection window.",
                    timestamp = now - (1L * 24 * 3600 * 1000),
                    severity = "HIGH",
                    assetId = "EXT-CH1-SR-102"
                )
            )

            for (notif in initialNotifications) {
                notificationDao.insertNotification(notif)
            }
        }
    }
}
