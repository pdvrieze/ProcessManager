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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    kotlin("jvm")
    id("idea")
}

val myJavaVersion: JavaVersion by project
val jaxwsVersion: String by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

version = "1.1.0"
description = "The api library for the Darwin system - Preferably this is loaded into the container classpath"

//group = ['server', 'serverclasspath']
registerAndroidAttributeForDeps()

dependencies {
    compileOnly(project(":JavaCommonApi"))
    compileOnly("org.jetbrains:annotations:13.0")
    compileOnly("com.sun.xml.ws:jaxws-ri:$jaxwsVersion")
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    jcenter()
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
