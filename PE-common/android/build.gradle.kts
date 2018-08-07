import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

plugins {
    id ("net.devrieze.gradlecodegen")
    id ("com.android.library")
    id ("kotlin-platform-android")
    id ("kotlin-kapt")
    id ("idea")
    id ("kotlinx-serialization")
}

version = "1.0.0"
description = "A library with process engine support classes"

base {
    archivesBaseName = "PE-common"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val kotlin_version:String by project
val serializationVersion: String by project
val argJvmDefault:String by project
val androidTarget:Int by project

android {
    compileSdkVersion(androidTarget)
//    defaultConfig {
//        applicationId 'net.devrieze.pe-common'
//    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs=listOf(argJvmDefault)
    kotlinOptions.jvmTarget = "1.8"
}

val xmlutilVersion: String by project

dependencies {
    expectedBy(project(":PE-common:common"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
    implementation("net.devrieze:xmlutil-android:$xmlutilVersion")
    implementation("net.devrieze:xmlutil-serialization-android:$xmlutilVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")

    implementation(project(":multiplatform:android"))
    implementation(project(":java-common:android"))

    implementation(kotlin("reflect", kotlin_version))

    compileOnly(project(":JavaCommonApi:jvm"))
}

