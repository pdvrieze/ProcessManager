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
    alias(libs.plugins.codegen)
    alias(libs.plugins.kotlin.serialization)
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":JavaCommonApi"))
                api(project(":ProcessEngine:core"))
                api(project(":TestSupport"))
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))

//                implementation(libs.kotlinx.serialization.core)
                api(libs.xmlutil.core)
                api(libs.xmlutil.testutil)
//                api(libs.xmlutil.serialutil)
                api(libs.xmlutil.serialization)

//                implementation(project(":java-common"))

            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.xmlutil.core)
                implementation(libs.xmlutil.serialization)
            }
        }
    }

}

tasks.register("test") {
    dependsOn(tasks.named("jvmTest"))
    group="verification"
}
