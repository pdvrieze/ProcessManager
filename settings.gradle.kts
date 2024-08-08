/*
 * Copyright (c) 2016.
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

pluginManagement {
     includeBuild("mpconsumer")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        google()
//        maven { url "https://plugins.gradle.org/m2/" }
//        maven { url "https://maven.pkg.jetbrains.space/public/p/compose/dev" }

    }
}

dependencyResolutionManagement {
//    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        mavenCentral()
        mavenLocal()
        google()
    }
}

rootProject.name = "ProcessManager"

include(":multiplatform")
include(":JavaCommonApi")

/*
if(file("kotlinsql").exists()) {
    includeBuild("kotlinsql") {
        dependencySubstitution {
            substitute module("io.github.pdvrieze.kotlinsql:kotlinsql-monadic:") using project(":")
        }
    }
}

if (file("xmlutil").exists()) {
    includeBuild("xmlutil") {
        dependencySubstitution {
            substitute module('io.github.pdvrieze.xmlutil:core') using project(':core')
            substitute module('io.github.pdvrieze.xmlutil:serialization') using project(':serialization')
        }
    }
}
*/

include(":java-common")
include(":java-common:jvmonly")
include(":DarwinJavaApi")

include(":PE-common")

include(":endpointDokkalet")

include(":DarwinGenerators")

include(":darwin-sql")

include(":ProcessEngine:core")
include(":ProcessEngine:testLib")
include(":ProcessEngine:servlet")
include(":ProcessEngine:simulator")

include(":TestSupport")
include(":DarwinClients:ProcessEngine")

include(":PEUserMessageHandler")
include(":DarwinClients:WorkQueue")

// No actual implementation yet
// include(":PE-dataservices")

include(":PE-diagram")

include(":accountcommon")
include(":DarwinRealm")
include(":darwin")
//include(":darwin:war")
include(":darwin:servletSupport")
include(":darwin:ktor")
include(":darwin:ktorSupport")
include(":accountmgr")
include(":accountmgr:js")

include(":DarwinServices")

include(":PE-server")

include(":dynamicProcessModel")

include(":pma:core")
include(":pma:dynamic")
include(":pma:loanorigination")
include(":pma:agfil")


/*
include(":PE-common:endpointDoclet")
*/

// Stale/broken
//include(":structurizr")

include(":jscommon")
//include(":webeditor")


/*
// Android modules
if (Boolean.parseBoolean(androidEnabledProp)) {
    if (file("android-auth").exists()) {
        include(":android-auth")
    }
    include(":darwinlib")
//    include(":PMEditor")

    include(":compose:common")
    include(":compose:jvm")
    include(":compose:android")
    include(":compose:js")

}
*/
