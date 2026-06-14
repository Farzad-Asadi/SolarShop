plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.hilt.android)   // 👈 اضافه شد
    kotlin("kapt")                      // چون Hilt هنوز از kapt استفاده می‌کنه
}

android {
    namespace = "com.example.solarShop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.solarShop"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {

        // حالت ۱: سرور لوکال فقط برای امولاتور
        debug {
            buildConfigField("boolean", "USE_MOCK", "false")
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
        }

        // حالت ۲: سرور لوکال برای گوشی فیزیکی و امولاتور روی شبکه
        create("localDeviceDebug") {
            initWith(getByName("debug"))

            buildConfigField("boolean", "USE_MOCK", "false")
            buildConfigField("String", "BASE_URL", "\"http://192.168.43.94:8080\"")

            matchingFallbacks += listOf("debug")
        }

        // حالت ۳: سرور VPS
        create("onlineDebug") {
            initWith(getByName("debug"))

            buildConfigField("boolean", "USE_MOCK", "false")
            buildConfigField("String", "BASE_URL", "\"http://185.204.197.217:8080\"")

            matchingFallbacks += listOf("debug")
        }

        release {
            buildConfigField("boolean", "USE_MOCK", "false")
            buildConfigField("String", "BASE_URL", "\"http://185.204.197.217:8080\"")
            isMinifyEnabled = false
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// تضمین اینکه javapoet قدیمی در پردازشگرها استفاده نشود
configurations.matching {
    it.name.contains("kapt", ignoreCase = true) ||
            it.name.contains("ksp", ignoreCase = true) ||
            it.name.contains("annotationProcessor", ignoreCase = true)
}.configureEach {
    resolutionStrategy.force("com.squareup:javapoet:1.13.0")
}
configurations.all {
    resolutionStrategy.force(
        "org.jetbrains.kotlin:kotlin-stdlib:1.9.24",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.24",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24",
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3"
    )
}


dependencies {
    // --- Compose BOM و هسته‌ها ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.foundation)


    // --- AppCompat/Core (برای per-app language) ---
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.core.ktx)

    // --- Navigation Compose ---
    implementation("androidx.navigation:navigation-compose:${rootProject.extra["nav_version"]}")

    // --- DataStore / Lifecycle ---
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose.android)

    // --- Ktor ---
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.mock)

    // --- JSON ---
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // --- Coil ---
    implementation(libs.coil.compose)        // 2.4.0
    implementation(libs.coil.compose.v260)   // 2.6.0

    // --- Balloon Compose ---
    implementation("com.github.skydoves:balloon-compose:1.6.5")

    // --- سایر قبلی‌ها ---
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.material)
    implementation(libs.androidx.exifinterface)
    implementation(libs.firebase.crashlytics.buildtools)

    // --- Hilt / Room ---
    implementation(libs.hilt.android)
    implementation(libs.androidx.benchmark.traceprocessor.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // --- تست‌ها (فعلاً فقط unit test لوکال) ---
    testImplementation(libs.junit.junit)

    implementation(libs.reorderable)

    //Zelory Compressor
    implementation(libs.compressor)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

// Room testing (هم‌ورژن با Room پروژه‌ت: 2.7.2)
    androidTestImplementation(libs.androidx.room.testing)

// برای گرفتن Context در تست
    androidTestImplementation(libs.androidx.core)

// (اختیاری ولی مفید) اگر بعداً Flow تست کردی
    androidTestImplementation(libs.kotlinx.coroutines.test)


    // ❌ هیچ androidTestImplementation فعلاً نداریم
    // (همهٔ این‌ها را اگر جایی هنوز هستند، کامنت/حذف کن)
    // androidTestImplementation(libs.androidx.monitor)
    // androidTestImplementation(libs.androidx.junit.ktx)
    // androidTestImplementation(libs.junit.junit)
    // androidTestImplementation("androidx.test.ext:junit:1.2.1")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

