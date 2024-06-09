package dev.datlag.burningseries.model

import dev.datlag.burningseries.model.algorithm.JaroWinkler
import dev.datlag.burningseries.model.common.moreThan
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmOverloads

@Serializable
abstract class SeriesData {
    abstract val href: String
    abstract val title: String
    abstract val coverHref: String?

    private val defaultValues by lazy {
        createDefaultValues()
    }

    val source: String by lazy {
        defaultValues.first
    }
    open val season: Int? by lazy {
        defaultValues.second
    }
    open val language: String? by lazy {
        defaultValues.third
    }

    val allTitles by lazy {
        val pipeSplit = title.split('|').filterNot {
            it.isBlank()
        }.map {
            it.trim()
        }.toImmutableList()

        if (pipeSplit.size == 1) {
            if (title.moreThan(':', 1)) {
                pipeSplit
            } else {
                title.split(':').filterNot { it.isBlank() }.map { it.trim() }.toImmutableList()
            }
        } else {
            pipeSplit
        }
    }

    val bestTitle by lazy {
        when {
            allTitles.size <= 1 -> allTitles.firstOrNull() ?: title
            else -> {
                val newTitles = mutableListOf<String>()
                allTitles.forEach { str ->
                    val strFlatten = str.replace("\\s".toRegex(RegexOption.MULTILINE), String()).trim()

                    if (newTitles.none {
                            JaroWinkler.distance(str, it) > 0.95 || run {
                                val itFlatten = it.replace("\\s".toRegex(RegexOption.MULTILINE), String()).trim()

                                JaroWinkler.distance(strFlatten, itFlatten) > 0.95
                            }
                        }) {
                        newTitles.add(str)
                    }
                }
                newTitles.toSet().joinToString(separator = " | ")
            }
        }
    }

    open val mainTitle by lazy {
        bestTitle.substringBefore('|').trim()
    }

    open val subTitle by lazy {
        bestTitle.substringAfter('|', missingDelimiterValue = "").trim().ifBlank { null }
    }

    val hasSubtitle by lazy {
        !subTitle.isNullOrBlank()
    }

    private fun getHrefTitle(): String {
        val newHref = if (href.startsWith("series/")) {
            href.substringAfter("series/")
        } else if (href.startsWith("serie/")) {
            href.substringAfter("serie/")
        } else if (href.startsWith("/series/")) {
            href.substringAfter("/series/")
        } else if (href.startsWith("/serie/")) {
            href.substringAfter("/serie/")
        } else {
            href
        }
        val potentialTitle = if (newHref.startsWith('/')) {
            newHref.substringAfter('/')
        } else {
            newHref
        }
        val potTitle = potentialTitle.substringBefore('/').trim()
        return if (potTitle.equals("serie", true) || potTitle.equals("series", true)) {
            potentialTitle.substringAfter('/').substringBefore('/').trim()
        } else {
            potTitle
        }
    }

    private fun createDefaultValues(): Triple<String, Int?, String?> {
        var newHref = BSUtil.normalizeHref(href)
        if (newHref.startsWith('/')) {
            newHref = newHref.substring(1)
        }
        if (newHref.startsWith("serie/", true) || newHref.startsWith("series/", true)) {
            newHref = newHref.substringAfter('/')
        }
        val hrefSplit = newHref.split('/')
        val season = if (hrefSplit.size >= 2) hrefSplit[1] else null
        val language = if (hrefSplit.size >= 3) hrefSplit[2] else null
        val fallbackLanguage = if (hrefSplit.size >= 4) hrefSplit[3] else null
        var title = hrefSplit[0].ifBlank {
            getHrefTitle()
        }
        if (title.equals(season, true)) {
            title = getHrefTitle()
        }

        return Triple(
            first = title,
            second = season?.ifBlank { null }?.toIntOrNull(),
            third = if (!fallbackLanguage.isNullOrBlank()) {
                fallbackLanguage
            } else {
                if (language.isNullOrBlank()) null else language
            }
        )
    }

    @JvmOverloads
    fun toHref(newSeason: Int? = season, newLanguage: String? = language): String {
        return if (newSeason != null && newLanguage != null) {
            "serie/${source}/${newSeason}/${newLanguage}"
        } else if (newSeason != null) {
            "serie/${source}/${newSeason}"
        } else if (newLanguage != null) {
            "serie/${source}/${newLanguage}"
        } else {
            "serie/$source"
        }
    }

    companion object {
        fun fromHref(href: String) = object : SeriesData() {
            override val href: String = href
            override val title: String = ""
            override val coverHref: String? = null
        }

        fun commonHref(href: String) = object : SeriesData() {
            override val href: String = href
            override val title: String = ""
            override val coverHref: String? = null

            override val season: Int? = null
            override val language: String? = null
        }
    }
}