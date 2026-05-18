// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("nav_version", "2.5.3")
    }
    dependencies {
        classpath("com.squareup:javapoet:1.13.0")
    }

    // 2-ب) وادار کن classpath پلاگین‌ها از همین نسخه استفاده کند
    configurations.getByName("classpath").resolutionStrategy {
        force("com.squareup:javapoet:1.13.0")
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("com.squareup:javapoet:1.13.0")
        }
    }
}

