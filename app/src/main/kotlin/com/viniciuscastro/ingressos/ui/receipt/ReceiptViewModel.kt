package com.viniciuscastro.ingressos.ui.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.usecase.ObservePurchaseUseCase
import com.viniciuscastro.ingressos.domain.usecase.ReleasePendingPurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReceiptUiState {
    data object Loading : ReceiptUiState
    data object NotFound : ReceiptUiState
    data class Content(val purchase: Purchase) : ReceiptUiState
}

/**
 * Observes the purchase in the database, the single source of truth: when the terminal callback is
 * processed (even after process death) this screen updates by itself, from "pending" to the outcome.
 */
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePurchase: ObservePurchaseUseCase,
    private val releasePendingPurchase: ReleasePendingPurchaseUseCase,
) : ViewModel() {

    private val purchaseId: String = checkNotNull(savedStateHandle[ARG_PURCHASE_ID]) { "Missing $ARG_PURCHASE_ID" }

    val uiState: StateFlow<ReceiptUiState> = observePurchase(purchaseId)
        .map { purchase -> purchase?.let(ReceiptUiState::Content) ?: ReceiptUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReceiptUiState.Loading)

    fun onReleaseConfirmed() {
        viewModelScope.launch { releasePendingPurchase(purchaseId) }
    }

    companion object {
        const val ARG_PURCHASE_ID = "purchaseId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
