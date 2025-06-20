/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

import multiplatform.androidAttribute
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versions.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-android-extensions")
}

version = "0.5.1"
description = "Android interface with process model editor and task interface"

configurations {
    create("cleanedAnnotations")

    "implementation" {
        attributes {
            attribute(androidAttribute, true)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        }
    }

    create("releaseRuntimeClasspath") {
        attributes {
            attribute(androidAttribute, true)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        }
    }

    create("debugRuntimeClasspath") {
        attributes {
            attribute(androidAttribute, true)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        }
    }

    "compile" {
        exclude(group = "org.jetbrains", module = "annotations")
        attributes {
            attribute(androidAttribute, true)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        }

    }
}

registerAndroidAttributeForDeps()

class MyPlatformRule : AttributeDisambiguationRule<KotlinPlatformType> {
    override fun execute(t: MultipleCandidatesDetails<KotlinPlatformType>) {
        if (t.candidateValues.contains(KotlinPlatformType.androidJvm)) {
            t.closestMatch(KotlinPlatformType.androidJvm)
        } else if (t.candidateValues.contains(KotlinPlatformType.jvm)) {
            t.closestMatch(KotlinPlatformType.jvm)
        }
    }

}

dependencies {
//    KotlinPlatformType.setupAttributesMatchingStrategy(attributesSchema)
    attributesSchema.attribute(KotlinPlatformType.attribute).disambiguationRules.add(MyPlatformRule::class.java)
    implementation("org.jetbrains:annotations-java5:15.0")

    implementation(project(":PE-diagram"))
    implementation(project(":PE-common"))
    implementation(project(":darwinlib"))

    implementation(project(":JavaCommonApi"))

    implementation(libs.xmlutil.core)
    implementation(libs.xmlutil.serialization)
    implementation(project(":java-common"))
//    implementation("net.devrieze:android-coroutines-appcompat:0.7.990")

    implementation("com.android.support.constraint:constraint-layout:1.1.2")
    implementation("com.android.support:support-vector-drawable:${androidCompatVersion}")
//    kapt "com.android.databinding:compiler:$databindingVersion"

    testImplementation("junit:junit:4.12")
    testImplemntation(libs.xmlutil.testutil)
    testImplementation("xmlunit:xmlunit:1.6")
    testImplementation("net.sf.kxml:kxml2:2.3.0")

    implementation("com.android.support:appcompat-v7:${androidCompatVersion}")
    implementation("com.android.support:support-annotations:${androidCompatVersion}")
    implementation("com.android.support:design:${androidCompatVersion}")
    implementation("com.android.support:support-v4:${androidCompatVersion}")
    implementation("com.android.support:recyclerview-v7:${androidCompatVersion}")
    implementation("com.android.support:cardview-v7:${androidCompatVersion}")
    implementation("com.android.support:multidex:1.0.3")

    val nav_version = "1.0.0-alpha02"

    implementation("android.arch.navigation:navigation-fragment-ktx:$nav_version") // use -ktx for Kotlin)
    implementation("android.arch.navigation:navigation-ui-ktx:$nav_version") // use -ktx for Kotlin)

    val lifecycle_version = "1.1.1"
    // ViewModel and LiveData
    implementation("android.arch.lifecycle:extensions:$lifecycle_version")

    "kapt"("android.arch.lifecycle:compiler:$lifecycle_version")
//    "kapt"("com.android.databinding:compiler:$androidPluginVersion")

    "cleanedAnnotations"("org.jetbrains:annotations-java5:15.0")
}

ext {
    if (!rootProject.hasProperty("androidTarget")) {
        set("androidTarget", "26")
    }
    if (!rootProject.hasProperty("androidCompatVersion")) {
        set("androidCompatVersion", "26.3.1")
    }
    if (!rootProject.hasProperty("androidPluginVersion")) {
//        androidPluginVersion = '3.3.2'
        set("androidPluginVersion", "3.4.1")
    }
}

android {
    compileSdkVersion(androidTarget.toInt())

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(androidTarget.toInt())
        versionCode = 2
        versionName = version.toString()
//        manifestPlaceholders = [serverScheme: 'https', serverHost: 'darwin.bournemouth.ac.uk']
//        jackOptions {
//            enabled = true
//        }
        vectorDrawables.useSupportLibrary = true
    }

    dataBinding {
        isEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.kotlin.androidClassTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.kotlin.androidClassTarget.get())
    }

    lintOptions {
//        abortOnError=false
    }

    buildTypes {
        named("release") {
//            minifyEnabled=true
        }
        named("debug") {
//            multiDexEnabled true
//            minSdkVersion 21
            manifestPlaceholders = mapOf("serverScheme" to "http", "serverHost" to "10.0.2.2")
//            applicationIdSuffix ".debug"
//            minifyEnabled=true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-project.txt"
            )
            versionNameSuffix = "-debug"
        }
    }

    testOptions {
//        unitTests.returnDefaultValues = true
    }

    packagingOptions {
        exclude("META-INF/*.kotlin_module")
        exclude("**.kotlin_metadata")
        exclude("nl/adaptivity/xmlutil/SubstreamFilterWriter.kotlin_metadata")
    }
}


tasks.withType(KotlinCompile::class) {

    kotlinOptions.freeCompilerArgs = listOf(argJvmDefault)
}

val cleanAnnotationsJar by tasks.registering(Jar::class) {
    dependsOn(configurations["cleanedAnnotations"])
    archiveName = "annotations-cleaned.jar"
    exclude("org/jetbrains/annotations/NotNull.class")
    exclude("org/jetbrains/annotations/Nullable.class")

    doFirst {
        configurations["cleanedAnnotations"].forEach { f ->
            project.logger.debug("Found ${f} in cleanedAnnotations")
            from(zipTree(f))
        }
    }
}
