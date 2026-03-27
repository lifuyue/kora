plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.lifuyue.kora.core.testing"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":core:database"))

    api(libs.junit)
    api(libs.okhttp)
    api(libs.coroutines.test)
    api(libs.mockwebserver)
    api(libs.androidx.test.core)
    api(libs.androidx.datastore.preferences)
    api(libs.androidx.room.runtime)
    api(libs.robolectric)
}
