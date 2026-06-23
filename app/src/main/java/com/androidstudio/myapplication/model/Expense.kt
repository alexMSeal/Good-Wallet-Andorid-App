package com.androidstudio.myapplication.model

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val type: String = "Expense",
    val category: String = "",
    val method: String = "",
    val caption: String = "",
    val imageUri: String? = null,
    val album: String = "Default",
)

data class User(
    val name: String,
    val profilePictureUrl: String? = null
)