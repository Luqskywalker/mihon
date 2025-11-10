import mihon.buildlogic.Config
import mihon.buildlogic.getBuildTime
import mihon.buildlogic.getCommitCount
import mihon.buildlogic.getGitSha

plugins {
    id("mihon.android.application")
    id("mihon.android.application.compose")
    id("com.github.zellius.shortcut-helper")
    kotlin("plugin.serialization")
    alias(libs.plugins.aboutLibraries)
}

// Conditional plugin application
if (Config.includeTelemetry) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

shortcutHelper.setFilePath("./shortcuts.xml")

android {
    namespace = "eu.kanade.tachiyomi"

    defaultConfig {
        applicationId = "app.mihon"
        versionCode = 16
        versionName = "0.19.3"

        // Build config fields
        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLastCommitTime = false)}\"")
        buildConfigField("boolean", "TELEMETRY_INCLUDED", Config.includeTelemetry.toString())
        buildConfigField("boolean", "UPDATER_ENABLED", Config.enableUpdater.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Resource configuration to reduce APK size
        resourceConfigurations += setOf("en", "xxhdpi")
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-${getCommitCount()}"
            isPseudoLocalesEnabled = true
            isTestCoverageEnabled = true
            
            // Debug-only optimizations
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }

        val release by getting {
            isMinifyEnabled = Config.enableCodeShrink
            isShrinkResources = Config.enableCodeShrink
            isCrunchPngs = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLastCommitTime = true)}\"")
        }

        // Common configuration for custom build types
        val commonMatchingFallbacks = listOf(release.name)

        register("foss") {
            initWith(release)
            applicationIdSuffix = ".foss"
            matchingFallbacks.addAll(commonMatchingFallbacks)
        }

        register("preview") {
            initWith(release)
            applicationIdSuffix = ".debug"
            versionNameSuffix = debug.versionNameSuffix
            signingConfig = debug.signingConfig
            matchingFallbacks.addAll(commonMatchingFallbacks)
            buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLastCommitTime = false)}\"")
        }

        register("benchmark") {
            initWith(release)
            isDebuggable = false
            isProfileable = true
            versionNameSuffix = "-benchmark"
            applicationIdSuffix = ".benchmark"
            signingConfig = debug.signingConfig
            matchingFallbacks.addAll(commonMatchingFallbacks)
        }
    }

    // Source set configurations
    sourceSets {
        named("preview") {
            res.srcDirs("src/debug/res")
        }
        named("benchmark") {
            res.srcDirs("src/debug/res")
            java.srcDirs("src/benchmark/java")
        }
    }

    // ABI splits for optimized APK distribution
    splits {
        abi {
            isEnable = true
            isUniversalApk = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
        density {
            isEnable = false // Disabled to reduce build time, enable if needed
        }
    }

    // Packaging options for optimized APK size
    packaging {
        jniLibs {
            keepDebugSymbols += setOf(
                "libandroidx.graphics.path",
                "libarchive-jni", 
                "libconscrypt_jni",
                "libimagedecoder",
                "libquickjs",
                "libsqlite3x",
            ).map { "**/$it.so" }
        }
        
        resources {
            // Reduced exclusions for better performance
            excludes += setOf(
                "/META-INF/**",
                "/kotlin/**",
                "**/*.properties",
                "**/*.version",
                "**/DEPENDENCIES",
                "**/LICENSE*",
                "**/NOTICE*",
                "**/README*",
                "kotlin-tooling-metadata.json",
            )
        }
    }

    // Build features configuration
    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
        
        // Disable unused features
        renderScript = false
        shaders = false
        resValues = false
    }

    // Compile options for better performance
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        
        isCoreLibraryDesugaringEnabled = true
    }

    // Kotlin options
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
        )
    }

    // Lint configuration
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        ignoreTestSources = true
        quiet = true
    }

    // Test options
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }
}

// Kotlin compiler configuration
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi", 
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=coil3.annotation.ExperimentalCoilApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.InternalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            // Performance optimizations
            "-Xstring-concat=inline",
        )
    }
}

// Dependencies configuration
dependencies {
    // Core modules
    implementation(projects.i18n)
    implementation(projects.core.archive)
    implementation(projects.core.common)
    implementation(projects.coreMetadata)
    implementation(projects.sourceApi)
    implementation(projects.sourceLocal)
    implementation(projects.data)
    implementation(projects.domain)
    implementation(projects.presentationCore)
    implementation(projects.presentationWidget)
    implementation(projects.telemetry)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(compose.activity)
    implementation(compose.foundation)
    implementation(compose.material3.core)
    implementation(compose.material.icons)
    implementation(compose.animation)
    implementation(compose.animation.graphics)
    implementation(compose.ui.tooling.preview)
    implementation(compose.ui.util)
    debugImplementation(compose.ui.tooling)

    // AndroidX
    implementation(androidx.interpolator)
    implementation(androidx.paging.runtime)
    implementation(androidx.paging.compose)
    implementation(androidx.annotation)
    implementation(androidx.appcompat)
    implementation(androidx.biometricktx)
    implementation(androidx.constraintlayout)
    implementation(androidx.corektx)
    implementation(androidx.splashscreen)
    implementation(androidx.recyclerview)
    implementation(androidx.viewpager)
    implementation(androidx.profileinstaller)
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.workmanager)

    // Kotlin
    implementation(kotlinx.reflect)
    implementation(kotlinx.immutables)
    implementation(platform(kotlinx.coroutines.bom))
    implementation(kotlinx.bundles.coroutines)

    // Database
    implementation(libs.bundles.sqlite)

    // Networking
    implementation(libs.bundles.okhttp)
    implementation(libs.okio)
    implementation(libs.conscrypt.android)

    // Serialization
    implementation(kotlinx.bundles.serialization)

    // HTML parsing
    implementation(libs.jsoup)

    // Storage
    implementation(libs.disklrucache)
    implementation(libs.unifile)

    // Preferences
    implementation(libs.preferencektx)

    // DI
    implementation(libs.injekt)

    // Image loading
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)
    implementation(libs.subsamplingscaleimageview) {
        exclude(module = "image-decoder")
    }
    implementation(libs.image.decoder)

    // UI components
    implementation(libs.material)
    implementation(libs.flexible.adapter.core)
    implementation(libs.photoview)
    implementation(libs.directionalviewpager) {
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    implementation(libs.richeditor.compose)
    implementation(libs.aboutLibraries.compose)
    implementation(libs.bundles.voyager)
    implementation(libs.compose.materialmotion)
    implementation(libs.swipe)
    implementation(libs.compose.webview)
    implementation(libs.compose.grid)
    implementation(libs.reorderable)
    implementation(libs.bundles.markdown)

    // Reactive
    implementation(libs.rxjava)

    // System integration
    implementation(libs.bundles.shizuku)

    // Utilities
    implementation(libs.stringSimilarity)
    implementation(libs.logcat)

    // Debug tools
    debugImplementation(libs.leakcanary.plumber)

    // Testing
    testImplementation(libs.bundles.test)
    testImplementation(kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Core library desugaring for newer APIs on older devices
    coreLibraryDesugaring(libs.android.desugarJdkLibs)
}

// Variant configuration
androidComponents {
    onVariants(selector().withFlavor("default" to "standard")) {
        it.packaging.resources.excludes.add("META-INF/*.version")
    }
}

// Build performance optimizations
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.memoryMaximumSize = "2g"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        allWarningsAsErrors = false
    }
}
