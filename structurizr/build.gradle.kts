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

plugins {
    id("kotlin")
    id("application")
    id("mpconsumer")
}

description = "Structurizr architecture generation for the project"

ext {
    set("structurizrVer", "1.9.5")
    if (!hasProperty("structurizrWs1ApikeyProp")) {
        set("structurizrWs1ApikeyProp", "dummy")
    }
    if (!hasProperty("structurizrWs1ApisecretProp")) {
        set("structurizrWs1ApisecretProp", "dummy")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

val kotlin_version: String by project
val structurizrVer: String by project
val myJavaVersion: JavaVersion by project

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains:annotations:13.0")
    implementation("com.structurizr:structurizr-core:${structurizrVer}")
    implementation("com.structurizr:structurizr-client:${structurizrVer}")
    implementation(project(":darwin-sql"))
}

application {
    mainClass.set("uk.ac.bournemouth.darwin.architecture.StructurizrKt")
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

tasks.named<JavaExec>("run") {
    args ("10541", ext["structurizrWs1ApikeyProp"], ext["structurizrWs1ApisecretProp"])
    workingDir = file("${buildDir}/structurizr")
    doFirst {
        file(workingDir).mkdir()
    }
}
