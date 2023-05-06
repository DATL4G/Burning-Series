package dev.datlag.burningseries.model

object BSUtil {

    fun normalizeHref(href: String): String {
        val regex = "serie\\S+".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(href)?.value ?: href
    }

    fun fixSeriesHref(href: String): String {
        return rebuildHrefFromData(hrefDataFromHref(normalizeHref(href)))
    }

    fun hrefDataFromHref(href: String): Triple<String, String?, String?> {
        fun getTitle(): String {
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

        var newHref = normalizeHref(href)
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
        val title = getTitle().ifBlank {
            hrefSplit[0]
        }
        println(href)
        println(title)
        return Triple(
            title,
            if (season.isNullOrEmpty()) null else season,
            if (!fallbackLanguage.isNullOrEmpty()) {
                fallbackLanguage
            } else {
                if (language.isNullOrEmpty()) null else language
            }
        )
    }

    fun rebuildHrefFromData(hrefData: Triple<String, String?, String?>): String {
        return if (hrefData.second != null && hrefData.third != null) {
            "serie/${hrefData.first}/${hrefData.second}/${hrefData.third}"
        } else if (hrefData.second != null) {
            "serie/${hrefData.first}/${hrefData.second}"
        } else if (hrefData.third != null) {
            "serie/${hrefData.first}/${hrefData.third}"
        } else {
            "serie/${hrefData.first}"
        }
    }

}