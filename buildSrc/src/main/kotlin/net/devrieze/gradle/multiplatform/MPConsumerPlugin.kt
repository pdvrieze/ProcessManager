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

package multiplatform.net.devrieze.gradle.multiplatform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.hasPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformAndroidPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJsPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin

class MPConsumerPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            logger.lifecycle("Multiplatform consumer plugin applied!")
            if (plugins.hasPlugin(KotlinMultiplatformPlugin::class)) {
                logger.warn("Applying multiplatform consumer plugin on a multiplatform project")
            } else {
                val platformType = when {
                    // Don't use classes for the android plugins as we don't want to pull in the android plugin into the
                    // classpath just to find that android is not needed.
                    plugins.hasPlugin(KotlinPlatformAndroidPlugin::class) or
                    plugins.hasPlugin("com.android.application") or
                    plugins.hasPlugin("com.android.feature") or
                    plugins.hasPlugin("com.android.test") or
                    plugins.hasPlugin("com.android.library") -> KotlinPlatformType.androidJvm

                    plugins.hasPlugin(KotlinPlatformJsPlugin::class) -> KotlinPlatformType.js

                    else -> KotlinPlatformType.jvm
                }

//            logger.lifecycle("Applying multiplatform consumer plugin. Applied plugins: ${plugins.joinToString { it.javaClass.name }}")
                configurations.all {
                    if (isCanBeResolved) {
                        logger.debug("Skipping configuration $name because it is resolvable")
//                    } else if (isCanBeConsumed) {
//                        logger.debug("Skipping configuration $name because it is consumable")
                    } else {
                        // All should defer actual application
                        attributes {
                            if (! contains(KotlinPlatformType.attribute)) {
                                logger.lifecycle("Adding kotlin usage attribute $platformType to configuration: ${name}")
                                attribute(KotlinPlatformType.attribute, platformType)
                            } else {
                                logger.lifecycle("Preserving kotlin usage attribute on configuration $name as ${getAttribute(KotlinPlatformType.attribute)} instead of $platformType")
                            }
                        }
                    }
                }
                logger.lifecycle("Registering the platform type attributes to the schema with the resolution rules")
                KotlinPlatformType.setupAttributesMatchingStrategy(dependencies.attributesSchema)
            }
        }
    }
}
