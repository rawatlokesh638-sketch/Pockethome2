package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
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
                    var pinChecked by remember { mutableStateOf(false) }
                    var pinSet by remember { mutableStateOf<String?>(null) }
                    var isAuthenticated by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        pinSet = securityManager.getPin()
                        pinChecked = true
                    }

                    if (!pinChecked) {
                        // Loading state while checking PIN
                        Box(modifier = Modifier.fillMaxSize())
                    } else if (pinSet == null && !isAuthenticated) {
                        com.example.ui.screens.PinScreen(
                            mode = com.example.ui.screens.PinMode.SETUP,
                            onPinSuccess = { pin ->
                                lifecycleScope.launch {
                                    securityManager.setPin(pin)
                                    pinSet = pin
                                    isAuthenticated = true
                                }
                            },
                            onSkipSetup = {
                                isAuthenticated = true
                            }
                        )
                    } else if (!isAuthenticated) {
                        com.example.ui.screens.PinScreen(
                            mode = com.example.ui.screens.PinMode.UNLOCK,
                            storedPin = pinSet,
                            onPinSuccess = {
                                isAuthenticated = true
                            },
                            onResetPin = {
                                lifecycleScope.launch {
                                    securityManager.clearPin()
                                    pinSet = null
                                    isAuthenticated = false
                                }
                            }
                        )
                    } else {
                        DashboardScreen(viewModel = dashboardViewModel)
                    }
                }
            }
        }
    }
}
