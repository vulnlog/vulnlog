plugins {
    id("vulnlog.lib-convention")
}

group = "dev.vulnlog.dsl-interpreter"
version = "0.2.0"

dependencies {
    implementation(project(":dsl"))
    implementation(libs.bundles.kotlinScript)
}
