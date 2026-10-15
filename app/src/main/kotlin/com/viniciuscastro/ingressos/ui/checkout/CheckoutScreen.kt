package com.viniciuscastro.ingressos.ui.checkout

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viniciuscastro.ingressos.R
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.ui.common.Formatters
import com.viniciuscastro.ingressos.ui.common.LoadingState
import com.viniciuscastro.ingressos.ui.common.MessageState
import com.viniciuscastro.ingressos.ui.common.hintRes
import com.viniciuscastro.ingressos.ui.common.icon
import com.viniciuscastro.ingressos.ui.common.labelRes
import com.viniciuscastro.ingressos.ui.designsystem.BottomActionBar
import com.viniciuscastro.ingressos.ui.designsystem.BrandCard
import com.viniciuscastro.ingressos.ui.designsystem.BrandColors
import com.viniciuscastro.ingressos.ui.designsystem.BrandDivider
import com.viniciuscastro.ingressos.ui.designsystem.BrandHeader
import com.viniciuscastro.ingressos.ui.designsystem.BrandIcons
import com.viniciuscastro.ingressos.ui.designsystem.BrandRadius
import com.viniciuscastro.ingressos.ui.designsystem.Pill
import com.viniciuscastro.ingressos.ui.designsystem.PrimaryButton
import com.viniciuscastro.ingressos.ui.designsystem.QuantityStepper
import com.viniciuscastro.ingressos.ui.designsystem.SectionTitle
import com.viniciuscastro.ingressos.ui.designsystem.TrustNote

@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onShowReceipt: (purchaseId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CheckoutEffect.ShowReceipt -> onShowReceipt(effect.purchaseId)
                is CheckoutEffect.ShowMessage -> {
                    val message = effect.message
                    val pendingId = (message as? CheckoutMessage.AnotherPaymentInProgress)?.purchaseId
                    val result = snackbarHostState.showSnackbar(
                        message = message.text(resources),
                        actionLabel = pendingId?.let { resources.getString(R.string.message_view) },
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed && pendingId != null) onShowReceipt(pendingId)
                }
            }
        }
    }

    CheckoutScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDecrease = viewModel::onDecrease,
        onIncrease = viewModel::onIncrease,
        onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
        onPay = viewModel::onPayClicked,
    )
}

private fun CheckoutMessage.text(resources: Resources): String = when (this) {
    is CheckoutMessage.AnotherPaymentInProgress -> resources.getString(R.string.message_another_payment)
    CheckoutMessage.SoldOut -> resources.getString(R.string.message_sold_out)
    CheckoutMessage.EventUnavailable -> resources.getString(R.string.message_event_unavailable)
    is CheckoutMessage.InvalidQuantity -> resources.getString(R.string.message_invalid_quantity, min, max)
    CheckoutMessage.UnexpectedError -> resources.getString(R.string.message_unexpected_error)
}

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onPaymentMethodSelected: (PaymentMethod?) -> Unit,
    onPay: () -> Unit,
) {
    Scaffold(
        containerColor = BrandColors.Canvas,
        topBar = { BrandHeader(title = stringResource(R.string.checkout_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { if (state is CheckoutUiState.Ready) PayBar(state, onPay) },
    ) { padding ->
        when (state) {
            CheckoutUiState.Loading -> LoadingState(Modifier.padding(padding))
            CheckoutUiState.NotFound -> MessageState(stringResource(R.string.checkout_event_not_found), Modifier.padding(padding))
            is CheckoutUiState.Ready -> Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 18.dp),
            ) {
                EventHeader(state)
                BrandDivider(Modifier.padding(vertical = 22.dp))
                SectionTitle(stringResource(R.string.checkout_choose_tickets))
                TicketOption(state, onDecrease, onIncrease)
                SectionTitle(stringResource(R.string.checkout_how_to_pay), Modifier.padding(top = 28.dp, bottom = 4.dp))
                PAYMENT_OPTIONS.forEach { option ->
                    PaymentMethodCard(
                        method = option,
                        selected = state.paymentMethod == option,
                        enabled = !state.isSubmitting,
                        onClick = { onPaymentMethodSelected(option) },
                    )
                }
                TrustNote(stringResource(R.string.checkout_secure_note), Modifier.padding(top = 18.dp))
            }
        }
    }
}

@Composable
private fun EventHeader(state: CheckoutUiState.Ready) {
    val event = state.event
    Pill(event.city.uppercase(), container = BrandColors.Sky, content = BrandColors.Cielo, modifier = Modifier.padding(bottom = 12.dp))
    Text(event.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 18.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetaRow(BrandIcons.Calendar, Formatters.eventDateTime(event.startsAt))
        MetaRow(BrandIcons.Pin, event.venue, event.city)
    }
}

@Composable
private fun MetaRow(icon: ImageVector, text: String, secondary: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = BrandColors.Cielo, modifier = Modifier.size(17.dp))
        Column {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (secondary != null) Text(secondary, fontSize = 13.sp, color = BrandColors.Muted)
        }
    }
}

@Composable
private fun TicketOption(state: CheckoutUiState.Ready, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    BrandCard(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.checkout_ticket_name), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.checkout_quantity_limit, state.maxQuantity),
                    fontSize = 11.sp,
                    color = BrandColors.Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    Formatters.money(state.event.ticketPrice),
                    color = BrandColors.Cielo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
            QuantityStepper(
                value = state.quantity,
                canDecrease = !state.isSubmitting && state.quantity > 1,
                canIncrease = !state.isSubmitting && state.quantity < state.maxQuantity,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                decreaseDescription = stringResource(R.string.checkout_decrease),
                increaseDescription = stringResource(R.string.checkout_increase),
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(method: PaymentMethod?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(BrandRadius.Medium)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = BrandColors.Surface,
        border = if (selected) BorderStroke(2.dp, BrandColors.Cielo) else BorderStroke(1.dp, BrandColors.Line),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(BrandColors.Sky, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(method.icon(), contentDescription = null, tint = BrandColors.Cielo, modifier = Modifier.size(20.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(stringResource(method.labelRes()), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(method.hintRes()), fontSize = 11.sp, color = BrandColors.Muted)
            }
            RadioMark(selected)
        }
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    Box(
        Modifier
            .size(18.dp)
            .border(if (selected) 5.dp else 1.5.dp, if (selected) BrandColors.Cielo else BrandColors.Line, CircleShape),
    )
}

@Composable
private fun PayBar(state: CheckoutUiState.Ready, onPay: () -> Unit) {
    BottomActionBar {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.widthIn(min = 96.dp)) {
                Text(stringResource(R.string.checkout_total), fontSize = 10.sp, color = BrandColors.Muted)
                Text(
                    Formatters.money(state.total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            PrimaryButton(
                text = stringResource(if (state.isSubmitting) R.string.checkout_sending else R.string.checkout_pay),
                onClick = onPay,
                enabled = state.canPay,
                loading = state.isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

private val PAYMENT_OPTIONS: List<PaymentMethod?> = listOf(null) + PaymentMethod.entries
