import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import versions.kotlinx_html_version
import versions.requirejs_version
import versions.xmlutilVersion

/*
 * Copyright (c) 2021.
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
    kotlin("multiplatform")
    application
    kotlin("plugin.serialization")
    mpconsumer
}

base {
    description = "Main darwin web interface as ktor app"
    archivesName.set("darwinktor")
}

val serializationVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val kmongoVersion: String by project
val reactWrappersVersion: String by project


kotlin {
    jvm {}
    js {
        moduleName = "darwin"
        browser {
            dceTask {
                keep("darwin.html.onLinkClick")
                dceOptions.devMode = true
            }

            webpackTask {
                devtool = "source-map"
                outputFileName = "js/darwin.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-auth:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.github.pdvrieze.xmlutil:ktor:$xmlutilVersion")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
                implementation(project(":darwin:ktorSupport"))
                runtimeOnly("org.webjars:requirejs:$requirejs_version")

                val webpackTask = tasks.getByName<KotlinWebpack>("jsBrowserDevelopmentWebpack")
                runtimeOnly(files(webpackTask.destinationDirectory))
                compileOnly(project(":JavaCommonApi"))
                compileOnly(project(":DarwinJavaApi"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
                implementation("org.webjars:requirejs:$requirejs_version")
                api(project(":darwin"/*, configuration = "jsDefault"*/))
            }
        }

    }
}

application {
    mainClass.set("io.github.pdvrieze.darwin.ktor.MainKt")
}

val webPacks: Configuration by configurations.creating {
    description = "Exclusive configuration for webPack dependencies"
}

dependencies {
    "webPacks"("org.webjars:requirejs:$requirejs_version")
}

tasks.named<Jar>("jvmJar") {
    val taskName = if (project.hasProperty("isProduction")) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }

    val webpackTask = tasks.getByName<KotlinWebpack>(taskName)
    dependsOn(webpackTask) // make sure JS gets compiled first

    from(webPacks.map { zipTree(it) }) {
        this.include { it.path.endsWith(".js")/* && path.startsWith("META-INF/resources")*/ }
        eachFile {
            logger.debug("Webpack dependency: ${this.sourceName}")
            val i = sourcePath.lastIndexOf('/')
            if (sourcePath.startsWith("META-INF") && i > 0) {
                val myNewPath = "js/${sourcePath.substring(i + 1)}"
                logger.debug("Renaming $sourcePath to $myNewPath")
                path = myNewPath
            }
        }
    }

    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName)) {// bring output file along into the JAR
        into("js/")
    }

    duplicatesStrategy = DuplicatesStrategy.WARN
}

/*
distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}
*/

tasks.named<JavaExec>("run") {
    classpath(configurations["jvmRuntimeClasspath"])
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}
