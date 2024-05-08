package com.example.locationapp.data

/*data class SensorResult(
    val temperature: Float,
    val humidity: Float,
    val connectionSate: ConnectionState
)*/

/*data class SensorResult(
    val accData: AccData,
    val connectionSate: ConnectionState
)*/

data class SensorResult(
    val byteData: ByteArray,
    val connectionState: ConnectionState
)
