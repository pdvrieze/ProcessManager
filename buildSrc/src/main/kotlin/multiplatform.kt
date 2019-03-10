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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.dependencies

val androidAttribute = Attribute.of("net.devrieze.android", Boolean::class.javaObjectType)

fun Project.registerAndroidAttributeForDeps() {
    dependencies {
        attributesSchema {
            attribute(androidAttribute) {
                compatibilityRules.add(AndroidCompatRule::class.java)
                disambiguationRules.add(AndroidDisambiguationRule::class.java)
            }
        }
    }
}

class AndroidCompatRule : AttributeCompatibilityRule<Boolean> {
    override fun execute(t: CompatibilityCheckDetails<Boolean>) {
        val producer = t.producerValue
        val consumer = t.consumerValue
        when {
            consumer == null -> if (producer!=true) t.compatible() else t.incompatible()
            producer == null -> t.compatible()
        }
    }
}

class AndroidDisambiguationRule : AttributeDisambiguationRule<Boolean> {
    override fun execute(t: MultipleCandidatesDetails<Boolean>) {
        when (t.consumerValue) {
            true -> if (true in t.candidateValues) t.closestMatch(true)
            false -> if (false in t.candidateValues) t.closestMatch(false)
            null -> if (false in t.candidateValues) t.closestMatch(false)
        }
    }

}
