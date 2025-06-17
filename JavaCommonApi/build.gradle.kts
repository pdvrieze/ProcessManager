import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
}

base {
    archivesName.set("javaCommonApi")
    version = "1.1.0"
    description = "Api project for the java-common project"
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
/*
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
*/
        val commonMain by getting {
            dependencies {
                implementation(project(":multiplatform"))
                implementation(libs.xmlutil.core)
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }

}

