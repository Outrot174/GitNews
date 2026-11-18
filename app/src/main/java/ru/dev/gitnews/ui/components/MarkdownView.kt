package ru.dev.gitnews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class ListItem(val isOrdered: Boolean, val prefix: String, val text: String, val indentLevel: Int) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    object HorizontalRule : MarkdownBlock
}

@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    SelectionContainer {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceSubtle)
                .hairlineBorder(colors.borderGhost, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                blocks.forEachIndexed { index, block ->
                    when (block) {
                        is MarkdownBlock.Heading -> {
                            val (fontSize, fontWeight) = when (block.level) {
                                1 -> 20.sp to FontWeight.Bold
                                2 -> 17.sp to FontWeight.SemiBold
                                3 -> 15.sp to FontWeight.SemiBold
                                else -> 14.sp to FontWeight.SemiBold
                            }

                            Text(
                                text = buildInlineMarkdown(block.text, colors.textPrimary, fontSize),
                                style = typography.title.copy(
                                    fontSize = fontSize,
                                    fontWeight = fontWeight,
                                    color = colors.textPrimary,
                                    lineHeight = (fontSize.value * 1.35f).sp
                                ),
                                modifier = Modifier.padding(top = if (index > 0) 6.dp else 0.dp)
                            )
                        }

                        is MarkdownBlock.CodeBlock -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceCanvas)
                                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    if (!block.language.isNullOrBlank()) {
                                        Text(
                                            text = block.language.uppercase(),
                                            style = typography.caption.copy(
                                                fontSize = 10.sp,
                                                color = colors.textTertiary,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.sp
                                            ),
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .padding(bottom = 6.dp)
                                        )
                                    }
                                    Text(
                                        text = block.code,
                                        style = typography.monoData.copy(
                                            fontSize = 12.sp,
                                            color = colors.textPrimary,
                                            lineHeight = 18.sp
                                        ),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }

                        is MarkdownBlock.Blockquote -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.surfaceCanvas)
                                    .padding(vertical = 6.dp, horizontal = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .background(colors.signalEmerald)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = buildInlineMarkdown(block.text, colors.textSecondary, 13.sp),
                                    style = typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = colors.textSecondary,
                                        lineHeight = 19.sp
                                    )
                                )
                            }
                        }

                        is MarkdownBlock.ListItem -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = (block.indentLevel * 14).dp, top = 1.dp, bottom = 1.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = block.prefix,
                                    style = if (block.isOrdered) typography.monoData.copy(fontSize = 12.sp, color = colors.signalEmerald)
                                    else typography.bodyMedium.copy(fontSize = 13.sp, color = colors.signalEmerald),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = buildInlineMarkdown(block.text, colors.textPrimary, 13.5.sp),
                                    style = typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        color = colors.textPrimary,
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is MarkdownBlock.Paragraph -> {
                            Text(
                                text = buildInlineMarkdown(block.text, colors.textPrimary, 13.5.sp),
                                style = typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    color = colors.textPrimary,
                                    lineHeight = 21.sp
                                )
                            )
                        }

                        is MarkdownBlock.HorizontalRule -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.borderGhost)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun buildInlineMarkdown(
    text: String,
    baseColor: Color,
    baseFontSize: TextUnit
): AnnotatedString {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography

    return remember(text, baseColor, baseFontSize, colors) {
        buildAnnotatedString {
            val regex = Regex(
                "\\*\\*(.+?)\\*\\*|" +
                "__(.+?)__|" +
                "`([^`]+)`|" +
                "\\[([^\\]]+)\\]\\(([^)]+)\\)|" +
                "~~(.+?)~~|" +
                "\\*([^*]+)\\*|" +
                "_([^_]+)_"
            )

            var currentIndex = 0
            for (match in regex.findAll(text)) {
                val matchRange = match.range
                if (matchRange.first > currentIndex) {
                    append(text.substring(currentIndex, matchRange.first))
                }

                when {
                    match.groups[1] != null || match.groups[2] != null -> {
                        val content = match.groups[1]?.value ?: match.groups[2]?.value ?: ""
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold, color = baseColor),
                            start,
                            length
                        )
                    }

                    match.groups[3] != null -> {
                        val content = match.groups[3]?.value ?: ""
                        val start = length
                        append(" $content ")
                        addStyle(
                            SpanStyle(
                                fontFamily = typography.monoData.fontFamily,
                                background = colors.surfaceElevated,
                                color = colors.signalCyan,
                                fontSize = (baseFontSize.value * 0.88f).sp,
                                fontWeight = FontWeight.Medium
                            ),
                            start,
                            length
                        )
                    }

                    match.groups[4] != null && match.groups[5] != null -> {
                        val label = match.groups[4]?.value ?: ""
                        val url = match.groups[5]?.value ?: ""
                        val start = length
                        append(label)
                        addLink(
                            url = LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = colors.signalCyan,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            ),
                            start = start,
                            end = length
                        )
                    }

                    match.groups[6] != null -> {
                        val content = match.groups[6]?.value ?: ""
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough, color = colors.textTertiary),
                            start,
                            length
                        )
                    }

                    match.groups[7] != null || match.groups[8] != null -> {
                        val content = match.groups[7]?.value ?: match.groups[8]?.value ?: ""
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic, color = baseColor),
                            start,
                            length
                        )
                    }
                }

                currentIndex = matchRange.last + 1
            }

            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }
}

