package com.example.kumbuka.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kumbuka.ui.components.KumbukaLogo
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.ManropeFamily
import com.example.kumbuka.viewmodel.HomeTab
import com.example.kumbuka.viewmodel.HomeViewModel
import com.example.kumbuka.viewmodel.TabState
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Layout constants — same straddling pattern used across all screens
// ─────────────────────────────────────────────────────────────────────────────
private val TOP_BAR_HEIGHT = 64.dp
private val LOGO_CIRCLE    = 80.dp
private val LOGO_HALF      = LOGO_CIRCLE / 2   // 40.dp

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onLogOut:              () -> Unit,
    onNavigateToCircles:   () -> Unit = {},
    onNavigateToActivity:  () -> Unit = {},
    onNavigateToSettings:  () -> Unit = {},
    onNavigateToRecordLent: () -> Unit = {},
    onNavigateToRecordBorrowed: () -> Unit = {},
    onGetStarted:          () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ── Drawer state ──────────────────────────────────────────────────────────
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // ── Collect ViewModel state ───────────────────────────────────────────────
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabState by viewModel.tabState.collectAsState()

    // ── Reset ViewModel on first appearance ───────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.resetState()
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
                    // ── Top bar — hamburger (left) + empty centre + empty right ───
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
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
                // Centre sits exactly on the top-bar bottom edge
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Tab content — varies based on selectedTab ──────────────────
                when {
                    tabState is TabState.Loading -> {
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
                    tabState is TabState.Error -> {
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
                        if (selectedTab == HomeTab.Dashboard) {
                            EmptyDashboard()
                        } else {
                            CashFlowTabContent(
                                onRecordLent = onNavigateToRecordLent,
                                onRecordBorrowed = onNavigateToRecordBorrowed
                            )
                        }
                    }
                }
            }
        }
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
    onRecordLent: () -> Unit,
    onRecordBorrowed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Empty state feel (matching Dashboard) ─────────────────────────────
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
            fontSize   = 20.sp,
            color      = KumbukaColors.Primary,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text       = "Record your first transaction to see your lending and borrowing trends here.",
            fontFamily = ManropeFamily,
            fontSize   = 15.sp,
            color      = KumbukaColors.OnSurfaceVariant,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = 24.dp),
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(60.dp))

        // ── Action Buttons ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick  = onRecordLent,
                colors   = ButtonDefaults.buttonColors(containerColor = KumbukaColors.Primary),
                modifier = Modifier.weight(1f).height(56.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("+ Money Lent", fontFamily = ManropeFamily, 
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Button(
                onClick  = onRecordBorrowed,
                colors   = ButtonDefaults.buttonColors(containerColor = KumbukaColors.Secondary),
                modifier = Modifier.weight(1f).height(56.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("+ Money Borrowed", fontFamily = ManropeFamily, 
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty States
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyDashboard() {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
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
            text       = "Your ledger is a clean slate",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 20.sp,
            color      = KumbukaColors.Primary,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text       = "When you start lending or borrowing within your circles, your activity and balances will appear here.",
            fontFamily = ManropeFamily,
            fontSize   = 15.sp,
            color      = KumbukaColors.OnSurfaceVariant,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = 48.dp),
            lineHeight = 24.sp
        )
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
