package com.antimoshennik.app

import android.content.Context
import android.util.Log
import org.json.JSONObject

enum class RiskLevel(val displayName: String, val emoji: String) {
    SAFE("БЕЗОПАСНО", "✅"),
    LOW("НИЗКИЙ РИСК", "ℹ️"),
    MEDIUM("СРЕДНИЙ РИСК", "⚡"),
    HIGH("ВЫСОКИЙ РИСК", "⚠️"),
    CRITICAL("КРИТИЧЕСКАЯ ОПАСНОСТЬ", "🚨")
}

data class AnalysisResult(
    val riskLevel: RiskLevel,
    val score: Int,
    val findings: List<String>,
    val highRiskMatches: List<String>,
    val mediumRiskMatches: List<String>,
    val pressureMatches: List<String>,
    val safeIndicators: List<String>,
    val summary: String,
    val debugInfo: String = ""
)

class FraudDetector(private val context: Context) {
    companion object {
        private const val TAG = "FraudDetector"
    }
    
    // Веса
    private var weightRedFlag = 50
    private var weightHighRisk = 40
    private var weightMediumRisk = 25
    private var weightPressure = 30
    private var weightDataRequest = 35
    private var weightMoneyTransfer = 45
    private var weightVictimResponse = 60
    private var weightSafe = -30
    private var weightKeyword = 15
    private var weightCombo = 20
    
    // Пороги
    private var thresholdCritical = 150
    private var thresholdHigh = 80
    private var thresholdMedium = 40
    
    // Паттерны
    private var redFlags = listOf<String>()
    private var highRiskPatterns = listOf<String>()
    private var mediumRiskPatterns = listOf<String>()
    private var pressureTactics = listOf<String>()
    private var dataRequests = listOf<String>()
    private var moneyTransfer = listOf<String>()
    private var victimResponses = listOf<String>()
    private var safeIndicators = listOf<String>()
    private var dangerousKeywords = listOf<String>()
    
    init { loadPatterns() }
    
    
    private fun loadPatterns() {
        // Сначала пробуем загрузить обновлённые паттерны
        val updatedJson = PatternUpdater.loadPatterns(context)
        val json = if (updatedJson != null) {
            Log.d(TAG, "Using updated patterns")
            updatedJson
        } else {
            Log.d(TAG, "Using built-in patterns")
            context.assets.open("fraud_patterns.json").bufferedReader().use { it.readText() }
        }
        parsePatterns(json)
    }
    
