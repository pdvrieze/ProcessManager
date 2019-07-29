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

import multiplatform.androidAttribute
import multiplatform.registerAndroidAttributeForDeps
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import versions.*

plugins {
    kotlin("multiplatform")
    id("idea")
    id("net.devrieze.gradlecodegen")
    id("kotlinx-serialization")
}

base {
    archivesBaseName = "PE-common"
    version = "1.0.0"
    description = "A library with process engine support classes"
}

kotlin {
    targets {
        jvm {
            compilations.all {
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.8"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental", "-Xjvm-default=enable")
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
            attributes.attribute(androidAttribute, false)
        }
        jvm("android") {
            attributes.attribute(androidAttribute, true)
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            compilations.all {
                tasks.getByName<KotlinCompile>(compileKotlinTaskName).kotlinOptions {
                    jvmTarget = "1.6"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
        }
        js {
            compilations.all {
                tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).kotlinOptions {
                    sourceMap = true
                    suppressWarnings = false
                    verbose = true
                    metaInfo = true
                    moduleKind = "umd"
                    main = "call"
                    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":multiplatform"))
                implementation(kotlin("stdlib"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
                implementation("net.devrieze:xmlutil:$xmlutilVersion")
                implementation("net.devrieze:serialutil:$xmlutilVersion")
                api("net.devrieze:xmlutil-serialization:$xmlutilVersion")

                compileOnly(project(":JavaCommonApi"))
                implementation(project(":java-common"))

            }
        }
        val javaMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("net.devrieze:xmlutil:$xmlutilVersion")
                implementation("net.devrieze:serialutil:$xmlutilVersion")
            }
        }
        val jvmMain by getting {
            dependsOn(javaMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                api("net.devrieze:kotlinsql:$kotlinsqlVersion")
                compileOnly(project(":DarwinJavaApi"))
                compileOnly(project(":JavaCommonApi"))
                compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
                implementation("jakarta.jws:jakarta.jws-api:$jwsApiVersion")
                implementation("javax.activation:javax.activation-api:$activationVersion")
                implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")

                implementation("net.devrieze:xmlutil:$xmlutilVersion")
                implementation("net.devrieze:serialutil:$xmlutilVersion")
                implementation("net.devrieze:xmlutil-serialization:$xmlutilVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")

                implementation("org.xmlunit:xmlunit-core:2.6.0")
                implementation("org.mockito:mockito-core:2.25.0")
                implementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")

                runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")

                implementation("net.devrieze:xmlutil-jvm:$xmlutilVersion")
                implementation("net.devrieze:serialutil-jvm:$xmlutilVersion")
                implementation("net.devrieze:xmlutil-serialization-jvm:$xmlutilVersion")

//                implementation(project(":JavaCommonApi"))
                implementation(project(":DarwinJavaApi"))
                implementation(project(":TestSupport"))

            }
        }
        val androidMain by getting {
            dependsOn(javaMain)
            dependencies {
                compileOnly(project(":DarwinJavaApi"))
                compileOnly(project(":JavaCommonApi"))
                implementation(kotlin("stdlib-jdk7"))
                implementation("net.devrieze:xmlutil-android:$xmlutilVersion")
                implementation("net.devrieze:serialutil-android:$xmlutilVersion")
                implementation("net.devrieze:xmlutil-serialization-android:$xmlutilVersion")
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("net.devrieze:serialutil-js:$xmlutilVersion")
                api("net.devrieze:xmlutil-js:$xmlutilVersion")
                api("net.devrieze:xmlutil-serialization-js:$xmlutilVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version")
            }
        }
    }

}

registerAndroidAttributeForDeps()


repositories {
    jcenter()
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
}

tasks.create<Task>("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}

//test {
//    useJUnitPlatform()
//}

/*
tasks.withType(KotlinCompile) {
    kotlinOptions.freeCompilerArgs=[argJvmDefault]
}
*/

/*
dependencies {
    api project(':JavaCommonApi')
    api project(':multiplatform')
    api(project(":java-common:java"))
    api("net.devrieze:kotlinsql:$kotlinsqlVersion")

    implementation "org.jetbrains:annotations:13.0"
    api "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    testImplementation "org.junit.jupiter:junit-jupiter-api:$jupiterVersion"
    testRuntime "org.junit.jupiter:junit-jupiter-engine:$jupiterVersion"
}
*/
