import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

/*val baseApplicationId = "com.metrolist.music"
val applicationIdOverride = System.getenv("METROLIST_APPLICATION_ID")?.takeIf { it.isNotBlank() }
val appNameOverride = System.getenv("METROLIST_APP_NAME")?.takeIf { it.isNotBlank() }
val buildCommit =
    System.getenv("METROLIST_BUILD_COMMIT")
        ?.trim()
        ?.takeIf { it.matches(Regex("[0-9a-fA-F]{7,40}")) }
        ?.take(7)
        ?.lowercase()
val debugKeystorePathOverride = System.getenv("METROLIST_DEBUG_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val debugKeystorePassword = System.getenv("METROLIST_DEBUG_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
val debugKeyAlias = System.getenv("METROLIST_DEBUG_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "androiddebugkey"
val debugKeyPassword = System.getenv("METROLIST_DEBUG_KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
val persistentDebugKeystoreFile = file("persistent-debug.keystore")
val workflowDebugKeystoreFile = debugKeystorePathOverride?.let(::file)*/

plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.metrolist.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.itsmcodez.justplayr" /*applicationIdOverride ?: baseApplicationId*/
        minSdk = 26
        targetSdk = 36
        versionCode = 11 // 152
        versionName = "2.0.0-beta" // "13.6.3"
        /*val baseVersionName = requireNotNull(versionName)
        buildConfigField("String", "BASE_VERSION_NAME", "\"$baseVersionName\"")
        buildCommit?.let { versionName = "$baseVersionName+$it" }
        resValue("string", "app_name", appNameOverride ?: "Metrolist")*/

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        // LastFM API keys from GitHub Secrets
        val lastFmKey = localProperties.getProperty("LASTFM_API_KEY") ?: System.getenv("LASTFM_API_KEY") ?: ""
        val lastFmSecret = localProperties.getProperty("LASTFM_SECRET") ?: System.getenv("LASTFM_SECRET") ?: ""

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastFmSecret\"")
        buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        buildConfigField("Long", "DISCORD_APP_ID", "1447278780795064401L")
        buildConfigField("Boolean", "CAST_AVAILABLE", "true")
        buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
    }

    /*flavorDimensions += listOf("variant")
    productFlavors {
        // FOSS - Updater, but no gcast
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        // GMS - Updater and gcast
        create("gms") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "true")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        // IzzyOnDroid - no gcast, no updater - the ONLY F-droid compliant build
        create("izzy") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "false")
        }
    }*/

    // JustPlayr Signing is handled by Android Studio
    /*signingConfigs {
        create("persistentDebug") {
            storeFile = persistentDebugKeystoreFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("workflowDebug") {
            storeFile = workflowDebugKeystoreFile ?: persistentDebugKeystoreFile
            storePassword = debugKeystorePassword
            keyAlias = debugKeyAlias
            keyPassword = debugKeyPassword
        }
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }
    }*/

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // Admob App ID
            val admobAppId = localProperties.getProperty("ADMOB_APP_ID") ?: System.getenv("ADMOB_APP_ID") ?: ""
            manifestPlaceholders["admobAppId"] = admobAppId

            // Admob Ad Unit IDs
            val admobBannerId = localProperties.getProperty("ADMOB_BANNER_ID") ?: System.getenv("ADMOB_BANNER_ID") ?: ""
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")

            val admobInterstitialId = localProperties.getProperty("ADMOB_INTERSTITIAL_ID") ?: System.getenv("ADMOB_INTERSTITIAL_ID") ?: ""
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")

            val admobRewardedInterstitialId = localProperties.getProperty("ADMOB_REWARDED_INTERSTITIAL_ID") ?: System.getenv("ADMOB_REWARDED_INTERSTITIAL_ID") ?: ""
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"$admobRewardedInterstitialId\"")

            val admobRewardedId = localProperties.getProperty("ADMOB_REWARDED_ID") ?: System.getenv("ADMOB_REWARDED_ID") ?: ""
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")

            val admobAppOpenId = localProperties.getProperty("ADMOB_APP_OPEN_ID") ?: System.getenv("ADMOB_APP_OPEN_ID") ?: ""
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$admobAppOpenId\"")

            // RevenueCat
            val revenuecatApiKey = localProperties.getProperty("REVENUECAT_API_KEY") ?: System.getenv("REVENUECAT_API_KEY") ?: ""
            buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenuecatApiKey\"")
        }
        debug {
            applicationIdSuffix = ".debug" // JustPlayr uses this suffix
            isDebuggable = true
            resValue("string", "app_name", "JustPlayr Debug") // JustPlayr debug app name
            // Google UMP
            val umpTestDeviceHashedId =
                localProperties.getProperty("UMP_TEST_DEVICE_HASHED_ID")
                    ?: System.getenv("UMP_TEST_DEVICE_HASHED_ID")
                    ?: ""
            buildConfigField("String", "UMP_TEST_DEVICE_HASHED_ID", "\"$umpTestDeviceHashedId\"")

            val umpDebugGeography =
                localProperties.getProperty("UMP_DEBUG_GEOGRAPHY")
                    ?: System.getenv("UMP_DEBUG_GEOGRAPHY")
                    ?: "DISABLED"
            buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", "\"$umpDebugGeography\"")

            // Admob Test App ID
            val testAdmobAppId = localProperties.getProperty("TEST_ADMOB_APP_ID") ?: System.getenv("TEST_ADMOB_APP_ID") ?: ""
            manifestPlaceholders["admobAppId"] = testAdmobAppId

            // Admob Test Unit Ad IDs (BuildConfig field names kept same as release config for convenience)
            val testAdmobBannerId = localProperties.getProperty("TEST_ADMOB_BANNER_ID") ?: System.getenv("TEST_ADMOB_BANNER_ID") ?: ""
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testAdmobBannerId\"")

            val testAdmobInterstitialId = localProperties.getProperty("TEST_ADMOB_INTERSTITIAL_ID") ?: System.getenv("TEST_ADMOB_INTERSTITIAL_ID") ?: ""
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testAdmobInterstitialId\"")

            val testAdmobRewardedInterstitialId = localProperties.getProperty("TEST_ADMOB_REWARDED_INTERSTITIAL_ID") ?: System.getenv("TEST_ADMOB_REWARDED_INTERSTITIAL_ID") ?: ""
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"$testAdmobRewardedInterstitialId\"")

            val testAdmobRewardedId = localProperties.getProperty("TEST_ADMOB_REWARDED_ID") ?: System.getenv("TEST_ADMOB_REWARDED_ID") ?: ""
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$testAdmobRewardedId\"")

            val testAdmobAppOpenId = localProperties.getProperty("TEST_ADMOB_APP_OPEN_ID") ?: System.getenv("TEST_ADMOB_APP_OPEN_ID") ?: ""
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$testAdmobAppOpenId\"")

            // RevenueCat Test
            val testRevenueCatApiKey = "test_nPqhnpXCYgkqWFwZrerZYRMlAXt"
            buildConfigField("String", "REVENUECAT_API_KEY", "\"$testRevenueCatApiKey\"")

            // JustPlayr Signing is handled by Android Studio
            /*signingConfig =
                if (workflowDebugKeystoreFile != null) {
                    signingConfigs.getByName("workflowDebug")
                } else if (persistentDebugKeystoreFile.exists()) {
                    signingConfigs.getByName("persistentDebug")
                } else {
                    signingConfigs.getByName("debug")
                }*/
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
        // Lint never gated anything here (abortOnError = false), so the
        // lintVital pass that assembleRelease implicitly triggers was pure
        // build time. Run lint on demand with ./gradlew :app:lintGmsRelease.
        checkReleaseBuilds = false
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols +=
                listOf(
                    "**/libandroidx.graphics.path.so",
                    "**/libdatastore_shared_counter.so",
                )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}

