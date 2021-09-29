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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import versions.*

plugins {
    id("idea")
    kotlin("multiplatform")
    id("kotlinx-serialization")
    mpconsumer
}

base {
    archivesName.set("formats-xmlschema")
    version = "1.0.0"
    description = "A simple library for serializing/deserializing xmlschema"
}

kotlin {
    targets {
        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
            attributes.attribute(androidAttribute, false)
        }
/*
        jvm("android") {
            attributes.attribute(androidAttribute, true)
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            compilations.all {
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
*/
        js(BOTH) {
            browser()
            nodejs()

            compilations.all {
                tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).kotlinOptions {
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
                optIn("kotlin.RequiresOptIn")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("io.github.pdvrieze.xmlutil:core:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serialutil:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serialization:$xmlutilVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("io.github.pdvrieze.xmlutil:core:$xmlutilVersion")
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

                implementation("org.xmlunit:xmlunit-core:2.6.0")

                implementation(kotlin("test-junit5"))

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
                runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core-js:$serializationVersion")
            }
        }
    }

}

//registerAndroidAttributeForDeps()


tasks.create<Task>("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}

//test {
//    useJUnitPlatform()
//}

/*
tasks.withType(KotlinCompile) {
    kotlinOptions.freeCompilerArgs=[argJvmDefault]
}
*/

/*
dependencies {
    api project(':JavaCommonApi')
    api project(':multiplatform')
    api(project(":java-common:java"))
    api("net.devrieze:kotlinsql:$kotlinsqlVersion")

    implementation "org.jetbrains:annotations:13.0"
    api "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    testImplementation "org.junit.jupiter:junit-jupiter-api:$jupiterVersion"
    testRuntime "org.junit.jupiter:junit-jupiter-engine:$jupiterVersion"
}
*/
