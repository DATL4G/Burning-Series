package dev.datlag.burningseries.scraper.bs

import dev.datlag.burningseries.model.*
import dev.datlag.burningseries.model.common.getDigitsOrNull
import dev.datlag.burningseries.scraper.Constants
import dev.datlag.burningseries.scraper.common.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import kotlin.time.Duration.Companion.seconds


object JvmBsScraper {

    var coverBlock: suspend (String?, Boolean) -> Pair<Cover, Boolean> = { _, _ ->
        Cover(String()) to false
    }

    private var client: HttpClient? = null
    private var logger: ActionLogger? = null

    fun client(client: HttpClient) = apply {
        if (this.client == null) {
            this.client = client
        }
    }

    fun logger(logger: ActionLogger) = apply {
        if (this.logger == null) {
            this.logger = logger
        }
    }

    suspend fun getDocument(url: String, mode: Int? = null): Document? = withTimeout(15.seconds) {
        val link = Constants.getBurningSeriesLink(url)
        info(mode, "Loading document from url: $link")
        return@withTimeout client?.let {
            suspendCatching {
                it.get(link).bodyAsText().trim().ifEmpty { null }?.let { body ->
                    Jsoup.parse(body)
                }
            }.getOrNull()
        } ?: suspendCatching {
            warning(mode, "Could not load with ktor client, trying with plain jsoup")
            Jsoup.connect(link).followRedirects(true).get()
        }.getOrNull()
    }

