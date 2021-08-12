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

import versions.*

plugins {
    kotlin("jvm")
    `java-library`
    mpconsumer
}

description = "A library that abstracts away the access to the account database through a nicer api"

dependencies {
    api("io.github.pdvrieze.kotlinsql:kotlinsql-monadic:$kotlinsqlVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation(project(":darwin-sql"))
    implementation("io.github.pdvrieze.xmlutil:core:$xmlutilVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntimeOnly("org.mariadb.jdbc:mariadb-java-client:$mariaDbConnectorVersion")
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
                jvmTarget = "1.8"
            }
        }
    }
    sourceSets.all {
        languageSettings.apply {
            languageVersion = "1.5"
            apiVersion = "1.5"
            useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
    }
}


tasks.named<Test>("test") {
    useJUnitPlatform()
}
