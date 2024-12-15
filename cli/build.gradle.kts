plugins {
    id("vulnlog.app-convention")
}

description = "CLI application parsing Vulnlog files."

group = "dev.vulnlog.cli"
version = providers.gradleProperty("vlVersion").get()

dependencies {
    implementation(project(":dsl-interpreter"))

    implementation(libs.clikt)
}

application {
    mainClass.set("dev.vulnlog.cli.AppKt")
}
