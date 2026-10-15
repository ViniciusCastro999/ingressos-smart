package com.viniciuscastro.ingressos.ui.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.domain.usecase.ObserveEventsUseCase
import com.viniciuscastro.ingressos.domain.usecase.ObservePendingPurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data object Error : EventsUiState
    data class Content(
        val events: List<Event>,
        val query: String,
        val pendingPurchaseId: String?,
    ) : EventsUiState
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeEvents: ObserveEventsUseCase,
    observePendingPurchase: ObservePendingPurchaseUseCase,
) : ViewModel() {

    /** Bound directly to the search field (kept apart from uiState so typing never lags behind). */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val uiState: StateFlow<EventsUiState> =
        combine(observeEvents(), observePendingPurchase(), query) { events, pending, query ->
            EventsUiState.Content(events.filter { it.matches(query) }, query, pending?.id) as EventsUiState
        }
            .catch { emit(EventsUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EventsUiState.Loading)

    fun onQueryChange(query: String) {
        savedStateHandle[KEY_QUERY] = query
    }

    private companion object {
        const val KEY_QUERY = "events.query"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
