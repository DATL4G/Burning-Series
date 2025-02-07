package dev.datlag.burningseries.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.datlag.burningseries.other.PictureInPicture
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle

interface DialogComponent : Component {
    fun dismiss()

    @Composable
    override fun onRender(content: @Composable (Boolean) -> Unit) {
        val isPip by PictureInPicture.active.collectAsStateWithLifecycle()

        if (!isPip || handlesPIP) {
            super.onRender(content)
        }
    }
}