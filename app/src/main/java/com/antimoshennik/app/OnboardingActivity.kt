package com.antimoshennik.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.antimoshennik.app.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private var currentPage = 0
    
    private val pages = listOf(
        OnboardingPage(
            "🛡️",
            "Добро пожаловать!",
            "Антимошенник защитит вас от телефонных мошенников.\n\nПриложение анализирует записи звонков и предупреждает об опасности."
        ),
        OnboardingPage(
            "🎙️",
            "Как это работает?",
            "1️⃣ Включите запись звонков в телефоне\n\n2️⃣ Включите защиту в приложении\n\n3️⃣ После каждого звонка — автоматический анализ\n\n4️⃣ При опасности — громкий алерт!"
        ),
        OnboardingPage(
            "📞",
            "SOS звонок",
            "При обнаружении мошенников приложение может автоматически позвонить родственнику 3 раза подряд!\n\nУкажите номер на следующем шаге."
        ),
        OnboardingPage(
            "✅",
            "Разрешения",
            "Для работы нужны разрешения:\n\n📁 Доступ к файлам — читать записи\n\n📞 Звонки — SOS родственнику\n\n🔔 Уведомления — показывать статус"
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Если onboarding пройден — сразу на главную
        if (Settings.isOnboardingDone(this)) {
            goToMain()
            return
        }
        
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        showPage(0)
        
        binding.btnNext.setOnClickListener {
            if (currentPage < pages.size - 1) {
                showPage(currentPage + 1)
            } else {
                requestPermissions()
            }
        }
        
        binding.btnSkip.setOnClickListener {
            Settings.setOnboardingDone(this)
            goToMain()
        }
    }
    
    private fun showPage(index: Int) {
        currentPage = index
        val page = pages[index]
        
        binding.tvEmoji.text = page.emoji
        binding.tvTitle.text = page.title
        binding.tvDescription.text = page.description
        
        // Индикаторы
        binding.tvIndicator.text = "${index + 1} / ${pages.size}"
        
        // Кнопка
        binding.btnNext.text = if (index == pages.size - 1) "НАЧАТЬ" else "ДАЛЕЕ"
    }
    
    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.CALL_PHONE)
        
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
        } else {
            finishOnboarding()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finishOnboarding()
    }
    
    private fun finishOnboarding() {
        Settings.setOnboardingDone(this)
        Toast.makeText(this, "✅ Настройка завершена!", Toast.LENGTH_SHORT).show()
        goToMain()
    }
    
    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    data class OnboardingPage(val emoji: String, val title: String, val description: String)
}
