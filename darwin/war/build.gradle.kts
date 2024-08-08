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

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("multiplatform")
    war
    id("mpconsumer")
}

base {
    description = "Main darwin web interface ported from PHP/GWT"
    archivesName.set("darwinjvm")
}

kotlin {
    targets {
        jvm {
            withJava()
        }
        js(IR) {
            moduleName = "darwinwar"
            browser {
/*
                dceTask {
                    keep("darwin.html.onLinkClick")
                    dceOptions.devMode = true
                }
*/

                webpackTask {
                    outputFileName = "darwinwar.js"
                }
            }
            binaries.executable()
            compilations.all {
                kotlinOptions {
                    sourceMap=true
                    moduleKind = "umd"
                }
            }
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.html)
                implementation(libs.servletApi)
                implementation(project(":darwin:servletSupport"))

                compileOnly(project(":JavaCommonApi"))
                compileOnly(project(":DarwinJavaApi"))
            }
        }
        val jsMain by getting {
            dependencies {
                runtimeOnly(libs.requirejs)
                api(project(":darwin"/*, configuration = "jsDefault"*/))
            }
        }
    }
}

configurations {
    create("javascript") {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
            attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        }
    }
    create("warConfig")
}

war {
    webAppDirName="src/jvmMain/webapp"
}

val jsBrowserDistribution by tasks.getting
val jsRuntimeConfiguration = configurations.named("jsRuntimeClasspath")

val war by tasks.getting(War::class) {
    dependsOn(jsBrowserDistribution)
    dependsOn(jsRuntimeConfiguration)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    into("js") {
        from(jsBrowserDistribution)
        from({ configurations["jsRuntimeClasspath"].map { zipTree(it) } } )
        include { fileTreeElement ->
            val path = fileTreeElement.path
            path.endsWith(".js") && path.startsWith("META-INF/resources") || !path.startsWith("META-INF/")
        }
        eachFile {
            logger.lifecycle("  - Copying: ${sourcePath}")
            val fileCopyDetails = this
            val mysrc = fileCopyDetails.sourcePath
            val i = mysrc.lastIndexOf('/')
            if (fileCopyDetails.sourcePath.startsWith("META-INF") && i > 0) {
                val myNewPath = "js/${mysrc.substring(i + 1)}"
                logger.lifecycle("Renaming ${fileCopyDetails.sourcePath} to $myNewPath")
                fileCopyDetails.path = myNewPath
            }

        }
        exclude { f ->
            val r = f.name.endsWith(".class") || (f.name.endsWith(".kjsm") && f.path.startsWith(
                "kotlinx.html.shared"))
            if (r) logger.info("Skipping inclusion of `${f.relativePath.pathString}` into javascript path")
            r
        }
    }
}

artifacts {
    add("warConfig", war)
}
