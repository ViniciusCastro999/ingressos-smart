package com.viniciuscastro.ingressos.payment.cielo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.viniciuscastro.ingressos.domain.payment.DispatchResult
import com.viniciuscastro.ingressos.domain.payment.PaymentRequest
import com.viniciuscastro.ingressos.domain.payment.PaymentTerminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hands the payment to Cielo Smart through its mandatory abstraction layer, the `lio://` deep link
 * (direct access to the terminal hardware SDK is forbidden by Cielo's integration policy).
 * Uses the application context so it can be a singleton without leaking an Activity.
 */
class CieloDeepLinkTerminal(
    private val context: Context,
    private val deepLinks: CieloDeepLinkFactory,
) : PaymentTerminal {

    override suspend fun requestPayment(request: PaymentRequest): DispatchResult =
        withContext(Dispatchers.Main.immediate) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinks.payment(request)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                DispatchResult.Dispatched
            } catch (e: ActivityNotFoundException) {
                DispatchResult.Unavailable("No app handles ${deepLinks.terminalScheme}:// (Cielo Smart not installed)")
            } catch (e: SecurityException) {
                DispatchResult.Unavailable("Payment app refused the request: ${e.message}")
            }
        }
}
