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

        // Deliberately its own high-importance channel: the reminders only matter
        // if they pop up over whatever the user switched to.
        val appClosedChannel = NotificationChannel(
            Constants.APP_CLOSED_CHANNEL_ID,
            context.getString(R.string.app_closed_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.app_closed_channel_description)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val distractionChannel = NotificationChannel(
            Constants.DISTRACTION_CHANNEL_ID,
            context.getString(R.string.distraction_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.distraction_channel_description)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(generalChannel)
        notificationManager.createNotificationChannel(appClosedChannel)
        notificationManager.createNotificationChannel(distractionChannel)
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

    /**
     * The countdown warning. The service re-posts this under the same id every
     * second, so the remaining time ticks down in place instead of stacking a
     * new heads-up alert each tick. Marked ongoing + alert-once so it stays put
     * while still punching through on first appearance.
     */
    fun buildDistractionWarningNotification(
        appName: String,
        moduleCode: String?,
        secondsLeft: Long,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val target = if (moduleCode != null) "for $moduleCode" else "deep work"

        return NotificationCompat.Builder(context, Constants.DISTRACTION_CHANNEL_ID)
            .setContentTitle("⚠️ Close $appName — session ends in ${secondsLeft}s")
            .setContentText("Return to LockIn to keep the timer $target running.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setWhen(System.currentTimeMillis() + secondsLeft * 1000L)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()
    }

    fun cancelDistractionWarningNotification() {
        notificationManager.cancel(Constants.DISTRACTION_NOTIFICATION_ID)
    }

    /** Posted the moment the grace window runs out — the session is already dead. */
    fun showSessionEndedByDistraction(appName: String, moduleCode: String?) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val target = if (moduleCode != null) "on $moduleCode" else "deep work session"
        val notification = NotificationCompat.Builder(context, Constants.DISTRACTION_CHANNEL_ID)
            .setContentTitle("Session ended")
            .setContentText("You stayed in $appName for ${Constants.DISTRACTION_GRACE_SECONDS}s. The $target was marked failed.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 300, 120, 300))
            .build()

        notificationManager.notify(Constants.SESSION_ENDED_NOTIFICATION_ID, notification)
    }

    fun showWifiStudyZonePromptNotification(zoneLabel: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, Constants.GENERAL_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("📍 Arrived at $zoneLabel")
            .setContentText("Ready to lock in? Tap to start a deep work session.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(Constants.WIFI_PROMPT_NOTIFICATION_ID, notification)
    }

    fun showAppClosedNotification(remainingMs: Long, moduleCode: String?) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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

        val title = if (moduleCode != null) "⏳ LockIn Active • $moduleCode" else "⏳ LockIn Active • Deep Work"
        val text = "$timeFormatted remaining. Keep studying! Tap to return to LockIn."

        val notification = NotificationCompat.Builder(context, Constants.APP_CLOSED_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Open LockIn", contentIntent)
            .addAction(0, "Give Up", stopIntent)
            .build()

        notificationManager.notify(Constants.APP_CLOSED_REMINDER_NOTIFICATION_ID, notification)
    }

    fun cancelTimerNotification() {
        notificationManager.cancel(Constants.TIMER_NOTIFICATION_ID)
    }

    /** Makes sure the "app closed" heads-up is gone once the user is back in the app. */
    fun cancelAppClosedNotification() {
        notificationManager.cancel(Constants.APP_CLOSED_REMINDER_NOTIFICATION_ID)
    }
}

