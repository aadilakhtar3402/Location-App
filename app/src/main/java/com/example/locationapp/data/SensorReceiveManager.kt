package com.example.locationapp.data

import com.example.locationapp.Utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface SensorReceiveManager {

    val data: MutableSharedFlow<Resource<SensorResult>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun stopReceiving()

    fun closeConnection()

}