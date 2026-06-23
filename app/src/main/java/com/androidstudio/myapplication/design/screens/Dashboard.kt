package com.androidstudio.myapplication.design.screens

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.androidstudio.myapplication.R
import com.androidstudio.myapplication.model.Expense
import com.androidstudio.myapplication.model.HomeScreenViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.androidstudio.myapplication.model.SettingsViewModel
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale
import java.time.Month
import java.time.YearMonth

@Composable
fun DashboardScreen(navController: NavHostController, viewModel: HomeScreenViewModel, settingsViewModel: SettingsViewModel) {
    val transactions = viewModel.expenseList
    val selectedMonth by viewModel.selectedMonth
    val selectedYear by viewModel.selectedYear
    val selectedAlbum by viewModel.selectedAlbum
    var selectedType by rememberSaveable { mutableStateOf("Expense") }
    var chartView by remember { mutableStateOf("Pie") }
    var showDatePicker by remember { mutableStateOf(false) }
    val currencySymbol by settingsViewModel.currencySymbol.collectAsState()
    val placeBefore by settingsViewModel.currencyPlacement.collectAsState()

    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val filteredTransactions by remember(transactions, selectedMonth, selectedYear, selectedAlbum, selectedType) {
        derivedStateOf {
            transactions.filter { txn ->
                val date = LocalDate.parse(txn.date, formatter)
                date.monthValue == selectedMonth &&
                        date.year == selectedYear &&
                        txn.album == selectedAlbum &&
                        txn.type == selectedType
            }
        }
    }
    val pieData by remember(transactions, selectedMonth, selectedYear, selectedAlbum, selectedType) {
        derivedStateOf {
            val categorySums = mutableMapOf<String, Float>()
            transactions.filter { txn ->
                txn.type == selectedType &&
                        LocalDate.parse(txn.date, formatter).monthValue == selectedMonth &&
                        LocalDate.parse(txn.date, formatter).year == selectedYear &&
                        txn.album == selectedAlbum
            }.forEach { txn ->
                categorySums[txn.category] = (categorySums[txn.category] ?: 0f) + txn.amount.toFloat()
            }
            categorySums
        }
    }
    val chartData by remember(transactions, selectedMonth, selectedYear, selectedAlbum, selectedType) {
        derivedStateOf {
            val map = mutableMapOf<Int, Float>()
            transactions.filter {
                it.type == selectedType &&
                        LocalDate.parse(it.date, formatter).monthValue == selectedMonth &&
                        LocalDate.parse(it.date, formatter).year == selectedYear &&
                        it.album == selectedAlbum
            }.forEach {
                val day = LocalDate.parse(it.date, formatter).dayOfMonth
                map[day] = (map[day] ?: 0f) + it.amount.toFloat()
            }
            map.toSortedMap().map { Entry(it.key.toFloat(), it.value) }
        }
    }

    fun formatMoney(amount: Float): String {
        val valueformat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formatted = valueformat.format(amount)
        return if (placeBefore)
            "$currencySymbol$formatted"
        else
            "$formatted$currencySymbol"
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF50C878))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Spending & Income Overview",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }


        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Expense", "Income").forEach { type ->
                        Button(
                            onClick = { selectedType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == type) Color.White else Color(0xFF50C878),
                                contentColor = if (selectedType == type) Color(0xFF50C878) else Color.White
                            ),
                            modifier = Modifier.defaultMinSize(minWidth = 80.dp)
                        ) {
                            Text(type)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.decrementMonth() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month", tint = Color.White)
                    }

                    val shortMonth = Month.of(selectedMonth).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                    val yearMonthText = "$selectedYear $shortMonth"

                    Text(
                        text = yearMonthText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { viewModel.incrementMonth() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Month", tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Pie", "Line").forEach { type ->
                    Button(
                        onClick = { chartView = type },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (chartView == type) Color.White else Color(
                                0xFFEAFFEB
                            ),
                            contentColor = if (chartView == type) Color(0xFF76A869) else Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(type)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (chartView == "Pie") {
                CardSection("$selectedType Category Distribution") {
                    val total = pieData.values.sum()
                    PieChartDisplay(pieData, selectedType, settingsViewModel)
                }
            }

            else {
                if (chartView == "Line") {
                    if (chartData.isEmpty()) {
                        Text("No data to display.", color = Color.Gray)
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                LineChartDisplay(
                                    entries = chartData,
                                    selectedMonth = selectedMonth,
                                    selectedYear = selectedYear,
                                    lineColor = if (selectedType == "Expense") Color(0xFFFF8C00) else Color(0xFF368339),
                                    label = selectedType,
                                    settingsViewModel = settingsViewModel
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            CardSection("$selectedType Category Ranking") {
                if (pieData.isEmpty()) {
                    Text("No data to display.", color = Color.Gray)
                } else {
                    val total = pieData.values.sum()
                    Column {
                        pieData.entries
                            .sortedByDescending { it.value }
                            .forEach { (category, amount) ->
                                val percentage = (amount / total * 100)
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(category, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${percentage.roundToInt()}% (${formatMoney(amount)})"
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = (percentage / 100).coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = getCategoryColor(category),
                                        trackColor = Color.LightGray
                                    )
                                }
                            }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            title = { Text("Select Month & Year") },
            text = {
                val allMonths = Month.values().map { it.name.lowercase().replaceFirstChar { it.uppercase() } }
                val yearRange = (2020..2030).toList()

                Row(
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Month roller
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEFFAF1)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 24.dp)
                    ) {
                        itemsIndexed(allMonths) { index, month ->
                            Text(
                                text = month,
                                fontSize = 18.sp,
                                fontWeight = if (index + 1 == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                color = if (index + 1 == selectedMonth) Color(0xFF50C878) else Color.Gray,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        viewModel.setSelectedMonth(index + 1)
                                    }
                            )
                        }
                    }

                    // Year roller
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEFFAF1)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 24.dp)
                    ) {
                        items(yearRange) { year ->
                            Text(
                                text = year.toString(),
                                fontSize = 18.sp,
                                fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                color = if (year == selectedYear) Color(0xFF50C878) else Color.Gray,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        viewModel.setSelectedYear(year)
                                    }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun CardSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAFFEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color(0xFF76A869), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun LineChartDisplay(
    entries: List<Entry>,
    selectedMonth: Int,
    selectedYear: Int,
    lineColor: Color,
    label: String,
    chartBackgroundColor: Color = Color(0xFFEAFFEB),
    axisTextColor: Color = Color(0xFF383737),
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val currencySymbol by settingsViewModel.currencySymbol.collectAsState()
    val placeBefore by settingsViewModel.currencyPlacement.collectAsState()
    val daysInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()

    fun formatMoney(amount: Float): String {
        val valueFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formatted = valueFormat.format(amount)
        return if (placeBefore)
            "$currencySymbol$formatted"
        else
            "$formatted$currencySymbol"
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(chartBackgroundColor),
        factory = {
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                setScaleEnabled(false)
                isDoubleTapToZoomEnabled = false
                setPinchZoom(false)
                setDrawGridBackground(false)
                setDrawBorders(false)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setGridBackgroundColor(chartBackgroundColor.toArgb())
                setViewPortOffsets(30f, 20f, 30f, 40f)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = axisTextColor.toArgb()
                    setDrawGridLines(true)
                    gridColor = Color.LightGray.toArgb()
                    gridLineWidth = 0.5f
                    enableGridDashedLine(10f, 10f, 0f)
                    granularity = 1f
                    labelRotationAngle = -45f
                    // Show day numbers only
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val day = value.toInt()
                            return day.toString()
                        }
                    }
                    axisMinimum = 1f
                    axisMaximum = daysInMonth.toFloat()
                }
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, label).apply {
                color = lineColor.toArgb()
                setCircleColor(lineColor.toArgb())
                circleRadius = 5f
                setDrawCircleHole(false)
                lineWidth = 3f
                valueTextSize = 0f
                setDrawValues(false)
                setDrawFilled(true)
                mode = LineDataSet.Mode.LINEAR

                // Transparent gradient fill
                fillDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(lineColor.copy(alpha = 0.3f).toArgb(), Color.Transparent.toArgb())
                )
                highLightColor = Color.Black.toArgb()
            }

            chart.marker = object : MarkerView(context, android.R.layout.simple_list_item_1) {
                private val textView = findViewById<TextView>(android.R.id.text1)

                override fun refreshContent(e: Entry?, highlight: Highlight?) {
                    val day = e?.x?.toInt() ?: 0
                    val amount = e?.y ?: 0f
                    val date = "%02d-%02d".format(selectedMonth, day)
                    val money = formatMoney(amount)
                    textView.text = "$date\n$money"
                    super.refreshContent(e, highlight)
                }

                override fun getOffset(): MPPointF {
                    return MPPointF(-(width / 2f), -height.toFloat())
                }
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}


@Composable
fun PieChartDisplay(
    pieData: Map<String, Float>,
    selectedType: String,
    settingsViewModel: SettingsViewModel
) {
    val total = pieData.values.sum()

    val currencySymbol by settingsViewModel.currencySymbol.collectAsState()
    val placeBefore by settingsViewModel.currencyPlacement.collectAsState()

    fun formatMoney(amount: Float): String {
        val valueformat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formatted = valueformat.format(amount)
        return if (placeBefore)
            "$currencySymbol$formatted"
        else
            "$formatted$currencySymbol"
    }


    val entries = pieData.map { PieEntry(it.value, it.key) }
    val categoryColors = pieData.keys.map { category ->
        val color = getCategoryColor(category)
        android.graphics.Color.rgb(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            factory = { context ->
                PieChart(context).apply {
                    description.isEnabled = false
                    isDrawHoleEnabled = true
                    setHoleColor(android.graphics.Color.WHITE)
                    setUsePercentValues(true)
                    setDrawEntryLabels(false)
                    legend.isEnabled = false
                    setCenterText("") // Remove center text
                    animateY(800)
                }
            },
            update = { chart ->
                val dataSet = PieDataSet(entries, null).apply {
                    colors = categoryColors
                    valueTextColor = android.graphics.Color.TRANSPARENT
                    valueTextSize = 0f
                }

                chart.data = PieData(dataSet)
                chart.invalidate()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TOTAL: ${formatMoney(total)}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color(0xFF4A4A4A),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Box(
            modifier = Modifier
                .heightIn(max = 150.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                pieData.keys.forEach { category ->
                    val color = getCategoryColor(category)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Food" -> Color(0xFFFFB700)
        "Transport" -> Color(0xFF2196F3)
        "Education" -> Color(0xFF85DA4F)
        "Shopping" -> Color(0xFFFF6F00)
        "Utilities" -> Color(0xFF8321F3)
        "Healthcare" -> Color(0xFFE02A2A)
        "Entertainment" -> Color(0xFF28DDF6)
        "Salary" -> Color(0xFF855110)
        "Bonus" -> Color(0xFFFFDF1F)
        "Allowance" -> Color(0xFFFF4F9B)
        "Investment" -> Color(0xFF4F81FF)
        else -> Color.Gray
    }
}
