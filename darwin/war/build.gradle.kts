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

import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import versions.kotlinx_html_version
import versions.myJavaVersion
import versions.requirejs_version
import versions.tomcatVersion


plugins {
    kotlin("multiplatform")
    war
}

base {
    description = "Main darwin web interface ported from PHP/GWT"
    archivesName.set("darwinjvm")
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

registerAndroidAttributeForDeps()

kotlin {
    targets {
        jvm {
            withJava()
        }
        js(LEGACY) {
            moduleName = "darwin"
            browser {
                dceTask {
                    keep("darwin.html.onLinkClick")
                    dceOptions.devMode = true
                }

                webpackTask {

                    outputFileName = "darwin.js"
                }
            }
            binaries.executable()
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
                implementation(project(":darwin"))
                implementation("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
                compileOnly(project(":JavaCommonApi"))
                compileOnly(project(":DarwinJavaApi"))
            }
        }
        val jsMain by getting {
            dependencies {
                runtimeOnly("org.webjars:requirejs:$requirejs_version")
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
