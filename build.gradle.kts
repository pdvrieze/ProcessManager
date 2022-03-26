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

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("idea")
    id("project-report")
    id("mpconsumer")
    kotlin("android") apply false
//    alias(libs.plugins.kotlin.android) apply false
    id("com.android.application") apply false
//    id("kotlin-android") apply false
}

//plugins {
//    id "net.devrieze.gradlecodegen" version "0.5.5"
//}

description = "The overall project that manages all artefacts of the processmanager"

ext {
    set("androidCompatVersion", "28.0.0-rc02")
    set("dbcpSpec", "com.zaxxer:HikariCP:2.4.5")
    set("collectDir", "${buildDir}/artifacts")

    set("myJavaVersion", JavaVersion.VERSION_1_8)
}

val androidEnabled get() = (project.ext["androidEnabledProp"] as String).toBoolean()

//def artifactType = Attribute.of("artifactType", String)

configurations {
    register("tomcatWars")
    register("tomcatClasspath") {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        }
    }
    if (androidEnabled) {
        register("androidApps") {
            attributes {
//                attribute(artifactType, "apk")
            }
        }
    }
    register("wsDoc")
}


dependencies {
/*
    tomcatWars project(path: ":ProcessEngine:servlet", configuration: "archives")
    tomcatWars project(path: ":PEUserMessageHandler", configuration: "archives")
    tomcatWars project(path: ":accountmgr", configuration: "archives")
    tomcatWars project(path: ":darwin:war", configuration: "warConfig")
    tomcatWars project(path: ":webeditor", configuration: "archives")
    tomcatWars project(path: ":DarwinServices", configuration: "archives")
*/


/*
    tomcatClasspath "org.mariadb.jdbc:mariadb-java-client:$mariaDbConnectorVersion"
    tomcatClasspath project(":DarwinJavaApi")
    tomcatClasspath project(":JavaCommonApi")
    tomcatClasspath project(":DarwinRealm")
*/

/*
    if (Boolean.valueOf(androidEnabledProp)) {
        if (file("android-auth/build.gradle").exists()) {
            "androidApps"(project(path= ":android-auth", configuration= "debugRuntimeElements"))
        }
//        androidApps project(path: ':PMEditor', configuration: 'debugRuntimeElements')
    }
*/

/*
    wsDoc project(path: "ProcessEngine:servlet", configuration: "wsDocOutput")
    wsDoc project(path: "PEUserMessageHandler", configuration: "wsDocOutput")
*/
}

//def soapTarget = file("wiki/SOAP")
//if (soapTarget.isDirectory()) {
//    task wsDoc(type: Copy) {
//        group = "documentation"
//        dependsOn configurations.wsDoc
//        destinationDir = file("wiki")
//
//        into(".") {
//            from files({ dependsOn.findAll { dep -> dep instanceof Configuration } })
////            from files({ dependsOn.findAll { dep -> dep instanceof Configuration } })
//            include "**/SOAP/*.md"
//            exclude "**/*InternalEndpointImpl.md"
//            rename { path ->
//                def file = path.substring(path.lastIndexOf('/') + 1)
//                def extPos = file.lastIndexOf('.')
//                def dotPos = file.lastIndexOf('.', extPos - 1) // we want the second last dot, the last is the extension one
//                if (dotPos >= 0) {
//                    file.substring(dotPos + 1, extPos) + "_SOAP.md"
//                } else file
//            }
//        }
//
//        into(".") {
//            from files({ dependsOn.findAll { dep -> dep instanceof Configuration } })
//            include "**/REST/*.md"
//            rename { path ->
//                def file = path.substring(path.lastIndexOf('/') + 1)
//                def dotPos = file.lastIndexOf('.', file.lastIndexOf('.') - 1) // we want the second last dot, the last is the extension one
//                def extPos = file.lastIndexOf('.')
//                if (dotPos >= 0) {
//                    file.substring(dotPos + 1, extPos) + "_REST.md"
//                } else file
//            }
//        }
//    }
//}


/*
tasks.wrapper {
    gradleVersion = "5.4.1"
}

task copyTomcatWars(type: Copy) {
    group = "dist"
    dependsOn configurations.tomcatWars
    from files { dependsOn.findAll { dep -> dep instanceof Configuration } }
    into "${collectDir}/webapps/"
}

task copyTomcatClasspath(type: Copy) {
    group = "dist"
    dependsOn configurations.tomcatClasspath
    from files { dependsOn.findAll { dep -> dep instanceof Configuration } }
    into "${collectDir}/tomcatlibs/"
    exclude { file -> file.name.contains("tomcat-servlet-api") }
}

if (Boolean.valueOf(androidEnabledProp)) {
    task copyAndroid(type: Copy) {
        group = "dist"
        dependsOn configurations.androidApps
        from files { dependsOn.findAll { dep -> dep instanceof Configuration } }
        into "${collectDir}/androidApps/"
    }
}

task dist {
    group = "dist"
    dependsOn copyTomcatWars, copyTomcatClasspath
    if (Boolean.valueOf(androidEnabledProp)) {
        dependsOn copyAndroid
    }
}
*/

/*
task run(dependsOn: [":PE-server:tomcatRun"], type: DefaultTask) {
    group = "application"
    description = "Run the server in a simple configuration"
}
*/



/*
htmlDependencyReport {
    projects = project.allprojects
}
*/

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    }
//
//    tasks.withType(KotlinCompile) {
//        kotlinOptions.jvmTarget = "1.8"
//    }
}

/*
idea {
    project {
        languageLevel = JavaVersion.VERSION_1_8
    }
    module {
        downloadSources = true
        contentRoot = projectDir
    }
}
*/
