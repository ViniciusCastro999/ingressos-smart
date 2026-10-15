package com.viniciuscastro.ingressos.ui.receipt

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.viniciuscastro.ingressos.ui.designsystem.BrandColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrCodeEncoder {

    private const val DARK = 0xFF10242E.toInt() // identity's QR ink
    private const val LIGHT = 0xFFFFFFFF.toInt()

    fun encode(content: String, sizePx: Int): BitMatrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
            EncodeHintType.MARGIN to 0,
        ),
    )

    fun BitMatrix.toPixels(): IntArray = IntArray(width * height) { index ->
        if (get(index % width, index / width)) DARK else LIGHT
    }
}

/** QR on a white tile with the identity's hairline frame; always dark-on-white for scanners. */
@Composable
fun QrCode(content: String, contentDescription: String, modifier: Modifier = Modifier, size: Dp = 164.dp) {
    val innerSize = size - 20.dp
    val sizePx = with(LocalDensity.current) { innerSize.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, content, sizePx) {
        value = withContext(Dispatchers.Default) {
            with(QrCodeEncoder) {
                val matrix = encode(content, sizePx)
                Bitmap.createBitmap(matrix.toPixels(), matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }
        }
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, shape)
            .border(1.dp, BrandColors.Line, shape)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap == null) {
            CircularProgressIndicator(Modifier.size(24.dp), color = BrandColors.Cielo, strokeWidth = 2.dp)
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
