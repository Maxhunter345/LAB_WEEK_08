package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "channel_02"
        const val NOTIFICATION_ID = 2
        val trackingCompletion = MutableLiveData<String>()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(MainActivity.EXTRA_ID) ?: ""
        showNotification(id)
        return START_STICKY
    }

    private fun showNotification(id: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Second Notification Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Pastikan Anda punya icon ini
            .setContentTitle("Second Service")
            .setContentText("Proses akhir dengan ID: $id telah selesai.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Simulasikan pekerjaan lalu beritahu bahwa service selesai
        Thread {
            try {
                Thread.sleep(2000) // Simulasi kerja 2 detik
                trackingCompletion.postValue(CHANNEL_ID)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: InterruptedException) {
                Log.e("SecondNotification", "Service interrupted", e)
            }
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
