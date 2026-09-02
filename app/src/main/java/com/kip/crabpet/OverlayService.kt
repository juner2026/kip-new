package com.kip.crabpet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient

class OverlayService : Service() {
    private var wm: WindowManager? = null
    private var web: WebView? = null
    private var lp: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false
    private var downAt = 0L
    private var lastTap = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            createChannel()
            startForeground(7, notification())
        } catch (e: Exception) {
            Log.e("CrabPet", "startForeground failed", e)
        }
        handler.post { setupOverlay() }
        return START_STICKY
    }

    private fun setupOverlay() {
        try {
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            val p = WindowManager.LayoutParams(
                dp(180), dp(220), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dp(30)
                y = dp(220)
            }
            lp = p
            val v = WebView(this).apply {
                setBackgroundColor(0x00000000)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/pet.html")
                setOnTouchListener { _, e -> touch(e) }
            }
            web = v
            wm?.addView(v, p)
        } catch (e: Exception) {
            Log.e("CrabPet", "overlay setup failed", e)
        }
    }

    private fun touch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val p = lp ?: return true
                downX = e.rawX; downY = e.rawY; startX = p.x; startY = p.y
                downAt = System.currentTimeMillis(); moved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val p = lp ?: return true
                val dx = (e.rawX - downX).toInt(); val dy = (e.rawY - downY).toInt()
                if (kotlin.math.abs(dx) > dp(8) || kotlin.math.abs(dy) > dp(8)) moved = true
                if (moved) { p.x = startX + dx; p.y = startY + dy; wm?.updateViewLayout(web, p) }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) {
                    val elapsed = System.currentTimeMillis() - downAt
                    when {
                        elapsed > 600 -> js("onLongPress")
                        System.currentTimeMillis() - lastTap < 300 -> { js("onDoubleTap"); lastTap = 0 }
                        else -> { js("onTap"); lastTap = System.currentTimeMillis() }
                    }
                }
                return true
            }
        }
        return true
    }

    private fun js(name: String) { web?.evaluateJavascript("window.petEngine && window.petEngine.$name()", null) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("pet", "桌宠", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, packageManager.getLaunchIntentForPackage(packageName), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "pet").setContentTitle("螃蟹桌宠").setContentText("桌宠正在屏幕上").setSmallIcon(android.R.drawable.ic_menu_compass).setContentIntent(pi).setOngoing(true).build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        web?.let { wm?.removeView(it); it.destroy() }
        web = null
        super.onDestroy()
    }
}
