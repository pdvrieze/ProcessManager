import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
    id("mpconsumer")
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName.set("java-common-jvmonly")
    version = "1.1.0"
    description = "java-common with jvm only depenendencies. support"

}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.kotlin.classTarget.get())
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.languageVersion.get())
        apiVersion = KotlinVersion.fromVersion(libs.versions.kotlin.apiVersion.get())
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
