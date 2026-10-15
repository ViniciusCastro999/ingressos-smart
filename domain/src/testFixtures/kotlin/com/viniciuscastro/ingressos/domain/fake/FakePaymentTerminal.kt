package com.viniciuscastro.ingressos.domain.fake

import com.viniciuscastro.ingressos.domain.payment.DispatchResult
import com.viniciuscastro.ingressos.domain.payment.PaymentRequest
import com.viniciuscastro.ingressos.domain.payment.PaymentTerminal
import java.util.concurrent.CopyOnWriteArrayList

class FakePaymentTerminal(
    var dispatchResult: DispatchResult = DispatchResult.Dispatched,
) : PaymentTerminal {

    /** Every payment that reached the terminal: one entry means one (potential) charge. */
    val requests: MutableList<PaymentRequest> = CopyOnWriteArrayList()

    /** Suspends inside the dispatch, widening the window in which a double tap could slip through. */
    var onDispatch: suspend () -> Unit = {}

    override suspend fun requestPayment(request: PaymentRequest): DispatchResult {
        onDispatch()
        requests += request
        return dispatchResult
    }
}