private fun sanitizeMarkdown(raw: String): String {
    var text = raw.replace("\r\n", "\n").replace("\r", "\n")

    // Replace HTML links: <a href="url">text</a> -> [text](url)
    text = text.replace(Regex("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", RegexOption.IGNORE_CASE)) { match ->
        val url = match.groupValues[1]
        val label = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
        if (label.isNotBlank()) "[$label]($url)" else ""
    }

    // Strip images
    text = text.replace(Regex("<img\\s+[^>]*>", RegexOption.IGNORE_CASE), "")
    text = text.replace(Regex("!\\[([^\\]]*)\\]\\([^)]+\\)"), "")

    // Line breaks
    text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

    // Clean common HTML tags
    text = text.replace(Regex("</?(?:div|p|span|center|summary|details|table|tbody|thead|tr|td|th)[^>]*>", RegexOption.IGNORE_CASE), "")

    // Bold/Italic HTML
    text = text.replace(Regex("</?(?:strong|b)>", RegexOption.IGNORE_CASE), "**")
    text = text.replace(Regex("</?(?:em|i)>", RegexOption.IGNORE_CASE), "*")

    return text
}

private fun parseMarkdownBlocks(rawMarkdown: String): List<MarkdownBlock> {
    val sanitized = sanitizeMarkdown(rawMarkdown)
    val lines = sanitized.lines()
    val blocks = mutableListOf<MarkdownBlock>()

    var inCodeBlock = false
    var codeLanguage: String? = null
    val currentCodeLines = mutableListOf<String>()
    val currentParagraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (currentParagraphLines.isNotEmpty()) {
            val content = currentParagraphLines.joinToString(" ").trim()
            if (content.isNotBlank()) {
                blocks.add(MarkdownBlock.Paragraph(content))
            }
            currentParagraphLines.clear()
        }
    }

    fun flushCodeBlock() {
        val codeContent = currentCodeLines.joinToString("\n")
        blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeContent))
        currentCodeLines.clear()
        inCodeBlock = false
        codeLanguage = null
    }

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                flushCodeBlock()
            } else {
                flushParagraph()
                inCodeBlock = true
                val lang = trimmed.removePrefix("```").trim()
                codeLanguage = if (lang.isNotBlank()) lang else null
            }
            continue
        }

        if (inCodeBlock) {
            currentCodeLines.add(line)
            continue
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            continue
        }

        if (trimmed.matches(Regex("^([*\\-_])\\s*(\\1\\s*){2,}$"))) {
            flushParagraph()
            blocks.add(MarkdownBlock.HorizontalRule)
            continue
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.+)").find(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            val level = headingMatch.groupValues[1].length
            val title = headingMatch.groupValues[2].trim()
            blocks.add(MarkdownBlock.Heading(level, title))
            continue
        }

        if (trimmed.startsWith(">")) {
            flushParagraph()
            val quoteText = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.Blockquote(quoteText))
            continue
        }

        val bulletMatch = Regex("^(\\s*)([-*+])\\s+(.+)").find(line)
        if (bulletMatch != null) {
            flushParagraph()
            val indentSpaces = bulletMatch.groupValues[1].length
            val indentLevel = (indentSpaces / 2).coerceIn(0, 4)
            val bulletText = bulletMatch.groupValues[3].trim()
            blocks.add(MarkdownBlock.ListItem(isOrdered = false, prefix = "•", text = bulletText, indentLevel = indentLevel))
            continue
        }

        val orderedMatch = Regex("^(\\s*)(\\d+)\\.\\s+(.+)").find(line)
        if (orderedMatch != null) {
            flushParagraph()
            val indentSpaces = orderedMatch.groupValues[1].length
            val indentLevel = (indentSpaces / 2).coerceIn(0, 4)
            val num = orderedMatch.groupValues[2]
            val itemText = orderedMatch.groupValues[3].trim()
            blocks.add(MarkdownBlock.ListItem(isOrdered = true, prefix = "$num.", text = itemText, indentLevel = indentLevel))
            continue
        }

        currentParagraphLines.add(trimmed)
    }

    if (inCodeBlock) {
        flushCodeBlock()
    }
    flushParagraph()

    return blocks
}
