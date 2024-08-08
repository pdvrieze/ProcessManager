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
    alias(libs.plugins.codegen)
    alias(libs.plugins.kotlin.serialization)
    id("mpconsumer")
}

base {
    archivesName.set("PE-pma-dynamic")
    version = "1.0.0"
    description = "Dynamic PMA model"
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
                api(project(":pma:core"))
                implementation(project(":ProcessEngine:testLib"))
                implementation(project(":multiplatform"))
                implementation(project(":dynamicProcessModel"))

                implementation(libs.kotlinx.serialization.core)
                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.serialutil)
                implementation(libs.xmlutil.serialization)
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))


//                implementation(project(":java-common"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.xmlutil.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.xmlutil.xmlserializable)
                implementation(kotlin("test-junit5"))
            }
        }
        val jvmTest by getting {
            dependencies {

            }
        }
    }

}
