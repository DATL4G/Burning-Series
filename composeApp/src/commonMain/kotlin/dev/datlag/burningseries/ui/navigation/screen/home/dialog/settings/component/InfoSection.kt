package dev.datlag.burningseries.ui.navigation.screen.home.dialog.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.datlag.burningseries.composeapp.generated.resources.Res
import dev.datlag.burningseries.composeapp.generated.resources.close
import dev.icerock.moko.resources.compose.painterResource
import org.jetbrains.compose.resources.stringResource
import dev.datlag.burningseries.MokoRes
import dev.datlag.burningseries.composeapp.generated.resources.app_name
import dev.datlag.burningseries.github.model.UserAndRelease
import dev.datlag.burningseries.other.Constants
import dev.datlag.tooling.Platform
import dev.datlag.tooling.compose.platform.typography

@Composable
fun InfoSection(
    dismissVisible: Boolean,
    user: UserAndRelease.User?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            this@Column.AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterStart),
                visible = dismissVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = stringResource(Res.string.close)
                    )
                }
            }

            AsyncImage(
                modifier = Modifier.size(96.dp).clip(CircleShape),
                model = user?.avatar,
                contentDescription = null,
                error = painterResource(MokoRes.images.AppIcon),
                placeholder = painterResource(MokoRes.images.AppIcon),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )

            this@Column.AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterEnd),
                visible = user != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val uriHandler = LocalUriHandler.current

                IconButton(
                    onClick = {
                        uriHandler.openUri(Constants.GITHUB_REPO)
                    }
                ) {
                    Icon(
                        imageVector = if (user?.hasStarred == true) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = null
                    )
                }
            }
        }
        Text(
            text = user?.name ?: stringResource(Res.string.app_name),
            style = Platform.typography().headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}