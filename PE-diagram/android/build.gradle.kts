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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    base
    id("com.android.library")
    id("kotlin-platform-android")
    id("kotlin-kapt")
}

base {
    archivesBaseName="PE-diagram"
}

val `kotlin_version`: String by project
val kotlinVersion get() = `kotlin_version`
val androidTarget: String by project
val xmlutilVersion: String by project

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    api(project(":PE-common:android"))
    implementation(project(":multiplatform:android"))
    api("net.devrieze:xmlutil-android:$xmlutilVersion")
    api(project(":java-common:android"))
    expectedBy(project(":PE-diagram:common"))
}

android {
    compileSdkVersion(androidTarget.toInt())
    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(androidTarget.toInt())
        versionCode = 1
        versionName = "1.0"
    }

}

val argJvmDefault:String by project

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs=listOf(argJvmDefault)
    kotlinOptions.jvmTarget = "1.8"
}
