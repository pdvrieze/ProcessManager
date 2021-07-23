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
import versions.*

plugins {
    kotlin("multiplatform")
    mpconsumer
}

base {
    archivesName.set("darwin")
    version = "1.1.0"
    description = "Wrapper project for the main darwin web interface. This is not really process dependent."
}

kotlin {
    targets {
        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
        js(BOTH) {
            browser()
            compilations.all {
                kotlinOptions {
                    sourceMap = true
                    suppressWarnings = false
                    verbose = true
                    metaInfo = true
                    moduleKind = "umd"
                    main = "call"
                }
            }
        }
    }

    sourceSets {

        all {
            languageSettings {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":multiplatform"))
                implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
                compileOnly(project(":JavaCommonApi"))
                compileOnly(project(":DarwinJavaApi"))

                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinx_html_version")
            }
        }
    }

}
