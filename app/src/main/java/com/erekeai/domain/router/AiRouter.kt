package com.erekeai.domain.router

import com.erekeai.domain.model.AiProviderType

data class RoutingDecision(val provider: AiProviderType, val reason: String)

/**
 * 🟡 "AI Router" — выбирает провайдера под задачу автоматически, вместо того чтобы пользователь
 * каждый раз переключал его руками. ЧЕСТНО: это эвристический (rule-based) роутер по ключевым
 * словам и признакам задачи (код/изображение/скорость/глубина рассуждений), а не обученный ML-
 * классификатор — такой классификатор было бы избыточно тяжело гонять на телефоне ради выбора
 * из 3-4 вариантов. Работает только среди СКОНФИГУРИРОВАННЫХ (есть API-ключ) провайдеров.
 */
interface AiRouter {
    suspend fun route(
        taskText: String,
        hasImage: Boolean,
        availableProviders: List<AiProviderType>
    ): RoutingDecision
}
