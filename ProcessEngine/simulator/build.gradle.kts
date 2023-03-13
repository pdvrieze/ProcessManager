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
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":ProcessEngine:core"))

                implementation(libs.kotlinx.serialization.core)
                implementation(libs.xmlutil.core)
//                api(libs.xmlutil.serialutil)
                api(libs.xmlutil.serialization)
                api(project(":multiplatform"))
                api(project(":JavaCommonApi"))

//                implementation(project(":java-common"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.xmlutil.core)
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val javaMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.xmlutil.xmlserializable)
            }
        }
        val jvmMain by getting {
            dependsOn(javaMain)
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {

            }
        }
    }

}

tasks.register("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}
