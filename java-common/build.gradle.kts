/*
 * Copyright (c) 2018. 
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
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import versions.jaxbVersion
import versions.jupiterVersion
import versions.kotlinsqlVersion

plugins {
    kotlin("multiplatform")
    mpconsumer
}

base {
    archivesName.set("java-common")
    version = "1.1.0"
    description = "A library with generic support classes"
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
        }
        jvmAndroid()
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
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":multiplatform"))

                compileOnly(project(":JavaCommonApi"))

                api(project(":JavaCommonApi"))
                api(project(":multiplatform"))
            }
        }
        val javaMain by creating {
            dependsOn(commonMain)
        }
        val jvmMain by getting {
            dependsOn(javaMain)
            dependencies {
                implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")
                api("net.devrieze:kotlinsql:$kotlinsqlVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
            }
        }
        val androidMain by getting {
            dependsOn(javaMain)
            dependencies {
                implementation(kotlin("stdlib-jdk7"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
