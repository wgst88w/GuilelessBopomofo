plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    namespace = "org.ghostsinthelab.apps.guilelessbopomofo"

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "org.ghostsinthelab.apps.guilelessbopomofo"
        minSdk = 21
        targetSdk = 36
        versionCode = 184
        versionName = "3.6.0"

        ndk {
            abiFilters += listOf("x86")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cFlags("-Wno-unused-function", "-Wno-unused-but-set-variable")
                cppFlags += listOf("")
            }
        }

        vectorDrawables {
            useSupportLibrary = true
        }

        setProperty("archivesBaseName", "${applicationId}_v${versionName}")
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
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.24.0+"
            arguments += listOf(
                "-DANDROID_ABI=x86",
                "-DANDROID_PLATFORM=android-21"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    ndkVersion = "28.1.13356709"

    val chewingLibraryPath = if (System.getenv("CI") != null) {
        "/workspace/project/app/src/main/cpp/libs/libchewing"
    } else {
        "$rootDir/app/src/main/cpp/libs/libchewing"
    }

    val chewingBuildDir = "$chewingLibraryPath/build"
    val chewingAssetsDir = if (System.getenv("CI") != null) {
        "/workspace/project/app/src/main/assets"
    } else {
        "$rootDir/app/src/main/assets"
    }

    val chewingDataFiles = listOf("tsi.dat", "word.dat", "swkb.dat", "symbols.dat")

    val prepareChewing = tasks.register<Exec>("prepareChewing") {
        workingDir(chewingLibraryPath)
        commandLine(
            "cmake",
            "-B", "build/",
            "-DBUILD_INFO=false",
            "-DBUILD_TESTING=false",
            "-DWITH_SQLITE3=false",
            "-DCMAKE_BUILD_TYPE=Release"
        )
        outputs.dir(chewingBuildDir)
        outputs.upToDateWhen { file(chewingBuildDir).exists() }
    }

    val buildChewingData = tasks.register<Exec>("buildChewingData") {
        dependsOn(prepareChewing)
        workingDir(chewingBuildDir)
        commandLine("make", "dict_chewing", "misc")
        outputs.files(chewingDataFiles.map { "$chewingAssetsDir/$it" })
        outputs.upToDateWhen {
            chewingDataFiles.all { file("$chewingAssetsDir/$it").exists() }
        }
    }

    tasks.register<Copy>("copyChewingDataFiles") {
        dependsOn(buildChewingData)
        from("$chewingBuildDir/data/dict/chewing") {
            include("tsi.dat", "word.dat")
        }
        from("$chewingBuildDir/data/misc") {
            include("swkb.dat", "symbols.dat")
        }
        into(chewingAssetsDir)
    }

    val installRustup = tasks.register<Exec>("installRustup") {
        onlyIf {
            try {
                exec { commandLine("rustup", "-V"); isIgnoreExitValue = true }.exitValue != 0
            } catch (e: Exception) { true }
        }
        commandLine("sh", "-c", "curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- --default-toolchain none")
    }

    val installSpecifiedRustToolchain = tasks.register<Exec>("installSpecifiedRustToolchain") {
        dependsOn(installRustup)
        onlyIf {
            try {
                exec { commandLine("rustup", "-V"); isIgnoreExitValue = true }.exitValue != 0
            } catch (e: Exception) { true }
        }
        commandLine("rustup", "install")
    }

    tasks.preBuild {
        dependsOn("copyChewingDataFiles")
    }

    tasks.register<Delete>("cleanChewingDataFiles") {
        chewingDataFiles.forEach {
            delete("$chewingAssetsDir/$it")
        }
    }

    tasks.register<Exec>("execMakeClean") {
        onlyIf { file("$chewingLibraryPath/build/Makefile").exists() }
        workingDir("$chewingLibraryPath/build")
        commandLine("make", "clean")
        isIgnoreExitValue = true
    }

    tasks.register<Delete>("deleteChewingBuildDirectory") {
        onlyIf { file("$chewingLibraryPath/build/Makefile").exists() }
        delete(chewingBuildDir)
    }

    tasks.register<Delete>("deleteAppDotCxxDirectory") {
        delete(if (System.getenv("CI") != null) "/workspace/project/app/.cxx" else "$rootDir/app/.cxx")
    }

    tasks.clean {
        dependsOn(
            "cleanChewingDataFiles",
            "execMakeClean",
            "deleteChewingBuildDirectory",
            "deleteAppDotCxxDirectory"
        )
    }
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
