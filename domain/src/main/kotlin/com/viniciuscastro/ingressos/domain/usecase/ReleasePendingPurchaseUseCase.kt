package com.viniciuscastro.ingressos.domain.usecase

import com.viniciuscastro.ingressos.domain.common.Clock
import com.viniciuscastro.ingressos.domain.common.Telemetry
import com.viniciuscastro.ingressos.domain.common.TelemetryEvent
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository

/**
 * Escape hatch for a terminal that never answered (e.g. powered off mid-payment). The purchase becomes
 * UNCONFIRMED instead of FAILED: we do not know whether money was captured, so a late approval must
 * still be able to resolve it, and new payments are unblocked.
 */
class ReleasePendingPurchaseUseCase(
    private val purchases: PurchaseRepository,
    private val clock: Clock,
    private val telemetry: Telemetry,
) {
    suspend operator fun invoke(purchaseId: String): Boolean {
        val purchase = purchases.find(purchaseId)
        if (purchase?.status != PurchaseStatus.PENDING) return false

        val released = purchases.update(purchase.markUnconfirmed(clock.now()), expectedStatus = PurchaseStatus.PENDING)
        if (released) telemetry.track(TelemetryEvent.PurchaseReleased(purchaseId))
        return released
    }
}
