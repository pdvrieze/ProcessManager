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
import versions.*

plugins{
    kotlin("jvm")
    idea
}

base {
    version="1.0.0"
    description = "A generator for client code for services"
}

registerAndroidAttributeForDeps()

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

val mainClassName = "nl.adaptivity.messaging.MessagingSoapClientGenerator"
project.ext["mainClassName"] = mainClassName

tasks {
    named<Jar>("jar") {
        manifest {
            attributes["Main-Class"]=mainClassName
        }
    }
}

dependencies {
    implementation(project(":JavaCommonApi"))
    implementation(project(":DarwinJavaApi"))
    implementation(project(":java-common"))
    implementation(project(":PE-common"))
    implementation("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("jakarta.jws:jakarta.jws-api:$jwsApiVersion")

}
repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.target.compilations.all {
    kotlinOptions.jvmTarget = "1.8"
}
