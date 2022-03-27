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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versions.*

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("idea")
}

base {
    archivesName.set("${project.parent?.name}-${project.name}")
}

version = "1.0.0"
description = "The core process engine, independent of deployment location."

kotlin {
    targets {
        jvm {
            compilations.all {
                tasks.withType<KotlinCompile>/*(compileKotlinTaskName).*/  {
                    kotlinOptions {
                        jvmTarget = "1.8"
                        freeCompilerArgs = listOf(argJvmDefault)
                    }
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                    systemProperty("junit.jupiter.execution.parallel.enabled", true)
                }
            }
//            attributes.attribute(androidAttribute, false)

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
                    compileOnly(project(":JavaCommonApi"))
                    compileOnly(project(":DarwinJavaApi"))
                }
            }
            val jvmMain by getting {
                dependencies {
                    api(project(":java-common"))
                    api(project(":PE-common"))
                    api("jakarta.jws:jakarta.jws-api:$jwsApiVersion")
                    api("javax.activation:javax.activation-api:$activationVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

                    runtimeOnly("com.fasterxml.woodstox:woodstox-core:6.2.6")

                    compileOnly("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")
                }

            }
            val commonTest by getting {
                dependencies {
                    implementation("io.github.pdvrieze.xmlutil:core:$xmlutilVersion")
                    implementation("io.github.pdvrieze.xmlutil:serialization:$xmlutilVersion")
                }
            }
            val jvmTest by getting {
                dependencies {
                    implementation(project(":PE-common"))
                    implementation(kotlin("stdlib-jdk8"))
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

                    implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")

                    implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                    implementation("org.xmlunit:xmlunit-core:2.6.0")
//    implementation "org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}"

                    implementation(project(":DarwinJavaApi"))
                    implementation(project(":TestSupport"))
                    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutilVersion")

                    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
                    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
                }
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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

