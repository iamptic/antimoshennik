package com.antimoshennik.app

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object YandexSpeechRecognizer {
    private const val TAG = "YandexSTT"
    private const val API_URL = "https://stt.api.cloud.yandex.net/speech/v1/stt:recognize"
    
    // API ключ (в проде лучше хранить в настройках)
    private const val API_KEY = "YOUR_YANDEX_API_KEY"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Распознаёт аудиофайл через Yandex SpeechKit
     * @param audioFile аудиофайл (OGG, MP3, WAV)
     * @return распознанный текст или null при ошибке
     */
    suspend fun recognize(context: Context, audioFile: File): String? = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists()) {
                Log.e(TAG, "File not found: ${audioFile.absolutePath}")
                return@withContext null
            }
            
            val audioBytes = audioFile.readBytes()
            if (audioBytes.size > 1024 * 1024 * 10) { // 10MB limit
                Log.e(TAG, "File too large: ${audioBytes.size} bytes")
                return@withContext null
            }
            
            Log.d(TAG, "Sending ${audioBytes.size} bytes to Yandex STT")
            
            // Определяем формат по расширению
            val format = when (audioFile.extension.lowercase()) {
                "ogg", "opus" -> "oggopus"
                "mp3" -> "mp3"
                "wav" -> "lpcm"
                else -> "mp3"
            }
            
            val url = "$API_URL?lang=ru-RU&format=$format&sampleRateHertz=16000"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Api-Key $API_KEY")
                .post(audioBytes.toRequestBody("audio/$format".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val result = json.optString("result", "")
                Log.d(TAG, "Recognition result: $result")
                return@withContext result.ifEmpty { null }
            } else {
                Log.e(TAG, "API error: ${response.code} - $responseBody")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Recognition failed", e)
            return@withContext null
        }
    }
    
    /**
     * Проверяет доступность API
     */
    suspend fun isAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Settings.isInternetAvailable(context) && API_KEY.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
