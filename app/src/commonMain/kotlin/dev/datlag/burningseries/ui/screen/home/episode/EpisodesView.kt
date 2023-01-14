package dev.datlag.burningseries.ui.screen.home.episode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.datlag.burningseries.LocalStringRes
import dev.datlag.burningseries.common.getValueBlocking
import dev.datlag.burningseries.common.header
import dev.datlag.burningseries.common.isTv
import dev.datlag.burningseries.ui.screen.home.gridCellSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesView(component: EpisodesComponent) {
    val episodes by component.episodes.collectAsState(component.episodes.getValueBlocking(emptyList()))
    val lastWatched by component.lastWatched.collectAsState(component.lastWatched.getValueBlocking(emptyList()))

    if (isTv()) {
        LazyRow(
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(lastWatched) {
                EpisodeItem(it, component)
            }
            item {
                if (lastWatched.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(32.dp))
                }
            }
            items(episodes) {
                EpisodeItem(it, component)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = gridCellSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (lastWatched.isNotEmpty()) {
                header {
                    Text(
                        text = LocalStringRes.current.lastWatched,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(lastWatched) {
                    EpisodeItem(it, component)
                }
            }
            header {
                Text(
                    text = LocalStringRes.current.latestEpisodes,
                    fontWeight = FontWeight.Bold
                )
            }
            items(episodes) {
                EpisodeItem(it, component)
            }
        }
    }
}
