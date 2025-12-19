package com.antimoshennik.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlertActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESULT_TEXT = "result_text"
        const val EXTRA_RISK_LEVEL = "risk_level"
        const val EXTRA_SCORE = "score"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Показываем поверх lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_alert)
        
        setupUI()
        startAnimations()
        startAlertSound()
        startVibration()
        
        // Проверяем авто-звонок
        if (intent.getBooleanExtra("AUTO_CALLED", false)) {
            findViewById<TextView>(R.id.tvAutoCallInfo).visibility = View.VISIBLE
        }
    }

    private fun setupUI() {
        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val resultText = intent.getStringExtra(EXTRA_RESULT_TEXT) ?: ""
        val riskLevel = intent.getStringExtra(EXTRA_RISK_LEVEL) ?: "HIGH"
        
        findViewById<TextView>(R.id.tvScore).text = score.toString()
        findViewById<TextView>(R.id.tvDetails).text = resultText
        
        // Настраиваем цвет фона в зависимости от уровня
        val bgColor = when (riskLevel) {
            "CRITICAL" -> 0xFFB91C1C.toInt()
            "HIGH" -> 0xFFDC2626.toInt()
            else -> 0xFFEA580C.toInt()
        }
        findViewById<View>(R.id.alertBackground).setBackgroundColor(bgColor)
        
        // Кнопки
        findViewById<View>(R.id.btnCallFamily).setOnClickListener { callFamily() }
        findViewById<View>(R.id.btnDismiss).setOnClickListener { dismiss() }
    }

    private fun startAnimations() {
        // Пульсация иконки предупреждения
        val iconWarning = findViewById<View>(R.id.iconWarning)
        ObjectAnimator.ofFloat(iconWarning, "scaleX", 1f, 1.15f, 1f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(iconWarning, "scaleY", 1f, 1.15f, 1f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        
        // Пульсирующие кольца
        val ring1 = findViewById<View>(R.id.pulseRing1)
        val ring2 = findViewById<View>(R.id.pulseRing2)
        
        startPulseRing(ring1, 0)
        startPulseRing(ring2, 400)
        
        // Мигание фона
        val bg = findViewById<View>(R.id.alertBackground)
        ObjectAnimator.ofArgb(bg, "backgroundColor", 0xFFDC2626.toInt(), 0xFFB91C1C.toInt(), 0xFFDC2626.toInt()).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun startPulseRing(ring: View, delay: Long) {
        ring.postDelayed({
            ObjectAnimator.ofFloat(ring, "scaleX", 0.5f, 1.5f).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                start()
            }
            ObjectAnimator.ofFloat(ring, "scaleY", 0.5f, 1.5f).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                start()
            }
            ObjectAnimator.ofFloat(ring, "alpha", 0.8f, 0f).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }, delay)
    }

    private fun startAlertSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlertActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Игнорируем ошибки звука
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        // Паттерн: пауза, вибрация, пауза, вибрация...
        val pattern = longArrayOf(0, 500, 200, 500, 200, 800, 500)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun callFamily() {
        val phone = Settings.getFamilyPhone(this)
        if (phone.isBlank()) {
            Toast.makeText(this, "Номер не указан!", Toast.LENGTH_SHORT).show()
            return
        }
        stopAlerts()
        CallHelper.callFamilySOS(this)
    }

    private fun dismiss() {
        stopAlerts()
        finish()
    }

    private fun stopAlerts() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlerts()
    }

    override fun onBackPressed() {
        dismiss()
    }
}
