package com.erekeai.data.documents

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Извлекает текст из .xlsx без Apache POI (та же причина, что и для .docx —
 * POI плохо работает на Android). .xlsx — тоже ZIP с XML: shared strings
 * table (xl/sharedStrings.xml), список листов (xl/workbook.xml +
 * xl/_rels/workbook.xml.rels) и сами данные по листам (xl/worksheets/sheetN.xml).
 * Результат — простой текст с разделением ячеек табуляцией, строк — переносом,
 * листов — заголовком "=== Имя листа ===". Формулы читаются как их кэшированное
 * вычисленное значение (Excel всегда сохраняет его рядом с формулой), не как
 * сама формула.
 */
@Singleton
class XlsxTextExtractor @Inject constructor() {

    fun extract(file: File): String {
        ZipFile(file).use { zip ->
            val sharedStrings = readSharedStrings(zip)
            val sheets = readSheetList(zip)

            if (sheets.isEmpty()) {
                throw IllegalStateException("Некорректный .xlsx: листы не найдены")
            }

            val sb = StringBuilder()
            for ((name, path) in sheets) {
                val entry = zip.getEntry(path) ?: continue
                sb.append("=== $name ===\n")
                sb.append(readSheet(zip.getInputStream(entry), sharedStrings))
                sb.append("\n")
            }
            return sb.toString().trim()
        }
    }

    private fun readSharedStrings(zip: ZipFile): List<String> {
        val entry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
        val parser = newParser(zip.getInputStream(entry))

        val strings = mutableListOf<String>()
        val current = StringBuilder()
        var insideItem = false
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name.substringAfter(':')
                if (tag == "si") {
                    insideItem = true
                    current.clear()
                    event = parser.next()
                    continue
                }
                if (tag == "t" && insideItem) {
                    current.append(parser.nextText())
                    event = parser.eventType
                    continue
                }
            } else if (event == XmlPullParser.END_TAG && parser.name.substringAfter(':') == "si") {
                strings.add(current.toString())
                insideItem = false
            }
            event = parser.next()
        }
        return strings
    }

    /** Returns list of (sheetName, "xl/worksheets/sheetN.xml") in workbook order. */
    private fun readSheetList(zip: ZipFile): List<Pair<String, String>> {
        val relsEntry = zip.getEntry("xl/_rels/workbook.xml.rels")
        val idToTarget = mutableMapOf<String, String>()
        if (relsEntry != null) {
            val parser = newParser(zip.getInputStream(relsEntry))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name.substringAfter(':') == "Relationship") {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null && target.contains("worksheets")) {
                        val normalized = target.removePrefix("/xl/").removePrefix("/")
                        idToTarget[id] = if (normalized.startsWith("xl/")) normalized else "xl/$normalized"
                    }
                }
                event = parser.next()
            }
        }

        val workbookEntry = zip.getEntry("xl/workbook.xml") ?: return emptyList()
        val parser = newParser(zip.getInputStream(workbookEntry))
        val result = mutableListOf<Pair<String, String>>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.substringAfter(':') == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                val rId = (0 until parser.attributeCount)
                    .map { parser.getAttributeName(it) to parser.getAttributeValue(it) }
                    .firstOrNull { it.first.endsWith("id") }?.second
                val target = idToTarget[rId]
                if (target != null) result.add(name to target)
            }
            event = parser.next()
        }
        return result
    }

    private fun readSheet(input: java.io.InputStream, sharedStrings: List<String>): String {
        val parser = newParser(input)
        val sb = StringBuilder()
        var cellType: String? = null
        var cellValue = StringBuilder()
        var inValue = false
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.substringAfter(':')) {
                    "c" -> cellType = parser.getAttributeValue(null, "t")
                    "v", "t" -> { inValue = true; cellValue = StringBuilder() }
                }
                XmlPullParser.TEXT -> if (inValue) cellValue.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name.substringAfter(':')) {
                    "v", "is" -> {
                        inValue = false
                        val raw = cellValue.toString()
                        val resolved = if (cellType == "s") {
                            raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                        } else raw
                        sb.append(resolved).append("\t")
                    }
                    "row" -> sb.append("\n")
                }
            }
            event = parser.next()
        }
        return sb.toString()
    }

    private fun newParser(input: java.io.InputStream): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, "UTF-8")
        return parser
    }
}
