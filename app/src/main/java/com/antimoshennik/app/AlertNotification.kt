package com.antimoshennik.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object AlertNotification {
    private const val CHANNEL_ID = "antimoshennik_alert"
    private const val NOTIFICATION_ID = 9999
    
    fun show(context: Context, score: Int, riskLevel: String, resultText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Создаём канал с высоким приоритетом
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "🚨 Оповещения о мошенниках",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Срочные оповещения о мошенничестве"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Intent для открытия AlertActivity
        val intent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlertActivity.EXTRA_RESULT_TEXT, resultText)
            putExtra(AlertActivity.EXTRA_RISK_LEVEL, riskLevel)
            putExtra(AlertActivity.EXTRA_SCORE, score)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = when (riskLevel) {
            "CRITICAL" -> "🚨 МОШЕННИК! ПОЛОЖИТЕ ТРУБКУ!"
            "HIGH" -> "⚠️ ОСТОРОЖНО! Возможно мошенник!"
            else -> "⚠️ Подозрительный звонок"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("Обнаружено: $score баллов. Нажмите для подробностей.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "⚠️ Обнаружены признаки мошенничества!\n" +
                "Баллы риска: $score\n\n" +
                "НЕ СООБЩАЙТЕ:\n" +
                "• Коды из СМС\n" +
                "• Данные карты\n" +
                "• Пароли\n\n" +
                "Нажмите для подробностей"
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)  // Показать на весь экран
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // Показать на lock screen
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
