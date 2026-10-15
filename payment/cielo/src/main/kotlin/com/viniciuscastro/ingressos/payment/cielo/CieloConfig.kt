package com.viniciuscastro.ingressos.payment.cielo

/**
 * Integration settings. [callbackUri] must match the app's intent-filter: Cielo Smart opens it with
 * the result appended as `?response=<base64>&responsecode=<n>`.
 */
data class CieloConfig(
    val clientId: String,
    val accessToken: String,
    val callbackUri: String,
    val terminalScheme: String = DEFAULT_TERMINAL_SCHEME,
    val merchantCode: String? = null,
) {
    // Credentials must never reach logs or crash reports.
    override fun toString(): String =
        "CieloConfig(clientId=***, accessToken=***, callbackUri=$callbackUri, terminalScheme=$terminalScheme)"

    companion object {
        const val DEFAULT_TERMINAL_SCHEME = "lio"
    }
}
