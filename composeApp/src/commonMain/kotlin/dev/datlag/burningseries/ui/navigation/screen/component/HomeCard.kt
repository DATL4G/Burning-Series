package dev.datlag.burningseries.ui.navigation.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.datlag.burningseries.common.bottomShadowBrush
import dev.datlag.burningseries.common.onPrimary
import dev.datlag.burningseries.common.primary
import dev.datlag.burningseries.model.Home
import dev.datlag.burningseries.ui.theme.SchemeTheme
import dev.datlag.burningseries.ui.theme.rememberSchemeThemeDominantColorState
import io.github.aakira.napier.Napier

@Composable
fun HomeCard(
    series: Home.Series,
    modifier: Modifier = Modifier,
    onClick: (Home.Series) -> Unit
) {
    SchemeTheme(
        key = series.source
    ) { updater ->
        Card(
            modifier = modifier,
            onClick = {
                onClick(series)
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val colorState = rememberSchemeThemeDominantColorState(
                    key = series.source,
                    applyMinContrast = true,
                    minContrastBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                AsyncImage(
                    model = series.coverHref,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = series.title,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        updater?.update(state.painter)
                    }
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .bottomShadowBrush(colorState.primary)
                        .padding(16.dp)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = series.mainTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = if (series.hasSubtitle) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        color = colorState.onPrimary
                    )
                    series.subTitle?.let { sub ->
                        Text(
                            text = sub,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            color = colorState.onPrimary
                        )
                    }
                }
            }
        }
    }
}