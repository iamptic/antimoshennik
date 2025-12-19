package com.antimoshennik.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Автозапуск мониторинга при загрузке устройства
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting monitoring service")
            
            // Проверяем, включен ли автозапуск в настройках
            val prefs = context.getSharedPreferences("antimoshennik_prefs", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start", true)
            
            if (autoStart) {
                val serviceIntent = Intent(context, CallRecordMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
