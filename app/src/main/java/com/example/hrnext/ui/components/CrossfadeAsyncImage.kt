package com.example.hrnext.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/** [AsyncImage] that fades in once loaded instead of popping in abruptly. */
@Composable
fun CrossfadeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var loaded by remember(model) { mutableStateOf(false) }
    val alpha by animateFloatAsState(targetValue = if (loaded) 1f else 0f, animationSpec = tween(320), label = "imageFade")
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.alpha(alpha),
        onState = { state -> loaded = state is AsyncImagePainter.State.Success },
    )
}
