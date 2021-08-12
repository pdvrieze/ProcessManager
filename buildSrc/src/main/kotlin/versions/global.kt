/*
 * Copyright (c) 2019.
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

package versions

import org.gradle.api.JavaVersion
import org.gradle.api.Project

val Project.jaxbVersion: String get() = project.property("jaxbVersion") as String
val Project.kotlin_version: String get() = project.property("kotlin_version") as String
val Project.kotlin_plugin_version: String get() = project.property("kotlin_plugin_version") as String
val Project.serializationVersion: String get() = project.property("serializationVersion") as String
val Project.serializationPluginVersion: String get() = project.property("serializationPluginVersion") as String
val Project.jwsApiVersion: String get() = project.property("jwsApiVersion") as String
val Project.activationVersion: String get() = project.property("activationVersion") as String
val Project.xmlutilVersion: String get() = project.property("xmlutilVersion") as String
val Project.kotlinsqlVersion: String get() = project.property("kotlinsqlVersion") as String
val Project.androidPluginVersion: String get() = project.property("androidPluginVersion") as String
val Project.databindingVersion: String get() = project.property("databindingVersion") as String
val Project.kotlinx_html_version: String get() = project.property("kotlinx_html_version") as String
val Project.codegen_version: String get() = project.property("codegen_version") as String
val Project.junit5_version: String get() = project.property("junit5_version") as String
val Project.jupiterVersion: String get() = project.property("jupiterVersion") as String
val Project.spek2Version: String get() = project.property("spek2Version") as String
val Project.testngVersion: String get() = project.property("testngVersion") as String
val Project.tomcatPluginVersion: String get() = project.property("tomcatPluginVersion") as String
val Project.tomcatVersion: String get() = project.property("tomcatVersion") as String
val Project.mysqlConnectorVersion: String get() = project.property("mysqlConnectorVersion") as String
val Project.mariaDbConnectorVersion: String get() = project.property("mariaDbConnectorVersion") as String
val Project.androidTarget: String get() = project.property("androidTarget") as String
val Project.androidCompatVersion: String get() = project.property("androidCompatVersion") as String
val Project.androidCoroutinesVersion: String get() = project.property("androidCoroutinesVersion") as String
val Project.requirejs_version: String get() = project.property("requirejs_version") as String
val Project.easywsdlver: String get() = project.property("easywsdlver") as String

val Project.myJavaVersion: JavaVersion get() = project.property("myJavaVersion") as JavaVersion
val Project.argJvmDefault: String get() = project.property("argJvmDefault") as String
