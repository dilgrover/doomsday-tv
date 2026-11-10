import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The Doomsday app: a countdown to Avengers: Doomsday on its own, with four pictures the
// user brings by link (README.md). The look and the pictures' life live in :doomcore.
android {
    namespace = "com.avanhq.doomsday"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.avanhq.doomsday"
        minSdk = 28
        targetSdk = 34
        // A CI run number, if there is one, so every build installs over the last one.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.2"
    }

    // Debug builds use Android's own debug key. A release build is signed with your own key,
    // read from keystore.properties (never committed; docs/BUILDING.md).
    val keys = rootProject.file("keystore.properties")
    signingConfigs {
        if (keys.exists()) {
            val p = Properties().apply { keys.inputStream().use { load(it) } }
            create("release") {
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (keys.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.maxHeapSize = "2g" }
    }
}

dependencies {
    implementation(project(":doomcore"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // Android's org.json is a stub on the JVM
    testImplementation("org.robolectric:robolectric:4.14.1") // ShotsTest: the screens drawn off the TV
    testImplementation("androidx.test:core:1.6.1")
}
