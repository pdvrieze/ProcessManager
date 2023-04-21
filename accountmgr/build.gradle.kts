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
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    war
    mpconsumer
}

base {
    description = "The web application that is responsible for managing and maintaining accounts for darwin"
}

configurations {
    create("javascript") {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlinx.html)
    implementation("com.sun.mail:javax.mail:1.5.5")
    implementation(libs.kotlinsql.monadic)
    implementation(libs.xmlutil.core)
    implementation(project(":accountcommon"))
    implementation(project(":darwin:servletSupport"))
    implementation(project(":darwin-sql"))

    compileOnly(libs.servletApi)
    compileOnly(project(":JavaCommonApi"))
    compileOnly(project(":DarwinJavaApi"))

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.mariadbConnector)

    "javascript"(project(":accountmgr:js"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = libs.versions.kotlin.classTarget.get()
}

tasks.named<War>("war") {
    dependsOn(configurations["javascript"])

    into("js") {
        duplicatesStrategy = DuplicatesStrategy.WARN
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
}

