package com.kip.crabpet

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class OverlayService : Service() {
    private var wm: WindowManager? = null
    private var web: WebView? = null
    private lateinit var lp: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false
    private var downAt = 0L
    private var lastTap = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        if (Build.VERSION.SDK_INT >= 26) startForeground(7, notification())
        setupOverlay()
    }

    private fun setupOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        lp = WindowManager.LayoutParams(dp(180), dp(220), type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(30)
            y = dp(220)
        }
        web = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/pet.html")
            setOnTouchListener { _, e -> touch(e) }
        }
        wm?.addView(web, lp)
    }

    private fun touch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y
                downAt = System.currentTimeMillis(); moved = false; return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (e.rawX - downX).toInt(); val dy = (e.rawY - downY).toInt()
                if (kotlin.math.abs(dx) > dp(8) || kotlin.math.abs(dy) > dp(8)) moved = true
                if (moved) { lp.x = startX + dx; lp.y = startY + dy; wm?.updateViewLayout(web, lp) }
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
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("pet", "桌宠", NotificationManager.IMPORTANCE_LOW))
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
