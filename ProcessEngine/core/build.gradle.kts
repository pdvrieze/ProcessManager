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
import multiplatform.androidAttribute
import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versions.*

plugins {
    id("java-library")
    kotlin("jvm")
    id("idea")
}

registerAndroidAttributeForDeps()

version = "1.0.0"
description = "The core process engine, independent of deployment location."
//group = ['server', 'service' ]

val testJar = tasks.create<Jar>("testJar") {
    baseName = "${project.name}-test"
    from(sourceSets["test"].output)
}

tasks.named<Jar>("jar") {
    baseName = "${project.parent?.name}-${project.name}"
}

configurations {
    named<Configuration>("implementation") {
        attributes {
            attribute(androidAttribute, false)
        }
    }
}

artifacts {
    add("testRuntime", testJar)
}

dependencies {
    api(project(":java-common"))
    api(project(":PE-common"))

    implementation(kotlin("stdlib-jdk8"))
    api("jakarta.jws:jakarta.jws-api:$jwsApiVersion")
    api("javax.activation:javax.activation-api:$activationVersion")
    compileOnly("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")

    compileOnly(project(":JavaCommonApi"))
    compileOnly(project(":DarwinJavaApi"))

    runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")

    testImplementation(project(":PE-common"))

    testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${spek2Version}") {
        exclude(group="org.jetbrains.kotlin")
    }
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${spek2Version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

    testImplementation("org.xmlunit:xmlunit-core:2.6.0")
//    testImplementation "org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}"

    testImplementation(project(":DarwinJavaApi"))
    testImplementation(project(":TestSupport"))
    testImplementation("net.devrieze:xmlutil-serialization-jvm:$xmlutilVersion")

    testRuntime("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")

    testRuntime("org.spekframework.spek2:spek-runner-junit5:${spek2Version}") {
        exclude(group="org.junit.platform")
        exclude(group="org.jetbrains.kotlin")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs=listOf(argJvmDefault)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        includeEngines("spek2", "junit-jupiter")
        include("**/TestWorkflowPatterns**")
        include("**/TestProcessEngine**")
    }
//    include "**/FooUnitTest*"
//    include "nl/adaptivity/process/engine/FooSpek.class"
//    include "nl.adaptivity.process.engine.TestWorkflowPatterns2.class"
//    selectors {
//        classes { 'nl.adaptivity.process.engine.TestWorkflowPatterns2' }
//    }
}

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}
repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    mavenCentral()
}
