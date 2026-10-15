package com.viniciuscastro.ingressos.domain.common

import com.viniciuscastro.ingressos.domain.model.PurchaseStatus

/**
 * Structured business events: the raw material for KPIs such as approval rate or duplicate charges
 * prevented. Events must never carry credentials or card data.
 */
fun interface Telemetry {
    fun track(event: TelemetryEvent)
}

sealed interface TelemetryEvent {
    data class PaymentDispatched(val purchaseId: String, val amountCents: Long) : TelemetryEvent
    data class PaymentDispatchFailed(val purchaseId: String, val cause: String) : TelemetryEvent
    data class DuplicateAttemptBlocked(val purchaseId: String, val status: PurchaseStatus) : TelemetryEvent
    data class ConcurrentPaymentBlocked(val pendingPurchaseId: String) : TelemetryEvent
    data class PaymentResolved(val purchaseId: String, val status: PurchaseStatus) : TelemetryEvent
    data class LateApprovalRecovered(val purchaseId: String, val previousStatus: PurchaseStatus) : TelemetryEvent
    data class CallbackIgnored(val purchaseId: String, val status: PurchaseStatus) : TelemetryEvent
    data class CallbackUnmatched(val reference: String?) : TelemetryEvent
    data class PurchaseReleased(val purchaseId: String) : TelemetryEvent
}
