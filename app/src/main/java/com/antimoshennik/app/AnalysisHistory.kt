package com.antimoshennik.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Хранение истории анализов звонков
 */
object AnalysisHistory {
    
    private const val PREFS_NAME = "antimoshennik_history"
    private const val KEY_HISTORY = "analysis_history"
    private const val KEY_TOTAL_CALLS = "total_calls"
    private const val KEY_FRAUD_DETECTED = "fraud_detected"
    private const val MAX_HISTORY_SIZE = 50
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Добавить запись в историю
     */
    fun addEntry(
        context: Context,
        fileName: String,
        riskLevel: RiskLevel,
        score: Int,
        findings: List<String>,
        transcript: String
    ) {
        val prefs = getPrefs(context)
        
        // Увеличиваем счётчики
        val totalCalls = prefs.getInt(KEY_TOTAL_CALLS, 0) + 1
        var fraudDetected = prefs.getInt(KEY_FRAUD_DETECTED, 0)
        if (riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL)) {
            fraudDetected++
        }
        
        // Создаём запись
        val entry = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("fileName", fileName)
            put("riskLevel", riskLevel.name)
            put("score", score)
            put("findings", JSONArray(findings))
            put("transcriptPreview", transcript.take(200))
        }
        
        // Загружаем историю
        val historyJson = prefs.getString(KEY_HISTORY, "[]")
        val history = JSONArray(historyJson)
        
        // Добавляем в начало
        val newHistory = JSONArray()
        newHistory.put(entry)
        for (i in 0 until minOf(history.length(), MAX_HISTORY_SIZE - 1)) {
            newHistory.put(history.getJSONObject(i))
        }
        
        // Сохраняем
        prefs.edit()
            .putInt(KEY_TOTAL_CALLS, totalCalls)
            .putInt(KEY_FRAUD_DETECTED, fraudDetected)
            .putString(KEY_HISTORY, newHistory.toString())
            .apply()
    }
    
    /**
     * Получить статистику
     */
    fun getStats(context: Context): Stats {
        val prefs = getPrefs(context)
        return Stats(
            totalCalls = prefs.getInt(KEY_TOTAL_CALLS, 0),
            fraudDetected = prefs.getInt(KEY_FRAUD_DETECTED, 0)
        )
    }
    
    /**
     * Получить историю
     */
    fun getHistory(context: Context): List<HistoryEntry> {
        val prefs = getPrefs(context)
        val historyJson = prefs.getString(KEY_HISTORY, "[]")
        val history = JSONArray(historyJson)
        
        val entries = mutableListOf<HistoryEntry>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        
        for (i in 0 until history.length()) {
            val obj = history.getJSONObject(i)
            entries.add(HistoryEntry(
                timestamp = obj.getLong("timestamp"),
                dateTime = dateFormat.format(Date(obj.getLong("timestamp"))),
                fileName = obj.getString("fileName"),
                riskLevel = RiskLevel.valueOf(obj.getString("riskLevel")),
                score = obj.getInt("score"),
                transcriptPreview = obj.optString("transcriptPreview", "")
            ))
        }
        
        return entries
    }
    
    /**
     * Очистить историю
     */
    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
    
    data class Stats(
        val totalCalls: Int,
        val fraudDetected: Int
    )
    
    data class HistoryEntry(
        val timestamp: Long,
        val dateTime: String,
        val fileName: String,
        val riskLevel: RiskLevel,
        val score: Int,
        val transcriptPreview: String
    )
}
