package com.example.kumbuka.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kumbuka.ui.components.KumbukaLogo
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.ManropeFamily
import com.example.kumbuka.viewmodel.AuthState
import com.example.kumbuka.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SignUpScreen
//
// Primary landing screen for unauthenticated users.
// Scenarios handled:
//   • New user         → fills form → "Get Started" → Firebase creates account
//   • Returning user   → taps "Log in" link → LoginScreen
//   • Already logged in → never reaches this screen (NavGraph routes to Home)
//   • Error cases      → animated error banner with Firebase error message
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignUpScreen(
    onSignUpSuccess:   () -> Unit,   // → HomeScreen
    onNavigateToLogin: () -> Unit    // → LoginScreen (back arrow OR "Log in" link)
) {
    val focusManager = LocalFocusManager.current

    // ── ViewModel wired to Firebase ───────────────────────────────────────────
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()

    // ── Form fields ───────────────────────────────────────────────────────────
    var fullName        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted   by remember { mutableStateOf(false) }

    // ── Derived UI state ──────────────────────────────────────────────────────
    val isLoading = authState is AuthState.Loading
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── React to Firebase auth state changes ──────────────────────────────────
    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> {
                viewModel.resetState()
                onSignUpSuccess()
            }
            is AuthState.Error -> errorMessage = s.message
            else               -> {}
        }
    }

    // ── Reset ViewModel state when screen first appears ───────────────────────
    LaunchedEffect(Unit) { viewModel.resetState() }

    // ── Validation ────────────────────────────────────────────────────────────
    fun isEmailValid(e: String)   = android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()
    fun isPhoneValid(p: String)   = p.length >= 7 && p.all { it.isDigit() || it in "+- " }
    fun isPasswordStrong(p: String) = p.length >= 8

    fun validate(): Boolean {
        return when {
            fullName.isBlank()          -> { errorMessage = "Please enter your full name."; false }
            !isEmailValid(email)        -> { errorMessage = "Please enter a valid email address."; false }
            !isPhoneValid(phone)        -> { errorMessage = "Please enter a valid phone number."; false }
            !isPasswordStrong(password) -> { errorMessage = "Password must be at least 8 characters."; false }
            !termsAccepted              -> { errorMessage = "You must accept the Terms and Conditions."; false }
            else                        -> { errorMessage = null; true }
        }
    }

    // ── Submit ────────────────────────────────────────────────────────────────
    fun onGetStartedClicked() {
        focusManager.clearFocus()
        if (!validate()) return
        errorMessage = null
        viewModel.signUp(email, password)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumbukaColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header: back arrow ────────────────────────────────────────────
            SignUpTopBar(onBack = onNavigateToLogin)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 48.dp)
            ) {


                // ── Headings ──────────────────────────────────────────────────
                Text(
                    text       = "Create Account",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 32.sp,
                    color      = KumbukaColors.Primary,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Join Kumbuka to start managing your commitments.",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 16.sp,
                    color      = KumbukaColors.OnSurfaceVariant,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(40.dp))

                // ── Fields ────────────────────────────────────────────────────
                SignUpTextField(
                    value         = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    label         = "Full Name",
                    placeholder   = "John Doe",
                    keyboardType  = KeyboardType.Text,
                    imeAction     = ImeAction.Next,
                    onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                    isError       = errorMessage != null && fullName.isBlank()
                )
                Spacer(Modifier.height(20.dp))

                SignUpTextField(
                    value         = email,
                    onValueChange = { email = it; errorMessage = null },
                    label         = "Email Address",
                    placeholder   = "name@example.com",
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Next,
                    onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                    isError       = errorMessage != null && !isEmailValid(email) && email.isNotBlank()
                )
                Spacer(Modifier.height(20.dp))

                SignUpTextField(
                    value         = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label         = "Phone Number",
                    placeholder   = "+254 700 000 000",
                    keyboardType  = KeyboardType.Phone,
                    imeAction     = ImeAction.Next,
                    onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                    isError       = errorMessage != null && !isPhoneValid(phone) && phone.isNotBlank()
                )
                Spacer(Modifier.height(20.dp))

                SignUpPasswordField(
                    password         = password,
                    onPasswordChange = { password = it; errorMessage = null },
                    visible          = passwordVisible,
                    onToggleVisible  = { passwordVisible = !passwordVisible },
                    onDone           = { onGetStartedClicked() },
                    isError          = errorMessage != null && !isPasswordStrong(password) && password.isNotBlank()
                )
                Spacer(Modifier.height(20.dp))

                TermsCheckbox(
                    checked   = termsAccepted,
                    onChecked = { termsAccepted = it; errorMessage = null },
                    isError   = errorMessage?.contains("Terms") == true
                )

                // ── Error banner ──────────────────────────────────────────────
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumbukaColors.ErrorContainer)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Warning, null,
                                tint = KumbukaColors.Error,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, fontFamily = ManropeFamily,
                                fontSize = 12.sp, color = KumbukaColors.Error)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Get Started button ────────────────────────────────────────
                Button(
                    onClick  = { onGetStartedClicked() },
                    enabled  = !isLoading,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = KumbukaColors.Primary,
                        contentColor           = KumbukaColors.OnPrimary,
                        disabledContainerColor = KumbukaColors.Primary.copy(alpha = 0.6f)
                    ),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(22.dp),
                            color = KumbukaColors.OnPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Get Started", fontFamily = ManropeFamily,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                            modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── "Already have an account? Log in" ─────────────────────────
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Already have an account? ",
                        fontFamily = ManropeFamily, fontSize = 16.sp,
                        color = KumbukaColors.OnSurfaceVariant)
                    Text("Log in",
                        fontFamily = ManropeFamily, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = KumbukaColors.Primary,
                        modifier = Modifier.clickable { onNavigateToLogin() })
                }
            }

            // ── Decorative gradient strip ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                KumbukaColors.SurfaceContainerHigh.copy(alpha = 0.35f)
                            )
                        )
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SignUpTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)                         // matches LoginTopBar height
            .background(KumbukaColors.SurfaceContainerLowest)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                tint = KumbukaColors.OnSurface)
        }
        KumbukaLogo(
            size     = 56.dp,                       // matches LoginTopBar logo size
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SignUpTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, placeholder: String,
    keyboardType: KeyboardType, imeAction: ImeAction,
    onImeAction: () -> Unit, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, color = KumbukaColors.OnSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, fontFamily = ManropeFamily,
                    fontSize = 16.sp, color = KumbukaColors.Outline)
            },
            singleLine = true, isError = isError,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = KumbukaColors.Primary,
                unfocusedBorderColor    = Color.Transparent,
                focusedContainerColor   = KumbukaColors.SurfaceContainerLow,
                unfocusedContainerColor = KumbukaColors.SurfaceContainerLow,
                errorContainerColor     = KumbukaColors.ErrorContainer,
                errorBorderColor        = KumbukaColors.Error
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}

