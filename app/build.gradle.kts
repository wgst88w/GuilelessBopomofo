plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    // id("kotlin-kapt")
}

android {
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    namespace = "org.ghostsinthelab.apps.guilelessbopomofo"

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "org.ghostsinthelab.apps.guilelessbopomofo"
        minSdk = 21
        targetSdk = 35
        versionCode = 180
        versionName = "3.5.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cFlags("-Wno-unused-function", "-Wno-unused-but-set-variable")
                cppFlags += listOf("")
            }
        }
        ndk {
            abiFilters.addAll(listOf("x86"))
        }
        vectorDrawables {
            useSupportLibrary = true
        }

        setProperty("archivesBaseName", "_v")
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // ✅ 移到這裡（和 buildTypes 平行）
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt") // ✅ Kotlin DSL 要用 =
            version = "3.24.0+"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    ndkVersion = "28.1.13356709"
}

// ---------- Rust & chewing 自定義 tasks ----------

val chewingLibraryPath: String = "src/main/cpp/libs/libchewing"

tasks.register<Exec>("prepareChewing") {
    workingDir(chewingLibraryPath)
    environment("RUST_TOOLCHAIN", "1.88.0")
    commandLine(
        "/usr/bin/cmake",
        "-B",
        "build/",
        "-DBUILD_INFO=false",
        "-DBUILD_TESTING=false",
        "-DWITH_SQLITE3=false",
        "-DCMAKE_BUILD_TYPE=Release"
    )
}

val chewingDataFiles = listOf("tsi.dat", "word.dat", "swkb.dat", "symbols.dat")

tasks.register<Exec>("buildChewingData") {
    dependsOn("prepareChewing")
    workingDir("$projectDir/src/main/cpp/libs/libchewing/build")
    commandLine("make", "data", "all_static_data")
}

tasks.register<Copy>("copyChewingDataFiles") {
    dependsOn("buildChewingData")
    for (chewingDataFile in chewingDataFiles) {
        from("/build/data/")
        into("/app/src/main/assets")
    }
}

tasks.register<Exec>("installRustup") {
    onlyIf {
        try {
            val result = exec {
                isIgnoreExitValue = false
                standardOutput = System.out
                errorOutput = System.err
                commandLine("rustup", "-V")
            }
            result.exitValue != 0
        } catch (e: Exception) {
            return@onlyIf false
        }
    }
    commandLine(
        "curl", "--proto", "'=https'", "--tlsv1.2", "-sSf",
        "https://sh.rustup.rs", "|", "sh", "-s", "--", "--default-toolchain", "none"
    )
}

tasks.register<Exec>("installSpecifiedRustToolchain") {
    dependsOn("installRustup")
    onlyIf {
        try {
            val result = exec {
                isIgnoreExitValue = true
                commandLine("rustup", "-V")
            }
            result.exitValue != 0
        } catch (e: Exception) {
            return@onlyIf false
        }
    }
    commandLine("rustup", "install")
}

tasks.preBuild {
    dependsOn(
        "installSpecifiedRustToolchain",
        "copyChewingDataFiles"
    )
}

tasks.register<Delete>("cleanChewingDataFiles") {
    for (chewingDataFile in chewingDataFiles) {
        file("/app/src/main/assets/").delete()
    }
}

tasks.register<Exec>("execMakeClean") {
    onlyIf { file("/build/Makefile").exists() }
    workingDir("$projectDir/src/main/cpp/libs/libchewing/build")
    commandLine("make", "clean")
    isIgnoreExitValue = true
}

tasks.register<Delete>("deleteChewingBuildDirectory") {
    onlyIf { file("/build/Makefile").exists() }
    delete("/build")
}

tasks.register<Delete>("deleteAppDotCxxDirectory") {
    delete("/app/.cxx")
}

tasks.clean {
    dependsOn(
        "cleanChewingDataFiles",
        "execMakeClean",
        "deleteChewingBuildDirectory",
        "deleteAppDotCxxDirectory"
    )
}

dependencies {
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.espresso.idling.resource)
    androidTestImplementation(libs.androidx.espresso.web)
    androidTestImplementation(libs.androidx.idling.concurrent)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.test.services)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.espresso.accessibility)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.core)
    debugImplementation(libs.leakcanary.android)
    implementation(libs.appcompat)
    implementation(libs.appcompat.resources)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.emoji2)
    implementation(libs.emoji2.bundled)
    implementation(libs.emoji2.views)
    implementation(libs.emoji2.views.helper)
    implementation(libs.eventbus)
    implementation(libs.flexbox.layout)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.preference.ktx)
    testImplementation(libs.junit)
}
