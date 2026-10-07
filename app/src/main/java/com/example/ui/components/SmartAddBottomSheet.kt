package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Bill
import com.example.model.Transaction
import com.example.service.ParsedFinancialResult
import com.example.service.SmartVoiceAndReceiptParser
import com.example.ui.DashboardViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddBottomSheet(
    userId: String,
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit,
    onSaveBill: (Bill) -> Unit,
    onOpenManualExpense: () -> Unit,
    onOpenManualIncome: () -> Unit,
    onOpenManualBill: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var activeMode by remember { mutableStateOf("VOICE") } // VOICE, CAMERA, DOC, MANUAL
    var voiceInputText by remember { mutableStateOf("") }
    var documentInputText by remember { mutableStateOf("") }
    var detectedResult by remember { mutableStateOf<ParsedFinancialResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                voiceInputText = spokenText
                detectedResult = SmartVoiceAndReceiptParser.parseSmartInput(spokenText, userId)
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isProcessing = true
            detectedResult = SmartVoiceAndReceiptParser.parseReceiptSimulation("bill receipt store photo", userId)
            isProcessing = false
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            detectedResult = SmartVoiceAndReceiptParser.parseReceiptSimulation("supermarket receipt doc", userId)
            isProcessing = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("smart_add_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(RoyalBlue, BrandOrange))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Universal Add 🪄",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Photo kheencho, Bolke ya Doc se turant add karein",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Input Mode Tabs (Voice, Photo/Camera, Document/Text, Manual)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmartModeTab(
                    label = "🎙️ Bolke",
                    selected = activeMode == "VOICE",
                    onClick = { activeMode = "VOICE" },
                    modifier = Modifier.weight(1f)
                )
                SmartModeTab(
                    label = "📸 Photo",
                    selected = activeMode == "CAMERA",
                    onClick = { activeMode = "CAMERA" },
                    modifier = Modifier.weight(1f)
                )
                SmartModeTab(
                    label = "📄 Doc/Text",
                    selected = activeMode == "DOC",
                    onClick = { activeMode = "DOC" },
                    modifier = Modifier.weight(1f)
                )
                SmartModeTab(
                    label = "⚡ Manual",
                    selected = activeMode == "MANUAL",
                    onClick = { activeMode = "MANUAL" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Content
            when (activeMode) {
                "VOICE" -> {
                    VoiceModeSection(
                        voiceText = voiceInputText,
                        onVoiceTextChange = {
                            voiceInputText = it
                            if (it.isNotBlank()) {
                                detectedResult = SmartVoiceAndReceiptParser.parseSmartInput(it, userId)
                            }
                        },
                        onStartMic = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye: e.g. Dmart se ₹1200 ka rashan kharida")
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                // Fallback suggestion
                            }
                        },
                        onSelectSuggestion = { suggestion ->
                            voiceInputText = suggestion
                            detectedResult = SmartVoiceAndReceiptParser.parseSmartInput(suggestion, userId)
                        }
                    )
                }

                "CAMERA" -> {
                    CameraPhotoSection(
                        onTakePhoto = { cameraLauncher.launch(null) },
                        onPickGallery = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onTestSampleBill = {
                            detectedResult = SmartVoiceAndReceiptParser.parseReceiptSimulation("Dmart Groceries Receipt", userId)
                        }
                    )
                }

                "DOC" -> {
                    DocumentTextSection(
                        docText = documentInputText,
                        onDocTextChange = {
                            documentInputText = it
                            if (it.isNotBlank()) {
                                detectedResult = SmartVoiceAndReceiptParser.parseSmartInput(it, userId)
                            }
                        },
                        onPasteSample = { sample ->
                            documentInputText = sample
                            detectedResult = SmartVoiceAndReceiptParser.parseSmartInput(sample, userId)
                        }
                    )
                }

                "MANUAL" -> {
                    ManualQuickSection(
                        onOpenExpense = {
                            onDismiss()
                            onOpenManualExpense()
                        },
                        onOpenIncome = {
                            onDismiss()
                            onOpenManualIncome()
                        },
                        onOpenBill = {
                            onDismiss()
                            onOpenManualBill()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detected Financial Entry Card Preview & Save Button
            AnimatedVisibility(visible = detectedResult != null) {
                detectedResult?.let { result ->
                    DetectedResultPreviewCard(
                        result = result,
                        onConfirmSave = {
                            when (result) {
                                is ParsedFinancialResult.ExpenseResult -> onSaveTransaction(result.transaction)
                                is ParsedFinancialResult.IncomeResult -> onSaveTransaction(result.transaction)
                                is ParsedFinancialResult.BillResult -> onSaveBill(result.bill)
                            }
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SmartModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RoyalBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun VoiceModeSection(
    voiceText: String,
    onVoiceTextChange: (String) -> Unit,
    onStartMic: () -> Unit,
    onSelectSuggestion: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.07f)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RoyalBlue.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(RoyalBlue, PurpleAccent)))
                        .clickable(onClick = onStartMic),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tap to Speak (Boliye)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalBlue
                )
                Text(
                    text = "Hindi ya English me bole: \"Dmart se ₹1200 ka rashan kharida\"",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = voiceText,
            onValueChange = onVoiceTextChange,
            label = { Text("Spoken or Typed Text") },
            placeholder = { Text("e.g. ₹500 petrol Indian Oil via PhonePe") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = RoyalBlue) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Try these Voice Presets (1-Tap):",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            VoiceSuggestionChip("🛒 \"Dmart se ₹1,200 ka rashan kharida via UPI\"", onClick = { onSelectSuggestion("Dmart se ₹1200 ka rashan kharida via UPI") })
            VoiceSuggestionChip("⛽ \"₹500 petrol Indian Oil pe PhonePe se diya\"", onClick = { onSelectSuggestion("₹500 petrol Indian Oil pe PhonePe se diya") })
            VoiceSuggestionChip("💰 \"Office se ₹45,000 salary account me aayi\"", onClick = { onSelectSuggestion("Office se ₹45000 salary account me aayi") })
            VoiceSuggestionChip("⚡ \"Bijli ka bill ₹1,850 due on 25th\"", onClick = { onSelectSuggestion("Bijli ka bill ₹1850 due on 25th") })
        }
    }
}

@Composable
private fun VoiceSuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CameraPhotoSection(
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onTestSampleBill: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📸 Bill & Receipt Scanner",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dukan, Grocery ya Mall ke receipt ki photo khinchein. Amount aur store name auto-extract ho jayega.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTakePhoto,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera")
                    }

                    OutlinedButton(
                        onClick = onPickGallery,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onTestSampleBill,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sample Bill Scan Karein (DMart ₹1,240)")
        }
    }
}

@Composable
private fun DocumentTextSection(
    docText: String,
    onDocTextChange: (String) -> Unit,
    onPasteSample: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = docText,
            onValueChange = onDocTextChange,
            label = { Text("Paste Invoice / Bill / SMS Text") },
            placeholder = { Text("Paste WhatsApp receipt, SMS alert or invoice details here...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { onPasteSample("Swiggy Order #9284 Total: Rs 480 Paid via PhonePe on 06-Oct") },
                label = { Text("Paste Swiggy Bill", fontSize = 11.sp) }
            )
            SuggestionChip(
                onClick = { onPasteSample("Electricity Bill BSES Account #98213 Due Date 22-Oct Amount: Rs 1750") },
                label = { Text("Paste Bijli Bill", fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun ManualQuickSection(
    onOpenExpense: () -> Unit,
    onOpenIncome: () -> Unit,
    onOpenBill: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickManualCard(
            title = "Add Expense (Kharcha)",
            subtitle = "Groceries, Food, Shopping, Transport etc.",
            icon = Icons.Default.Add,
            color = MoneyGreen,
            onClick = onOpenExpense
        )
        QuickManualCard(
            title = "Add Income (Aay)",
            subtitle = "Salary, Business kamai, Rental income",
            icon = Icons.Default.Description,
            color = BalanceBlue,
            onClick = onOpenIncome
        )
        QuickManualCard(
            title = "Add Upcoming Bill Reminder",
            subtitle = "Bijli, Wi-Fi, Gas cylinder, House rent due date",
            icon = Icons.Default.Receipt,
            color = PurpleAccent,
            onClick = onOpenBill
        )
    }
}

@Composable
private fun QuickManualCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetectedResultPreviewCard(
    result: ParsedFinancialResult,
    onConfirmSave: () -> Unit
) {
    val (title, amount, category, typeLabel, color) = when (result) {
        is ParsedFinancialResult.ExpenseResult -> Tuple5(
            result.transaction.title,
            result.transaction.amount,
            result.transaction.category,
            "💸 Expense (Kharcha)",
            MoneyGreen
        )
        is ParsedFinancialResult.IncomeResult -> Tuple5(
            result.transaction.title,
            result.transaction.amount,
            result.transaction.category,
            "💰 Income (Aay)",
            BalanceBlue
        )
        is ParsedFinancialResult.BillResult -> Tuple5(
            result.bill.title,
            result.bill.amount,
            result.bill.category,
            "🧾 Upcoming Bill (Due: ${result.bill.dueDate})",
            BrandOrange
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ AI Detected Details:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Category: $category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = DashboardViewModel.formatCurrency(amount),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onConfirmSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Confirm & Add to Hisaab", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
