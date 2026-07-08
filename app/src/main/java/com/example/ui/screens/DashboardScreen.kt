package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityLog
import com.example.data.model.FireAsset
import com.example.data.model.Notification
import com.example.ui.theme.*
import com.example.ui.viewmodel.FireAssetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: FireAssetViewModel,
    onNavigateToInspection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.allAssets.collectAsState()
    val filteredAssets by viewModel.filteredAssets.collectAsState()
    val selectedPlant by viewModel.selectedPlant.collectAsState()
    val activities by viewModel.recentActivities.collectAsState()
    val notifications by viewModel.allNotifications.collectAsState()

    // Aggregate statistics
    val totalAssets = filteredAssets.size
    val healthyAssets = filteredAssets.count { it.currentStatus == "HEALTHY" }
    val inspectionDue = filteredAssets.count { it.currentStatus == "DUE" }
    val inspectionOverdue = filteredAssets.count { it.currentStatus == "OVERDUE" }
    val breakdownAssets = filteredAssets.count { it.currentStatus == "BREAKDOWN" }
    val oosAssets = filteredAssets.count { it.currentStatus == "OOS" }
    
    // Hydro & Refilling counts
    val now = System.currentTimeMillis()
    val days30 = 30L * 24 * 3600 * 1000
    val hydroTestDue = filteredAssets.count { it.nextHydroTestDate in 1..(now + days30) }
    val refillingDue = filteredAssets.count { it.nextMaintenanceDate in 1..(now + days30) && it.category == "PORTABLE" }
    val expiredAssets = filteredAssets.count { it.warrantyExpiry in 1..now }

    // Compliance Metrics
    val complianceScore = if (totalAssets > 0) {
        ((healthyAssets + inspectionDue).toFloat() / totalAssets * 100).toInt()
    } else 100

    val assetAvailability = if (totalAssets > 0) {
        ((totalAssets - breakdownAssets).toFloat() / totalAssets * 100).toInt()
    } else 100

    // Dropdown States
    var plantDropdownExpanded by remember { mutableStateOf(false) }
    val plantsList = listOf("All Plants", "Chennai Plant A", "Pune Plant B", "Bengaluru Tech Park")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // --- 1. Header with Dropdown ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile initials bubble (JD)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column {
                        Text(
                            text = "PLANT INSPECTOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "IgnisGuard Enterprise",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Box {
                    Button(
                        onClick = { plantDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("plant_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Factory,
                            contentDescription = "Plant Selector",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedPlant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = plantDropdownExpanded,
                        onDismissRequest = { plantDropdownExpanded = false }
                    ) {
                        plantsList.forEach { plant ->
                            DropdownMenuItem(
                                text = { Text(plant, fontSize = 13.sp) },
                                onClick = {
                                    viewModel.setPlant(plant)
                                    plantDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Compliance Score Hero Card (Bold Typography style) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Right-side concentric decorative circles matching HTML
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 20.dp, y = (-20).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .border(
                                    width = 12.dp,
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .border(
                                        width = 8.dp,
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Content on the left
                    Column(
                        modifier = Modifier.fillMaxWidth(0.7f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Compliance Score",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = complianceScore.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Text(
                            text = "Target: 100% • $selectedPlant",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // --- Asset Availability Companion Card (Bold Typography style) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Asset Availability",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = assetAvailability.toString(),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Emergency System Uptime",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 8.dp)) {
                        CircularProgressIndicator(
                            progress = { assetAvailability / 100f },
                            modifier = Modifier.size(64.dp),
                            color = if (assetAvailability > 95) AccentGreen else PrimaryRed,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (assetAvailability > 95) AccentGreen else PrimaryRed
                            )
                        )
                    }
                }
            }
        }

        // --- 2. High-Density KPI Grid ---
        item {
            Text(
                text = "OPERATIONAL METRICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "TOTAL ASSETS",
                    value = totalAssets.toString(),
                    icon = Icons.Default.Inventory,
                    tint = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "HEALTHY STATUS",
                    value = "$healthyAssets ($complianceScore%)",
                    icon = Icons.Default.CheckCircle,
                    tint = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "INSPECTION DUE",
                    value = inspectionDue.toString(),
                    icon = Icons.Default.Schedule,
                    tint = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "OVERDUE CHECKS",
                    value = inspectionOverdue.toString(),
                    icon = Icons.Default.Warning,
                    tint = PrimaryRed,
                    modifier = Modifier.weight(1f),
                    isAlert = inspectionOverdue > 0
                )
                KpiCard(
                    title = "BREAKDOWN / FAULTS",
                    value = breakdownAssets.toString(),
                    icon = Icons.Default.ReportProblem,
                    tint = PrimaryRed,
                    modifier = Modifier.weight(1f),
                    isAlert = breakdownAssets > 0
                )
                KpiCard(
                    title = "HYDRO TESTS DUE",
                    value = hydroTestDue.toString(),
                    icon = Icons.Default.Compress,
                    tint = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 3. Critical Infrastructure Status feeds (IoT Feeds) ---
        item {
            Text(
                text = "CRITICAL OPERATIONS STATUS (IOT HEARTBEATS)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pump House Status
                val dieselDown = assets.any { it.assetType == "Diesel Fire Pump" && !it.isOperational }
                val electricDown = assets.any { it.assetType == "Electric Fire Pump" && !it.isOperational }
                val pumpStatusString = when {
                    dieselDown && electricDown -> "CRITICAL OUTAGE"
                    dieselDown -> "RUNNING - STANDBY FAULT"
                    else -> "STABLE / AUTO"
                }
                val pumpColor = when {
                    dieselDown && electricDown -> PrimaryRed
                    dieselDown -> AccentAmber
                    else -> AccentGreen
                }
                StatusRow(
                    label = "Fire Pump Room",
                    statusText = pumpStatusString,
                    color = pumpColor,
                    icon = Icons.Default.Settings
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))

                // Alarm Panel Status
                val alarmsUnreadCount = notifications.count { it.severity == "CRITICAL" }
                val alarmStatusString = if (alarmsUnreadCount > 0) "$alarmsUnreadCount ACTIVE ALARMS" else "NORMAL / SECURITY MONITOR"
                val alarmColor = if (alarmsUnreadCount > 0) PrimaryRed else AccentGreen
                StatusRow(
                    label = "Fire Alarm Panels",
                    statusText = alarmStatusString,
                    color = alarmColor,
                    icon = Icons.Default.NotificationImportant
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))

                // Hydrant Network Pressure
                val hydrantDown = assets.any { it.assetType == "Fire Hydrant System" && !it.isOperational }
                val hydrantPressure = if (hydrantDown) "3.1 Bar - CRITICAL LOSS" else "7.8 Bar - STABLE"
                val hydrantColor = if (hydrantDown) PrimaryRed else AccentGreen
                StatusRow(
                    label = "Hydrant Mainline Pressure",
                    statusText = hydrantPressure,
                    color = hydrantColor,
                    icon = Icons.Default.WaterDrop
                )
            }
        }

        // --- 5. Upcoming Due Schedules ---
        item {
            Text(
                text = "UPCOMING INSPECTIONS DUE (Next 15 Days)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val upcomingAssets = assets.filter {
                it.nextInspectionDate > now && it.nextInspectionDate <= (now + 15L * 24 * 3600 * 1000)
            }.sortedBy { it.nextInspectionDate }

            if (upcomingAssets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No inspections due in the next 15 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcomingAssets.take(3).forEach { asset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable { onNavigateToInspection(asset.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AccentAmber.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = asset.id,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${asset.assetType} • ${asset.building}, ${asset.floor}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Due " + SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(asset.nextInspectionDate)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                        }
                    }
                }
            }
        }

        // --- 6. Recent Operational Activities ---
        item {
            Text(
                text = "RECENT ACTIVITIES (Audit Trail)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No recent activities logged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    activities.take(4).forEach { activity ->
                        ActivityRow(activity = activity)
                        if (activity != activities.take(4).last()) {
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    // Select the bold semantic colors based on tint parameter
    val containerColor = when (tint) {
        PrimaryRed -> BoldErrorContainer
        AccentAmber -> BoldSecondaryContainer
        AccentGreen -> BoldTertiaryContainer
        InfoBlue -> Color(0xFFD1E4FF)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when (tint) {
        PrimaryRed -> BoldErrorBorder
        AccentAmber -> BoldSecondaryBorder
        AccentGreen -> Color(0xFFC8E6C9)
        InfoBlue -> Color(0xFFA6C8FF)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    }
    val contentColor = when (tint) {
        PrimaryRed -> BoldErrorRed
        AccentAmber -> BoldSecondaryAmber
        AccentGreen -> BoldTertiaryGreen
        InfoBlue -> BoldPrimaryBlue
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .height(108.dp)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    statusText: String,
    color: Color,
    icon: ImageVector
) {
    if (color == PrimaryRed) {
        // Red critical alarm alert card layout matching HTML mockup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BoldErrorRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                    Text(
                        text = "$label: $statusText",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "ACTIVE",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                )
            }
        }
    } else {
        // Normal row layout matching HTML mockup with indicator pulse dot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                )
            }

            val isPressure = label.lowercase().contains("pressure")
            if (isPressure) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = color,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityRow(activity: ActivityLog) {
    val icon = when (activity.logType) {
        "INSPECTION" -> Icons.Default.CheckCircle
        "ALERT" -> Icons.Default.ReportProblem
        "MAINTENANCE" -> Icons.Default.Build
        else -> Icons.Default.Info
    }
    val iconColor = when (activity.severity) {
        "CRITICAL" -> PrimaryRed
        "HIGH" -> PrimaryRed
        "MEDIUM" -> AccentAmber
        else -> AccentGreen
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = activity.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activity.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Text(
                text = activity.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
            if (activity.assetId.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Asset ID: ${activity.assetId}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
