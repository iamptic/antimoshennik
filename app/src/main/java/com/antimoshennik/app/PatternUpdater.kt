package com.antimoshennik.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object PatternUpdater {
    private const val TAG = "PatternUpdater"
    
    // GitHub raw URL - замени на свой репозиторий
    private const val GITHUB_BASE_URL = "https://raw.githubusercontent.com/iamptic/antimoshennik-patterns/main"
    private const val PATTERNS_URL = "$GITHUB_BASE_URL/patterns.json"
    private const val VERSION_URL = "$GITHUB_BASE_URL/version.txt"
    
    private const val LOCAL_PATTERNS_FILE = "patterns_updated.json"
    private const val PREFS_KEY_VERSION = "patterns_version"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    /**
     * Проверяет и скачивает обновления паттернов
     * Возвращает true если обновление было скачано
     */
    suspend fun checkAndUpdate(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for pattern updates...")
            
            // Получаем текущую версию
            val currentVersion = Settings.getPatternsVersion(context)
            
            // Получаем версию на сервере
            val remoteVersion = fetchRemoteVersion()
            
            if (remoteVersion == null) {
                Log.d(TAG, "Failed to fetch remote version")
                return@withContext false
            }
            
            Log.d(TAG, "Current version: $currentVersion, Remote version: $remoteVersion")
            
            if (remoteVersion > currentVersion) {
                Log.d(TAG, "New version available, downloading...")
                
                val patterns = fetchPatterns()
                if (patterns != null) {
                    // Сохраняем в файл
                    val file = File(context.filesDir, LOCAL_PATTERNS_FILE)
                    file.writeText(patterns)
                    
                    // Обновляем версию
                    Settings.setPatternsVersion(context, remoteVersion)
                    
                    Log.d(TAG, "Patterns updated to version $remoteVersion")
                    return@withContext true
                }
            } else {
                Log.d(TAG, "Patterns are up to date")
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking updates", e)
            false
        }
    }
    
    /**
     * Получает версию с сервера
     */
    private fun fetchRemoteVersion(): Int? {
        return try {
            val request = Request.Builder()
                .url(VERSION_URL)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.trim()?.toIntOrNull()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching version", e)
            null
        }
    }
    
    /**
     * Скачивает паттерны с сервера
     */
    private fun fetchPatterns(): String? {
        return try {
            val request = Request.Builder()
                .url(PATTERNS_URL)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    // Валидируем JSON
                    if (body != null) {
                        JSONObject(body) // Проверяем что это валидный JSON
                        body
                    } else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching patterns", e)
            null
        }
    }
    
    /**
     * Загружает паттерны из локального файла или assets
     */
    fun loadPatterns(context: Context): String? {
        // Сначала пробуем загрузить обновлённые паттерны
        val updatedFile = File(context.filesDir, LOCAL_PATTERNS_FILE)
        if (updatedFile.exists()) {
            try {
                val content = updatedFile.readText()
                JSONObject(content) // Валидация
                Log.d(TAG, "Loaded updated patterns from file")
                return content
            } catch (e: Exception) {
                Log.e(TAG, "Error loading updated patterns, falling back to assets", e)
                updatedFile.delete()
            }
        }
        
        // Fallback на встроенные паттерны
        return null // FraudDetector загрузит из assets
    }
    
    /**
     * Получает информацию о текущей версии
     */
    fun getVersionInfo(context: Context): String {
        val version = Settings.getPatternsVersion(context)
        val hasUpdated = File(context.filesDir, LOCAL_PATTERNS_FILE).exists()
        return if (hasUpdated) {
            "v$version (обновлено)"
        } else {
            "v1 (встроенная)"
        }
    }
}
