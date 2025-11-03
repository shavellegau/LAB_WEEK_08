package com.example.lab_week_08

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker

class MainActivity : AppCompatActivity() {

    // ⚙️ WorkManager instance — manages your background tasks
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

        // Initialize WorkManager
        workManager = WorkManager.getInstance(this)

        // Define constraints — workers need an internet connection
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        // --- Create first work request ---
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // --- Create second work request ---
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // Chain the work: First → Second
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // Observe output of first worker
        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info.state.isFinished) {
                showResult("First process is done")
            }
        }

        // Observe output of second worker
        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info.state.isFinished) {
                showResult("Second process is done")
            }
        }
    }

    // ✅ Build Data input for worker
    private fun getIdInputData(idKey: String, idValue: String): Data =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    // ✅ Show result as Toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
