package com.viniciuscastro.ingressos.domain.model

/** A ticket only exists for an approved purchase: it is issued in the same transition that approves it. */
data class Ticket(
    val id: String,
    val purchaseId: String,
    val eventId: String,
    val number: Int,
) {
    /**
     * Content encoded in the QR Code. It links the ticket to the purchase; validating it at the gate
     * would require a signature issued by a backend (see README, "O que faria com mais tempo").
     */
    val qrCodeContent: String get() = "INGRESSO:$eventId:$purchaseId:$id"
}