    suspend fun getLatestEpisodes(doc: Document): List<Home.Episode> = coroutineScope {
        return@coroutineScope doc.select("#newest_episodes li").mapNotNull {
            async {
                val episodeTitle = it.selectFirst("li a")?.getTitle() ?: String()
                val episodeHref = BSUtil.normalizeHref(it.selectFirst("li a")?.getHref() ?: String())
                val episodeInfo = it.selectFirst("li .info")?.text() ?: String()
                val episodeFlagElements = it.select("li .info i")

                val episodeInfoFlags: MutableList<Home.Episode.Flag> = mutableListOf()
                episodeFlagElements.forEach { infoFlags ->
                    val flagClass = infoFlags.selectFirst("i")?.className() ?: String()
                    val flagTitle = infoFlags.selectFirst("i")?.getTitle() ?: String()
                    episodeInfoFlags.add(Home.Episode.Flag(flagClass, flagTitle))
                }

                if (episodeTitle.isNotEmpty() && episodeHref.isNotEmpty()) {
                    val (cover, nsfw) = getCover(episodeHref)
                    val newEpisode = Home.Episode(
                        episodeTitle,
                        episodeHref,
                        episodeInfo,
                        episodeInfoFlags,
                        nsfw,
                        cover
                    )
                    newEpisode
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getLatestSeries(doc: Document): List<Home.Series> = coroutineScope {
        return@coroutineScope doc.select("#newest_series li").mapNotNull {
            async {
                val seriesTitle = it.selectFirst("a")?.getTitle() ?: String()
                val seriesHref = BSUtil.normalizeHref(it.selectFirst("a")?.getHref() ?: String())

                if (seriesTitle.isNotEmpty() && seriesHref.isNotEmpty()) {
                    val (cover, nsfw) = getCover(seriesHref)
                    val newSeries = Home.Series(
                        seriesTitle,
                        seriesHref,
                        nsfw,
                        cover
                    )
                    newSeries
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getAllSeries(doc: Document): List<Genre> = coroutineScope {
        return@coroutineScope doc.select("#seriesContainer .genre").mapNotNull {
            async {
                val genre = it.selectFirst("strong")?.text() ?: String()

                if (genre.isNotEmpty()) {
                    Genre(
                        genre,
                        it.select("li").mapNotNull { item ->
                            async {
                                val title = item.selectFirst("a")?.text() ?: String()
                                val href = BSUtil.normalizeHref(item.selectFirst("a")?.getHref() ?: String())

                                if (title.isNotEmpty() && href.isNotEmpty()) {
                                    Genre.Item(
                                        title,
                                        href
                                    )
                                } else {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    )
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getSeries(doc: Document, href: String): Series? {
        val title = doc.selectFirst(".serie h2")?.wholeText() ?: String()
        val description = doc.selectFirst(".serie #sp_left > p")?.text() ?: String()

        val seasons: List<Series.Season> = doc.select(".serie #seasons ul li").mapIndexed { index, it ->
            val seasonTitle = it.selectFirst("a")?.text() ?: it.text()
            val link = BSUtil.normalizeHref(it.selectFirst("a")?.getHref() ?: it.getHref() ?: String())
            val value: Int = if (link.isEmpty()) {
                index
            } else {
                if (link.split('/').size >= 3) {
                    link.split('/')[2].toIntOrNull() ?: index
                } else {
                    index
                }
            }
            Series.Season(seasonTitle, value)
        }

        val docLocation = doc.location().trim()
        fun fallbackDocHref(): String = try {
            URI(docLocation).path.ifBlank { href }
        } catch (ignored: Throwable) {
            href
        }

        val docHref = if (docLocation.isEmpty()) {
            href
        } else {
            try {
                Url(docLocation).encodedPath.ifBlank {
                    fallbackDocHref()
                }
            } catch (ignored: Throwable) {
                fallbackDocHref()
            }
        }

        val selectedValue = doc.selectFirst(".series-language option[selected]")?.getValue()
        var selectedLanguage: String? = null
        val languagesElements = doc.select(".series-language > option")

        val languages = languagesElements.mapNotNull {
            val value = it.getValue() ?: String()
            val text = it.text() ?: String()
            val selected = it.selectFirst("option[selected]")?.getValue()
            if (!selected.isNullOrEmpty() || (!selectedValue.isNullOrEmpty() && selectedValue == value)) {
                selectedLanguage = value
            }
            if (value.isNotEmpty() && text.isNotEmpty()) {
                Series.Language(value, text)
            } else {
                null
            }
        }

        if (selectedLanguage.isNullOrEmpty()) {
            selectedLanguage = selectedValue
            if (selectedLanguage.isNullOrEmpty()) {
                selectedLanguage = languages.firstOrNull()?.value
            }
        }

        val centeredInfo = doc.select(".serie center:has(a)")
        val linkedSeries = centeredInfo.mapNotNull {
            var infoText = it.text()
            val linked = it.selectFirst("a")!!
            infoText = infoText.replace(linked.text(), "")
            val isSpinOff = infoText.contains("spinoff", true)
            val isMainStory =
                infoText.contains("main-story", true) || infoText.contains("mainstory", true) || infoText.contains(
                    "main",
                    true
                )
            val linkedHref = linked.getHref()
            return@mapNotNull if (linkedHref.isNullOrEmpty()) {
                null
            } else {
                Series.Linked(isSpinOff, isMainStory, linked.text(), BSUtil.normalizeHref(linkedHref))
            }
        }

        val infoHeaders = doc.select(".serie .infos div > span").map { it.text() }
        val infoData: MutableList<String> = doc.select(".serie .infos p").map { it.wholeText() }.toMutableList()
        val infoList: MutableList<Series.Info> = mutableListOf()

        if (infoData.contains(description)) {
            infoData.removeAt(infoData.indexOf(description))
        } else if (infoData.contains(description.trim())) {
            infoData.removeAt(infoData.indexOf(description.trim()))
        }

        for (i in infoHeaders.indices) {
            val data = if (infoData.size > i && infoData[i].isNotEmpty()) {
                infoData[i].replace(Regex("(?:(\\n)*\\t)+", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), "\t")
            } else {
                String()
            }

            infoList.add(Series.Info(infoHeaders[i], data))
        }

        val replacedTitle =
            title.replace(Regex("(?:(\\n)*\\t)+", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), "\t")
        val splitTitle = replacedTitle.trim().split("\t")
        val normalizedTitle = splitTitle[0].trim()

        var canon: List<Int> = emptyList()
        var filler: List<Int> = emptyList()
        var mixed: List<Int> = emptyList()

        /**
         * if (
         *             infoList.mapNotNull {
         *                 if (it.header.equals("Genre", true) || it.header.equals("Genres", true)) {
         *                     it
         *                 } else {
         *                     null
         *                 }
         *             }.any { it.data.contains("Anime", true) }
         *         ) {
         *             // Check for filler information here
         *         }
         */

        val episodes = doc.select(".serie .episodes tr")
        val episodeInfoList = episodes.mapNotNull { episodesElement ->
            var watched: Boolean? = null
            if (episodesElement.classNames().contains("watched")) {
                watched = true
            }
            var watchHref: String? = null

            val episodeList = episodesElement.select("td a").mapNotNull { data ->
                val text = data.selectFirst("a")?.text() ?: String()
                val episodeHref = BSUtil.normalizeHref(data.selectFirst("a")?.getHref() ?: String())
                val isWatchIcon = data.selectFirst(".fas") != null

                if (isWatchIcon) {
                    var watchedString = episodeHref

                    if (watchedString.endsWith('/')) {
                        watchedString = watchedString.substring(0, watchedString.length - 2)
                    }
                    watchedString = watchedString.substringAfterLast('/', String())
                    if (watched != true && watchedString.contains("unwatch")) {
                        watched = true
                    }
                    if (watched != true) {
                        watchHref = episodeHref
                    }

                    null
                } else {
                    text.trim() to episodeHref
                }
            }

            val episodeHref = when {
                episodeList.isEmpty() -> return@mapNotNull null
                episodeList[0].second.isNotEmpty() -> episodeList[0].second
                episodeList.size > 1 && episodeList[1].second.isNotEmpty() -> episodeList[1].second
                else -> String()
            }
            val hoster = episodeList.map { it.second }.filterNot { it.isEmpty() }.toMutableSet()
            if (hoster.contains(episodeHref)) {
                hoster.remove(episodeHref)
            } else if (hoster.contains(episodeHref.trim())) {
                hoster.remove(episodeHref.trim())
            }

            val hosterList = hoster.map {
                Series.Episode.Hoster(it.replace(episodeHref, String()).replace("/", ""), it)
            }

            val episodeTitle = if (episodeList.size > 1) episodeList[1].first.trim() else String()
            val episodeNumber = Constants.episodeNumberRegex.find(episodeTitle)?.groupValues?.let {
                val numberMatch = it.lastOrNull()
                numberMatch?.toIntOrNull() ?: numberMatch?.getDigitsOrNull()?.toIntOrNull()
            }

            var isCanon: Boolean? = null
            var isFiller: Boolean? = null
            if (canon.contains(episodeNumber)) {
                isCanon = true
            }
            if (filler.contains(episodeNumber)) {
                isFiller = true
            }
            if (mixed.contains(episodeNumber)) {
                isCanon = true
                isFiller = true
            }

            if (isCanon == null && isFiller != null) {
                isCanon = false
            }
            if (isFiller == null && isCanon != null) {
                isFiller = false
            }

            Series.Episode(
                episodeList[0].first.trim(),
                episodeTitle,
                episodeHref,
                watched,
                watchHref,
                isCanon = isCanon,
                isFiller = isFiller,
                hoster = hosterList
            )
        }

        val (cover, nsfw) = getCover(doc)

        return Series(
            normalizedTitle,
            if (splitTitle.size >= 2) splitTitle[1].trim() else seasons.firstOrNull()?.title ?: String(),
            description,
            selectedLanguage ?: return null,
            cover,
            nsfw,
            infoList,
            languages,
            seasons,
            episodeInfoList,
            linkedSeries,
            docHref
        )
    }

    private suspend fun getCover(href: String): Pair<Cover, Boolean> {
        return getCover(getDocument(href))
    }

    private suspend fun getCover(doc: Document?): Pair<Cover, Boolean> {
        val allImages = doc?.select(".serie img")

        val cover = (allImages?.firstOrNull {
            it.hasAttr("alt") && it.attr("alt").equals("Cover", true)
        } ?: allImages?.firstOrNull())?.getSrc()
        val isNsfw = allImages?.firstOrNull {
            it.hasAttr("alt") && it.attr("alt").equals("AB 18", true)
        } != null

        return coverBlock(cover, isNsfw)
    }

    private fun info(mode: Int?, info: String) {
        if (mode != null) {
            logger?.logInfo(mode, info)
        }
    }

    private fun warning(mode: Int?, info: String) {
        if (mode != null) {
            logger?.logWarning(mode, info)
        }
    }

    private fun error(mode: Int?, info: String) {
        if (mode != null) {
            logger?.logError(mode, info)
        }
    }

}