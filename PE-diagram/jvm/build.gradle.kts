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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    `java-library`
    java
    id("kotlin-platform-jvm")
}

base {
    archivesBaseName="PE-diagram"
}

sourceSets {
    create("imageGen").apply {
        java.srcDir("src/imagegen/java")
    }
    Unit
}

val imageGenCompile = configurations["imageGenCompile"].apply { extendsFrom(configurations["apiElements"]) }
val imageGenRuntime = configurations["imageGenRuntime"].apply { extendsFrom(configurations["runtimeElements"]) }

val jupiterVersion: String by project
val xmlutilVersion: String by project

registerAndroidAttributeForDeps()

dependencies {
    expectedBy(project(":PE-diagram:common"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":PE-common:jvm"))
    compileOnly(project(path= ":PE-common:jvm", configuration="compileOnly"))
    imageGenCompile(project(":PE-diagram:jvm"))
    imageGenRuntime("net.devrieze:xmlutil:$xmlutilVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val argJvmDefault:String by project

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs=listOf(argJvmDefault)
    kotlinOptions.jvmTarget = "1.8"
}
