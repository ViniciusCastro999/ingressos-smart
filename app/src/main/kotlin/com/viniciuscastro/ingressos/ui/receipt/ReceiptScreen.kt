package com.viniciuscastro.ingressos.ui.receipt

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viniciuscastro.ingressos.R
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.StatusReason
import com.viniciuscastro.ingressos.domain.model.Ticket
import com.viniciuscastro.ingressos.domain.payment.PaymentReceipt
import com.viniciuscastro.ingressos.ui.common.Formatters
import com.viniciuscastro.ingressos.ui.common.LoadingState
import com.viniciuscastro.ingressos.ui.common.MessageState
import com.viniciuscastro.ingressos.ui.common.labelRes
import com.viniciuscastro.ingressos.ui.common.style
import com.viniciuscastro.ingressos.ui.designsystem.BrandCard
import com.viniciuscastro.ingressos.ui.designsystem.BrandColors
import com.viniciuscastro.ingressos.ui.designsystem.BrandHeader
import com.viniciuscastro.ingressos.ui.designsystem.BrandIcons
import com.viniciuscastro.ingressos.ui.designsystem.BrandRadius
import com.viniciuscastro.ingressos.ui.designsystem.DashedRule
import com.viniciuscastro.ingressos.ui.designsystem.InfoCard
import com.viniciuscastro.ingressos.ui.designsystem.Pill
import com.viniciuscastro.ingressos.ui.designsystem.PrimaryButton
import com.viniciuscastro.ingressos.ui.designsystem.RowsCard
import com.viniciuscastro.ingressos.ui.designsystem.SecondaryButton
import com.viniciuscastro.ingressos.ui.designsystem.SectionTitle

@Composable
fun ReceiptRoute(
    onBack: () -> Unit,
    onRetry: (eventId: String, quantity: Int) -> Unit,
    onDone: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReceiptScreen(state, onBack, onRetry, onDone, onReleaseConfirmed = viewModel::onReleaseConfirmed)
}

@Composable
fun ReceiptScreen(
    state: ReceiptUiState,
    onBack: () -> Unit,
    onRetry: (eventId: String, quantity: Int) -> Unit,
    onDone: () -> Unit,
    onReleaseConfirmed: () -> Unit,
) {
    var showReleaseDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = BrandColors.Canvas,
        topBar = { BrandHeader(title = stringResource(R.string.receipt_title), onBack = onBack) },
    ) { padding ->
        when (state) {
            ReceiptUiState.Loading -> LoadingState(Modifier.padding(padding))
            ReceiptUiState.NotFound -> MessageState(stringResource(R.string.receipt_not_found), Modifier.padding(padding))
            is ReceiptUiState.Content -> {
                val purchase = state.purchase
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = padding.calculateTopPadding() + 12.dp,
                        bottom = padding.calculateBottomPadding() + 30.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "result") { ResultHeader(purchase, onReleaseClick = { showReleaseDialog = true }) }
                    if (purchase.tickets.isNotEmpty()) {
                        item(key = "valid") {
                            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                                Pill(
                                    stringResource(R.string.receipt_valid_badge),
                                    container = BrandColors.SuccessSoft,
                                    content = BrandColors.Success,
                                    dot = true,
                                )
                            }
                        }
                        items(purchase.tickets, key = Ticket::id) { ticket -> DigitalTicket(purchase, ticket) }
                        item(key = "protect") {
                            InfoCard(
                                icon = BrandIcons.Shield,
                                title = stringResource(R.string.receipt_protect_title),
                                body = stringResource(R.string.receipt_protect_body),
                            )
                        }
                    }
                    item(key = "summary") { PurchaseSummary(purchase) }
                    purchase.receipt?.let { receipt -> item(key = "payment") { PaymentData(receipt) } }
                    item(key = "actions") { Actions(purchase, onRetry, onDone) }
                }
            }
        }
    }

    if (showReleaseDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseDialog = false },
            containerColor = BrandColors.Surface,
            title = { Text(stringResource(R.string.release_dialog_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.release_dialog_body), style = MaterialTheme.typography.bodyMedium, color = BrandColors.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showReleaseDialog = false
                    onReleaseConfirmed()
                }) { Text(stringResource(R.string.release_dialog_confirm), color = BrandColors.Cielo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showReleaseDialog = false }) {
                    Text(stringResource(R.string.release_dialog_dismiss), color = BrandColors.Muted)
                }
            },
        )
    }
}

