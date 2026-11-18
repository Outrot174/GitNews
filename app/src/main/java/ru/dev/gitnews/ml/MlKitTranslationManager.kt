package ru.dev.gitnews.ml

import android.util.LruCache
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import ru.dev.gitnews.data.local.dao.TranslationCacheDao
import ru.dev.gitnews.data.local.entity.TranslationCacheEntity
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Hybrid Translation Engine for GitNews
 * - Primary: Google Translate (HTTP engine via client=gtx, safe paragraph chunking, token preservation)
 * - Zero offline dictionary downloads when online
 * - Offline Fallback: On-device ML Kit ONLY if the required language model is already downloaded locally
 * - L1 In-Memory LRU Cache + L2 Room SQLite Persistence
 */
class MlKitTranslationManager(
    private val translationCacheDao: TranslationCacheDao
) {

    private val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()

    // Dedicated unauthenticated HTTP client for translation
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    // L1 Memory Cache: up to 500 recently translated snippets
    private val memoryCache = LruCache<String, String>(500)

    // Active ML Kit translators cache by source language tag
    private val activeMlKitTranslators = ConcurrentHashMap<String, Translator>()

    /**
     * Translates plain text into Russian (or targetLang).
     */
    suspend fun translateText(text: String, targetLang: String = "ru"): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext text

        val hash = "gt_v2_${targetLang}_" + sha256(trimmed)
        val l1Hit = memoryCache.get(hash)
        if (l1Hit != null) return@withContext l1Hit

        val l2Hit = translationCacheDao.getTranslation(hash)
        if (l2Hit != null) {
            memoryCache.put(hash, l2Hit.translatedText)
            return@withContext l2Hit.translatedText
        }

        // Skip translation if text is already predominantly Russian
        if (isRussianText(trimmed)) {
            return@withContext text
        }

        var translated: String? = null
        try {
            translated = translateWithGoogle(trimmed, targetLang)
        } catch (_: Exception) {
            // Offline or network error: fallback to ML Kit ONLY if local model is already downloaded
            translated = tryTranslateWithMlKit(trimmed)
        }

        val result = translated ?: text
        if (result != text) {
            memoryCache.put(hash, result)
            translationCacheDao.insertTranslation(
                TranslationCacheEntity(
                    cacheKey = hash,
                    sourceLang = "auto",
                    targetLang = targetLang,
                    translatedText = result
                )
            )
        }

        result
    }

    /**
     * Translates Markdown while preserving code blocks, inline backticks, and URLs.
     */
    suspend fun translateMarkdown(rawMarkdown: String, targetLang: String = "ru"): String = withContext(Dispatchers.IO) {
        if (rawMarkdown.isBlank()) return@withContext rawMarkdown

        val hash = "gt_v2_md_${targetLang}_" + sha256(rawMarkdown.trim())
        val l1Hit = memoryCache.get(hash)
        if (l1Hit != null) return@withContext l1Hit

        val l2Hit = translationCacheDao.getTranslation(hash)
        if (l2Hit != null) {
            memoryCache.put(hash, l2Hit.translatedText)
            return@withContext l2Hit.translatedText
        }

        // 1. Isolate code blocks, backticks, URLs, and HTML tags with unique placeholders
        val protected = CodeBlockProtector.protect(rawMarkdown)

        // 2. Skip if already Russian
        if (isRussianText(protected.sanitizedText)) {
            return@withContext rawMarkdown
        }

        // 3. Translate sanitized text using Google Translate with chunking
        var translatedBody: String? = null
        try {
            translatedBody = translateWithGoogle(protected.sanitizedText, targetLang)
        } catch (_: Exception) {
            // Offline fallback: on-device ML Kit ONLY if model is already downloaded
            translatedBody = tryTranslateWithMlKit(protected.sanitizedText)
        }

        val bodyToRestore = translatedBody ?: protected.sanitizedText

        // 4. Restore original code blocks, backticks, and URLs
        val restored = CodeBlockProtector.restore(bodyToRestore, protected.tokenMap)

        if (restored != rawMarkdown) {
            memoryCache.put(hash, restored)
            translationCacheDao.insertTranslation(
                TranslationCacheEntity(
                    cacheKey = hash,
                    sourceLang = "auto",
                    targetLang = targetLang,
                    translatedText = restored
                )
            )
        }

        restored
    }

    // =========================================================================
    // Primary Engine: Google Translate (HTTP POST client=gtx with safe chunking)
    // =========================================================================

    private suspend fun translateWithGoogle(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        val chunks = splitIntoSafeChunks(text, maxChunkSize = 1800)
        if (chunks.size == 1) {
            return@withContext executeGoogleTranslateCall(chunks[0], targetLang)
        }

        val results = mutableListOf<String>()
        for (chunk in chunks) {
            try {
                val translated = executeGoogleTranslateCall(chunk, targetLang)
                results.add(translated)
            } catch (e: Exception) {
                // Short delay & retry once
                try {
                    delay(200)
                    val retried = executeGoogleTranslateCall(chunk, targetLang)
                    results.add(retried)
                } catch (_: Exception) {
                    // Keep original chunk rather than aborting the entire document
                    results.add(chunk)
                }
            }
        }
        results.joinToString("\n\n")
    }

    private fun executeGoogleTranslateCall(chunk: String, targetLang: String): String {
        val trimmed = chunk.trim()
        if (trimmed.isBlank()) return chunk

        val formBody = FormBody.Builder()
            .add("q", trimmed)
            .build()

        val request = Request.Builder()
            .url("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .post(formBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Google Translate HTTP ${response.code}")
        }
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        val parsed = parseGoogleTranslateJson(responseBody)
        return if (parsed.isNotBlank()) parsed else chunk
    }

    private fun parseGoogleTranslateJson(jsonStr: String): String {
        return try {
            val outer = JSONArray(jsonStr)
            val sentences = outer.optJSONArray(0) ?: return ""
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val sentence = sentences.optJSONArray(i)
                if (sentence != null && !sentence.isNull(0)) {
                    sb.append(sentence.optString(0, ""))
                }
            }
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun splitIntoSafeChunks(text: String, maxChunkSize: Int = 1800): List<String> {
        if (text.length <= maxChunkSize) return listOf(text)

        val paragraphs = text.split("\n\n")
        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()

        fun flushCurrent() {
            if (currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString())
                currentChunk.clear()
            }
        }

        for (para in paragraphs) {
            if (currentChunk.length + para.length + 2 <= maxChunkSize) {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
                currentChunk.append(para)
            } else {
                flushCurrent()
                if (para.length <= maxChunkSize) {
                    currentChunk.append(para)
                } else {
                    // Split giant paragraph by single newline
                    val lines = para.split("\n")
                    for (line in lines) {
                        if (currentChunk.length + line.length + 1 <= maxChunkSize) {
                            if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                            currentChunk.append(line)
                        } else {
                            flushCurrent()
                            if (line.length <= maxChunkSize) {
                                currentChunk.append(line)
                            } else {
                                // Split giant single line while preserving <<<TOKEN>>> boundaries
                                val subLines = splitGiantLineSafely(line, maxChunkSize)
                                for (sub in subLines) {
                                    if (currentChunk.length + sub.length + 1 <= maxChunkSize) {
                                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                                        currentChunk.append(sub)
                                    } else {
                                        flushCurrent()
                                        currentChunk.append(sub)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        flushCurrent()
        return chunks
    }

    private fun splitGiantLineSafely(line: String, maxSize: Int): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        while (start < line.length) {
            if (line.length - start <= maxSize) {
                result.add(line.substring(start))
                break
            }
            var targetEnd = start + maxSize

            // Never cut <<<TOKEN>>> in half
            val lastOpen = line.lastIndexOf("<<<", targetEnd)
            val lastClose = line.lastIndexOf(">>>", targetEnd)
            if (lastOpen > lastClose && lastOpen >= start) {
                val tokenEnd = line.indexOf(">>>", targetEnd)
                targetEnd = if (tokenEnd != -1) tokenEnd + 3 else lastOpen
            } else {
                val spaceIndex = line.lastIndexOf(' ', targetEnd)
                if (spaceIndex > start + maxSize / 2) {
                    targetEnd = spaceIndex
                }
            }
            val safeEnd = targetEnd.coerceIn(start + 1, line.length)
            result.add(line.substring(start, safeEnd))
            start = if (safeEnd < line.length && line[safeEnd] == ' ') safeEnd + 1 else safeEnd
        }
        return result
    }

    // =========================================================================
    // Secondary Engine: On-Device ML Kit Fallback (ONLY if model is already downloaded)
    // =========================================================================

    private suspend fun tryTranslateWithMlKit(text: String): String? {
        return try {
            val detectedLang = identifyLanguage(text)
            if (detectedLang == "ru" || detectedLang == "und") return null
            val sourceMlKitLang = resolveMlKitLanguage(detectedLang) ?: return null

            // Strict rule: NEVER trigger downloadModelIfNeeded! Only translate if model exists locally.
            if (!isMlKitModelDownloaded(sourceMlKitLang)) {
                return null
            }

            val translator = getExistingMlKitTranslator(sourceMlKitLang) ?: return null
            performMlKitTranslation(translator, text)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun isMlKitModelDownloaded(sourceLanguage: String): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val model = TranslateRemoteModel.Builder(sourceLanguage).build()
            RemoteModelManager.getInstance().isModelDownloaded(model)
                .addOnSuccessListener { isDownloaded ->
                    cont.resume(isDownloaded)
                }
                .addOnFailureListener {
                    cont.resume(false)
                }
        } catch (_: Exception) {
            cont.resume(false)
        }
    }

    private fun getExistingMlKitTranslator(sourceLanguage: String): Translator? {
        val existing = activeMlKitTranslators[sourceLanguage]
        if (existing != null) return existing

        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(TranslateLanguage.RUSSIAN)
                .build()
            val translator = Translation.getClient(options)
            activeMlKitTranslators[sourceLanguage] = translator
            translator
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun performMlKitTranslation(
        translator: Translator,
        text: String
    ): String = suspendCancellableCoroutine { cont ->
        translator.translate(text)
            .addOnSuccessListener { result ->
                cont.resume(result)
            }
            .addOnFailureListener { e ->
                cont.cancel(e)
            }
    }

    private suspend fun identifyLanguage(text: String): String = suspendCancellableCoroutine { cont ->
        val sample = text.take(500)
        languageIdentifier.identifyLanguage(sample)
            .addOnSuccessListener { languageCode ->
                cont.resume(languageCode)
            }
            .addOnFailureListener {
                cont.resume("und")
            }
    }

    private fun resolveMlKitLanguage(detectedCode: String): String? {
        return when (detectedCode.lowercase()) {
            "en" -> TranslateLanguage.ENGLISH
            "zh", "zh-cn", "zh-tw" -> TranslateLanguage.CHINESE
            "ja" -> TranslateLanguage.JAPANESE
            "de" -> TranslateLanguage.GERMAN
            "fr" -> TranslateLanguage.FRENCH
            "es" -> TranslateLanguage.SPANISH
            "ko" -> TranslateLanguage.KOREAN
            "it" -> TranslateLanguage.ITALIAN
            "pt" -> TranslateLanguage.PORTUGUESE
            "nl" -> TranslateLanguage.DUTCH
            "pl" -> TranslateLanguage.POLISH
            "tr" -> TranslateLanguage.TURKISH
            "ar" -> TranslateLanguage.ARABIC
            else -> TranslateLanguage.ENGLISH
        }
    }

    private fun isRussianText(text: String): Boolean {
        var cyrillicCount = 0
        var totalLetters = 0
        for (ch in text) {
            if (ch.isLetter()) {
                totalLetters++
                if (ch in '\u0400'..'\u04FF') {
                    cyrillicCount++
                }
            }
        }
        return totalLetters > 0 && (cyrillicCount.toFloat() / totalLetters) > 0.45f
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
