plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "vulnlog"

include(
    "cli",
    "common",
    "dsl",
    "dsl-interpreter",
    "gradle-plugin",
    "report",
    "suppression",
)
