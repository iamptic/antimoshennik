package com.antimoshennik.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class AppWidget : AppWidgetProvider() {
    
    companion object {
        /**
         * Обновляет все виджеты
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, AppWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        // Вызывается при добавлении первого виджета
    }
    
    override fun onDisabled(context: Context) {
        // Вызывается при удалении последнего виджета
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val isActive = CallRecordMonitorService.isRunning
        
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        // Устанавливаем текст
        if (isActive) {
            views.setTextViewText(R.id.widgetStatus, "🛡️ ЗАЩИТА ВКЛ")
            views.setInt(R.id.widgetBackground, "setBackgroundResource", R.drawable.widget_bg_active)
        } else {
            views.setTextViewText(R.id.widgetStatus, "⚠️ ЗАЩИТА ВЫКЛ")
            views.setInt(R.id.widgetBackground, "setBackgroundResource", R.drawable.widget_bg_inactive)
        }
        
        // Клик открывает приложение
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetBackground, pendingIntent)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
