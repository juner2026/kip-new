package com.kip.crabpet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 40)
        }
        box.addView(TextView(this).apply {
            text = "螃蟹桌宠\n\n先打开悬浮窗权限，再启动桌宠。"
            textSize = 20f
        })
        box.addView(Button(this).apply {
            text = "打开悬浮窗权限"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        box.addView(Button(this).apply {
            text = "启动桌宠"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                }
            }
        })
        setContentView(box)
    }
}
