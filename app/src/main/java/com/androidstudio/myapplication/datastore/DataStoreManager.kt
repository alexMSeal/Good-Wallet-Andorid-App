package com.androidstudio.myapplication.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.androidstudio.myapplication.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Album(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String,
    val isDeleted: Boolean = false
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val SELECTED_ALBUM_KEY = stringPreferencesKey("selected_album")
        val PROFILE_NAME_KEY = stringPreferencesKey("profile_name")
        val PROFILE_IMAGE_URI_KEY = stringPreferencesKey("profile_image_uri")
        val CURRENCY_KEY = stringPreferencesKey("currency_code")
        val CURRENCY_SYMBOL_KEY = stringPreferencesKey("currency_symbol")
        val CURRENCY_PLACEMENT_KEY = booleanPreferencesKey("currency_placement_before")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
        val ALBUM_LIST_KEY = stringPreferencesKey("album_list")
        val LOCKED_ALBUMS_KEY = stringPreferencesKey("locked_albums")
        val TRASHED_ALBUMS_KEY = stringPreferencesKey("trashed_albums")
    }

    suspend fun saveAlbums(albums: List<Album>) {
        val defaultAlbum = Album(id = "default", name = "Default", colorHex = "#FFFFFF")

        // Ensure Default album exists (reuse if exists, else inject)
        val withDefault = if (albums.none { it.id == "default" }) {
            listOf(defaultAlbum) + albums
        } else {
            albums.map {
                if (it.id == "default") defaultAlbum.copy(name = it.name) else it
            }
        }

        val json = Json.encodeToString(withDefault)
        context.dataStore.edit { prefs ->
            prefs[ALBUM_LIST_KEY] = json
        }
    }

    suspend fun saveLockedAlbums(locked: List<String>) {
        val json = Json.encodeToString(locked)
        context.dataStore.edit { prefs ->
            prefs[LOCKED_ALBUMS_KEY] = json
        }
    }

    suspend fun saveTrashedAlbums(trashed: List<Album>) {
        val json = Json.encodeToString(trashed) // serialize full Album list
        context.dataStore.edit { prefs ->
            prefs[TRASHED_ALBUMS_KEY] = json
        }
    }


    val lockedAlbumsFlow: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            val jsonString = prefs[LOCKED_ALBUMS_KEY]
            try {
                if (jsonString.isNullOrBlank()) emptyList()
                else Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                Log.e("DataStore", "Failed to decode lockedAlbums JSON: $jsonString", e)
                emptyList()
            }
        }


    val trashedAlbumsFlow: Flow<List<Album>> = context.dataStore.data
        .map { prefs ->
            prefs[TRASHED_ALBUMS_KEY]?.let {
                try {
                    Json.decodeFromString(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }


    val albumListFlow: Flow<List<Album>> = context.dataStore.data
        .map { prefs ->
            prefs[ALBUM_LIST_KEY]?.let {
                try {
                    Json.decodeFromString(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    // Album
    val selectedAlbumFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[SELECTED_ALBUM_KEY] ?: "Default" }
    suspend fun saveSelectedAlbum(album: String) {
        context.dataStore.edit { prefs -> prefs[SELECTED_ALBUM_KEY] = album }
    }

    // Profile name
    val profileNameFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[PROFILE_NAME_KEY] ?: "User Name" }
    suspend fun saveProfileName(name: String) {
        context.dataStore.edit { prefs -> prefs[PROFILE_NAME_KEY] = name }
    }

    // Profile image URI
    val profileImageUriFlow: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[PROFILE_IMAGE_URI_KEY] }
    suspend fun saveProfileImageUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(PROFILE_IMAGE_URI_KEY)
            } else {
                prefs[PROFILE_IMAGE_URI_KEY] = uri
            }
        }
    }

    // Currency code
    val currencyFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[CURRENCY_KEY] ?: "USD" }

    suspend fun saveCurrency(code: String) {
        context.dataStore.edit { prefs -> prefs[CURRENCY_KEY] = code }
    }

    // Currency symbol
    val currencySymbolFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[CURRENCY_SYMBOL_KEY] ?: "$" }

    suspend fun saveCurrencySymbol(symbol: String) {
        context.dataStore.edit { prefs -> prefs[CURRENCY_SYMBOL_KEY] = symbol }
    }

    // Currency placement (before/after)
    val currencyPlacementFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[CURRENCY_PLACEMENT_KEY] ?: true }

    suspend fun saveCurrencyPlacement(placementBefore: Boolean) {
        context.dataStore.edit { prefs -> prefs[CURRENCY_PLACEMENT_KEY] = placementBefore }
    }

    // Dark mode enabled
    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] ?: false }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }

    val EXPENSE_LIST_KEY = stringPreferencesKey("expense_list_json")


    suspend fun saveExpenseList(expenseList: List<Expense>) {
        val json = Json.encodeToString(expenseList)
        context.dataStore.edit { prefs ->
            prefs[EXPENSE_LIST_KEY] = json
        }
    }

    val expenseListFlow: Flow<List<Expense>> = context.dataStore.data
        .map { prefs ->
            prefs[EXPENSE_LIST_KEY]?.let {
                try {
                    Json.decodeFromString(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
}
