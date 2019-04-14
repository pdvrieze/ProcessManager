import multiplatform.androidAttribute
import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/*
 * Copyright (c) 2017.
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
    description = "The web application that is responsible for managing and maintaining accounts for darwin"
}

configurations {
    create("javascript") {
        attributes {
            attribute(androidAttribute, false)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        }
    }
}

val kotlin_version: String by project
val kotlinx_html_version: String by project
val kotlinsqlVersion: String by project
val tomcatVersion: String by project
val jupiterVersion: String by project
val myJavaVersion: JavaVersion by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

registerAndroidAttributeForDeps()

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
    implementation("com.sun.mail:javax.mail:1.5.5")
    implementation(project(":accountcommon"))
    implementation("net.devrieze:kotlinsql:$kotlinsqlVersion")
    implementation(project(":darwin"))
    compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
    compileOnly(project(":JavaCommonApi"))
    compileOnly(project(":DarwinJavaApi"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    implementation(project(":darwin-sql"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntime("mysql:mysql-connector-java:5.1.36")

    "javascript"(project(":accountmgr:js"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<War>("war") {
    dependsOn(configurations["javascript"])

    into("js") {
        from({ configurations["javascript"].map { zipTree(it) } })
        include { fileTreeElement ->
            val path = fileTreeElement.path
            path.endsWith(".js")&& path.startsWith("META-INF/resources") || !path.startsWith("META-INF/")
        }
        exclude { f ->
            val r = f.name.endsWith(".class") || (f.name.endsWith(".kjsm") && f.path.startsWith("kotlinx.html.shared"))
            if (r) logger.info("Skipping inclusion of `${f.relativePath.pathString}` into javascript path")
            r
        }
    }
/*

    configurations["javascript"].forEach { file ->
        logger.debug("Adding javascript dependency "+file.toString())
        into("js") {
            from(zipTree(file.absolutePath))
        }
    }
*/
}

