package app.kumbuka.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
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
import app.kumbuka.viewmodel.ContactSummary
import app.kumbuka.viewmodel.CashFlowGroup
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

    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    // ── Notification Permission Launcher ─────────────────────────────────────
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle result if needed
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ── Pager state for swipe navigation ─────────────────────────────────────
    val pagerState = rememberPagerState(
        initialPage = if (initialTab == HomeTab.Dashboard) 0 else 1,
        pageCount = { 2 }
    )

    // ── Nested scroll to open drawer from Dashboard swipe ────────────────────
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If on Dashboard and swiping right, trigger drawer open
                if (pagerState.currentPage == 0 && available.x > 40f && source == NestedScrollSource.Drag) {
                    if (drawerState.isClosed) {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                        return available // Consume scroll to prevent simultaneous exit
                    } else {
                        // Drawer already open, swipe right again -> Leave app
                        activity?.finish()
                        return available
                    }
                }
                return Offset.Zero
            }
        }
    }

    // ── Collect ViewModel state ───────────────────────────────────────────────
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabState by viewModel.tabState.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val contactSummaries by viewModel.contactSummaries.collectAsState()
    val cashFlowGroups by viewModel.cashFlowGroups.collectAsState()
    val userName by viewModel.userName.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilters by viewModel.selectedFilters.collectAsState()
    val selectedDueOption by viewModel.selectedDueOption.collectAsState()

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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                userName           = userName,
                onLogOut           = onLogOut,
                onNavigateToCircles = onNavigateToCircles,
                onNavigateToActivity = onNavigateToActivity,
                onNavigateToSettings = onNavigateToSettings,
                onDrawerClose      = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        // Intercept back gesture on Dashboard to follow the open-then-exit flow
        BackHandler(enabled = pagerState.currentPage == 0) {
            if (drawerState.isClosed) {
                coroutineScope.launch { drawerState.open() }
            } else {
                activity?.finish()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KumbukaColors.Background)
                .nestedScroll(nestedScrollConnection)
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
                            .height(if (isLandscape) 48.dp else TOP_BAR_HEIGHT)
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
                            .padding(
                                top = if (isLandscape) 12.dp else (LOGO_HALF + 40.dp), 
                                bottom = if (isLandscape) 8.dp else 10.dp
                            )
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
                if (!isLandscape) {
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
            }

            // ── SCROLLABLE BODY: content starts below the fixed hero ─────────
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state    = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) { pageIndex ->
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (pageIndex == 0) {
                            if (transactions.isEmpty()) {
                                EmptyDashboard()
                            } else {
                                DashboardContent(
                                    summary = dashboardSummary,
                                    contactSummaries = contactSummaries
                                )
                            }
                        } else {
                            CashFlowTabContent(
                                groups = cashFlowGroups,
                                searchQuery = searchQuery,
                                selectedFilters = selectedFilters,
                                selectedDueOption = selectedDueOption,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onToggleFilter = { viewModel.toggleFilter(it) },
                                onUpdateDueOption = { viewModel.updateDueOption(it) },
                                onClearFilters = { viewModel.clearFilters() },
                                onRecordLent = { onNavigateToRecordLent(null) },
                                onRecordBorrowed = { onNavigateToRecordBorrowed(null) },
                                onEditTransaction = { transaction ->
                                    if (transaction.transactionType == "lent") {
                                        onNavigateToRecordLent(transaction.id)
                                    } else {
                                        onNavigateToRecordBorrowed(transaction.id)
                                    }
                                },
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onRecordPayment = { transaction, amount, onResult ->
                                    viewModel.recordPayment(transaction, amount, onResult)
                                },
                                hasAnyTransactions = transactions.isNotEmpty()
                            )
                        }
                    }
                }

                // ── OVERLAY: Loading & Error states ──────────────────────────
                // This prevents the Pager from being destroyed when syncing
                if (tabState is TabState.Loading && transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(KumbukaColors.Background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = KumbukaColors.Primary)
                    }
                } else if (tabState is TabState.Error) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(KumbukaColors.Background),
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
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard Content — displays backend stats
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardContent(
    summary: DashboardSummaryResponse?,
    contactSummaries: List<ContactSummary>
) {
    if (summary == null) {
        EmptyDashboard()
        return
    }

    var isVisible by remember { mutableStateOf(false) }
    
    val lentColor = Color(0xFF5F0500) // Burgundy
    val borrowedColor = Color(0xFFB52614) // Red

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Your Pockets",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = KumbukaColors.Primary
            )

            PrivacyToggle(
                isVisible = isVisible,
                onToggle = { isVisible = !isVisible },
                tint = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.6f),
                iconContainerSize = 65.dp
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── HERO: The Two Pockets (Visual Jars) ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MoneyJarCard(
                label = "Lent",
                amount = summary.amountOwedToMe,
                totalAmount = summary.totalLent,
                isVisible = isVisible,
                color = lentColor,
                modifier = Modifier.weight(1f)
            )
            MoneyJarCard(
                label = "Borrowed",
                amount = summary.amountIOwe,
                totalAmount = summary.totalBorrowed,
                isVisible = isVisible,
                color = borrowedColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── TOP CONTACTS: Horizontal Scroll ──────────────────────────────────
        Text(
            "My People",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = KumbukaColors.Primary
        )
        
        Spacer(Modifier.height(16.dp))

        if (contactSummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(KumbukaColors.SurfaceContainerLow, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No contacts yet",
                    fontFamily = ManropeFamily,
                    fontSize = 14.sp,
                    color = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                contactSummaries.forEach { contact ->
                    ContactCircleItem(contact)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── STATS SUMMARY (Simplified) ──────────────────────────────────────
        Text(
            "Quick Stats",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = KumbukaColors.Primary
        )
        Spacer(Modifier.height(12.dp))
        
        DashboardSmallCard(
            label = "OVERDUE LOANS",
            value = summary.overdueLoans.toString(),
            icon = Icons.Default.History,
            color = Color(0xFFE84C3D),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        // ── TREND CHART ──────────────────────────────────────────────────────
        Text(
            "Money Flow Trend",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = KumbukaColors.Primary
        )
        Spacer(Modifier.height(16.dp))
        
        MoneyTrendChart(
            trendData = summary.monthlyTrend ?: emptyList(),
            lentColor = lentColor,
            borrowedColor = borrowedColor,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MoneyTrendChart(
    trendData: List<app.kumbuka.network.MonthlyTrend>,
    lentColor: Color,
    borrowedColor: Color,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) return

    // Find the max value to scale the chart, ensuring we have at least a small scale even if all are 0
    val maxVal = trendData.flatMap { listOf(it.lent, it.borrowed) }.maxOrNull()?.toFloat()?.coerceAtLeast(100f) ?: 100f
    val pointSpacing = 70.dp
    // Ensure the chart is at least as wide as the screen
    val chartWidth = (pointSpacing * (trendData.size - 1)).coerceAtLeast(1.dp)

    Column(modifier = modifier) {
        Row(modifier = Modifier.weight(1f)) {
            // ── Y-AXIS LABELS ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val gridLineCount = 4
                for (i in 0..gridLineCount) {
                    val value = maxVal * (gridLineCount - i) / gridLineCount
                    Text(
                        text = when {
                            value >= 1_000_000 -> "${String.format("%.1f", value / 1_000_000)}M"
                            value >= 1000 -> "${(value / 1000).toInt()}k"
                            else -> value.toInt().toString()
                        },
                        fontFamily = ManropeFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // ── CHART AREA ──────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState(initial = Int.MAX_VALUE))
            ) {
                Canvas(modifier = Modifier
                    .width(chartWidth.coerceAtLeast(200.dp)) // Minimum width to avoid squishing
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val spacing = if (trendData.size > 1) width / (trendData.size - 1) else width
                    
                    fun getY(value: Double) = height - (value.toFloat() / maxVal * height)

                    // ── GRID LINES ──────────────────────────────
                    val gridLineCount = 4
                    for (i in 0..gridLineCount) {
                        val y = height * i / gridLineCount
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    // ── AXIS LINES ──────────────────────────────
                    // Y-Axis
                    drawLine(
                        color = KumbukaColors.Outline.copy(alpha = 0.3f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, height),
                        strokeWidth = 2.dp.toPx()
                    )
                    // X-Axis
                    drawLine(
                        color = KumbukaColors.Outline.copy(alpha = 0.3f),
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 2.dp.toPx()
                    )

                    // ── SMOOTH CURVE FUNCTION ──────────────────────────────
                    fun createSmoothPath(data: List<Double>): Path {
                        val path = Path()
                        if (data.isEmpty()) return path
                        
                        data.forEachIndexed { i, value ->
                            val x = i * spacing
                            val y = getY(value)
                            if (i == 0) {
                                path.moveTo(x, y)
                            } else {
                                val prevX = (i - 1) * spacing
                                val prevY = getY(data[i - 1])
                                val controlX1 = prevX + spacing / 2.5f
                                val controlX2 = x - spacing / 2.5f
                                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                            }
                        }
                        return path
                    }

                    // ── Draw Trend Lines ──────────────────────────────
                    val lentValues = trendData.map { it.lent }
                    val lentPath = createSmoothPath(lentValues)
                    val borrowedValues = trendData.map { it.borrowed }
                    val borrowedPath = createSmoothPath(borrowedValues)

                    // Areas (Fills)
                    if (trendData.size > 1) {
                        drawPath(
                            Path().apply {
                                addPath(lentPath)
                                lineTo((trendData.size - 1) * spacing, height)
                                lineTo(0f, height)
                                close()
                            },
                            brush = Brush.verticalGradient(listOf(lentColor.copy(alpha = 0.15f), Color.Transparent))
                        )
                        drawPath(
                            Path().apply {
                                addPath(borrowedPath)
                                lineTo((trendData.size - 1) * spacing, height)
                                lineTo(0f, height)
                                close()
                            },
                            brush = Brush.verticalGradient(listOf(borrowedColor.copy(alpha = 0.15f), Color.Transparent))
                        )
                    }

                    // Strokes (The actual lines)
                    drawPath(lentPath, lentColor, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
                    drawPath(borrowedPath, borrowedColor, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))

                    // Emphasis Points (Dots)
                    trendData.forEachIndexed { i, data ->
                        val x = i * spacing
                        drawCircle(lentColor, radius = 4.dp.toPx(), center = Offset(x, getY(data.lent)))
                        drawCircle(borrowedColor, radius = 4.dp.toPx(), center = Offset(x, getY(data.borrowed)))
                    }
                }
            }
        }
        
        // Month labels (aligned with chart area)
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(42.dp)) // Matches Y-labels width
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState(initial = Int.MAX_VALUE), enabled = false)
            ) {
                val labelsWidth = (pointSpacing * (trendData.size - 1)).coerceAtLeast(200.dp)
                Row(modifier = Modifier.width(labelsWidth), horizontalArrangement = Arrangement.SpaceBetween) {
                    trendData.forEach { data ->
                        Text(
                            data.month.split(" ").first(), // Show only month name to save space
                            fontFamily = ManropeFamily, 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Medium,
                            color = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(if (trendData.size > 1) labelsWidth / (trendData.size - 1) else labelsWidth),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.padding(start = 42.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TrendLegendItem("Lent", lentColor)
            TrendLegendItem("Borrowed", borrowedColor)
        }
    }
}

@Composable
private fun TrendLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp, 2.dp).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = ManropeFamily, fontSize = 10.sp, color = KumbukaColors.OnSurfaceVariant)
    }
}

@Composable
private fun MoneyJarCard(
    label: String,
    amount: Double,
    totalAmount: Double,
    isVisible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(180.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (label == "Lent") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF4CAF50))) {
                            append("$ ")
                        }
                        withStyle(SpanStyle(color = color)) {
                            append(label)
                        }
                    },
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    if (isVisible) "KSh ${String.format(Locale.getDefault(), "%,.0f", amount)}" else "••••",
                    fontFamily = ManropeFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }

            // Repayment Progress Bar
            val progress = if (totalAmount > 0) ((totalAmount - amount) / totalAmount).coerceIn(0.0, 1.0).toFloat() else 0f
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Repayment Progress",
                    fontFamily = ManropeFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(color.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceAtLeast(0.01f)) // Ensure at least a tiny bit is visible if progress is very small
                                .fillMaxHeight()
                                .background(color, CircleShape)
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontFamily = ManropeFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCircleItem(contact: ContactSummary) {
    val borderColor = if (contact.isOverdue) Color(0xFFE84C3D) else Color(0xFF4CAF50)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .drawBehind {
                    drawCircle(
                        color = borderColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                .padding(4.dp)
                .background(KumbukaColors.SurfaceContainerHigh, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contact.name.take(1).uppercase(),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = KumbukaColors.Primary
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            contact.name.split(" ").first(),
            fontFamily = ManropeFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = KumbukaColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun DebtAgingChart(data: Map<String, Double>, modifier: Modifier) {
    val maxValue = (data.values.maxOrNull() ?: 0.0).toFloat().coerceAtLeast(1f)
    val labels = listOf("1-7 Days", "8-30 Days", "30+ Days")
    val colors = listOf(Color(0xFFFFB4AB), Color(0xFFBA1A1A), Color(0xFF690005)) // Light red to Dark Red

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        labels.forEachIndexed { index, label ->
            val value = data[label]?.toFloat() ?: 0f
            val barHeightRatio = value / maxValue
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Bar Value
                if (value > 0) {
                    Text(
                        "KSh ${String.format(Locale.getDefault(), "%.0f", value)}",
                        fontFamily = ManropeFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors[index]
                    )
                    Spacer(Modifier.height(4.dp))
                }
                
                // The Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(barHeightRatio.coerceAtLeast(0.05f) * 0.8f) // Scale to 80% of height
                        .background(colors[index], RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Label
                Text(
                    label,
                    fontFamily = ManropeFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = KumbukaColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DonutChart(data: List<Pair<Float, Color>>, modifier: Modifier) {
    val total = data.sumOf { it.first.toDouble() }.toFloat()
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        
        if (total == 0f) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                style = Stroke(width = 40f)
            )
        } else {
            data.forEach { (value, color) ->
                val sweepAngle = (value / total) * 360f
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 40f)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
private fun ChartLegend(items: List<Pair<String, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    fontFamily = ManropeFamily,
                    fontSize = 10.sp,
                    color = KumbukaColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardSmallCard(
    label: String, 
    value: String, 
    icon: ImageVector, 
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    label, 
                    fontFamily = ManropeFamily, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold,
                    color = KumbukaColors.OnSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    value, 
                    fontFamily = ManropeFamily, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 18.sp, 
                    color = KumbukaColors.Primary
                )
            }
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    null, 
                    tint = color, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HomeDrawerContent — side menu that slides in from the left
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeDrawerContent(
    userName: String,
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
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1D2B), Color(0xFF394859))
                )
            )
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // User Profile Symbol in Rounded Box
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Profile",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    userName,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

            }
        }

        // ── Scrollable Menu ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            DrawerSectionHeader("NAVIGATE")
            DrawerItem(
                icon = Icons.Outlined.Notifications,
                label = "Active Reminders",
                onClick = {
                    onDrawerClose()
                    onNavigateToActivity()
                }
            )
            DrawerItem(
                icon = Icons.Outlined.Paid,
                label = "My Circles",
                onClick = {
                    onDrawerClose()
                    onNavigateToCircles()
                }
            )

            Spacer(Modifier.height(16.dp))
            DrawerSectionHeader("DATA")
            DrawerItem(
                icon = Icons.Outlined.Save,
                label = "Backup",
                onClick = {}
            )
            DrawerItem(
                icon = Icons.Outlined.CloudUpload,
                label = "Restore",
                onClick = {}
            )

            Spacer(Modifier.height(16.dp))
            DrawerSectionHeader("PREFERENCES")
            
            var isDarkMode by remember { mutableStateOf(true) }
            DrawerItem(
                icon = Icons.Outlined.DarkMode,
                label = "Dark Mode",
                onClick = { isDarkMode = !isDarkMode },
                trailing = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { isDarkMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFDAD6),
                            checkedTrackColor = Color(0xFFFFDAD6).copy(alpha = 0.5f)
                        )
                    )
                }
            )
            
            DrawerItem(
                icon = Icons.Outlined.Language,
                label = "Language",
                onClick = {},
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "English",
                            fontFamily = ManropeFamily,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
            DrawerSectionHeader("SUPPORT")
            DrawerItem(
                icon = Icons.Outlined.Policy,
                label = "Privacy Policy",
                onClick = {}
            )
            DrawerItem(
                icon = Icons.Outlined.Email,
                label = "Contact",
                onClick = {}
            )
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "Log Out",
                onClick = {
                    onDrawerClose()
                    onLogOut()
                }
            )
        }

        // ── Footer ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Version 1.0.0",
                fontFamily = ManropeFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = ManropeFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFFFDAD6),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
        
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CashFlowTabContent(
    groups: List<CashFlowGroup>,
    searchQuery: String,
    selectedFilters: Set<String>,
    selectedDueOption: String?,
    onSearchQueryChange: (String) -> Unit,
    onToggleFilter: (String) -> Unit,
    onUpdateDueOption: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRecordLent: () -> Unit,
    onRecordBorrowed: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onRecordPayment: (TransactionEntity, Double, (Result<Unit>) -> Unit) -> Unit,
    hasAnyTransactions: Boolean
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var isForgiven by remember { mutableStateOf(false) }
    var isBlacklisted by remember { mutableStateOf(false) }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var showDueMenu by remember { mutableStateOf(false) }
    val dueOptions = remember { listOf("1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "1 Week", "2 Weeks") }

    if (transactionToDelete != null) {
        // ... (Delete Dialog Logic stays the same, it uses transactionToDelete)
        val t = transactionToDelete!!
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
                        "Are you sure you want to delete this transaction? This action cannot be undone.",
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
                    
                    if (t.transactionType == "lent") {
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
                val isEnabled = if (t.transactionType == "lent") isForgiven || isBlacklisted else isForgiven
                
                TextButton(
                    enabled = isEnabled,
                    onClick = {
                        onDeleteTransaction(t)
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
            .padding(
                horizontal = if (isLandscape) 12.dp else 24.dp,
                vertical = if (isLandscape) 8.dp else 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Search & Filter Row ──────────────────────────────────────────────
        if (hasAnyTransactions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Search Bar
                    AnimatedVisibility(
                        visible = isFilterExpanded,
                        enter = scaleIn(
                            initialScale = 0.7f,
                            transformOrigin = TransformOrigin(1f, 0.5f),
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
                        ) + fadeIn(),
                        exit = scaleOut(
                            targetScale = 0.7f,
                            transformOrigin = TransformOrigin(1f, 0.5f),
                            animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
                        ) + fadeOut(),
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(end = 8.dp),
                            placeholder = { 
                                Text("Search...", fontFamily = ManropeFamily, fontSize = 13.sp) 
                            },
                            leadingIcon = { 
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) 
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )
                    }

                    // Lens Icon
                    if (!isFilterExpanded) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(KumbukaColors.SurfaceContainerLow, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isFilterExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, null, tint = KumbukaColors.Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    // Filter Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isFilterExpanded) KumbukaColors.Primary else KumbukaColors.SurfaceContainerLow, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isFilterExpanded = !isFilterExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tune, 
                            null, 
                            tint = if (isFilterExpanded) Color.White else KumbukaColors.Primary, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Filter Criteria
                AnimatedVisibility(
                    visible = isFilterExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Lent", "Borrowed", "Due", "Overdue").forEach { criteria ->
                            val isSelected = criteria in selectedFilters
                            
                            if (criteria == "Due") {
                                Box {
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onToggleFilter("Due"); if (!isSelected) showDueMenu = true },
                                        label = {
                                            Text(if (isSelected && selectedDueOption != null) "Due: $selectedDueOption" else "Due", fontFamily = ManropeFamily, fontSize = 12.sp)
                                        },
                                        trailingIcon = { Icon(if (isSelected) Icons.Default.ArrowDropDown else Icons.Default.Add, null, modifier = Modifier.size(14.dp)) },
                                        shape = RoundedCornerShape(20.dp)
                                    )

                                    DropdownMenu(
                                        expanded = showDueMenu,
                                        onDismissRequest = { showDueMenu = false }
                                    ) {
                                        DropdownMenuItem(text = { Text("All Due") }, onClick = { onUpdateDueOption(null); showDueMenu = false })
                                        dueOptions.forEach { option ->
                                            DropdownMenuItem(text = { Text(option) }, onClick = { onUpdateDueOption(option); showDueMenu = false })
                                        }
                                    }
                                }
                            } else {
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleFilter(criteria) },
                                    label = { Text(criteria, fontFamily = ManropeFamily, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        if (selectedFilters.isNotEmpty()) {
                            Text("Clear", fontWeight = FontWeight.Bold, color = KumbukaColors.Error, modifier = Modifier.padding(start = 4.dp).clickable { onClearFilters() })
                        }
                    }
                }
            }
        }

        // ── Scrollable Upper Part ──────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            if (!hasAnyTransactions) {
                // Empty state handled by Parent logic now (it passes hasAnyTransactions)
                // but we keep this as fallback if needed
            } else if (groups.isEmpty()) {
                // Search result empty
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp), tint = KumbukaColors.Primary.copy(alpha = 0.15f))
                    Spacer(Modifier.height(16.dp))
                    Text("No matching records", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KumbukaColors.Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                ) {
                    items(
                        items = groups,
                        key = { it.key }
                    ) { group ->
                        Box(modifier = Modifier.animateItem()) {
                            TransactionItem(
                                transaction = group.representative.copy(
                                    amount = group.totalAmount, 
                                    balance = group.totalBalance,
                                    dateInMillis = group.latestDate
                                ),
                                isExpanded = expandedGroupId == group.key,
                                onExpandClick = {
                                    expandedGroupId = if (expandedGroupId == group.key) null else group.key
                                },
                                onEditClick = onEditTransaction,
                                onDeleteClick = { transactionToDelete = it },
                                onRecordPayment = onRecordPayment,
                                subItems = group.items
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Action Buttons ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = if (isLandscape) 0.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onRecordLent, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F0500).copy(alpha = 0.85f)), modifier = Modifier.weight(1f).height(if (isLandscape) 44.dp else 56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("+ Money Lent", fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Button(onClick = onRecordBorrowed, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB52614).copy(alpha = 0.85f)), modifier = Modifier.weight(1f).height(if (isLandscape) 44.dp else 56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("+ Money Borrowed", fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
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
    onRecordPayment: (TransactionEntity, Double, (Result<Unit>) -> Unit) -> Unit,
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
                            Color(0xFF3D0300).copy(alpha = 0.8f),
                            Color(0xFF5F0500).copy(alpha = 0.95f),
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
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
                ),
                modifier = Modifier.weight(1f)
            )

            PrivacyToggle(
                isVisible = isVisible,
                onToggle = { isVisible = !isVisible },
                tint = Color.White.copy(alpha = 0.9f),
                iconContainerSize = 65.dp
            )
        }

        val amountText = if (isVisible) "$prefix KSh ${String.format(locale, "%,.2f", transaction.balance)}" else "••••"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
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
                            if (isVisible) "KSh ${String.format(locale, "%,.2f", item.balance)}" else "••••",
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

                    // Row for Due Date and Edit/Delete
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

                    // Row for Balance and Record Payment Icon
                    if (item.remoteId != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isVisible) "Bal: KSh ${String.format(locale, "%,.2f", item.balance)} (${item.status.replace("_", " ")})" else "Bal: ••••",
                                fontFamily = ManropeFamily,
                                fontSize = 10.sp,
                                color = contentColor.copy(alpha = 0.6f)
                            )

                            if (item.balance > 0) {
                                var showPaymentDialog by remember { mutableStateOf(false) }
                                val context = androidx.compose.ui.platform.LocalContext.current

                                Button(
                                    onClick = { showPaymentDialog = true },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KumbukaColors.Primary.copy(alpha = 0.1f),
                                        contentColor = KumbukaColors.Primary
                                    ),
                                    elevation = null
                                ) {
                                    Text(
                                        "+ Payment",
                                        fontFamily = ManropeFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (showPaymentDialog) {
                                    var paymentAmount by remember { mutableStateOf("") }
                                    var isSubmitting by remember { mutableStateOf(false) }

                                    AlertDialog(
                                        onDismissRequest = { if (!isSubmitting) showPaymentDialog = false },
                                        title = { Text("Record Payment", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold) },
                                        text = {
                                            Column {
                                                Text("Enter amount paid for this record:", fontFamily = ManropeFamily)
                                                Spacer(Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = paymentAmount,
                                                    onValueChange = { paymentAmount = it },
                                                    label = { Text("Amount (KSh)") },
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
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
                                                        onRecordPayment(item, amt) { result ->
                                                            isSubmitting = false
                                                            if (result.isSuccess) {
                                                                android.widget.Toast.makeText(context, "Payment recorded", android.widget.Toast.LENGTH_SHORT).show()
                                                                showPaymentDialog = false
                                                            } else {
                                                                android.widget.Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
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
    iconContainerSize: androidx.compose.ui.unit.Dp = 65.dp,
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

