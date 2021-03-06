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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versions.*

plugins {
    kotlin("multiplatform")
    id("java-library")
    id("kotlinx-serialization")
    id("idea")
    id("mpconsumer")
}

version = "1.0.0"
description = "The core process engine, independent of deployment location."
//group = ['server', 'service' ]

kotlin {
    targets {
        jvm {
            compilations.all {
                tasks.withType<KotlinCompile>/*(compileKotlinTaskName).*/  {
                    kotlinOptions {
                        jvmTarget = "1.8"
                        freeCompilerArgs = listOf(argJvmDefault)
//                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental", "-Xjvm-default=enable")
                    }
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
            attributes.attribute(androidAttribute, false)


            tasks.create<Test>("testWCP1") {
                useJUnitPlatform {
                    includeEngines("spek2"/*, "junit-jupiter"*/)
                }
                filter {
                    includeTestsMatching("nl.adaptivity.process.engine.patterns.WCP1.*")
                }
                val testCompilation = compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME) as KotlinJvmCompilation

                description = "Run WCP1"
                group = JavaBasePlugin.VERIFICATION_GROUP
                project.afterEvaluate {
                    // use afterEvaluate to override the JavaPlugin defaults for Test tasks
                    conventionMapping("testClassesDirs") { testCompilation.output.classesDirs }
                    conventionMapping("classpath") { testCompilation.runtimeDependencyFiles }
                }
            }

        }
/*
        jvm("android") {
            attributes.attribute(androidAttribute, true)
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            compilations.all {
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.6"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
        }
*/
        sourceSets {
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
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")

                    runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")

                    compileOnly("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")
                }

            }
            val jvmTest by getting {
                dependencies {
                    implementation(project(":PE-common"))
                    implementation(kotlin("stdlib-jdk8"))

                    implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")
                    implementation("org.spekframework.spek2:spek-dsl-jvm:${spek2Version}")
                    runtimeOnly("org.spekframework.spek2:spek-runtime-jvm:${spek2Version}")
                    implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                    implementation("org.xmlunit:xmlunit-core:2.6.0")
//    implementation "org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}"

                    implementation(project(":DarwinJavaApi"))
                    implementation(project(":TestSupport"))
                    implementation("net.devrieze:xmlutil-serialization-jvm:$xmlutilVersion")

                    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
                    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")

                    runtimeOnly("org.spekframework.spek2:spek-runner-junit5:${spek2Version}") {
                        exclude(group = "org.junit.platform")
                        exclude(group = "org.jetbrains.kotlin")
                    }
                }
            }
        }
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("${project.parent?.name}-${project.name}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        val newArgs = freeCompilerArgs.toMutableSet().apply { add("-Xuse-experimental=kotlin.Experimental")}.toList()
        freeCompilerArgs=newArgs
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform {
        includeEngines("spek2", "junit-jupiter")
    }
    include("**/TestWorkflowPatterns**")
    include("**/TestProcessEngine**")
    include("**/TestLoanOrigination**")
}

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}

