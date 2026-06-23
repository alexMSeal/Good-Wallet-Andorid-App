package com.androidstudio.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.androidstudio.myapplication.ui.theme.MyApplicationTheme
import com.androidstudio.myapplication.design.navgraph.MainNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                MainNavigation()
            }
        }
    }
}