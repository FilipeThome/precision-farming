package com.precisionfarming.reporting.application

import java.nio.charset.StandardCharsets
import java.time.Instant

class ReportPdfRenderer {
    fun render(title: String, subtitle: String, generatedAt: Instant, headers: List<String>, rows: List<List<String>>): ByteArray {
        val contentPages = buildLines(title, subtitle, generatedAt, headers, rows).chunked(34)
        val pageObjectIds = mutableListOf<Int>()
        val contentObjectIds = mutableListOf<Int>()
        val objects = mutableListOf<String>()
        var nextId = 4

        contentPages.forEach { lines ->
            val pageId = nextId++
            val contentId = nextId++
            pageObjectIds += pageId
            contentObjectIds += contentId
            objects += pageObject(pageId, contentId)
            objects += contentObject(contentId, lines)
        }

        val pagesKids = pageObjectIds.joinToString(" ") { "$it 0 R" }
        val body = buildString {
            append("%PDF-1.4\n")
            append(obj(1, "<< /Type /Catalog /Pages 2 0 R >>"))
            append(obj(2, "<< /Type /Pages /Kids [$pagesKids] /Count ${pageObjectIds.size} >>"))
            append(obj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"))
            objects.forEach(::append)
        }

        val bytes = body.toByteArray(StandardCharsets.ISO_8859_1)
        val offsets = mutableListOf<Int>()
        var idx = 0
        while (idx < bytes.size) {
            if (idx == 0 || bytes[idx - 1] == '\n'.code.toByte()) {
                if (bytes.copyOfRange(idx, minOf(idx + 12, bytes.size)).toString(StandardCharsets.ISO_8859_1).contains(" 0 obj")) {
                    offsets += idx
                }
            }
            idx++
        }

        val xrefStart = bytes.size
        val xref = buildString {
            append("xref\n")
            append("0 ${offsets.size + 1}\n")
            append("0000000000 65535 f \n")
            offsets.forEach { append(String.format("%010d 00000 n \n", it)) }
            append("trailer << /Size ${offsets.size + 1} /Root 1 0 R >>\n")
            append("startxref\n")
            append(xrefStart)
            append("\n%%EOF\n")
        }
        return bytes + xref.toByteArray(StandardCharsets.ISO_8859_1)
    }

    private fun buildLines(
        title: String,
        subtitle: String,
        generatedAt: Instant,
        headers: List<String>,
        rows: List<List<String>>,
    ): List<String> {
        val tableLines = rows.map { row -> row.joinToString(" | ") { sanitize(it) } }
        return buildList {
            add("Precision Farming")
            add(sanitize(title))
            add(sanitize(subtitle))
            add("Gerado em ${generatedAt.toString().replace("T", " ").removeSuffix("Z")} UTC")
            add("Demo operacional - Safra 2025/26")
            add("")
            add(headers.joinToString(" | ") { sanitize(it) })
            add("-".repeat(96))
            addAll(tableLines.ifEmpty { listOf("Nenhum dado disponivel para o filtro selecionado.") })
        }
    }

    private fun pageObject(pageId: Int, contentId: Int) = obj(
        pageId,
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentId 0 R >>",
    )

    private fun contentObject(contentId: Int, lines: List<String>): String {
        val content = buildString {
            append("BT\n/F1 16 Tf\n50 770 Td\n")
            lines.forEachIndexed { index, line ->
                if (index == 1) append("/F1 14 Tf\n")
                if (index == 3) append("/F1 11 Tf\n")
                if (index > 0) append("0 -16 Td\n")
                append("(")
                append(escape(line))
                append(") Tj\n")
            }
            append("ET")
        }
        val bytes = content.toByteArray(StandardCharsets.ISO_8859_1)
        return obj(contentId, "<< /Length ${bytes.size} >>\nstream\n$content\nendstream")
    }

    private fun sanitize(value: String): String =
        value
            .replace('–', '-')
            .replace('—', '-')
            .replace('…', '.')
            .replace('º', 'o')
            .replace('ª', 'a')

    private fun escape(value: String): String = sanitize(value)
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("\n", " ")
        .replace("\r", " ")

    private fun obj(id: Int, body: String) = "$id 0 obj\n$body\nendobj\n"
}
