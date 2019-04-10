import multiplatform.registerAndroidAttributeForDeps
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
    kotlin("jvm")
    id("war")
    id("idea")
}

version = "1.0.0"
description = "The service that provides the process coordination system"
//group = ['server', 'service' ]

val wsDestDir = file("${buildDir}/docs/wsDoc")

configurations {
    create("wsDoc") {
        description= "Dependencies needed to run the custom web service doclet."

    }
    create("wsDocOutput")
    create("codeGen") {
        extendsFrom(configurations.getByName("default"))
    }
}

val argJvmDefault: String by project
val kotlin_version: String by project
val tomcatVersion: String by project
val jupiterVersion: String by project

registerAndroidAttributeForDeps()

dependencies {
    providedCompile(project(":JavaCommonApi"))
    implementation(project(":ProcessEngine:core"))
    providedCompile("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
    implementation(kotlin("stdlib-jdk8"))

    implementation(project(":multiplatform"))
    implementation("org.jetbrains:annotations:13.0")
    implementation(project(":ProcessEngine:core"))

    testCompile(project(":DarwinJavaApi"))
    testCompile(project(":TestSupport"))
//    testCompile(project(path= ":PE-common", configuration="testRuntime"))
    testCompile("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testImplementation("org.xmlunit:xmlunit-core:2.6.0")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

val war = tasks.named<War>("war") {
    baseName = "${project.parent?.name}-${project.name}"
}

val jar = tasks.named<Jar>("jar"){
    baseName = "${project.parent?.name}-${project.name}"
}

tasks.create<Jar>("testJar") {
    baseName = "${project.name}-test"
    from(sourceSets["test"].output)
}

artifacts {
    add("archives", war)
    add("codeGen", jar)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs=listOf(argJvmDefault)
    }
}

//webAppDirName = 'src/main/webapp'


/*
task wsDoc(type:Javadoc) {
    dependsOn configurations.wsDoc
    dependsOn configurations.compile
    group = "documentation"
    classpath = sourceSets.main.compileClasspath
    source = sourceSets.main.allJava
    destinationDir = wsDestDir
    options.docletpath = configurations.wsDoc.files.asType(List)
    options.doclet = "nl.adaptivity.ws.doclet.WsDoclet"

    doFirst() {
        source=project.files(source, project(":PE-common:jvm").sourceSets.main.allJava)
    }

}

assemble.dependsOn tasks.wsDoc
*/

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}
