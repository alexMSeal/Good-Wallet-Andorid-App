package com.androidstudio.myapplication.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.androidstudio.myapplication.datastore.DataStoreManager
import com.androidstudio.myapplication.repository.AlbumRepository

class HomeScreenViewModelFactory(
    private val dataStoreManager: DataStoreManager,
    private val albumRepository: AlbumRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeScreenViewModel(dataStoreManager, albumRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
