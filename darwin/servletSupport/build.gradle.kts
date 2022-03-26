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
    kotlin("jvm")
}

base {
    description = "Main darwin web interface ported from PHP/GWT"
    archivesName.set("darwinservletsupport")
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.kotlinx.html)
    api(project(":darwin"))
    implementation(libs.servletApi)
}
