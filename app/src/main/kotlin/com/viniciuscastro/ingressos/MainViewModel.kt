package com.viniciuscastro.ingressos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viniciuscastro.ingressos.domain.payment.PaymentResultParser
import com.viniciuscastro.ingressos.domain.usecase.ProcessPaymentResult
import com.viniciuscastro.ingressos.domain.usecase.ProcessPaymentResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface MainEffect {
    data class ShowPurchase(val purchaseId: String) : MainEffect
    data object UnmatchedPaymentCallback : MainEffect
}

/** Receives the terminal callback delivered to the activity and records it. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val parser: PaymentResultParser,
    private val processPaymentResult: ProcessPaymentResultUseCase,
) : ViewModel() {

    private val effectChannel = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = effectChannel.receiveAsFlow()

    fun onPaymentCallback(callback: String) {
        viewModelScope.launch {
            // The outcome of a real charge must be recorded even if the screen goes away mid-write.
            val outcome = withContext(NonCancellable) { processPaymentResult(parser.parse(callback)) }
            effectChannel.send(
                when (outcome) {
                    is ProcessPaymentResult.Applied -> MainEffect.ShowPurchase(outcome.purchase.id)
                    is ProcessPaymentResult.AlreadyProcessed -> MainEffect.ShowPurchase(outcome.purchase.id)
                    is ProcessPaymentResult.Unmatched -> MainEffect.UnmatchedPaymentCallback
                },
            )
        }
    }
}
