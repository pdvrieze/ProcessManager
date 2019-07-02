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
import versions.*

plugins {
    java
    kotlin("jvm")
    war
    idea
}

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

version = "1.0.0"
description = "A container for general support services for the darwin system, including messaging"

//group = [ 'server', 'service' ]

tasks.register("tomcatRun") {
    dependsOn(tasks.named("classes"))
    group= "web application"
    description = "Do everything needed to be able to run as embedded tomcat"
}

registerAndroidAttributeForDeps()

dependencies {
    implementation(project(":PE-common"))
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")
    compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
    compileOnly(project(":JavaCommonApi"))
    compileOnly(project(":DarwinJavaApi"))
}

