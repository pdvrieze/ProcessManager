import versions.kotlin_version
import versions.kotlinx_html_version

/*
 * Copyright (c) 2017.
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
    kotlin("js")
}
base {
    version = "1.1.0"
    description = "A library with generic support classes for javascript"
}

kotlin {
    target {
        browser()
    }
}

dependencies {
    implementation(kotlin("stdlib"))
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlin_version")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinx_html_version")
}

