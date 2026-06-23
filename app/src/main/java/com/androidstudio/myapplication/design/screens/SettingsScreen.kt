package com.androidstudio.myapplication.design.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import com.androidstudio.myapplication.model.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.androidstudio.myapplication.R
import com.androidstudio.myapplication.model.SettingsViewModelFactory
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.Currency
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.ui.window.Dialog

data class CurrencyDisplay(
    val region: String,
    val code: String
) {
    val displayText: String get() = "$region ($code)"
}

@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val dataStoreManager = remember { com.androidstudio.myapplication.datastore.DataStoreManager(context) }
    val factory = remember {
        SettingsViewModelFactory(application, dataStoreManager)
    }
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val cropLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContract<Intent, Uri?>() {
            override fun createIntent(context: Context, input: Intent): Intent = input
            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                return UCrop.getOutput(intent ?: return null)
            }
        }
    ) { croppedUri ->
        croppedUri?.let {
            settingsViewModel.updateProfileImageUri(it.toString()) // ✅ This work
        }
    }

    val scope = rememberCoroutineScope()
    val profileImageUri by settingsViewModel.profileImageUriEdit.collectAsState()
    val userName by settingsViewModel.profileNameEdit.collectAsState()
    val selectedCurrency by settingsViewModel.currencyCodeEdit.collectAsState()
    val deletedAlbums by settingsViewModel.deletedAlbums.collectAsState()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val destinationUri = Uri.fromFile(
                File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
            )

            val uCrop = UCrop.of(uri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(500, 500)
                .getIntent(context)

            cropLauncher.launch(uCrop)
        }
    }


    val currencyList = remember {
        listOf(
            CurrencyDisplay("United States", "USD"),
            CurrencyDisplay("European Union", "EUR"),
            CurrencyDisplay("Japan", "JPY"),
            CurrencyDisplay("China", "CNY"),
            CurrencyDisplay("Taiwan", "NTD"),
            CurrencyDisplay("South Korea", "KOR"),
            CurrencyDisplay("Hong Kong", "HKD"),
            CurrencyDisplay("United Kingdom", "GBP"),
            CurrencyDisplay("India", "INR"),
            CurrencyDisplay("Russia", "RUB"),
            CurrencyDisplay("Indonesia", "IDR"),
            CurrencyDisplay("Philippines", "PHP"),
            CurrencyDisplay("Thailand", "THB"),
            CurrencyDisplay("Australia", "AUD"),
            CurrencyDisplay("New Zealand", "NZD"),
            CurrencyDisplay("Canada", "CAD"),
            CurrencyDisplay("Singapore", "SGD"),
            CurrencyDisplay("Malaysia", "MYR"),
            CurrencyDisplay("Vietnam", "VND")
        ).sortedBy { it.region }
    }


    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        LaunchedEffect(Unit) {
            viewModel.loadDeletedAlbums()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (profileImageUri != null) {
                val imagePainter = rememberAsyncImagePainter(
                    model = profileImageUri ?: ""
                )

                Image(
                    painter = imagePainter,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )

            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default Profile",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { settingsViewModel.updateProfileName(it) },
                label = { Text("User Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Currency Setting:")

            Box {
                OutlinedButton(
                    onClick = { currencyDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(currencyList.find { it.code == selectedCurrency }?.displayText ?: selectedCurrency)
                }
                DropdownMenu(
                    expanded = currencyDropdownExpanded,
                    onDismissRequest = { currencyDropdownExpanded = false },
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    currencyList.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency.displayText) },
                            onClick = {
                                settingsViewModel.updateCurrencyCode(currency.code)
                                currencyDropdownExpanded = false
                            }
                        )
                    }
                }


            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2ECC71),
                    contentColor = Color(0xFFF6FFEF)
                ),
                onClick = {
                    scope.launch {
                        settingsViewModel.saveSettings()
                        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showTrashDialog by remember { mutableStateOf(false) }

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2ECC71),
                    contentColor = Color(0xFFF6FFEF)
                ),
                onClick = { showTrashDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Trash Bin")
            }

//            if (showTrashDialog) {
//                LaunchedEffect(Unit) {
//                    settingsViewModel.loadDeletedAlbums()
//                }
//
//                AlertDialog(
//                    onDismissRequest = { showTrashDialog = false },
//                    confirmButton = {
//                        TextButton(onClick = { showTrashDialog = false }) {
//                            Text("Close")
//                        }
//                    },
//                    title = { Text("🗑️ Trash Bin - Deleted Albums", style = MaterialTheme.typography.titleLarge) },
//                    text = {
//                        if (deletedAlbums.isEmpty()) {
//                            Text("Trash bin is empty", style = MaterialTheme.typography.bodyMedium)
//                        } else {
//                            LazyColumn(
//                                modifier = Modifier
//                                    .heightIn(max = 400.dp)
//                                    .padding(vertical = 8.dp)
//                            ) {
//                                items(deletedAlbums) { album ->
//                                    Card(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .padding(horizontal = 8.dp, vertical = 6.dp),
//                                        elevation = CardDefaults.cardElevation(4.dp),
//                                        shape = RoundedCornerShape(12.dp),
//                                        colors = CardDefaults.cardColors(
//                                            containerColor = Color(0xFFF6C619)
//                                        )
//                                    ) {
//                                        Column(modifier = Modifier.padding(16.dp)) {
//                                            Text(
//                                                text = album.name,
//                                                style = MaterialTheme.typography.titleMedium,
//                                                color = MaterialTheme.colorScheme.onSurface
//                                            )
//                                            Spacer(modifier = Modifier.height(8.dp))
//                                            Row(
//                                                modifier = Modifier.fillMaxWidth(),
//                                                horizontalArrangement = Arrangement.End
//                                            ) {
//                                                IconButton(onClick = {
//                                                    settingsViewModel.restoreAlbum(album)
//                                                }) {
//                                                    Icon(Icons.Default.Restore, contentDescription = "Restore")
//                                                }
//                                                IconButton(onClick = {
//                                                    settingsViewModel.permanentlyDeleteAlbum(album)
//                                                }) {
//                                                    Icon(
//                                                        imageVector = Icons.Default.Delete,
//                                                        contentDescription = "Delete",
//                                                        tint = Color.Red
//                                                    )
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                )
//            }
            if (showTrashDialog) {
                TrashBinPopup(
                    settingsViewModel = settingsViewModel,
                    onDismiss = { showTrashDialog = false }
                )
            }
        }
    }
}

@Composable
fun TrashBinPopup(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val deletedAlbums by settingsViewModel.deletedAlbums.collectAsState()
    LaunchedEffect(deletedAlbums) {
        Log.d("TrashBinPopup", "Deleted albums in popup: ${deletedAlbums.size}")
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Trash Bin", style = MaterialTheme.typography.titleLarge)

                if (deletedAlbums.isEmpty()) {
                    Text("No trashed albums.", modifier = Modifier.padding(8.dp))
                } else {
                    LazyColumn {
                        items(deletedAlbums) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(album.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = { settingsViewModel.restoreAlbum(album) }) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Restore Album"
                                    )
                                }
                                IconButton(onClick = { settingsViewModel.permanentlyDeleteAlbum(album) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Permanently"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}