    private fun parsePatterns(json: String) {
        try {
            val obj = JSONObject(json)
            
            redFlags = obj.optJSONArray("red_flags")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            highRiskPatterns = obj.optJSONArray("high_risk_patterns")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            mediumRiskPatterns = obj.optJSONArray("medium_risk_patterns")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            pressureTactics = obj.optJSONArray("pressure_tactics")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            dataRequests = obj.optJSONArray("data_requests")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            moneyTransfer = obj.optJSONArray("money_transfer")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            victimResponses = obj.optJSONArray("victim_responses")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            safeIndicators = obj.optJSONArray("safe_indicators")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            dangerousKeywords = obj.optJSONArray("dangerous_keywords")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            } ?: emptyList()
            
            // Загружаем веса
            obj.optJSONObject("weights")?.let { w ->
                weightRedFlag = w.optInt("red_flag", 50)
                weightHighRisk = w.optInt("high_risk", 40)
                weightMediumRisk = w.optInt("medium_risk", 25)
                weightPressure = w.optInt("pressure", 30)
                weightDataRequest = w.optInt("data_request", 35)
                weightMoneyTransfer = w.optInt("money_transfer", 45)
                weightVictimResponse = w.optInt("victim_response", 60)
                weightSafe = w.optInt("safe_indicator", -30)
                weightKeyword = w.optInt("keyword", 15)
                weightCombo = w.optInt("combo_bonus", 20)
            }
            
            // Загружаем пороги
            obj.optJSONObject("thresholds")?.let { t ->
                thresholdCritical = t.optInt("critical", 150)
                thresholdHigh = t.optInt("high", 80)
                thresholdMedium = t.optInt("medium", 40)
            }
            
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load patterns", e)
            loadDefaultPatterns()
        }
    }
    
    private fun loadDefaultPatterns() {
        redFlags = listOf(
            "безопасный счет", "код из смс", "служба безопасности",
            "центральный банк", "уголовное дело"
        )
        highRiskPatterns = listOf(
            "служба безопасности банка", "следственный комитет", "прокуратура"
        )
        dangerousKeywords = listOf(
            "безопасн", "служб", "банк", "блокир", "код", "смс", "перевод"
        )
    }
    
    fun analyze(text: String): AnalysisResult {
        val normalizedText = text.lowercase().trim()
        var score = 0
        val findings = mutableListOf<String>()
        val highMatches = mutableListOf<String>()
        val mediumMatches = mutableListOf<String>()
        val pressureMatches = mutableListOf<String>()
        val safeMatches = mutableListOf<String>()
        var categoriesFound = 0
        
        
        // Проверяем красные флаги
        for (flag in redFlags) {
            if (normalizedText.contains(flag)) {
                score += weightRedFlag
                findings.add("🚨 КРАСНЫЙ ФЛАГ: \"$flag\"")
                highMatches.add(flag)
                categoriesFound++
            }
        }
        
        // Проверяем high risk паттерны
        for (pattern in highRiskPatterns) {
            if (normalizedText.contains(pattern) && pattern !in highMatches) {
                score += weightHighRisk
                findings.add("⚠️ Высокий риск: \"$pattern\"")
                highMatches.add(pattern)
                categoriesFound++
            }
        }
        
        // Проверяем medium risk
        for (pattern in mediumRiskPatterns) {
            if (normalizedText.contains(pattern)) {
                score += weightMediumRisk
                findings.add("⚡ Средний риск: \"$pattern\"")
                mediumMatches.add(pattern)
            }
        }
        
        // Проверяем давление
        for (pattern in pressureTactics) {
            if (normalizedText.contains(pattern)) {
                score += weightPressure
                findings.add("⏰ Давление: \"$pattern\"")
                pressureMatches.add(pattern)
                categoriesFound++
            }
        }
        
        // Проверяем запросы данных
        for (pattern in dataRequests) {
            if (normalizedText.contains(pattern)) {
                score += weightDataRequest
                findings.add("🔐 Запрос данных: \"$pattern\"")
                categoriesFound++
            }
        }
        
        // Проверяем переводы денег
        for (pattern in moneyTransfer) {
            if (normalizedText.contains(pattern)) {
                score += weightMoneyTransfer
                findings.add("💰 Перевод денег: \"$pattern\"")
                categoriesFound++
            }
        }
        
        // Проверяем ответы жертвы
        for (pattern in victimResponses) {
            if (normalizedText.contains(pattern)) {
                score += weightVictimResponse
                findings.add("😰 Ответ жертвы: \"$pattern\"")
            }
        }
        
        // Проверяем безопасные индикаторы
        for (pattern in safeIndicators) {
            if (normalizedText.contains(pattern)) {
                score += weightSafe
                findings.add("✅ Безопасно: \"$pattern\"")
                safeMatches.add(pattern)
            }
        }
        
        // Проверяем ключевые слова
        for (keyword in dangerousKeywords) {
            if (normalizedText.contains(keyword) && 
                highMatches.none { it.contains(keyword) }) {
                score += weightKeyword
                findings.add("🔍 Ключевое слово: \"$keyword\"")
            }
        }
        
        
        
        // Комбо бонус
        if (categoriesFound >= 3) {
            score += weightCombo * (categoriesFound - 2)
            findings.add("⚡ Комбо: $categoriesFound категорий (+${weightCombo * (categoriesFound - 2)})")
        }
        
        // Определяем уровень риска
        val riskLevel = when {
            score >= thresholdCritical -> RiskLevel.CRITICAL
            score >= thresholdHigh -> RiskLevel.HIGH
            score >= thresholdMedium -> RiskLevel.MEDIUM
            score > 0 -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }
        
        
        val summary = when (riskLevel) {
            RiskLevel.CRITICAL -> "🚨 МОШЕННИК! Положите трубку немедленно!"
            RiskLevel.HIGH -> "⚠️ Высокая вероятность мошенничества!"
            RiskLevel.MEDIUM -> "⚡ Будьте осторожны, есть подозрительные признаки"
            RiskLevel.LOW -> "ℹ️ Небольшие подозрения, будьте внимательны"
            RiskLevel.SAFE -> "✅ Признаков мошенничества не обнаружено"
        }
        
        return AnalysisResult(
            riskLevel = riskLevel,
            score = score,
            findings = findings,
            highRiskMatches = highMatches,
            mediumRiskMatches = mediumMatches,
            pressureMatches = pressureMatches,
            safeIndicators = safeMatches,
            summary = summary
        )
    }
    
    fun formatResult(result: AnalysisResult): String {
        val sb = StringBuilder()
        sb.appendLine("${result.riskLevel.emoji} ${result.riskLevel.displayName}")
        sb.appendLine("Баллы: ${result.score}")
        sb.appendLine()
        sb.appendLine(result.summary)
        if (result.findings.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Обнаружено:")
            result.findings.take(10).forEach { sb.appendLine("• $it") }
            if (result.findings.size > 10) {
                sb.appendLine("...и ещё ${result.findings.size - 10}")
            }
        }
        return sb.toString()
    }
}
