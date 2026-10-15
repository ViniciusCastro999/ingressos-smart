package com.viniciuscastro.ingressos.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viniciuscastro.ingressos.R
import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.ui.common.Formatters
import com.viniciuscastro.ingressos.ui.common.LoadingState
import com.viniciuscastro.ingressos.ui.common.MessageState
import com.viniciuscastro.ingressos.ui.designsystem.BrandBottomBar
import com.viniciuscastro.ingressos.ui.designsystem.BrandCard
import com.viniciuscastro.ingressos.ui.designsystem.BrandColors
import com.viniciuscastro.ingressos.ui.designsystem.BrandHeader
import com.viniciuscastro.ingressos.ui.designsystem.BrandIcons
import com.viniciuscastro.ingressos.ui.designsystem.BrandRadius
import com.viniciuscastro.ingressos.ui.designsystem.BrandTab
import com.viniciuscastro.ingressos.ui.designsystem.DateBlock
import com.viniciuscastro.ingressos.ui.designsystem.Eyebrow
import com.viniciuscastro.ingressos.ui.designsystem.InfoCard
import com.viniciuscastro.ingressos.ui.designsystem.InfoTone
import com.viniciuscastro.ingressos.ui.designsystem.SectionTitle

@Composable
fun EventsRoute(
    onEventClick: (eventId: String) -> Unit,
    onTicketsClick: () -> Unit,
    onPendingPurchaseClick: (purchaseId: String) -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    EventsScreen(state, query, viewModel::onQueryChange, onEventClick, onTicketsClick, onPendingPurchaseClick)
}

@Composable
fun EventsScreen(
    state: EventsUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onEventClick: (eventId: String) -> Unit,
    onTicketsClick: () -> Unit,
    onPendingPurchaseClick: (purchaseId: String) -> Unit,
) {
    Scaffold(
        containerColor = BrandColors.Canvas,
        topBar = { BrandHeader() },
        bottomBar = { BrandBottomBar(selected = BrandTab.Home, onHome = {}, onTickets = onTicketsClick) },
    ) { padding ->
        when (state) {
            EventsUiState.Loading -> LoadingState(Modifier.padding(padding))
            EventsUiState.Error -> MessageState(stringResource(R.string.events_error), Modifier.padding(padding))
            is EventsUiState.Content -> Column(Modifier.padding(top = padding.calculateTopPadding())) {
                // Pinned above the list (not a list item), so it is visible whatever the scroll position.
                state.pendingPurchaseId?.let { purchaseId ->
                    PendingPaymentBanner(onClick = { onPendingPurchaseClick(purchaseId) })
                }
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = 14.dp,
                        bottom = padding.calculateBottomPadding() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "hero") { Hero(query, onQueryChange) }
                    item(key = "section") { SectionRow(state.events.size) }
                    if (state.events.isEmpty()) {
                        item(key = "empty") {
                            MessageState(
                                if (state.query.isBlank()) {
                                    stringResource(R.string.events_empty)
                                } else {
                                    stringResource(R.string.events_search_empty, state.query.trim())
                                },
                            )
                        }
                    }
                    items(state.events, key = Event::id) { event ->
                        EventCard(event, onClick = { onEventClick(event.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(query: String, onQueryChange: (String) -> Unit) {
    Column {
        Eyebrow(stringResource(R.string.events_eyebrow), Modifier.padding(bottom = 10.dp))
        Text(stringResource(R.string.events_title), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(22.dp))
        SearchField(query, onQueryChange)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(BrandRadius.Small)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = BrandColors.Ink, fontSize = 13.sp),
        cursorBrush = SolidColor(BrandColors.Cielo),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(4.dp, shape, ambientColor = BrandColors.CieloDark.copy(alpha = 0.05f), spotColor = BrandColors.CieloDark.copy(alpha = 0.05f))
                    .background(BrandColors.Surface, shape)
                    .border(1.dp, BrandColors.Line, shape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(BrandIcons.Search, contentDescription = null, tint = BrandColors.Placeholder, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.events_search_placeholder), color = BrandColors.Placeholder, fontSize = 13.sp)
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        BrandIcons.Close,
                        contentDescription = stringResource(R.string.events_search_clear),
                        tint = BrandColors.Muted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(role = Role.Button) { onQueryChange("") },
                    )
                }
            }
        },
    )
}

@Composable
private fun SectionRow(count: Int) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(stringResource(R.string.events_section), Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.events_count, count, count),
            color = BrandColors.Cielo,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PendingPaymentBanner(onClick: () -> Unit) {
    InfoCard(
        icon = BrandIcons.Terminal,
        title = stringResource(R.string.pending_banner_title),
        body = stringResource(R.string.pending_banner_body),
        tone = InfoTone.Warning,
        modifier = Modifier
            .padding(start = 22.dp, end = 22.dp, top = 4.dp)
            .clickable(role = Role.Button, onClick = onClick),
        action = {
            Text(
                stringResource(R.string.pending_banner_action),
                color = BrandColors.Cielo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    BrandCard(onClick = onClick, enabled = !event.isSoldOut, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(15.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            DateBlock(Formatters.dayOfMonth(event.startsAt), Formatters.monthShort(event.startsAt))
            VerticalDivider(Modifier.fillMaxHeight(), thickness = 1.dp, color = BrandColors.Line)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${event.venue} · ${Formatters.time(event.startsAt)}",
                    fontSize = 11.sp,
                    color = BrandColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PriceLine(event)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(BrandColors.Sky, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(BrandIcons.Chevron, contentDescription = null, tint = BrandColors.Cielo, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PriceLine(event: Event) {
    val price = Formatters.money(event.ticketPrice)
    val (availability, color) = when {
        event.isSoldOut -> stringResource(R.string.event_sold_out) to BrandColors.Danger
        event.availableTickets <= LOW_STOCK_THRESHOLD ->
            pluralStringResource(R.plurals.event_last_tickets, event.availableTickets, event.availableTickets) to
                BrandColors.Warning
        else -> pluralStringResource(R.plurals.event_available, event.availableTickets, event.availableTickets) to
            BrandColors.Muted
    }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = BrandColors.Ink, fontWeight = FontWeight.Bold)) { append(price) }
            append("  ·  ")
            withStyle(SpanStyle(color = color, fontWeight = if (color == BrandColors.Muted) FontWeight.Normal else FontWeight.Bold)) {
                append(availability)
            }
        },
        fontSize = 11.sp,
        color = BrandColors.Muted,
        maxLines = 1,
    )
}

private const val LOW_STOCK_THRESHOLD = 10
