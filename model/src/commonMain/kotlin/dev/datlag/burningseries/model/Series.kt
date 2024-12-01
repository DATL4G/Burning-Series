package dev.datlag.burningseries.model

import dev.datlag.burningseries.model.serializer.SerializableImmutableSet
import dev.datlag.tooling.getDigitsOrNull
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Series(
    @SerialName("title") override val title: String,
    @SerialName("description") val description: String,
    @SerialName("cover") override val coverHref: String? = null,
    @SerialName("href") override val href: String,
    @SerialName("seasonTitle") val seasonTitle: String,
    @SerialName("selectedLanguage") val selectedLanguage: String?,
    @SerialName("seasons") val seasons: SerializableImmutableSet<Season> = persistentSetOf(),
    @SerialName("languages") val languages: SerializableImmutableSet<Language> = persistentSetOf(),
    @SerialName("info") val info: SerializableImmutableSet<Info> = persistentSetOf(),
    @SerialName("episodes") val episodes: SerializableImmutableSet<Episode> = persistentSetOf()
) : SeriesData() {

    @Transient
    val genres = info.filter {
        it.header.equals("Genre", ignoreCase = true) || it.header.equals("Genres", ignoreCase = true)
    }.toImmutableSet()

    @Transient
    val firstGenre = genres.firstNotNullOfOrNull { it.data.ifBlank { null } }

    @Transient
    val isAnime: Boolean = genres.any {
        it.data.contains("Anime", ignoreCase = true)
    }

    @Transient
    val infoWithoutGenre = info.filterNot {
        it.header.equals("Genre", ignoreCase = true) || it.header.equals("Genres", ignoreCase = true)
    }.toImmutableSet()

    val currentSeason: Season? by lazy {
        seasons.firstOrNull {
            it.value == season
        } ?: seasons.firstOrNull {
            it.title.equals(seasonTitle, true)
        } ?: seasons.firstOrNull {
            it.title.equals(seasonTitle.toIntOrNull()?.toString(), true)
        } ?: seasons.firstOrNull {
            val titleInt = it.title.toIntOrNull()
            val seasonInt = season

            titleInt != null && seasonInt != null && titleInt == seasonInt
        } ?: seasons.firstOrNull {
            it.title.equals(seasonTitle.getDigitsOrNull(), true)
        } ?: seasons.firstOrNull {
            it.title.getDigitsOrNull().equals(seasonTitle, true)
        } ?: seasons.firstOrNull {
            it.title.getDigitsOrNull().equals(seasonTitle.getDigitsOrNull(), true)
        }
    }

    val currentLanguage: Language? by lazy {
        languages.firstOrNull {
            it.value.equals(language, true)
        } ?: languages.firstOrNull {
            it.value.equals(selectedLanguage, true)
        }
    }

    @Serializable
    data class Season(
        @SerialName("value") val value: Int,
        @SerialName("title") val title: String
    )

    @Serializable
    data class Language(
        @SerialName("value") val value: String,
        @SerialName("title") val title: String
    )

    @Serializable
    data class Info(
        @SerialName("header") val header: String = "",
        @SerialName("data") val data: String = "",
    )

    @Serializable
    data class Episode(
        @SerialName("number") val number: String,
        @SerialName("title") val fullTitle: String,
        @SerialName("href") override val href: String,
        @SerialName("hoster") val hoster: SerializableImmutableSet<Hoster>
    ) : SeriesData() {

        override val coverHref: String? = null

        @Transient
        val episodeNumber: String = BSUtil.episodeNumberRegex.find(fullTitle)?.groupValues?.lastOrNull() ?: number

        @Transient
        override val title: String = BSUtil.episodeNumberRegex.replaceFirst(fullTitle, "").trim().ifBlank { fullTitle }

        @Transient
        val convertedNumber: Int? = episodeNumber.toIntOrNull() ?: episodeNumber.getDigitsOrNull()?.toIntOrNull()

        @Serializable
        data class Hoster(
            @SerialName("title") val title: String,
            @SerialName("href") val href: String
        )
    }
}