package com.viniciuscastro.ingressos.payment.cielo

import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.io.encoding.Base64

/** Wire-format helpers shared by the adapter and the terminal simulator. */
object CieloProtocol {

    const val PAYMENT_ACTION = "payment"
    const val REQUEST_PARAM = "request"
    const val CALLBACK_PARAM = "urlCallback"
    const val RESPONSE_PARAM = "response"
    const val RESPONSE_CODE_PARAM = "responsecode"

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false // optional fields are omitted, as in Cielo's examples
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    private val base64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

    /** Single-line Base64, the equivalent of Android's `Base64.NO_WRAP` used in Cielo's samples. */
    fun encodeBase64(text: String): String = base64.encode(text.encodeToByteArray())

    /**
     * Lenient decoding: the terminal may wrap lines (Android `Base64.DEFAULT`) and URI handling may
     * have turned '+' into ' '. Both are repaired before decoding.
     */
    fun decodeBase64(encoded: String): String {
        val normalized = encoded.trim().replace(' ', '+').filterNot { it == '\n' || it == '\r' }
        return base64.decode(normalized).decodeToString()
    }

    fun encodeQueryValue(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    /** Reads a query parameter from a raw URI. '+' is kept as is: in Base64 it is data, not a space. */
    fun queryParameter(uri: String, name: String): String? {
        val query = uri.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
        return query.split('&')
            .firstOrNull { it.substringBefore('=') == name }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.let { URLDecoder.decode(it.replace("+", "%2B"), Charsets.UTF_8.name()) }
    }
}
