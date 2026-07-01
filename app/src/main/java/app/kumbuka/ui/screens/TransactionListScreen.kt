package app.kumbuka.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.ui.theme.KumbukaColors
import app.kumbuka.ui.theme.ManropeFamily
import app.kumbuka.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionListScreen(
    onNavigateToRecord: (String, Long?) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KumbukaColors.Background)
            .padding(16.dp)
    ) {
        Text(
            text = "My Transactions",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = KumbukaColors.Primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No records found. Start by adding one!",
                    fontFamily = ManropeFamily,
                    color = KumbukaColors.OnSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(transactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onEdit = { onNavigateToRecord(transaction.transactionType, transaction.id) },
                        onDelete = { 
                            viewModel.deleteTransaction(transaction)
                            Toast.makeText(context, "Record deleted", Toast.LENGTH_SHORT).show()
                        },
                        onRecordPayment = { amt, onResult ->
                            viewModel.recordPayment(transaction, amt, onResult)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRecordPayment: (Double, (Result<Unit>) -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val isLent = transaction.transactionType.lowercase() == "lent"
    val cardBgColor = if (isLent) Color(0xFFFF5C43) else Color(0xFF5F0500)
    val contentColor = if (isLent) Color.Black else Color.White
    val secondaryContentColor = if (isLent) Color.DarkGray else Color.LightGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.name,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = contentColor
                    )
                    Text(
                        text = if (isLent) "Lent" else "Borrowed",
                        fontFamily = ManropeFamily,
                        fontSize = 14.sp,
                        color = secondaryContentColor
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "KES ${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = contentColor
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Show less" else "Show more",
                        tint = contentColor
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = secondaryContentColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                InfoRow(label = "Phone", value = transaction.phoneNumber, color = contentColor)
                InfoRow(
                    label = "Date",
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(transaction.dateInMillis)),
                    color = contentColor
                )
                
                // Row for Due Date and Edit/Delete Icons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Due Date",
                        fontFamily = ManropeFamily,
                        fontSize = 14.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.dueDateInMillis?.let { 
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) 
                            } ?: "N/A",
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = contentColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, "Edit", tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Row for Balance and + Payment Button
                if (transaction.remoteId != null) {
                    InfoRow(label = "Status", value = transaction.status.replace("_", " "), color = contentColor)
                    InfoRow(
                        label = "Amount Paid",
                        value = "KES ${String.format(Locale.getDefault(), "%,.2f", transaction.amountPaid)}",
                        color = contentColor
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Balance",
                                fontFamily = ManropeFamily,
                                fontSize = 14.sp,
                                color = contentColor.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "KES ${String.format(Locale.getDefault(), "%,.2f", transaction.balance)}",
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = contentColor
                            )
                        }

                        if (transaction.balance > 0) {
                            var showPaymentDialog by remember { mutableStateOf(false) }
                            val context = LocalContext.current

                            Button(
                                onClick = { showPaymentDialog = true },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = contentColor.copy(alpha = 0.15f),
                                    contentColor = contentColor
                                ),
                                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
                                elevation = null
                            ) {
                                Text("+ Payment", fontFamily = ManropeFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (showPaymentDialog) {
                                var paymentAmount by remember { mutableStateOf("") }
                                var isSubmitting by remember { mutableStateOf(false) }

                                AlertDialog(
                                    onDismissRequest = { if (!isSubmitting) showPaymentDialog = false },
                                    title = { Text("Record Payment", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column {
                                            Text("Enter amount paid towards this loan:", fontFamily = ManropeFamily)
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = paymentAmount,
                                                onValueChange = { paymentAmount = it },
                                                label = { Text("Amount (KES)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !isSubmitting
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            enabled = !isSubmitting,
                                            onClick = {
                                                val amt = paymentAmount.toDoubleOrNull()
                                                if (amt != null && amt > 0) {
                                                    isSubmitting = true
                                                    onRecordPayment(amt) { result ->
                                                        isSubmitting = false
                                                        if (result.isSuccess) {
                                                            Toast.makeText(context, "Payment recorded", Toast.LENGTH_SHORT).show()
                                                            showPaymentDialog = false
                                                        } else {
                                                            Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        ) { Text(if (isSubmitting) "Saving..." else "Submit") }
                                    },
                                    dismissButton = {
                                        if (!isSubmitting) {
                                            TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel") }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (transaction.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = secondaryContentColor
                    )
                    Text(
                        text = transaction.notes,
                        fontFamily = ManropeFamily,
                        fontSize = 14.sp,
                        color = contentColor
                    )
                }


            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            color = color.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = color
        )
    }
}
