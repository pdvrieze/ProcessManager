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

val `kotlin_version`: String by project
val kotlinVersion get() = `kotlin_version`
val androidTarget: String by project

base {
    archivesBaseName="java-common"
}


dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    api(project(":multiplatform:android"))
    api(project(":JavaCommonApi:java"))
    api(project(":java-common:java"))
}

android {
    compileSdkVersion(androidTarget.toInt())
}
