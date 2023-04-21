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
    kotlin("jvm")
    mpconsumer
    kotlin("plugin.serialization")
}

base {
    archivesName.set("java-common-jvmonly")
    version = "1.1.0"
    description = "java-common with jvm only depenendencies. support"

}

kotlin {

    target {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.kotlin.classTarget.get()
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":multiplatform"))
    implementation(libs.kotlinx.serialization.core)

    compileOnly(project(":JavaCommonApi"))

//                api(project(":JavaCommonApi"))
    api(project(":multiplatform"))
    api(project(":java-common"))

    implementation(libs.jaxb.api)
    api(libs.kotlinsql.monadic)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
