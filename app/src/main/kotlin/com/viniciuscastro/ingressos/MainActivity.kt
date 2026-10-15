package com.viniciuscastro.ingressos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.viniciuscastro.ingressos.navigation.IngressosNavHost
import com.viniciuscastro.ingressos.navigation.showPurchase
import com.viniciuscastro.ingressos.ui.designsystem.IngressosTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow

/**
 * Single activity. `singleTask` so the Cielo Smart callback (sent from the payment app's task) is
 * delivered to the existing instance through [onNewIntent] instead of stacking a new one.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The identity is light-only, so system bar icons are always dark, whatever the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // A recreated activity still carries the original intent: only a fresh launch may process it.
        if (savedInstanceState == null) handlePaymentCallback(intent)
        setContent {
            IngressosTheme {
                IngressosApp(mainEffects = viewModel.effects)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePaymentCallback(intent)
    }

    private fun handlePaymentCallback(intent: Intent?) {
        intent?.paymentCallbackOrNull()?.let(viewModel::onPaymentCallback)
    }
}

/** The callback URI, if this intent is the payment terminal answering (and not a relaunch from Recents). */
private fun Intent.paymentCallbackOrNull(): String? {
    if (action != Intent.ACTION_VIEW) return null
    if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
    val uri = data ?: return null
    val isCallback = uri.scheme == BuildConfig.PAYMENT_CALLBACK_SCHEME && uri.host == BuildConfig.PAYMENT_CALLBACK_HOST
    return uri.toString().takeIf { isCallback }
}

@Composable
private fun IngressosApp(mainEffects: Flow<MainEffect>) {
    val navController = rememberNavController()
    val context = LocalContext.current
    LaunchedEffect(navController) {
        mainEffects.collect { effect ->
            when (effect) {
                is MainEffect.ShowPurchase -> navController.showPurchase(effect.purchaseId)
                MainEffect.UnmatchedPaymentCallback ->
                    Toast.makeText(context, R.string.message_unmatched_callback, Toast.LENGTH_LONG).show()
            }
        }
    }
    IngressosNavHost(navController)
}
