import org.apache.tools.ant.filters.ReplaceTokens

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
    applicationName = "vl"
}

tasks.named<Copy>("processResources") {
    val vlVersion = providers.gradleProperty("vlVersion")
    doFirst {
        filesMatching("version.txt") {
            filter(ReplaceTokens::class, "tokens" to mapOf("vlVersion" to vlVersion.get()))
        }
    }
}
