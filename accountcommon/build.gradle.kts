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

import versions.myJavaVersion

plugins {
    kotlin("jvm")
    `java-library`
    mpconsumer
}

description = "A library that abstracts away the access to the account database through a nicer api"

dependencies {
    api(libs.kotlinsql.monadic)

    implementation(project(":darwin-sql"))
    implementation(libs.xmlutil.core)

    testImplementation(libs.junit5.api)

    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.mariadbConnector)
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

kotlin {
    explicitApiWarning()
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.kotlin.classTarget.get()
            }
        }
    }
    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.languageVersion.get()
            apiVersion = libs.versions.kotlin.apiVersion.get()
            optIn("kotlin.RequiresOptIn")
        }
    }
}


tasks.named<Test>("test") {
    useJUnitPlatform()
}
