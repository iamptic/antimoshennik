package com.antimoshennik.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val content = findViewById<View>(R.id.splashContent)
        val shield = findViewById<View>(R.id.shieldIcon)

        // Анимация появления
        content.alpha = 0f
        content.scaleX = 0.8f
        content.scaleY = 0.8f

        content.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Пульсация щита
        ObjectAnimator.ofFloat(shield, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 1000
            repeatCount = 1
            start()
        }
        ObjectAnimator.ofFloat(shield, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 1000
            repeatCount = 1
            start()
        }

        // Переход через 1.5 сек
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNext()
        }, 1500)
    }

    private fun navigateToNext() {
        val intent = if (Settings.isOnboardingDone(this)) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, WizardActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
