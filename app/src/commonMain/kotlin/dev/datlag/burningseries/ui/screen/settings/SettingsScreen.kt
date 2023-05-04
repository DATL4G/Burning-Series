package dev.datlag.burningseries.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.datlag.burningseries.LocalDarkMode
import dev.datlag.burningseries.LocalStringRes
import dev.datlag.burningseries.common.*
import dev.datlag.burningseries.other.Constants
import dev.datlag.burningseries.other.StateSaver
import dev.datlag.burningseries.other.isAndroid
import dev.datlag.burningseries.ui.custom.InfoCard
import dev.datlag.burningseries.ui.custom.dragdrop.DragDropColumn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import dev.datlag.burningseries.ui.custom.DropdownMenu
import dev.datlag.burningseries.ui.custom.DropdownMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val hosterList by component.hosterList.collectAsStateSafe { component.hosterList.getValueBlocking(emptyList()) }
    val newRelease by component.newRelease.collectAsStateSafe { null }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        component.onGoBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = LocalStringRes.current.back,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                },
                title = {
                    Text(
                        text = LocalStringRes.current.settings,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                },
                backgroundColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                elevation = 0.dp
            )
        }
    ) {
        val state = rememberLazyListState(
            StateSaver.settingsViewPos,
            StateSaver.settingsViewOffset
        )

        DragDropColumn(
            state = state,
            items = hosterList,
            onSwap = { old, new ->
                component.swapHoster(old, new)
            },
            itemContent = {index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            modifier = Modifier.padding(start = 16.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            modifier = Modifier.weight(1F).padding(16.dp),
                            text = item.name,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = true
                        )
                        if (index > 0) {
                            IconButton(onClick = {
                                component.swapHoster(index, index - 1)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropUp,
                                    contentDescription = LocalStringRes.current.moveUp
                                )
                            }
                        }
                        if (index < hosterList.size - 1) {
                            IconButton(onClick = {
                                component.swapHoster(index, index + 1)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = LocalStringRes.current.moveDown
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
            },
            itemsBefore = {
                item {
                    val strings = LocalStringRes.current
                    var cardMinWidth by remember { mutableStateOf(0) }

                    if (newRelease != null) {
                        InfoCard(
                            title = newRelease!!.title,
                            text = strings.newRelease,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            icon = Icons.Default.NewReleases,
                            modifier = Modifier
                                .fillWidthInPortraitMode()
                                .defaultMinSize(minWidth = Dp(cardMinWidth.toFloat()))
                                .padding(vertical = 16.dp)
                                .onClick {
                                    strings.openInBrowser(newRelease!!.htmlUrl.ifEmpty {
                                        Constants.GITHUB_REPOSITORY_URL
                                    })
                                }.onSizeChanged {
                                    if (it.width > cardMinWidth) {
                                        cardMinWidth = it.width
                                    }
                                }
                        )
                    }

                    InfoCard(
                        title = LocalStringRes.current.hosterOrder,
                        text = LocalStringRes.current.hosterOrderText,
                        backgroundColor = Color.Warning,
                        contentColor = Color.OnWarning,
                        icon = Icons.Default.FormatListNumbered,
                        modifier = Modifier
                            .fillWidthInPortraitMode()
                            .defaultMinSize(minWidth = Dp(cardMinWidth.toFloat()))
                            .onSizeChanged {
                            if (it.width > cardMinWidth) {
                                cardMinWidth = it.width
                            }
                        }
                    )
                    if (hosterList.isEmpty()) {
                        InfoCard(
                            title = LocalStringRes.current.noHoster,
                            text = LocalStringRes.current.noHosterText,
                            backgroundColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            icon = Icons.Default.Report,
                            modifier = Modifier
                                .fillWidthInPortraitMode()
                                .defaultMinSize(minWidth = Dp(cardMinWidth.toFloat()))
                                .padding(vertical = 16.dp)
                                .onSizeChanged {
                                    if (it.width > cardMinWidth) {
                                        cardMinWidth = it.width
                                    }
                                }
                        )
                    } else {
                        Text(
                            text = LocalStringRes.current.mostPreferred.format("1"),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        Text(
                            text = LocalStringRes.current.leastPreferred.format(max(hosterList.size, 2).toString()),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            },
            itemsAfter = {
                item {
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = LocalStringRes.current.appearance,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    val themeMode by component.themeMode.collectAsStateSafe {
                        component.themeMode.getValueBlocking(0)
                    }
                    var showMenu by remember { mutableStateOf(false) }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalStringRes.current.theming
                        )
                        Spacer(modifier = Modifier.weight(1F))
                        Button(onClick = {
                            showMenu = !showMenu
                        }) {
                            Text(
                                text = when (themeMode) {
                                    1 -> LocalStringRes.current.lightTheme
                                    2 -> LocalStringRes.current.darkTheme
                                    else -> LocalStringRes.current.followSystem
                                }
                            )
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = LocalStringRes.current.more
                            )

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = { component.changeThemeMode(1) },
                                    enabled = true,
                                    text = {
                                        Text(
                                            text = LocalStringRes.current.lightTheme,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.LightMode,
                                            contentDescription = LocalStringRes.current.lightTheme,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    onClick = { component.changeThemeMode(2) },
                                    enabled = true,
                                    text = {
                                        Text(
                                            text = LocalStringRes.current.darkTheme,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.DarkMode,
                                            contentDescription = LocalStringRes.current.darkTheme,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    onClick = { component.changeThemeMode(0) },
                                    enabled = true,
                                    text = {
                                        Text(
                                            text = LocalStringRes.current.followSystem,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isAndroid) Icons.Default.PhonelinkSetup else Icons.Default.LaptopWindows,
                                            contentDescription = LocalStringRes.current.followSystem,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    val amoled by component.amoled.collectAsStateSafe {
                        component.amoled.getValueBlocking(false)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalStringRes.current.amoledMode
                        )
                        Spacer(modifier = Modifier.weight(1F))
                        Switch(
                            checked = amoled,
                            onCheckedChange = {
                                component.changeAmoledState(it)
                            },
                            enabled = LocalDarkMode.current
                        )
                    }
                }
                item {
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = LocalStringRes.current.logging,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    val errorContent = component.errorFile.content()?.ifBlank { null }
                    val strings = LocalStringRes.current

                    if (!errorContent.isNullOrEmpty()) {
                        InfoCard(
                            title = strings.error,
                            text = strings.errorText,
                            backgroundColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            icon = Icons.Default.Report,
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(vertical = 8.dp)
                                .onClick {
                                    strings.copyToClipboard(errorContent)
                                }
                        )
                    }
                }
                item {
                    val loggingContent = component.loggingFile.content()?.ifBlank { null }
                    val strings = LocalStringRes.current

                    if (!loggingContent.isNullOrEmpty()) {
                        val (color, onColor) = when {
                            loggingContent.contains("ERROR", true) -> {
                                MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
                            }
                            loggingContent.contains("WARNING", true) -> {
                                Color.Warning to Color.OnWarning
                            }
                            else -> {
                                MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        }

                        InfoCard(
                            title = strings.logging,
                            text = strings.loggingText,
                            backgroundColor = color,
                            contentColor = onColor,
                            icon = Icons.Default.Report,
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(vertical = 8.dp)
                                .onClick {
                                    strings.copyToClipboard(loggingContent)
                                }
                        )
                    }
                }
                item {
                    val modes = mapOf(
                        0 to LocalStringRes.current.none,
                        1 to LocalStringRes.current.home,
                        2 to LocalStringRes.current.search,
                        3 to LocalStringRes.current.series,
                        4 to LocalStringRes.current.streams
                    )
                    val selected by component.loggingMode.collectAsStateSafe {
                        component.loggingMode.getValueBlocking(0)
                    }

                    Column(modifier = Modifier.fillParentMaxWidth()) {
                        modes.forEach { (t, u) ->
                            Row(
                                modifier = Modifier.fillParentMaxWidth().selectable(
                                    selected = t == selected,
                                    onClick = {
                                        component.changeLoggingMode(t)
                                    }
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = t == selected,
                                    onClick = {
                                        component.changeLoggingMode(t)
                                    }
                                )
                                Text(
                                    text = u,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = LocalStringRes.current.copyright.format(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        )

        DisposableEffect(state) {
            onDispose {
                StateSaver.settingsViewPos = state.firstVisibleItemIndex
                StateSaver.settingsViewOffset = state.firstVisibleItemScrollOffset
            }
        }
    }
}