package dev.datlag.burningseries.ui.navigation.screen.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.datlag.burningseries.common.merge
import dev.datlag.burningseries.common.rememberIsTv
import dev.datlag.burningseries.composeapp.generated.resources.Res
import dev.datlag.burningseries.composeapp.generated.resources.search
import dev.datlag.burningseries.network.state.SearchState
import dev.datlag.burningseries.other.AniFlow
import dev.datlag.burningseries.ui.navigation.screen.home.HomeComponent
import dev.datlag.tooling.Platform
import dev.datlag.tooling.compose.onClick
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import dev.datlag.burningseries.MokoRes
import dev.datlag.burningseries.composeapp.generated.resources.error
import dev.datlag.burningseries.composeapp.generated.resources.loading
import dev.datlag.burningseries.other.isInstalled
import dev.datlag.tooling.compose.platform.PlatformFilterChip
import dev.datlag.tooling.compose.platform.PlatformIcon
import dev.datlag.tooling.compose.platform.PlatformSelectableChipBorder
import dev.datlag.tooling.compose.platform.PlatformText
import kotlinx.collections.immutable.toImmutableSet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
internal fun HomeSearchBar(component: HomeComponent) {
    var query by remember { mutableStateOf("") }
    val searchState by component.search.collectAsStateWithLifecycle()
    val windowInsets = SearchBarDefaults.windowInsets.asPaddingValues().merge(16.dp)
    var isActive by remember(searchState) { mutableStateOf(searchState.hasQueryItems) }
    val isDesktopOrTv = Platform.isDesktop || Platform.rememberIsTv()
    val filterSearch = remember { mutableStateListOf<String>() }

    DockedSearchBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(windowInsets),
        query = query,
        onQueryChange = {
            query = it
            component.search(query, filterSearch)
        },
        active = isActive,
        enabled = searchState.isSuccess,
        onActiveChange = {
            if (it) {
                if (searchState.isError) {
                    component.retryLoadingSearch()
                }
            }
            isActive = it
        },
        onSearch = {
            query = it
            component.search(it, filterSearch)
        },
        leadingIcon = {
            if (isActive) {
                IconButton(
                    onClick = {
                        isActive = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            } else {
                AniFlowIconButton(
                    onClick = {
                        component.search(query, filterSearch)
                    }
                )
            }
        },
        placeholder = {
            val res = when {
                searchState.isLoading -> Res.string.loading
                searchState.isSuccess -> Res.string.search
                else -> Res.string.error
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(res),
                textAlign = TextAlign.Center
            )
        },
        trailingIcon = {
            if (isActive) {
                IconButton(
                    onClick = {
                        if (query.isEmpty()) {
                            isActive = false
                        } else {
                            query = ""
                            component.search(null, filterSearch)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = null
                    )
                }
            } else {
                val color = LocalContentColor.current.copy(alpha = 1F)

                if (isDesktopOrTv) {
                    IconButton(
                        onClick = {
                            component.showQrCode()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode,
                            contentDescription = null,
                            tint = color
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            component.settings()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = color
                        )
                    }
                }
            }
        },
        content = {
            val language by component.language.collectAsStateWithLifecycle(null)

            (searchState as? SearchState.Success)?.let { current ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    item {
                        LazyRow(
                            modifier = Modifier.fillParentMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        ) {
                            items(current.queriedGenres.toList()) {
                                var isSelected by remember { mutableStateOf(filterSearch.contains(it)) }

                                PlatformFilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        isSelected = !isSelected

                                        if (isSelected) {
                                            filterSearch.add(it)
                                        } else {
                                            filterSearch.remove(it)
                                        }

                                        component.search(query, filterSearch)
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            PlatformIcon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null
                                            )
                                        }
                                    } else null,
                                    content = {
                                        PlatformText(text = it)
                                    }
                                )
                            }
                        }
                    }
                    items(current.queriedItems.toImmutableList(), key = { it.href }) {
                        SearchResult(
                            item = it,
                            filterGenres = filterSearch.toImmutableSet(),
                            modifier = Modifier.fillParentMaxWidth(),
                            onClick = { data ->
                                component.details(data, language)
                            }
                        )
                    }
                }
            }
        }
    )
}
