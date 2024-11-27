plugins {
    id("vulnlog.lib-convention")
}

description = "The Vulnlog DSL parsing interpreter parses Vulnlog DSL files."

group = "dev.vulnlog.dsl-interpreter"
version = providers.gradleProperty("vlVersion").get()

dependencies {
    implementation(project(":dsl"))
    implementation(libs.bundles.kotlinScript)
}
