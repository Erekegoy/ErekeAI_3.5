// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
}

// layout.buildDirectory (вместо устаревшего rootProject.buildDir: File) — buildDir был
// объявлен deprecated ещё в Gradle 7/8 и полностью убран в API в Gradle 9; используем
// современный Provider<Directory> API, чтобы task 'clean' работал на любой версии Gradle.
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
