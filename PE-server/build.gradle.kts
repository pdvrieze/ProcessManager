import com.bmuschko.gradle.tomcat.embedded.TomcatUser
import com.bmuschko.gradle.tomcat.tasks.TomcatRun
import com.bmuschko.gradle.tomcat.tasks.TomcatRunWar
import multiplatform.androidAttribute
import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import versions.mysqlConnectorVersion
import versions.tomcatVersion

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
    java
    id("com.bmuschko.tomcat")
    idea
    mpconsumer
}

base {
    defaultTasks("tomcatRunWar")
    version = "1.0.0"
    description = "A project for running an embedded tomcat server that works."

}

configurations {
    create("extraBootCp") {
        description="This configuration allows for assembling all the jars for the boot classpath of catalina"
        attributes {
            attribute(androidAttribute, false)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        }
    }
    val warContents by creating {
        description="The contents of the combined war file"
    }
    "tomcat" {
        attributes {
            attribute(androidAttribute, false)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        }
    }
    "runtimeClasspath" {
        attributes {
            attribute(androidAttribute, false)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        }
    }
    "testRuntimeClasspath" {
        attributes {
            attribute(androidAttribute, false)
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        }
    }
    "runtimeOnly" {
        setExtendsFrom(listOf(warContents))
    }
}

registerAndroidAttributeForDeps()

dependencies {
    tomcat("org.apache.tomcat.embed:tomcat-embed-core:${tomcatVersion}")
    tomcat("org.apache.tomcat.embed:tomcat-embed-logging-juli:${tomcatVersion}")
    tomcat("org.apache.tomcat.embed:tomcat-embed-jasper:${tomcatVersion}")
//    tomcat dbcpSpec

    "extraBootCp"("mysql:mysql-connector-java:$mysqlConnectorVersion")
    "extraBootCp"(project(":DarwinJavaApi"))
    "extraBootCp"(project(":JavaCommonApi"))
    "extraBootCp"(project(":DarwinRealm"))
    // Todo dynamically determine version needed from -api version
    "extraBootCp"("org.slf4j:slf4j-simple:1.7.16")
//    extraBootCp dbcpSpec

    tomcat("mysql:mysql-connector-java:$mysqlConnectorVersion")
    tomcat(project(":DarwinJavaApi"))
    tomcat(project(":JavaCommonApi"))
    tomcat(project(":DarwinRealm"))

    "warContents"(project(path= ":PEUserMessageHandler", configuration= "runtimeElements"))
    "warContents"(project(":DarwinServices"))
    "warContents"(project(path= ":darwin:war", configuration= "warConfig"))
    "warContents"(project(path= ":ProcessEngine:servlet"))
    "warContents"(project(":accountmgr"))
}


idea {
    module {
        scopes["PROVIDED"]!!["plus"]!!.add(configurations["tomcat"])
        scopes["TEST"]!!["plus"]!!.add(configurations["tomcat"])
        excludeDirs.add(file("catalina"))
        excludeDirs.add(file("catalina7"))
    }
}

tomcat {
    contextPath="/"
    daemon=false

    users = listOf(TomcatUser("pdvrieze", "geheim", listOf("admin")))
}

val assembleExtraBootCp by tasks.creating(Copy::class) {
    from(configurations["extraBootCp"])
    into("$buildDir/bootClasspath")
}

/*
task war(type: War, overwrite:true, dependsOn: configurations.warContents, group:'build') {
    setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)

    classpath=files()

    exclude { file -> 
        def r =	file.name.endsWith(".war")
        logger.info("Evaluating exclude on $file as $r")
        r
    }
    doFirst {
        configurations.warContents.each { File file ->
            def depPath = file.path
            if (depPath.endsWith(".war")) {
                into("/") {
                    logger.info("Extracting war ${file}")
                    from zipTree(file.absolutePath)
                    include { innerFile ->
                        def path = innerFile.path
                        def name = innerFile.name
                        def result = name.endsWith(".js") && path.startsWith("META-INF/resources") || !(path.startsWith("META-INF/") || name.endsWith("web.xml"))
                        if (result) logger.info("  Including file $innerFile")
                        result
                    }
                    exclude { f ->
                        def r = f.name.endsWith(".war")|| (f.name.endsWith(".class") && f.relativePath.segments[0]=="js")
                        if (r) logger.info("  Skipping inclusion of `$f` into archive (from: ${file}).")
                        return r
                    }

                }
/ *
            } else if (! (file.name.startsWith("gwt-user-") )){
                into("/WEB-INF/lib") {
                    logger.info("Adding library $depPath")
                    from file
                }
* /
            }
        }
    }
}
*/

//artifacts { runtime war }


tasks.named<TomcatRun>("tomcatRun") {
    webDefaultXml = file ("src/main/webapp/WEB-INF/web.xml")
    configFile = file ("src/main/webapp/META-INF/context.xml")
/*
    dependsOn: configurations.warContents
    configurations.warContents.each { File file ->
        def depPath = file.path
        if (depPath.endsWith("war")) {
            into("/") {
                logger.lifecycle("Extracting war ${file}")
                from zipTree(file.absolutePath)
                include { innerFile ->
                    def path = innerFile.path
                    def name = innerFile.name
                    def result = name.endsWith(".js") && path.startsWith("META-INF/resources") || !(path.startsWith("META-INF/") || name.endsWith("web.xml") */
/*|| (name.endsWith(".jar")&& seen.contains(name))*//*
)
                    result
                }
            }
        } else if (! (file.name.startsWith("gwt-user-") )){
            into("/WEB-INF/lib") {
                logger.lifecycle("Adding library $depPath")
                from file
            }
        }
    }
*/


    doFirst {
//        webAppClasspath=webAppClasspath.plus(project(':ProcessEngine').sourceSets.tomcat.runtimeClasspath)
        logger.lifecycle("Running Tomcat")
        configurations.tomcat.get().allDependencies.forEach({ logger.debug("Dependency: $it")})
//        sourceSets.main.compileClasspath.each({ println("CompileClasspath: $it") })
//        logger.info("");
//        tomcatClasspath.each({ logger.info("TomcatClasspath: $it")})
//        logger.info("")

        System.setProperty("nl.adaptivity.messaging.localurl", "http://localhost:8080")
//	    systemProperties 'nl.adaptivity.messaging.baseurl': 'http://localhost:8080'
    }

}

tasks.named<TomcatRunWar>("tomcatRunWar") {
    webDefaultXml = file ("src/main/webapp/WEB-INF/web.xml")
    configFile = file ("src/main/webapp/META-INF/context.xml")
}


//artifacts {
//    extraBootCp tasks.assembleExtraBootCp
//}

idea {
    module {
        excludeDirs.add(file("catalina"))
        excludeDirs.add(file("catalina7"))

    }
}
