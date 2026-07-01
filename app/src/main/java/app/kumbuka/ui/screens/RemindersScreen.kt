package app.kumbuka.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.ui.theme.KumbukaColors
import app.kumbuka.ui.theme.ManropeFamily
import app.kumbuka.viewmodel.HomeViewModel
import java.util.Locale

private data class RemindNowState(
    val clickedAt: Long,
    val expirationTime: Long,
    val originalBalance: Double,
    val isGracePeriod: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val transactions by viewModel.allTransactions.collectAsState()
    var snoozedItems by remember { mutableStateOf(mapOf<Long, Long>()) }
    var remindNowItems by remember { mutableStateOf(mapOf<Long, RemindNowState>()) }
    var failedRemindNowIds by remember { mutableStateOf(setOf<Long>()) }

    // Manage Remind Now state transitions
    LaunchedEffect(transactions) {
        while(true) {
            val currentTime = System.currentTimeMillis()
            val nextRemindNowItems = remindNowItems.toMutableMap()
            val nextFailedIds = failedRemindNowIds.toMutableSet()
            var changed = false

            remindNowItems.forEach { (id, state) ->
                if (currentTime > state.expirationTime) {
                    val transaction = transactions.find { it.id == id }
                    if (transaction != null) {
                        if (transaction.balance < state.originalBalance) {
                            // Payment made
                            if (!state.isGracePeriod) {
                                // Start 5 day grace period
                                nextRemindNowItems[id] = state.copy(
                                    isGracePeriod = true,
                                    expirationTime = currentTime + 5L * 24 * 60 * 60 * 1000
                                )
                                changed = true
                            } else {
                                // Grace period over
                                nextRemindNowItems.remove(id)
                                changed = true
                            }
                        } else {
                            // No payment made in 24h (or grace period over with no further payment)
                            nextRemindNowItems.remove(id)
                            nextFailedIds.add(id)
                            changed = true
                        }
                    }
                }
            }

            if (changed) {
                remindNowItems = nextRemindNowItems
                failedRemindNowIds = nextFailedIds
            }
            delay(5000) // Check every 5 seconds
        }
    }

    // Filter and Sort Reminders
    val reminders = remember(transactions, snoozedItems, remindNowItems, failedRemindNowIds) {
        transactions.filter { it.status != "PAID" && it.dueDateInMillis != null }
            .sortedWith(compareByDescending<TransactionEntity> { it.id in failedRemindNowIds }
                .thenBy { it.dueDateInMillis })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Active Reminders", 
                        fontFamily = ManropeFamily, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = KumbukaColors.Background
                )
            )
        },
        containerColor = KumbukaColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = KumbukaColors.Primary.copy(alpha = 0.1f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Everything is on track!",
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            color = KumbukaColors.Primary
                        )
                        Text(
                            "You have no active due dates.",
                            fontFamily = ManropeFamily,
                            fontSize = 14.sp,
                            color = KumbukaColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    "You have ${reminders.size} items that need attention.",
                    fontFamily = ManropeFamily,
                    fontSize = 14.sp,
                    color = KumbukaColors.OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    val suppressedIds = snoozedItems.keys + remindNowItems.keys
                    val activeReminders = reminders.filter { it.id !in suppressedIds }
                    val snoozedReminders = reminders.filter { it.id in suppressedIds }

                    items(activeReminders, key = { it.id }) { transaction ->
                        ReminderActionCard(
                            transaction = transaction,
                            isSnoozed = false,
                            onSnooze = { days ->
                                val target = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
                                snoozedItems = snoozedItems + (transaction.id to target)
                            },
                            onRemindNow = {
                                val clickedTime = System.currentTimeMillis()
                                remindNowItems = remindNowItems + (transaction.id to RemindNowState(
                                    clickedAt = clickedTime,
                                    expirationTime = clickedTime + 24L * 60 * 60 * 1000,
                                    originalBalance = transaction.balance
                                ))
                                failedRemindNowIds = failedRemindNowIds - transaction.id
                            }
                        )
                    }

                    if (snoozedReminders.isNotEmpty()) {
                        item {
                            Text(
                                "Snoozed for Later",
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(snoozedReminders, key = { "snoozed_${it.id}" }) { transaction ->
                            val snoozeTime = snoozedItems[transaction.id]
                            val remindNowState = remindNowItems[transaction.id]
                            
                            ReminderActionCard(
                                transaction = transaction,
                                isSnoozed = true,
                                snoozeTargetTime = snoozeTime ?: remindNowState?.expirationTime
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderActionCard(
    transaction: TransactionEntity,
    isSnoozed: Boolean = false,
    snoozeTargetTime: Long? = null,
    onSnooze: (Int) -> Unit = {},
    onRemindNow: () -> Unit = {}
) {
    val context = LocalContext.current
    var showSnoozeMenu by remember { mutableStateOf(false) }

    var remainingTime by remember(snoozeTargetTime) {
        mutableLongStateOf(snoozeTargetTime?.let { it - System.currentTimeMillis() } ?: 0L)
    }

    LaunchedEffect(snoozeTargetTime) {
        if (snoozeTargetTime != null) {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime = snoozeTargetTime - System.currentTimeMillis()
            }
        }
    }

    val now = System.currentTimeMillis()
    val isOverdue = transaction.dueDateInMillis!! < now
    val daysDiff = ((transaction.dueDateInMillis - now) / (1000 * 60 * 60 * 24)).toInt()
    
    val statusText = when {
        isSnoozed -> "Snoozed"
        isOverdue -> "${Math.abs(daysDiff)} days overdue"
        daysDiff == 0 -> "Due today"
        else -> "Due in $daysDiff days"
    }
    
    val statusColor = when {
        isSnoozed -> KumbukaColors.OnSurfaceVariant.copy(alpha = 0.5f)
        isOverdue -> Color(0xFFE84C3D)
        else -> Color(0xFFF39C12)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isSnoozed) 0.6f else 1f },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.name,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isSnoozed) KumbukaColors.Primary.copy(alpha = 0.6f) else KumbukaColors.Primary
                    )
                    Text(
                        statusText,
                        fontFamily = ManropeFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "KSh ${String.format(Locale.getDefault(), "%,.0f", transaction.balance)}",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isSnoozed) KumbukaColors.Primary.copy(alpha = 0.6f) else KumbukaColors.Primary
                    )

                    if (isSnoozed && remainingTime > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = KumbukaColors.Primary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                formatCountdown(remainingTime),
                                fontFamily = ManropeFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KumbukaColors.Primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            val message = if (transaction.transactionType == "lent") {
                "Gently nudge ${transaction.name.split(" ").first()} about the KSh ${String.format(Locale.getDefault(), "%,.0f", transaction.balance)} balance."
            } else {
                "Plan to settle your KSh ${String.format(Locale.getDefault(), "%,.0f", transaction.balance)} debt with ${transaction.name.split(" ").first()}."
            }
            
            Text(
                message,
                fontFamily = ManropeFamily,
                fontSize = 13.sp,
                color = KumbukaColors.OnSurfaceVariant.copy(alpha = if (isSnoozed) 0.5f else 1f),
                lineHeight = 18.sp
            )
            
            if (transaction.transactionType == "lent" && !isSnoozed) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRemindNow,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KumbukaColors.Primary),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remind Now", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showSnoozeMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, KumbukaColors.Primary),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Remind Later", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            val options = listOf(
                                "1 Day" to 1, "2 Days" to 2, "3 Days" to 3, 
                                "4 Days" to 4, "5 Days" to 5, "1 Week" to 7, "2 Weeks" to 14
                            )
                            options.forEach { (label, days) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontFamily = ManropeFamily) },
                                    onClick = {
                                        onSnooze(days)
                                        showSnoozeMenu = false
                                        android.widget.Toast.makeText(context, "Reminder scheduled for $label", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val days = totalSeconds / (24 * 3600)
    val hours = (totalSeconds % (24 * 3600)) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (days > 0) {
        "${days}d ${String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)}"
    } else {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
