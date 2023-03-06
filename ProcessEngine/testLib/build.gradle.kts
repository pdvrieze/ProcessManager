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

import multiplatform.jvmAndroid

plugins {
    kotlin("multiplatform")
    id("net.devrieze.gradlecodegen")
    kotlin("plugin.serialization")
    mpconsumer
}

kotlin {
    targets {
        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = libs.versions.kotlin.classTarget.get()
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
        }
        jvmAndroid {
            compilations.all {
                kotlinOptions {
                    jvmTarget = libs.versions.kotlin.androidClassTarget.get()
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
        val commonMain by getting {
            dependencies {
                api(project(":JavaCommonApi"))
                api(project(":ProcessEngine:core"))
                api(project(":TestSupport"))
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))

//                implementation(libs.kotlinx.serialization.core)
                api(libs.xmlutil.core)
//                api(libs.xmlutil.serialutil)
                api(libs.xmlutil.serialization)

//                implementation(project(":java-common"))

            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.serialization)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.serialization)
            }
        }
    }

}

tasks.register("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}
