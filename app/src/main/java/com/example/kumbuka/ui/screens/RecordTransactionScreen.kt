package com.example.kumbuka.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kumbuka.ui.components.KumbukaLogo
import com.example.kumbuka.ui.theme.KumbukaColors
import com.example.kumbuka.ui.theme.ManropeFamily
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionScreen(
    transactionType: String, // "lent" or "borrowed"
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateInMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isLent = transactionType.lowercase() == "lent"
    val primaryColor = if (isLent) KumbukaColors.Primary else KumbukaColors.Secondary
    val titleText = if (isLent) "I Lent Money" else "I Borrowed Money"

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
                    onClick = onNavigateBack,
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
                    onValueChange = { name = it },
                    label = "Full Name",
                    placeholder = "Enter name",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
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
                    onValueChange = { phoneNumber = it },
                    label = "Contact Number",
                    placeholder = "0700 000 000",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
                Spacer(Modifier.height(20.dp))

                // ── AMOUNT FIELD ─────────────────────────────────────────────
                RecordTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    placeholder = "KES 0.00",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
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

                Spacer(Modifier.height(48.dp))

                // ── SAVE BUTTON ──────────────────────────────────────────────
                Button(
                    onClick = { onSaveSuccess() },
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
            singleLine = true,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KumbukaColors.Primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = KumbukaColors.SurfaceContainerLow,
                unfocusedContainerColor = KumbukaColors.SurfaceContainerLow
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
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
