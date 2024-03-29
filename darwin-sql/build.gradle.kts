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

import versions.jupiterVersion
import versions.kotlinsqlVersion
import versions.myJavaVersion
import versions.mysqlConnectorVersion

plugins {
    kotlin("jvm")
    `java-library`
    id("mpconsumer")
}

base {
    description = "The DDL files for the darwin databases"
}

java {
    targetCompatibility = myJavaVersion
}

dependencies {
    api(libs.kotlinsql.core)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.mariadbConnector)
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                val catalog = project.extensions.getByName("libs")

                jvmTarget = libs.versions.kotlin.classTarget.get()
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
