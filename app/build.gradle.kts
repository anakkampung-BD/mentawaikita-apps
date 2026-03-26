plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.io.File
import java.util.Properties

android {
    namespace = "com.obill.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.obill.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 203
        versionName = "2.0.3"
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
        updatesDir.resolve("latest-version.txt").writeText(versionText)
        updatesDir.resolve("latest-apk-name.txt").writeText(targetApk.name)

        println("Release APK dipublish ke: ${targetApk.absolutePath}")
        println("Versi: $versionText")
    }
}
