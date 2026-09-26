package com.vitalis.core.designsystem.component

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisType

/** White card on the paper ground. */
@Composable
fun VCard(
    modifier: Modifier = Modifier,
    color: Color = VitalisColors.Card,
    radius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    spacing: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(color)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Dark hero card — one per screen, holding that screen's dominant metric. */
@Composable
fun InkCard(modifier: Modifier = Modifier, spacing: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) =
    VCard(modifier, color = VitalisColors.Night, radius = 28.dp, padding = PaddingValues(20.dp), spacing = spacing, content = content)

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = VitalisType.Title, color = VitalisColors.Ink)
        if (trailing != null) {
            Text(
                trailing,
                style = VitalisType.Small,
                color = VitalisColors.InkMuted,
                modifier = if (onTrailing != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onTrailing).padding(4.dp) else Modifier,
            )
        }
    }
}

/** Label over value. */
@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, onDark: Boolean = false, valueStyle: TextStyle = VitalisType.Value, valueColor: Color? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = VitalisType.Caption, color = if (onDark) VitalisColors.OnNightMuted else VitalisColors.InkMuted)
        Text(value, style = valueStyle, color = valueColor ?: if (onDark) Color.White else VitalisColors.Ink)
    }
}

@Composable
fun TrackBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = VitalisColors.Sunken, height: Dp = 6.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(CircleShape).background(color))
    }
}

data class RingSpec(val fraction: Float, val color: Color)

/** Concentric progress rings (spec §9.7): kalori · aktif · langkah. */
@Composable
fun RingStack(rings: List<RingSpec>, description: String, modifier: Modifier = Modifier, size: Dp = 132.dp, stroke: Dp = 10.dp, track: Color = VitalisColors.NightTrack) {
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val w = stroke.toPx()
        rings.forEachIndexed { i, ring ->
            val inset = w / 2 + i * (w + 3.dp.toPx())
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
            drawArc(ring.color, -90f, 360f * ring.fraction.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
    }
}

enum class ButtonKind { Primary, Lime, Outline, Ghost }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
    textColor: Color? = null,
) {
    val (bg, fg) = when (kind) {
        ButtonKind.Primary -> VitalisColors.Ink to Color.White
        ButtonKind.Lime -> VitalisColors.Lime to VitalisColors.Ink
        ButtonKind.Outline -> VitalisColors.Card to VitalisColors.Ink
        ButtonKind.Ghost -> Color.Transparent to VitalisColors.Ink
    }
    Row(
        modifier = modifier
            .heightIn(min = height)
            .clip(CircleShape)
            .background(bg)
            .then(if (kind == ButtonKind.Outline) Modifier.border(1.dp, VitalisColors.BorderControl, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = if (kind == ButtonKind.Primary) VitalisColors.Lime else fg)
        Text(text, style = VitalisType.BodyStrong, color = textColor ?: fg)
    }
}

/** 44dp round icon button; [description] is required because there is no visible label. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = VitalisColors.Card,
    tint: Color = VitalisColors.Ink,
    border: BorderStroke? = BorderStroke(1.dp, VitalisColors.Border),
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Text(
        text,
        style = VitalisType.Small,
        color = if (selected) Color.White else VitalisColors.Ink,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) VitalisColors.Ink else Color.Transparent)
            .then(if (selected) Modifier else Modifier.border(1.dp, VitalisColors.BorderControl, CircleShape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Labelled input on a white pill; [error] replaces the hint and outlines the field. */
@Composable
fun VTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String = "",
    error: String? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = VitalisType.Small, color = VitalisColors.InkMuted)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(VitalisColors.Card)
                .border(if (error != null) 1.5.dp else 1.dp, if (error != null) VitalisColors.Danger else VitalisColors.Border, shape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = VitalisType.BodyStrong.copy(color = VitalisColors.Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(VitalisColors.Ink),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f).semantics { contentDescription = label },
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = VitalisType.Body, color = VitalisColors.InkFaint)
                    inner()
                },
            )
            if (suffix != null) Text(suffix, style = VitalisType.Body, color = VitalisColors.InkMuted)
        }
        if (error != null) Text(error, style = VitalisType.Caption, color = VitalisColors.Danger)
    }
}

/** Selectable option card: ink when selected, white otherwise. */
@Composable
fun OptionCard(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) VitalisColors.Night else VitalisColors.Card)
            .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, style = VitalisType.BodyStrong, color = if (selected) Color.White else VitalisColors.Ink)
        if (subtitle != null) Text(subtitle, style = VitalisType.Small, color = if (selected) VitalisColors.OnNightMuted else VitalisColors.InkMuted)
    }
}

/** Soft rounded tag, e.g. XP breakdown or source labels. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, background: Color = VitalisColors.Sunken, color: Color = VitalisColors.InkMuted, strong: Boolean = false) {
    Text(
        text,
        style = if (strong) VitalisType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else VitalisType.Caption,
        color = color,
        modifier = modifier.clip(CircleShape).background(background).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
