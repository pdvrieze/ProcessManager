plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version libs.versions.compose.get()
}

dependencies {
    implementation(project(":compose:common"))
    implementation(compose.desktop.currentOs)

}
