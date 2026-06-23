package com.androidstudio.myapplication.model

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidstudio.myapplication.datastore.Album
import com.androidstudio.myapplication.repository.AlbumRepository
import com.androidstudio.myapplication.datastore.DataStoreManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.mutableStateListOf
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.util.Locale
import java.text.NumberFormat

class HomeScreenViewModel(  private val dataStoreManager: DataStoreManager,
                            private val albumRepository: AlbumRepository) : ViewModel()
{
    var user = mutableStateOf(User("User Name")) // Replace with actual logic
        private set

    var expenseList = mutableStateListOf<Expense>()
        private set

    val totalExpense: Double
        get() = expenseList.filter { it.type == "Expense" }.sumOf { it.amount }

    val totalIncome: Double
        get() = expenseList.filter { it.type == "Income" }.sumOf { it.amount }

    val balance: Double
        get() = totalIncome - totalExpense

    var currencyCode by mutableStateOf("USD")  // default currency
        private set


    fun updateCurrencyCode(newCode: String) {
        currencyCode = newCode
    }

    fun setCurrencyCodeFromSettings(code: String) {
        currencyCode = code
    }

    private var nextId = 1

    private val _albums = mutableStateListOf<Album>()
    val albums: List<Album> get() = _albums


    fun addTransaction(expense: Expense) {
        Log.d("HomeVM", "addTransaction called with: $expense")
        val finalId = if (expense.id.isNotEmpty()) expense.id else nextId.toString()
        nextId++
        expenseList.add(expense.copy(id = finalId))
        saveToStore()
        Log.d("HomeVM", "expenseList now has ${expenseList.size} items")
    }


    fun updateTransaction(updated: Expense) {
        val index = expenseList.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            expenseList[index] = updated
        }
        saveToStore()
    }

    fun deleteExpenseById(id: String) {
        expenseList.removeAll { it.id == id }
        saveToStore()
    }

    private fun saveToStore() {
        viewModelScope.launch {
            dataStoreManager.saveExpenseList(expenseList)
        }
    }
    var filteredTransactionList = mutableStateListOf<Expense>()
        private set

    private val _lockedAlbums = mutableStateListOf<String>("Default")
    val lockedAlbums: List<String> get() = _lockedAlbums

    // Change _trashedAlbums type to Album objects
    private val _trashedAlbums = mutableStateListOf<Album>()
    val trashedAlbums: List<Album> get() = _trashedAlbums

    // 🧾 Wallet album-related state (no DataStore)
    var selectedAlbum = mutableStateOf("Default")
        private set

    init {
        viewModelScope.launch {
            val currentAlbums = dataStoreManager.albumListFlow.first()
            val hasDefault = currentAlbums.any { it.id == "default" }
            if (!hasDefault) {
                val defaultAlbum = Album(id = "default", name = "Default", colorHex = "#FFFFFF")
                val newAlbums = currentAlbums + defaultAlbum
                dataStoreManager.saveAlbums(newAlbums)
            }

            val selected = dataStoreManager.selectedAlbumFlow.first()
            if (selected.isBlank()) {
                dataStoreManager.saveSelectedAlbum("default")
            }
        }


        viewModelScope.launch {
            dataStoreManager.albumListFlow.collectLatest { savedAlbums ->
                val albumsToUse = if (savedAlbums.isEmpty()) {
                    listOf(getDefaultAlbum())
                } else {
                    val hasDefault = savedAlbums.any { it.id == "default" }
                    if (!hasDefault) listOf(getDefaultAlbum()) + savedAlbums else savedAlbums
                }

                _albums.clear()
                _albums.addAll(albumsToUse)
            }
        }

        viewModelScope.launch {
            dataStoreManager.selectedAlbumFlow.collect { savedAlbum ->
                selectedAlbum.value = savedAlbum
            }
        }
        viewModelScope.launch {
            dataStoreManager.expenseListFlow.collectLatest { list ->
                expenseList.clear()
                expenseList.addAll(list)
                nextId = (list.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
            }
        }

        // 🔁 Load locked albums
        viewModelScope.launch {
            dataStoreManager.lockedAlbumsFlow.collectLatest { locked ->
                Log.d("HomeVM", "_lockedAlbums is null? ${_lockedAlbums == null}")
                Log.d("HomeVM", "locked albums received: $locked")
                _lockedAlbums.clear()
                _lockedAlbums.addAll(locked.toList()) // defensive copy
            }
        }


        viewModelScope.launch {
            dataStoreManager.trashedAlbumsFlow.collectLatest { trashedAlbums ->
                _trashedAlbums.clear()
                _trashedAlbums.addAll(trashedAlbums)
            }
        }



    }
    private fun getDefaultAlbum() = Album(id = "default", name = "Default", colorHex = "#FFFFFF")

    private fun generateAlbumId(name: String): String {
        return name.lowercase().replace(" ", "_")
    }

    fun addAlbum(name: String) {
        if (name.isNotBlank() && _albums.none { it.name == name } && name != "Default") {
            val newAlbum = Album(id = generateAlbumId(name), name = name, colorHex = "#FFFFFF") // default white color
            _albums.add(newAlbum)
            persistAlbumList()
        }
    }

    fun updateAlbumColor(albumName: String, newColorHex: String) {
        val index = _albums.indexOfFirst { it.name == albumName }
        if (index != -1) {
            val oldAlbum = _albums[index]
            val updatedAlbum = oldAlbum.copy(colorHex = newColorHex)
            _albums[index] = updatedAlbum
            persistAlbumList()
        }
    }

    private fun deduplicateAndEnsureDefault(albums: List<Album>): List<Album> {
        val uniqueById = albums.distinctBy { it.id }
        val hasDefault = uniqueById.any { it.id == "default" }
        return if (hasDefault) {
            uniqueById
        } else {
            uniqueById + getDefaultAlbum()
        }
    }

    private fun persistAlbumList() {
        viewModelScope.launch {
            val albumsToSave = deduplicateAndEnsureDefault(_albums.toList())
            dataStoreManager.saveAlbums(albumsToSave)
        }
    }

    private fun persistLockedAlbums() {
        viewModelScope.launch {
            dataStoreManager.saveLockedAlbums(_lockedAlbums.toList())
        }
    }

    fun selectAlbum(name: String) {
        selectedAlbum.value = name
        viewModelScope.launch {
            dataStoreManager.saveSelectedAlbum(name)
        }
    }


    fun toggleAlbumLock(album: String) {
        if (_lockedAlbums.contains(album)) {
            _lockedAlbums.remove(album)
        } else {
            _lockedAlbums.add(album)
        }
        persistAlbumList()
        persistLockedAlbums()
    }


    private fun persistTrashedAlbums() {
        viewModelScope.launch {
            dataStoreManager.saveTrashedAlbums(_trashedAlbums.toList())
        }
    }

    // ---- FIXED: renameAlbum updates Album object correctly ----
    fun renameAlbum(oldName: String, newName: String) {
        if (oldName == "Default" || _lockedAlbums.contains(oldName)) return

        if (newName.isNotBlank() && _albums.none { it.name == newName }) {
            val index = _albums.indexOfFirst { it.name == oldName }
            if (index != -1) {
                val oldAlbum = _albums[index]
                val updatedAlbum = oldAlbum.copy(name = newName)
                _albums[index] = updatedAlbum

                // Update expenses' album field accordingly
                expenseList.replaceAll { if (it.album == oldName) it.copy(album = newName) else it }

                if (selectedAlbum.value == oldName) {
                    selectedAlbum.value = newName
                }
            }
            persistAlbumList()
        }
    }


    fun deleteAlbum(name: String) {
        if (name != "Default" && !_lockedAlbums.contains(name)) {
            // Find album object to trash
            val albumToTrash = _albums.find { it.name == name }
            if (albumToTrash != null && !_trashedAlbums.any { it.id == albumToTrash.id }) {
                _trashedAlbums.add(albumToTrash)
            }
            _albums.removeAll { it.name == name }
            expenseList.removeAll { it.album == name }
            if (selectedAlbum.value == name) {
                selectedAlbum.value = "Default"
            }
            persistAlbumList()
        }
    }

    fun softDeleteAlbum(name: String) {
        if (name != "Default" && !_lockedAlbums.contains(name)) {
            val albumToTrash = _albums.find { it.name == name }
            if (albumToTrash != null && !_trashedAlbums.any { it.id == albumToTrash.id }) {
                _trashedAlbums.add(albumToTrash)
            }

            _albums.removeAll { it.name == name }

            // Optionally remove expenses linked to that album (or keep them for future restore)
            expenseList.removeAll { it.album == name }

            if (selectedAlbum.value == name) {
                selectedAlbum.value = "Default"
            }

            persistAlbumList()
            viewModelScope.launch {
                dataStoreManager.saveTrashedAlbums(_trashedAlbums.toList())
                dataStoreManager.saveSelectedAlbum(selectedAlbum.value)
                dataStoreManager.saveExpenseList(expenseList)
            }
        }
    }
    fun saveExpensesToStorage(expenses: List<Expense>) {
        viewModelScope.launch {
            dataStoreManager.saveExpenseList(expenses)
        }
    }

    fun clearTransactionsInAlbum(albumName: String) {
        val updated = expenseList.filter { it.album != albumName }
        expenseList.clear()
        expenseList.addAll(updated)
        saveExpensesToStorage(expenseList)
    }

    fun restoreAlbum(name: String) {
        val trashedAlbum = _trashedAlbums.find { it.name == name }
        if (trashedAlbum != null &&
            _albums.none { it.id == trashedAlbum.id } &&
            _trashedAlbums.any { it.id == trashedAlbum.id }) {
            _albums.add(trashedAlbum)
            _trashedAlbums.remove(trashedAlbum)
            persistAlbumList()
            persistTrashedAlbums()
        }
    }


    fun refreshAlbums() {
        viewModelScope.launch {
            albumRepository.getAllAlbums().collect { albums ->
                _albums.clear()
                _albums.addAll(albums)
            }
        }
    }

    fun observeRestoresFrom(settingsViewModel: SettingsViewModel) {
        viewModelScope.launch {
            settingsViewModel.albumRestoredEvent.collect {
                refreshAlbums()
            }
        }
    }


    fun exportAlbumToCSV(
        context: Context,
        albumName: String,
        uri: Uri,
        expenseList: List<Expense>,
        currencySymbol: String,
        currencyPlacement: Boolean // true = before, false = after
    ) {
        val transactions = expenseList.filter { it.album == albumName }
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions in '$albumName' to export", Toast.LENGTH_SHORT).show()
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val csvHeader = "Date,Type,Category,Title,Caption,Method,Amount"
        val csvRows = transactions.joinToString("\n") { t ->
            val safeCaption = t.caption.replace(",", " ")
            val safeTitle = t.title.replace(",", " ")
            val safeMethod = t.method.replace(",", " ")

            val formattedAmount = numberFormat.format(t.amount)
            val displayAmount = if (currencyPlacement) {
                "$currencySymbol$formattedAmount"
            } else {
                "$formattedAmount$currencySymbol"
            }

            "${t.date},${t.type},${t.category},$safeTitle,$safeCaption,$safeMethod,$displayAmount"
        }
        val csvContent = "$csvHeader\n$csvRows"

        try {
            val resolver = context.contentResolver
            val albumFileName = "$albumName-transactions.csv"

            val pickedDir = DocumentFile.fromTreeUri(context, uri)
            val csvFile = pickedDir?.createFile("text/csv", albumFileName)
                ?: throw IOException("Unable to create CSV file")
            resolver.openOutputStream(csvFile.uri)?.use { output ->
                output.write(csvContent.toByteArray())
            }

            Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }


    val filteredExpenses by derivedStateOf {
        expenseList.filter { it.album == selectedAlbum.value }
    }

    //For dashboard page
    var selectedMonth = mutableStateOf(java.time.LocalDate.now().monthValue)
        private set

    var selectedYear = mutableStateOf(java.time.LocalDate.now().year)
        private set

    fun setSelectedMonth(month: Int) {
        selectedMonth.value = month
    }

    fun setSelectedYear(year: Int) {
        selectedYear.value = year
    }

    fun decrementMonth() {
        if (selectedMonth.value == 1) {
            selectedMonth.value = 12
            selectedYear.value -= 1
        } else {
            selectedMonth.value -= 1
        }
    }

    fun incrementMonth() {
        if (selectedMonth.value == 12) {
            selectedMonth.value = 1
            selectedYear.value += 1
        } else {
            selectedMonth.value += 1
        }
    }

    companion object {
        fun getDefaultAlbum() = Album(
            id = "default",
            name = "Default",
            colorHex = "#FFFFFF"
        )
    }
}
