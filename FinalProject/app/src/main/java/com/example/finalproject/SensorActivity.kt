package com.example.finalproject

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import kotlin.math.abs
import kotlin.math.sin


// Whole class was automatically converted from the provided starter code for the Sensor lab
// to Kotlin by Android Studio. This class is what I submitted for the Sensor Lab, except I added
// the quit button.
class SensorActivity : AppCompatActivity(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private var mSensorAccelerometer: Sensor? = null
    private var mSensorMagnetometer: Sensor? = null
    private var mAccelerometerData = FloatArray(3)
    private var mMagnetometerData = FloatArray(3)
    private var verticalBubble: ImageView? = null
    private var horizontalBubble: ImageView? = null
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        backButton = findViewById<Button>(R.id.backButton) as Button
        backButton.setOnClickListener {backButtonCallback()}
        verticalBubble = findViewById<View>(R.id.verticalBubble) as ImageView
        horizontalBubble = findViewById<View>(R.id.horizontalBubble) as ImageView
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mSensorAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorMagnetometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onStart() {
        super.onStart()
        if (mSensorAccelerometer != null) {
            mSensorManager!!.registerListener(
                this, mSensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        if (mSensorMagnetometer != null) {
            mSensorManager!!.registerListener(
                this, mSensorMagnetometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onStop() {
        super.onStop()
        mSensorManager!!.unregisterListener(this)
    }

    private fun randiansToPercent(radians: Float, bottom: Boolean): Float {
        if (bottom) {
            return ((sin(radians) * -1.0 + 1.0) / 2.0).toFloat()
        } else {
            return ((sin(radians) + 1.0) / 2.0).toFloat()
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val sensorType = sensorEvent.sensor.type
        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> mAccelerometerData = sensorEvent.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> mMagnetometerData = sensorEvent.values.clone()
            else -> return
        }
        val rotationMatrix = FloatArray(9)
        val rotationOK = SensorManager.getRotationMatrix(
            rotationMatrix, null,
            mAccelerometerData, mMagnetometerData
        )
        val orientationValues = FloatArray(3)
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrix, orientationValues)
        }

//        var azimuth = orientationValues[0];
        var pitch = orientationValues[1]
        var roll = orientationValues[2]
        if (abs(pitch) < VALUE_DRIFT) {
            pitch = 0f
        }
        if (abs(roll) < VALUE_DRIFT) {
            roll = 0f
        }
        val verticalFraction = randiansToPercent(pitch, false)
        val horizontalFraction = randiansToPercent(roll, true)
        val verticalPercent = (verticalFraction * 100.0).toInt()
        val horizontalPercent = (horizontalFraction * 100.0).toInt()
        horizontalBubble!!.contentDescription = "A bubble in a horizontal bar, " +
                verticalPercent + " percent of the way down."
        verticalBubble!!.contentDescription = "A bubble in a vertical bar, " +
                horizontalPercent + " percent of the way down."
        // Following two lines I got from Ashley's answer  to this stack overflow post:
        // https://stackoverflow.com/questions/45920205/how-to-modify-a-constraint-layout-programmatically
        horizontalBubble!!.updateLayoutParams<ConstraintLayout.LayoutParams> { horizontalBias = horizontalFraction }
        verticalBubble!!.updateLayoutParams<ConstraintLayout.LayoutParams> { verticalBias = verticalFraction }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
//            intentionally blank
    }

    companion object {
        private const val VALUE_DRIFT = 0.05f
    }

    private fun backButtonCallback() {
        val myIntent = Intent(this, MainActivity::class.java)
        startActivity(myIntent)
    }
}