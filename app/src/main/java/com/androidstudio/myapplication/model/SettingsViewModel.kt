package com.androidstudio.myapplication.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidstudio.myapplication.datastore.Album
import com.androidstudio.myapplication.datastore.DataStoreManager
import com.androidstudio.myapplication.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale


data class CurrencyInfo(val symbol: String, val placeBefore: Boolean)

class SettingsViewModel(
    application: Application,
    private val dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {

    private val additionalCurrencies = mapOf(
        "NTD" to CurrencyInfo("NT$", true),
        "KOR" to CurrencyInfo("₩", true),
        "RUB" to CurrencyInfo("₽", false),
        "HKD" to CurrencyInfo("HK$", true)
    )

    private val currencyMap: Map<String, CurrencyInfo> =
        Currency.getAvailableCurrencies().associate { currency ->
            val code = currency.currencyCode
            val symbol = currency.getSymbol(Locale.getDefault())

            val placeBefore = when (code) {
                "EUR", "VND", "THB", "PHP", "INR" -> false
                else -> true
            }

            code to CurrencyInfo(symbol, placeBefore)
        } + additionalCurrencies



    // Flows from DataStore exposed as StateFlows to Compose
    val profileName = dataStoreManager.profileNameFlow.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        "User Name"
    )
    val profileImageUri = dataStoreManager.profileImageUriFlow.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        null
    )
    val currencyCode = dataStoreManager.currencyFlow.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        "USD"
    )
    val currencySymbol = dataStoreManager.currencySymbolFlow.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        "$"
    )
    val currencyPlacement = dataStoreManager.currencyPlacementFlow.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        true
    )
    // Mutable states for edits before saving
    private val _profileNameEdit = MutableStateFlow("")
    private val _profileImageUriEdit = MutableStateFlow<String?>(null)
    private val _currencyCodeEdit = MutableStateFlow("USD")
    private val _currencySymbolEdit = MutableStateFlow("$")
    private val _currencyPlacementEdit = MutableStateFlow(true)
    private val _darkModeEnabledEdit = MutableStateFlow(false)

    // Expose edit states as StateFlow
    val profileNameEdit: StateFlow<String> = _profileNameEdit.asStateFlow()
    val profileImageUriEdit: StateFlow<String?> = _profileImageUriEdit.asStateFlow()
    val currencyCodeEdit: StateFlow<String> = _currencyCodeEdit.asStateFlow()
    val currencySymbolEdit: StateFlow<String> = _currencySymbolEdit.asStateFlow()
    val currencyPlacementEdit: StateFlow<Boolean> = _currencyPlacementEdit.asStateFlow()
    val darkModeEnabledEdit: StateFlow<Boolean> = _darkModeEnabledEdit.asStateFlow()

    init {
        // Initialize edit states with current DataStore values
        viewModelScope.launch {
            profileName.collect { _profileNameEdit.value = it }
        }
        viewModelScope.launch {
            profileImageUri.collect { _profileImageUriEdit.value = it }
        }
        viewModelScope.launch {
            currencyCode.collect { code ->
                _currencyCodeEdit.value = code
                val info = currencyMap[code] ?: CurrencyInfo("$", true)
                _currencySymbolEdit.value = info.symbol
                _currencyPlacementEdit.value = info.placeBefore
            }
        }
        viewModelScope.launch {
            currencySymbol.collect { _currencySymbolEdit.value = it }
        }
        viewModelScope.launch {
            currencyPlacement.collect { _currencyPlacementEdit.value = it }
        }
    }

    // Update functions for UI changes
    fun updateProfileName(name: String) {
        _profileNameEdit.value = name
    }

    fun updateProfileImageUri(uri: String?) {
        _profileImageUriEdit.value = uri
    }

    fun updateCurrencyCode(code: String) {
        _currencyCodeEdit.value = code
        val info = currencyMap[code] ?: CurrencyInfo("$", true)
        _currencySymbolEdit.value = info.symbol
        _currencyPlacementEdit.value = info.placeBefore
    }

    fun updateDarkModeEnabled(enabled: Boolean) {
        _darkModeEnabledEdit.value = enabled
    }

    // Save all edits to DataStore
    suspend fun saveSettings() {
        dataStoreManager.saveProfileName(_profileNameEdit.value)
        dataStoreManager.saveProfileImageUri(_profileImageUriEdit.value)
        dataStoreManager.saveCurrency(_currencyCodeEdit.value)
        dataStoreManager.saveCurrencySymbol(_currencySymbolEdit.value)
        dataStoreManager.saveCurrencyPlacement(_currencyPlacementEdit.value)
        dataStoreManager.saveDarkMode(_darkModeEnabledEdit.value)
    }

    private val albumRepo = AlbumRepository(dataStoreManager)

    private val _deletedAlbums = MutableStateFlow<List<Album>>(emptyList())
//    val deletedAlbums: StateFlow<List<Album>> = _deletedAlbums.asStateFlow()

    // Expose deleted albums from repository
    val deletedAlbums: StateFlow<List<Album>> = albumRepo
        .getDeletedAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _albumChangedEvent = MutableSharedFlow<Unit>()
    val albumChangedEvent: SharedFlow<Unit> = _albumChangedEvent
    val albumRestoredEvent = MutableSharedFlow<Unit>()

    fun loadDeletedAlbums() {
        viewModelScope.launch {
            dataStoreManager.trashedAlbumsFlow
                .collectLatest { albums ->
                    _deletedAlbums.value = albums
                }
        }
    }

    fun restoreAlbum(album: Album) {
        viewModelScope.launch {
            albumRepo.restoreAlbum(album.id)
            _albumChangedEvent.emit(Unit)
            albumRestoredEvent.emit(Unit)
//            loadDeletedAlbums()
        }
    }

    fun permanentlyDeleteAlbum(album: Album) {
        viewModelScope.launch {
            albumRepo.permanentlyDeleteAlbum(album.id)
//            loadDeletedAlbums()
            _albumChangedEvent.emit(Unit)
        }
    }
}
