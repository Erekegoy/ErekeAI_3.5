package com.erekeai.data.documents

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Извлекает текст из .docx без тяжёлой зависимости Apache POI (которая плохо
 * дружит с Android — тянет java.awt и StAX, которых на Android нет). .docx —
 * это ZIP-архив с XML внутри; этот класс читает word/document.xml напрямую
 * и вытаскивает текстовые узлы <w:t>, вставляя перевод строки на каждом
 * закрытии параграфа <w:p>. Это реальный, работающий парсер — не заглушка —
 * но он извлекает только текст (без таблиц-как-таблиц, изображений, сносок).
 */
@Singleton
class DocxTextExtractor @Inject constructor() {

    fun extract(file: File): String {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml")
                ?: throw IllegalStateException("Некорректный .docx: word/document.xml не найден")

            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(zip.getInputStream(entry), "UTF-8")

            val sb = StringBuilder()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.substringAfter(':') == "t") {
                            sb.append(parser.nextText())
                            // nextText() consumes up through this element's END_TAG and
                            // leaves the parser positioned on the *next* event already,
                            // so read the current event type instead of calling next() again.
                            event = parser.eventType
                            continue
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.substringAfter(':') == "p") {
                            sb.append("\n")
                        }
                    }
                }
                event = parser.next()
            }
            return sb.toString().trim()
        }
    }
}
