package dev.datlag.burningseries.network

import dev.datlag.burningseries.model.HosterStream
import dev.datlag.burningseries.model.VideoStream
import dev.datlag.burningseries.network.common.getSources
import dev.datlag.jsunpacker.JsUnpacker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object VideoScraper {

    suspend fun scrapeVideosFrom(hosterStream: HosterStream): VideoStream? {
        val doc = try {
            Jsoup.connect(hosterStream.url).get()
        } catch (ignored: Throwable) {
            return null
        }

        val srcList: MutableList<String> = mutableListOf()
        srcList.addAll(getStreamsInDoc(doc))

        doc.select("iframe").forEach {
            it.getSources().forEach src@{ src ->
                val iframeDoc = try {
                    Jsoup.connect(src).get()
                } catch (ignored: Throwable) {
                    return@src
                }

                srcList.addAll(getStreamsInDoc(iframeDoc))
            }
        }

        val finalList = if (hosterStream.hoster.equals("StreamZ", true) || hosterStream.hoster.equals("StreamZZ", true)) {
            srcList.filterNot { it.contains("getlink") }
        } else {
            srcList.toList()
        }

        return if (srcList.isEmpty()) {
            null
        } else {
            VideoStream(hosterStream, srcList)
        }
    }

    private suspend fun getStreamsInDoc(doc: Document): List<String> {
        val srcList: MutableList<String> = mutableListOf()
        val videoElements = doc.select("video")
        videoElements.forEach {
            srcList.addAll(it.getSources())
        }
        val html = doc.html()
        val regex = Regex(
            "http(s?)://\\S+\\.(mp4|m3u8|webm|mkv|flv|vob|drc|gifv|avi|((m?)(2?)ts)|mov|qt|wmv|yuv|rm((vb)?)|viv|asf|amv|m4p|m4v|mp2|mp((e)?)g|mpe|mpv|m2v|svi|3gp|3g2|mxf|roq|nsv|f4v|f4p|f4a|f4b)",
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        )
        val regexWithQueryParams = "${regex.pattern}(\\?\\w+=(\\w|-)*(?:&(?:\\w+=(\\w|[-_.~%])*|=(\\w|[-_.~%])+))*)?".toRegex(
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        )
        regex.findAll(html).forEach {
            srcList.add(it.value)
        }
        regexWithQueryParams.findAll(html).forEach {
            srcList.add(it.value)
        }

        JsUnpacker.unpack(doc.select("script").map { it.data().trim() }).forEach {
            srcList.addAll(regex.findAll(it).map { result -> result.value })
            srcList.addAll(regexWithQueryParams.findAll(it).map { result -> result.value })
            if (it.contains("video", true)) {
                val dllRegex = Regex(
                    "http(s?)://\\S+\\.(dll)",
                    setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
                )
                srcList.addAll(dllRegex.findAll(it).map { result -> result.value })
            }
        }

        return srcList
    }

}