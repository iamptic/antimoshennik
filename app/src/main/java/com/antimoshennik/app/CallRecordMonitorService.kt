package com.antimoshennik.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class CallRecordMonitorService : Service() {
    
    companion object {
        private const val TAG = "CallMonitor"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "antimoshennik_channel"
        var isRunning = false
            private set
        var watchingPaths = mutableListOf<String>()
        var lastEvent = "Нет событий"
        var lastTranscript = ""
        var lastScore = -1
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var fileObservers = mutableListOf<FileObserver>()
    private lateinit var fraudDetector: FraudDetector
    private lateinit var speechRecognizer: SpeechRecognizer
    private var notificationManager: NotificationManager? = null
    
    private val processedFiles = ConcurrentHashMap<String, Long>()
    private val alreadyAlerted = ConcurrentHashMap<String, Boolean>()
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val recordingPaths: List<String>
        get() {
            val storage = Environment.getExternalStorageDirectory().absolutePath
            return listOf(
                "$storage/Recordings/sound_recorder/call_rec",
                "$storage/Recordings/sound_recorder",
                "$storage/MIUI/sound_recorder/call_rec",
                "$storage/Recordings"
            )
        }
    
    override fun onCreate() {
        super.onCreate()
        fraudDetector = FraudDetector(this)
        speechRecognizer = SpeechRecognizer(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Antimoshennik::MonitorWakeLock"
            )
            wakeLock?.acquire()
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock error", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        AppWidget.updateAllWidgets(this)
        watchingPaths.clear()
        startForeground(NOTIFICATION_ID, createNotification("Запуск..."))
        startMonitoring()
        updateNotification("🛡️ Защита активна")
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        AppWidget.updateAllWidgets(this)
        fileObservers.forEach { it.stopWatching() }
        fileObservers.clear()
        serviceScope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Антимошенник", NotificationManager.IMPORTANCE_LOW)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Антимошенник")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(PendingIntent.getActivity(this, 0, 
                Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(text: String) {
        lastEvent = text
        notificationManager?.notify(NOTIFICATION_ID, createNotification(text))
    }
    
    private fun startMonitoring() {
        for (path in recordingPaths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                watchingPaths.add(path)
                
                val observer = object : FileObserver(dir, CLOSE_WRITE or CREATE or MODIFY) {
                    override fun onEvent(event: Int, fileName: String?) {
                        if (fileName == null) return
                        
                        val lower = fileName.lowercase()
                        val isAudio = lower.endsWith(".mp3") || lower.endsWith(".m4a") || 
                              lower.endsWith(".amr") || lower.endsWith(".3gp")
                        
                        if (!isAudio || event != CLOSE_WRITE) return
                        
                        val fullPath = "$path/$fileName"
                        
                        if (Settings.isInWhitelist(this@CallRecordMonitorService, fileName)) {
                            updateNotification("✅ Свой номер")
                            return
                        }
                        
                        updateNotification("📝 Анализирую...")
                        analyzeFile(fullPath, fileName)
                    }
                }
                observer.startWatching()
                fileObservers.add(observer)
            }
        }
    }
    
    private fun analyzeFile(fullPath: String, fileName: String) {
        val lastTime = processedFiles[fullPath]
        if (lastTime != null && System.currentTimeMillis() - lastTime < 30000) return
        processedFiles[fullPath] = System.currentTimeMillis()
        
        serviceScope.launch {
            try {
                updateNotification("⏳ Жду записи...")
                
                var lastSize = 0L
                var stableCount = 0
                for (i in 1..15) {
                    delay(1000)
                    val file = File(fullPath)
                    val currentSize = if (file.exists()) file.length() else 0
                    if (currentSize == lastSize && currentSize > 1000) {
                        stableCount++
                        if (stableCount >= 2) break
                    } else stableCount = 0
                    lastSize = currentSize
                }
                
                delay(2000)
                
                val file = File(fullPath)
                if (!file.exists() || file.length() < 1000) {
                    updateNotification("❌ Файл пуст")
                    return@launch
                }
                
                updateNotification("🎙️ Распознаю...")
                
                val transcript = speechRecognizer.recognizeFromFile(fullPath)
                lastTranscript = transcript
                
                val result = if (transcript.isNotBlank()) {
                    fraudDetector.analyze(transcript)
                } else {
                    AnalysisResult(RiskLevel.SAFE, 0, listOf("Пустой текст"), 
                        emptyList(), emptyList(), emptyList(), emptyList(), "empty")
                }
                
                lastScore = result.score
                
                AnalysisHistory.addEntry(this@CallRecordMonitorService, fileName,
                    result.riskLevel, result.score, result.findings, transcript)
                
                updateNotification("${result.riskLevel.emoji} ${result.score} баллов")
                
                sendBroadcast(Intent("com.antimoshennik.ANALYSIS_COMPLETE"))
                
                val threshold = Settings.getAlertThreshold(this@CallRecordMonitorService)
                
                if (result.score >= threshold && alreadyAlerted[fullPath] != true) {
                    alreadyAlerted[fullPath] = true
                    showAlert(result, CallHelper.callFamilySOS(this@CallRecordMonitorService))
                }
                
                delay(60000)
                alreadyAlerted.remove(fullPath)
                
            } catch (e: Exception) {
                Log.e(TAG, "Analysis error", e)
                updateNotification("❌ Ошибка")
            }
        }
    }
    
    private fun showAlert(result: AnalysisResult, autoCalled: Boolean) {
        // Показываем heads-up уведомление
        AlertNotification.show(this, result.score, result.riskLevel.name, fraudDetector.formatResult(result))
        
        // Отправляем уведомление в Telegram
        serviceScope.launch {
            TelegramNotifier.sendAlert(this@CallRecordMonitorService, result.score, lastTranscript)
        }
        
        // Также запускаем Activity
        try {
            startActivity(Intent(this, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlertActivity.EXTRA_RESULT_TEXT, fraudDetector.formatResult(result))
                putExtra(AlertActivity.EXTRA_RISK_LEVEL, result.riskLevel.name)
                putExtra(AlertActivity.EXTRA_SCORE, result.score)
                putExtra("AUTO_CALLED", autoCalled)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Alert activity error", e)
        }
    }
}
