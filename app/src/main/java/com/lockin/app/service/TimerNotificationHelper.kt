package com.lockin.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lockin.app.R
import com.lockin.app.ui.MainActivity
import com.lockin.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val timerChannel = NotificationChannel(
            Constants.TIMER_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW, // Low importance so it doesn't repeatedly make sounds on tick
        ).apply {
            description = context.getString(R.string.timer_notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val generalChannel = NotificationChannel(
            Constants.GENERAL_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.general_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.general_notification_channel_description)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(generalChannel)
    }

    fun buildTimerNotification(
        remainingMs: Long,
        totalMs: Long,
        isPaused: Boolean,
        moduleCode: String? = null,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val minutes = (remainingMs / 1000) / 60
        val seconds = (remainingMs / 1000) % 60
        val timeFormatted = "%02d:%02d".format(minutes, seconds)

        val title = if (moduleCode != null) "LockIn • $moduleCode" else "LockIn • Deep Work"
        val statusText = if (isPaused) "Paused ($timeFormatted remaining)" else "$timeFormatted remaining"

        val targetEpochMs = System.currentTimeMillis() + remainingMs

        return NotificationCompat.Builder(context, Constants.TIMER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                0,
                "Give Up",
                stopIntent,
            )
            .setWhen(targetEpochMs)
            .setUsesChronometer(!isPaused)
            .setChronometerCountDown(true)
            .build()
    }

    fun buildArmingNotification(
        triggerType: com.lockin.app.domain.model.TriggerType,
        moduleCode: String? = null,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (moduleCode != null) "LockIn • $moduleCode" else "LockIn • Deep Work"
        val statusText = when (triggerType) {
            com.lockin.app.domain.model.TriggerType.FLIP -> "Place phone face-down to start session"
            com.lockin.app.domain.model.TriggerType.BARCODE -> "Scan barcode to start session"
            com.lockin.app.domain.model.TriggerType.NFC -> "Tap ID card to start session"
            else -> "Ready to lock in"
        }

        return NotificationCompat.Builder(context, Constants.TIMER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Cancel", stopIntent)
            .build()
    }

    fun buildGraceNotification(
        graceRemainingSec: Long,
        moduleCode: String? = null,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = "⚠️ PHONE LIFTED! Return face-down"
        val statusText = "Session breaks in ${graceRemainingSec}s! Place phone face-down."

        return NotificationCompat.Builder(context, Constants.GENERAL_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Give Up", stopIntent)
            .build()
    }

    fun showCompletionNotification(moduleCode: String?) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = "🎉 Session Completed!"
        val text = if (moduleCode != null) {
            "Great job locking in on $moduleCode! Take a break."
        } else {
            "Great job! Deep work session completed."
        }

        val notification = NotificationCompat.Builder(context, Constants.GENERAL_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(Constants.TIMER_NOTIFICATION_ID, notification)
    }

    fun cancelTimerNotification() {
        notificationManager.cancel(Constants.TIMER_NOTIFICATION_ID)
    }
}

