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
    java
    kotlin("jvm")
    war
    idea
    id("mpconsumer")
}

version = "1.0.0"
description = "A container for general support services for the darwin system, including messaging"

tasks.register("tomcatRun") {
    dependsOn(tasks.named("classes"))
    group= "web application"
    description = "Do everything needed to be able to run as embedded tomcat"
}

dependencies {
    implementation(project(":PE-common"))
    implementation(libs.jaxb.api)
    runtimeOnly(libs.jaxb.impl)

    compileOnly(libs.servletApi)
    compileOnly(project(":JavaCommonApi"))
    compileOnly(project(":DarwinJavaApi"))
}

