package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FireAsset
import com.example.ui.theme.*
import com.example.ui.viewmodel.FireAssetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionScreen(
    viewModel: FireAssetViewModel,
    modifier: Modifier = Modifier
) {
    val activeAsset by viewModel.activeAsset.collectAsState()
    val allAssets by viewModel.allAssets.collectAsState()
    val checklistResults by viewModel.checklistResults.collectAsState()
    val customValueResults by viewModel.customValueResults.collectAsState()
    val capturedPhotos by viewModel.capturedPhotos.collectAsState()
    val signatureData by viewModel.digitalSignature.collectAsState()

    var manualAssetId by remember { mutableStateOf("") }
    var technicianNotes by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf("") }
    var forceFailToggle by remember { mutableStateOf(false) }

    // Navigation and state when no active asset is loaded
    if (activeAsset == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "QR SCAN & INSPECTION GATEWAY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Begin Field Audit",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // QR Code Camera Scanner Sandbox
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan Physical Asset Tag",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Align the asset's QR Code or Barcode inside the camera focus window to initiate. (Since emulator camera is unavailable, use manual key-in below)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Manual Entry
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MANUAL TAG ID BYPASS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualAssetId,
                        onValueChange = {
                            manualAssetId = it
                            searchError = ""
                        },
                        placeholder = { Text("e.g. EXT-CH1-WH-101", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("manual_asset_id_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        isError = searchError.isNotEmpty()
                    )

                    Button(
                        onClick = {
                            val target = allAssets.find { it.id.equals(manualAssetId.trim(), ignoreCase = true) }
                            if (target != null) {
                                viewModel.selectAsset(target.id)
                                manualAssetId = ""
                            } else {
                                searchError = "Asset Tag ID not registered in database."
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("submit_manual_tag_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Verify Tag", fontSize = 13.sp)
                    }
                }
                if (searchError.isNotEmpty()) {
                    Text(searchError, color = PrimaryRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Quick Selection list for testing
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PENDING AUDIT SCHEDULE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                
                val dueAssets = allAssets.filter { it.currentStatus == "DUE" || it.currentStatus == "OVERDUE" || !it.isOperational }
                if (dueAssets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active schedules overdue at this plant.", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    dueAssets.take(4).forEach { asset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectAsset(asset.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(asset.id, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${asset.assetType} • ${asset.building}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            
                            val labelColor = if (asset.currentStatus == "OVERDUE" || !asset.isOperational) PrimaryRed else AccentAmber
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(labelColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(asset.currentStatus, color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Active inspection form
        val asset = activeAsset!!
        
        // Define Dynamic Checklist structure based on category
        val questions = remember(asset.category) {
            getChecklistForCategory(asset.category)
        }

        // Initialize question states in viewModel if empty
        LaunchedEffect(questions) {
            questions.forEach { q ->
                if (checklistResults[q.key] == null) {
                    viewModel.updateChecklistResult(q.key, true) // default to compliant (PASS)
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Audit Sheet: ${asset.id}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearActiveAsset() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.submitInspection(
                                userId = "USR-002",
                                userName = "Amit Sharma",
                                notes = technicianNotes,
                                forceStatusFail = forceFailToggle
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_inspection_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddTask, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Certify & Submit Inspection", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                // Asset Details Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(asset.category) {
                                        "PORTABLE" -> Icons.Default.FireExtinguisher
                                        "WATER_BASED" -> Icons.Default.Water
                                        "PUMPING" -> Icons.Default.Engineering
                                        else -> Icons.Default.Sensors
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(asset.assetType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${asset.building} • ${asset.floor} • ${asset.area}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("MFR: ${asset.manufacturer} • Model: ${asset.model}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                // Compliance Mandate Reminder
                item {
                    val mandateText = when (asset.category) {
                        "PORTABLE" -> "NFPA 10 & IS 2190 Portable Fire Extinguisher compliance mandates a monthly visual verification of pressure gauge, physical body integrity, nozzle blockages, and tamper seal."
                        "WATER_BASED" -> "NFPA 25 and IS 15105 Water-based fire protection requires verifying landing valve accessibility, hose completeness inside the Hose Box, leak-free seals, and annual hydrostatic flow test."
                        "PUMPING" -> "NFPA 25 Weekly churn testing is mandatory for fire pump control mechanisms to confirm prompt diesel engine crank voltage, jockey pressure switches, and stable pipeline pressure."
                        else -> "NFPA 72 Addressable fire detection mandate: verify Loop circuit backbones, visual beacon signaling, and manual call station glass actuators."
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = "Legal Mandate", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mandateText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // DYNAMIC CHECKLIST ENGINE
                item {
                    Text(
                        text = "COMPLIANCE AUDIT CHECKS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Checklist Items
                items(questions) { q ->
                    val isChecked = checklistResults[q.key] ?: true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(q.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(q.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isChecked) "COMPLIANT" else "NON-COMPLIANT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChecked) AccentGreen else PrimaryRed,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { viewModel.updateChecklistResult(q.key, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentGreen,
                                    checkedTrackColor = AccentGreen.copy(alpha = 0.2f),
                                    uncheckedThumbColor = PrimaryRed,
                                    uncheckedTrackColor = PrimaryRed.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("switch_${q.key}")
                            )
                        }
                    }
                }

                // Quantitative inputs based on category
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "METROLOGY & PRESSURE LOGS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (asset.category == "PORTABLE") {
                            // Weight / pressure rating
                            val currentWeight = customValueResults["weight_kg"] ?: ""
                            val currentPressure = customValueResults["pressure_bar"] ?: ""
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = currentWeight,
                                    onValueChange = { viewModel.updateCustomValue("weight_kg", it) },
                                    label = { Text("Weight (kg)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("weight_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                OutlinedTextField(
                                    value = currentPressure,
                                    onValueChange = { viewModel.updateCustomValue("pressure_bar", it) },
                                    label = { Text("Pressure (bar)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("pressure_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        } else if (asset.category == "WATER_BASED") {
                            // Static vs Residual bar
                            val staticPressure = customValueResults["static_bar"] ?: ""
                            val residualPressure = customValueResults["residual_bar"] ?: ""

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = staticPressure,
                                    onValueChange = { viewModel.updateCustomValue("static_bar", it) },
                                    label = { Text("Static Pressure (bar)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("static_pressure_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                OutlinedTextField(
                                    value = residualPressure,
                                    onValueChange = { viewModel.updateCustomValue("residual_bar", it) },
                                    label = { Text("Residual Flow (bar)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("residual_pressure_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        } else if (asset.category == "PUMPING") {
                            // Battery Voltage / Fuel level
                            val batteryVolt = customValueResults["battery_volt"] ?: ""
                            val fuelLevel = customValueResults["fuel_pct"] ?: ""

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = batteryVolt,
                                    onValueChange = { viewModel.updateCustomValue("battery_volt", it) },
                                    label = { Text("Battery Voltage (V)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("battery_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                OutlinedTextField(
                                    value = fuelLevel,
                                    onValueChange = { viewModel.updateCustomValue("fuel_pct", it) },
                                    label = { Text("Fuel Reserves (%)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("fuel_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                }

                // Photographic Evidence
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PHOTOGRAPHIC AUDIT EVIDENCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.addSimulatedPhoto() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("add_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Capture Seal / Gauge Photo", fontSize = 12.sp)
                            }
                            
                            Text(
                                text = "${capturedPhotos.size} photo(s) attached",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        if (capturedPhotos.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                capturedPhotos.forEach { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Digital Signature Pad (Canvas drawing!)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DIGITAL COMPLIANCE SIGNATURE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        SignaturePad(
                            onSignatureRecorded = { viewModel.setSignature(it) },
                            modifier = Modifier.testTag("signature_canvas_pad")
                        )
                    }
                }

                // Force Fail Checkbox
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryRed.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, PrimaryRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = forceFailToggle,
                            onCheckedChange = { forceFailToggle = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryRed),
                            modifier = Modifier.testTag("force_fail_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("FLAG CRITICAL FAILURE (OOS)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryRed)
                            Text("Check this box if physical defects (e.g. cracked hose, empty sand, broken glass) mandate immediate de-commissioning regardless of checklist logs.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                // Technician Notes
                item {
                    OutlinedTextField(
                        value = technicianNotes,
                        onValueChange = { technicianNotes = it },
                        placeholder = { Text("Enter supplementary audit observations, vendor replacements needed or hydro-testing warnings...", fontSize = 12.sp) },
                        label = { Text("Audit Notes / Remarks", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("inspection_remarks_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SignaturePad(
    onSignatureRecorded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Draw signature with finger inside the slate:",
                fontSize = 11.sp,
                color = Color.Black.copy(alpha = 0.6f)
            )

            TextButton(
                onClick = {
                    paths.clear()
                    currentPath = null
                    onSignatureRecorded("")
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Clear Slate", color = PrimaryRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val path = Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                            currentPath = path
                            paths.add(path)
                            onSignatureRecorded("recorded_signature_base64")
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.lineTo(change.position.x, change.position.y)
                            // trigger simple re-draw trigger
                            val temp = currentPath
                            currentPath = null
                            currentPath = temp
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            if (paths.isEmpty()) {
                Text(
                    text = "[ X SIGNATURE PAD OUTLINE ]",
                    color = Color.Black.copy(alpha = 0.15f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// Helper to bundle checklist questions
data class ChecklistQuestion(
    val key: String,
    val title: String,
    val description: String
)

fun getChecklistForCategory(category: String): List<ChecklistQuestion> {
    return when (category) {
        "PORTABLE" -> listOf(
            ChecklistQuestion("gauge_green", "Gauge Indicator in Green", "Visual check of pressure needle positioning in standard green zone."),
            ChecklistQuestion("seal_intact", "Security Lock & Seal Intact", "Confirm lead/plastic pull-tight tamper seal is un-broken."),
            ChecklistQuestion("nozzle_clear", "Hose & Discharge Nozzle Clear", "No clogging, debris, spiders, or physical splits in hose structure."),
            ChecklistQuestion("pin_inserted", "Metal Pull Safety Pin Inserted", "Verify the secondary steel pull safety pin is fully inserted.")
        )
        "WATER_BASED" -> listOf(
            ChecklistQuestion("landing_valve_leak", "Landing Valve Leak-free", "Verify gland packing and seals are dry under pressure."),
            ChecklistQuestion("hose_completeness", "Hose Box Completeness", "Are 2 standard synthetic fire hoses and the branch nozzle present?"),
            ChecklistQuestion("accessibility", "Clear Egress & Accessibility", "No equipment, pallets, or clutter obstructing Hydrant Post or Hose Reel."),
            ChecklistQuestion("leak_test", "Reel / Line Leakage Verification", "Hose reel glands are free of dripping water and fully lubricated.")
        )
        "PUMPING" -> listOf(
            ChecklistQuestion("oil_level", "Motor / Engine Lubricant Level", "Verify engine crankcase oil is between MIN and MAX notches."),
            ChecklistQuestion("batteries_operational", "DC Starter Battery Charged", "Ensure charger is operational and terminal cables are corrosion free."),
            ChecklistQuestion("vibration_normal", "Stable Sound & Vibration", "Confirm pump running churn emits standard non-destructive decibels."),
            ChecklistQuestion("auto_start", "Pressure Switch Auto Kick-in", "Verify the pump automatically turns on when line pressure drops.")
        )
        "DETECTION" -> listOf(
            ChecklistQuestion("panel_heartbeat", "FACP Heartbeat Normal", "Power indicator active, zero Loop faults, zero earth leaks logged."),
            ChecklistQuestion("mcp_test", "Call Station Actuator Healthy", "Glass panel intact and secondary addressable loops respond in test."),
            ChecklistQuestion("sounder_beacon_db", "Hooters & Beacons Operational", "Audibility exceeds 75dBA at standard room partitions during test loop.")
        )
        else -> listOf(
            ChecklistQuestion("general_health", "Physical Frame Intact", "No structural rusting, warping, or electromagnetic circuit faults."),
            ChecklistQuestion("label_clear", "Signage Highly Legible", "Emergency operating procedures or directional arrows clearly visible.")
        )
    }
}
