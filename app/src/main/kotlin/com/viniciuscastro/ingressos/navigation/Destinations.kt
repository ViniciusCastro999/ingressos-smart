package com.viniciuscastro.ingressos.navigation

import kotlinx.serialization.Serializable

// Type-safe routes. Property names are also the SavedStateHandle keys read by the ViewModels.

@Serializable
data object EventsDestination

@Serializable
data class CheckoutDestination(val eventId: String, val quantity: Int = 1)

@Serializable
data class ReceiptDestination(val purchaseId: String)

@Serializable
data object PurchasesDestination
