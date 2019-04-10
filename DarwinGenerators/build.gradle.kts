import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

plugins{
//    id("application")
    id("idea")
    kotlin("jvm")
}

base {
    version="1.0.0"
    description = "A generator for client code for services"
}

registerAndroidAttributeForDeps()

val myJavaVersion: JavaVersion by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}
//group = 'util'


val mainClassName = "nl.adaptivity.messaging.MessagingSoapClientGenerator"
project.ext["mainClassName"] = mainClassName

tasks {
    named<Jar>("jar") {
        manifest {
            attributes["Main-Class"]=mainClassName
        }
    }
}

val tomcatVersion: String by project
val kotlin_version: String by project

dependencies {
    implementation(project(":JavaCommonApi"))
    implementation(project(":DarwinJavaApi"))
    implementation(project(":java-common"))
    implementation(project(":PE-common"))
    implementation("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

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
