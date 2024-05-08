package com.example.locationapp.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locationapp.Utils.Resource
import com.example.locationapp.data.AccData
import com.example.locationapp.data.ConnectionState
import com.example.locationapp.data.SensorReceiveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager
) : ViewModel(){

    var initializingMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var temperature by mutableStateOf(0f)
        private set

    var humidity by mutableStateOf(0f)
        private set

    var acceleration by mutableStateOf(AccData(0f, 0f, 0f))
        private set

    var byteData by mutableStateOf(byteArrayOf())
        private set

    var isLoggingStarted by mutableStateOf(false)
        private set

    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)

    private fun subscribeToChanges() {
        viewModelScope.launch {
            sensorReceiveManager.data.collect{ result ->
                when(result) {
                    is Resource.Success -> {
                        connectionState = result.data.connectionState
                        byteData = result.data.byteData
                        //acceleration = result.data.accData
                        //temperature = result.data.temperature
                        //humidity = result.data.humidity
                    }

                    is Resource.Loading -> {
                        initializingMessage = result.message
                        connectionState = ConnectionState.CurrentlyInitializing
                    }

                    is Resource.Error -> {
                        errorMessage = result.errorMessage
                        connectionState = ConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    fun disconnect(){
        sensorReceiveManager.disconnect()
    }

    fun reconnect() {
        sensorReceiveManager.reconnect()
    }

    fun initializeConnection() {
        errorMessage = null
        subscribeToChanges()
        sensorReceiveManager.startReceiving()
    }

    override fun onCleared() {
        super.onCleared()
        sensorReceiveManager.closeConnection()
    }

    fun startLogging() {
        isLoggingStarted = true
    }

    fun stopLogging() {
        isLoggingStarted = false
    }

    fun startReceivingSensorData() {
        sensorReceiveManager.startReceiving()
    }

    fun stopReceivingSensorData() {
        sensorReceiveManager.stopReceiving()
    }

}