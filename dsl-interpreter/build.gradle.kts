plugins {
    id("vulnlog.lib-convention")
}

group = "io.vulnlog.dsl-interpreter"
version = "0.1.0"

dependencies {
    implementation(project(":dsl"))
    implementation(libs.bundles.kotlinScript)
}
