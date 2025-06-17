import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import versions.argJvmDefault

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

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.codegen)
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName.set("PE-common")
    version = "1.0.0"
    description = "A library with process engine support classes"
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.fromVersion(libs.versions.kotlin.languageVersion.get())
        apiVersion = KotlinVersion.fromVersion(libs.versions.kotlin.apiVersion.get())
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.kotlin.classTarget.get())
        }

        compilations.all {
            tasks.withType<Test> {
                useJUnitPlatform()
            }
        }
    }
/*
    jvmAndroid {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.kotlin.androidClassTarget.get()
            }
        }
    }
*/
    js {
        browser()
        nodejs()

        compilerOptions {
            sourceMap = true
            suppressWarnings = false
            verbose = true
//                metaInfo = true
            moduleKind = JsModuleKind.MODULE_UMD
            main = JsMainFunctionExecutionMode.CALL
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":multiplatform"))

                api(libs.kotlinx.serialization.core)
                api(libs.xmlutil.core)
                api(libs.xmlutil.serialutil)
                api(libs.xmlutil.serialization)

                compileOnly(project(":JavaCommonApi"))
                api(project(":java-common"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.testutil)
                implementation(project(":JavaCommonApi"))
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
//            kotlin.srcDir(file("src/javaMain/kotlin"))
            dependencies {
                api(libs.kotlinsql.core)
                compileOnly(project(":DarwinJavaApi"))
                compileOnly(project(":JavaCommonApi"))
                compileOnly(libs.servletApi)
                compileOnly(project(":java-common:jvmonly"))
                implementation(libs.xmlutil.xmlserializable)
                implementation(libs.jwsApi)
                implementation(libs.activationApi)
                implementation(libs.jaxb.api)
            }
        }
        val jvmTest by getting {
            dependencies {
//                implementation(project(":ProcessEngine:core"))
//                implementation(project(":JavaCommonApi"))
                implementation(project(":DarwinJavaApi"))
//                implementation(project(":TestSupport"))
                implementation(libs.junit5.api)


                implementation(libs.xmlutil.testutil)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
                implementation(kotlin("test-junit5"))

                runtimeOnly(libs.junit5.engine)
                runtimeOnly(libs.woodstox)

            }
        }
/*
        val androidTest by getting {
            dependencies {
                runtimeOnly(libs.kxml2)
            }
        }
*/
        val jsMain by getting {
            dependencies {
                api(project(":JavaCommonApi"))
            }
//            dependsOn(commonMain)
        }
    }

}

tasks.register("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}
