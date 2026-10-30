package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RoyalBlue

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp)
                .testTag("privacy_policy_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Privacy Policy & Terms",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gopniyata aur Niyam o Shartein",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Contact Highlight Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RoyalBlue.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Official Support & Help Contact",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalBlue
                                )
                                Text(
                                    text = "rawatlokesh638@gmail.com",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    PolicySection(
                        title = "1. Hamari Pratibaddhata (Our Privacy Commitment)",
                        body = "PocketHome aapki personal privacy aur data suraksha ko sarvochch prathmikta deta hai. Hamara uddeshya aapko ek saral, surakshit aur intelligent hisaab-kitab ka sadhan pradan karna hai bina kisi data misuse ke."
                    )

                    PolicySection(
                        title = "2. SMS Permission & Data Collection (SMS ka Upyog)",
                        body = "App READ_SMS aur RECEIVE_SMS permissions ki mang karti hai. Iska ekmatra uddeshya aapke bank, UPI (GPay, PhonePe, Paytm, etc.) aur card transactions ke debits aur credits ko automatically track karna hai taaki aapko hath se ek-ek kharcha na likhna pade."
                    )

                    PolicySection(
                        title = "3. Kya Data Access Nahi Kiya Jata (Strict Exclusions)",
                        body = "Ham spasht karte hain ki PocketHome aapke personal parivarik messages, kisi bhi prakaar ke OTPs (One Time Passwords), login credentials, phone contacts, photos, ya call history ko access ya store NAHI karta hai."
                    )

                    // Strict Zero-Sharing Section
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "4. Zero Third-Party Sharing (100% Guaranteed)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ham aapka koi bhi transaction ya personal data kisi bhi teesre paksh (Third Party), advertisement company, marketing agency ya data broker ke saath KABHI BHI share ya bechte nahi hain. 'Share m kisi se nahi karuga' hamara dridh niyam hai.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    PolicySection(
                        title = "5. In-App Account & Data Deletion (Aapka Poora Niyantran)",
                        body = "Aapka apne data par 100% poora adhikar hai. Aap kisi bhi samay:\n• Profile Avatar par tap karke 'Delete Account & All Data' chun sakte hain.\n• Home screen ke footer me 'Delete Account & Wipe All Data' par click karke turant apna sara data (expenses, bills, budget, leaderboard, chat) aur Firebase account permanently delete kar sakte hain.\n• 'Reset All Data to 0' button se sara hisaab zero kar sakte hain."
                    )

                    PolicySection(
                        title = "6. Online Account Deletion Request (Right to Erasure)",
                        body = "App store niyam aur data protection adhiniyam ke tehat, yadi aap app uninstall kar chuke hain aur apna account/data delete karwana chahte hain, toh aap hamare support email:\n📧 rawatlokesh638@gmail.com\npar 'Delete My Account' subject ke saath apna registered email bhej sakte hain. Aapka sara data 24 se 48 ghante ke andar permanently wipe kar diya jata hai."
                    )

                    PolicySection(
                        title = "7. Terms of Conditions (Niyam evam Shartein)",
                        body = "• Yah app vyaktigat (personal) aur parivarik kharche ke hisaab-kitab ke liye banayi gayi hai.\n• App me diye gaye financial health score aur AI insights keval sahayata ke liye hain.\n• Pro membership aur AI features fair-use policy ke tahat hain."
                    )

                    PolicySection(
                        title = "8. Developer & Store Contact (Sampark)",
                        body = "Official Developer Contact for Store Inquiries, Grievance Officer & Help Support:\n📧 rawatlokesh638@gmail.com\nDeveloper: Lokesh Rawat | App: PocketHome"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Theek Hai / Samajh Gaya 👍", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 17.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
