// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
// build.gradle.kts (корень)

// Форсим javapoet на всех конфигурациях всех проектов (и app, и корень)
allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.squareup" && requested.name == "javapoet") {
                useVersion("1.13.0")
                because("Fix NoSuchMethodError: ClassName.canonicalName() из старого javapoet")
            }
        }
    }
}

// Отдельно форсим на classpath плагинов (где работает hiltAggregateDeps)
buildscript {
    configurations.getByName("classpath").resolutionStrategy.eachDependency {
        if (requested.group == "com.squareup" && requested.name == "javapoet") {
            useVersion("1.13.0")
        }
    }
}
