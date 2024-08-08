plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

base {
    archivesName.set("multiplatform")
    version = "0.1"
}

group = "io.github.pdvrieze.pm"
version = "0.1"

//dependencies {
//    testImplementation(kotlin("test"))
//}

//tasks.test {
//    useJUnitPlatform()
//}
kotlin {
    jvmToolchain(11)
    jvm()
    js {
        browser()
        nodejs()

    }
}
