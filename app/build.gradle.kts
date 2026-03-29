plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.io.File
import java.util.Properties

fun resolveProp(
    key: String,
    project: org.gradle.api.Project,
    localProps: Properties,
    envKey: String? = null,
): String =
    (project.findProperty(key) as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: localProps.getProperty(key)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: envKey?.let { System.getenv(it)?.trim()?.takeIf { v -> v.isNotEmpty() } }
        ?: ""

val localPropsForSigning = Properties().apply {
    val localFile = project.rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val playstoreStoreFile = resolveProp(
    key = "obill.playstore.storeFile",
    project = project,
    localProps = localPropsForSigning,
    envKey = "OBILL_PLAYSTORE_STORE_FILE",
)
val playstoreStorePassword = resolveProp(
    key = "obill.playstore.storePassword",
    project = project,
    localProps = localPropsForSigning,
    envKey = "OBILL_PLAYSTORE_STORE_PASSWORD",
)
val playstoreKeyAlias = resolveProp(
    key = "obill.playstore.keyAlias",
    project = project,
    localProps = localPropsForSigning,
    envKey = "OBILL_PLAYSTORE_KEY_ALIAS",
)
val playstoreKeyPassword = resolveProp(
    key = "obill.playstore.keyPassword",
    project = project,
    localProps = localPropsForSigning,
    envKey = "OBILL_PLAYSTORE_KEY_PASSWORD",
)
val hasPlaystoreSigning =
    playstoreStoreFile.isNotBlank() &&
        playstoreStorePassword.isNotBlank() &&
        playstoreKeyAlias.isNotBlank() &&
        playstoreKeyPassword.isNotBlank()

android {
    namespace = "com.obill.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.obill.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 204
        versionName = "2.0.4"
        val obillApiBase =
            (project.findProperty("obill.api.base.url") as String?)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { if (it.endsWith("/")) it else "$it/" }
                ?: "https://sln.onesky.id/"
        val updateCheckUrl =
            (project.findProperty("obill.update.check.url") as String?)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: ""
        val localProps = Properties().apply {
            val localFile = project.rootProject.file("local.properties")
            if (localFile.exists()) {
                localFile.inputStream().use { load(it) }
            }
        }
        val updateCheckToken =
            (project.findProperty("obill.update.check.token") as String?)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: localProps.getProperty("obill.update.check.token")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                ?: ""
        buildConfigField("String", "API_BASE_URL", "\"$obillApiBase\"")
        buildConfigField("String", "UPDATE_CHECK_URL", "\"$updateCheckUrl\"")
        buildConfigField("String", "UPDATE_CHECK_TOKEN", "\"$updateCheckToken\"")
    }

    signingConfigs {
        if (hasPlaystoreSigning) {
            create("playstore") {
                storeFile = project.rootProject.file(playstoreStoreFile)
                storePassword = playstoreStorePassword
                keyAlias = playstoreKeyAlias
                keyPassword = playstoreKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("playstore") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = false
            if (hasPlaystoreSigning) {
                signingConfig = signingConfigs.getByName("playstore")
            }
            matchingFallbacks += listOf("release")
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.coil-kt:coil-compose:2.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

fun resolveAndroidSdkRoot(project: org.gradle.api.Project): File {
    System.getenv("ANDROID_HOME")?.trim()?.takeIf { it.isNotEmpty() }?.let { File(it) }?.takeIf { it.isDirectory }
        ?.let { return it }
    val local = project.rootProject.file("local.properties")
    if (local.exists()) {
        val p = Properties()
        local.inputStream().use { p.load(it) }
        p.getProperty("sdk.dir")?.let { File(it) }?.takeIf { it.isDirectory }?.let { return it }
    }
    error("Android SDK tidak ditemukan: set ANDROID_HOME atau sdk.dir di local.properties")
}

tasks.register<org.gradle.api.tasks.Exec>("installDebugAndRun") {
    dependsOn("installDebug")
    group = "install"
    description = "Install debug APK lalu buka MainActivity di emulator/perangkat"
    doFirst {
        val sdk = resolveAndroidSdkRoot(project)
        val adb = sdk.resolve("platform-tools").let { dir ->
            val name = if (System.getProperty("os.name").lowercase().contains("windows")) "adb.exe" else "adb"
            dir.resolve(name)
        }
        commandLine(
            adb.absolutePath,
            "shell", "am", "start", "-n", "com.obill.app/.MainActivity",
        )
    }
}

tasks.register("publishReleaseApkToRepo") {
    group = "distribution"
    description = "Build release APK dan salin ke folder updates untuk dipush"
    dependsOn("assembleRelease")

    doLast {
        val outputsDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val apk = outputsDir
            .listFiles()
            ?.firstOrNull { it.isFile && it.name.endsWith(".apk") }
            ?: error("APK release tidak ditemukan di ${outputsDir.absolutePath}")

        val updatesDir = rootProject.layout.projectDirectory.dir("updates").asFile
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val targetApk = updatesDir.resolve("app-release.apk")
        apk.copyTo(targetApk, overwrite = true)

        val versionText = android.defaultConfig.versionName ?: "unknown"
        val notesFile = updatesDir.resolve("release-notes.txt")
        val latestVersionPayload = if (notesFile.exists()) {
            val raw = notesFile.readText()
            val escaped = raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r\n", "\n")
                .replace("\n", "\\n")
            """
            {
              "latest_version": "$versionText",
              "force_update": true,
              "release_notes": "$escaped"
            }
            """.trimIndent()
        } else {
            versionText
        }
        updatesDir.resolve("latest-version.txt").writeText(latestVersionPayload)
        updatesDir.resolve("latest-apk-name.txt").writeText(targetApk.name)

        println("Release APK dipublish ke: ${targetApk.absolutePath}")
        println("Versi: $versionText")
    }
}

tasks.matching { it.name in setOf("bundlePlaystore", "assemblePlaystore") }.configureEach {
    doFirst {
        val missing = mutableListOf<String>()
        if (playstoreStoreFile.isBlank()) missing += "obill.playstore.storeFile"
        if (playstoreStorePassword.isBlank()) missing += "obill.playstore.storePassword"
        if (playstoreKeyAlias.isBlank()) missing += "obill.playstore.keyAlias"
        if (playstoreKeyPassword.isBlank()) missing += "obill.playstore.keyPassword"
        if (missing.isNotEmpty()) {
            error(
                "Konfigurasi signing Play Store belum lengkap. Isi properti: " +
                    missing.joinToString(", ") +
                    ". Lihat file PLAYSTORE_RELEASE_GUIDE.md",
            )
        }
    }
}

tasks.register("bundlePlaystoreRelease") {
    group = "distribution"
    description = "Build App Bundle (.aab) untuk upload Play Store"
    dependsOn("bundlePlaystore")
    doLast {
        val out = layout.buildDirectory.dir("outputs/bundle/playstore").get().asFile
        println("App Bundle Play Store tersedia di: ${out.absolutePath}")
    }
}
