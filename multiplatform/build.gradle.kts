/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date
import multiplatform.androidAttribute

plugins {
    id("org.jetbrains.kotlin.multiplatform")
//    id("kotlinx-serialization")
//    id("maven-publish")
//    id("com.jfrog.bintray")
}

base {
    archivesBaseName = "multiplatform"
    version = "0.1"
}

val serializationVersion: String by project

val kotlin_version: String by project



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
        jvm("android") {
            attributes.attribute(androidAttribute, true)
            compilations.all {
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.6"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
        }
        js {
            compilations.all {
                tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).kotlinOptions {
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
                implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
            }
        }
        val javaShared by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
            }
        }
        val jvmMain by getting {
            dependsOn(javaShared)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
            }
        }
        val androidMain by getting {
            dependsOn(javaShared)
        }
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version")
            }
        }
    }

}



repositories {
    jcenter()
}
