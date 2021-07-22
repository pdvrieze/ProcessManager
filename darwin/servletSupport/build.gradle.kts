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

import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import versions.kotlinx_html_version
import versions.myJavaVersion
import versions.requirejs_version
import versions.tomcatVersion


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

registerAndroidAttributeForDeps()

kotlin {
    explicitApi()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
    api(project(":darwin"))
    implementation("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
//    compileOnly(project(":JavaCommonApi"))
//    compileOnly(project(":DarwinJavaApi"))
}
