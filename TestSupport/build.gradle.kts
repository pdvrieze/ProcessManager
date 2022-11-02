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
    idea
}

version = "1.0.0"
description = "A library with process engine support classes"

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

kotlin {
    target.compilations.all {
        kotlinOptions.jvmTarget = libs.versions.kotlin.classTarget.get()
    }
}

configurations {
    val testRuntimeClasspath by getting {}
    val testJarConfig by creating {
        extendsFrom(testRuntimeClasspath)
    }
}

val testJar by tasks.creating(Jar::class) {
    from(sourceSets["test"].output)
    archiveClassifier.set("test")
}

artifacts {
    add("testRuntimeClasspath", testJar)
}

dependencies {
    api(project(":java-common"))
    implementation(project(":JavaCommonApi"))
    implementation(project(":ProcessEngine:core"))
    implementation(project(":PE-common"))
    implementation(project(":DarwinJavaApi"))
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenLocal()
    mavenCentral()
}
