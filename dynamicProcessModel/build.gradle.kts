import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
    archivesName.set("PE-dynamic-model")
    version = "1.0.0"
    description = "A library supporting in-process process models"
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.languageVersion.get())
        apiVersion = KotlinVersion.fromVersion(libs.versions.kotlin.apiVersion.get())
    }

    targets {
        jvm {
            compilerOptions {
                jvmTarget = JvmTarget.fromTarget(libs.versions.kotlin.classTarget.get())
            }
            compilations.all {
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
        }
/*
        jvmAndroid {
            compilations.all {
                kotlinOptions {
                    jvmTarget = libs.versions.kotlin.androidClassTarget.get()
                }
            }
        }
*/
/*
        js {
            browser()
            nodejs()

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
*/
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
                implementation(project(":PE-common"))
                implementation(project(":JavaCommonApi"))
                api(project(":multiplatform"))

                implementation(libs.kotlinx.serialization.core)
                implementation(libs.xmlutil.core)
                api(libs.xmlutil.serialutil)
                api(libs.xmlutil.serialization)

//                implementation(project(":java-common"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.xmlutil.core)
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":ProcessEngine:testLib"))
            }
        }
/*
        val javaMain by creating {
            dependsOn(commonMain)
        }
        val jvmMain by getting {
            dependsOn(javaMain)
        }
*/
/*
        val jvmTest by getting {
            dependencies {

            }
        }
*/
/*
        val androidMain by getting {
            dependsOn(javaMain)
            dependencies {
            }
        }
        val androidTest by getting {
            dependencies {
                runtimeOnly(libs.kxml2)
            }
        }
*/
    }

}

tasks.register("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}
