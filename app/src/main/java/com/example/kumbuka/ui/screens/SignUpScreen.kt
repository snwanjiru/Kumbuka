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
import androidx.hilt.navigation.compose.hiltViewModel               // CHANGED: was viewModel
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
//   • New user         → fills form → "Get Started" → Spring Boot creates account
//   • Returning user   → taps "Log in" link → LoginScreen
//   • Already logged in → never reaches this screen (NavGraph routes to Home)
//   • Error cases      → animated error banner with Spring Boot error message
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignUpScreen(
    onSignUpSuccess:   () -> Unit,
    onNavigateToLogin: () -> Unit,
    // CHANGED: ViewModel is now a parameter with a default value.
    //
    // WHY: makes this screen testable. In production NavGraph calls
    // SignUpScreen() without passing viewModel — hiltViewModel() is used.
    // In tests, a fake ViewModel is passed in directly:
    //   SignUpScreen(onSignUpSuccess = {}, onNavigateToLogin = {}, viewModel = fakeVm)
    //
    // The default must be hiltViewModel() not viewModel() because AuthViewModel
    // now has an @Inject constructor that requires AuthRepository. viewModel()
    // cannot satisfy that — only hiltViewModel() can.
    viewModel: AuthViewModel = hiltViewModel()                      // CHANGED
) {
    val focusManager = LocalFocusManager.current

    // REMOVED: val viewModel: AuthViewModel = viewModel()
    // The ViewModel is now received as a parameter above.
    val authState by viewModel.authState.collectAsState()

    // ─────────────────────────────────────────────────────────────────────────────
    // FORM FIELDS
    // ─────────────────────────────────────────────────────────────────────────────

    var name        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted   by remember { mutableStateOf(false) }

    // ─────────────────────────────────────────────────────────────────────────────
    // UI state
    // ─────────────────────────────────────────────────────────────────────────────

    val isLoading    = authState is AuthState.Loading
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ─────────────────────────────────────────────────────────────────────────────
    // REACT TO AUTH STATE CHANGES
    // ─────────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────────

    fun isEmailValid(e: String)     = android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()
    fun isPhoneValid(p: String)     = p.length >= 7 && p.all { it.isDigit() || it in "+- " }
    fun isPasswordStrong(p: String) = p.length >= 8

    fun validate(): Boolean {
        return when {
            name.isBlank()          -> { errorMessage = "Please enter your full name."; false }
            !isEmailValid(email)        -> { errorMessage = "Please enter a valid email address."; false }
            !isPhoneValid(phone)        -> { errorMessage = "Please enter a valid phone number."; false }
            !isPasswordStrong(password) -> { errorMessage = "Password must be at least 8 characters."; false }
             // blank check comes before mismatch check
            confirmPassword.isBlank()         -> { errorMessage = "Please confirm your password."; false }
            confirmPassword != password       -> { errorMessage = "Passwords do not match."; false }
            // !termsAccepted              -> { errorMessage = "You must accept the Terms and Conditions."; false }
            else                        -> { errorMessage = null; true }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SUBMIT
    // ─────────────────────────────────────────────────────────────────────────────

    fun onGetStartedClicked() {
        focusManager.clearFocus()
        if (!validate()) return
        errorMessage = null
        // CHANGED: was viewModel.signUp(email, password)
        // Now passes all four fields that Spring Boot's /register endpoint expects.
        // name and phone were already collected by the form but were previously
        // dropped before reaching the API call. They are now included.
        viewModel.signUp(
            name = name,
            email    = email,
            phone    = phone,
            password = password,
            confirmPassword = confirmPassword
        )
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
            SignUpTopBar(onBack = onNavigateToLogin)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 48.dp)
            ) {
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

                // ─────────────────────────────────────────────────────────────────────────────
                // NAME
                // ─────────────────────────────────────────────────────────────────────────────

                SignUpTextField(
                    value         = name,
                    onValueChange = { name = it; errorMessage = null },
                    label         = "Name",
                    placeholder   = "John Doe",
                    keyboardType  = KeyboardType.Text,
                    imeAction     = ImeAction.Next,
                    onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                    isError       = errorMessage != null && name.isBlank()
                )
                Spacer(Modifier.height(20.dp))

                // ─────────────────────────────────────────────────────────────────────────────
                // EMAIL
                // ─────────────────────────────────────────────────────────────────────────────

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

                // ─────────────────────────────────────────────────────────────────────────────
                // PHONE
                // ─────────────────────────────────────────────────────────────────────────────

                SignUpTextField(
                    value         = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label         = "Phone Number",
                    placeholder   = "0700 000 000",
                    keyboardType  = KeyboardType.Phone,
                    imeAction     = ImeAction.Next,
                    onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                    isError       = errorMessage != null && !isPhoneValid(phone) && phone.isNotBlank()
                )
                Spacer(Modifier.height(20.dp))

                // ─────────────────────────────────────────────────────────────────────────────
                // PASSWORD
                // onDone moves focus to Confirm Password, not submit
                // ─────────────────────────────────────────────────────────────────────────────

                SignUpPasswordField(
                    label            = "Password",
                    password         = password,
                    onPasswordChange = { password = it; errorMessage = null },
                    visible          = passwordVisible,
                    onToggleVisible  = { passwordVisible = !passwordVisible },
                    imeAction = ImeAction.Next,
                    onDone           = { focusManager.moveFocus(FocusDirection.Down) },
                    isError          = errorMessage != null && !isPasswordStrong(password) && password.isNotBlank()
                )
                Spacer(Modifier.height(20.dp))

                // ─────────────────────────────────────────────────────────────────────────────
                // CONFIRM PASSWORD
                // ─────────────────────────────────────────────────────────────────────────────

                SignUpPasswordField(
                    label            = "Confirm Password",
                    password         = confirmPassword,
                    onPasswordChange = { confirmPassword = it; errorMessage = null },
                    visible          = confirmPasswordVisible,
                    onToggleVisible  = { confirmPasswordVisible = !confirmPasswordVisible },
                    imeAction = ImeAction.Done,
                    onDone           = { onGetStartedClicked() },
                    isError          = errorMessage != null && confirmPassword.isNotBlank() && confirmPassword !=password
                )
                Spacer(Modifier.height(20.dp))

                // ─────────────────────────────────────────────────────────────────────────────
                // TERMS & CONDITIONS
                // ─────────────────────────────────────────────────────────────────────────────

                /*
                TermsCheckbox(
                    checked   = termsAccepted,
                    onChecked = { termsAccepted = it; errorMessage = null },
                    isError   = errorMessage?.contains("Terms") == true
                )
                */

                // ─────────────────────────────────────────────────────────────────────────────
                // ERROR BANNER
                // ─────────────────────────────────────────────────────────────────────────────

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
                                tint     = KumbukaColors.Error,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, fontFamily = ManropeFamily,
                                fontSize = 12.sp, color = KumbukaColors.Error)
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────────────────────
                // GET STARTED BUTTON
                // ─────────────────────────────────────────────────────────────────────────────


                Spacer(Modifier.height(24.dp))

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

                // ─────────────────────────────────────────────────────────────────────────────
                // "Already have an account? Log in"
                // ─────────────────────────────────────────────────────────────────────────────

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Already have an account? ",
                        fontFamily = ManropeFamily,
                        fontSize = 16.sp,
                        color = KumbukaColors.OnSurfaceVariant)
                    Text("Log in",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = KumbukaColors.Primary,
                        modifier = Modifier.clickable { onNavigateToLogin() })
                }
            }

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
            .height(100.dp)
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
            size     = 56.dp,
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
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// label - allows both "Password" and "Confirm Password" to reuse this composable without duplicating
//code. Defaults to "Password" so any call site that doesn't pass it works correctly.

