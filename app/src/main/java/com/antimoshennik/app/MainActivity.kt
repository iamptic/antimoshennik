package com.antimoshennik.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var fraudDetector: FraudDetector
    private lateinit var speechRecognizer: SpeechRecognizer
    private val handler = Handler(Looper.getMainLooper())
    
    private lateinit var fabToggle: TextView
    private lateinit var shieldCircle: View
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusHint: TextView
    private lateinit var tvCallsCount: TextView
    private lateinit var tvThreatsCount: TextView
    private lateinit var tvLastResult: TextView

    private val analysisReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateStats()
            updateHistory()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        fraudDetector = FraudDetector(this)
        speechRecognizer = SpeechRecognizer(this)
        
        initViews()
        setupListeners()
        updateUI()
    }

    private fun initViews() {
        fabToggle = findViewById(R.id.fabToggle)
        shieldCircle = findViewById(R.id.shieldCircle)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusHint = findViewById(R.id.tvStatusHint)
        tvCallsCount = findViewById(R.id.tvCallsCount)
        tvThreatsCount = findViewById(R.id.tvThreatsCount)
        tvLastResult = findViewById(R.id.tvLastResult)
    }

    private fun setupListeners() {
        fabToggle.setOnClickListener { toggleProtection() }
        
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        findViewById<View>(R.id.btnTest).setOnClickListener { testDetector() }
        findViewById<View>(R.id.btnFiles).setOnClickListener { showFilesAndTest() }
        findViewById<View>(R.id.btnDiag).setOnClickListener { showDiagnostics() }
        findViewById<View>(R.id.btnClearHistory).setOnClickListener {
            AnalysisHistory.clear(this)
            updateStats()
            updateHistory()
            Toast.makeText(this, "Очищено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleProtection() {
        if (CallRecordMonitorService.isRunning) {
            stopMonitoringService()
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, CallRecordMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        handler.postDelayed({ updateUI() }, 1000)
    }

    private fun stopMonitoringService() {
        stopService(Intent(this, CallRecordMonitorService::class.java))
        handler.postDelayed({ updateUI() }, 500)
    }

    private fun updateUI() {
        val isActive = CallRecordMonitorService.isRunning
        
        // FAB
        if (isActive) {
            fabToggle.text = "⏹️  ВЫКЛЮЧИТЬ ЗАЩИТУ"
            fabToggle.setBackgroundResource(R.drawable.btn_fab_danger)
        } else {
            fabToggle.text = "▶️  ВКЛЮЧИТЬ ЗАЩИТУ"
            fabToggle.setBackgroundResource(R.drawable.btn_fab)
        }
        
        // Status
        if (isActive) {
            tvStatus.text = "ЗАЩИТА АКТИВНА"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            shieldCircle.setBackgroundResource(R.drawable.shield_modern)
            shieldCircle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        } else {
            tvStatus.text = "ЗАЩИТА ОТКЛЮЧЕНА"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            shieldCircle.setBackgroundResource(R.drawable.shield_inactive)
            shieldCircle.clearAnimation()
        }
        
        updateStatusHint()
        updateStats()
        updateHistory()
    }

    private fun updateStatusHint() {
        val phone = Settings.getFamilyPhone(this)
        val threshold = Settings.getAlertThreshold(this)
        val whitelist = Settings.getWhitelist(this).size
        
        val parts = mutableListOf<String>()
        if (phone.isNotBlank()) parts.add("📞 ${phone.take(8)}...")
        parts.add("🎚️ $threshold")
        if (whitelist > 0) parts.add("✅ $whitelist")
        
        tvStatusHint.text = parts.joinToString(" | ")
    }

    private fun updateStats() {
        val stats = AnalysisHistory.getStats(this)
        tvCallsCount.text = stats.totalCalls.toString()
        tvThreatsCount.text = stats.fraudDetected.toString()
    }

    private fun updateHistory() {
        val history = AnalysisHistory.getHistory(this)
        if (history.isEmpty()) {
            tvLastResult.text = "История пуста"
            return
        }
        val sb = StringBuilder()
        history.take(3).forEachIndexed { i, e ->
            sb.appendLine("${e.riskLevel.emoji} ${e.dateTime} — ${e.score} баллов")
            if (i < 2) sb.appendLine()
        }
        tvLastResult.text = sb.toString()
    }

    private fun testDetector() {
        val text = "служба безопасности банка ваша карта заблокирована"
        val result = fraudDetector.analyze(text)
        startActivity(Intent(this, AlertActivity::class.java).apply {
            putExtra(AlertActivity.EXTRA_RESULT_TEXT, fraudDetector.formatResult(result))
            putExtra(AlertActivity.EXTRA_RISK_LEVEL, result.riskLevel.name)
            putExtra(AlertActivity.EXTRA_SCORE, result.score)
        })
        overridePendingTransition(R.anim.scale_in, android.R.anim.fade_out)
    }

    private fun showFilesAndTest() {
        val storage = Environment.getExternalStorageDirectory().absolutePath
        val paths = listOf(
            "$storage/Recordings/sound_recorder/call_rec",
            "$storage/Recordings/sound_recorder",
            "$storage/MIUI/sound_recorder/call_rec",
            "$storage/Recordings"
        )
        
        val allFiles = mutableListOf<File>()
        for (path in paths) {
            File(path).listFiles()?.filter { 
                it.name.lowercase().let { n -> n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".amr") }
            }?.let { allFiles.addAll(it) }
        }
        
        val audioFiles = allFiles.sortedByDescending { it.lastModified() }.take(15)
        
        if (audioFiles.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Нет файлов")
                .setMessage("Записи не найдены")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        val items = audioFiles.map { "${it.name.take(25)}... (${it.length()/1024}KB)" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("📁 Файлы (${audioFiles.size})")
            .setItems(items) { _, index -> testRecognition(audioFiles[index]) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun testRecognition(file: File) {
        val progress = AlertDialog.Builder(this)
            .setTitle("🎙️ Анализ...")
            .setMessage(file.name)
            .setCancelable(false)
            .create()
        progress.show()
        
        lifecycleScope.launch {
            try {
                val transcript = withContext(Dispatchers.IO) { 
                    speechRecognizer.recognizeFromFile(file.absolutePath) 
                }
                progress.dismiss()
                
                if (transcript.isBlank()) {
                    Toast.makeText(this@MainActivity, "Не распознано", Toast.LENGTH_SHORT).show()
                } else {
                    val result = fraudDetector.analyze(transcript)
                    val threshold = Settings.getAlertThreshold(this@MainActivity)
                    val wouldAlert = result.score >= threshold
                    
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("${result.riskLevel.emoji} ${result.score} баллов")
                        .setMessage("📝 \"$transcript\"\n\n🎚️ Порог: $threshold\n🚨 Алерт: ${if (wouldAlert) "ДА" else "НЕТ"}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                progress.dismiss()
                Toast.makeText(this@MainActivity, "Ошибка: $e", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDiagnostics() {
        val phone = Settings.getFamilyPhone(this)
        val threshold = Settings.getAlertThreshold(this)
        val whitelist = Settings.getWhitelist(this)
        
        val diag = """
=== СТАТУС ===
Защита: ${if (CallRecordMonitorService.isRunning) "✅ Активна" else "❌ Выключена"}
Папок: ${CallRecordMonitorService.watchingPaths.size}

=== НАСТРОЙКИ ===
Порог: $threshold баллов
Белый список: ${whitelist.size}
SOS номер: ${phone.ifBlank { "❌ НЕ УКАЗАН" }}

=== РАСПОЗНАВАНИЕ ===
Режим: ${SpeechRecognizer.lastMode}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("🔍 Диагностика")
            .setMessage(diag)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        val filter = IntentFilter("com.antimoshennik.ANALYSIS_COMPLETE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(analysisReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(analysisReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(analysisReceiver) } catch (e: Exception) {}
    }
}
