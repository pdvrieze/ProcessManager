/*
 * Copyright (c) 2016.
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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versions.myJavaVersion

plugins{
    kotlin("jvm")
    idea
    mpconsumer
}

val dbcpSpec: String by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

version = "1.1.0"
description = "A tomcat realm to work with the darwin authentication system"

dependencies {
    compileOnly(libs.tomcat)
    runtimeOnly(dbcpSpec)

    implementation(project(":DarwinJavaApi"))
    implementation(project(":JavaCommonApi"))
    implementation(project(":accountcommon"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = libs.versions.kotlin.classTarget.get()
}
