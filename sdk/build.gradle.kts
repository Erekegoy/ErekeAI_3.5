plugins {
    id("org.jetbrains.kotlin.jvm")
}

// 🟡 SDK для других ваших проектов (Android, JVM-бэкенд, десктоп-скрипты — что угодно на JVM):
// тонкий клиент, который просто оборачивает REST API ErekeAI (см. com.erekeai.server.ErekeApiServer,
// порт 8765 по умолчанию). Никакой магии — обычные HTTP-запросы, чтобы модуль было легко
// переиспользовать даже вне Android (например, в скрипте автоматизации на компьютере).
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
