import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Development-only stand-in for the Cielo Smart payment app. Packaged only in the "simulator" flavor.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.viniciuscastro.ingressos.payment.simulator"
    compileSdk = 36

    defaultConfig {
        minSdk = 25
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":payment:cielo"))
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)

    testImplementation(project(":domain"))
    testImplementation(libs.kotlin.test)
}
