package com.antimoshennik.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object DeviceHelper {
    
    enum class Manufacturer {
        XIAOMI, SAMSUNG, HUAWEI, OPPO, VIVO, ONEPLUS, GOOGLE, OTHER
    }
    
    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> Manufacturer.XIAOMI
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("huawei") || brand.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("oppo") || brand.contains("realme") -> Manufacturer.OPPO
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            manufacturer.contains("oneplus") -> Manufacturer.ONEPLUS
            manufacturer.contains("google") -> Manufacturer.GOOGLE
            else -> Manufacturer.OTHER
        }
    }
    
    fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    fun getSettingsInstructions(): String {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI -> """
                📱 Настройки для Xiaomi/Redmi/POCO:
                
                1️⃣ Автозапуск:
                Настройки → Приложения → Разрешения → Автозапуск → включить Антимошенник
                
                2️⃣ Батарея:
                Настройки → Приложения → Антимошенник → Батарея → Без ограничений
                
                3️⃣ Заблокировать в недавних:
                Открыть недавние → найти Антимошенник → потянуть вниз (появится замок 🔒)
                
                4️⃣ Уведомления:
                Настройки → Приложения → Антимошенник → Уведомления → Включить все + Плавающие
            """.trimIndent()
            
            Manufacturer.SAMSUNG -> """
                📱 Настройки для Samsung:
                
                1️⃣ Батарея:
                Настройки → Приложения → Антимошенник → Батарея → Неограниченный
                
                2️⃣ Спящие приложения:
                Настройки → Обслуживание устройства → Батарея → Ограничения в фоне → 
                Никогда не переводить в сон → добавить Антимошенник
                
                3️⃣ Уведомления:
                Настройки → Приложения → Антимошенник → Уведомления → Включить все
                
                ✅ Samsung обычно хорошо работает с фоновыми приложениями!
            """.trimIndent()
            
            Manufacturer.HUAWEI -> """
                📱 Настройки для Huawei/Honor:
                
                1️⃣ Автозапуск:
                Настройки → Батарея → Запуск приложений → найти Антимошенник → 
                выключить автоматическое управление → включить все 3 переключателя
                
                2️⃣ Защищённые приложения:
                Настройки → Батарея → Защищённые приложения → включить Антимошенник
                
                3️⃣ Игнорировать оптимизацию:
                Настройки → Приложения → Антимошенник → Батарея → 
                Игнорировать оптимизацию батареи
                
                4️⃣ Заблокировать в недавних:
                Недавние → провести вниз по карточке приложения (появится замок)
                
                ⚠️ Huawei очень агрессивно убивает приложения!
            """.trimIndent()
            
            Manufacturer.OPPO -> """
                📱 Настройки для OPPO/Realme:
                
                1️⃣ Автозапуск:
                Настройки → Управление приложениями → Список приложений → 
                Антимошенник → Автозапуск → включить
                
                2️⃣ Батарея:
                Настройки → Батарея → Оптимизация батареи → 
                Антимошенник → Не оптимизировать
                
                3️⃣ Заблокировать:
                Недавние → долгое нажатие на приложении → Заблокировать
            """.trimIndent()
            
            Manufacturer.VIVO -> """
                📱 Настройки для Vivo:
                
                1️⃣ Автозапуск:
                Настройки → Разрешения → Автозапуск → включить Антимошенник
                
                2️⃣ Батарея:
                Настройки → Батарея → Высокое потребление в фоне → 
                разрешить Антимошенник
                
                3️⃣ Заблокировать:
                Недавние → провести вниз по карточке
            """.trimIndent()
            
            Manufacturer.ONEPLUS -> """
                📱 Настройки для OnePlus:
                
                1️⃣ Оптимизация батареи:
                Настройки → Батарея → Оптимизация батареи → 
                Антимошенник → Не оптимизировать
                
                2️⃣ Заблокировать:
                Недавние → долгое нажатие → Заблокировать
                
                ✅ OnePlus обычно хорошо работает!
            """.trimIndent()
            
            Manufacturer.GOOGLE -> """
                📱 Настройки для Google Pixel:
                
                1️⃣ Батарея:
                Настройки → Приложения → Антимошенник → Батарея → Неограниченно
                
                ✅ Google Pixel отлично работает с фоновыми приложениями!
            """.trimIndent()
            
            Manufacturer.OTHER -> """
                📱 Общие настройки:
                
                1️⃣ Батарея:
                Настройки → Приложения → Антимошенник → Батарея → 
                Без ограничений / Не оптимизировать
                
                2️⃣ Автозапуск (если есть):
                Настройки → Приложения → Автозапуск → включить
                
                3️⃣ Уведомления:
                Настройки → Приложения → Антимошенник → Уведомления → Включить
            """.trimIndent()
        }
    }
    
    fun openAutoStartSettings(context: Context): Boolean {
        val intents = when (getManufacturer()) {
            Manufacturer.XIAOMI -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", 
                    "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                Intent().setComponent(ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"))
            )
            Manufacturer.HUAWEI -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"))
            )
            Manufacturer.OPPO -> listOf(
                Intent().setComponent(ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"))
            )
            Manufacturer.VIVO -> listOf(
                Intent().setComponent(ComponentName("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            )
            else -> emptyList()
        }
        
        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                continue
            }
        }
        return false
    }
    
    fun openBatterySettings(context: Context): Boolean {
        val intents = when (getManufacturer()) {
            Manufacturer.XIAOMI -> listOf(
                Intent().setComponent(ComponentName("com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
            )
            Manufacturer.HUAWEI -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"))
            )
            Manufacturer.SAMSUNG -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"))
            )
            else -> emptyList()
        }
        
        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                continue
            }
        }
        
        // Fallback
        return try {
            BatteryHelper.requestIgnoreBatteryOptimizations(context)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun needsSpecialSetup(): Boolean {
        return getManufacturer() in listOf(
            Manufacturer.XIAOMI, Manufacturer.HUAWEI, 
            Manufacturer.OPPO, Manufacturer.VIVO
        )
    }
}
