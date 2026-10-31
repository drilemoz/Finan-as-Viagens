plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "br.com.adriedson.financasdaviagem"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.adriedson.financasdaviagem"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Variáveis apenas do ambiente do build. Chaves privadas nunca são empacotadas no projeto.
    // Sem Secrets, um bundleRelease LOCAL fica sem assinatura e não deve ir à Play.
    val keyFile = System.getenv("SIGNING_KEYSTORE_FILE")
    val keyStorePassword = System.getenv("KEYSTORE_PASSWORD")
    val keyAliasName = System.getenv("KEY_ALIAS")
    val keyPasswordValue = System.getenv("KEY_PASSWORD")
    val hasReleaseSigning = listOf(keyFile, keyStorePassword, keyAliasName, keyPasswordValue)
        .all { !it.isNullOrBlank() }
    signingConfigs {
        if (hasReleaseSigning) {
            create("playUpload") {
                storeFile = file(keyFile!!)
                storePassword = keyStorePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
                storeType = "PKCS12"
            }
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("playUpload")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
