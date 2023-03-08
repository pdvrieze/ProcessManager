/*
 * Copyright (c) 2021.
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

import versions.kotlinx_html_version
import versions.xmlutilVersion


plugins {
    kotlin("jvm")
}

base {
    description = "Support for the darwin page system in a ktor context"
    archivesName.set("darwinktorsupport")
}

val ktorVersion = libs.versions.ktor.get()


kotlin {
    explicitApi()
    sourceSets {


    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.serialization.xml)


    implementation("io.github.pdvrieze.xmlutil:ktor:$xmlutilVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
    api(project(":darwin"))
}


