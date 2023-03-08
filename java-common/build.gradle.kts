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

plugins {
    kotlin("multiplatform")
    mpconsumer
    kotlin("plugin.serialization")
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
                    jvmTarget = libs.versions.kotlin.classTarget.get()
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
        }
        js(BOTH) {
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
                implementation(libs.kotlinx.serialization.core)

                compileOnly(project(":JavaCommonApi"))

//                api(project(":JavaCommonApi"))
                api(project(":multiplatform"))
            }
        }
        val jvmMain by getting {
            dependencies {
//                implementation(libs.jaxb.api)
                api(libs.kotlinsql.monadic)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit5.api)
                runtimeOnly(libs.junit5.engine)
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
