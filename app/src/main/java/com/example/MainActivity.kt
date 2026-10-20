package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.SecurityManager
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.screens.PinScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        securityManager = SecurityManager(this)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var pinSet by remember { mutableStateOf<String?>(null) }
                    var isAuthenticated by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        pinSet = securityManager.getPin()
                    }

                    if (pinSet == null) {
                        PinScreen(isSettingPin = true) { pin ->
                            lifecycleScope.launch {
                                securityManager.setPin(pin)
                                pinSet = pin
                                isAuthenticated = true
                            }
                        }
                    } else if (!isAuthenticated) {
                        PinScreen(isSettingPin = false) { pin ->
                            lifecycleScope.launch {
                                if (securityManager.getPin() == pin) {
                                    isAuthenticated = true
                                }
                            }
                        }
                    } else {
                        DashboardScreen(viewModel = dashboardViewModel)
                    }
                }
            }
        }
    }
}
