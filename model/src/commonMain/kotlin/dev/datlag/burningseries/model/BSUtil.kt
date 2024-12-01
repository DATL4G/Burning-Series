package dev.datlag.burningseries.model

data object BSUtil {

    const val PROTOCOL_HTTP = "http://"
    const val PROTOCOL_HTTPS = "https://"

    const val HOST_BS_TO = "bs.to"
    const val SEARCH = "andere-serien"

    val episodeNumberRegex = "[|({]\\s*(Ep([.]|isode)?|Folge)\\s*(\\d+)\\s*[|)}]".toRegex(RegexOption.IGNORE_CASE)

    fun getBurningSeriesLink(href: String, http: Boolean = false, host: String = HOST_BS_TO): String {
        return if (!href.matches("^\\w+?://.*".toRegex())) {
            if (!href.startsWith("/")) {
                "${if (http) PROTOCOL_HTTP else PROTOCOL_HTTPS}${host}/$href"
            } else {
                "${if (http) PROTOCOL_HTTP else PROTOCOL_HTTPS}${host}${"(?!:|/{2,})(/.*)".toRegex().find(href)?.value}"
            }
        } else {
            href
        }
    }

    fun normalizeHref(href: String): String {
        val regex = "serie\\S+".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(href)?.value ?: href
    }

    fun fixSeriesHref(href: String): String {
        return SeriesData.fromHref(normalizeHref(href)).toHref()
    }

    fun commonSeriesHref(href: String): String {
        return SeriesData.commonHref(fixSeriesHref(href)).toHref()
    }

    fun seasonFrom(href: String): Int? {
        return SeriesData.fromHref(href).season
    }

    fun matchingUrl(url1: String?, url2: String?): String? {
        val fixedUrl1 = url1?.let(::normalizeHref)
        val regex = "serie\\S+".toRegex(RegexOption.IGNORE_CASE)

        if (regex.containsMatchIn(fixedUrl1 ?: "")) {
            return fixedUrl1
        }

        val fixedUrl2 = url2?.let(::normalizeHref)
        if (regex.containsMatchIn(fixedUrl2 ?: "")) {
            return fixedUrl2
        }

        return null
    }
}