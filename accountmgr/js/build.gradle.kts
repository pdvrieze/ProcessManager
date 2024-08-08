/*
 * Copyright (c) 2016.
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
    id("mpconsumer")
}

base {
    archivesName = "accountmgrjs"
}

description = "Javascript library supporting the accountmanager funcationality."

val myJavaVersion: JavaVersion by project

kotlin {
    js {
        moduleName = "accountmgr"
        browser{
        }
        binaries.executable()
        compilations.all {
            kotlinOptions {
                sourceMap = true
                verbose = true
                moduleKind = "umd"
            }
        }
    }
/*
    kotlinOptions {
        outputFile = outDir + "accountmgr.js"
        sourceMap = true
        suppressWarnings = false
        verbose = true
//    kjsm = false
        moduleKind = "umd"

    }
*/
    sourceSets {
        val jsMain by getting {
            dependencies {
            //    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
                implementation(libs.kotlinx.html)
                implementation(project(":darwin"))
            }
        }
    }
}

//val outDir = "${buildDir}/kotlin2js/main/"
