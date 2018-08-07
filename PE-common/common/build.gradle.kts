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
    id("kotlin-platform-common")
    id("idea")
    id("kotlinx-serialization")
}

base {
    archivesBaseName = "PE-common-common"

}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8 //myJavaVersion
    targetCompatibility = JavaVersion.VERSION_1_8 //myJavaVersion
}

version = "1.0.0"
description = "A library with process engine support classes"

val kotlin_version: String by project
val serializationVersion: String by project
val xmlutilVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
    implementation("net.devrieze:xmlutil-common-nonshared:$xmlutilVersion")
    implementation("net.devrieze:xmlutil-common:$xmlutilVersion")
    implementation("net.devrieze:xmlutil-serialization-common:$xmlutilVersion")
    implementation(project(":multiplatform:common"))
    implementation(project(":multiplatform:common-nonshared"))
    compileOnly(project(":JavaCommonApi:common"))
    implementation(project(":java-common:common"))
}
