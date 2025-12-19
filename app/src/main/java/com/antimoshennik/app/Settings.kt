package com.antimoshennik.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object Settings {
    private const val PREFS = "antimoshennik_settings"
    private const val KEY_FAMILY_PHONE = "family_phone"
    private const val KEY_AUTO_CALL = "auto_call"
    private const val KEY_ONLINE_MODE = "online_mode"
    private const val KEY_ALERT_THRESHOLD = "alert_threshold"
    private const val KEY_WHITELIST = "whitelist"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_XIAOMI_SETUP_DONE = "xiaomi_setup_done"
    private const val KEY_PATTERNS_VERSION = "patterns_version"
    private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
    private const val KEY_TELEGRAM_ENABLED = "telegram_enabled"
    
    // Номер родственника
    fun getFamilyPhone(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FAMILY_PHONE, "") ?: ""
    }
    
    fun setFamilyPhone(context: Context, phone: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_FAMILY_PHONE, phone).apply()
    }
    
    // Автозвонок
    fun isAutoCallEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CALL, true)
    }
    
    fun setAutoCallEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_CALL, enabled).apply()
    }
    
    // Онлайн режим
    fun isOnlineModeEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONLINE_MODE, true)
    }
    
    fun setOnlineModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONLINE_MODE, enabled).apply()
    }
    
    // Порог алерта (50-150, по умолчанию 80)
    fun getAlertThreshold(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ALERT_THRESHOLD, 80)
    }
    
    fun setAlertThreshold(context: Context, threshold: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ALERT_THRESHOLD, threshold.coerceIn(50, 150)).apply()
    }
    
    // Белый список номеров
    fun getWhitelist(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
    }
    
    fun setWhitelist(context: Context, numbers: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_WHITELIST, numbers).apply()
    }
    
    fun addToWhitelist(context: Context, number: String) {
        val current = getWhitelist(context).toMutableSet()
        current.add(normalizePhone(number))
        setWhitelist(context, current)
    }
    
    fun removeFromWhitelist(context: Context, number: String) {
        val current = getWhitelist(context).toMutableSet()
        current.remove(normalizePhone(number))
        setWhitelist(context, current)
    }
    
    fun isInWhitelist(context: Context, fileName: String): Boolean {
        val whitelist = getWhitelist(context)
        if (whitelist.isEmpty()) return false
        val phoneRegex = Regex("\\d{10,}")
        val match = phoneRegex.find(fileName)
        val filePhone = match?.value ?: return false
        return whitelist.any { normalizePhone(it).contains(filePhone) || filePhone.contains(normalizePhone(it).takeLast(10)) }
    }
    
    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }
    
    // Onboarding
    fun isOnboardingDone(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_DONE, false)
    }
    
    fun setOnboardingDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }
    
    // Интернет
    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Xiaomi setup
    fun isXiaomiSetupDone(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_XIAOMI_SETUP_DONE, false)
    }
    
    fun setXiaomiSetupDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_XIAOMI_SETUP_DONE, true).apply()
    }
    
    // Версия паттернов
    fun getPatternsVersion(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_PATTERNS_VERSION, 1)
    }
    
    fun setPatternsVersion(context: Context, version: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PATTERNS_VERSION, version).apply()
    }
    
    // Telegram настройки
    fun getTelegramChatId(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
    }
    
    fun setTelegramChatId(context: Context, chatId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TELEGRAM_CHAT_ID, chatId).apply()
    }
    
    fun isTelegramEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_TELEGRAM_ENABLED, false)
    }
    
    fun setTelegramEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_TELEGRAM_ENABLED, enabled).apply()
    }
}
