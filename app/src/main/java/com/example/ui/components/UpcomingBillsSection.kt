package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Bill
import com.example.model.ExpenseCategories
import com.example.ui.DashboardViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun UpcomingBillsSection(
    bills: List<Bill>,
    onToggleBillPaid: (billId: String, currentStatus: Boolean) -> Unit,
    onDeleteBill: (billId: String) -> Unit,
    onAddBillClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("upcoming_bills_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Upcoming Bills",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Bijli, Pani, Wifi & Rent reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onAddBillClicked,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("section_add_bill_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Bill",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Bill",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No upcoming bills! Tap + Add Bill to track one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bills.forEach { bill ->
                        BillItemCard(
                            bill = bill,
                            onTogglePaid = { onToggleBillPaid(bill.id, bill.isPaid) },
                            onDelete = { onDeleteBill(bill.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillItemCard(
    bill: Bill,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit
) {
    val catInfo = ExpenseCategories.getCategoryInfo(bill.category)
    val statusInfo = computeDueStatus(bill.dueDate, bill.isPaid)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bill.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Checkbox Toggle & Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onTogglePaid,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (bill.isPaid) "Mark Unpaid" else "Mark Paid",
                        tint = if (bill.isPaid) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(catInfo.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catInfo.icon,
                        contentDescription = bill.category,
                        tint = catInfo.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (bill.isPaid) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusInfo.text,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = statusInfo.color
                        )
                        if (bill.recurring.isNotEmpty() && bill.recurring != "ONE_TIME") {
                            Text(
                                text = " • ${bill.recurring.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Amount & Delete Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DashboardViewModel.formatCurrency(bill.amount),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (bill.isPaid) "Paid ✅" else "Pending ⏳",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (bill.isPaid) EmeraldPrimary else MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Bill",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class DueStatusInfo(val text: String, val color: Color)

private fun computeDueStatus(dueDateStr: String, isPaid: Boolean): DueStatusInfo {
    if (isPaid) {
        return DueStatusInfo("Paid", EmeraldPrimary)
    }
    if (dueDateStr.isEmpty()) {
        return DueStatusInfo("Upcoming", Color.Gray)
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val dueDate = sdf.parse(dueDateStr) ?: return DueStatusInfo("Upcoming", Color.Gray)
        val today = sdf.parse(sdf.format(Date())) ?: Date()

        val diffMillis = dueDate.time - today.time
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        when {
            diffDays < 0 -> DueStatusInfo("Overdue by ${-diffDays}d", RosePrimary)
            diffDays == 0L -> DueStatusInfo("Due Today!", RosePrimary)
            diffDays == 1L -> DueStatusInfo("Due Tomorrow", Color(0xFFD97706))
            else -> DueStatusInfo("Due in $diffDays days", Color(0xFF0284C7))
        }
    } catch (e: Exception) {
        DueStatusInfo(dueDateStr, Color.Gray)
    }
}
