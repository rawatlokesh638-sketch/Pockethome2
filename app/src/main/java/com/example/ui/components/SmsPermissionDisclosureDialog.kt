package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.RoyalBlue

@Composable
fun SmsPermissionDisclosureDialog(
    onDismiss: () -> Unit,
    onAcceptAndRequestPermission: () -> Unit,
    onViewFullPrivacyPolicy: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("sms_disclosure_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(RoyalBlue.copy(alpha = 0.12f))
                        .border(1.5.dp, RoyalBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy Shield",
                        tint = RoyalBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Subtitle
                Text(
                    text = "SMS Permission & Data Disclosure",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "PocketHome Expense Auto-Tracking Transparency",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Kyu le rahe hain
                DisclosurePointItem(
                    icon = Icons.Default.ReceiptLong,
                    iconBg = RoyalBlue.copy(alpha = 0.15f),
                    iconTint = RoyalBlue,
                    title = "Kyu Chahiye SMS Permission? (Purpose)",
                    description = "Aapke bank, UPI (GPay, PhonePe, Paytm) aur credit card ke transaction SMS se amount, dukaan/merchant ka naam aur date automatically track karke kharcha jodne ke liye."
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Kya read hoga
                DisclosurePointItem(
                    icon = Icons.Default.CheckCircle,
                    iconBg = MoneyGreen.copy(alpha = 0.15f),
                    iconTint = MoneyGreen,
                    title = "Kya Data Read Hoga? (Data Collected)",
                    description = "Sirf bank aur payment sender IDs (jaise HDFC, SBI, PUREBR, PAYTM) se aane wale financial debits aur credits ke messages."
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Kya KABHI read nahi hoga
                DisclosurePointItem(
                    icon = Icons.Default.Block,
                    iconBg = Color(0xFFEF4444).copy(alpha = 0.12f),
                    iconTint = Color(0xFFEF4444),
                    title = "Kya KABHI NAHI Padha Jata? (Safe & Private)",
                    description = "Aapke personal family ya friends ke chats, confidential OTPs, passwords ya contacts ko app KABHI bhi access nahi karti."
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Zero Sharing Guarantee
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Zero Sharing",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Zero Third-Party Sharing (100% Private)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ham aapka data kisi bhi third party, ad company ya broker ke sath share nahi karte. Sara analysis aapke device par on-device rehta hai.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Help Support Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = "Email",
                        tint = RoyalBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Help & Privacy Support: rawatlokesh638@gmail.com",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onViewFullPrivacyPolicy) {
                    Text(
                        text = "Puri Privacy Policy aur Terms dekhein 📜",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Abhi Nahi", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onAcceptAndRequestPermission,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("confirm_sms_permission_btn")
                    ) {
                        Text(
                            text = "Agree & Grant ✅",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosurePointItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