@Composable
private fun SignUpPasswordField(
    password: String, onPasswordChange: (String) -> Unit,
    visible: Boolean, onToggleVisible: () -> Unit,
    onDone: () -> Unit, isError: Boolean
) {
    SignUpTextField(
        value = password, onValueChange = onPasswordChange,
        label = "Password", placeholder = "••••••••",
        keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
        onImeAction = onDone, isError = isError,
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    if (visible) "Hide password" else "Show password",
                    tint = KumbukaColors.Outline
                )
            }
        }
    )
}

@Composable
private fun TermsCheckbox(checked: Boolean, onChecked: (Boolean) -> Unit, isError: Boolean) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (isError) Modifier.border(1.dp, KumbukaColors.Error, RoundedCornerShape(8.dp)) else Modifier)
            .padding(4.dp)
    ) {
        Checkbox(
            checked = checked, onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(
                checkedColor   = KumbukaColors.Secondary,
                uncheckedColor = if (isError) KumbukaColors.Error else KumbukaColors.Outline,
                checkmarkColor = KumbukaColors.OnSecondary
            )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = ManropeFamily, fontSize = 14.sp,
                    color = KumbukaColors.OnSurfaceVariant)) { append("I agree to the ") }
                withStyle(SpanStyle(fontFamily = ManropeFamily, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = KumbukaColors.Secondary)) { append("Terms and Conditions") }
                withStyle(SpanStyle(fontFamily = ManropeFamily, fontSize = 14.sp,
                    color = KumbukaColors.OnSurfaceVariant)) { append(" and Privacy Policy") }
            },
            modifier = Modifier.padding(top = 10.dp).clickable { onChecked(!checked) }
        )
    }
}