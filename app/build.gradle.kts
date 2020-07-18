/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
import java.util.Properties

plugins {
    kotlin("android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")

fun isSnapshot(): Boolean {
    return System.getenv("GITHUB_WORKFLOW") != null && System.getenv("SNAPSHOT") != null
}

android {
    /*
    if (isSnapshot()) {
        android.applicationVariants.all { variant ->
            variant.outputs.all {
                outputFileName = "aps-${variant.getFlavorName()}_${defaultConfig.versionName}.apk"
            }
        }
    }
    */

    buildFeatures.viewBinding = true

    defaultConfig {
        applicationId = "dev.msfjarvis.aps"
        versionCode = 10921
        versionName = "1.10.0-SNAPSHOT"
    }

    lintOptions {
        isAbortOnError = true
        isCheckReleaseBuilds = false
        disable("MissingTranslation", "PluralsCandidate")
    }

    packagingOptions {
        exclude(".readme")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/NOTICE.txt")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            setProguardFiles(listOf("proguard-android-optimize.txt", "proguard-rules.pro"))
            buildConfigField("boolean", "ENABLE_DEBUG_FEATURES", if (isSnapshot()) "true" else "false")
        }
        named("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_DEBUG_FEATURES", "true")
        }
    }

    if (keystorePropertiesFile.exists()) {
        val keystoreProperties = Properties()
        keystoreProperties.load(keystorePropertiesFile.inputStream())
        signingConfigs {
            register("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("release")
    }

    flavorDimensions("free")
    productFlavors {
        create("free") {
            versionNameSuffix = "-free"
        }
        create("nonFree") {
        }
    }
}

dependencies {
    compileOnly(Dependencies.AndroidX.annotation)
    implementation(Dependencies.AndroidX.activity_ktx)
    implementation(Dependencies.AndroidX.autofill)
    implementation(Dependencies.AndroidX.appcompat)
    implementation(Dependencies.AndroidX.biometric)
    implementation(Dependencies.AndroidX.constraint_layout)
    implementation(Dependencies.AndroidX.core_ktx)
    implementation(Dependencies.AndroidX.documentfile)
    implementation(Dependencies.AndroidX.fragment_ktx)
    implementation(Dependencies.AndroidX.lifecycle_common)
    implementation(Dependencies.AndroidX.lifecycle_livedata_ktx)
    implementation(Dependencies.AndroidX.lifecycle_viewmodel_ktx)
    implementation(Dependencies.AndroidX.material)
    implementation(Dependencies.AndroidX.preference)
    implementation(Dependencies.AndroidX.recycler_view)
    implementation(Dependencies.AndroidX.recycler_view_selection)
    implementation(Dependencies.AndroidX.security)
    implementation(Dependencies.AndroidX.swiperefreshlayout)

    implementation(Dependencies.Kotlin.Coroutines.android)
    implementation(Dependencies.Kotlin.Coroutines.core)
    implementation(Dependencies.Kotlin.stdlib8)

    implementation(Dependencies.FirstParty.openpgp_ktx)
    implementation(Dependencies.FirstParty.zxing_android_embedded)

    implementation(Dependencies.ThirdParty.commons_codec)
    implementation(Dependencies.ThirdParty.fastscroll)
    implementation(Dependencies.ThirdParty.jgit) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation(Dependencies.ThirdParty.jsch)
    implementation(Dependencies.ThirdParty.sshj)
    implementation(Dependencies.ThirdParty.bouncycastle)
    implementation(Dependencies.ThirdParty.plumber)
    implementation(Dependencies.ThirdParty.ssh_auth)
    implementation(Dependencies.ThirdParty.timber)
    implementation(Dependencies.ThirdParty.timberkt)

    if (isSnapshot()) {
        implementation(Dependencies.ThirdParty.leakcanary)
        implementation(Dependencies.ThirdParty.whatthestack)
    } else {
        debugImplementation(Dependencies.ThirdParty.leakcanary)
        debugImplementation(Dependencies.ThirdParty.whatthestack)
    }

    // Workaround for product flavors not resolving correctly
    "nonFreeImplementation"(Dependencies.NonFree.google_play_auth_api_phone)

    // Testing-only dependencies
    androidTestImplementation(Dependencies.Testing.junit)
    androidTestImplementation(Dependencies.Testing.kotlin_test_junit)
    androidTestImplementation(Dependencies.Testing.AndroidX.runner)
    androidTestImplementation(Dependencies.Testing.AndroidX.rules)
    androidTestImplementation(Dependencies.Testing.AndroidX.junit)
    androidTestImplementation(Dependencies.Testing.AndroidX.espresso_core)
    androidTestImplementation(Dependencies.Testing.AndroidX.espresso_intents)

    testImplementation(Dependencies.Testing.junit)
    testImplementation(Dependencies.Testing.kotlin_test_junit)
}
