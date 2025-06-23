package com.example.workerdemo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        findViewById<Button>(R.id.startWorkerButton).setOnClickListener {
            lifecycleScope.launch {
                DummyWorker.enqueue(this@MainActivity)
            }
        }

        findViewById<Button>(R.id.cancelWorkButton).setOnClickListener {
            lifecycleScope.launch {
                DummyWorker.cancelAll(this@MainActivity)
            }
        }

        DummyWorker.createChannels(this)
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(POST_NOTIFICATIONS), 0)
            }
        }
    }
}
