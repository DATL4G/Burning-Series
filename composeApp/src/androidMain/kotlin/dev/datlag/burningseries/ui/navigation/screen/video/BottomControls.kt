package dev.datlag.burningseries.ui.navigation.screen.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import dev.datlag.burningseries.common.toDuration
import dev.datlag.tooling.compose.platform.PlatformText
import dev.datlag.tooling.compose.platform.ProvideNonTvContentColor
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToLong

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControls(
    isVisible: Boolean,
    playerWrapper: PlayerWrapper,
    modifier: Modifier = Modifier,
) {
    val isFinished by playerWrapper.isFinished.collectAsStateWithLifecycle()

    AnimatedVisibility(
        modifier = modifier.safeDrawingPadding(),
        visible = isVisible || isFinished,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut()
    ) {
        BottomAppBar(
            modifier = modifier,
            containerColor = Color.Black.copy(alpha = 0.5F),
            contentColor = Color.White
        ) {
            ProvideNonTvContentColor {
                val source = remember { MutableInteractionSource() }
                val isDragging by source.collectIsDraggedAsState()
                val position by playerWrapper.progress.collectAsStateWithLifecycle()
                val duration by playerWrapper.length.collectAsStateWithLifecycle()
                var progress by remember { mutableFloatStateOf(0F) }
                val progressForText by remember(progress, duration) {
                    derivedStateOf {
                        duration.times(progress).roundToLong()
                    }
                }

                LaunchedEffect(position, duration) {
                    if (!isDragging) {
                        progress = if (position <= 0L || duration <= 0L) {
                            0F
                        } else {
                            position.toFloat() / duration.toFloat()
                        }
                    }
                }

                PlatformText(
                    text = progressForText.toDuration(),
                    maxLines = 1
                )
                Slider(
                    modifier = Modifier.weight(1F),
                    value = progress,
                    onValueChange = {
                        playerWrapper.showControls()

                        progress = it
                    },
                    onValueChangeFinished = {
                        playerWrapper.seekTo(duration.times(progress).roundToLong())
                    },
                    interactionSource = source
                )
                PlatformText(
                    text = duration.toDuration(),
                    maxLines = 1
                )
            }
        }
    }
}