package com.example.shealthprobe

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var urlEdit: EditText
    private lateinit var tokenEdit: EditText
    private lateinit var autoCheck: CheckBox
    private lateinit var timeButton: Button
    private lateinit var statusText: TextView
    private var hour = 21
    private var minute = 30

    private val reader by lazy { HealthReader(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlEdit = findViewById(R.id.urlEdit)
        tokenEdit = findViewById(R.id.tokenEdit)
        autoCheck = findViewById(R.id.autoCheck)
        timeButton = findViewById(R.id.timeButton)
        statusText = findViewById(R.id.statusText)

        loadPrefs()

        timeButton.setOnClickListener { chooseTime() }
        findViewById<Button>(R.id.saveButton).setOnClickListener { saveConfig() }
        findViewById<Button>(R.id.permissionButton).setOnClickListener { requestPermissions() }
        findViewById<Button>(R.id.previewButton).setOnClickListener { previewData() }
        findViewById<Button>(R.id.syncButton).setOnClickListener {
            saveConfig(showToast = false)
            Scheduler.runNow(this)
            toast("Sync queued. Refresh status in a few seconds.")
        }
        findViewById<Button>(R.id.refreshButton).setOnClickListener { refreshStatus() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun loadPrefs() {
        urlEdit.setText(AppPrefs.url(this))
        tokenEdit.setText(AppPrefs.token(this))
        autoCheck.isChecked = AppPrefs.autoEnabled(this)
        hour = AppPrefs.hour(this)
        minute = AppPrefs.minute(this)
        updateTimeButton()
        refreshStatus()
    }

    private fun chooseTime() {
        TimePickerDialog(this, { _, h, m ->
            hour = h
            minute = m
            updateTimeButton()
        }, hour, minute, true).show()
    }

    private fun updateTimeButton() {
        timeButton.text = String.format("Daily time: %02d:%02d", hour, minute)
    }

    private fun saveConfig(showToast: Boolean = true) {
        AppPrefs.saveConfig(
            this,
            urlEdit.text.toString(),
            tokenEdit.text.toString(),
            autoCheck.isChecked,
            hour,
            minute
        )
        if (autoCheck.isChecked) Scheduler.scheduleNext(this) else Scheduler.cancel(this)
        if (showToast) toast("Saved")
    }

    private fun requestPermissions() {
        lifecycleScope.launch {
            try {
                val granted = reader.store.requestPermissions(reader.requiredPermissions, this@MainActivity)
                val ok = granted.containsAll(reader.requiredPermissions)
                statusText.text = if (ok) {
                    "Samsung Health permissions granted."
                } else {
                    "Some Samsung Health permissions were not granted. You can still sync fields whose permissions are available, but this version currently requires all configured permissions for scheduled sync."
                }
            } catch (e: Throwable) {
                statusText.text = "Permission error: ${e::class.java.simpleName}: ${e.message}"
            }
        }
    }

    private fun previewData() {
        lifecycleScope.launch {
            statusText.text = "Reading Samsung Health..."
            try {
                val granted = reader.store.getGrantedPermissions(reader.requiredPermissions)
                if (!granted.containsAll(reader.requiredPermissions)) {
                    statusText.text = "Permissions missing. Tap 'Grant Samsung Health permissions' first."
                    return@launch
                }
                val payload = reader.readTodayPayload()
                statusText.text = GsonBuilder().setPrettyPrinting().create().toJson(payload)
            } catch (e: Throwable) {
                statusText.text = "Read error: ${e::class.java.simpleName}: ${e.message}"
            }
        }
    }

    private fun refreshStatus() {
        statusText.text = AppPrefs.lastStatus(this)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
