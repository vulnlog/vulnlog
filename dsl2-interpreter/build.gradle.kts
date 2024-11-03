plugins {
    id("vulnlog.lib-convention")
}

group = "io.vulnlog.dsl2-interpreter"
version = "0.1.0"

dependencies {
    implementation(project(":dsl2"))
    implementation(libs.bundles.kotlinScript)
}
