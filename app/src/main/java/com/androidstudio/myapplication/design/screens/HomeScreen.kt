package com.androidstudio.myapplication.design.screens


import android.app.Application
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.androidstudio.myapplication.model.HomeScreenViewModel
import java.util.*
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.androidstudio.myapplication.design.navgraph.NavRoutes
import com.androidstudio.myapplication.model.Expense
import com.androidstudio.myapplication.model.SettingsViewModel
import com.androidstudio.myapplication.model.SettingsViewModelFactory
import java.util.Locale
import java.text.NumberFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.androidstudio.myapplication.datastore.DataStoreManager


fun getMonthName(monthIndex: Int): String {
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(
        Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex) }.time
    )
}

val currencyMap = mapOf(
    "USD" to Pair("$", true),
    "EUR" to Pair("€", false),
    "JPY" to Pair("¥", true),
    "GBP" to Pair("£", true),
    "IDR" to Pair("Rp", true),
    "MYR" to Pair("RM", true),
    "INR" to Pair("₹", false),
    "PHP" to Pair("₱", false),
    "RUB" to Pair("₽", true),
    "NTD" to Pair("NT$", true),
    "CNY" to Pair("¥", true),
    "KOR" to Pair("₩", true),
    "SGD" to Pair("SG$", true),
    "VND" to Pair("₫", false),
    "THB" to Pair("฿", false),
    "AUD" to Pair("AU$", true),
    "CAD" to Pair("CA$", true),
    "NZD" to Pair("NZ$", true),
    "HKD" to Pair("HK$", true)
)

fun formatAmountWithSymbol(amount: Double, currencyCode: String?, currencyPlacement: Boolean? = null): String {
    val safeCode = currencyCode?.uppercase()?.takeIf { it.length == 3 && it.all(Char::isLetter) } ?: "USD"
    val (defaultSymbol, defaultPlacement) = currencyMap[safeCode] ?: Pair("$", true)

    val placeBefore = currencyPlacement ?: defaultPlacement
    val symbol = defaultSymbol

    val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
        isGroupingUsed = true
    }
    val formattedAmount = nf.format(amount)

    return if (placeBefore) "$symbol$formattedAmount" else "$formattedAmount$symbol"
}


@Composable
fun DropdownMenuSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedOption)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.toList().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }

    }
}


