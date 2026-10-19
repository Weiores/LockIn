package com.lockin.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerEngine: TimerEngine

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TimerEngine.ACTION_ALARM_COMPLETED) {
            timerEngine.onTimerFinished()
        }
    }
}
