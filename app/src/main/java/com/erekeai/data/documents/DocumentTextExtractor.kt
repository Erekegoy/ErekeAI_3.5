package com.erekeai.data.documents

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единая точка входа для извлечения текста из документов: сама решает,
 * каким экстрактором пользоваться, по расширению файла.
 */
@Singleton
class DocumentTextExtractor @Inject constructor(
    private val pdf: PdfTextExtractor,
    private val docx: DocxTextExtractor,
    private val xlsx: XlsxTextExtractor
) {

    val supportedExtensions = setOf("pdf", "docx", "xlsx", "txt", "md")

    fun extract(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> pdf.extract(file)
            "docx" -> docx.extract(file)
            "xlsx" -> xlsx.extract(file)
            "txt", "md" -> file.readText()
            else -> throw IllegalArgumentException(
                "Формат .${file.extension} не поддерживается. Поддерживаются: ${supportedExtensions.joinToString()}"
            )
        }
    }
}
