package com.androidstudio.myapplication.design.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.androidstudio.myapplication.model.HomeScreenViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.androidstudio.myapplication.model.SettingsViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    var newAlbumName by remember { mutableStateOf(TextFieldValue("")) }
    var renameDialogAlbum by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf(TextFieldValue("")) }
    var deleteDialogAlbum by remember { mutableStateOf<String?>(null) } // For delete confirmation
    val presetColors = listOf(
        "#FFFFFFFF", // DEFAULT WHITE
        "#FFC2C2C2", // Light gray
        "#FFEFA8FF", // Light Purple
        "#FF918FFF", // Light violet
        "#FF85FFF3", // Light Teal
        "#FFFF6380", // Light Red
        "#FFFFC107", // Amber 500
        "#FFCBFF87", //Lime Green
        "#FF4CAF50", // Green 500
        "#FF8FFFB4", // Emerald green
        "#FFFFA36E",// Light Orange
        "#FFFF9EEF" //Pink
    )
    var exportPendingAlbum by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && exportPendingAlbum != null) {
            viewModel.exportAlbumToCSV(
                context = context,
                albumName = exportPendingAlbum!!,
                uri = uri,
                expenseList = viewModel.expenseList,
                currencySymbol = settingsViewModel.currencySymbol.value,
                currencyPlacement = settingsViewModel.currencyPlacement.value
            )
        }
    }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAFFEB))
            .padding(16.dp)
    ) {
        Text(
            "Current Album: ${viewModel.selectedAlbum.value}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Create New Album", style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            TextField(
                value = newAlbumName,
                onValueChange = { newAlbumName = it },
                label = { Text("Album Name") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    val trimmed = newAlbumName.text.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.addAlbum(trimmed)  // Add with default color inside ViewModel
                        newAlbumName = TextFieldValue("")
                        Toast.makeText(context, "Album '$trimmed' added", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Album name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your Albums", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Clear Default Album?") },
                text = { Text("Are you sure you want to delete all transactions in the Default album? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearTransactionsInAlbum("Default")
                            showClearConfirmDialog = false
                            Toast.makeText(context, "Default album cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearConfirmDialog = false }
                    ) {
                        Text("No")
                    }
                }
            )
        }

        LazyColumn {
            items(viewModel.albums) { album ->
                val expenses = viewModel.expenseList.filter { it.album == album.name }
                val income = expenses.filter { it.type == "Income" }.sumOf { it.amount }
                val expense = expenses.filter { it.type == "Expense" }.sumOf { it.amount }
                val balance = income - expense

                val backgroundColor = try {
                    Color(android.graphics.Color.parseColor(album.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.surfaceVariant
                }

                val currencySymbol = settingsViewModel.currencySymbol.collectAsState().value
                val currencyPlacement = settingsViewModel.currencyPlacement.collectAsState().value

                val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
                val formattedAmount = numberFormat.format(balance)

                val formattedBalance = if (currencyPlacement) {
                    "$currencySymbol$formattedAmount"
                } else {
                    "$formattedAmount$currencySymbol"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 8.dp, bottomStart = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { viewModel.selectAlbum(album.name) }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = album.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val isDefaultAlbum = album.name == "Default"
                            val isLocked = viewModel.lockedAlbums.contains(album.name)

                            IconButton(onClick = { viewModel.toggleAlbumLock(album.name) }) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    exportPendingAlbum = album.name
                                    launcher.launch(null)
                                }
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export Album")
                            }

                            if (!isLocked) {
                                IconButton(
                                    onClick = {
                                        renameDialogAlbum = album.name
                                        renameText = TextFieldValue(album.name)
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename Album")
                                }

                                if (isDefaultAlbum) {
                                    IconButton(
                                        onClick = {
                                            showClearConfirmDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Default Album")
                                    }
                                }
                                else {
                                    IconButton(
                                        onClick = {
                                            deleteDialogAlbum = album.name
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Album")
                                    }
                                }
                            }

                            if (album.name == viewModel.selectedAlbum.value) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Balance: $formattedBalance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    renameDialogAlbum?.let { albumToRename ->
        if (albumToRename != "Default") {
            val currentAlbum = viewModel.albums.find { it.name == albumToRename }
            var editColorHex by remember { mutableStateOf(currentAlbum?.colorHex ?: "#FFFFFF") }

            AlertDialog(
                onDismissRequest = { renameDialogAlbum = null },
                confirmButton = {
                    TextButton(onClick = {
                        val newName = renameText.text.trim()
                        if (newName.isNotEmpty()) {
                            viewModel.renameAlbum(albumToRename, newName)
                            viewModel.updateAlbumColor(newName, editColorHex) // update color on save
                            renameDialogAlbum = null
                            Toast.makeText(context, "Album renamed and color updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogAlbum = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Edit Album") },
                text = {
                    Column {
                        TextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text("Album Name") }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Pick Album Color", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            presetColors.forEach { colorHex ->
                                val color = try {
                                    Color(android.graphics.Color.parseColor(colorHex))
                                } catch (e: Exception) {
                                    Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(4.dp)
                                        .background(color, shape = CircleShape)
                                        .border(
                                            width = if (colorHex == editColorHex) 3.dp else 1.dp,
                                            color = if (colorHex == editColorHex) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { editColorHex = colorHex }
                                )
                            }
                        }
                    }
                }
            )
        } else {
            renameDialogAlbum = null
        }
    }

    // Delete Confirmation Dialog
    deleteDialogAlbum?.let { albumToDelete ->
        AlertDialog(
            onDismissRequest = { deleteDialogAlbum = null },
            title = { Text("Delete Album") },
            text = { Text("Do you want to delete the album \"$albumToDelete\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.softDeleteAlbum(albumToDelete)
                    Toast.makeText(context, "'$albumToDelete' moved to Trash Bin", Toast.LENGTH_SHORT).show()
                    deleteDialogAlbum = null
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogAlbum = null }) {
                    Text("No")
                }
            }
        )
    }
}
