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

import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("js")
    idea
    mpconsumer
}

description = "Javascript library supporting the accountmanager funcationality."

val kotlin_version: String by project
val kotlinx_html_version: String by project
val myJavaVersion: JavaVersion by project

kotlin {
    js(KotlinJsCompilerType.LEGACY) {
        browser()
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
}

dependencies {
//    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
    implementation(project(":darwin"))
}

val outDir = "${buildDir}/kotlin2js/main/"

tasks.create<Jar>("jar") {
    archiveBaseName.set("accountmgrjs")
}


/*
idea {
    module {
//        name="accountmgrjs"
    }
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}
*/
