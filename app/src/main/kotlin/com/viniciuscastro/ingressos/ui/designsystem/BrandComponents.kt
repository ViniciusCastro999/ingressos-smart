package com.viniciuscastro.ingressos.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.viniciuscastro.ingressos.R

// Building blocks of the visual identity. Screens compose these instead of styling Material
// components ad hoc, so the brand lives in one place.

@Composable
fun BrandLogo(modifier: Modifier = Modifier, color: Color = BrandColors.Cielo) {
    Row(modifier) {
        Text(
            text = stringResource(R.string.brand_wordmark),
            color = color,
            fontFamily = BrandFont,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.066).em,
            lineHeight = 27.sp,
        )
        Box(
            Modifier
                .padding(start = 2.dp, top = 4.dp)
                .size(6.dp)
                .background(BrandColors.LogoDot, CircleShape),
        )
    }
}

/** "Smart" status pill: the app runs on (and talks to) a Cielo Smart terminal. */
@Composable
fun SmartPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(29.dp)
            .background(BrandColors.Surface, CircleShape)
            .border(1.dp, BrandColors.Line, CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(13.dp)
                .background(BrandColors.LiveHalo, CircleShape)
                .padding(3.dp)
                .background(BrandColors.LiveDot, CircleShape),
        )
        Text(
            text = stringResource(R.string.brand_smart_pill).uppercase(),
            color = BrandColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.05.em,
        )
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(BrandColors.Surface)
            .border(1.dp, BrandColors.Line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = BrandColors.Ink, modifier = Modifier.size(20.dp))
    }
}

/** Top bar: logo (or back) on the left, optional centred title, "Smart" pill on the right. */
@Composable
fun BrandHeader(title: String? = null, onBack: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandColors.Canvas)
            .statusBarsPadding()
            .height(76.dp)
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 10.dp),
    ) {
        if (onBack != null) {
            CircleIconButton(
                icon = BrandIcons.Back,
                contentDescription = stringResource(R.string.navigate_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        } else {
            BrandLogo(Modifier.align(Alignment.CenterStart))
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp),
            )
        }
        SmartPill(Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = BrandColors.Cielo) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.145.em,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, modifier = modifier)
}

/** White card with the identity's hairline border and soft shadow. */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    radius: Dp = BrandRadius.Medium,
    border: BorderStroke = BorderStroke(1.dp, BrandColors.Line),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val base = modifier
        .shadow(6.dp, shape, ambientColor = BrandColors.CieloDark.copy(alpha = 0.08f), spotColor = BrandColors.CieloDark.copy(alpha = 0.08f))
        .alpha(if (enabled) 1f else 0.55f)
    if (onClick != null) {
        Surface(onClick = onClick, enabled = enabled, modifier = base, shape = shape, color = BrandColors.Surface, border = border) {
            Column(content = content)
        }
    } else {
        Surface(modifier = base, shape = shape, color = BrandColors.Surface, border = border) {
            Column(content = content)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(BrandRadius.Small)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandColors.Cielo,
            contentColor = BrandColors.Surface,
            disabledContainerColor = BrandColors.Cielo.copy(alpha = if (loading) 0.85f else 0.4f),
            disabledContentColor = BrandColors.Surface,
        ),
        contentPadding = PaddingValues(horizontal = 22.dp),
        modifier = modifier
            .height(54.dp)
            .shadow(if (enabled) 10.dp else 0.dp, shape, spotColor = BrandColors.Cielo.copy(alpha = 0.35f)),
    ) {
        ButtonContent(text, loading, leadingIcon, trailingIcon)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(BrandRadius.Small),
        border = BorderStroke(1.dp, BrandColors.SecondaryBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandColors.Cielo),
        contentPadding = PaddingValues(horizontal = 22.dp),
        modifier = modifier.height(54.dp),
    ) {
        ButtonContent(text, loading = false, leadingIcon = leadingIcon, trailingIcon = null)
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, loading: Boolean, leadingIcon: ImageVector?, trailingIcon: ImageVector?) {
    if (loading) {
        CircularProgressIndicator(Modifier.size(18.dp), color = BrandColors.Surface, strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
    } else if (leadingIcon != null) {
        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    if (trailingIcon != null && !loading) {
        Spacer(Modifier.width(8.dp))
        Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/** Pill stepper: − value + */
@Composable
fun QuantityStepper(
    value: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
) {
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(CircleShape)
            .background(BrandColors.Canvas)
            .border(1.dp, BrandColors.Line, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(BrandIcons.Minus, decreaseDescription, canDecrease, onDecrease)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 25.dp),
        )
        StepperButton(BrandIcons.Plus, increaseDescription, canIncrease, onIncrease)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) BrandColors.Cielo else BrandColors.Line,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Icon + text note, e.g. "Compra segura processada pela Cielo". */
@Composable
fun TrustNote(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BrandIcons.Shield, contentDescription = null, tint = BrandColors.Success, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = BrandColors.Muted)
    }
}

enum class InfoTone(val background: Color, val content: Color, val body: Color) {
    Info(BrandColors.Sky, BrandColors.Cielo, BrandColors.InfoText),
    Warning(BrandColors.WarningSoft, BrandColors.Warning, BrandColors.Warning),
}

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: InfoTone = InfoTone.Info,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tone.background, RoundedCornerShape(BrandRadius.Small))
            .padding(15.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = tone.content, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(body, color = tone.body, fontSize = 11.sp, lineHeight = 15.sp)
        }
        action?.invoke()
    }
}

/** Key/value rows inside a card; the emphasized last row sits under a dashed rule. */
@Composable
fun RowsCard(rows: List<Pair<String, String>>, total: Pair<String, String>? = null) {
    BrandCard {
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(label, fontSize = 12.sp, color = BrandColors.Muted)
                Text(
                    value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }
        if (total != null) {
            DashedRule()
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(total.first, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    total.second,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun DashedRule(modifier: Modifier = Modifier, color: Color = BrandColors.Line) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
    }
}

@Composable
fun BrandDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = BrandColors.Line)
}

/** Small rounded label (status, category, badge). */
@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier, dot: Boolean = false) {
    Row(
        modifier = modifier
            .background(container, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(Modifier.size(6.dp).background(content, CircleShape))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = content, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.05.em)
    }
}

enum class BrandTab { Home, Tickets }

/** Bottom navigation with the two top-level destinations: Início and Ingressos. */
@Composable
fun BrandBottomBar(selected: BrandTab, onHome: () -> Unit, onTickets: () -> Unit) {
    Surface(color = BrandColors.Surface) {
        Column {
            BrandDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(71.dp),
            ) {
                NavItem(BrandIcons.Home, stringResource(R.string.nav_home), selected == BrandTab.Home, onHome)
                NavItem(BrandIcons.Ticket, stringResource(R.string.nav_tickets), selected == BrandTab.Tickets, onTickets)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) BrandColors.Cielo else BrandColors.NavInactive
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Fixed bottom area with a top hairline, used for the main action of a screen. */
@Composable
fun BottomActionBar(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = BrandColors.Surface) {
        Column {
            BrandDivider()
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                content = content,
            )
        }
    }
}

/** Day over month, as on the identity's event cards. */
@Composable
fun DateBlock(day: String, month: String, modifier: Modifier = Modifier) {
    Column(modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(day, color = BrandColors.Cielo, fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            month,
            color = BrandColors.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
