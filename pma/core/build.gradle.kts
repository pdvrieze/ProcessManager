import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import versions.argJvmDefault

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
            tasks.withType<Test> {
                useJUnitPlatform()
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
                implementation(project(":ProcessEngine:testLib"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.xmlutil.xmlserializable)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }

}

tasks.register("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}
