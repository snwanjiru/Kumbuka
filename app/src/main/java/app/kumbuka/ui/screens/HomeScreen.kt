package app.kumbuka.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import app.kumbuka.R
import app.kumbuka.data.local.entity.TransactionEntity
import app.kumbuka.network.DashboardSummaryResponse
import app.kumbuka.ui.components.KumbukaLogo
import app.kumbuka.ui.theme.KumbukaColors
import app.kumbuka.ui.theme.ManropeFamily
import app.kumbuka.viewmodel.HomeTab
import app.kumbuka.viewmodel.HomeViewModel
import app.kumbuka.viewmodel.TabState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Layout constants — same straddling pattern used across all screens
// ─────────────────────────────────────────────────────────────────────────────
private val TOP_BAR_HEIGHT = 64.dp
private val LOGO_CIRCLE    = 80.dp
private val LOGO_HALF      = LOGO_CIRCLE / 2   // 40.dp

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onLogOut:              () -> Unit,
    onNavigateToCircles:   () -> Unit = {},
    onNavigateToActivity:  () -> Unit = {},
    onNavigateToSettings:  () -> Unit = {},
    onNavigateToRecordLent: (Long?) -> Unit = {},
    onNavigateToRecordBorrowed: (Long?) -> Unit = {},
    initialTab: HomeTab = HomeTab.Dashboard,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ── Drawer state ──────────────────────────────────────────────────────────
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // ── Pager state for swipe navigation ─────────────────────────────────────
    val pagerState = rememberPagerState(
        initialPage = if (initialTab == HomeTab.Dashboard) 0 else 1,
        pageCount = { 2 }
    )

    // ── Collect ViewModel state ───────────────────────────────────────────────
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabState by viewModel.tabState.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()

    // ── Sync Pager with ViewModel ────────────────────────────────────────────
    // When user swipes, update the ViewModel so the header highlights correct tab
    LaunchedEffect(pagerState.currentPage) {
        val tab = if (pagerState.currentPage == 0) HomeTab.Dashboard else HomeTab.CashFlow
        viewModel.selectTab(tab)
    }

    // When a tab is clicked in the header, scroll the pager
    LaunchedEffect(selectedTab) {
        val targetPage = if (selectedTab == HomeTab.Dashboard) 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // ── Reset ViewModel on first appearance ───────────────────────────────────
    LaunchedEffect(initialTab) {
        viewModel.resetState(initialTab)
    }

    // ── Main layout wrapped in navigation drawer ──────────────────────────────
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                onLogOut           = onLogOut,
                onNavigateToCircles = onNavigateToCircles,
                onNavigateToActivity = onNavigateToActivity,
                onNavigateToSettings = onNavigateToSettings,
                onDrawerClose      = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KumbukaColors.Background)
        ) {
            // ── FIXED HEADER: Top Bar + Hero Section + Straddling Logo ───────
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // ── Top bar — hamburger (left) + empty center + empty right ───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TOP_BAR_HEIGHT)
                            .background(KumbukaColors.Background)
                            .statusBarsPadding()
                    ) {
                        // Hamburger icon — left side
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open menu",
                                tint               = KumbukaColors.Primary,
                                modifier           = Modifier.size(24.dp)
                            )
                        }
                    }

                    // ── Dark hero section with 2-tab header ──────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KumbukaColors.Primary)
                            .padding(horizontal = 24.dp)
                            .padding(top = LOGO_HALF + 40.dp, bottom = 12.dp)
                    ) {
                        Column {
                            // ── Tab row (2 columns) ───────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Left: Dashboard (Active)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { viewModel.selectTab(HomeTab.Dashboard) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Dashboard",
                                        fontFamily = ManropeFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.White.copy(
                                            alpha = if (selectedTab == HomeTab.Dashboard) 1f else 0.7f
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Right: Cash Flow
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { viewModel.selectTab(HomeTab.CashFlow) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Cash Flow",
                                        fontFamily = ManropeFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = KumbukaColors.OnPrimaryContainer.copy(
                                            alpha = if (selectedTab == HomeTab.CashFlow) 1f else 0.7f
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // ── Selection Indicator (Dynamic Underline) ──────
                            // Underline moves to the selected tab
                            Row(modifier = Modifier.fillMaxWidth()) {
                                when (selectedTab) {
                                    HomeTab.Dashboard -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(3.dp)
                                                .padding(horizontal = 40.dp)
                                                .background(Color.White, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    HomeTab.CashFlow -> {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(3.dp)
                                                .padding(horizontal = 40.dp)
                                                .background(Color.White, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Straddling logo circle ───────────────────────────────
                // Center sits exactly on the top-bar bottom edge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = TOP_BAR_HEIGHT - LOGO_HALF)
                        .size(LOGO_CIRCLE)
                        .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                        .background(Color.White, CircleShape)
                ) {
                    KumbukaLogo(size = LOGO_CIRCLE * 0.68f)
                }
            }

            // ── SCROLLABLE BODY: content starts below the fixed hero ─────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Tab content — varies based on pageIndex ──────────────────
                    when (tabState) {
                        is TabState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = KumbukaColors.Primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        is TabState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (tabState as TabState.Error).message,
                                    fontFamily = ManropeFamily,
                                    color = KumbukaColors.Error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            if (pageIndex == 0) {
                                if (transactions.isEmpty()) {
                                    EmptyDashboard()
                                } else {
                                    DashboardContent(dashboardSummary)
                                }
                            } else {
                                CashFlowTabContent(
                                    transactions = transactions,
                                    onRecordLent = { onNavigateToRecordLent(null) },
                                    onRecordBorrowed = { onNavigateToRecordBorrowed(null) },
                                    onEditTransaction = { transaction ->
                                        if (transaction.transactionType == "lent") {
                                            onNavigateToRecordLent(transaction.id)
                                        } else {
                                            onNavigateToRecordBorrowed(transaction.id)
                                        }
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard Content — displays backend stats
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardContent(summary: DashboardSummaryResponse?) {
    if (summary == null) {
        EmptyDashboard()
        return
    }

    var isVisible by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales.get(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // ── Main Overview Card ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KumbukaColors.SurfaceContainerLow),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Balance Overview",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = KumbukaColors.OnSurfaceVariant
                    )
                    
                    PrivacyToggle(
                        isVisible = isVisible,
                        onToggle = { isVisible = !isVisible },
                        tint = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryStat(
                        label = "Lent",
                        value = if (isVisible) "KSh ${String.format(locale, "%,.0f", summary.totalLent)}" else "••••",
                        color = Color(0xFF5F0500),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStat(
                        label = "Borrowed",
                        value = if (isVisible) "KSh ${String.format(locale, "%,.0f", summary.totalBorrowed)}" else "••••",
                        color = Color(0xFFB52614),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Details Grid ─────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardSmallCard(
                label = "Owed to Me",
                value = if (isVisible) "KSh ${String.format(locale, "%,.0f", summary.amountOwedToMe)}" else "••••",
                icon = Icons.Outlined.Shield, // placeholder
                modifier = Modifier.weight(1f)
            )
            DashboardSmallCard(
                label = "I Owe",
                value = if (isVisible) "KSh ${String.format(locale, "%,.0f", summary.amountIOwe)}" else "••••",
                icon = Icons.Default.PanTool, // placeholder
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Activity Summary ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = KumbukaColors.SurfaceContainerLowest),
            border = BorderStroke(1.dp, KumbukaColors.OutlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Loan Activity",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = KumbukaColors.Primary
                )
                Spacer(Modifier.height(12.dp))
                ActivityRow("Active Lent", summary.activeLoansLent.toString())
                ActivityRow("Active Borrowed", summary.activeLoansBorrowed.toString())
                ActivityRow("Overdue Loans", summary.overdueLoans.toString(), isAlert = summary.overdueLoans > 0)
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy((-2).dp)) {
        Text(label, fontFamily = ManropeFamily, fontSize = 12.sp, color = KumbukaColors.OnSurfaceVariant)
        Text(value, fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
    }
}

@Composable
private fun DashboardSmallCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, KumbukaColors.OutlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy((-2).dp)) {
            Icon(icon, null, tint = KumbukaColors.Primary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontFamily = ManropeFamily, fontSize = 12.sp, color = KumbukaColors.OnSurfaceVariant)
            Text(value, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KumbukaColors.Primary)
        }
    }
}

@Composable
private fun ActivityRow(label: String, value: String, isAlert: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = ManropeFamily, fontSize = 14.sp, color = KumbukaColors.OnSurfaceVariant)
        Text(
            value, 
            fontFamily = ManropeFamily, 
            fontWeight = FontWeight.Bold, 
            fontSize = 14.sp, 
            color = if (isAlert) KumbukaColors.Error else KumbukaColors.Primary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HomeDrawerContent — side menu that slides in from the left
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeDrawerContent(
    onLogOut: () -> Unit,
    onNavigateToCircles: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDrawerClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(KumbukaColors.SurfaceContainerLowest)
    ) {
        // ── Header with close button ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = onDrawerClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close menu",
                    tint               = KumbukaColors.Primary,
                    modifier           = Modifier.size(24.dp)
                )
            }
        }

        HorizontalDivider(color = KumbukaColors.OutlineVariant)

        // ── Menu items ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            listOf(
                "My Circles" to onNavigateToCircles,
                "Activity" to onNavigateToActivity,
                "Settings" to onNavigateToSettings
            ).forEach { (label, onClick) ->
                Text(
                    label,
                    fontFamily = ManropeFamily,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = KumbukaColors.Primary,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .clickable(remember { MutableInteractionSource() }, null) {
                            onDrawerClose()
                            onClick()
                        }
                        .padding(vertical = 16.dp)
                )
            }
        }

        HorizontalDivider(color = KumbukaColors.OutlineVariant)

        // ── Logout at bottom ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            TextButton(
                onClick = {
                    onDrawerClose()
                    onLogOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Log Out",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    color      = KumbukaColors.Secondary
                )
            }
        }
    }
}

@Composable
private fun CashFlowTabContent(
    transactions: List<TransactionEntity>,
    onRecordLent: () -> Unit,
    onRecordBorrowed: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var isForgiven by remember { mutableStateOf(false) }
    var isBlacklisted by remember { mutableStateOf(false) }

    if (transactionToDelete != null) {
        val isLent = transactionToDelete!!.transactionType == "lent"
        val isGrouped = transactions.count { 
            val normalizedPhone = it.phoneNumber.filter { char -> char.isDigit() }.takeLast(9)
            val targetPhone = transactionToDelete!!.phoneNumber.filter { char -> char.isDigit() }.takeLast(9)
            it.name.equals(transactionToDelete!!.name, ignoreCase = true) && 
            normalizedPhone == targetPhone && 
            it.transactionType == transactionToDelete!!.transactionType
        } > 1

        AlertDialog(
            onDismissRequest = { 
                transactionToDelete = null
                isForgiven = false
                isBlacklisted = false
            },
            title = { Text("Delete Record", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text(
                        if (isGrouped) "Are you sure you want to delete this specific record from the group? This total will be updated."
                        else "Are you sure you want to delete this transaction? This action cannot be undone.",
                        fontFamily = ManropeFamily
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Options
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { isForgiven = !isForgiven }
                    ) {
                        Checkbox(
                            checked = isForgiven,
                            onCheckedChange = { isForgiven = it },
                            colors = CheckboxDefaults.colors(checkedColor = KumbukaColors.Primary)
                        )
                        Text("Forgiven", fontFamily = ManropeFamily, fontSize = 14.sp)
                    }
                    
                    if (isLent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { isBlacklisted = !isBlacklisted }
                        ) {
                            Checkbox(
                                checked = isBlacklisted,
                                onCheckedChange = { isBlacklisted = it },
                                colors = CheckboxDefaults.colors(checkedColor = KumbukaColors.Primary)
                            )
                            Text("Blacklist Contact", fontFamily = ManropeFamily, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                val isEnabled = if (isLent) isForgiven || isBlacklisted else isForgiven
                
                TextButton(
                    enabled = isEnabled,
                    onClick = {
                        onDeleteTransaction(transactionToDelete!!)
                        transactionToDelete = null
                        isForgiven = false
                        isBlacklisted = false
                    }
                ) {
                    Text(
                        "Delete", 
                        color = if (isEnabled) KumbukaColors.Error else KumbukaColors.Error.copy(alpha = 0.4f), 
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    transactionToDelete = null
                    isForgiven = false
                    isBlacklisted = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Scrollable Upper Part (Now using LazyColumn for shuffling animations) ──
        Box(modifier = Modifier.weight(1f)) {
            if (transactions.isEmpty()) {
                val infiniteTransition = rememberInfiniteTransition(label = "cashFlowEmpty")
                val offsetY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue  = 12f,
                    animationSpec = infiniteRepeatable(
                        animation  = tween(2500, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "y"
                )

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = 60.dp)
                            .graphicsLayer { translationY = offsetY }
                            .size(140.dp)
                            .background(KumbukaColors.SurfaceContainerHigh.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.Notes,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = KumbukaColors.Primary.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    Text(
                        text       = "Your cash flow is quiet",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = KumbukaColors.Primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Record your first transaction to see your lending and borrowing trends here.",
                        fontFamily = ManropeFamily,
                        fontSize = 15.sp,
                        color = KumbukaColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        lineHeight = 24.sp
                    )
                }
            } else {
                // ── Grouping and Sorting Logic ──────────────────────────────
                // 1. Group by Person first
                val personGroups = transactions.groupBy { 
                    val normalizedPhone = it.phoneNumber.filter { char -> char.isDigit() }.takeLast(9)
                    "${it.name.lowercase()}|$normalizedPhone"
                }
                
                // 2. Sort People by their latest overall transaction date
                val sortedPeople = personGroups.entries.sortedByDescending { it.value.maxOf { t -> t.dateInMillis } }
                
                // 3. Create a flat list of groups (Person-Type combinations)
                val sortedGroups = mutableListOf<Triple<String, List<TransactionEntity>, String>>()
                sortedPeople.forEach { entry ->
                    val personKey = entry.key
                    val personTransactions = entry.value
                    val typeGroups = personTransactions.groupBy { it.transactionType }
                    
                    // Always show "lent" then "borrowed" for the same person
                    val typesInOrder = typeGroups.keys.sortedBy { if (it == "lent") 0 else 1 }
                    
                    typesInOrder.forEach { type ->
                        val itemsOfType = typeGroups[type]!!
                        val groupKey = "$personKey|$type"
                        sortedGroups.add(Triple(groupKey, itemsOfType, type))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                ) {
                    items(
                        items = sortedGroups,
                        key = { it.first } // Stable key for shuffling animation
                    ) { triple ->
                        val key = triple.first
                        val groupItems = triple.second
                        
                        val representative = groupItems.first()
                        val totalAmount = groupItems.sumOf { it.amount }
                        val latestDate = groupItems.maxOf { it.dateInMillis }
                        
                        Box(modifier = Modifier.animateItem()) {
                            TransactionItem(
                                transaction = representative.copy(amount = totalAmount, dateInMillis = latestDate),
                                isExpanded = expandedGroupId == key,
                                onExpandClick = {
                                    expandedGroupId = if (expandedGroupId == key) null else key
                                },
                                onEditClick = onEditTransaction,
                                onDeleteClick = { transactionToDelete = it },
                                subItems = groupItems
                            )
                        }
                    }
                }
            }
        }

        // ── FIXED BOUNDARY ────────────────────────────────────────────────────
        // Creates a clear gap so scrolling records don't touch the buttons
        Spacer(Modifier.height(24.dp))

        // ── Action Buttons (Pinned to bottom) ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick  = onRecordLent,
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F0500).copy(alpha = 0.85f)),
                modifier = Modifier.weight(1f).height(56.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("+ Money Lent", fontFamily = ManropeFamily, 
                    fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Button(
                onClick  = onRecordBorrowed,
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFB52614).copy(alpha = 0.85f)),
                modifier = Modifier.weight(1f).height(56.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("+ Money Borrowed", fontFamily = ManropeFamily, 
                    fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onEditClick: (TransactionEntity) -> Unit,
    onDeleteClick: (TransactionEntity) -> Unit,
    subItems: List<TransactionEntity> = emptyList()
) {
    var isVisible by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales.get(0)
    val dateFormatter = remember(locale) { SimpleDateFormat("dd MMM, yyyy", locale) }
    val dateString = dateFormatter.format(Date(transaction.dateInMillis))

    val isLent = transaction.transactionType == "lent"
    val contentColor = KumbukaColors.Primary // Main text color for white background
    val prefix = if (isLent) "+" else "-"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val width = size.width
                val height = size.height
                
                // Shadow brush focused on the wave's transition edge for the "layered" effect
                val shadowBrush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f)),
                    startX = width * 0.30f,
                    endX = width * 0.65f
                )

                // Aurora-like dual-tone brush for deep richness matching the image
                // Lent (+) is dark burgundy, Borrowed (-) is vibrant red
                val mainBrush = Brush.linearGradient(
                    colors = if (isLent) {
                        listOf(
                            Color(0xFF3D0300).copy(alpha = 0.7f),
                            Color(0xFF5F0500).copy(alpha = 0.9f),
                        )
                    } else {
                        listOf(
                            Color(0xFF801000).copy(alpha = 0.7f),
                            Color(0xFFB52614).copy(alpha = 0.9f),
                        )
                    },
                    start = Offset(width * 0.50f, 0f),
                    end = Offset(width, height)
                )

                // The Asymmetrical Wave: Starts at 65% (top) and curves to 0% (bottom-left)
                // This ensures the red gradient becomes a background for the expanded content
                val mainPath = Path().apply {
                    moveTo(width * 0.65f, 0f) 
                    cubicTo(
                        x1 = width * 0.85f, y1 = height * 0.25f, // Ease right slightly
                        x2 = width * 0.25f, y2 = height * 0.55f, // Sweep across left
                        x3 = 0f, y3 = height * 0.75f            // Reach left edge
                    )
                    lineTo(0f, height)
                    lineTo(width, height)
                    lineTo(width, 0f)
                    close()
                }

                // Internal depth shadow tracks the new expanding curve
                val shadowPath = Path().apply {
                    moveTo(width * 0.62f, 0f)
                    cubicTo(
                        x1 = width * 0.82f, y1 = height * 0.25f,
                        x2 = width * 0.22f, y2 = height * 0.55f,
                        x3 = -width * 0.03f, y3 = height * 0.75f
                    )
                    lineTo(0f, height)
                    lineTo(width, height)
                    lineTo(width, 0f)
                    close()
                }

                drawPath(shadowPath, brush = shadowBrush)
                drawPath(mainPath, brush = mainBrush)
            }
            .clickable { onExpandClick() }
            .animateContentSize()
            .padding(16.dp)
    ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((-2).dp)
    ) {
            Text(
                text = transaction.name,
                style = TextStyle(
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.15f),
                        offset = Offset(0f, 2f),
                        blurRadius = 3f
                    )
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (subItems.size > 1) "${subItems.size} records" else dateString,
                    style = TextStyle(
                        fontFamily = ManropeFamily,
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.65f),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.1f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    )
                )

                Spacer(Modifier.weight(1f))

                PrivacyToggle(
                    isVisible = isVisible,
                    onToggle = { isVisible = !isVisible },
                    tint = Color.White.copy(alpha = 0.9f)
                ) {
                    val amountText = if (isVisible) "$prefix KSh ${String.format(locale, "%,.2f", transaction.amount)}" else "••••"
                    Box(contentAlignment = Alignment.CenterEnd) {
                        // 1. The Outline (Black)
                        Text(
                            text = amountText,
                            style = TextStyle(
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.Black,
                                drawStyle = Stroke(
                                    width = 5f,
                                    join = StrokeJoin.Round
                                )
                            )
                        )
                        // 2. The Fill (White)
                        Text(
                            text = amountText,
                            style = TextStyle(
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (isExpanded) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            // Extra Details
            DetailRow("Contact", transaction.phoneNumber, contentColor)
            
            Spacer(Modifier.height(8.dp))

            // Show breakdown/details for all items (single or grouped)
            Text(
                if (subItems.size > 1) "Breakdown" else "Record Details",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            subItems.sortedByDescending { it.dateInMillis }.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(KumbukaColors.SurfaceContainerLow, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    // Row 1: Date and Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dateFormatter.format(Date(item.dateInMillis)),
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = contentColor
                        )
                        Text(
                            if (isVisible) "KSh ${String.format(locale, "%,.2f", item.amount)}" else "••••",
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = contentColor
                        )
                    }

                    if (item.notes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.notes,
                            fontFamily = ManropeFamily,
                            fontSize = 12.sp,
                            color = contentColor.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }

                    // Row 2: Due Date and Actions (Edit/Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.dueDateInMillis != null) {
                            Text(
                                "Due: ${dateFormatter.format(Date(item.dueDateInMillis))}",
                                fontFamily = ManropeFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor.copy(alpha = 0.5f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onEditClick(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit record",
                                    tint = contentColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onDeleteClick(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete record",
                                    tint = contentColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* TODO: Open Payment recording */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLent) Color.White.copy(alpha = 0.15f) else KumbukaColors.Primary.copy(alpha = 0.1f),
                        contentColor = if (isLent) Color.White else KumbukaColors.Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, if (isLent) Color.White.copy(alpha = 0.2f) else KumbukaColors.Primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        "Record Payment",
                        style = TextStyle(
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isLent) Color.White else KumbukaColors.Primary,
                            shadow = if (isLent) Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            ) else null
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            "$label:",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = color.copy(alpha = 0.6f),
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty States
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyDashboard() {
    val infiniteTransition = rememberInfiniteTransition(label = "swipeHint")
    
    // Horizontal swipe animation
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue  = -40f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "x"
    )

    // Fade animation to make the hand "reset" smoothly
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = keyframes {
                durationMillis = 2000
                0f at 0
                1f at 500
                1f at 1500
                0f at 2000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 100.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo puck that moves horizontally, containing ONLY the finger/frame
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer { 
                    translationX = offsetX 
                    this.alpha = alpha
                }
                .size(76.dp) // Window size remains the same
                .clip(CircleShape) // This clips the logo text out
        ) {
            KumbukaLogo(
                size = 130.dp, // Zoomed out from 155.dp
                modifier = Modifier.offset(y = 20.5.dp), // Adjusted to keep text hidden but show more frame
                colorFilter = ColorFilter.tint(KumbukaColors.Background, BlendMode.Multiply)
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text       = "Swipe left to get started.",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 20.sp,
            color      = KumbukaColors.Primary,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clickable extension — avoids importing the wrong clickable overload
// ─────────────────────────────────────────────────────────────────────────────
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication        = null,
        onClick           = onClick
    )
}

@Composable
private fun PrivacyToggle(
    isVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = KumbukaColors.Primary,
    iconContainerSize: androidx.compose.ui.unit.Dp = 55.dp, // Adjust this for overall icon touch area
    content: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // This renders the "+KSh" text if provided
        content?.invoke(this)

        Box(
            modifier = Modifier
                .size(iconContainerSize)
                .noRippleClickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.hide1))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                isPlaying = true,
                restartOnPlay = false,
                speed = if (isVisible) -1.5f else 1.5f
            )

            val dynamicProperties = rememberLottieDynamicProperties(
                rememberLottieDynamicProperty(
                    property = LottieProperty.COLOR,
                    value = tint.toArgb(),
                    keyPath = arrayOf("**")
                )
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(iconContainerSize), // Adjust this for the eye icon itself
                contentScale = ContentScale.Fit,
                dynamicProperties = dynamicProperties
            )
        }
    }
}

