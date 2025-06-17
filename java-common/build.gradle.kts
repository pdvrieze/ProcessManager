import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import versions.argJvmDefault

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
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName.set("java-common")
    version = "1.1.0"
    description = "A library with generic support classes"
}

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.languageVersion.get())
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
                implementation(libs.kotlinx.serialization.core)
                implementation(kotlin("stdlib"))

                compileOnly(project(":JavaCommonApi"))

//                api(project(":JavaCommonApi"))
                api(project(":multiplatform"))
            }
        }
        val jvmMain by getting {
            dependencies {
//                implementation(libs.jaxb.api)
                api(libs.kotlinsql.monadic)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit5.api)
                runtimeOnly(libs.junit5.engine)
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
