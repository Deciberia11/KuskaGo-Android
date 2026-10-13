import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "pe.kuskago.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.kuskago.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Clave de la IA alternativa (Gemini). Se lee de local.properties para no subirla al repositorio:
        //   GEMINI_API_KEY=tu_clave
        //   GEMINI_MODEL=gemini-3.5-flash-lite   (opcional)
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"${localProps.getProperty("GEMINI_MODEL", "gemini-3.5-flash-lite")}\"")

        // IA local con Ollama (gratis, corre en tu PC). Valores por defecto; se pueden cambiar dentro de la app.
        //   OLLAMA_URL=http://10.0.2.2:11434   (emulador)  ·  http://IP_DE_TU_PC:11434  (celular en la misma Wi-Fi)
        //   OLLAMA_MODEL=llama3.2:3b
        buildConfigField("String", "OLLAMA_URL", "\"${localProps.getProperty("OLLAMA_URL", "http://10.0.2.2:11434")}\"")
        buildConfigField("String", "OLLAMA_MODEL", "\"${localProps.getProperty("OLLAMA_MODEL", "llama3.2:3b")}\"")
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Base de datos local SQLite con Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
