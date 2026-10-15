package com.viniciuscastro.ingressos.ui.common

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.viniciuscastro.ingressos.R
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.ui.designsystem.BrandColors
import com.viniciuscastro.ingressos.ui.designsystem.BrandIcons

data class StatusStyle(
    val icon: ImageVector,
    val container: Color,
    val content: Color,
    @param:StringRes val title: Int,
    @param:StringRes val label: Int,
)

/** Result colours follow the identity: success green, danger red for refusals, warning amber for errors. */
fun PurchaseStatus.style(): StatusStyle = when (this) {
    PurchaseStatus.PENDING -> StatusStyle(
        BrandIcons.Card, BrandColors.Sky, BrandColors.Cielo, R.string.status_pending, R.string.status_chip_pending,
    )
    PurchaseStatus.APPROVED -> StatusStyle(
        BrandIcons.Check, BrandColors.SuccessSoft, BrandColors.Success, R.string.status_approved, R.string.status_chip_approved,
    )
    PurchaseStatus.DECLINED -> StatusStyle(
        BrandIcons.Close, BrandColors.DangerSoft, BrandColors.Danger, R.string.status_declined, R.string.status_chip_declined,
    )
    PurchaseStatus.CANCELED -> StatusStyle(
        BrandIcons.Close, BrandColors.DangerSoft, BrandColors.Danger, R.string.status_canceled, R.string.status_chip_canceled,
    )
    PurchaseStatus.FAILED -> StatusStyle(
        BrandIcons.Alert, BrandColors.WarningSoft, BrandColors.Warning, R.string.status_failed, R.string.status_chip_failed,
    )
    PurchaseStatus.UNCONFIRMED -> StatusStyle(
        BrandIcons.Alert, BrandColors.WarningSoft, BrandColors.Warning, R.string.status_unconfirmed, R.string.status_chip_unconfirmed,
    )
}

@StringRes
fun PaymentMethod?.labelRes(): Int = when (this) {
    null -> R.string.payment_method_terminal
    PaymentMethod.CREDIT -> R.string.payment_method_credit
    PaymentMethod.DEBIT -> R.string.payment_method_debit
    PaymentMethod.PIX -> R.string.payment_method_pix
}

@StringRes
fun PaymentMethod?.hintRes(): Int = when (this) {
    null -> R.string.payment_method_terminal_hint
    PaymentMethod.CREDIT -> R.string.payment_method_credit_hint
    PaymentMethod.DEBIT -> R.string.payment_method_debit_hint
    PaymentMethod.PIX -> R.string.payment_method_pix_hint
}

fun PaymentMethod?.icon(): ImageVector = when (this) {
    null -> BrandIcons.Terminal
    PaymentMethod.CREDIT, PaymentMethod.DEBIT -> BrandIcons.Card
    PaymentMethod.PIX -> BrandIcons.Pix
}
