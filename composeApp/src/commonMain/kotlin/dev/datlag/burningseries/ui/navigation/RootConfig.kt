package dev.datlag.burningseries.ui.navigation

import dev.datlag.burningseries.model.SearchItem
import dev.datlag.burningseries.model.Series
import dev.datlag.burningseries.model.SeriesData
import dev.datlag.burningseries.model.serializer.SerializableImmutableList
import dev.datlag.burningseries.model.serializer.SerializableImmutableSet
import dev.datlag.burningseries.settings.model.Language
import dev.datlag.skeo.DirectLink
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.serialization.Serializable

@Serializable
sealed class RootConfig {

    @Serializable
    data object Welcome : RootConfig()

    @Serializable
    data class Home(val syncId: String?) : RootConfig()

    @Serializable
    data class Medium(
        val seriesData: SeriesData,
        val language: Language?,
        val isAnime: Boolean = if (seriesData is SearchItem) seriesData.isAnime else false
    ) : RootConfig()

    @Serializable
    data class Video(
        val series: Series,
        val episode: Series.Episode,
        val streams: SerializableImmutableSet<DirectLink>
    ) : RootConfig()

    @Serializable
    data class Activate(
        val series: Series,
        val episode: Series.Episode
    ) : RootConfig()
}