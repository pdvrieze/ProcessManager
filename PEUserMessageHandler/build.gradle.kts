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
import versions.*

plugins {
    kotlin("jvm")
    war
    id("idea")
}

base {
    version = "1.0.0"
    description = "The service that handles tasks for users (and functions as web interface entry point"
//group = ['service', 'server']
}

val argJvmDefault: String by project
val wsDestDir = file("${buildDir}/docs/wsDoc")
val genImageDir = "$projectDir/gen/generated-images"
val genResourceDir = "$projectDir/gen/genResources"

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

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
//        compileClasspath.add(files(api.compileClasspath))
    }
//    imageSource {
//        output.dir(genImageDir, builtBy: 'generateImages')
//        files('images/blackSplitPanel.svg',
//                'images/arrow.svg')
//    }
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

/*
class PngGenerator extends Exec {
    @InputFile
    File inputFile

    @OutputFile
    File outputFile

    @Override
    protected void exec() {
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        setExecutable('inkscape')
        setArgs(['-e', outputFile, inputFile])
        super.exec()
    }
}

task generateSplitPanel(type: PngGenerator) {
    inputFile = file('images/blackSplitPanel.svg')
    outputFile = new File(genResourceDir, 'nl/adaptivity/process/userMessageHandler/client/blackSplitPanel.png')
    description = "Generate ${outputFile} from ${inputFile}.svg"
    group = "codegen"
}

['arrow', 'diagramBackground', 'endNode', 'startNode'].each { String file ->
    task "generate${Character.toUpperCase(file.charAt(0))}${file.substring(1)}"(type: PngGenerator) {
        description = "Generate ${file}.png from ${file}.svg"
        group = "codegen"
        inputFile = project.file("images/${file}.svg")
        outputFile = new File(genImageDir + '/images', "${file}.png")
    }

}

processResources.dependsOn(generateSplitPanel)

task generateAll {
    group 'codegen'
    dependsOn generateSplitPanel
    dependsOn project.tasks['generateArrow']
    dependsOn project.tasks['generateDiagramBackground']
    dependsOn project.tasks['generateEndNode']
    dependsOn project.tasks['generateStartNode']
}
*/
val tomcatRun by tasks.registering {
    dependsOn("war")
    group = "web application"
    description = "Do everything needed to be able to run as embedded tomcat"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(argJvmDefault)
    }
}

val apiJar by tasks.registering(Jar::class) {
    from(sourceSets["api"].output)
    appendix = "api"
}

tasks.named<Jar>("jar") {
    dependsOn(apiJar)
    from(sourceSets["main"].output)
}

artifacts {
    add("api", apiJar)
}
/*

tasks.named("buildApiElements") {
    dependsOn(apiJar)
}
*/

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

registerAndroidAttributeForDeps()

dependencies {
    //    apiCompileOnly "org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}"
    "apiCompileOnly"(project(":JavaCommonApi"))
    "apiCompileOnly"(project(":DarwinJavaApi"))
    "apiImplementation"(project(":PE-common"))
    "apiImplementation"("jakarta.jws:jakarta.jws-api:$jwsApiVersion")
//    "apiElements"(apiJar)

    compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
    compileOnly(project(":JavaCommonApi"))
    compileOnly(sourceSets["api"].output)


    runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")
    implementation(("io.github.pdvrieze.xmlutil:core:$xmlutilVersion"))
    implementation(project(":PE-common"))
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")

    implementation(project(":DarwinClients:ProcessEngine"))
    implementation(project(":darwin-sql"))
    compileOnly(project(":DarwinJavaApi"))


    testRuntimeOnly(project(":DarwinJavaApi"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testImplementation("org.xmlunit:xmlunit-core:2.6.0")
    testImplementation(project(":PE-common"))
    testRuntimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")
    testRuntimeOnly("mysql:mysql-connector-java:5.1.36")

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
