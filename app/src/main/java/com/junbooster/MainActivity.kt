
package com.junbooster

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(12, 20, 17))
        }

        val title = TextView(this).apply {
            text = "JUN BOOSTER"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Ready to boost"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val connectButton = Button(this).apply {
            text = "CONNECT"
            textSize = 18f
            setOnClickListener {
                val intent = VpnService.prepare(this@MainActivity)
                if (intent != null) {
                    startActivityForResult(intent, 100)
                } else {
                    statusText.text = "VPN permission granted"
                }
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(connectButton)

        setContentView(layout)
    }
}
