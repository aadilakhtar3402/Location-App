package com.example.locationapp.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation(
    onBluetoothStateChanged:()->Unit
) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.StartScreen.route) {
        composable(Screen.StartScreen.route) {
            StartScreen(navController = navController)
        }

        composable(Screen.SensorScreen.route) {
            SensorScreen(
                onBluetoothStateChanged
            )
        }
    }

}

sealed class Screen(val route:String) {
    object StartScreen:Screen("start_screen")
    object SensorScreen:Screen("sensor_screen")
}