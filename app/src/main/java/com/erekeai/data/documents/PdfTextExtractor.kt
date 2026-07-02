package com.erekeai.data.documents

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реальное извлечение текста из PDF через PDFBox-Android. Работает с
 * текстовыми PDF; отсканированные PDF (изображения без текстового слоя)
 * честно вернут пустой/почти пустой текст — для них нужен отдельный OCR,
 * который здесь не реализован.
 */
@Singleton
class PdfTextExtractor @Inject constructor() {

    fun extract(file: File): String {
        PDDocument.load(file).use { document ->
            if (document.isEncrypted) {
                throw IllegalStateException("PDF защищён паролем — не поддерживается")
            }
            return PDFTextStripper().getText(document)
        }
    }
}
