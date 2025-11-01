package com.example.lab_week_08

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.observe
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    // Deklarasikan workManager di sini agar bisa diakses di seluruh class
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi WorkManager
        workManager = WorkManager.getInstance(this)

        // Meminta izin untuk notifikasi di Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // Mulai semua proses worker
        startWorkers()
    }

    private fun startWorkers() {
        val id = "001"

        // Membuat batasan (constraint) agar worker hanya berjalan saat ada koneksi internet
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1. Membuat request untuk FirstWorker
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // 2. Membuat request untuk SecondWorker
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // 3. Membuat request untuk ThirdWorker (dengan constraint)
        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        // Menjalankan worker secara berurutan: first -> second -> third
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest) // ThirdWorker dijalankan setelah SecondWorker selesai
            .enqueue()

        // Mengamati status dari setiap worker
        observeWorkers(firstRequest, secondRequest, thirdRequest)
    }

    private fun observeWorkers(
        firstRequest: OneTimeWorkRequest,
        secondRequest: OneTimeWorkRequest,
        thirdRequest: OneTimeWorkRequest
    ) {
        // Observer untuk FirstWorker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("First process is done")
                    // Setelah FirstWorker selesai, luncurkan notifikasi pertama
                    launchNotificationService()
                }
            }

        // Observer untuk SecondWorker
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Second process is done")
                }
            }

        // Observer untuk ThirdWorker
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Third process is done")
                    // Setelah ThirdWorker selesai, luncurkan notifikasi kedua
                    launchSecondNotificationService()
                }
            }

        // Observer untuk memantau status penyelesaian dari service notifikasi
        NotificationService.trackingCompletion.observe(this) { channelId ->
            showResult("First Notification (Channel ID $channelId) done!")
        }

        SecondNotificationService.trackingCompletion.observe(this) { channelId ->
            showResult("Final Notification (Channel ID $channelId) done!")
        }
    }

    private fun getIdInputData(idKey: String, idValue: String): Data =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun launchNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun launchSecondNotificationService() {
        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(EXTRA_ID, "002")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
