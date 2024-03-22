package de.michelinside.glucodatahandler.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.michelinside.glucodatahandler.common.Constants
import de.michelinside.glucodatahandler.common.utils.Utils

class SnoozeAlarmReceiver: BroadcastReceiver() {
    private val LOG_ID = "GDH.AlarmSnoozeReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(LOG_ID, "Intent ${intent.action} received: ${Utils.dumpBundle(intent.extras)}" )
        AlarmNotification.stopNotifications(intent.getIntExtra(Constants.ALARM_SNOOZE_EXTRA_NOTIFY_ID, 0), context)
    }

}