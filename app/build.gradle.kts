import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Cielo credentials come from local.properties (git-ignored) or CI environment variables, never from git.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun secret(key: String, fallback: String): String =
    localProperties.getProperty(key) ?: System.getenv(key.uppercase().replace('.', '_')) ?: fallback

val paymentCallbackHost = "payment-result"

android {
    namespace = "com.viniciuscastro.ingressos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.viniciuscastro.ingressos"
        minSdk = 25 // Cielo Smart L300 v3 and the Cielo emulator run Android 7.1
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "CIELO_CLIENT_ID", "\"${secret("cielo.clientId", "seu-client-id")}\"")
        buildConfigField("String", "CIELO_ACCESS_TOKEN", "\"${secret("cielo.accessToken", "seu-access-token")}\"")
        buildConfigField("String", "PAYMENT_CALLBACK_HOST", "\"$paymentCallbackHost\"")
        manifestPlaceholders["paymentCallbackHost"] = paymentCallbackHost
    }

    flavorDimensions += "terminal"
    productFlavors {
        // Real integration: talks to Cielo Smart (terminal or official emulator) through lio://.
        create("cielo") {
            dimension = "terminal"
            buildConfigField("String", "TERMINAL_SCHEME", "\"lio\"")
            buildConfigField("String", "PAYMENT_CALLBACK_SCHEME", "\"ingressos\"")
            manifestPlaceholders["paymentCallbackScheme"] = "ingressos"
        }
        // Same adapter and wire format, answered by the bundled terminal simulator (any device/API level).
        create("simulator") {
            dimension = "terminal"
            applicationIdSuffix = ".simulator"
            versionNameSuffix = "-simulator"
            buildConfigField("String", "TERMINAL_SCHEME", "\"lio-sim\"")
            buildConfigField("String", "PAYMENT_CALLBACK_SCHEME", "\"ingressos-sim\"")
            manifestPlaceholders["paymentCallbackScheme"] = "ingressos-sim"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":payment:cielo"))
    "simulatorImplementation"(project(":payment:simulator"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.zxing.core)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
