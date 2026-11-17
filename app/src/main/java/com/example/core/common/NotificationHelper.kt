package com.example.core.common

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.AppLanguage
import java.util.Calendar

enum class GentleReminderType {
    MISSION_REMINDER,
    UNFINISHED_PROJECT,
    LOG_RESULT,
    SHORT_ACTIVITY,
    ACHIEVEMENT_CELEBRATION
}

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "my_journey_gentle_reminders"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Journey Gentle Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Optional, encouraging reminders with quiet hours support"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun isWithinQuietHours(quietStartHour: Int, quietEndHour: Int): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (quietStartHour > quietEndHour) {
            currentHour >= quietStartHour || currentHour < quietEndHour
        } else {
            currentHour in quietStartHour until quietEndHour
        }
    }

    fun sendGentleReminder(
        type: GentleReminderType,
        language: AppLanguage,
        notificationsEnabled: Boolean,
        quietStartHour: Int,
        quietEndHour: Int,
        detailText: String = ""
    ): Boolean {
        if (!notificationsEnabled) return false
        if (type != GentleReminderType.ACHIEVEMENT_CELEBRATION && isWithinQuietHours(quietStartHour, quietEndHour)) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val isFa = language == AppLanguage.PERSIAN
        val (title, body) = when (type) {
            GentleReminderType.MISSION_REMINDER -> if (isFa) {
                "همراه کاوشگر، وقت کنجکاوی!" to "هر وقت حوصله داشتی یک مأموریت جدید منتظر کشف توست."
            } else {
                "Ready to explore?" to "Whenever you feel like it, a fresh mission is waiting."
            }
            GentleReminderType.UNFINISHED_PROJECT -> if (isFa) {
                "پروژه شخصی‌ات منتظر ایده تازه است" to (detailText.ifBlank { "می‌توانی فقط ۵ دقیقه روی مرحله بعدی کار کنی." })
            } else {
                "Your project is waiting" to (detailText.ifBlank { "Even 5 minutes on your next step counts." })
            }
            GentleReminderType.LOG_RESULT -> if (isFa) {
                "ثبت تجربه امروز" to "اگر مأموریتت را انجام دادی، یادت نرود حس و نتیجه خودت را بنویسی."
            } else {
                "Capture what you learned" to "Don't forget to jot down your reflection when ready."
            }
            GentleReminderType.SHORT_ACTIVITY -> if (isFa) {
                "پیشنهاد کوتاه ۵ دقیقه‌ای" to "روز شلوغی داشتی؟ نسخه کوتاه مأموریت برای همین روزهاست."
            } else {
                "Quick 5-minute idea" to "Busy day? Try a relaxed 5-minute short version."
            }
            GentleReminderType.ACHIEVEMENT_CELEBRATION -> if (isFa) {
                "دستاورد تازه آزاد شد!" to detailText.ifBlank { "قدم تازه تو در مسیر رشد ثبت شد." }
            } else {
                "New Achievement Unlocked!" to detailText.ifBlank { "Another milestone on your journey!" }
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(1000 + type.ordinal, notification)
        return true
    }
}
