package moe.gbxin.noticefixrev.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import moe.gbxin.noticefixrev.ui.theme.NoticeFixRevTheme
import moe.gbxin.noticefixrev.util.localizedContext

/** Full-screen AGPL-3.0 viewer; back returns to settings instead of exiting the app. */
class LicenseActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NoticeFixRevTheme {
                LicenseScreen(onBack = { finish() })
            }
        }
    }
}
