package de.michelinside.glucodatahandler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import de.michelinside.glucodatahandler.common.*
import de.michelinside.glucodatahandler.common.notifier.*


class GlucoDataServiceWear: GlucoDataService(AppSource.WEAR_APP) {
    private var isForegroundService = false
    init {
        Log.d(LOG_ID, "init called")
        InternalNotifier.addNotifier(ActiveComplicationHandler, mutableSetOf(NotifyDataSource.MESSAGECLIENT,NotifyDataSource.BROADCAST,NotifyDataSource.SETTINGS,NotifyDataSource.OBSOLETE_VALUE))
        InternalNotifier.addNotifier(BatteryLevelComplicationUpdater, mutableSetOf(NotifyDataSource.CAPILITY_INFO,NotifyDataSource.BATTERY_LEVEL, NotifyDataSource.NODE_BATTERY_LEVEL))
    }

    companion object GlucoDataServiceWear {
        private val LOG_ID = "GlucoDataHandler.GlucoDataServiceWear"
        fun isWearOS3(): Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
        fun start(context: Context) {
            if (!running) {
                try {
                    val serviceIntent = Intent(
                        context,
                        de.michelinside.glucodatahandler.GlucoDataServiceWear::class.java
                    )
                    val sharedPref = context.getSharedPreferences(
                        Constants.SHARED_PREF_TAG,
                        Context.MODE_PRIVATE
                    )
                    serviceIntent.putExtra(
                        Constants.SHARED_PREF_FOREGROUND_SERVICE,
                        sharedPref.getBoolean(Constants.SHARED_PREF_FOREGROUND_SERVICE, isWearOS3())
                    )
                    context.startService(serviceIntent)
                } catch (exc: Exception) {
                    Log.e(
                        LOG_ID,
                        "GlucoDataServiceWear::start exception: " + exc.message.toString()
                    )
                }
            }
        }
    }

    override fun onCreate() {
        Log.d(LOG_ID, "onCreate called")
        super.onCreate()
        ActiveComplicationHandler.OnNotifyData(this, NotifyDataSource.CAPILITY_INFO, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            Log.d(LOG_ID, "onStartCommand called")
            super.onStartCommand(intent, flags, startId)
            val isForeground = intent?.getBooleanExtra(Constants.SHARED_PREF_FOREGROUND_SERVICE, false)
            if (isForeground == true && !isForegroundService) {
                isForegroundService = true
                Log.i(LOG_ID, "Starting service in foreground!")
                val channelId = "glucodatahandler_service_01"
                val channel = NotificationChannel(
                    channelId,
                    "Foregorund GlucoDataService",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                    channel
                )
                val notificationIntent = Intent(this, WaerActivity::class.java)

                val pendingIntent = PendingIntent.getActivity(
                    this, 1,
                    notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification: Notification = Notification.Builder(this, channelId)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.forground_notification_descr))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .build()
                startForeground(1, notification)
            } else if ( isForegroundService && intent?.getBooleanExtra(Constants.ACTION_STOP_FOREGROUND, false) == true ) {
                isForegroundService = false
                Log.i(LOG_ID, "Stopping service in foreground!")
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        } catch (exc: Exception) {
            Log.e(LOG_ID, "onStartCommand exception: " + exc.toString())
        }
        return START_STICKY  // keep alive
    }

}