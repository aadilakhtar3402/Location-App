package com.example.locationapp.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.locationapp.Utils.Resource
import com.example.locationapp.data.AccData
import com.example.locationapp.data.ConnectionState
import com.example.locationapp.data.SensorReceiveManager
import com.example.locationapp.data.SensorResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class SensorBLEReceiveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : SensorReceiveManager {

    private val DEVICE_NAME = "DTLG151"
    private val DEVICE_ID = "ED:2A:4E:FD:81:0E" //D"
    private val ACC_SERVICE_UIID = "00000000-0001-11e1-9ab4-0002a5d5c51b"
    private val ACC_CHARACTERISTICS_UUID = "00000011-0002-11e1-ac36-0002a5d5c51b"
    // private val TEMP_HUMIDITY_SERVICE_UIID = "00000000-0001-11e1-9ab4-0002a5d5c51b"
    // private val TEMP_HUMIDITY_CHARACTERISTICS_UUID = "00000011-0002-11e1-ac36-0002a5d5c51b"
    // private val HUMIDITY_CHARACTERISTICS_UUID = "0000000f-0002-11e1-ac36-0002a5d5c51b"
    //7ed9df4f-9b5a-4fe2-bbe1-5d439228e2bb

    override val data: MutableSharedFlow<Resource<SensorResult>> = MutableSharedFlow()

    //val scanFilter = ScanFilter.Builder().build()
    //val scanSettings = ScanSettings.Builder().build()

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var gatt: BluetoothGatt? = null

    private var isScanning = false

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // super.onScanResult(callbackType, result)   maybe you want to find all device
            // result.device.address
            if (result.device.address == DEVICE_ID) { // result.device.name == DEVICE_NAME) {
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Connecting to device..."))
                }
                if (isScanning) {
                    result.device.connectGatt(context, false, gattCallback)
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }

    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            // super.onConnectionStateChange(gatt, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Discovering Services..."))
                    }
                    gatt.discoverServices()
                    this@SensorBLEReceiveManager.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        data.emit(
                            Resource.Success(
                                data = SensorResult(
                                    byteArrayOf(),
                                    ConnectionState.Disconnected
                                )
                            )
                        )
                    }
                    gatt.close()
                }
            } else {
                gatt.close()
                currentConnectionAttempt += 1
                coroutineScope.launch {
                    data.emit(
                        Resource.Loading(
                            message = "Attempting to connect $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"
                        )
                    )
                }
                if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                    startReceiving()
                } else {
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not connect to ble device"))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            //super.onServicesDiscovered(gatt, status)
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(517)
            }
            /*super.onServicesDiscovered(gatt, status)
            gatt?.services?.forEach { service ->
                Log.d("Service UUID", service.uuid.toString())
                service.characteristics.forEach { characteristic ->
                    Log.d("Characteristic UUID", characteristic.uuid.toString())
                }
            }
            gatt?.disconnect()*/
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic =
                findCharacteristics(ACC_SERVICE_UIID, ACC_CHARACTERISTICS_UUID)
            if (characteristic == null) {
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Could not find temp and humidity publisher"))
                }
                return
            }
            enableNotification(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                when (uuid) {
                    UUID.fromString(ACC_CHARACTERISTICS_UUID) -> {
                        // xx xx xx xx xx
                        //val multiplicator = if (value.first().toInt() > 0) -1 else 1
                        //val temperature = value[1].toInt() + value[2].toInt() / 10f
                        //val humidity = value[4].toInt() + value[5].toInt() / 10f
                        /*val temperatureRawData = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1)
                        val humidityRawData = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4)
                        val temperature = calculateTemperature(temperatureRawData)
                        val humidity = calculateHumidity(humidityRawData)*/
                        // val acceleration = calculateAcceleration(characteristic.value)
                        Log.d("Sensor Data", characteristic.value.toString())
                        val tempHumidityResult = SensorResult(
                            characteristic.value,
                            ConnectionState.Connected
                        )
                        coroutineScope.launch {
                            data.emit(
                                Resource.Success(data = tempHumidityResult)
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }

        private fun calculateTemperature(rawTemperature: Int): Float {
            return String.format("%.2f",(rawTemperature / 65536.0 * 165 - 40)).toFloat()
        }

        private fun calculateHumidity(rawHumidity: Int): Float {
            return String.format("%.2f",(rawHumidity / 65536.0 * 100)).toFloat()
        }

        private fun calculateAcceleration(value: ByteArray): AccData {
            val x = value[0].toInt() or (value[1].toInt() shl 8)
            val y = value[2].toInt() or (value[3].toInt() shl 8)
            val z = value[4].toInt() or (value[5].toInt() shl 8)
            return AccData(
                String.format("%.2f",x).toFloat(),
                String.format("%.2f",y).toFloat(),
                String.format("%.2f",z).toFloat()
            )
        }

        /*override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            characteristic?.value
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {

        }*/

    }

    /*private fun example() {
        val characteristic = gatt?.getService(UUID.fromString("fasdfasd"))?.getCharacteristic(UUID.fromString("ddddddddd"))
        gatt?.readCharacteristic(characteristic)
    }*/

    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.d("BLEReceiveManager", "set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        gatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE Device!")
    }

    private fun findCharacteristics(serviceUUID: String, characteristicsUUID: String): BluetoothGattCharacteristic? {
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }
    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        gatt?.disconnect()
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Scanning Ble devices..."))
        }
        isScanning = true
        //bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    override fun stopReceiving() {
        // Disconnect from the Bluetooth device
        disconnect()
        // Reconnect to the Bluetooth device
        reconnect()
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic = findCharacteristics(ACC_SERVICE_UIID, ACC_CHARACTERISTICS_UUID)
        if (characteristic != null) {
            disconnectCharacteristic(characteristic)
        }
        gatt?.close()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.d("TempHumidReceiveManager", "set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }

}