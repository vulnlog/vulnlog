plugins {
    id("vulnlog.lib-convention")
}

description = "Common classes used by all CLI commands and subcommands."

dependencies {
    api(project(":dsl"))
}

group = "dev.vulnlog"
version = providers.gradleProperty("vlVersion").get()
