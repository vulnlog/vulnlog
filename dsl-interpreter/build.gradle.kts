plugins {
    id("vulnlog.lib-convention")
}

group = "dev.vulnlog.dsl-interpreter"
version = "0.3.0-SNAPSHOT"

dependencies {
    implementation(project(":dsl"))
    implementation(libs.bundles.kotlinScript)
}
