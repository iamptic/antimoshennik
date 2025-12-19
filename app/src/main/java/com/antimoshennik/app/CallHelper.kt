package com.antimoshennik.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

object CallHelper {
    private const val TAG = "CallHelper"
    private val handler = Handler(Looper.getMainLooper())
    
    // SOS-звонок: 3 раза подряд с паузами
    fun callFamilySOS(context: Context): Boolean {
        val phone = Settings.getFamilyPhone(context)
        
        if (phone.isBlank()) {
            Log.d(TAG, "No family phone configured")
            return false
        }
        
        if (!Settings.isAutoCallEnabled(context)) {
            Log.d(TAG, "Auto call disabled")
            return false
        }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No CALL_PHONE permission")
            return false
        }
        
        // Первый звонок сразу
        makeCall(context, phone)
        Log.d(TAG, "SOS call 1/3")
        
        // Второй звонок через 8 секунд
        handler.postDelayed({
            makeCall(context, phone)
            Log.d(TAG, "SOS call 2/3")
        }, 8000)
        
        // Третий звонок через 16 секунд
        handler.postDelayed({
            makeCall(context, phone)
            Log.d(TAG, "SOS call 3/3")
        }, 16000)
        
        return true
    }
    
    // Одиночный звонок
    fun callFamily(context: Context): Boolean {
        val phone = Settings.getFamilyPhone(context)
        
        if (phone.isBlank()) return false
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) return false
        
        return makeCall(context, phone)
    }
    
    private fun makeCall(context: Context, phone: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Call failed", e)
            false
        }
    }
}
