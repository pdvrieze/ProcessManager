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
    idea
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName.set("PE-common")
}

version = "1.0.0"
description = "A library with process engine support classes"

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
/*
        jvmAndroid {
        }
*/
        js {
            browser()
            binaries
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

                implementation(project(":multiplatform"))
                implementation(kotlin("stdlib"))
                implementation(project(":java-common"))
                implementation(project(":PE-common"))
                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.xmlserializable)
                compileOnly(project(":JavaCommonApi"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
/*
        val javaMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
*/
        val jvmMain by getting {
//            dependsOn(javaMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.woodstox)
                runtimeOnly(libs.junit5.engine)
            }
        }
/*
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.junit5.engine)
            }
        }
        val androidMain by getting {
            dependsOn(javaMain)
        }
*/
/*
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                api(libs.xmlutil.core)
                api(libs.xmlutil.serialization)
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-core-js:$serializationVersion")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version")
            }
        }
*/
    }

}


tasks.register<Task>("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}
