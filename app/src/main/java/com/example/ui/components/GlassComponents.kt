package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmberTheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = EmberTheme.palette.glassSurface.copy(alpha = if (EmberTheme.palette.isOledBlack) 0.84f else 0.72f),
    borderColor: Color = EmberTheme.palette.glassBorderOuter,
    innerHighlightColor: Color = EmberTheme.palette.glassBorderInner.copy(alpha = if (EmberTheme.palette.isOledBlack) 0.55f else 0.96f),
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .border(width = 1.3.dp, color = borderColor, shape = shape)
            .padding(1.dp)
            .border(width = 1.dp, color = innerHighlightColor, shape = shape),
        shape = shape,
        color = containerColor,
        shadowElevation = if (EmberTheme.palette.isOledBlack) 0.dp else 3.dp,
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

data class SegmentedOption<T>(
    val value: T,
    val label: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val tag: String? = null,
    val barsCount: Int = 0,
)

@Composable
fun <T> SlidingGlassSegmentedControl(
    options: List<SegmentedOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = EmberTheme.palette.signalCrimson,
    activeTintColor: Color = EmberTheme.palette.glassSurface.copy(alpha = if (EmberTheme.palette.isOledBlack) 0.96f else 0.94f),
) {
    val palette = EmberTheme.palette
    val selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
    val trackShape = RoundedCornerShape(18.dp)
    val pillShape = RoundedCornerShape(14.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(palette.recessedSurface.copy(alpha = if (palette.isOledBlack) 0.92f else 0.68f))
            .border(1.2.dp, palette.glassBorderOuter, trackShape)
            .padding(4.dp),
    ) {
        val count = options.size.coerceAtLeast(1)
        val segmentWidth = maxWidth / count
        val animatedOffsetX by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "slidingPillOffset",
        )

        Surface(
            modifier = Modifier
                .offset(x = animatedOffsetX)
                .width(segmentWidth)
                .height(56.dp)
                .border(1.4.dp, activeColor.copy(alpha = 0.80f), pillShape)
                .padding(1.dp)
                .border(1.dp, palette.glassBorderInner.copy(alpha = if (palette.isOledBlack) 0.45f else 1f), pillShape),
            shape = pillShape,
            color = activeTintColor,
            shadowElevation = if (palette.isOledBlack) 0.dp else 2.dp,
        ) {}

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { idx, option ->
                val isSelected = idx == selectedIndex
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else palette.inkPrimary,
                    animationSpec = tween(180),
                    label = "segContentColor",
                )
                val subColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor.copy(alpha = 0.88f) else palette.inkSecondary,
                    animationSpec = tween(180),
                    label = "segSubColor",
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "segItemScale",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .then(if (option.tag != null) Modifier.testTag(option.tag) else Modifier)
                        .scale(scale)
                        .clip(pillShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = activeColor),
                            onClick = { onSelect(option.value) },
                        )
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (option.icon != null) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            } else if (option.barsCount > 0) {
                                StrengthBarsIndicator(
                                    bars = option.barsCount,
                                    active = isSelected,
                                    activeColor = activeColor,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (option.subtitle != null) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = subColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthBarsIndicator(
    bars: Int,
    active: Boolean,
    activeColor: Color,
) {
    val palette = EmberTheme.palette
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(13.dp),
    ) {
        for (i in 1..3) {
            val lit = i <= bars
            val barColor = when {
                lit && active -> activeColor
                lit -> palette.inkPrimary.copy(alpha = 0.68f)
                else -> palette.glassBorderOuter
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction = 0.35f + i * 0.21f)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
fun GlassChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    activeColor: Color = EmberTheme.palette.signalCrimson,
    activeTintColor: Color = EmberTheme.palette.signalCrimsonTint,
    horizontalPadding: Dp = 10.dp,
    tag: String? = null,
) {
    val palette = EmberTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chipScale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) activeTintColor.copy(alpha = 0.88f) else palette.glassSurface.copy(alpha = if (palette.isOledBlack) 0.82f else 0.72f),
        animationSpec = tween(durationMillis = 180),
        label = "chipBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeColor else palette.glassBorderOuter,
        animationSpec = tween(durationMillis = 180),
        label = "chipBorder",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            if (palette.isOledBlack) Color.White else activeColor
        } else {
            palette.inkPrimary
        },
        animationSpec = tween(durationMillis = 180),
        label = "chipContent",
    )

    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(bgColor)
            .border(
                border = BorderStroke(if (selected) 1.6.dp else 1.1.dp, borderColor),
                shape = shape,
            )
            .padding(1.dp)
            .border(
                border = BorderStroke(0.8.dp, palette.glassBorderInner.copy(alpha = if (selected) 0.45f else 0.75f)),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = activeColor),
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected && palette.isOledBlack) activeColor else contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) contentColor.copy(alpha = 0.85f) else palette.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = EmberTheme.palette.inkPrimary,
    containerColor: Color = EmberTheme.palette.glassSurface.copy(alpha = if (EmberTheme.palette.isOledBlack) 0.85f else 0.78f),
    borderColor: Color = EmberTheme.palette.glassBorderOuter,
    tag: String? = null,
) {
    val palette = EmberTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconBtnScale",
    )
    val shape = RoundedCornerShape(15.dp)

    Surface(
        modifier = modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .scale(scale)
            .size(48.dp)
            .border(1.2.dp, borderColor, shape)
            .padding(1.dp)
            .border(1.dp, palette.glassBorderInner.copy(alpha = if (palette.isOledBlack) 0.45f else 1f), shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = tint),
                onClick = onClick,
            ),
        shape = shape,
        color = containerColor,
        shadowElevation = if (palette.isOledBlack) 0.dp else 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
