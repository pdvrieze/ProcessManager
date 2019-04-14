import multiplatform.registerAndroidAttributeForDeps

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
plugins{
    kotlin("jvm")
    id("idea")
}

val myJavaVersion: JavaVersion by project
val dbcpSpec: String by project
val tomcatVersion: String by project

java {
    sourceCompatibility = myJavaVersion
    targetCompatibility = myJavaVersion
}

version = "1.1.0"
description = "A tomcat realm to work with the darwin authentication system"

registerAndroidAttributeForDeps()

dependencies {
    compileOnly("org.apache.tomcat:tomcat-catalina:${tomcatVersion}")
    runtime(dbcpSpec)

    compile(project(":DarwinJavaApi"))
    compile(project(":JavaCommonApi"))
    compile(project(":accountcommon"))
}