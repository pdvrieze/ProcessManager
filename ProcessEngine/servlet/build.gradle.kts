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
    idea
    mpconsumer
}

base {
    archivesName.set("${project.parent?.name}-${project.name}")
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

dependencies {
    compileOnly(project(":JavaCommonApi"))
    compileOnly(project(":DarwinJavaApi"))
    compileOnly(libs.servletApi)
    implementation(libs.jwsApi)
    implementation(libs.activationApi)
    implementation(libs.jaxb.api)
    runtimeOnly(libs.jaxb.impl)

    api(project(":ProcessEngine:core"))
    implementation(project(":multiplatform"))

    implementation(kotlin("stdlib-jdk8"))
//    implementation("org.jetbrains:annotations:13.0")

    implementation(project(":java-common"))
    implementation(project(":PE-common"))
    implementation(libs.kotlinx.serialization.core)
    testImplementation(project(":ProcessEngine:core"))
    testImplementation(project(":JavaCommonApi"))
    testImplementation(project(":DarwinJavaApi"))
    testImplementation(project(":TestSupport"))
    testImplementation(libs.servletApi)
//    testImplementation(project(path= ":PE-common", configuration="testRuntime"))

    testImplementation(libs.junit5.api)
    testImplementation(libs.xmlunit)
    testRuntimeOnly(libs.junit5.engine)
}

/*
tasks.named<Test>("test") {
    useJUnitPlatform()
}
*/

/*
val war = tasks.named<War>("war") {
    archiveBaseName.set("${project.parent?.name}-${project.name}")
}

val jar = tasks.named<Jar>("jar"){
    archiveBaseName.set("${project.parent?.name}-${project.name}")
}
*/

/*
tasks.create<Jar>("testJar") {
    archiveBaseName.set("${project.name}-test")
    from(sourceSets["test"].output)
}
*/

artifacts {
    add("archives", tasks.named("war"))
    add("codeGen", tasks.named("jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.kotlin.classTarget.get()
                freeCompilerArgs=listOf(argJvmDefault)
            }
        }
    }
    sourceSets.all {
        languageSettings {
            optIn("kotlin.RequiresOptIn")
        }
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
