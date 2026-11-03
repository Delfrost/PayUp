package com.payup.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.payup.app.ui.navigation.PayUpNavHost
import com.payup.app.ui.theme.PayUpTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayUpTheme {
                val navController = rememberNavController()
                PayUpNavHost(navController = navController)
            }
        }
    }
}
