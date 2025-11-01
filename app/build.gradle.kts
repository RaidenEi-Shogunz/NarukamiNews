plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services") // ✅ Firebase plugin
}

android {
    namespace = "com.example.narukaminews"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.narukaminews"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "2.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.2-dev-k2.0.21"
    }

    packaging {
        resources.excludes += listOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt",
            "META-INF/{AL2.0,LGPL2.1}"
        )
    }
}

dependencies {
    // 🧱 AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation(libs.androidx.activity.compose)

    // 🎨 Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // 🧭 Navigation (AndroidX chính thức, có animation)
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.35.0")
    // ⚠️ Gỡ dòng này nếu bạn đã có:
    implementation(libs.accompanist.navigation.animation)

    // 🎨 Accompanist System UI Controller (giữ lại vì hữu ích)
    implementation(libs.accompanist.systemuicontroller)

    // ⚡ Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // 💾 Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // 🌐 Jsoup + WebView
    implementation(libs.org.jsoup)
    implementation(libs.androidx.webkit)

    // 💾 DataStore
    implementation(libs.androidx.datastore.preferences)

    // 🖼️ Coil
    implementation(libs.coil.compose)

    // 🌀 Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // 🌐 Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ✅ Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.firebase:firebase-storage")
    implementation("androidx.compose.ui:ui-text")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    // 🧪 Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.2")
}

kapt {
    correctErrorTypes = true
}
