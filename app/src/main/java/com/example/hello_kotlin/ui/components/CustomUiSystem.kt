package com.example.hello_kotlin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hello_kotlin.ui.theme.DarkSurface
import com.example.hello_kotlin.ui.theme.AccentPrimary
import kotlinx.coroutines.launch

import android.graphics.Typeface

val CondensedFontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
val MonoFontFamily = FontFamily(Typeface.create("monospace", Typeface.NORMAL))

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AccentPrimary.copy(alpha = 0.25f),
    shadowColor: Color = Color(0xFF060608),
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.5.dp,
    shadowOffset: Dp = 6.dp,
    backgroundColor: Color = DarkSurface,
    content: @Composable () -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Box(modifier = modifier.padding(bottom = shadowOffset, end = shadowOffset)) {
        // Shadow Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, shape)
        )
        // Main Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape)
                .border(borderWidth, borderColor, shape)
        ) {
            content()
        }
    }
}

@Composable
fun StaggeredEntrance(
    index: Int,
    content: @Composable () -> Unit
) {
    val animatableOffset = remember { Animatable(60f) }
    val animatableAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val delayTime = minOf(index, 6) * 45L
        kotlinx.coroutines.delay(delayTime)
        launch {
            animatableOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 250f
                )
            )
        }
        launch {
            animatableAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(200)
            )
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = animatableOffset.value
                alpha = animatableAlpha.value
            }
    ) {
        content()
    }
}
