plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
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
    workingDir("$projectDir/src/main/cpp/libs/libchewing")
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
    commandLine("cmake", "--build", ".")
}

tasks.register<Copy>("copyChewingDataFiles") {
    dependsOn("buildChewingData")
    from("$chewingLibraryPath/build/data/dict/chewing") { include("tsi.dat", "word.dat") }
    from("$chewingLibraryPath/build/data/misc") { include("swkb.dat", "symbols.dat") }
    into("$projectDir/src/main/assets")
}

tasks.preBuild {
    dependsOn("copyChewingDataFiles")
}

tasks.register<Delete>("cleanChewingDataFiles") {
    chewingDataFiles.forEach {
        file("$projectDir/src/main/assets/$it").delete()
    }
}

tasks.register<Exec>("execMakeClean") {
    onlyIf { file("$chewingLibraryPath/build/Makefile").exists() }
    workingDir("$chewingLibraryPath/build")
    commandLine("make", "clean")
    isIgnoreExitValue = true
}

tasks.register<Delete>("deleteChewingBuildDirectory") {
    onlyIf { file("$chewingLibraryPath/build").exists() }
    delete("$chewingLibraryPath/build")
}

tasks.register<Delete>("deleteAppDotCxxDirectory") {
    delete("$projectDir/.cxx")
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
