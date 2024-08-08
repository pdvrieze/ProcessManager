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

plugins{
    kotlin("jvm")
    idea
}

base {
    version="1.0.0"
    description = "A generator for client code for services"
}

val mainClassName = "nl.adaptivity.messaging.MessagingSoapClientGenerator"
project.ext["mainClassName"] = mainClassName

tasks {
    named<Jar>("jar") {
        manifest {
            attributes["Main-Class"]=mainClassName
        }
    }
}

dependencies {
    implementation(project(":JavaCommonApi"))
    implementation(project(":DarwinJavaApi"))
    implementation(project(":java-common"))
    implementation(project(":PE-common"))
    implementation(libs.servletApi)
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(libs.jwsApi)

}

kotlin.target.compilations.all {
    kotlinOptions.jvmTarget = libs.versions.kotlin.classTarget.get()
}
