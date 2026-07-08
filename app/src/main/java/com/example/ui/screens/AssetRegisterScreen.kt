package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FireAsset
import com.example.data.model.InspectionLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.FireAssetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetRegisterScreen(
    viewModel: FireAssetViewModel,
    onNavigateToInspection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredAssets by viewModel.filteredAssets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPlant by viewModel.selectedPlant.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()

    var activeDetailAsset by remember { mutableStateOf<FireAsset?>(null) }
    var reportFaultAssetId by remember { mutableStateOf<String?>(null) }
    var faultReason by remember { mutableStateOf("") }

    val categoriesList = listOf(
        "All Categories" to "All",
        "PORTABLE" to "Portable",
        "WATER_BASED" to "Water Suppression",
        "PUMPING" to "Pump House",
        "DETECTION" to "Detection & Alarm",
        "SPECIAL_HAZARD" to "Gas & Chemical",
        "LIFE_SAFETY" to "Life Safety"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- 1. Top Search and Information ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FIRE ASSET REGISTER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        placeholder = { Text("Search ID, type, room or building...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("asset_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )

                    // Categories slider
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoriesList) { (catId, catLabel) ->
                            val isSelected = selectedCategory == catId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCategory(catId) },
                                label = { 
                                    Text(
                                        text = catLabel, 
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BoldPrimaryContainer,
                                    selectedLabelColor = BoldPrimaryBlue,
                                    selectedLeadingIconColor = BoldPrimaryBlue
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("category_chip_$catId")
                            )
                        }
                    }
                }
            }

            // --- 2. Main Lazy List of Assets ---
            if (filteredAssets.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Fire Assets Found",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try refining your search text or changing the plant and category filters.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                    items(filteredAssets) { asset ->
                        AssetCard(
                            asset = asset,
                            onViewDetail = { activeDetailAsset = asset },
                            onInspect = { onNavigateToInspection(asset.id) },
                            onReportFault = { reportFaultAssetId = asset.id }
                        )
                    }
                }
            }
        }

        // --- 3. Asset Details Overlay Panel (BottomSheet effect) ---
        AnimatedVisibility(
            visible = activeDetailAsset != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            activeDetailAsset?.let { asset ->
                val assetLogs = allLogs.filter { it.assetId == asset.id }
                AssetDetailOverlay(
                    asset = asset,
                    logs = assetLogs,
                    onClose = { activeDetailAsset = null },
                    onInspect = {
                        activeDetailAsset = null
                        onNavigateToInspection(asset.id)
                    },
                    onResolveBreakdown = { remarks ->
                        viewModel.repairAsset(asset.id, remarks, "Amit Sharma")
                        activeDetailAsset = null
                    }
                )
            }
        }

        // --- 4. Report Fault Alert Dialog ---
        if (reportFaultAssetId != null) {
            AlertDialog(
                onDismissRequest = {
                    reportFaultAssetId = null
                    faultReason = ""
                },
                title = { Text("Report Asset Failure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Provide failure details for the critical work order escalation:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        OutlinedTextField(
                            value = faultReason,
                            onValueChange = { faultReason = it },
                            placeholder = { Text("Enter the specific physical fault or breakdown observations...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("fault_reason_input"),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.reportAssetBreakdown(
                                assetId = reportFaultAssetId!!,
                                reason = faultReason.ifEmpty { "Unspecified operational fault" },
                                userName = "Amit Sharma"
                            )
                            reportFaultAssetId = null
                            faultReason = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        modifier = Modifier.testTag("confirm_fault_button")
                    ) {
                        Text("Submit Alert", fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        reportFaultAssetId = null
                        faultReason = ""
                    }) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun AssetCard(
    asset: FireAsset,
    onViewDetail: () -> Unit,
    onInspect: () -> Unit,
    onReportFault: () -> Unit
) {
    val statusColor = when (asset.currentStatus) {
        "HEALTHY" -> AccentGreen
        "DUE" -> AccentAmber
        "OVERDUE" -> PrimaryRed
        "BREAKDOWN" -> PrimaryRed
        else -> AccentAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (asset.currentStatus == "BREAKDOWN") PrimaryRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = asset.id,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = asset.currentStatus,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Text(
                text = asset.assetType,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Location
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${asset.plant} • ${asset.building}, ${asset.floor}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewDetail,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Digital Twin Profile", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (asset.isOperational) {
                        IconButton(
                            onClick = onReportFault,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryRed.copy(alpha = 0.1f)),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = PrimaryRed)
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = "Report Fault", modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = onInspect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (asset.currentStatus == "DUE" || asset.currentStatus == "OVERDUE") AccentAmber else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (asset.currentStatus == "HEALTHY") "Re-Inspect" else "Inspect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun AssetDetailOverlay(
    asset: FireAsset,
    logs: List<InspectionLog>,
    onClose: () -> Unit,
    onInspect: () -> Unit,
    onResolveBreakdown: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var resolveRemarks by remember { mutableStateOf("") }
    var resolveDrawerExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle/Close Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ASSET DIGITAL TWIN PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = asset.id,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Profile")
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (asset.category) {
                                "PORTABLE" -> Icons.Default.FireExtinguisher
                                "WATER_BASED" -> Icons.Default.Water
                                "PUMPING" -> Icons.Default.Engineering
                                else -> Icons.Default.Sensors
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${asset.assetType} schematic loaded",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Breakdown warning
                if (asset.currentStatus == "BREAKDOWN") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryRed.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PrimaryRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("SYSTEM FAULT REPORTED", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryRed)
                                Text(
                                    text = asset.remarks.ifEmpty { "General mechanical / circuitry failure flagged." },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                
                                if (!resolveDrawerExpanded) {
                                    TextButton(
                                        onClick = { resolveDrawerExpanded = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Log Repairs & Re-commission", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = resolveRemarks,
                                        onValueChange = { resolveRemarks = it },
                                        placeholder = { Text("Describe repairs done...", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth().height(64.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        textStyle = TextStyle(fontSize = 12.sp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { resolveDrawerExpanded = false }) {
                                            Text("Cancel", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onResolveBreakdown(resolveRemarks.ifEmpty { "Repairs certified by technician." }) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Certify Repair", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Specifications
                DetailSection(title = "TECHNICAL IDENTIFICATION") {
                    SpecRow("QR Code", asset.qrCode)
                    SpecRow("Barcode Number", asset.barcode)
                    SpecRow("Manufacturer", asset.manufacturer)
                    SpecRow("Model Designation", asset.model)
                    SpecRow("Serial Number", asset.serialNumber)
                    SpecRow("Capacity Rating", asset.capacity)
                    SpecRow("Test Pressure", asset.pressureRating)
                }

                DetailSection(title = "LOCATION HIERARCHY") {
                    SpecRow("Site Plant", asset.plant)
                    SpecRow("Facility Building", asset.building)
                    SpecRow("Floor Zone", asset.floor)
                    SpecRow("EHS Department", asset.department)
                    SpecRow("Area Description", asset.area)
                    SpecRow("Precise Coordinates", "Lat: ${String.format("%.4f", asset.gpsLat)}, Lng: ${String.format("%.4f", asset.gpsLng)}")
                }

                DetailSection(title = "COMPLIANCE SCHEDULE") {
                    SpecRow("Last Inspection", if (asset.lastInspectionDate > 0) sdf.format(Date(asset.lastInspectionDate)) else "No previous log")
                    SpecRow("Next Inspection Due", sdf.format(Date(asset.nextInspectionDate)))
                    SpecRow("Maintenance Due", sdf.format(Date(asset.nextMaintenanceDate)))
                    SpecRow("Hydro Test Due", if (asset.nextHydroTestDate > 0) sdf.format(Date(asset.nextHydroTestDate)) else "N/A")
                    SpecRow("Warranty Expiry", sdf.format(Date(asset.warrantyExpiry)))
                }

                // Inspection history logs
                DetailSection(title = "INSPECTION HISTORY LOGS") {
                    if (logs.isEmpty()) {
                        Text(
                            "No inspection history logs recorded yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        logs.take(3).forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sdf.format(Date(log.timestamp)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )

                                    val badgeColor = if (log.resultStatus == "PASS") AccentGreen else PrimaryRed
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(badgeColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(log.resultStatus, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "Inspector: ${log.userName} (ID: ${log.userId})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (log.notes.isNotEmpty()) {
                                    Text(
                                        text = "Remarks: ${log.notes}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onInspect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("inspect_twin_cta"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute Routine Audit / Inspection", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        content()
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
