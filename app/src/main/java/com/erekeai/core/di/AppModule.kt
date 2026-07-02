package com.erekeai.core.di

/**
 * Раньше здесь были ручные @Provides для SecureKeyStore/SettingsDataStore — это дублировало
 * их собственные @Inject-конструкторы (оба класса теперь сами объявляют
 * @Inject constructor(@ApplicationContext context: Context)) и вызывало у Hilt ошибку
 * компиляции "X is bound multiple times". Модуль оставлен пустым файлом-заглушкой на случай,
 * если понадобится добавить сюда что-то ещё общее для приложения.
 */
