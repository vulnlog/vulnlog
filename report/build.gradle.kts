plugins {
    id("vulnlog.lib-convention")
}

description = "The Vulnlog report generation functionality."

group = "dev.vulnlog"
version = providers.gradleProperty("vlVersion").get()
