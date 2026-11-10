plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// The look and the life of the Doomsday screen: Ui and Fonts (the bundled Manrope), the paced
// frames, the rolling digits, the countdown and the pictures' effects (DoomFx).
// Package com.avanhq.tv (where this code began).
android {
    namespace = "com.avanhq.doomcore"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
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
    implementation("androidx.core:core-ktx:1.13.1")
}
