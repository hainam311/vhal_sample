package androidguy.car.app

import android.car.Car
import android.car.VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.CarPropertyManager.CarPropertyEventCallback
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // 1. Change to nullable to match the AOSP API signature
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var text: TextView
    private val MAX_UPDATE_RATE_HZ = 100f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        text = findViewById(R.id.idReady)
        initCarPropertyManager()
    }

    override fun onStop() {
        super.onStop()
        // 2. Safe disconnect check
        if (car?.isConnected == true) {
            car?.disconnect()
        }
    }

    private fun initCarPropertyManager() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) && car == null) {
            // 3. Assign to the nullable car variable
            car = Car.createCar(this)
            
            // 4. Safe cast and retrieval
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            
            if (carPropertyManager != null) {
                onCarServiceReady()
            } else {
                Log.e(TAG, "CarPropertyManager not available!")
            }
        }
    }

    private fun onCarServiceReady() {
        val manager = carPropertyManager ?: return

        if (manager.isPropertyAvailable(
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                VEHICLE_AREA_TYPE_GLOBAL
            )
        ) {
            manager.registerCallback(
                callback,
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                MAX_UPDATE_RATE_HZ
            )
        } else {
            Log.e(TAG, "Vehicle speed property not available!")
        }
    }

    private val callback: CarPropertyEventCallback = object : CarPropertyEventCallback {
        override fun onChangeEvent(p: CarPropertyValue<*>?) {
            // Updating UI on the main thread
            runOnUiThread {
                text.text = "Speed: ${p?.value} km/h"
            }
        }

        override fun onErrorEvent(p0: Int, p1: Int) {
            runOnUiThread {
                text.text = "ERROR: Unable to read speed"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 5. Safe unregistration
        carPropertyManager?.unregisterCallback(callback)
    }
}