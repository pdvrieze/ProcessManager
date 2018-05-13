import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    `java-library`
    id( "kotlin-platform-jvm")
}

val myJavaVersion: JavaVersion by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

base {
    version = "1.1.0"
    description = "A library with generic support classes"
    archivesBaseName="java-common"
}

//group = 'util'

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs=listOf("-Xenable-jvm-default")
}

val kotlin_version:String by project
val jupiterVersion:String by project

dependencies {
    api(project(":JavaCommonApi:jvm"))
    api(project(":multiplatform:jvm"))
//    api(project(":kotlinsql"))
    implementation("org.jetbrains:annotations:13.0")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
    expectedBy(project(":java-common:common"))
//    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
//    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
}
