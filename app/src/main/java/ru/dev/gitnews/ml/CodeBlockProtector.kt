package ru.dev.gitnews.ml

import java.util.regex.Pattern

data class ProtectedContent(
    val sanitizedText: String,
    val tokenMap: Map<String, String>
)

object CodeBlockProtector {

    private val FENCED_CODE_PATTERN = Pattern.compile("```[\\s\\S]*?```")
    private val INLINE_CODE_PATTERN = Pattern.compile("`[^`\\n]+`")
    private val IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\([^)]+\\)")
    private val LINK_URL_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    private val HTML_TAG_PATTERN = Pattern.compile("<[^>]+>")

    fun protect(rawMarkdown: String): ProtectedContent {
        if (rawMarkdown.isBlank()) {
            return ProtectedContent(rawMarkdown, emptyMap())
        }

        val tokenMap = mutableMapOf<String, String>()
        var counter = 0

        // 1. Protect Fenced Code Blocks (```code```)
        val fencedMatcher = FENCED_CODE_PATTERN.matcher(rawMarkdown)
        val sbFenced = StringBuffer()
        while (fencedMatcher.find()) {
            val token = "<<<CODE_BLOCK_${counter++}>>>"
            tokenMap[token] = fencedMatcher.group()
            fencedMatcher.appendReplacement(sbFenced, token)
        }
        fencedMatcher.appendTail(sbFenced)
        var current = sbFenced.toString()

        // 2. Protect Image Tags (![alt](url))
        val imgMatcher = IMAGE_PATTERN.matcher(current)
        val sbImg = StringBuffer()
        while (imgMatcher.find()) {
            val token = "<<<IMG_${counter++}>>>"
            tokenMap[token] = imgMatcher.group()
            imgMatcher.appendReplacement(sbImg, token)
        }
        imgMatcher.appendTail(sbImg)
        current = sbImg.toString()

        // 3. Protect Inline Code (`code`)
        val inlineMatcher = INLINE_CODE_PATTERN.matcher(current)
        val sbInline = StringBuffer()
        while (inlineMatcher.find()) {
            val token = "<<<INLINE_${counter++}>>>"
            tokenMap[token] = inlineMatcher.group()
            inlineMatcher.appendReplacement(sbInline, token)
        }
        inlineMatcher.appendTail(sbInline)
        current = sbInline.toString()

        // 4. Protect Link URLs ([text](url) -> [text](<<<URL_X>>>))
        val linkMatcher = LINK_URL_PATTERN.matcher(current)
        val sbLink = StringBuffer()
        while (linkMatcher.find()) {
            val label = linkMatcher.group(1) ?: ""
            val url = linkMatcher.group(2) ?: ""
            val urlToken = "<<<URL_${counter++}>>>"
            tokenMap[urlToken] = url
            // Keep label for natural language translation, isolate the raw URL
            linkMatcher.appendReplacement(sbLink, "[$label]($urlToken)")
        }
        linkMatcher.appendTail(sbLink)
        current = sbLink.toString()

        // 5. Protect HTML Tags (<tag>)
        val htmlMatcher = HTML_TAG_PATTERN.matcher(current)
        val sbHtml = StringBuffer()
        while (htmlMatcher.find()) {
            val token = "<<<HTML_${counter++}>>>"
            tokenMap[token] = htmlMatcher.group()
            htmlMatcher.appendReplacement(sbHtml, token)
        }
        htmlMatcher.appendTail(sbHtml)
        current = sbHtml.toString()

        return ProtectedContent(current, tokenMap)
    }

    fun restore(translatedText: String, tokenMap: Map<String, String>): String {
        if (tokenMap.isEmpty() || translatedText.isBlank()) {
            return translatedText
        }

        var result = translatedText
        for ((token, originalCode) in tokenMap) {
            // Replace both exact and possible spaces inserted by translation engines
            result = result.replace(token, originalCode)
            val spacedToken = token.replace("<", "< ").replace(">", " >")
            if (result.contains(spacedToken)) {
                result = result.replace(spacedToken, originalCode)
            }
        }
        return result
    }
}
