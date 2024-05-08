package com.example.locationapp.Utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.example.locationapp.data.LocationData
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object LocationUtils {

    private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private fun isGpsEnabled(activity: Activity): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // Prompt user to enable GPS
    private fun promptGpsActivation(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.apply {
            setTitle("Enable GPS")
            setMessage("GPS is required for location services. Do you want to enable it now?")
            setPositiveButton("Yes") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private var isLoggingEnabled = false
    private var locationDataList = mutableListOf<LocationData>()
    fun startLocationUpdates(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else if (!isGpsEnabled(activity)) {
            promptGpsActivation(activity)
        } else {

            var locationManager: LocationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationGPS: Location? =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (!isLoggingEnabled) {
                isLoggingEnabled = true
                startLocationLogging(locationGPS)
            } else {
                isLoggingEnabled = false
                stopLocationLogging()
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private lateinit var locationJob: Job

    private fun startLocationLogging(locationGPS: Location?) {
        locationJob = coroutineScope.launch {
            if (locationGPS != null) {
                while (isLoggingEnabled) {
                    val lat: Double = locationGPS.latitude
                    val longi: Double = locationGPS.longitude
                    val currentTimeMillis = System.currentTimeMillis()
                    val currentTime = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date(currentTimeMillis))
                    locationDataList.add(LocationData(lat, longi, currentTime))
                    delay(1_000)
                }
            }
        }

    }

    // 3. Stop Logging Button Click
    private fun stopLocationLogging() {
        locationJob.cancel()
        saveLocationDataToFile()
        locationDataList.clear()
    }

    private fun saveLocationDataToFile() {
        val csvHeader = "Latitude,Longitude,Time\n"
        val csvData = locationDataList.joinToString("\n"){"${it.latitude},${it.longitude},${it.time}"}
        val csvContent = csvHeader + csvData

        /*val gson = Gson()
        val jsonData = gson.toJson(locationDataList)*/
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val locationFolder = File(downloadsFolder, "Location Data Folder")

        if (!locationFolder.exists()) {
            locationFolder.mkdirs()
        }

        var lastFileNumber = 0
        locationFolder.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("location_data_")) {
                val fileNumber = file.name.substringAfter("location_data_").substringBefore(".csv").toIntOrNull()
                if (fileNumber != null && fileNumber > lastFileNumber) {
                    lastFileNumber = fileNumber
                }
            }
        }

        // Generate the next file number
        val nextFileNumber = lastFileNumber + 1

        val fileName = "location_data_$nextFileNumber.csv"
        //val file = File(downloadsFolder, "location_data.json")
        //file.writeText(jsonData
        try {
            val file = File(locationFolder, fileName)
            val fos = FileOutputStream(file)
            //fos.write(jsonData.toByteArray())
            fos.write(csvContent.toByteArray())
            fos.close()
            // Optionally, you can show a toast message or log the file path
            // Toast.makeText(context, "Location data saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle any exceptions that occur during file writing
        }
    }
}