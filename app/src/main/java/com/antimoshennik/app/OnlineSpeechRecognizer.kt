package com.antimoshennik.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object OnlineSpeechRecognizer {
    private const val TAG = "OnlineSTT"
    
    // API временно недоступен (410 Gone)
    private const val HF_API_URL = "https://api-inference.huggingface.co/models/openai/whisper-small"
    private const val HF_TOKEN = "YOUR_HUGGINGFACE_TOKEN_HERE"
    
    private const val MAX_RETRIES = 2
    private const val RETRY_DELAY_MS = 3000L
    
    suspend fun recognizeFromFile(context: Context, filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found"))
            }
            
            val audioBytes = file.readBytes()
            
            for (attempt in 1..MAX_RETRIES) {
                val result = sendToWhisper(audioBytes)
                
                if (result.isSuccess) return@withContext result
                
                val error = result.exceptionOrNull()?.message ?: ""
                if (error.contains("503") || error.contains("500")) {
                    delay(RETRY_DELAY_MS)
                    continue
                }
                
                return@withContext result
            }
            
            return@withContext Result.failure(Exception("Max retries exceeded"))
            
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    private fun sendToWhisper(audioBytes: ByteArray): Result<String> {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(HF_API_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $HF_TOKEN")
            connection.setRequestProperty("Content-Type", "audio/mpeg")
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.doOutput = true
            
            connection.outputStream.use { it.write(audioBytes) }
            
            val responseCode = connection.responseCode
            
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val text = parseResponse(response)
                return Result.success(text)
            } else {
                return Result.failure(Exception("API error: $responseCode"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
    
    private fun parseResponse(response: String): String {
        return try {
            val json = JSONObject(response)
            json.optString("text", "")
        } catch (e: Exception) {
            try {
                val arr = JSONArray(response)
                if (arr.length() > 0) arr.getJSONObject(0).optString("text", "") else ""
            } catch (e2: Exception) {
                response.trim()
            }
        }
    }
}
