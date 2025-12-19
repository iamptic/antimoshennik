package com.antimoshennik.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TelegramNotifier {
    private const val TAG = "TelegramNotifier"
    
    // TODO: Заменить на реальный URL сервера
    private const val SERVER_URL = "https://your-server.com/api/alert"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Отправляет уведомление через Telegram бота
     */
    suspend fun sendAlert(
        context: Context,
        score: Int,
        text: String
    ): Boolean = withContext(Dispatchers.IO) {
        
        // Проверяем, включены ли уведомления
        if (!Settings.isTelegramEnabled(context)) {
            Log.d(TAG, "Telegram notifications disabled")
            return@withContext false
        }
        
        val userId = Settings.getTelegramChatId(context)
        if (userId.isBlank()) {
            Log.d(TAG, "Telegram user ID not set")
            return@withContext false
        }
        
        try {
            val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date())
            
            val json = JSONObject().apply {
                put("userId", userId)
                put("score", score)
                put("text", text.take(500)) // Ограничиваем длину
                put("timestamp", timestamp)
            }
            
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Alert sent successfully")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to send alert: ${response.code}")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending alert", e)
            return@withContext false
        }
    }
    
    /**
     * Проверяет соединение с сервером
     */
    suspend fun testConnection(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(SERVER_URL.replace("/api/alert", "/health"))
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }
}
