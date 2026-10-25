// Shared plugin declarations. Android configuration and dependencies belong in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
