package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoyalBlue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.model.ProStatus
import com.example.ui.theme.BgLight
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPlanSheet(
    proStatus: ProStatus,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSubmitRequest: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPlanPeriod by remember { mutableStateOf("MONTHLY") } // MONTHLY or YEARLY
    
    var txnId by remember { mutableStateOf("") }
    var utrNumber by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("pro_plan_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(RoyalBlue, PurpleAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Pro", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pockethome PRO ✨",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ghar ka Smart, Automated Hisaab",
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

            when (proStatus.status) {
                "ACTIVE" -> {
                    ProActiveSection(onDismiss)
                }
                "PENDING" -> {
                    ProPendingSection(onDismiss)
                }
                else -> {
                    ProPaymentForm(
                        selectedPlanPeriod = selectedPlanPeriod,
                        onPeriodChange = { selectedPlanPeriod = it },
                        txnId = txnId,
                        onTxnIdChange = { txnId = it },
                        utrNumber = utrNumber,
                        onUtrNumberChange = { utrNumber = it },
                        onSubmit = { onSubmitRequest(txnId, utrNumber) },
                        isLoading = isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pro Features List
            Text(
                text = "Everything Included in PRO (₹25/mo)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProFeatureItem(
                    icon = Icons.Default.Sms,
                    title = "Auto All Transaction SMS Sync",
                    desc = "Bank, Amazon, Wallets, Credit Cards, UPI, Swiggy, Zomato ke sabhi SMS se kharche/aay automatically jud jate hain."
                )
                ProFeatureItem(
                    icon = Icons.Default.Description,
                    title = "Export to PDF & Excel",
                    desc = "Pura monthly hisaab ek click mein clean PDF aur Excel spreadsheet mein download karein."
                )
                ProFeatureItem(
                    icon = Icons.Default.FamilyRestroom,
                    title = "Family Sharing & Sync",
                    desc = "Pati, Patni aur bachhe sabhi ek saath sync karke ghar ka hisaab manage kar sakte hain."
                )
                ProFeatureItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Smart WhatsApp & Bill Reminders",
                    desc = "Bijli, Pani, Wifi, School fees aur EMI ke reminders due date se 3 din pehle milein."
                )
                ProFeatureItem(
                    icon = Icons.Default.Receipt,
                    title = "Unlimited Bill Photo Vault",
                    desc = "Groceries aur dukan ke receipt photos cloud par hamesha safe aur organized raheinge."
                )
                ProFeatureItem(
                    icon = Icons.Default.PieChart,
                    title = "Category Budgets & Overspending Alerts",
                    desc = "Groceries, Food, Shopping ke alag budget banayein aur limit paar hone par alert paayein."
                )
                ProFeatureItem(
                    icon = Icons.Default.Lock,
                    title = "Bank-Grade PIN Lock & Ad-Free",
                    desc = "100% private. Koi ads nahi, aur fingerprint/PIN se hisaab secure."
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProActiveSection(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MoneyGreen.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.Default.CloudDone, contentDescription = "Active", tint = MoneyGreen, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Membership Active! 🎉",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MoneyGreen
            )
            Text(
                text = "Aap ab PRO features ka anand le sakte hain. Agla recharge ek mahine baad karein.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MoneyGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Awesome!")
            }
        }
    }
}

@Composable
private fun ProPendingSection(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Pending", tint = BrandOrange, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Approval Pending... ⏳",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = BrandOrange
            )
            Text(
                text = "Aapka payment request humein mil gaya hai. Admin team verify karke jald hi activate kar degi.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it, Thanks!")
            }
        }
    }
}

@Composable
private fun ProPaymentForm(
    selectedPlanPeriod: String,
    onPeriodChange: (String) -> Unit,
    txnId: String,
    onTxnIdChange: (String) -> Unit,
    utrNumber: String,
    onUtrNumberChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    Column {
        // Pricing Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(2.dp, RoyalBlue.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(RoyalBlue).padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AFFORDABLE FAMILY PLAN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = if (selectedPlanPeriod == "MONTHLY") "₹25" else "₹249",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, color = RoyalBlue)
                    )
                    Text(
                        text = if (selectedPlanPeriod == "MONTHLY") " / month" else " / year (₹20/mo)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = "☕ Ek chai ke daam mein pura ghar ka digital hisaab!",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlanOptionPill(
                        title = "Monthly @ ₹25/mo",
                        selected = selectedPlanPeriod == "MONTHLY",
                        onClick = { onPeriodChange("MONTHLY") },
                        modifier = Modifier.weight(1f)
                    )
                    PlanOptionPill(
                        title = "Yearly @ ₹249/yr (Save 17%)",
                        selected = selectedPlanPeriod == "YEARLY",
                        onClick = { onPeriodChange("YEARLY") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payment Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = RoyalBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Payment Instructions", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. PhonePe pe ₹25 transfer karein: 9050884894\n2. Transfer ke baad Txn ID aur UTR number yahan enter karein.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        OutlinedTextField(
            value = txnId,
            onValueChange = onTxnIdChange,
            label = { Text("Transaction ID") },
            placeholder = { Text("Enter PhonePe Txn ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RoyalBlue)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = utrNumber,
            onValueChange = onUtrNumberChange,
            label = { Text("UTR Number") },
            placeholder = { Text("12-digit UTR number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RoyalBlue)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            enabled = txnId.isNotBlank() && utrNumber.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(text = "Submit Request for Approval", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlanOptionPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RoyalBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun ProFeatureItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(RoyalBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = RoyalBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
