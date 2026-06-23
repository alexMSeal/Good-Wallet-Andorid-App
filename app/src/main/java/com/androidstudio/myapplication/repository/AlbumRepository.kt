package com.androidstudio.myapplication.repository

import com.androidstudio.myapplication.datastore.Album
import com.androidstudio.myapplication.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AlbumRepository(
    private val dataStore: DataStoreManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    init {
        // Collect albumListFlow instead of the non-existent albumFlow
        scope.launch {
            dataStore.albumListFlow.collect { loadedAlbums ->
                _albums.value = loadedAlbums
            }
        }
    }

    fun getAllAlbums(): Flow<List<Album>> = albums

    fun getActiveAlbums(): Flow<List<Album>> =
        albums.map { list -> list.filter { !it.isDeleted } }

    fun getDeletedAlbums(): Flow<List<Album>> =
        albums.map { list -> list.filter { it.isDeleted } }

    suspend fun addAlbum(album: Album) {
        val updated = albums.value + album
        dataStore.saveAlbums(updated)
    }

    suspend fun softDeleteAlbum(albumId: String) {
        updateAlbum(albumId) { it.copy(isDeleted = true) }
    }

    suspend fun restoreAlbum(albumId: String) {
        updateAlbum(albumId) { it.copy(isDeleted = false) }
    }

    suspend fun permanentlyDeleteAlbum(albumId: String) {
        val updated = albums.value.filterNot { it.id == albumId }
        dataStore.saveAlbums(updated)
    }

    private suspend fun updateAlbum(id: String, update: (Album) -> Album) {
        val updated = albums.value.map { album ->
            if (album.id == id) update(album) else album
        }
        dataStore.saveAlbums(updated)
    }

}
