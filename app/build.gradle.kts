import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DivvyUp"
            isStatic = true
        }
        iosTarget.compilations.getByName("main").defaultSourceSet.dependencies {
            // Ktor engine para iOS (requerido por Supabase)
            implementation(libs.ktor.client.darwin)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // KMP core
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.uiToolingPreview)

                // Navigation KMP — via Compose Multiplatform plugin accessor (KMP-compatible)
                implementation(libs.androidx.navigation.compose)

                // ViewModel + lifecycle KMP (2.8+ es KMP-compatible)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodel.compose)

                // Supabase BOM + módulos (KMP-compatible desde v2.x)
                implementation(project.dependencies.platform(libs.supabase.bom))
                implementation(libs.supabase.postgrest)
                implementation(libs.supabase.auth)
                implementation(libs.supabase.realtime)
                implementation(libs.supabase.storage)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                // Android-specific: Activity, Context, coroutines dispatcher
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)

                // Custom Tabs — para el flujo OAuth fallback (Google, etc.)
                implementation(libs.androidx.browser)

                // Ktor engine para Android (requerido por Supabase)
                implementation(libs.ktor.client.android)

                // Coroutines Android dispatcher
                implementation(libs.kotlinx.coroutines.android)

                // Google Sign-In nativo con Credential Manager
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.googleid)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.compose.ui.test.junit4)
            }
        }
    }
}


extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.example.divvyup"
    compileSdk = 36

    // Read Supabase credentials from local.properties
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) load(f.inputStream())
    }

    defaultConfig {
        applicationId = "com.example.divvyup"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val supabaseUrl = localProps["SUPABASE_URL"]?.toString() ?: ""
        // Extraer host del Supabase URL (ej: "xxxx.supabase.co") para el intent-filter HTTPS
        val supabaseHost = supabaseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
            .ifEmpty { "placeholder.supabase.co" }

        manifestPlaceholders["supabaseHost"] = supabaseHost

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps["SUPABASE_ANON_KEY"] ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps["GOOGLE_WEB_CLIENT_ID"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res")
            java.setSrcDirs(emptyList<File>())
            kotlin.setSrcDirs(emptyList<File>())
        }
    }
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
