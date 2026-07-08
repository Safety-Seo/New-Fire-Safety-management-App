package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Calls the Gemini API to generate content.
     * Uses gemini-3.5-flash by default for general operations, or gemini-3.1-pro-preview for complex reasoning.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String = "You are a professional industrial safety engineer and fire protection systems consultant.",
        model: String = "gemini-3.5-flash",
        enableHighThinking: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is missing or is placeholder")
            return@withContext "Error: Gemini API Key is not set. Please add your GEMINI_API_KEY in the Secrets Panel in AI Studio."
        }

        // Construct Request JSON
        val requestJson = JSONObject()
        
        // System instruction if provided
        if (systemInstruction.isNotEmpty()) {
            val systemPart = JSONObject().put("text", systemInstruction)
            val systemContent = JSONObject().put("parts", JSONArray().put(systemPart))
            requestJson.put("systemInstruction", systemContent)
        }

        // Contents
        val textPart = JSONObject().put("text", prompt)
        val contentObj = JSONObject().put("parts", JSONArray().put(textPart))
        requestJson.put("contents", JSONArray().put(contentObj))

        // Generation Config
        val genConfigObj = JSONObject()
        if (enableHighThinking && model == "gemini-3.1-pro-preview") {
            // Enable thinking configuration for gemini-3.1-pro-preview as per prompt
            val thinkingConfig = JSONObject().put("thinkingLevel", "HIGH")
            genConfigObj.put("thinkingConfig", thinkingConfig)
        } else {
            // Default parameters
            genConfigObj.put("temperature", 0.7)
        }
        requestJson.put("generationConfig", genConfigObj)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val url = "$BASE_URL$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed with code ${response.code}: $bodyString")
                    return@withContext "Error calling Gemini API: Server returned code ${response.code}"
                }

                // Parse response JSON
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                    }
                }
                return@withContext "No response candidates returned."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception calling Gemini API", e)
            return@withContext "Error: Failed to connect to Gemini API. Check internet permission and connection. Details: ${e.localizedMessage}"
        }
    }
}
