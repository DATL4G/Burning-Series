package dev.datlag.burningseries.ui.screen.initial.series.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.vanniktech.blurhash.BlurHash
import dev.datlag.burningseries.common.*
import dev.datlag.burningseries.database.Episode
import dev.datlag.burningseries.model.Series
import dev.datlag.burningseries.ui.theme.TopLeftBottomRightRoundedShape
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun EpisodeItem(content: Series.Episode, dbEpisode: Episode?, isLoading: Boolean, onClick: () -> Unit) {
    val blurHash = remember(content.href) { BlurHash.random() }
    val enabled = content.hosters.isNotEmpty()
    val isFinished = remember(dbEpisode) {
        val length = dbEpisode?.length ?: 0L
        val progress = dbEpisode?.progress ?: 0L

        if (length != 0L || progress != 0L) {
            Napier.e("Length: $length")
            Napier.e("Progress: $progress")
        }

        if (length > 0L && progress > 0L) {
            (progress.toDouble() / length.toDouble() * 100.0).toFloat() >= 85F
        } else {
            false
        }
    }

    Row(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().height(100.dp).onClick(enabled) {
            onClick()
        }.ifTrue(enabled) { bounceClick(0.95F) }.ifFalse(enabled) { alpha(0.5F) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1.75F, true)
                .clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            val imageBitmap: ImageBitmap? by produceState<ImageBitmap?>(null) {
                value = withIOContext {
                    BlurHash.decode(hash = blurHash, width = 175.dp.value.roundToInt(), height = 100.dp.value.roundToInt())
                }
            }
            imageBitmap?.let {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = it,
                    contentDescription = content.title
                )
            } ?: Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5F)))
            if (isFinished) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = content.title,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = content.title,
                    tint = Color.White
                )
            }
            Text(
                modifier = Modifier.clip(TopLeftBottomRightRoundedShape(
                    baseShape = MaterialTheme.shapes.medium,
                    otherSideRounding = 0.dp
                )).background(Color.White).align(Alignment.BottomEnd).padding(4.dp),
                text = content.episodeNumber,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (isLoading) {
                CircularProgressIndicator()
            }
        }
        Text(
            text = content.episodeTitle,
            maxLines = 3
        )
    }
}