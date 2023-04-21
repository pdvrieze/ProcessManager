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

package multiplatform

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.*
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.targets.js.*
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

fun KotlinTargetContainerWithPresetFunctions.jvmAndroid(configure: KotlinJvmTarget.() -> Unit = { }) {
    jvm("android") {
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        compilations.all {
/*
            kotlinOptions {
                val catalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
                jvmTarget = catalog.findVersion("kotlin.androidClassTarget").get().requiredVersion
            }
*/
        }
        configure()
    }
}
