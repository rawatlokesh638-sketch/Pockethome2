package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.RoyalBlue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class PinMode {
    SETUP,
    UNLOCK
}

@Composable
fun PinScreen(
    mode: PinMode,
    storedPin: String? = null,
    onPinSuccess: (String) -> Unit,
    onSkipSetup: (() -> Unit)? = null,
    onResetPin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var setupStep by remember { mutableStateOf(1) } // 1: Enter new PIN, 2: Confirm new PIN
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShakeAndError(msg: String) {
        errorMessage = msg
        scope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    0f at 400
                }
            )
            currentInput = ""
        }
    }

    fun onKeyPressed(digit: String) {
        if (currentInput.length >= 4) return
        errorMessage = null
        val next = currentInput + digit
        currentInput = next

        if (next.length == 4) {
            when (mode) {
                PinMode.SETUP -> {
                    if (setupStep == 1) {
                        firstPin = next
                        currentInput = ""
                        setupStep = 2
                    } else {
                        // Confirm step
                        if (next == firstPin) {
                            onPinSuccess(next)
                        } else {
                            triggerShakeAndError("PINs do not match. Start over.")
                            setupStep = 1
                            firstPin = ""
                        }
                    }
                }
                PinMode.UNLOCK -> {
                    if (storedPin != null && next == storedPin) {
                        onPinSuccess(next)
                    } else {
                        triggerShakeAndError("Incorrect PIN! Please try again.")
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (currentInput.isNotEmpty()) {
            errorMessage = null
            currentInput = currentInput.dropLast(1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("pin_screen_root"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Skip / Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode == PinMode.SETUP && onSkipSetup != null) {
                    TextButton(
                        onClick = onSkipSetup,
                        modifier = Modifier.testTag("skip_pin_setup_button")
                    ) {
                        Text(
                            text = "Skip for Now",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(RoyalBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mode == PinMode.SETUP) Icons.Default.Security else Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = RoyalBlue,
                        modifier = Modifier.size(34.dp)
                    )
                }

                val titleText = when {
                    mode == PinMode.SETUP && setupStep == 1 -> "Create App PIN"
                    mode == PinMode.SETUP && setupStep == 2 -> "Confirm App PIN"
                    else -> "Pockethome Locked"
                }

                val subtitleText = when {
                    mode == PinMode.SETUP && setupStep == 1 -> "Set a 4-digit security PIN to protect your personal ledger & bills."
                    mode == PinMode.SETUP && setupStep == 2 -> "Re-enter the same 4 digits to confirm."
                    else -> "Enter your 4-digit PIN to access your finances."
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PIN Dots indicator with shake effect
                Row(
                    modifier = Modifier
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < currentInput.length
                        val dotColor = when {
                            errorMessage != null -> ExpenseRed
                            isFilled -> RoyalBlue
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled || errorMessage != null) dotColor else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = dotColor,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Error Message
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Numeric Keypad
            NumericKeypad(
                onDigitClick = { onKeyPressed(it) },
                onBackspaceClick = { onBackspace() },
                bottomLeftText = if (mode == PinMode.UNLOCK && onResetPin != null) "Forgot?" else null,
                onBottomLeftClick = {
                    if (mode == PinMode.UNLOCK && onResetPin != null) {
                        showResetDialog = true
                    }
                }
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset App PIN?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Do you want to reset your app security PIN? You will be prompted to create a new PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetPin?.invoke()
                    }
                ) {
                    Text("Reset PIN", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    bottomLeftText: String? = null,
    onBottomLeftClick: (() -> Unit)? = null
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        text = digit,
                        onClick = { onDigitClick(digit) }
                    )
                }
            }
        }

        // Bottom Row: [Optional action / Forgot] [ 0 ] [ Backspace ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bottom-Left key
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable(enabled = bottomLeftText != null) { onBottomLeftClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                if (bottomLeftText != null) {
                    Text(
                        text = bottomLeftText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue
                    )
                }
            }

            // '0' Key
            KeypadButton(
                text = "0",
                onClick = { onDigitClick("0") }
            )

            // Backspace Key
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable { onBackspaceClick() }
                    .testTag("keypad_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .testTag("keypad_digit_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
