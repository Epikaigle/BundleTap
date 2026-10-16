buildscript {
    dependencies {
        // Keep Android Gradle Plugin's built-in Kotlin and Compose compiler in sync.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
