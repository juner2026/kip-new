package com.kip.crabpet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

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
                    val i = Intent(this@MainActivity, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(i)
                    } else {
                        startService(i)
                    }
                } else {
                    Toast.makeText(this@MainActivity, "请先打开悬浮窗权限", Toast.LENGTH_SHORT).show()
                }
            }
        })
        setContentView(box)
    }
}
