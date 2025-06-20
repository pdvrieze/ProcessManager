import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
    `java-library`
    id("mpconsumer")
}

base {
    description = "The DDL files for the darwin databases"
}

dependencies {
    api(libs.kotlinsql.core)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.mariadbConnector)
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.languageVersion.get())
        apiVersion = KotlinVersion.fromVersion(libs.versions.kotlin.apiVersion.get())
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
