package com.sentinel.freewrite

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    companion object {
        private const val TAG = "SentinelMain"
    }

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
        }

        val enableButton = Button(this).apply {
            text = "Enable floating widget"
            setOnClickListener { enableOverlay() }
        }

        val stopButton = Button(this).apply {
            text = "Stop floating widget"
            setOnClickListener {
                Log.i(TAG, "Stopping OverlayService")
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                statusText.text = "Floating widget stopped"
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            addView(TextView(this@MainActivity).apply {
                text = "Sentinel Free Write"
                textSize = 24f
                gravity = Gravity.CENTER
            }, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(statusText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(enableButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(stopButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        setContentView(content)
        Log.i(TAG, "MainActivity created")
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) updateStatus()
    }

    private fun enableOverlay() {
        Log.i(TAG, "Enable overlay clicked; canDrawOverlays=${Settings.canDrawOverlays(this)}")
        if (!Settings.canDrawOverlays(this)) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            Log.i(TAG, "Opening overlay permission settings")
            startActivity(settingsIntent)
            return
        }

        Log.i(TAG, "Starting OverlayService")
        startForegroundService(Intent(this, OverlayService::class.java))
        statusText.text = "Floating widget started"
    }

    private fun updateStatus() {
        val canDrawOverlays = Settings.canDrawOverlays(this)
        Log.d(TAG, "Overlay permission state: canDrawOverlays=$canDrawOverlays")
        statusText.text = if (canDrawOverlays) {
            "Overlay permission is enabled"
        } else {
            "Overlay permission is required"
        }
    }
}
