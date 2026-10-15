package com.viniciuscastro.ingressos.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.usecase.ObservePurchaseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface PurchasesUiState {
    data object Loading : PurchasesUiState
    data class Content(val purchases: List<Purchase>) : PurchasesUiState
}

@HiltViewModel
class PurchasesViewModel @Inject constructor(
    observePurchaseHistory: ObservePurchaseHistoryUseCase,
) : ViewModel() {

    val uiState: StateFlow<PurchasesUiState> = observePurchaseHistory()
        .map<List<Purchase>, PurchasesUiState>(PurchasesUiState::Content)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchasesUiState.Loading)
}
