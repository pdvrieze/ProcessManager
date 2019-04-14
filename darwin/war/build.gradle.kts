import multiplatform.androidAttribute
import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

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
    kotlin("jvm")
    war
}

base {
    description = "Main darwin web interface ported from PHP/GWT"
    archivesBaseName = "darwinjvm"
}

val html_version: String by project
val kotlinx_html_version: String by project
val tomcatVersion: String by project
val requirejs_version: String by project
val myJavaVersion: JavaVersion by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

configurations {
    create("javascript") {
        attributes {
            attribute(androidAttribute, false)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        }
    }
    create("warConfig")
}

registerAndroidAttributeForDeps()

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
    implementation(project(":darwin"))
    compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
    compileOnly(project(":JavaCommonApi"))
    compileOnly(project(":DarwinJavaApi"))
    "javascript"("org.webjars:requirejs:${requirejs_version}")
    "javascript"(project(":darwin"/*, configuration = "jsDefault"*/))
//    javascript project(":accountmgr:js")
}

val war by tasks.getting(War::class) {
    dependsOn(configurations["javascript"])
    into("js") {
        from({ configurations["javascript"].map { zipTree(it) } })
        include { fileTreeElement ->
            val path = fileTreeElement.path
            path.endsWith(".js") && path.startsWith("META-INF/resources") || !path.startsWith("META-INF/")
        }
        eachFile {
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
