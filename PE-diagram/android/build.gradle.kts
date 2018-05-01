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

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(project(":PE-common:android"))
    implementation(project(":multiplatform:android"))
    api(project(":xmlutil:android"))
    api(project(":java-common:android"))
    expectedBy(project(":PE-diagram:common"))
}

android {
    compileSdkVersion(27)
    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(27)
        versionCode = 1
        versionName = "1.0"
    }

}
