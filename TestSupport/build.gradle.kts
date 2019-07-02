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
import versions.myJavaVersion

plugins {
    kotlin("jvm")
    id("idea")
}

version = "1.0.0"
description = "A library with process engine support classes"

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

configurations {
    val testRuntime by getting {}
    val testJarConfig by creating {
        extendsFrom(testRuntime)
    }
}

val testJar by tasks.creating(Jar::class) {
    from(sourceSets["test"].output)
    classifier = "test"
}

artifacts {
    add("testRuntime", testJar)
}

registerAndroidAttributeForDeps()

dependencies {
    api(project(":java-common"))
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
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
