package app.kumbuka.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.kumbuka.ui.components.KumbukaLogo
import app.kumbuka.ui.theme.KumbukaColors
import app.kumbuka.ui.theme.ManropeFamily
import app.kumbuka.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionScreen(
    transactionType: String, // "lent" or "borrowed"
    transactionId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateInMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dueDateInMillis by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.getTransactionById(transactionId) { transaction ->
                if (transaction != null) {
                    name = transaction.name
                    phoneNumber = transaction.phoneNumber
                    amount = transaction.amount.toString()
                    dateInMillis = transaction.dateInMillis
                    dueDateInMillis = transaction.dueDateInMillis
                    notes = transaction.notes
                }
            }
        }
    }

    // ── VALIDATION STATE ──────────────────────────────────────────────────────
    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var dueDateError by remember { mutableStateOf(false) }

    fun hasChanges(): Boolean {
        return name.isNotBlank() || phoneNumber.isNotBlank() || amount.isNotBlank() || 
               notes.isNotBlank() || dueDateInMillis != null
    }

    fun validateFields(): Boolean {
        nameError = name.isBlank()
        phoneError = phoneNumber.isBlank()
        amountError = amount.isBlank() || amount.toDoubleOrNull() == null
        // Due date is optional — removed the mandatory check
        val isValid = !nameError && !phoneError && !amountError
        
        if (!isValid) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
        }
        return isValid
    }

    fun handleBack() {
        if (hasChanges()) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        handleBack()
    }

    val isLent = transactionType.lowercase() == "lent"
    val primaryColor = if (isLent) Color(0xFF5F0500) else Color(0xFFB52614)
    val titleText = if (isLent) "I Lent" else "I Owe"

    // ── DISCARD DIALOG ────────────────────────────────────────────────────────
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    "Unsaved Changes",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "You have unsaved changes. Discard them?",
                    fontFamily = ManropeFamily,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text("Discard", color = KumbukaColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing", color = primaryColor)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── CONTACT PICKER ────────────────────────────────────────────────────────
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            contactUri?.let { uri ->
                val (pickedName, pickedPhone) = queryContactDetails(context, uri)
                if (pickedName != null) name = pickedName
                if (pickedPhone != null) phoneNumber = pickedPhone
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }
    }

    // ── DATE PICKER ──────────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK", color = primaryColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── DUE DATE PICKER ──────────────────────────────────────────────────────
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateInMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateInMillis = datePickerState.selectedDateMillis
                    if (dueDateInMillis != null) dueDateError = false
                    showDueDatePicker = false
                }) { Text("OK", color = primaryColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumbukaColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // ── TOP BAR ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(KumbukaColors.SurfaceContainerLowest)
                    .statusBarsPadding()
            ) {
                IconButton(
                    onClick = { handleBack() },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KumbukaColors.OnSurface)
                }
                KumbukaLogo(size = 56.dp, modifier = Modifier.align(Alignment.Center))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 48.dp)
            ) {
                Text(
                    text = titleText,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = primaryColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Record the details of this commitment below.",
                    fontFamily = ManropeFamily,
                    fontSize = 16.sp,
                    color = KumbukaColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(40.dp))

                // ── NAME FIELD ───────────────────────────────────────────────
                RecordTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = "Full Name",
                    placeholder = "Enter name",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    primaryColor = primaryColor,
                    isError = nameError,
                    trailingIcon = {
                        IconButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                            Icon(Icons.Default.ContactPage, "Select from contacts", tint = primaryColor)
                        }
                    }
                )
                Spacer(Modifier.height(20.dp))

                // ── PHONE FIELD ──────────────────────────────────────────────
                RecordTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        phoneNumber = it
                        if (it.isNotBlank()) phoneError = false
                    },
                    label = "Contact Number",
                    placeholder = "0700 000 000",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                    isError = phoneError,
                    primaryColor = primaryColor
                )
                Spacer(Modifier.height(20.dp))

                // ── AMOUNT FIELD ─────────────────────────────────────────────
                RecordTextField(
                    value = amount,
                    onValueChange = { 
                        amount = it
                        if (it.isNotBlank() && it.toDoubleOrNull() != null) amountError = false
                    },
                    label = "Amount",
                    placeholder = "KES 0.00",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    isError = amountError,
                    primaryColor = primaryColor
                )
                Spacer(Modifier.height(20.dp))

                // ── DATE FIELD ───────────────────────────────────────────────
                Column {
                    Text(
                        "Date", fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp, color = KumbukaColors.OnSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dateInMillis)),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, "Select date", tint = primaryColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = KumbukaColors.SurfaceContainerLow,
                            unfocusedContainerColor = KumbukaColors.SurfaceContainerLow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── DUE DATE FIELD ───────────────────────────────────────────
                Column {
                    Text(
                        "Due Date (Optional)", fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp, color = KumbukaColors.OnSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = dueDateInMillis?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Select due date (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        isError = dueDateError,
                        trailingIcon = {
                            IconButton(onClick = { showDueDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, "Select due date", tint = primaryColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = if (dueDateError) KumbukaColors.Error else Color.Transparent,
                            errorBorderColor = KumbukaColors.Error,
                            focusedContainerColor = KumbukaColors.SurfaceContainerLow,
                            unfocusedContainerColor = KumbukaColors.SurfaceContainerLow,
                            errorContainerColor = KumbukaColors.SurfaceContainerLow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── NOTES FIELD ──────────────────────────────────────────────
                RecordTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes",
                    placeholder = "Add any additional details...",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    singleLine = false,
                    primaryColor = primaryColor
                )

                Spacer(Modifier.height(48.dp))

                // ── SAVE BUTTON ──────────────────────────────────────────────
                Button(
                    onClick = {
                        if (validateFields()) {
                            viewModel.saveTransaction(
                                id = transactionId ?: 0L,
                                name = name,
                                phoneNumber = phoneNumber,
                                amount = amount,
                                dateInMillis = dateInMillis,
                                dueDateInMillis = dueDateInMillis,
                                notes = notes,
                                transactionType = transactionType,
                                onSuccess = {
                                    Toast.makeText(context, "Record Saved Successfully", Toast.LENGTH_SHORT).show()
                                    onSaveSuccess()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        "Save Record",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    singleLine: Boolean = true,
    isError: Boolean = false,
    primaryColor: Color = KumbukaColors.Primary,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, color = KumbukaColors.OnSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontFamily = ManropeFamily, color = KumbukaColors.Outline) },
            singleLine = singleLine,
            trailingIcon = trailingIcon,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = if (isError) KumbukaColors.Error else Color.Transparent,
                errorBorderColor = KumbukaColors.Error,
                focusedContainerColor = KumbukaColors.SurfaceContainerLow,
                unfocusedContainerColor = KumbukaColors.SurfaceContainerLow,
                errorContainerColor = KumbukaColors.SurfaceContainerLow
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (singleLine) Modifier.height(56.dp)
                    else Modifier.heightIn(min = 56.dp, max = 120.dp)
                )
        )
    }
}

@SuppressLint("Range")
private fun queryContactDetails(context: Context, contactUri: Uri): Pair<String?, String?> {
    var name: String? = null
    var phone: String? = null
    val contentResolver = context.contentResolver
    contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
        }
    }
    return name to phone
}
