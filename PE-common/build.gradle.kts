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
    mpconsumer
}

base {
    archivesName.set("PE-common")
    version = "1.0.0"
    description = "A library with process engine support classes"
}

kotlin {
    targets {
        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
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
                }
            }
        }
        js(BOTH) {
            browser()
            nodejs()
            compilations.all {
                tasks.getByName<KotlinJsCompile>(compileKotlinTaskName).kotlinOptions {
                    sourceMap = true
                    suppressWarnings = false
                    verbose = true
                    metaInfo = true
                    moduleKind = "umd"
                    main = "call"
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":multiplatform"))
                implementation(kotlin("stdlib"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("io.github.pdvrieze.xmlutil:core:$xmlutilVersion")
                api("io.github.pdvrieze.xmlutil:serialutil:$xmlutilVersion")
                api("io.github.pdvrieze.xmlutil:serialization:$xmlutilVersion")

                compileOnly(project(":JavaCommonApi"))
                implementation(project(":java-common"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val javaMain by creating {
            dependsOn(commonMain)
            dependencies {
//                implementation(kotlin("stdlib"))
//                implementation("io.github.pdvrieze.xmlutil:core:$xmlutilVersion")
//                implementation("io.github.pdvrieze.xmlutil:serialutil:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:xmlserializable:$xmlutilVersion")
            }
        }
        val jvmMain by getting {
            dependsOn(javaMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                api("io.github.pdvrieze.kotlinsql:kotlinsql-core:$kotlinsqlVersion")
                compileOnly(project(":DarwinJavaApi"))
                compileOnly(project(":JavaCommonApi"))
                compileOnly("org.apache.tomcat:tomcat-servlet-api:${tomcatVersion}")
                implementation("jakarta.jws:jakarta.jws-api:$jwsApiVersion")
                implementation("javax.activation:javax.activation-api:$activationVersion")
                implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbVersion")

/*
                implementation("io.github.pdvrieze.xmlutil:core:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serialutil:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serialization:$xmlutilVersion")
*/
            }
        }
        val jvmTest by getting {
            dependencies {
//                implementation(project(":ProcessEngine:core"))
//                implementation(project(":JavaCommonApi"))
                implementation(project(":DarwinJavaApi"))
//                implementation(project(":TestSupport"))
                implementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")


                implementation("org.xmlunit:xmlunit-core:2.6.0")
                implementation("org.mockito:mockito-core:2.25.0")
                implementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
                implementation(kotlin("test-junit5"))

                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
                runtimeOnly("com.fasterxml.woodstox:woodstox-core:5.1.0")

/*
                implementation("io.github.pdvrieze.xmlutil:core-jvm:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serialutil-jvm:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutilVersion")
*/


            }
        }
        val androidMain by getting {
            dependsOn(javaMain)
            dependencies {
                compileOnly(project(":DarwinJavaApi"))
                compileOnly(project(":JavaCommonApi"))
                implementation(kotlin("stdlib-jdk7"))
                implementation("io.github.pdvrieze.xmlutil:core-android:$xmlutilVersion")
/*
                implementation("io.github.pdvrieze.xmlutil:serialutil-android:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serializable-android:$xmlutilVersion")
                implementation("io.github.pdvrieze.xmlutil:serialization-android:$xmlutilVersion")
*/
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
/*
                implementation("io.github.pdvrieze.xmlutil:serialutil-js:$xmlutilVersion")
                api("io.github.pdvrieze.xmlutil:core-js:$xmlutilVersion")
                api("io.github.pdvrieze.xmlutil:serialization-js:$xmlutilVersion")
*/
                api("org.jetbrains.kotlinx:kotlinx-serialization-core-js:$serializationVersion")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version")
            }
        }
    }

}

//registerAndroidAttributeForDeps()


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
