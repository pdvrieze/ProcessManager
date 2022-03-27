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

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import versions.myJavaVersion

plugins {
    kotlin("jvm")
    `java-library`
    idea
    mpconsumer
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

version = "1.0.0"
description = "A project for the automatically generated service client the process engine"

//group = 'server'

val genDir = File(projectDir, "gen")
val genClasses = listOf("nl.adaptivity.process.engine.servlet.ServletProcessEngine")

configurations {
    create("codegen") {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, Usage.JAVA_RUNTIME))
        }
    }
    create("codegenClasspath") {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, Usage.JAVA_RUNTIME))
        }
    }
}

KotlinPlatformType.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)

kotlin.sourceSets.named("main") {
    kotlin.srcDir(genDir)
}

tasks {

    val generate by registering(JavaExec::class) {
        dependsOn(configurations["codegen"])
        dependsOn(configurations["codegenClasspath"])
        dependsOn(":DarwinGenerators:assemble")
        dependsOn(":ProcessEngine:servlet:classes")
        group = "build"
        description = "Generate the client sources"
        mainClass.set("nl.adaptivity.messaging.MessagingSoapClientGenerator")
        classpath = files(configurations.named("codegen"))

        doFirst {
            println()

            val cp = configurations["codegenClasspath"]

            args("-gencp")
            val cpString = cp.asPath
            args(cpString)
            logger.info("Classpath for generation: $cpString")

            args("-package", "nl.adaptivity.process.client", "-dstdir", genDir.absolutePath)
            genClasses.forEach {
                args(it)
            }
        }
    }

    val compileKotlin by existing {
        dependsOn(generate)
    }

    named<Jar>("jar") {
        archiveBaseName.set("ProcessEngineClients")
    }
}

dependencies {
    "codegen"(project(":DarwinGenerators"))
    "codegenClasspath"(project(path = ":ProcessEngine:servlet", configuration = "codeGen"))
    "codegenClasspath"(project(path = ":PE-common"))
    "codegenClasspath"(project(":JavaCommonApi"))

    "api"(project(":JavaCommonApi"))
    "api"(project(":java-common"))
    "api"(project(":PE-common"))
    "api"(project(":ProcessEngine:core"))

    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.jaxb)
    compileOnly(project(":DarwinJavaApi"))

    compileOnly(libs.servletApi)
}


idea {
    module {
        sourceDirs.remove(genDir)
        generatedSourceDirs.add(genDir)
    }
}
