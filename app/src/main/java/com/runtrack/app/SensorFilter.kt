package com.runtrack.app
import kotlin.math.abs
class SensorFilter(private val minDelta: Float, private val minIntervalMs: Long) {
    private var lastValues: FloatArray? = null
    private var lastTimeMs = 0L

    fun accept(values: FloatArray, nowMs: Long): Boolean {
        val previous = lastValues
        if (previous != null) {
            // Control de frecuencia: descarta lecturas demasiado seguidas
            if (nowMs - lastTimeMs < minIntervalMs) return false
            // Control de duplicados: descarta lecturas casi iguales a la anterior
            val changed = (0..2).any { abs(values[it] - previous[it]) >= minDelta }
            if (!changed) return false
        }
        lastValues = values.copyOf(3)
        lastTimeMs = nowMs
        return true
    }

    fun reset() {
        lastValues = null
        lastTimeMs = 0L
    }
}
