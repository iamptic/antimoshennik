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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupViews()
    }

    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener { 
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        
        // Permissions
        updatePermissionsStatus()
        findViewById<View>(R.id.cardPermissions).setOnClickListener { openAppSettings() }
        findViewById<View>(R.id.btnFixPermissions).setOnClickListener { requestAllPermissions() }
        
        // SOS Phone
        findViewById<View>(R.id.cardSosPhone).setOnClickListener { showPhoneDialog() }
        updatePhoneDisplay()
        
        // Threshold
        findViewById<View>(R.id.cardThreshold).setOnClickListener { showThresholdDialog() }
        updateThresholdDisplay()
        
        // Whitelist
        findViewById<View>(R.id.cardWhitelist).setOnClickListener { showWhitelistDialog() }
        updateWhitelistDisplay()
        
        
        // Auto call
        val switchAutoCall = findViewById<Switch>(R.id.switchAutoCall)
        switchAutoCall.isChecked = Settings.isAutoCallEnabled(this)
        switchAutoCall.setOnCheckedChangeListener { _, isChecked ->
            Settings.setAutoCallEnabled(this, isChecked)
        }
        
        // Telegram
        val switchTelegram = findViewById<Switch>(R.id.switchTelegram)
        switchTelegram.isChecked = Settings.isTelegramEnabled(this)
        switchTelegram.setOnCheckedChangeListener { _, isChecked ->
            Settings.setTelegramEnabled(this, isChecked)
        }
        
        findViewById<View>(R.id.cardTelegramId).setOnClickListener { showTelegramIdDialog() }
        updateTelegramDisplay()
    }
    
    private fun showTelegramIdDialog() {
        val input = EditText(this).apply {
            hint = "ID из бота (числа)"
            setText(Settings.getTelegramChatId(this@SettingsActivity))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(60, 40, 60, 40)
        }
        
        AlertDialog.Builder(this)
            .setTitle("📱 Telegram ID")
            .setMessage("1. Откройте @AntimoshennikBot\n2. Нажмите /start\n3. Скопируйте ID")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                Settings.setTelegramChatId(this, input.text.toString().trim())
                updateTelegramDisplay()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun updateTelegramDisplay() {
        val id = Settings.getTelegramChatId(this)
        val tv = findViewById<TextView>(R.id.tvTelegramId)
        if (id.isBlank()) {
            tv.text = "Не указан"
            tv.setTextColor(ContextCompat.getColor(this, R.color.danger))
        } else {
            tv.text = id
            tv.setTextColor(ContextCompat.getColor(this, R.color.success))
        }
    }

    private fun updatePermissionsStatus() {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        } else true
        
        val hasPhone = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val hasBattery = pm.isIgnoringBatteryOptimizations(packageName)
        
        findViewById<TextView>(R.id.tvPermStorage).text = if (hasStorage) "✅" else "❌"
        findViewById<TextView>(R.id.tvPermNotifications).text = if (hasNotifications) "✅" else "❌"
        findViewById<TextView>(R.id.tvPermPhone).text = if (hasPhone) "✅" else "❌"
        findViewById<TextView>(R.id.tvPermBattery).text = if (hasBattery) "✅" else "❌"
        
        // Показываем кнопку если не все разрешения даны
        val allGranted = hasStorage && hasNotifications && hasPhone && hasBattery
        findViewById<View>(R.id.btnFixPermissions).visibility = if (allGranted) View.GONE else View.VISIBLE
    }

    private fun requestAllPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        perms.add(Manifest.permission.CALL_PHONE)
        
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
        
        // Открываем настройки батареи
        try {
            startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (e: Exception) {}
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

    override fun onResume() {
        super.onResume()
        updatePermissionsStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updatePermissionsStatus()
    }

    private fun showPhoneDialog() {
        val input = EditText(this).apply {
            hint = "+7 XXX XXX XX XX"
            setText(Settings.getFamilyPhone(this@SettingsActivity))
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(60, 40, 60, 40)
        }
        
        AlertDialog.Builder(this)
            .setTitle("📞 SOS номер")
            .setMessage("При угрозе позвоним 3 раза подряд")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                Settings.setFamilyPhone(this, input.text.toString().trim())
                updatePhoneDisplay()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updatePhoneDisplay() {
        val phone = Settings.getFamilyPhone(this)
        val tv = findViewById<TextView>(R.id.tvPhoneValue)
        if (phone.isBlank()) {
            tv.text = "Не указан"
            tv.setTextColor(ContextCompat.getColor(this, R.color.danger))
        } else {
            tv.text = phone
            tv.setTextColor(ContextCompat.getColor(this, R.color.success))
        }
    }

    private fun showThresholdDialog() {
        val currentThreshold = Settings.getAlertThreshold(this)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }
        
        val label = TextView(this).apply {
            text = "$currentThreshold баллов"
            textSize = 20f
            setPadding(0, 0, 0, 20)
        }
        
        val seekBar = SeekBar(this).apply {
            max = 100
            progress = currentThreshold - 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                    label.text = "${p + 50} баллов"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        
        val hint = TextView(this).apply {
            text = "50 = чувствительно (больше алертов)\n80 = рекомендуется\n150 = только явные угрозы"
            textSize = 13f
            alpha = 0.7f
            setPadding(0, 20, 0, 0)
        }
        
        layout.addView(label)
        layout.addView(seekBar)
        layout.addView(hint)
        
        AlertDialog.Builder(this)
            .setTitle("🎚️ Чувствительность")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                Settings.setAlertThreshold(this, seekBar.progress + 50)
                updateThresholdDisplay()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateThresholdDisplay() {
        val threshold = Settings.getAlertThreshold(this)
        findViewById<TextView>(R.id.tvThresholdValue).text = "$threshold баллов"
    }

    private fun showWhitelistDialog() {
        val whitelist = Settings.getWhitelist(this).toMutableList()
        
        val options = mutableListOf("➕ Добавить номер")
        whitelist.forEach { options.add("📞 $it") }
        
        AlertDialog.Builder(this)
            .setTitle("✅ Белый список")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    addToWhitelist()
                } else {
                    removeFromWhitelist(whitelist[which - 1])
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun addToWhitelist() {
        val input = EditText(this).apply {
            hint = "+7XXXXXXXXXX"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(60, 40, 60, 40)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Добавить номер")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val phone = input.text.toString().trim()
                if (phone.length >= 10) {
                    Settings.addToWhitelist(this, phone)
                    updateWhitelistDisplay()
                    Toast.makeText(this, "✅ Добавлен", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun removeFromWhitelist(phone: String) {
        AlertDialog.Builder(this)
            .setTitle("Удалить?")
            .setMessage(phone)
            .setPositiveButton("Удалить") { _, _ ->
                Settings.removeFromWhitelist(this, phone)
                updateWhitelistDisplay()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateWhitelistDisplay() {
        val count = Settings.getWhitelist(this).size
        findViewById<TextView>(R.id.tvWhitelistValue).text = "$count номеров"
    }
}
