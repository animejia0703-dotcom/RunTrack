package com.runtrack.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Hilo secundario: los callbacks de los sensores no corren en el hilo principal
    private var sensorThread: HandlerThread? = null
    private var listening = false

    // Filtros: cambio mínimo por eje y tiempo mínimo entre lecturas aceptadas
    private val accelFilter = SensorFilter(minDelta = 0.15f, minIntervalMs = 100L)
    private val gyroFilter = SensorFilter(minDelta = 0.05f, minIntervalMs = 100L)

    private var receivedEvents = 0
    private var processedEvents = 0

    private lateinit var tvStatus: TextView
    private lateinit var tvAccel: TextView
    private lateinit var tvGyro: TextView
    private lateinit var tvStats: TextView
    private lateinit var btnToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvAccel = findViewById(R.id.tvAccel)
        tvGyro = findViewById(R.id.tvGyro)
        tvStats = findViewById(R.id.tvStats)
        btnToggle = findViewById(R.id.btnToggle)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // 1) Verificar si el hardware existe (devuelve null si no está disponible)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        tvStatus.text = getString(
            R.string.status_format, describe(accelerometer), describe(gyroscope)
        )
        btnToggle.isEnabled = accelerometer != null || gyroscope != null
        btnToggle.setOnClickListener {
            if (listening) stopListening() else startListening()
        }
    }

    private fun describe(sensor: Sensor?): String =
        if (sensor == null) getString(R.string.unavailable)
        else getString(R.string.available, sensor.name)

    // 2) Habilitar los sensores y registrar el listener
    private fun startListening() {
        if (listening) return
        receivedEvents = 0
        processedEvents = 0
        accelFilter.reset()
        gyroFilter.reset()

        // Hilo secundario para que los callbacks no bloqueen la interfaz
        val thread = HandlerThread("SensorThread").also { it.start() }
        sensorThread = thread
        val handler = Handler(thread.looper)

        // SENSOR_DELAY_GAME: ~20 ms entre muestras (es una sugerencia al sistema)
        accelerometer?.let {
            sensorManager.registerListener(
                this, it, SensorManager.SENSOR_DELAY_GAME, handler
            )
        }
        gyroscope?.let {
            sensorManager.registerListener(
                this, it, SensorManager.SENSOR_DELAY_GAME, handler
            )
        }
        listening = true
        btnToggle.setText(R.string.btn_stop)
    }

    // 5) Liberar el hardware: sin esto el sensor sigue activo y gasta batería
    private fun stopListening() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        sensorThread?.quitSafely()
        sensorThread = null
        listening = false
        btnToggle.setText(R.string.btn_start)
    }

    override fun onResume() {
        super.onResume()
        startListening()
    }

    override fun onPause() {
        super.onPause()
        stopListening()
    }

    override fun onSensorChanged(event: SensorEvent) {
        receivedEvents++
        val now = SystemClock.elapsedRealtime()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER ->
                if (accelFilter.accept(event.values, now)) {
                    processedEvents++
                    show(tvAccel, event.values)
                }
            Sensor.TYPE_GYROSCOPE ->
                if (gyroFilter.accept(event.values, now)) {
                    processedEvents++
                    show(tvGyro, event.values)
                }
        }
    }

    // 4) Solo las lecturas aceptadas llegan a la interfaz
    private fun show(target: TextView, v: FloatArray) {
        val text = String.format(
            Locale.US, "X: %.2f   Y: %.2f   Z: %.2f", v[0], v[1], v[2]
        )
        val discarded = receivedEvents - processedEvents
        val pct = if (receivedEvents > 0) discarded * 100f / receivedEvents else 0f
        val stats = getString(
            R.string.stats_format, receivedEvents, processedEvents, discarded, pct
        )
        runOnUiThread {
            target.text = text
            tvStats.text = stats
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No se requiere manejo especial de precisión en este prototipo
    }
}
