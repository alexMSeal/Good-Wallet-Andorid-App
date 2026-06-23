package com.androidstudio.myapplication.ui

import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.androidstudio.myapplication.model.Expense
import java.util.*
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import com.androidstudio.myapplication.model.HomeScreenViewModel


@Composable
fun ToggleButton(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row {
        options.forEach { option ->
            val isSelected = option == selected
            Button(
                onClick = { onSelected(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF4CAF50) else Color.LightGray,
                    contentColor = Color.White
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(option)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    existingExpense: Expense? = null,
    onSave: (Expense) -> Unit,
    navController: NavController,
    viewModel: HomeScreenViewModel = viewModel(),  // Add this parameter
    modifier: Modifier = Modifier,

) {
    val context = LocalContext.current

    // Default to today’s date if none provided
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var transaction by remember {
        mutableStateOf(existingExpense ?: Expense(date = today, method = "Cash"))
    }

    var amountInput by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }

    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
    val finalAmount = if (transaction.type == "Expense") -kotlin.math.abs(parsedAmount) else kotlin.math.abs(parsedAmount)
    val idToUse = transaction.id.ifEmpty { UUID.randomUUID().toString() }
    val toSave = transaction.copy(id = idToUse, amount = finalAmount)

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        transaction = transaction.copy(imageUri = uri?.toString() ?: "")
    }

    // Dropdown options
    val incomeCategories = listOf("Salary","Allowance","Bonus", "Investment")
    val expenseCategories = listOf("Food", "Transport", "Shopping", "Utilities", "Education", "Healthcare", "Entertainment")
    val expensePaymentMethods = listOf("Cash", "Credit Card", "Debit Card", "Split-Bill", "Online APP Payment")
    val incomePaymentMethods = listOf("Bank Transfer", "Cash", "Check", "Online Transfer")
    // UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFEFF5E9)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Fixed amount input
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Type: ")
                        Spacer(modifier = Modifier.width(8.dp))
                        ToggleButton(
                            options = listOf("Income", "Expense"),
                            selected = transaction.type,
                            onSelected = { transaction = transaction.copy(type = it, category = "") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DropdownSelector(
                        label = "Category",
                        options = if (transaction.type == "Income") incomeCategories else expenseCategories,
                        selected = transaction.category,
                        onSelected = { transaction = transaction.copy(category = it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DropdownSelector(
                        label = "Payment Method",
                        options = if (transaction.type == "Income") incomePaymentMethods else expensePaymentMethods,
                        selected = transaction.method,
                        onSelected = { transaction = transaction.copy(method = it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = transaction.caption,
                        onValueChange = { transaction = transaction.copy(caption = it) },
                        label = { Text("Caption") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date picker with calendar icon
                    val calendar = Calendar.getInstance()
                    val datePickerDialog = remember {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                transaction = transaction.copy(date = formatted)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                    }

                    OutlinedTextField(
                        value = transaction.date,
                        onValueChange = {},
                        label = { Text("Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Image Picker
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            imagePickerLauncher.launch("image/*")
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Upload Image")
                        }
                        Text("Upload Receipt (optional)")
                    }

                    if (!transaction.imageUri.isNullOrEmpty()) {
                        Box(modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = transaction.imageUri,
                                contentDescription = "Transaction Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { transaction = transaction.copy(imageUri = "") },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.Red)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    val parsedAmount = amountInput.toDoubleOrNull()

                    if (transaction.category.isBlank() || parsedAmount == null) {
                        Toast.makeText(context, "Please enter a valid amount and category", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val selectedAlbum = viewModel.selectedAlbum.value
                    transaction = transaction.copy(album = selectedAlbum)

                    val finalAmount = kotlin.math.abs(parsedAmount)
                    val safeId = transaction.id.takeIf { !it.isNullOrEmpty() } ?: UUID.randomUUID().toString()
                    val toSave = transaction.copy(id = safeId, amount = finalAmount)


                    //Check if this is a new or existing transaction
                    val exists = viewModel.expenseList.any { it.id == transaction.id }

                    if (exists) {
                        viewModel.updateTransaction(toSave)
                        Log.d("EditTransaction", "Updated transaction: $toSave")
                    } else {
                        viewModel.addTransaction(toSave)
                        Log.d("EditTransaction", "Added new transaction: $toSave")
                    }

                    navController.popBackStack()
                    Log.d("EditTransaction", "Saving transaction: $toSave")
                    Log.d("EditTransaction", "Expense list after save: ${viewModel.expenseList.size}")

                },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color(0xFFEFF5E9)
                    )
                ) {
                    Text("Save")
                }


            }
        }
    }
}
