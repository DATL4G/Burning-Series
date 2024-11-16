package dev.datlag.burningseries.ui.navigation.screen.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.datlag.burningseries.composeapp.generated.resources.Res
import dev.datlag.burningseries.composeapp.generated.resources.download
import dev.datlag.burningseries.composeapp.generated.resources.vlc_required_1
import dev.datlag.burningseries.composeapp.generated.resources.vlc_required_2
import dev.datlag.burningseries.other.Constants
import org.jetbrains.compose.resources.stringResource
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun VideoScreen(component: VideoComponent) {
    val foundVlc = NativeDiscovery().discover()

    if (foundVlc) {
        val mediaPlayer = VideoPlayer(component)

        Scaffold(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            topBar = {
                VideoInfo(component)
            },
            bottomBar = {
                VideoControls(mediaPlayer)
            }
        ) { padding ->
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                var playReady by remember { mutableStateOf(false) }

                SwingPanel(
                    background = Color.Black,
                    modifier = Modifier.fillMaxSize(),
                    factory = { mediaPlayer.component },
                    update = {
                        playReady = true
                    }
                )

                LaunchedEffect(playReady) {
                    if (playReady) {
                        mediaPlayer.startPlaying()
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(fraction = 0.7F),
                text = stringResource(Res.string.vlc_required_1),
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.fillMaxWidth(fraction = 0.7F),
                text = stringResource(Res.string.vlc_required_2),
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val uriHandler = LocalUriHandler.current

                FilledTonalIconButton(
                    onClick = {
                        component.back()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = null
                    )
                }
                Button(
                    onClick = {
                        uriHandler.openUri(Constants.VLC_WEBSITE)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(Res.string.download))
                }
            }
        }
    }
}