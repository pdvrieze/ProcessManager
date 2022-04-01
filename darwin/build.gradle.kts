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
    mpconsumer
}

base {
    archivesName.set("darwin")
    version = "1.1.0"
    description = "Wrapper project for the main darwin web interface. This is not really process dependent."
}

kotlin {
    targets {
        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
        js(BOTH) {
            moduleName = "darwin"
            browser()
            compilations.all {
                kotlinOptions {
                    sourceMap = true
                    suppressWarnings = false
                    verbose = true
                    metaInfo = true
                    moduleKind = "umd"
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
                implementation(libs.kotlinx.html)
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly(libs.servletApi)
                compileOnly(project(":JavaCommonApi"))
                compileOnly(project(":DarwinJavaApi"))

                implementation(kotlin("stdlib-jdk8"))
            }
        }
    }

}
