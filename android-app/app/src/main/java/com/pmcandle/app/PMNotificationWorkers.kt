package com.pmcandle.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private const val CHANNEL_ID = "pm_candle_reminders"
private const val CATALOG_URL = "https://pmcandle.github.io/PM.Candle2026/"

internal object PMNotifications {
    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "تنبيهات PM Candle", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "تذكيرات السلة والأشكال الجديدة والخصومات"
            }
        )
    }

    fun show(context: Context, id: Int, title: String, message: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}

class CartReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val cart = applicationContext.getSharedPreferences("pm_cart_state", Context.MODE_PRIVATE)
            .getString("items", "[]") ?: "[]"
        val items = runCatching { JSONArray(cart) }.getOrNull()
        val count = if (items == null) 0 else (0 until items.length()).sumOf { items.optJSONObject(it)?.optInt("quantity", 0) ?: 0 }
        if (count > 0) PMNotifications.show(applicationContext, 1201, "لسه في أشكال مستنياكي 🛍️", "عندك $count قطعة في سلة PM Candle ولسه الطلب متأكدش. كمّلي طلبك وقت ما تحبي.")
        return Result.success()
    }
}

class DiscountReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        PMNotifications.show(applicationContext, 1202, "خصم PM Candle 🎁", "متنسيش تشوفي الخصم المتاح على كميات توزيعات PM Candle.")
        return Result.success()
    }
}

class NewCatalogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val connection = (URL(CATALOG_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 30000
                setRequestProperty("User-Agent", "PM-Candle-Android/1.0")
            }
            val html = try { connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
                finally { connection.disconnect() }
            val match = Regex("var PRODUCTS=(\\[.*?\\]);", RegexOption.DOT_MATCHES_ALL).find(html)
                ?: return Result.retry()
            val products = JSONArray(match.groupValues[1])
            val ids = (0 until products.length()).mapNotNull { products.optJSONObject(it)?.optInt("id")?.takeIf { id -> id > 0 } }.toSet()
            val prefs = applicationContext.getSharedPreferences("pm_catalog_state", Context.MODE_PRIVATE)
            val oldRaw = prefs.getString("ids", null)
            if (oldRaw == null) {
                prefs.edit().putString("ids", ids.joinToString(",")).apply()
            } else {
                val oldIds = oldRaw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
                val added = ids - oldIds
                if (added.isNotEmpty()) PMNotifications.show(applicationContext, 1203, "أشكال جديدة وصلت! 🕯️", "اتضاف ${added.size} شكل جديد لكتالوج PM Candle. افتحي التطبيق وشوفيهم.")
                prefs.edit().putString("ids", ids.joinToString(",")).apply()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