// imeAction - "Password" field uses ImeAction.Next (moves focus to Confirm).
// "Confirm Password" field uses ImeAction.Done (submits the form).
// ─────────────────────────────────────────────────────────────────────────────


@Composable
private fun SignUpPasswordField(
    label: String = "Password",
    password: String,
    onPasswordChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit,
    isError: Boolean
) {
    SignUpTextField(
        value                = password,
        onValueChange        = onPasswordChange,
        label                = label,
        placeholder          = "••••••••",
        keyboardType         = KeyboardType.Password,
        imeAction            = imeAction,
        onImeAction          = onDone,
        isError              = isError,
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
            .then(
                if (isError) Modifier.border(1.dp, KumbukaColors.Error, RoundedCornerShape(8.dp))
                else Modifier
            )
            .padding(4.dp)
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = CheckboxDefaults.colors(
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
                    fontWeight = FontWeight.Bold,
                    color = KumbukaColors.Secondary)) { append("Terms and Conditions") }
                withStyle(SpanStyle(fontFamily = ManropeFamily, fontSize = 14.sp,
                    color = KumbukaColors.OnSurfaceVariant)) { append(" and Privacy Policy") }
            },
            modifier = Modifier
                .padding(top = 10.dp)
                .clickable { onChecked(!checked) }
        )
    }
}