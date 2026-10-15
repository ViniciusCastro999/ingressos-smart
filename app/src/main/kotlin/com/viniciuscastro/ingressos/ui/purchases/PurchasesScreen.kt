package com.viniciuscastro.ingressos.ui.purchases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viniciuscastro.ingressos.R
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.ui.common.Formatters
import com.viniciuscastro.ingressos.ui.common.LoadingState
import com.viniciuscastro.ingressos.ui.common.MessageState
import com.viniciuscastro.ingressos.ui.common.StatusChip
import com.viniciuscastro.ingressos.ui.designsystem.BrandBottomBar
import com.viniciuscastro.ingressos.ui.designsystem.BrandCard
import com.viniciuscastro.ingressos.ui.designsystem.BrandColors
import com.viniciuscastro.ingressos.ui.designsystem.BrandHeader
import com.viniciuscastro.ingressos.ui.designsystem.BrandTab
import com.viniciuscastro.ingressos.ui.designsystem.DateBlock
import com.viniciuscastro.ingressos.ui.designsystem.Eyebrow

@Composable
fun PurchasesRoute(
    onHomeClick: () -> Unit,
    onPurchaseClick: (purchaseId: String) -> Unit,
    viewModel: PurchasesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PurchasesScreen(state, onHomeClick, onPurchaseClick)
}

@Composable
fun PurchasesScreen(state: PurchasesUiState, onHomeClick: () -> Unit, onPurchaseClick: (purchaseId: String) -> Unit) {
    Scaffold(
        containerColor = BrandColors.Canvas,
        topBar = { BrandHeader() },
        bottomBar = { BrandBottomBar(selected = BrandTab.Tickets, onHome = onHomeClick, onTickets = {}) },
    ) { padding ->
        when (state) {
            PurchasesUiState.Loading -> LoadingState(Modifier.padding(padding))
            is PurchasesUiState.Content -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    top = padding.calculateTopPadding() + 14.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "title") {
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Eyebrow(stringResource(R.string.purchases_eyebrow), Modifier.padding(bottom = 10.dp))
                        Text(stringResource(R.string.purchases_title), style = MaterialTheme.typography.headlineMedium)
                    }
                }
                if (state.purchases.isEmpty()) {
                    item(key = "empty") { MessageState(stringResource(R.string.purchases_empty)) }
                }
                items(state.purchases, key = Purchase::id) { purchase ->
                    PurchaseCard(purchase, onClick = { onPurchaseClick(purchase.id) })
                }
            }
        }
    }
}

@Composable
private fun PurchaseCard(purchase: Purchase, onClick: () -> Unit) {
    BrandCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(15.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            DateBlock(Formatters.dayOfMonth(purchase.event.startsAt), Formatters.monthShort(purchase.event.startsAt))
            VerticalDivider(Modifier.fillMaxHeight(), thickness = 1.dp, color = BrandColors.Line)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    purchase.event.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${Formatters.dateTime(purchase.createdAt)} · " +
                        pluralStringResource(R.plurals.purchase_tickets, purchase.quantity, purchase.quantity),
                    fontSize = 11.sp,
                    color = BrandColors.Muted,
                    maxLines = 1,
                )
                Text(Formatters.money(purchase.total), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            StatusChip(purchase.status)
        }
    }
}
