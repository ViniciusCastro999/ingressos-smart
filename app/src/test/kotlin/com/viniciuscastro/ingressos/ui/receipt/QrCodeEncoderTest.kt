package com.viniciuscastro.ingressos.ui.receipt

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.viniciuscastro.ingressos.domain.model.Ticket
import com.viniciuscastro.ingressos.ui.receipt.QrCodeEncoder.toPixels
import kotlin.test.Test
import kotlin.test.assertEquals

class QrCodeEncoderTest {

    @Test
    fun `ticket QR Code decodes back to the ticket content`() {
        val ticket = Ticket(id = "t-1", purchaseId = "pur-1", eventId = "evt-1", number = 1)

        val matrix = QrCodeEncoder.encode(ticket.qrCodeContent, sizePx = 300)
        val source = RGBLuminanceSource(matrix.width, matrix.height, matrix.toPixels())
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)))

        assertEquals("INGRESSO:evt-1:pur-1:t-1", decoded.text)
    }
}
