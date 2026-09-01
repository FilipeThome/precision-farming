package com.precisionfarming.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleStore.init(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF1B5E3B),
                    secondary = Color(0xFF0D9488),
                    background = Color(0xFFF4F1EA),
                    surface = Color.White,
                    onPrimary = Color.White,
                    onBackground = Color(0xFF1A1A1A),
                    onSurface = Color(0xFF1A1A1A),
                ),
            ) {
                AppRoot()
            }
        }
    }
}
