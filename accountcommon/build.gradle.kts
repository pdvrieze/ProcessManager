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
    id("org.jetbrains.kotlin.jvm")
    id("java-library")
    mpconsumer
}

description = "A library that abstracts away the access to the account database through a nicer api"

dependencies {
    api("net.devrieze:kotlinsql:$kotlinsqlVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation(project(":darwin-sql"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntimeOnly("mysql:mysql-connector-java:$mysqlConnectorVersion")
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}


tasks.named<Test>("test") {
    useJUnitPlatform()
}
