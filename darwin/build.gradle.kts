/*
 * Copyright (c) 2019.
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
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
}

base {
    archivesBaseName = "darwin"
    version = "1.1.0"
    description = "Wrapper project for the main darwin web interface. This is not really process dependent."
}

val kotlin_version: String by project
val kotlinx_html_version: String by project
val requirejs_version: String by project
val tomcatVersion: String by project
val outDir = "$buildDir/kotlin2js/main/"

lateinit var javascriptConfiguration: Configuration

configurations {
    javascriptConfiguration = create("javascript")
    create("warConfig")
}


registerAndroidAttributeForDeps()

kotlin {
    targets {
        jvm {
            compilations.all {
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.8"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
            attributes.attribute(androidAttribute, false)
        }
        js {
            compilations.all {
                tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).kotlinOptions {
//                    outputFile = outDir + "darwin.js"
                    sourceMap = true
                    suppressWarnings = false
                    verbose = true
                    metaInfo = true
                    moduleKind = "umd"
                    main = "call"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":multiplatform"))
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-common:$kotlinx_html_version")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

                compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
                compileOnly(project(":JavaCommonApi"))
                compileOnly(project(":DarwinJavaApi"))

            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinx_html_version")
            }
        }
    }

}

repositories {
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
}
