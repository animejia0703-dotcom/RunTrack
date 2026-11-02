# RunTrack – Gestión de sensores (AVS.03.03.01)

Prototipo Android (Kotlin) que lee acelerómetro y giroscopio, filtra eventos duplicados
y muestra los datos en tiempo real.

## Cómo ejecutarlo
1. Abrir la carpeta en Android Studio y esperar el *Gradle Sync*.
2. Ejecutar en un teléfono físico (recomendado) o en un emulador
   (Extended controls > Virtual sensors permite mover el dispositivo).
3. Mover el teléfono y observar los valores y los contadores de eventos descartados.

## Archivos principales
- `MainActivity.kt`: verificación de sensores, listener en hilo secundario, ciclo de vida y UI.
- `SensorFilter.kt`: filtro por umbral de cambio e intervalo mínimo.
