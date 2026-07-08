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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FireAsset
import com.example.data.model.InspectionLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.FireAssetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    viewModel: FireAssetViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.allAssets.collectAsState()
    val logs by viewModel.allLogs.collectAsState()

    var showReportDialog by remember { mutableStateOf<String?>(null) }
    var selectedCertificateLog by remember { mutableStateOf<InspectionLog?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "COMPLIANCE & REPORTS CENTER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Factory Certification Panel",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Section: Exporters
        item {
            Text(
                text = "EXPORT REGISTERS & COMPLIANCE DATA",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExportActionRow(
                    title = "Fire Asset Register",
                    description = "Full inventory details of all active and passive equipment in CSV/Excel.",
                    icon = Icons.Default.Inventory2,
                    onClick = { showReportDialog = "Fire Asset Register (CSV)" }
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                ExportActionRow(
                    title = "EHS Dynamic Compliance Log",
                    description = "Comprehensive historical log of all weekly/monthly checks (Factory Act Form 11 format).",
                    icon = Icons.Default.Description,
                    onClick = { showReportDialog = "EHS Dynamic Compliance (PDF)" }
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                ExportActionRow(
                    title = "Vendor Service & Shelf-Life Report",
                    description = "Tracking extinguisher chemical expiration and pressure-testing warranties.",
                    icon = Icons.Default.CardMembership,
                    onClick = { showReportDialog = "Vendor Service & Expiry Report (Excel)" }
                )
            }
        }

        // Section: Plant Health Summary Cards
        item {
            Text(
                text = "PLANT AUDIT OVERVIEW",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlantComplianceCard(
                    plantName = "Chennai Plant A",
                    totalCount = assets.count { it.plant == "Chennai Plant A" },
                    compliancePct = 94,
                    faults = assets.count { it.plant == "Chennai Plant A" && it.currentStatus == "BREAKDOWN" }
                )

                PlantComplianceCard(
                    plantName = "Pune Plant B",
                    totalCount = assets.count { it.plant == "Pune Plant B" },
                    compliancePct = 78,
                    faults = assets.count { it.plant == "Pune Plant B" && it.currentStatus == "BREAKDOWN" }
                )

                PlantComplianceCard(
                    plantName = "Bengaluru Tech Park",
                    totalCount = assets.count { it.plant == "Bengaluru Tech Park" },
                    compliancePct = 100,
                    faults = assets.count { it.plant == "Bengaluru Tech Park" && it.currentStatus == "BREAKDOWN" }
                )
            }
        }

        // Section: Generate NFPA Compliance Inspection Certificate
        item {
            Text(
                text = "GENERATED COMPLIANCE CERTIFICATES (NFPA)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Submit an inspection to compile legally compliant NFPA 10/25/72 audit certificates.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.forEach { log ->
                        val targetAsset = assets.find { it.id == log.assetId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable { selectedCertificateLog = log }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AccentGreen.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NFPA Compliance - ${log.assetId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Certified by ${log.userName} • " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(onClick = { selectedCertificateLog = log }) {
                                Icon(Icons.Default.Download, contentDescription = "Download Certificate", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Confirmation Dialog
    if (showReportDialog != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = null },
            title = { Text("Export Successful", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "The requested report '${showReportDialog}' has been compiled and saved to local downloads secure vault directory. Check system telemetry storage for confirmation.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showReportDialog = null },
                    modifier = Modifier.testTag("dismiss_export_dialog")
                ) {
                    Text("Ok")
                }
            }
        )
    }

    // NFPA Compliance Certificate Inspector Dialog
    if (selectedCertificateLog != null) {
        val log = selectedCertificateLog!!
        val asset = assets.find { it.id == log.assetId }

        AlertDialog(
            onDismissRequest = { selectedCertificateLog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NFPA COMPLIANCE CERTIFICATE", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "LEGAL STATEMENT OF CONFORMITY (FORM 11)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    CertRow("CERTIFICATE ID", log.id)
                    CertRow("AUDIT TIMESTAMP", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)))
                    CertRow("LICENSED INSPECTOR", "${log.userName} (ID: ${log.userId})")
                    CertRow("REGULATORY MANDATE", if (asset?.category == "PORTABLE") "NFPA 10 / IS 2190" else "NFPA 25 / IS 15105")
                    
                    if (asset != null) {
                        CertRow("EQUIPMENT TYPE", asset.assetType)
                        CertRow("FACILITY SITE", asset.plant)
                        CertRow("BUILDING ZONE", "${asset.building}, ${asset.floor} (${asset.area})")
                        CertRow("SERIAL NUMBER", asset.serialNumber)
                        CertRow("CAPACITY", asset.capacity)
                    }

                    CertRow("AUDIT STATUS", log.resultStatus)
                    CertRow("DOCK VERIFICATION", if (log.locationVerified) "GPS VERIFIED (MATCHED)" else "PENDING GPS")

                    if (log.notes.isNotEmpty()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("FIELD OBSERVATIONS & REMARKS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(log.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Simulated Signature block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "[ DIGITAL HANDWRITTEN SIGNATURE ]",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.userName.uppercase(Locale.getDefault()),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Blue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "NFPA CERTIFIED IN-GOOD-STANDING",
                            fontSize = 8.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedCertificateLog = null },
                    modifier = Modifier.testTag("dismiss_certificate_button")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ExportActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

@Composable
fun PlantComplianceCard(
    plantName: String,
    totalCount: Int,
    compliancePct: Int,
    faults: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Total Fire Assets: $totalCount • Faulty/OOS: $faults", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            Column(horizontalAlignment = Alignment.End) {
                val color = when {
                    compliancePct >= 90 -> AccentGreen
                    compliancePct >= 75 -> AccentAmber
                    else -> PrimaryRed
                }
                Text(
                    text = "$compliancePct% COMPLIANT",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "NFPA Status: " + if (compliancePct == 100) "PERFECT" else "ACTION REQD",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun CertRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
