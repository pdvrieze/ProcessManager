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
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

private val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
private fun Project.catalogVersion(name: String): String {
    return libs.findVersion(name).get().requiredVersion
}

val Project.jaxbVersion: String get() = catalogVersion("jaxb")
val Project.kotlin_version: String get() = catalogVersion("kotlin.compiler")
val Project.serializationVersion: String get() = catalogVersion("kotlinx.serialization")
val Project.jwsApiVersion: String get() = catalogVersion("jwsApi")
val Project.activationVersion: String get() = catalogVersion("activation")
val Project.xmlutilVersion: String get() = catalogVersion("xmlutil")
val Project.kotlinsqlVersion: String get() = catalogVersion("kotlinsql")
val Project.androidPluginVersion: String get() = catalogVersion("androidPlugin")
val Project.databindingVersion: String get() = catalogVersion("databinding")
val Project.kotlinx_html_version: String get() = catalogVersion("kotlinx.html")
val Project.junit5_version: String get() = catalogVersion("junit5")
val Project.jupiterVersion: String get() = catalogVersion("jupiter")
val Project.testngVersion: String get() = catalogVersion("testng")
val Project.tomcatPluginVersion: String get() = catalogVersion("tomcatPlugin")
val Project.tomcatVersion: String get() = catalogVersion("tomcat")
val Project.mysqlConnectorVersion: String get() = catalogVersion("mysqlConnector")
val Project.mariaDbConnectorVersion: String get() = catalogVersion("mariaDbConnector")
val Project.androidTarget: String get() = catalogVersion("androidTarget")
val Project.androidCompatVersion: String get() = catalogVersion("androidCompat")
val Project.androidCoroutinesVersion: String get() = catalogVersion("androidCoroutines")
val Project.requirejs_version: String get() = catalogVersion("requirejs")
val Project.easywsdlver: String get() = catalogVersion("easywsdl")

val Project.myJavaVersion: JavaVersion get() = project.property("myJavaVersion") as JavaVersion
val Project.argJvmDefault: String get() = project.property("argJvmDefault") as String
