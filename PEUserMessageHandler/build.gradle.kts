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
    war
    idea
    alias(libs.plugins.kotlin.serialization)
}

version = "1.0.0"
description = "The service that handles tasks for users (and functions as web interface entry point"

val argJvmDefault: String by project
val wsDestDir = file("${buildDir}/docs/wsDoc")
val genImageDir = "$projectDir/gen/generated-images"
val genResourceDir = "$projectDir/gen/genResources"

sourceSets {
    val api by creating {
        java {
            srcDir("src/api/java")
        }
    }
    val main by getting {
        java {
            srcDirs(api.allSource)
        }
        resources {
            srcDir(genResourceDir)
        }
    }
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.kotlin.classTarget.get()
            }
        }
    }
    sourceSets.all {
        languageSettings {
            optIn("kotlin.RequiresOptIn")
        }
    }
}

configurations {
    val apiImplementation by getting
    val implementation by getting {
        extendsFrom(apiImplementation)
    }
    val wsDoc by creating {
        description = "Dependencies needed to run the custom web service doclet."
    }
    val wsDocOutput by creating
}

val tomcatRun by tasks.registering {
    dependsOn("war")
    group = "web application"
    description = "Do everything needed to be able to run as embedded tomcat"
}

val apiJar by tasks.registering(Jar::class) {
    from(sourceSets["api"].output)
    archiveAppendix.set("api")
}

tasks.named<Jar>("jar") {
    dependsOn(apiJar)
    from(sourceSets["main"].output)
}

artifacts {
    add("api", apiJar)
}

tasks.named<War>("war") {
    //    dependsOn generateAll
//    classpath=sourceSets["api"].output
    from(fileTree(genImageDir))
//    dependsOn(project.task('apiCompile'))
//    from tasks.apiCompile {
//        into 'WEB-INF/classes'
//    }
}

/*
tomcat {
    contextPath='/PEUserMessageHandler'
}
*/

dependencies {
    //    apiCompileOnly libs.servletApi
    "apiCompileOnly"(project(":JavaCommonApi"))
    "apiCompileOnly"(project(":DarwinJavaApi"))
    "apiImplementation"(project(":PE-common"))
    "apiImplementation"(libs.jwsApi)
//    "apiElements"(apiJar)

    compileOnly(libs.servletApi)
    compileOnly(project(":JavaCommonApi"))
    compileOnly(sourceSets["api"].output)


    runtimeOnly(libs.woodstox)
    implementation((libs.xmlutil.serialization))
    implementation(project(":PE-common"))
    implementation(libs.jaxb.api)
    runtimeOnly(libs.jaxb.impl)


    implementation(project(":DarwinClients:ProcessEngine"))
    implementation(project(":darwin-sql"))
    implementation(libs.kotlinsql.monadic)
    compileOnly(project(":DarwinJavaApi"))


    testRuntimeOnly(project(":DarwinJavaApi"))
    testImplementation(libs.junit5.api)
//    testImplementation(libs.xmlunit)
    testImplementation(libs.xmlutil.testutil)
    testImplementation(project(":PE-common"))

    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.woodstox)
    testRuntimeOnly(libs.mariadbConnector)

/*
    wsDoc project(":PE-common:endpointDoclet")
    wsDocOutput files(wsDestDir) { builtBy 'wsDoc' }
*/
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

/*
task wsDoc(type:Javadoc) {
    dependsOn configurations.wsDoc
    dependsOn configurations.compile
    group = "documentation"
    classpath = sourceSets.main.compileClasspath.filter{f -> !f.path.contains("gwt-user")}
    source = sourceSets.main.allJava
    destinationDir = file("${buildDir}/docs/wsDoc")
    options.docletpath = configurations.wsDoc.files.asType(List)
    options.doclet = "nl.adaptivity.ws.doclet.WsDoclet"

    doFirst() {
        source=project.files(source, project(":PE-common:jvm").sourceSets.main.allJava)
    }
}

assemble.dependsOn tasks.wsDoc
*/