val cleanLegacyProtoSources = tasks.register<Delete>("cleanLegacyProtoSources") {
    delete(layout.projectDirectory.dir("src/main/java/com/metrolist/music/listentogether/proto"))
}

tasks.named("preBuild") {
    dependsOn(cleanLegacyProtoSources)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
        )
        suppressWarnings.set(false)
    }
}

// Android provides org.json as a platform API (/apex/com.android.art/javalib/core-libart.jar).
// The standalone org.json:json artefact bundles an older Apache Harmony copy of JSONArray that
// contains an internal `myArrayList` field absent from the platform class.  Without obfuscation
// R8 inlines against this internal field; at runtime the platform class is resolved instead,
// producing a NoSuchFieldError.  Excluding the artefact globally ensures only the platform
// class is ever referenced.
configurations.configureEach {
    exclude(group = "org.json", module = "json")
}

dependencies {
    // Ads
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    // RevenueCat
    implementation(libs.revenuecat.purchases)
    implementation(libs.revenuecat.purchases.ui)

    // PostHog
    implementation("com.posthog:posthog-android:3.+")

    implementation(libs.guava)
    implementation(libs.coroutines.guava)

    implementation(libs.activity)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(libs.materialKolor)

    implementation(libs.appcompat)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.browser)

    implementation(libs.ucrop)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    // Google Cast - only included in GMS flavor (not available in F-Droid/FOSS builds)
    implementation(libs.media3.cast)
    implementation(libs.mediarouter)
    implementation(libs.cast.framework)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)

    // Protobuf for message serialization (lite version for Android)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.ktor.client.mock)
}
