package com.gpstobt.nmeabridge

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggle: Button
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var tvSatellites: TextView
    private lateinit var tvPosition: TextView
    private lateinit var tvUtcTime: TextView
    private lateinit var tvSentenceCount: TextView
    private lateinit var tvNmeaLog: TextView
    private lateinit var svNmeaLog: ScrollView

    private var serviceRunning = false
    private val nmeaLogLines = mutableListOf<String>()
    private val maxLogLines = 50

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            updateUi(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btnToggle)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        tvSatellites = findViewById(R.id.tvSatellites)
        tvPosition = findViewById(R.id.tvPosition)
        tvUtcTime = findViewById(R.id.tvUtcTime)
        tvSentenceCount = findViewById(R.id.tvSentenceCount)
        tvNmeaLog = findViewById(R.id.tvNmeaLog)
        svNmeaLog = findViewById(R.id.svNmeaLog)

        btnToggle.setOnClickListener {
            if (!serviceRunning) {
                startBridge()
            } else {
                stopBridge()
            }
        }

        findViewById<TextView>(R.id.tvSupportLink).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/avantol/AccuTime/releases/latest")))
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver,
            IntentFilter(NmeaBluetoothService.ACTION_STATUS_UPDATE)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver)
    }

    private fun startBridge() {
        // Check permissions
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestPermissions(this)
            return
        }

        // Check Bluetooth enabled
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = btManager.adapter
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not available on this device", Toast.LENGTH_LONG).show()
            return
        }
        if (!btAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_LONG).show()
            return
        }

        // Start service
        val intent = Intent(this, NmeaBluetoothService::class.java)
        ContextCompat.startForegroundService(this, intent)
        serviceRunning = true
        btnToggle.text = getString(R.string.btn_stop)
        styleStopButton()

        nmeaLogLines.clear()
        tvNmeaLog.text = ""
    }

    private fun stopBridge() {
        val intent = Intent(this, NmeaBluetoothService::class.java)
        stopService(intent)
        serviceRunning = false
        btnToggle.text = getString(R.string.btn_start)
        styleStartButton()

        tvBluetoothStatus.text = getString(R.string.status_disconnected)
        tvBluetoothStatus.setTextColor(getColor(R.color.status_disconnected))
    }

    private fun styleStartButton() {
        btnToggle.layoutParams = (btnToggle.layoutParams as LinearLayout.LayoutParams).apply {
            height = (64 * resources.displayMetrics.density).toInt()
        }
        btnToggle.textSize = 22f
        btnToggle.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
        btnToggle.setTextColor(0xFFFFFFFF.toInt())
    }

    private fun styleStopButton() {
        btnToggle.layoutParams = (btnToggle.layoutParams as LinearLayout.LayoutParams).apply {
            height = (40 * resources.displayMetrics.density).toInt()
        }
        btnToggle.textSize = 14f
        btnToggle.backgroundTintList = ColorStateList.valueOf(0xFF666666.toInt())
        btnToggle.setTextColor(0xFFCCCCCC.toInt())
    }

    private fun updateUi(intent: Intent) {
        // Bluetooth status
        val btStatus = intent.getIntExtra(NmeaBluetoothService.EXTRA_BT_STATUS, 0)
        when (btStatus) {
            NmeaBluetoothService.BT_DISCONNECTED -> {
                tvBluetoothStatus.text = getString(R.string.status_disconnected)
                tvBluetoothStatus.setTextColor(getColor(R.color.status_disconnected))
            }
            NmeaBluetoothService.BT_WAITING -> {
                tvBluetoothStatus.text = getString(R.string.status_waiting)
                tvBluetoothStatus.setTextColor(getColor(R.color.status_waiting))
            }
            NmeaBluetoothService.BT_CONNECTED -> {
                val deviceName = intent.getStringExtra(NmeaBluetoothService.EXTRA_DEVICE_NAME) ?: ""
                tvBluetoothStatus.text = getString(R.string.status_connected) +
                    if (deviceName.isNotEmpty()) " ($deviceName)" else ""
                tvBluetoothStatus.setTextColor(getColor(R.color.status_connected))
            }
        }

        // GPS status
        val gpsStatus = intent.getIntExtra(NmeaBluetoothService.EXTRA_GPS_STATUS, 0)
        when (gpsStatus) {
            NmeaBluetoothService.GPS_NO_FIX -> {
                tvGpsStatus.text = getString(R.string.gps_no_fix)
                tvGpsStatus.setTextColor(getColor(R.color.gps_no_fix))
            }
            NmeaBluetoothService.GPS_FIX_2D -> {
                tvGpsStatus.text = getString(R.string.gps_fix_2d)
                tvGpsStatus.setTextColor(getColor(R.color.gps_fix))
            }
            NmeaBluetoothService.GPS_FIX_3D -> {
                tvGpsStatus.text = getString(R.string.gps_fix_3d)
                tvGpsStatus.setTextColor(getColor(R.color.gps_fix))
            }
        }

        // Satellites
        val satsUsed = intent.getIntExtra(NmeaBluetoothService.EXTRA_SATELLITES_USED, 0)
        val satsView = intent.getIntExtra(NmeaBluetoothService.EXTRA_SATELLITES_VIEW, 0)
        tvSatellites.text = "Satellites: $satsUsed in use / $satsView in view"

        // Position
        val lat = intent.getDoubleExtra(NmeaBluetoothService.EXTRA_LATITUDE, 0.0)
        val lon = intent.getDoubleExtra(NmeaBluetoothService.EXTRA_LONGITUDE, 0.0)
        if (lat != 0.0 || lon != 0.0) {
            tvPosition.text = "Position: %.6f, %.6f".format(lat, lon)
        }

        // UTC Time
        val utcTime = intent.getStringExtra(NmeaBluetoothService.EXTRA_UTC_TIME) ?: ""
        if (utcTime.isNotEmpty()) {
            tvUtcTime.text = "UTC Time: $utcTime"
        }

        // Sentence count
        val count = intent.getLongExtra(NmeaBluetoothService.EXTRA_SENTENCE_COUNT, -1)
        if (count >= 0) {
            tvSentenceCount.text = "Sentences sent: $count"
        }

        // NMEA log
        val sentence = intent.getStringExtra(NmeaBluetoothService.EXTRA_NMEA_SENTENCE)
        if (sentence != null) {
            nmeaLogLines.add(sentence.trim())
            while (nmeaLogLines.size > maxLogLines) {
                nmeaLogLines.removeAt(0)
            }
            tvNmeaLog.text = nmeaLogLines.joinToString("\n")
            svNmeaLog.post { svNmeaLog.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE) {
            if (PermissionHelper.allGranted(grantResults)) {
                startBridge()
            } else {
                Toast.makeText(
                    this,
                    "All permissions are required for GPS and Bluetooth operation",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
