package com.example.hrnext.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** Scales content down slightly while pressed, for a tactile "press" feel on tappable cards. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.scale(scale)
}

/** Counts up from 0 to [target] whenever it changes — used for stat numbers so data landing reads
 * as a lively tally instead of a value just popping into place. */
@Composable
fun AnimatedCountText(
    target: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(durationMillis = 700, easing = FastOutSlowInEasing)) }
    Text(anim.value.toInt().toString(), style = style, color = color, fontWeight = fontWeight, modifier = modifier)
}

/** Same idea as [AnimatedCountText] but for fractional totals (e.g. leave days remaining). */
@Composable
fun AnimatedDecimalText(
    target: Double,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    format: String = "%.1f",
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(durationMillis = 700, easing = FastOutSlowInEasing)) }
    Text(format.format(anim.value), style = style, color = color, fontWeight = fontWeight, modifier = modifier)
}
