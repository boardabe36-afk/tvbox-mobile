import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * keystore 凭证：参考 tvbox-simple 的 keystore.properties.example
 */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        load(keystorePropsFile.inputStream())
    }
}
val releaseStoreFile: String = (System.getenv("TVBOX_KEYSTORE_PATH")
    ?: keystoreProps.getProperty("storeFile", ""))
val releaseStorePassword: String = (System.getenv("TVBOX_KEYSTORE_PASS")
    ?: keystoreProps.getProperty("storePassword", ""))
val releaseKeyAlias: String = (System.getenv("TVBOX_KEY_ALIAS")
    ?: keystoreProps.getProperty("keyAlias", ""))
val releaseKeyPassword: String = (System.getenv("TVBOX_KEY_PASS")
    ?: keystoreProps.getProperty("keyPassword", ""))

android {
    namespace = "com.simple.tvboxmobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.simple.tvboxmobile"
        minSdk = 24     // Compose 友好底线（API 21 太老，依赖库支持更窄）
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.5"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile.isNotBlank() && rootProject.file(releaseStoreFile).exists()) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseStoreFile.isNotBlank() && rootProject.file(releaseStoreFile).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // AndroidX core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Compose BOM 控制整个 compose 生态版本
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material 组件（少数非 compose 的仍可能用到）
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 协程 + 网络
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Image loading (Coil for Compose)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // 播放器（Media3 ExoPlayer）
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // Window size class 适配手机/平板
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.window:window:1.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
