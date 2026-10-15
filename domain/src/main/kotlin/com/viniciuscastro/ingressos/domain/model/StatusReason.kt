package com.viniciuscastro.ingressos.domain.model

/** Why a purchase is not approved. Typed so the UI owns the wording (and its translation). */
sealed interface StatusReason {

    /** Message reported by the terminal itself (e.g. "CANCELADO PELO USUÁRIO"). */
    data class Terminal(val code: Int?, val message: String) : StatusReason

    /** The terminal approved an amount different from the purchase total. */
    data object AmountMismatch : StatusReason

    /** The operator released the purchase because the terminal never answered. */
    data object NoTerminalResponse : StatusReason

    /** The payment app is not installed or refused the request. */
    data object TerminalUnavailable : StatusReason
}
