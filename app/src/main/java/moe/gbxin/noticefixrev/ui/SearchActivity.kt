package moe.gbxin.noticefixrev.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.gbxin.noticefixrev.ui.theme.NoticeFixRevTheme
import moe.gbxin.noticefixrev.util.localizedContext

/**
 * Quick-management search, launched from the home top bar. Living in its own activity means
 * the system back gesture returns to the list instead of leaving the app.
 */
class SearchActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NoticeFixRevTheme {
                LaunchedEffect(Unit) { viewModel.refresh() }
                val state by viewModel.state.collectAsStateWithLifecycle()
                AppSearchScreen(
                    state = state,
                    onToggleApp = viewModel::setAppEnabled,
                    onBack = { finish() },
                )
            }
        }
    }
}
