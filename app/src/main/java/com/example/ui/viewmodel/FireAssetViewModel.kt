package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FireAssetRepository
import com.example.api.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class FireAssetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = FireAssetRepository(
        database.userDao(),
        database.fireAssetDao(),
        database.inspectionLogDao(),
        database.activityLogDao(),
        database.notificationDao()
    )

    // Filter states
    val selectedPlant = MutableStateFlow("All Plants")
    val selectedCategory = MutableStateFlow("All Categories")
    val searchQuery = MutableStateFlow("")

    // Raw data flows
    val allAssets: StateFlow<List<FireAsset>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<InspectionLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotifications: StateFlow<List<Notification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentActivities: StateFlow<List<ActivityLog>> = repository.getRecentActivities(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Asset List
    val filteredAssets: StateFlow<List<FireAsset>> = combine(
        allAssets,
        selectedPlant,
        selectedCategory,
        searchQuery
    ) { assets, plant, category, query ->
        assets.filter { asset ->
            val matchesPlant = plant == "All Plants" || asset.plant == plant
            val matchesCategory = category == "All Categories" || asset.category == category
            val matchesQuery = query.isEmpty() ||
                    asset.id.contains(query, ignoreCase = true) ||
                    asset.assetType.contains(query, ignoreCase = true) ||
                    asset.area.contains(query, ignoreCase = true) ||
                    asset.building.contains(query, ignoreCase = true)
            matchesPlant && matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active scanned or selected asset
    private val _activeAssetId = MutableStateFlow<String?>(null)
    val activeAsset: StateFlow<FireAsset?> = _activeAssetId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getAssetByIdFlow(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Inspection state
    val checklistResults = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val customValueResults = MutableStateFlow<Map<String, String>>(emptyMap())
    val capturedPhotos = MutableStateFlow<List<String>>(emptyList())
    val digitalSignature = MutableStateFlow<String>("") // base64 representation

    // Gemini Chat & Assistant States
    private val _geminiChatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("assistant", "Hello! I am IgnisGuard AI, your NFPA and IS compliance safety supervisor. Ask me anything about your current assets, maintenance forecasts, or industrial fire safety regulations.")
    ))
    val geminiChatHistory: StateFlow<List<ChatMessage>> = _geminiChatHistory.asStateFlow()

    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading: StateFlow<Boolean> = _isGeminiLoading.asStateFlow()

    init {
        // Initialize the database with seeds if empty
        viewModelScope.launch {
            repository.prepopulateDatabase()
        }
    }

    // Set filters
    fun setPlant(plant: String) {
        selectedPlant.value = plant
    }

    fun setCategory(category: String) {
        selectedCategory.value = category
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    // Scan or Select Asset
    fun selectAsset(assetId: String) {
        _activeAssetId.value = assetId
        // Initialize checklist results
        checklistResults.value = emptyMap()
        customValueResults.value = emptyMap()
        capturedPhotos.value = emptyList()
        digitalSignature.value = ""
    }

    fun clearActiveAsset() {
        _activeAssetId.value = null
    }

    // Inspection interactions
    fun updateChecklistResult(key: String, passed: Boolean) {
        checklistResults.value = checklistResults.value + (key to passed)
    }

    fun updateCustomValue(key: String, value: String) {
        customValueResults.value = customValueResults.value + (key to value)
    }

    fun addSimulatedPhoto() {
        val photoNum = capturedPhotos.value.size + 1
        capturedPhotos.value = capturedPhotos.value + ("img_inspection_photo_$photoNum.jpg")
    }

    fun setSignature(base64: String) {
        digitalSignature.value = base64
    }

    fun submitInspection(userId: String, userName: String, notes: String, forceStatusFail: Boolean = false) {
        val asset = activeAsset.value ?: return
        viewModelScope.launch {
            // Check if any critical check in checklist failed
            val allChecklistPassed = checklistResults.value.values.all { it }
            val overallPassed = allChecklistPassed && !forceStatusFail

            // Form JSON structure for values and checklist answers
            val jsonObj = JSONObject()
            checklistResults.value.forEach { (k, v) -> jsonObj.put(k, v) }
            customValueResults.value.forEach { (k, v) -> jsonObj.put(k, v) }

            val photosArray = JSONArray()
            capturedPhotos.value.forEach { photosArray.put(it) }

            val log = InspectionLog(
                id = "LOG-${System.currentTimeMillis()}",
                assetId = asset.id,
                userId = userId,
                userName = userName,
                timestamp = System.currentTimeMillis(),
                checklistJson = jsonObj.toString(),
                photosJson = photosArray.toString(),
                digitalSignature = digitalSignature.value.ifEmpty { "digital_sign_amit_sharma" },
                resultStatus = if (overallPassed) "PASS" else "FAIL",
                locationVerified = true,
                notes = notes
            )

            repository.submitInspection(log, asset)
            
            // Clean up state
            clearActiveAsset()
        }
    }

    // Report unexpected breakdown
    fun reportAssetBreakdown(assetId: String, reason: String, userName: String) {
        viewModelScope.launch {
            repository.triggerAssetBreakdown(assetId, reason, userName)
        }
    }

    // Clear breakdown / repair
    fun repairAsset(assetId: String, remarks: String, userName: String) {
        viewModelScope.launch {
            repository.resolveBreakdown(assetId, remarks, userName)
        }
    }

    // Mark notification read
    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    // Clear all alerts
    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    // Gemini safety advisor chat interface
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        
        val userMsg = ChatMessage("user", message)
        _geminiChatHistory.value = _geminiChatHistory.value + userMsg
        _isGeminiLoading.value = true

        viewModelScope.launch {
            // Build Context
            val assets = allAssets.value
            val total = assets.size
            val healthy = assets.count { it.currentStatus == "HEALTHY" }
            val overdue = assets.count { it.currentStatus == "OVERDUE" }
            val breakdown = assets.count { it.currentStatus == "BREAKDOWN" }
            val activeBreakdowns = assets.filter { it.currentStatus == "BREAKDOWN" }
                .joinToString(", ") { "${it.id} (${it.assetType} at ${it.plant} - ${it.remarks})" }

            val systemContext = """
                You are IgnisGuard AI, an expert industrial fire protection architect, NFPA certified safety consultant, and plant manager's AI assistant.
                You are currently deployed inside the Enterprise Fire Asset Management System (EFAMS) of a manufacturing organization.
                
                Current factory stats:
                - Total registered fire safety assets: $total
                - Healthy assets: $healthy
                - Overdue inspections: $overdue
                - Asset breakdowns / failures: $breakdown
                ${if (breakdown > 0) "- Current breakdown details: $activeBreakdowns" else "- No active breakdowns at present."}
                
                Answer the user's question with precise compliance details, referring to NFPA 10 (Extinguishers), NFPA 25 (Water systems), or NFPA 72 (Alarms) where applicable.
                Be clear, concise, professional, and action-oriented. Provide direct steps when asked for maintenance or hazard troubleshooting.
            """.trimIndent()

            val response = GeminiClient.generateContent(
                prompt = message,
                systemInstruction = systemContext,
                model = "gemini-3.5-flash"
            )

            val botMsg = ChatMessage("assistant", response)
            _geminiChatHistory.value = _geminiChatHistory.value + botMsg
            _isGeminiLoading.value = false
        }
    }

    fun resetChat() {
        _geminiChatHistory.value = listOf(
            ChatMessage("assistant", "Chat history cleared. How can I assist you with fire compliance today?")
        )
    }
}

data class ChatMessage(
    val sender: String, // "user" or "assistant"
    val content: String
)
