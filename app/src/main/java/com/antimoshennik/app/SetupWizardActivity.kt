package com.antimoshennik.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class SetupWizardActivity : AppCompatActivity() {
    
    private var currentStep = 0
    private lateinit var tvStep: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnAction: Button
    private lateinit var btnNext: Button
    
    data class Step(val title: String, val desc: String, val action: () -> Unit, val check: () -> Boolean?)
    private val steps = mutableListOf<Step>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        
        tvStep = TextView(this).apply { textSize = 14f; layout.addView(this) }
        tvTitle = TextView(this).apply { textSize = 24f; setPadding(0,24,0,16); layout.addView(this) }
        tvDesc = TextView(this).apply { textSize = 16f; setPadding(0,0,0,24); layout.addView(this) }
        tvStatus = TextView(this).apply { textSize = 18f; setPadding(0,0,0,32); layout.addView(this) }
        btnAction = Button(this).apply { text = "Открыть настройки"; setOnClickListener { doAction() }; layout.addView(this) }
        btnNext = Button(this).apply { text = "Далее →"; setOnClickListener { nextStep() }; layout.addView(this) }
        
        setContentView(layout)
        initSteps()
        showStep(0)
    }
    
    private fun initSteps() {
        // 1. Батарея
        steps.add(Step("🔋 Батарея", "Найдите Антимошенник и выберите «Без ограничений»", {
            try {
                startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: Exception) {
                openAppSettings()
            }
        }, { (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName) }))
        
        // 2. Уведомления
        steps.add(Step("🔔 Уведомления", "Включите все уведомления + Плавающие", {
            try {
                startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName)
                })
            } catch (e: Exception) {
                openAppSettings()
            }
        }, { NotificationManagerCompat.from(this).areNotificationsEnabled() }))
        
        // 3. Автозапуск (только Xiaomi)
        if (DeviceHelper.getManufacturer() == DeviceHelper.Manufacturer.XIAOMI) {
            steps.add(Step("🚀 Автозапуск", 
                "Найдите Антимошенник и включите переключатель.\n\nЕсли не открылось — зайдите:\nНастройки → Приложения → Разрешения → Автозапуск", {
                if (!DeviceHelper.openAutoStartSettings(this)) {
                    // Fallback - открываем настройки приложения
                    openAppSettings()
                    Toast.makeText(this, "Откройте: Разрешения → Автозапуск", Toast.LENGTH_LONG).show()
                }
            }, { null }))
            
            // 4. Закрепить
            steps.add(Step("🔒 Закрепить в памяти", 
                "1. Нажмите кнопку □ (недавние)\n2. Найдите карточку Антимошенник\n3. Удерживайте или потяните вниз\n4. Появится замок 🔒\n\nЭто не даст системе убить приложение.", {
                // Показываем toast и переходим на главный экран
                Toast.makeText(this, "Нажмите □ и закрепите Антимошенник", Toast.LENGTH_LONG).show()
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            }, { null }))
        }
    }
    
    private fun openAppSettings() {
        try {
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Откройте Настройки → Приложения → Антимошенник", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showStep(i: Int) {
        if (i >= steps.size) { 
            Settings.setXiaomiSetupDone(this)
            Toast.makeText(this, "✅ Настройка завершена!", Toast.LENGTH_SHORT).show()
            finish()
            return 
        }
        currentStep = i
        val s = steps[i]
        tvStep.text = "Шаг ${i+1} из ${steps.size}"
        tvTitle.text = s.title
        tvDesc.text = s.desc
        updateStatus()
    }
    
    private fun updateStatus() {
        when (steps[currentStep].check()) {
            true -> { tvStatus.text = "✅ Уже настроено!"; tvStatus.setTextColor(0xFF4CAF50.toInt()) }
            false -> { tvStatus.text = "❌ Нужно настроить"; tvStatus.setTextColor(0xFFF44336.toInt()) }
            null -> { tvStatus.text = "⚠️ Настройте вручную"; tvStatus.setTextColor(0xFFFF9800.toInt()) }
        }
    }
    
    private fun doAction() { 
        steps[currentStep].action()
    }
    
    private fun nextStep() = showStep(currentStep + 1)
    
    override fun onResume() { 
        super.onResume()
        if (currentStep < steps.size) {
            updateStatus()
            // Автопереход если настроено
            if (steps[currentStep].check() == true) {
                btnNext.postDelayed({ nextStep() }, 800)
            }
        }
    }
}
