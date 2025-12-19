package com.antimoshennik.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class WizardActivity : AppCompatActivity() {

    private var currentPage = 0
    private lateinit var pages: List<WizardPage>
    
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmoji: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvWarning: TextView
    private lateinit var btnAction: TextView
    private lateinit var btnNext: TextView
    private lateinit var dotsContainer: LinearLayout
    private lateinit var inputPhone: EditText
    private lateinit var inputContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wizard)
        
        initViews()
        initPages()
        showPage(0)
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        tvEmoji = findViewById(R.id.tvEmoji)
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvStatus = findViewById(R.id.tvStatus)
        tvWarning = findViewById(R.id.tvWarning)
        btnAction = findViewById(R.id.btnAction)
        btnNext = findViewById(R.id.btnNext)
        dotsContainer = findViewById(R.id.dotsContainer)
        inputPhone = findViewById(R.id.inputPhone)
        inputContainer = findViewById(R.id.inputContainer)
        
        btnNext.setOnClickListener { nextPage() }
    }

    private fun initPages() {
        val pagesList = mutableListOf<WizardPage>()
        
        // 1. Приветствие
        pagesList.add(WizardPage(
            emoji = "🛡️",
            title = "Добро пожаловать!",
            description = "Антимошенник защитит вас от телефонных мошенников.\n\nПриложение анализирует записи звонков и предупреждает об опасности.",
            actionText = null,
            action = null,
            checkStatus = null,
            isRequired = false
        ))
        
        // 2. Как работает
        pagesList.add(WizardPage(
            emoji = "🎙️",
            title = "Как это работает?",
            description = "1️⃣ Включите запись звонков\n\n2️⃣ Включите защиту\n\n3️⃣ После звонка — анализ\n\n4️⃣ При опасности — алерт!",
            actionText = null,
            action = null,
            checkStatus = null,
            isRequired = false
        ))
        
        // 3. Разрешения (ОБЯЗАТЕЛЬНО)
        pagesList.add(WizardPage(
            emoji = "📱",
            title = "Разрешения",
            description = "⚠️ БЕЗ РАЗРЕШЕНИЙ ПРИЛОЖЕНИЕ НЕ БУДЕТ РАБОТАТЬ!\n\n📁 Файлы — читать записи звонков\n📞 Звонки — SOS родственнику\n🔔 Уведомления — показывать алерты",
            actionText = "РАЗРЕШИТЬ ВСЁ",
            action = { requestPermissions() },
            checkStatus = { checkPermissions() },
            isRequired = true,
            warningText = "⛔ Нажмите «Разрешить всё» для продолжения"
        ))
        
        // 4. Батарея
        pagesList.add(WizardPage(
            emoji = "🔋",
            title = "Батарея",
            description = "Разрешите работу без ограничений, чтобы защита работала в фоне даже при заблокированном экране.",
            actionText = "Настроить",
            action = { openBatterySettings() },
            checkStatus = { checkBattery() },
            isRequired = false
        ))
        
        // 5. Автозапуск (только Xiaomi)
        if (DeviceHelper.getManufacturer() == DeviceHelper.Manufacturer.XIAOMI) {
            pagesList.add(WizardPage(
                emoji = "🚀",
                title = "Автозапуск",
                description = "Включите автозапуск для Антимошенник.\n\nНайдите приложение в списке и включите переключатель.",
                actionText = "Открыть",
                action = { openAutoStart() },
                checkStatus = { null },
                isRequired = false
            ))
        }
        
        // 6. Номер родственника
        pagesList.add(WizardPage(
            emoji = "📞",
            title = "SOS номер",
            description = "При обнаружении мошенников позвоним родственнику 3 раза подряд!\n\nМожно указать позже в настройках.",
            actionText = null,
            action = null,
            checkStatus = null,
            hasInput = true,
            isRequired = false
        ))
        
        // 7. Готово
        pagesList.add(WizardPage(
            emoji = "✅",
            title = "Всё готово!",
            description = "Защита настроена.\n\nТеперь включите защиту на главном экране.",
            actionText = null,
            action = null,
            checkStatus = null,
            isFinal = true,
            isRequired = false
        ))
        
        pages = pagesList
        progressBar.max = pages.size
        setupDots()
    }

    private fun setupDots() {
        dotsContainer.removeAllViews()
        for (i in pages.indices) {
            val dot = TextView(this).apply {
                text = "●"
                textSize = 12f
                setPadding(8, 0, 8, 0)
                setTextColor(if (i == 0) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt())
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots() {
        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as TextView
            dot.setTextColor(if (i == currentPage) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt())
            dot.textSize = if (i == currentPage) 14f else 12f
        }
    }

    private fun showPage(index: Int) {
        currentPage = index
        val page = pages[index]
        
        // Анимация
        val content = findViewById<View>(R.id.contentContainer)
        content.alpha = 0f
        content.translationX = 50f
        content.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(0.5f))
            .start()
        
        // Обновляем UI
        progressBar.progress = index + 1
        tvEmoji.text = page.emoji
        tvTitle.text = page.title
        tvDescription.text = page.description
        
        // Кнопка действия
        if (page.actionText != null) {
            btnAction.visibility = View.VISIBLE
            btnAction.text = page.actionText
            btnAction.setOnClickListener { page.action?.invoke() }
        } else {
            btnAction.visibility = View.GONE
        }
        
        // Input для телефона
        inputContainer.visibility = if (page.hasInput) View.VISIBLE else View.GONE
        
        // Статус и предупреждение
        updateStatus()
        
        // Кнопка далее
        btnNext.text = when {
            page.isFinal -> "НАЧАТЬ"
            else -> "ДАЛЕЕ →"
        }
        
        updateDots()
    }

    private fun updateStatus() {
        val page = pages[currentPage]
        val status = page.checkStatus?.invoke()
        
        when (status) {
            true -> {
                tvStatus.text = "✅ Разрешения получены"
                tvStatus.setTextColor(0xFF4ADE80.toInt())
                tvStatus.visibility = View.VISIBLE
                tvWarning.visibility = View.GONE
                // Разблокируем кнопку
                btnNext.alpha = 1f
                btnNext.isEnabled = true
            }
            false -> {
                tvStatus.text = "❌ Требуются разрешения"
                tvStatus.setTextColor(0xFFF87171.toInt())
                tvStatus.visibility = View.VISIBLE
                // Показываем предупреждение для обязательных страниц
                if (page.isRequired) {
                    tvWarning.text = page.warningText ?: "Требуется для работы"
                    tvWarning.visibility = View.VISIBLE
                    // Блокируем кнопку
                    btnNext.alpha = 0.5f
                    btnNext.isEnabled = false
                } else {
                    tvWarning.visibility = View.GONE
                    btnNext.alpha = 1f
                    btnNext.isEnabled = true
                }
            }
            null -> {
                if (page.actionText != null) {
                    tvStatus.text = "⚠️ Проверьте вручную"
                    tvStatus.setTextColor(0xFFFBBF24.toInt())
                    tvStatus.visibility = View.VISIBLE
                } else {
                    tvStatus.visibility = View.GONE
                }
                tvWarning.visibility = View.GONE
                btnNext.alpha = 1f
                btnNext.isEnabled = true
            }
        }
    }

    private fun nextPage() {
        val page = pages[currentPage]
        
        // Проверяем обязательные разрешения
        if (page.isRequired && page.checkStatus?.invoke() == false) {
            Toast.makeText(this, "⚠️ Сначала дайте разрешения!", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Сохраняем телефон если есть
        if (page.hasInput) {
            val phone = inputPhone.text.toString().trim()
            if (phone.isNotEmpty()) {
                Settings.setFamilyPhone(this, phone)
            }
        }
        
        if (currentPage < pages.size - 1) {
            showPage(currentPage + 1)
        } else {
            finishWizard()
        }
    }

    private fun finishWizard() {
        Settings.setOnboardingDone(this)
        Settings.setXiaomiSetupDone(this)
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // === Permissions & Settings ===
    
    private fun checkPermissions(): Boolean {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val hasPhone = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        return hasStorage && hasNotifications && hasPhone
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        perms.add(Manifest.permission.CALL_PHONE)
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
    }

    private fun checkBattery(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openBatterySettings() {
        try {
            startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun openAutoStart() {
        if (!DeviceHelper.openAutoStartSettings(this)) {
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
            Toast.makeText(this, "Откройте: Разрешения → Автозапуск", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
    }

    data class WizardPage(
        val emoji: String,
        val title: String,
        val description: String,
        val actionText: String?,
        val action: (() -> Unit)?,
        val checkStatus: (() -> Boolean?)?,
        val hasInput: Boolean = false,
        val isFinal: Boolean = false,
        val isRequired: Boolean = false,
        val warningText: String? = null
    )
}