fun PrimaryGlassWarmButton(
    isHeating: Boolean,
    text: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = EmberTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "primaryScale",
    )

    val bgColor by animateColorAsState(
        targetValue = if (isHeating) palette.signalCrimson.copy(alpha = 0.94f) else palette.glassSurface.copy(alpha = if (palette.isOledBlack) 0.88f else 0.84f),
        animationSpec = tween(260),
        label = "primaryBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHeating) palette.signalCrimson else palette.iceCobalt,
        animationSpec = tween(260),
        label = "primaryBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (isHeating) Color.White else palette.inkPrimary,
        animationSpec = tween(260),
        label = "primaryText",
    )
    val subColor by animateColorAsState(
        targetValue = if (isHeating) Color.White.copy(alpha = 0.86f) else palette.inkSecondary,
        animationSpec = tween(260),
        label = "primarySub",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseRings")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulsePhase",
    )

    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("start_stop_button"),
        contentAlignment = Alignment.Center,
    ) {
        if (isHeating) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 2.dp),
            ) {
                val expand1 = pulsePhase * 12.dp.toPx()
                val alpha1 = (1f - pulsePhase) * 0.48f
                drawRoundRect(
                    color = palette.signalCrimson.copy(alpha = alpha1),
                    topLeft = Offset(-expand1 * 0.5f, -expand1 * 0.35f),
                    size = Size(size.width + expand1, size.height + expand1 * 0.7f),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx()),
                )

                val phase2 = (pulsePhase + 0.5f) % 1f
                val expand2 = phase2 * 12.dp.toPx()
                val alpha2 = (1f - phase2) * 0.30f
                drawRoundRect(
                    color = palette.signalCrimson.copy(alpha = alpha2),
                    topLeft = Offset(-expand2 * 0.5f, -expand2 * 0.35f),
                    size = Size(size.width + expand2, size.height + expand2 * 0.7f),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .defaultMinSize(minHeight = 66.dp)
                .border(
                    width = 1.8.dp,
                    color = borderColor,
                    shape = shape,
                )
                .padding(1.dp)
                .border(
                    width = 1.dp,
                    color = if (isHeating) Color.White.copy(alpha = 0.40f) else palette.glassBorderInner.copy(alpha = if (palette.isOledBlack) 0.45f else 1f),
                    shape = shape,
                )
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = if (isHeating) Color.White else palette.iceCobalt),
                    onClick = onClick,
                ),
            shape = shape,
            color = bgColor,
            shadowElevation = if (palette.isOledBlack) 0.dp else if (isHeating) 5.dp else 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isHeating) Color.White.copy(alpha = 0.22f) else palette.iceCobalt.copy(alpha = 0.14f))
                            .border(
                                1.2.dp,
                                if (isHeating) Color.White.copy(alpha = 0.45f) else palette.iceCobalt.copy(alpha = 0.40f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isHeating) Color.White else palette.iceCobalt,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isHeating) Color.White.copy(alpha = 0.22f) else palette.iceCobalt.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            if (isHeating) Color.White.copy(alpha = 0.42f) else palette.iceCobalt.copy(alpha = 0.32f),
                            RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (isHeating) "ACTIVE" else "IGNITE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isHeating) Color.White else palette.iceCobalt,
                    )
                }
            }
        }
    }
}
