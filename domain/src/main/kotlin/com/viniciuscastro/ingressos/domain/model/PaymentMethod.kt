package com.viniciuscastro.ingressos.domain.model

/** Payment method pre-selected in the app. `null` means the operator chooses it on the terminal. */
enum class PaymentMethod {
    CREDIT,
    DEBIT,
    PIX,
}
