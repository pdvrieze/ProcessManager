import multiplatform.registerAndroidAttributeForDeps

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
    kotlin("jvm")
}

description = "Doclet implementation that extracts information on web GenericEndpoints"

val myJavaVersion: JavaVersion by project
val testngVersion: String by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

registerAndroidAttributeForDeps()

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains:annotations:13.0")
    implementation(files(org.gradle.internal.jvm.Jvm.current().toolsJar))
    implementation(project(":PE-common"))
    compileOnly(project(":JavaCommonApi"))
    implementation(project(":DarwinJavaApi"))

    testImplementation("org.testng:testng:$testngVersion")
}

