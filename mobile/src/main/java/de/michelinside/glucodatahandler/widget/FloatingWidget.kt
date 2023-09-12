package de.michelinside.glucodatahandler.widget

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.provider.Settings
import de.michelinside.glucodatahandler.MainActivity
import de.michelinside.glucodatahandler.R
import de.michelinside.glucodatahandler.common.Constants
import de.michelinside.glucodatahandler.common.ReceiveData
import de.michelinside.glucodatahandler.common.Utils
import de.michelinside.glucodatahandler.common.notifier.InternalNotifier
import de.michelinside.glucodatahandler.common.notifier.NotifierInterface
import de.michelinside.glucodatahandler.common.notifier.NotifyDataSource
import java.text.DateFormat
import java.util.*


class FloatingWidget(val context: Context) : NotifierInterface, SharedPreferences.OnSharedPreferenceChangeListener {
    private var mWindowManager: WindowManager? = null
    private lateinit var mFloatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var txtBgValue: TextView
    private lateinit var viewIcon: ImageView
    private lateinit var txtDelta: TextView
    private lateinit var txtTime: TextView
    private lateinit var sharedPref: SharedPreferences
    private val shortTimeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private val LOG_ID = "GlucoDataHandler.FloatingWidget"

    fun create() {
        Log.d(LOG_ID, "create called")
        sharedPref = context.getSharedPreferences(Constants.SHARED_PREF_TAG, Context.MODE_PRIVATE)
        sharedPref.registerOnSharedPreferenceChangeListener(this)

        //getting the widget layout from xml using layout inflater
        mFloatingView = LayoutInflater.from(context).inflate(R.layout.floating_widget, null)
        txtBgValue = mFloatingView.findViewById(R.id.glucose)
        viewIcon = mFloatingView.findViewById(R.id.trendImage)
        txtDelta = mFloatingView.findViewById(R.id.deltaText)
        txtTime = mFloatingView.findViewById(R.id.timeText)
        //setting the layout parameters
        update()
    }

    fun destroy() {
        Log.d(LOG_ID, "destroy called")
        sharedPref.unregisterOnSharedPreferenceChangeListener(this)
        remove()
    }

    private fun remove() {
        Log.d(LOG_ID, "remove called")
        InternalNotifier.remNotifier(this)
        if (mWindowManager != null) mWindowManager?.removeView(mFloatingView)
        mWindowManager = null
    }

    private fun update() {
        Log.d(LOG_ID, "update called")
        try {
            if (sharedPref.getBoolean(Constants.SHARED_PREF_FLOATING_WIDGET, false)) {
                val filter = mutableSetOf(
                    NotifyDataSource.BROADCAST,
                    NotifyDataSource.MESSAGECLIENT,
                    NotifyDataSource.SETTINGS,
                    NotifyDataSource.OBSOLETE_VALUE)   // to trigger re-start for the case of stopped by the system
                InternalNotifier.addNotifier(this, filter)
                if (Settings.canDrawOverlays(context)) {
                    txtBgValue.text = ReceiveData.getClucoseAsString()
                    txtBgValue.setTextColor(ReceiveData.getClucoseColor())
                    if (ReceiveData.isObsolete(Constants.VALUE_OBSOLETE_SHORT_SEC) && !ReceiveData.isObsolete()) {
                        txtBgValue.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    } else {
                        txtBgValue.paintFlags = 0
                    }
                    viewIcon.setImageIcon(Utils.getRateAsIcon())
                    txtDelta.text =ReceiveData.getDeltaAsString()
                    txtTime.text = shortTimeFormat.format(Date(ReceiveData.time))

                    val resizeFactor = sharedPref.getInt(Constants.SHARED_PREF_FLOATING_WIDGET_SIZE, 3).toFloat()
                    txtBgValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f+resizeFactor*4f)
                    viewIcon.minimumWidth = Utils.dpToPx(32f+resizeFactor*4f, context)
                    txtDelta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f+resizeFactor*2f)
                    txtTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f+resizeFactor*2f)

                    //getting windows services and adding the floating view to it
                    if (mWindowManager == null) {
                        createWindow()
                    } else {
                        Log.d(LOG_ID, "update window")
                        mWindowManager!!.updateViewLayout(mFloatingView, params)
                        Log.d(LOG_ID, "window size width/height: " + mFloatingView.width + "/" + mFloatingView.height)
                    }
                }
            } else {
                remove()
            }
        } catch (exc: Exception) {
            Log.e(LOG_ID, "update exception: " + exc.message.toString() )
        }
    }

    private fun createWindow() {
        Log.d(LOG_ID, "create window")
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.x = sharedPref.getInt(Constants.SHARED_PREF_FLOATING_WIDGET_X, 0)
        params.y = sharedPref.getInt(Constants.SHARED_PREF_FLOATING_WIDGET_Y, 0)

        mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager?
        mWindowManager!!.addView(mFloatingView, params)

        val widget = mFloatingView.findViewById<View>(R.id.widget)
        widget.setOnTouchListener(object : OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var startClickTime : Long = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        startClickTime = Calendar.getInstance().timeInMillis
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // only check duration, if there was no movement...
                        with(sharedPref.edit()) {
                            putInt(Constants.SHARED_PREF_FLOATING_WIDGET_X,params.x)
                            putInt(Constants.SHARED_PREF_FLOATING_WIDGET_Y,params.y)
                            apply()
                        }
                        if  (Math.abs(params.x - initialX) < 50 && Math.abs(params.y - initialY) < 50 ) {
                            val duration = Calendar.getInstance().timeInMillis - startClickTime
                            Log.d(LOG_ID, "Duration: " + duration.toString() + " - x=" + Math.abs(params.x - initialX) + " y=" + Math.abs(params.y - initialY) )
                            if (duration < 200) {
                                Log.d(LOG_ID, "Call application after " + duration.toString() + "ms")
                                var launchIntent: Intent? = context.packageManager.getLaunchIntentForPackage("tk.glucodata")
                                if (launchIntent == null) {
                                    launchIntent = Intent(context, MainActivity::class.java)
                                }
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            } else if (duration > 4000) {
                                with(sharedPref.edit()) {
                                    Log.d(LOG_ID, "remove floating widget after " + duration.toString() + "ms")
                                    putBoolean(Constants.SHARED_PREF_FLOATING_WIDGET,false)
                                    apply()
                                }
                                remove()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        //this code is helping the widget to move around the screen with fingers
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        mWindowManager!!.updateViewLayout(mFloatingView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        try {
            Log.d(LOG_ID, "onSharedPreferenceChanged called for key " + key)
            when(key) {
                Constants.SHARED_PREF_FLOATING_WIDGET,
                Constants.SHARED_PREF_FLOATING_WIDGET_SIZE -> {
                    update()
                }
            }
        } catch (exc: Exception) {
            Log.e(LOG_ID, "onSharedPreferenceChanged exception: " + exc.toString() )
        }
    }

    override fun OnNotifyData(context: Context, dataSource: NotifyDataSource, extras: Bundle?) {
        try {
            Log.d(LOG_ID, "OnNotifyData called for source " + dataSource.toString())
            update()
        } catch (exc: Exception) {
            Log.e(LOG_ID, "OnNotifyData exception: " + exc.toString() )
        }
    }
}