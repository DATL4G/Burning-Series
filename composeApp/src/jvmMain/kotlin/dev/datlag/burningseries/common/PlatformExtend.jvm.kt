package dev.datlag.burningseries.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.vanniktech.blurhash.BlurHash
import dev.datlag.tooling.Platform
import dev.datlag.tooling.scopeCatching
import dev.datlag.tooling.systemEnv
import dev.datlag.tooling.systemProperty
import java.net.InetAddress

actual fun BlurHash.decode(
    hash: String?,
    width: Int,
    height: Int
): ImageBitmap? {
    if (hash.isNullOrBlank()) {
        return null
    }

    val image = decode(
        blurHash = hash,
        width = width,
        height = height
    )
    return image?.toComposeImageBitmap()
}

@Composable
actual fun Modifier.drawProgress(color: Color, progress: Float): Modifier = drawWithContent {
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)

        drawContent()

        drawRect(
            color = color,
            size = Size(size.width * progress, size.height),
            blendMode = BlendMode.SrcOut
        )

        restoreToCount(checkPoint)
    }
}

@Composable
actual fun Platform.rememberIsTv(): Boolean {
    return false
}

fun Platform.deviceName(): String {
    return systemEnv("COMPUTERNAME")?.ifBlank { null }
        ?: systemEnv("HOSTNAME")?.ifBlank { null }
        ?: systemProperty("COMPUTERNAME")?.ifBlank { null }
        ?: systemProperty("HOSTNAME")?.ifBlank { null }
        ?: scopeCatching {
            InetAddress.getLocalHost().hostName
        }.getOrNull()?.ifBlank { null }
        ?: scopeCatching {
            InetAddress.getLocalHost().canonicalHostName
        }.getOrNull()?.ifBlank { null }
        ?: "Desktop"
}