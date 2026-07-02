package com.erekeai.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.erekeai.data.plugin.PluginRepositoryImpl
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ErekeAiApplication : Application(), Configuration.Provider {

    // 🟡 Планировщик фоновых агентов: WorkManager должен создавать Worker'ы через Hilt,
    // чтобы им можно было внедрять ChatRepository/AgentOrchestrator и т.д.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // 🟡 Система плагинов: установленные ранее плагины нужно заново зарегистрировать как
    // инструменты при каждом запуске приложения (реестр инструментов не переживает перезапуск процесса).
    @Inject lateinit var pluginRepository: PluginRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        // PDFBox-Android needs its font/resource assets initialized once
        // before any PDDocument is loaded, or PDF text extraction throws.
        PDFBoxResourceLoader.init(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { pluginRepository.restoreInstalledIntoRegistry() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
