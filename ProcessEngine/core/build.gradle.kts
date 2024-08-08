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
import versions.argJvmDefault

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    idea
}

base {
    archivesName.set("${project.parent?.name}-${project.name}")
}

version = "1.0.0"
description = "The core process engine, independent of deployment location."

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.kotlin.classTarget.get()
                freeCompilerArgs = listOf(argJvmDefault)
            }
            tasks.withType<Test> {
                useJUnitPlatform()
                systemProperty("junit.jupiter.execution.parallel.enabled", true)
            }
        }
//        attributes.attribute(androidAttribute, false)

        /*
                testRuns.create("WCP1") {
                    setExecutionSourceFrom(compilations[KotlinCompilation.MAIN_COMPILATION_NAME])
                    setExecutionSourceFrom(compilations[KotlinCompilation.TEST_COMPILATION_NAME])

                    executionTask.configure {
                        useJUnitPlatform {
                            includeEngines("junit-jupiter")
                        }

                        filter {
                            includeTestsMatching("nl.adaptivity.process.engine.patterns.WCP1.*")
                        }

                        description = "Run WCP1"

                    }
                }
    */

    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("nl.adaptivity.process.engine.ProcessInstanceStorage")
            }
        }
        val commonMain by getting {
            dependencies {
                api(project(":java-common"))
                api(project(":PE-common"))

                implementation(project(":multiplatform"))
                implementation(kotlin("stdlib"))
                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.serialization)
                implementation(libs.xmlutil.serialutil)
                compileOnly(project(":JavaCommonApi"))
                compileOnly(project(":DarwinJavaApi"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(project(":java-common"))
                api(project(":PE-common"))
                api(libs.jwsApi)
                api(libs.activationApi)
                implementation(libs.kotlinx.serialization.core)
                implementation(kotlin("stdlib-jdk8"))

                runtimeOnly(libs.woodstox)

                compileOnly(libs.jaxb.api)
                runtimeOnly(libs.jaxb.impl)
            }

        }
        val commonTest by getting {
            dependencies {
//                    implementation(libs.xmlutil.core)
//                    implementation(libs.xmlutil.serialization)
                implementation(project(":ProcessEngine:testLib"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(project(":PE-common"))
                implementation(kotlin("stdlib-jdk8"))
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.jaxb.api)
                runtimeOnly(libs.jaxb.impl)

                implementation(libs.junit5.api)

                implementation(libs.xmlunit)
//    implementation libs.servletApi

                implementation(project(":DarwinJavaApi"))
                implementation(project(":TestSupport"))
//                    implementation(libs.xmlutil.serialization)

                runtimeOnly(kotlin("reflect"))
                runtimeOnly(libs.junit5.engine)
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.kotlin.classTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.kotlin.classTarget.get())
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
//    include("**/TestWorkflowPatterns**")
//    include("**/TestProcessEngine**")
//    include("**/TestLoanOrigination**")
//    dependsOn(tasks.named("jvmSpekTest"))
}

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}

