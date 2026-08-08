

package com.sentinel.freewrite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import java.util.Arrays

class OverlayService : Service() {

    companion object {
        private const val TAG = "SentinelOverlay"
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private var vibrator: Vibrator? = null
    private var sensitiveTextBuffer: CharArray? = CharArray(1024)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "OverlayService onCreate")
        startForegroundServiceNotification()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        // Inflate your floating widget layout
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        // Set layout parameters for system-level overlay window
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(overlayView, params)
        Log.i(TAG, "Overlay view attached at x=${params.x}, y=${params.y}")

        val phoenixIcon = overlayView?.findViewById<ImageView>(R.id.phoenix_icon)
        phoenixIcon?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_anim))
        Log.i(TAG, "Phoenix pulse animation started")

        phoenixIcon?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        Log.d(TAG, "Touch down at x=${event.rawX}, y=${event.rawY}")
                        triggerHapticFeedback()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val isClick = Math.abs(event.rawX - initialTouchX) < 10 &&
                            Math.abs(event.rawY - initialTouchY) < 10
                        Log.d(TAG, "Touch up at x=${event.rawX}, y=${event.rawY}, isClick=$isClick")
                        if (isClick) {
                            onPhoenixWidgetClicked()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
            Log.d(TAG, "Haptic feedback triggered")
        } catch (securityException: SecurityException) {
            Log.e(TAG, "Haptic feedback unavailable; check android.permission.VIBRATE", securityException)
        }
    }

    private fun onPhoenixWidgetClicked() {
        Log.i(TAG, "Phoenix widget clicked")
        triggerHapticFeedback()
    }

    private fun wipeSensitiveMemory() {
        sensitiveTextBuffer?.let {
            Arrays.fill(it, '\u0000')
            Log.i(TAG, "Sensitive memory buffer wiped")
        }
        sensitiveTextBuffer = null
    }

    private fun startForegroundServiceNotification() {
        val channelId = "sentinel_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sentinel Overlay Active",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("The Sentinel Free Write")
            .setContentText("Overlay service is running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        Log.i(TAG, "OverlayService onDestroy")
        super.onDestroy()
        wipeSensitiveMemory()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            Log.i(TAG, "Overlay view removed")
        }
    }
}
