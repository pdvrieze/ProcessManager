import multiplatform.androidAttribute
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
    id("kotlin")
    id("java-library")
    id("idea")
}

val myJavaVersion: JavaVersion by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

version = "1.0.0"
description = "A project for the automatically generated service clients for various services"

//group = 'server'

val genDir = File(projectDir, "gen")
val genClasses = listOf("nl.adaptivity.process.userMessageHandler.server.InternalEndpoint",
                        "nl.adaptivity.process.engine.servlet.ServletProcessEngine")
val kotlin_version: String by project
val tomcatVersion: String by project

configurations {
    create("codegen") {
        attributes {
            attribute(androidAttribute, false)
        }
    }
}

sourceSets {
    named("main") {
//        kotlin.srcDir(genDir)
    }
}

val generate = tasks.register<JavaExec>("generate") {
    dependsOn(configurations["codegen"])
    dependsOn(":DarwinGenerators:assemble")
    dependsOn(":ProcessEngine:servlet:classes")
    dependsOn(":PEUserMessageHandler:apiClasses")
    group = "build"
    description = "Generate the client sources"

    doFirst {
        println()
        main = "nl.adaptivity.messaging.MessagingSoapClientGenerator"
        val cp = configurations["codegen"]
            .plus(project(":PEUserMessageHandler").sourceSets["api"].runtimeClasspath)

        classpath = cp
        args("-cp")
        args(cp.joinToString(":"))

        args("-package", "nl.adaptivity.process.client", "-dstdir", genDir.absolutePath)
        genClasses.forEach {
            args(it)
        }
    }
}

val compileJava by tasks.existing {
    dependsOn(generate)
}

registerAndroidAttributeForDeps()

dependencies {
    "codegen"(project(":DarwinGenerators"))
    "codegen"(project(":PEUserMessageHandler"))
    "codegen"(project(path= ":ProcessEngine:servlet", configuration= "codeGen"))
//    compile 'generate'
    "api"(project(":JavaCommonApi"))
    "api"(project(":java-common"))
    "api"(project(":PE-common"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
//    compileOnly(project(path= ":PE-common", configuration="compileOnly"))
}


idea {
    module {
        sourceDirs.remove(genDir)
        generatedSourceDirs.add(genDir)
    }
}
