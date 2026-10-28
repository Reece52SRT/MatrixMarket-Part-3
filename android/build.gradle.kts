// Top-level build file. Versions are pinned to a combination known to work together:
// AGP 8.5.2 + Gradle 8.9 + Kotlin 1.9.24 + Compose compiler 1.5.14 + KSP 1.9.24-1.0.20.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
