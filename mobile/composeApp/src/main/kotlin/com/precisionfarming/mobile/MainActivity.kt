package com.precisionfarming.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.precisionfarming.mobile.data.TokenStore
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.ui.AppRoot
import com.precisionfarming.mobile.ui.theme.AgOsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleStore.init(this)
        TokenStore.init(this)
        OfflineRuntime.init(this)
        setContent {
            AgOsTheme { AppRoot() }
        }
    }
}
