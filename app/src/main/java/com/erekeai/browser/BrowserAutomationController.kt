package com.erekeai.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * ✅ "Browser Automation" — настоящее управление веб-страницей через Android WebView + JavaScript
 * (не просто скачивание HTML, как [com.erekeai.data.tools.ReadWebPageTool], если такой есть):
 * умеет открыть страницу, дождаться загрузки, выполнить произвольный JS (клик по селектору,
 * заполнение полей, чтение текста), то есть реально взаимодействовать со страницей так же, как
 * это делает браузер, включая JS-сгенерированный контент (SPA), недоступный простому HTTP-фетчу.
 *
 * Технические ограничения (честно): WebView требует Looper главного потока — здесь используется
 * скрытый WebView, создаваемый на Main thread через Handler; сайты с сильной анти-бот защитой
 * или капчей всё равно не пройти; выполнение JS ограничено песочницей WebView (как в обычном
 * браузере) — то есть это НЕ обход системы безопасности сайта, а обычное взаимодействие как
 * пользователь с включённым JavaScript.
 */
@Singleton
class BrowserAutomationController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Открывает URL, ждёт полной загрузки страницы и возвращает результат выполнения [script] (может быть "" — просто чтение title/url не нужно). */
    suspend fun navigateAndRun(url: String, script: String, timeoutMs: Long = 15_000): String = suspendCancellableCoroutine { cont ->
        mainHandler.post {
            val webView = createHeadlessWebView()
            var resumed = false
            val timeoutRunnable = Runnable {
                if (!resumed) { resumed = true; webView.destroy(); cont.resume("Таймаут загрузки страницы ($url)") }
            }
            mainHandler.postDelayed(timeoutRunnable, timeoutMs)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, finishedUrl: String) {
                    if (resumed) return
                    view.evaluateJavascript(script.ifBlank { "document.body.innerText" }) { jsResult ->
                        if (!resumed) {
                            resumed = true
                            mainHandler.removeCallbacks(timeoutRunnable)
                            val cleaned = jsResult?.trim('"')?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: ""
                            webView.destroy()
                            cont.resume(cleaned.take(8000))
                        }
                    }
                }
            }
            webView.loadUrl(url)
        }
        cont.invokeOnCancellation { mainHandler.post { } }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createHeadlessWebView(): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = settings.userAgentString + " ErekeAI-BrowserAgent"
    }
}
