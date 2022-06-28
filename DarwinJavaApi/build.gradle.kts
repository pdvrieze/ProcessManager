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

import versions.myJavaVersion

plugins {
    `java-library`
    kotlin("jvm")
    mpconsumer
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

kotlin.target.compilations.all {
    kotlinOptions.jvmTarget = libs.versions.kotlin.classTarget.get()
}

version = "1.1.0"
description = "The api library for the Darwin system - Preferably this is loaded into the container classpath"

dependencies {
    compileOnly(project(":JavaCommonApi"))
    compileOnly(libs.jwsApi)

    compileOnly(project(":multiplatform"))
    implementation(libs.activationApi)
    implementation(kotlin("stdlib-jdk8"))
}