@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = viewModel()
) {
    var showFilterPopup by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val application = context.applicationContext as Application
    val dataStoreManager = remember { DataStoreManager(context) }
    val settingsFactory = remember { SettingsViewModelFactory(application, dataStoreManager) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)

    // UI states
    val profileName by settingsViewModel.profileName.collectAsState()
    val profileImageUri by settingsViewModel.profileImageUri.collectAsState()
    val currencyCode by settingsViewModel.currencyCode.collectAsState()
    val currencyPlacement by settingsViewModel.currencyPlacement.collectAsState()

    val transactions = viewModel.expenseList
    val selectedAlbum by viewModel.selectedAlbum

    //FOR FILTER OPTIONS IN THE EXPENSE LIST
    var filterType by remember { mutableStateOf("All") }
    var filterCategory by remember { mutableStateOf("All") }
    var filterPaymentMethod by remember { mutableStateOf("All") }

    val calendar = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val formatAmount = { amount: Double -> formatAmountWithSymbol(amount, currencyCode, currencyPlacement) }

    val filtered = remember(
        transactions, selectedMonth, selectedYear, selectedAlbum,
        filterType, filterCategory, filterPaymentMethod
    ) {
        transactions.filter {
            try {
                val cal = Calendar.getInstance()
                val parsed = dateFormat.parse(it.date)
                val matchesDate = parsed != null && cal.run {
                    time = parsed
                    get(Calendar.MONTH) == selectedMonth && get(Calendar.YEAR) == selectedYear
                }
                val matchesAlbum = it.album == selectedAlbum
                val matchesType = filterType == "All" || it.type == filterType
                val matchesCategory = filterCategory == "All" || it.category == filterCategory
                val matchesPayment = filterPaymentMethod == "All" || it.method == filterPaymentMethod

                matchesDate && matchesAlbum && matchesType && matchesCategory && matchesPayment
            } catch (e: Exception) {
                false
            }
        }
    }

    val monthlyIncome = filtered.filter { it.type == "Income" }.sumOf { it.amount }
    val monthlyExpense = filtered.filter { it.type == "Expense" }.sumOf { it.amount }
    val monthlyBalance = monthlyIncome - monthlyExpense

    val grouped = filtered.groupBy {
        try {
            dateFormat.format(dateFormat.parse(it.date) ?: return@groupBy it.date)
        } catch (e: Exception) {
            it.date
        }
    }.toSortedMap(compareByDescending { it })

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // --- PROFILE CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth(),
//                .clickable { navController.navigate(NavRoutes.Settings.route) },
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8FCE8))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!profileImageUri.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Default", modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Welcome, $profileName", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- BALANCE CARD ---
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
            Box(
                Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFAFFF84),
                                Color(0xFFE0FFC4)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        selectedAlbum ?: "No Wallet Selected",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Total Balance:", fontSize = 20.sp, color = Color.DarkGray)
                    Text(
                        formatAmount(monthlyBalance),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoItem("Income", monthlyIncome, Icons.Default.ArrowUpward, Color(0xFF2E7D32), formatAmount)
                        InfoItem("Expenses", monthlyExpense, Icons.Default.ArrowDownward, Color.Red, formatAmount)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- MONTH NAVIGATION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Arrow
            IconButton(onClick = {
                if (selectedMonth == 0) {
                    selectedMonth = 11
                    selectedYear--
                } else selectedMonth--
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
            }

            // Month & Year Dropdowns
            Row(verticalAlignment = Alignment.CenterVertically) {
                var expandedMonth by remember { mutableStateOf(false) }
                var expandedYear by remember { mutableStateOf(false) }

                Box {
                    Text(
                        getMonthName(selectedMonth),
                        modifier = Modifier
                            .clickable { expandedMonth = true }
                            .background(Color(0xFFF6FFEF), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF2ECC71)
                    )
                    DropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false },
                        modifier = Modifier
                            .heightIn(max = 250.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        (0..11).forEach { monthIndex ->
                            DropdownMenuItem(
                                text = { Text(getMonthName(monthIndex)) },
                                onClick = {
                                    selectedMonth = monthIndex
                                    expandedMonth = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box {
                    Box {
                        Text(
                            selectedYear.toString(),
                            modifier = Modifier
                                .clickable { expandedYear = true }
                                .background(Color(0xFFF6FFEF), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF2ECC71)
                        )
                        }
                    val yearRange = (1980..Calendar.getInstance().get(Calendar.YEAR) + 85)

                    DropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false },
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        yearRange.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    selectedYear = year
                                    expandedYear = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Right Arrow
            IconButton(onClick = {
                if (selectedMonth == 11) {
                    selectedMonth = 0
                    selectedYear++
                } else selectedMonth++
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
            }
        }

        // --- TRANSACTION LIST ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF6FFEF)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            LazyColumn(Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2F4558)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showFilterPopup = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter Transactions",
                                    tint = Color(0xFF2F4558)
                                )
                            }

                            Button(
                                onClick = { navController.navigate(NavRoutes.EditTransaction.route) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2ECC71),
                                    contentColor = Color(0xFFF6FFEF)
                                )
                            ) {
                                Text("Add")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("List is empty", color = Color.Gray)
                        }
                    }
                } else {
                    grouped.forEach { (date, items) ->
                        item {
                            val parsed = runCatching { dateFormat.parse(date) }.getOrNull()
                            val dayName = parsed?.let {
                                SimpleDateFormat("EEEE", Locale.getDefault()).format(it)
                            } ?: ""
                            val dayTotal =
                                items.sumOf { if (it.type == "Income") it.amount else -it.amount }

                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "$dayName, $date",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            formatAmount(dayTotal),
                                            color = if (dayTotal >= 0) Color(0xFF2E7D32) else Color.Red,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    items.forEachIndexed { index, tx ->
                                        Column(Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate("${NavRoutes.EditTransaction.route}/${tx.id}")
                                            }) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        tx.category,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    if (tx.caption.isNotBlank()) {
                                                        Text(
                                                            tx.caption,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                Text(
                                                    formatAmount(tx.amount),
                                                    color = if (tx.type == "Income") Color(
                                                        0xFF2E7D32
                                                    ) else Color.Red,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                IconButton(onClick = { showDeleteDialog = tx.id }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                            if (index < items.lastIndex) Divider(
                                                Modifier.padding(
                                                    vertical = 8.dp
                                                )
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

        // --- DELETE CONFIRMATION ---
        showDeleteDialog?.let { idToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete this transaction?") },
                confirmButton = {
                    TextButton(onClick = {
                        val index = transactions.indexOfFirst { it.id == idToDelete }
                        if (index != -1) viewModel.deleteExpenseById(idToDelete)
                        showDeleteDialog = null
                    }) {
                        Text("Yes", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("No")
                    }
                }
            )
        }
    }


    // --- Global filter state ---
    val checkedIncomeExpense = remember { mutableStateListOf<String>() }
    val checkedCategories = remember { mutableStateListOf<String>() }
    val checkedPayments = remember { mutableStateListOf<String>() }

    if (showFilterPopup) {
        val incomeExpenseOptions = listOf("Income", "Expense")
        val categoryOptionsIncome = listOf("Salary", "Allowance", "Bonus", "Investment")
        val categoryOptionsExpense = listOf("Food", "Transport", "Shopping", "Utilities", "Education", "Healthcare", "Entertainment")
        val paymentOptionsIncome = listOf("Cash", "Bank Transfer", "Online Transfer", "Check")
        val paymentOptionsExpense = listOf("Cash", "Credit Card", "Debit Card", "Split-Bill", "Online APP Payment")


        val scrollState = rememberScrollState()

        val currentCategories = when {
            checkedIncomeExpense.isEmpty() || checkedIncomeExpense.containsAll(incomeExpenseOptions) ->
                categoryOptionsIncome + categoryOptionsExpense
            checkedIncomeExpense.contains("Income") && checkedIncomeExpense.contains("Expense") ->
                categoryOptionsIncome + categoryOptionsExpense
            checkedIncomeExpense.contains("Income") -> categoryOptionsIncome
            checkedIncomeExpense.contains("Expense") -> categoryOptionsExpense
            else -> categoryOptionsIncome + categoryOptionsExpense // fallback
        }

        val currentPayments = when {
            checkedIncomeExpense.isEmpty() || checkedIncomeExpense.containsAll(incomeExpenseOptions) ->
                (paymentOptionsIncome + paymentOptionsExpense).distinct()
            checkedIncomeExpense.contains("Income") && checkedIncomeExpense.contains("Expense") ->
                (paymentOptionsIncome + paymentOptionsExpense).distinct()
            checkedIncomeExpense.contains("Income") -> paymentOptionsIncome
            checkedIncomeExpense.contains("Expense") -> paymentOptionsExpense
            else -> (paymentOptionsIncome + paymentOptionsExpense).distinct() // fallback
        }

        LaunchedEffect(showFilterPopup) {
            if (showFilterPopup) {
                if (checkedIncomeExpense.isEmpty()) {
                    if (filterType == "All") checkedIncomeExpense.addAll(incomeExpenseOptions)
                    else checkedIncomeExpense.add(filterType)
                }

                // Only add categories if list is empty
                if (checkedCategories.isEmpty()) {
                    val initialCategories = when (filterType) {
                        "Income" -> categoryOptionsIncome
                        "Expense" -> categoryOptionsExpense
                        else -> categoryOptionsIncome + categoryOptionsExpense
                    }
                    if (filterCategory == "All") checkedCategories.addAll(initialCategories)
                    else checkedCategories.add(filterCategory)
                }

                if (checkedPayments.isEmpty()) {
                    val initialPayments = when (filterType) {
                        "Income" -> paymentOptionsIncome
                        "Expense" -> paymentOptionsExpense
                        else -> paymentOptionsIncome + paymentOptionsExpense
                    }
                    if (filterPaymentMethod == "All") checkedPayments.addAll(initialPayments)
                    else checkedPayments.add(filterPaymentMethod)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showFilterPopup = false },
            containerColor = Color(0xFFF6FFEF),
            title = { Text("Filter Transactions") },
            text = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(scrollState)
                ) {
                    Column {
                        SectionWithCheckboxes(
                            title = "Type",
                            options = incomeExpenseOptions,
                            selectedOptions = checkedIncomeExpense
                        )
                        Spacer(Modifier.height(8.dp))

                        SectionWithCheckboxes(
                            title = "Category",
                            options = currentCategories,
                            selectedOptions = checkedCategories
                        )
                        Spacer(Modifier.height(8.dp))

                        SectionWithCheckboxes(
                            title = "Payment Method",
                            options = currentPayments,
                            selectedOptions = checkedPayments
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    filterType = when {
                        checkedIncomeExpense.isEmpty() || checkedIncomeExpense.size == incomeExpenseOptions.size -> "All"
                        checkedIncomeExpense.size == 1 -> checkedIncomeExpense.first()
                        else -> "All"
                    }
                    filterCategory = when {
                        checkedCategories.isEmpty() || checkedCategories.size == currentCategories.size -> "All"
                        checkedCategories.size == 1 -> checkedCategories.first()
                        else -> "All"
                    }
                    filterPaymentMethod = when {
                        checkedPayments.isEmpty() || checkedPayments.size == currentPayments.size -> "All"
                        checkedPayments.size == 1 -> checkedPayments.first()
                        else -> "All"
                    }
                    showFilterPopup = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFilterPopup = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionWithCheckboxes(
    title: String,
    options: List<String>,
    selectedOptions: SnapshotStateList<String>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            val allSelected = options.isNotEmpty() && options.all { it in selectedOptions }
            TextButton(onClick = {
                if (allSelected) {
                    selectedOptions.clear() // allow unselect all
                } else {
                    selectedOptions.clear()
                    selectedOptions.addAll(options)
                }
            })
            {
                Text(if (allSelected) "Unselect All" else "Check All")
            }
        }

        options.forEach { option ->
            val isChecked = selectedOptions.contains(option)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable {
                        if (isChecked) selectedOptions.remove(option)
                        else selectedOptions.add(option)
                    }
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        if (it) selectedOptions.add(option)
                        else selectedOptions.remove(option)
                    }
                )
                Text(option)
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    formatAmount: (Double) -> String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(
            formatAmount(amount),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}


