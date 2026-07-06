package com.erekeai.data.remote.provider

import android.content.Context
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.repository.AiProvider
import com.erekeai.llm.LlamaManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineProvider @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val llamaManager: LlamaManager
) : AiProvider {

    override val type = AiProviderType.OFFLINE

    override fun isConfigured(): Boolean = true

    override fun streamReply(history: List<ChatMessage>): Flow<String> = flow {

        if (!llamaManager.isLoaded()) {
            llamaManager.load()
        }

        val prompt = history.lastOrNull()?.text.orEmpty()

        llamaManager.generate(prompt).collect { token ->
            emit(token)
        }
    }
}
