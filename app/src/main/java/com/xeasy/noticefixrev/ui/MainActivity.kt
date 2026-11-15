package com.xeasy.noticefixrev.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xeasy.noticefixrev.ui.theme.NoticeFixRevTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NoticeFixRevTheme {
                AppRoot()
            }
        }
    }
}
