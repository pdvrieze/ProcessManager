plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version libs.versions.compose.get()
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":compose:common"))
                implementation(compose.web.core)
                implementation(compose.web.svg)
                implementation(compose.runtime)
            }
        }
        val jsTest by getting
    }
}

compose.web {

}
