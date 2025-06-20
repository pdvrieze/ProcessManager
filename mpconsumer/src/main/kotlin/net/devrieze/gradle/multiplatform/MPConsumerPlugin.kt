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
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinJsPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

class MPConsumerPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            logger.lifecycle("Multiplatform consumer plugin applied!")
            if (pluginManager.hasPlugin("kotlin-multiplatform")) {
                logger.warn("Applying multiplatform consumer plugin on a multiplatform project, this has no effect")
            } else {
                val platformType = when {
                    // Don't use classes for the android plugins as we don't want to pull in the android plugin into the
                    // classpath just to find that android is not needed.
                    plugins.hasPlugin(KotlinAndroidPluginWrapper::class) or
                        plugins.hasPlugin("com.android.application") or
                        plugins.hasPlugin("com.android.feature") or
                        plugins.hasPlugin("com.android.test") or
                        plugins.hasPlugin("com.android.library") -> KotlinPlatformType.androidJvm

                    plugins.hasPlugin(KotlinJsPluginWrapper::class) -> KotlinPlatformType.js

                    else -> KotlinPlatformType.jvm
                }
                configurations.configureEach {
                    if (! isCanBeConsumed) {
                        attributes {
                            if (!contains(KotlinPlatformType.attribute)) {
                                logger.info("Adding kotlin usage attribute $platformType to configuration: ${name}")
                                attribute(KotlinPlatformType.attribute, platformType)
                            } else {
                                logger.debug(
                                    "Preserving kotlin usage attribute on configuration $name as ${
                                        getAttribute(
                                            KotlinPlatformType.attribute
                                        )
                                    } instead of $platformType"
                                )
                            }

                            if (platformType == KotlinPlatformType.js && !contains(KotlinJsCompilerAttribute.jsCompilerAttribute)) {
                                attribute(
                                    KotlinJsCompilerAttribute.jsCompilerAttribute,
                                    KotlinJsCompilerAttribute.legacy
                                )
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

