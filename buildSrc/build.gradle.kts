/*
 * Copyright (c) 2019. 
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

import java.util.Properties
import java.io.FileInputStream

plugins {
    `kotlin-dsl`
}

run {
    val properties = Properties()
    FileInputStream(file("../gradle.properties")).use { input ->
        properties.load(input)
    }
    for (key in properties.stringPropertyNames()) {
        ext[key] = properties[key]
    }
}

val kotlin_version: String by project
val androidPluginVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
    runtimeOnly("com.android.tools.build:gradle:$androidPluginVersion")
}

//dependencies {
//    "implementationOnly"("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
//    runtimeOnly("com.android.tools.build:gradle:$androidPluginVersion")
//}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
