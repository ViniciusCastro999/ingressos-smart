package com.viniciuscastro.ingressos.domain.model

enum class PurchaseStatus {
    /** Payment handed to the terminal, waiting for its callback. Blocks any other payment. */
    PENDING,
    APPROVED,
    DECLINED,
    CANCELED,
    FAILED,

    /**
     * The app could not confirm the outcome (no callback, or the terminal reported an amount that does
     * not match the purchase). The operator must check the terminal; a late callback can still resolve it.
     */
    UNCONFIRMED,
    ;

    val isFinal: Boolean get() = this == APPROVED || this == DECLINED || this == CANCELED || this == FAILED
}