@Composable
private fun ResultHeader(purchase: Purchase, onReleaseClick: () -> Unit) {
    val style = purchase.status.style()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (purchase.status == PurchaseStatus.PENDING) ProcessingRing() else ResultIcon(style.icon, style.container, style.content)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(style.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(9.dp))
        Text(
            statusCopy(purchase),
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 310.dp),
        )
        (purchase.reason as? StatusReason.Terminal)?.let { reason ->
            Text(
                reason.code?.let { stringResource(R.string.reason_terminal_with_code, reason.message, it) }
                    ?: stringResource(R.string.reason_terminal, reason.message),
                style = MaterialTheme.typography.bodySmall,
                color = BrandColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        when {
            purchase.status == PurchaseStatus.APPROVED && purchase.receipt != null ->
                ApprovalReceipt(purchase.receipt!!, Modifier.padding(top = 26.dp))
            purchase.status == PurchaseStatus.PENDING ->
                SecondaryButton(
                    text = stringResource(R.string.receipt_release),
                    onClick = onReleaseClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                )
        }
    }
}

/** The identity's processing state: a sky disc with a rotating arc around it. */
@Composable
private fun ProcessingRing() {
    val rotation by rememberInfiniteTransition(label = "processing").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Box(Modifier.size(118.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .rotate(rotation),
        ) {
            drawArc(
                color = BrandColors.Cielo,
                startAngle = -135f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Box(
            Modifier
                .size(104.dp)
                .background(BrandColors.Sky, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(BrandIcons.Card, contentDescription = null, tint = BrandColors.Cielo, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun ResultIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, container: Color, content: Color) {
    Box(
        Modifier
            .size(112.dp)
            .background(container.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(92.dp)
                .background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun statusCopy(purchase: Purchase): String = when (val reason = purchase.reason) {
    StatusReason.AmountMismatch -> stringResource(
        R.string.reason_amount_mismatch,
        Formatters.money(purchase.receipt?.paidAmount ?: Money.ZERO),
        Formatters.money(purchase.total),
    )
    StatusReason.NoTerminalResponse -> stringResource(R.string.reason_no_terminal_response)
    StatusReason.TerminalUnavailable -> stringResource(R.string.reason_terminal_unavailable)
    is StatusReason.Terminal, null -> when (purchase.status) {
        PurchaseStatus.PENDING -> stringResource(R.string.receipt_copy_pending)
        PurchaseStatus.APPROVED -> stringResource(R.string.receipt_copy_approved)
        PurchaseStatus.DECLINED -> stringResource(R.string.receipt_copy_declined)
        PurchaseStatus.CANCELED -> stringResource(R.string.receipt_copy_canceled)
        PurchaseStatus.FAILED -> stringResource(R.string.receipt_copy_failed)
        PurchaseStatus.UNCONFIRMED -> stringResource(R.string.reason_no_terminal_response)
    }
}

@Composable
private fun ApprovalReceipt(receipt: PaymentReceipt, modifier: Modifier = Modifier) {
    BrandCard(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LabeledValue(stringResource(R.string.receipt_paid_amount), Formatters.money(receipt.paidAmount))
            receipt.authorizationCode?.let { LabeledValue(stringResource(R.string.receipt_authorization), it) }
            receipt.nsu?.let { LabeledValue(stringResource(R.string.receipt_nsu), it) }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 10.sp, color = BrandColors.Muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DigitalTicket(purchase: Purchase, ticket: Ticket) {
    BrandCard(Modifier.fillMaxWidth(), radius = BrandRadius.Large) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(144.dp)
                .background(Brush.linearGradient(listOf(BrandColors.CieloDark, BrandColors.Cielo)))
                .padding(18.dp),
        ) {
            Pill(
                stringResource(R.string.receipt_ticket_label, ticket.number, purchase.tickets.size),
                container = Color.White.copy(alpha = 0.18f),
                content = Color.White,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${Formatters.shortDate(purchase.event.startsAt)} · ${Formatters.time(purchase.event.startsAt)}",
                    color = BrandColors.TicketCoverLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(purchase.event.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(purchase.event.venue, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }
        Perforation()
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QrCode(
                content = ticket.qrCodeContent,
                contentDescription = stringResource(R.string.receipt_ticket_qr, ticket.number),
            )
            Text(
                stringResource(R.string.receipt_ticket_present),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "#${Formatters.shortId(purchase.id)}-${ticket.number.toString().padStart(2, '0')}",
                fontSize = 10.sp,
                color = BrandColors.Muted,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.05.em,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Ticket stub perforation: two half-circle cut-outs joined by a dashed rule. */
@Composable
private fun Perforation() {
    Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .offset(x = (-10).dp)
                .size(20.dp)
                .background(BrandColors.Canvas, CircleShape),
        )
        DashedRule(Modifier.weight(1f))
        Box(
            Modifier
                .offset(x = 10.dp)
                .size(20.dp)
                .background(BrandColors.Canvas, CircleShape),
        )
    }
}

@Composable
private fun PurchaseSummary(purchase: Purchase) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.receipt_section_purchase), Modifier.padding(top = 8.dp))
        RowsCard(
            rows = listOf(
                stringResource(R.string.receipt_quantity_line, purchase.quantity, Formatters.money(purchase.unitPrice)) to
                    Formatters.money(purchase.total),
                stringResource(R.string.receipt_event) to purchase.event.title,
                stringResource(R.string.receipt_date) to Formatters.eventDateTime(purchase.event.startsAt),
                stringResource(R.string.receipt_venue) to purchase.event.venue,
                stringResource(R.string.receipt_method) to stringResource(purchase.paymentMethod.labelRes()),
                stringResource(R.string.receipt_order) to Formatters.shortId(purchase.id),
                stringResource(R.string.receipt_purchased_at) to Formatters.dateTime(purchase.createdAt),
            ),
            total = stringResource(R.string.receipt_total) to Formatters.money(purchase.total),
        )
    }
}

@Composable
private fun PaymentData(receipt: PaymentReceipt) {
    val card = listOfNotNull(receipt.cardBrand, receipt.cardMask?.takeIf { it.isNotBlank() })
        .joinToString(" ")
        .takeIf { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.receipt_section_payment), Modifier.padding(top = 8.dp))
        RowsCard(
            rows = listOfNotNull(
                stringResource(R.string.receipt_paid_amount) to Formatters.money(receipt.paidAmount),
                receipt.authorizationCode?.let { stringResource(R.string.receipt_authorization) to it },
                receipt.nsu?.let { stringResource(R.string.receipt_nsu) to it },
                card?.let { stringResource(R.string.receipt_card) to it },
                receipt.productName?.let { stringResource(R.string.receipt_product) to it },
                receipt.terminalId?.let { stringResource(R.string.receipt_terminal) to it },
                receipt.terminalOrderId?.let { stringResource(R.string.receipt_terminal_order) to Formatters.shortId(it) },
            ),
        )
    }
}

@Composable
private fun Actions(purchase: Purchase, onRetry: (eventId: String, quantity: Int) -> Unit, onDone: () -> Unit) {
    val canRetry = purchase.status != PurchaseStatus.PENDING && purchase.status != PurchaseStatus.APPROVED
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (canRetry) {
            PrimaryButton(
                text = stringResource(R.string.receipt_retry),
                onClick = { onRetry(purchase.event.id, purchase.quantity) },
                leadingIcon = BrandIcons.Refresh,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SecondaryButton(
            text = stringResource(R.string.receipt_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
