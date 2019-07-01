import multiplatform.androidAttribute
import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

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
    id("java-library")
    id("idea")
}

val myJavaVersion: JavaVersion by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

version = "1.0.0"
description = "A project for the automatically generated service clients for the workqueue"

//group = 'server'

val genDir = File(projectDir, "gen")
val genClasses = listOf("nl.adaptivity.process.userMessageHandler.server.InternalEndpoint")
val kotlin_version: String by project
val tomcatVersion: String by project

configurations {
    create("codegen") {
        attributes {
            attribute(androidAttribute, false)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, Usage.JAVA_RUNTIME))
        }
    }
    create("codegenClasspath") {
        attributes {
            attribute(androidAttribute, false)
        }
    }
}

KotlinPlatformType.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)

sourceSets {
    named<SourceSet>("main") {
        java{
            srcDir(genDir)
        }
    }
}

tasks {

    val generate by registering(JavaExec::class) {
        dependsOn(configurations["codegen"])
        dependsOn(configurations["codegenClasspath"])
        dependsOn(":DarwinGenerators:assemble")
        dependsOn(":PEUserMessageHandler:apiClasses")
        group = "build"
        description = "Generate the client sources"

        doFirst {
            println()
            main = "nl.adaptivity.messaging.MessagingSoapClientGenerator"

            classpath = configurations["codegen"]
            val cp = configurations["codegenClasspath"]

            args("-cp")
            val cpString = cp.asPath
            args(cpString)
            logger.info("Classpath for generation: $cpString")

            args("-package", "nl.adaptivity.process.client", "-dstdir", genDir.absolutePath)
            genClasses.forEach {
                args(it)
            }
        }
    }

    val compileJava by existing {
        dependsOn(generate)
    }

    named<Jar>("jar") {
        baseName="WorkQueueClients"
    }
}

registerAndroidAttributeForDeps()

dependencies {
    "codegen"(project(":DarwinGenerators"))
    "codegenClasspath"(project(path=":PEUserMessageHandler", configuration="apiElements"))
//    "codegenClasspath"(project(":PE-common"))
//    "codegen"(project(path= ":ProcessEngine:servlet", configuration= "codeGen"))
//    compile 'generate'
    "api"(project(":JavaCommonApi"))
    "api"(project(":java-common"))
    "api"(project(":PE-common"))

    implementation(kotlin("stdlib-jdk8"))
    compileOnly(project(":DarwinJavaApi"))

    compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
//    compileOnly(project(path= ":PE-common", configuration="compileOnly"))
}


idea {
    module {
        sourceDirs.remove(genDir)
        generatedSourceDirs.add(genDir)
    }
}
