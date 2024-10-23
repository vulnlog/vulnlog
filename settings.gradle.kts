plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "vulnlog"

include(
    "cli",
    "core",
    "dsl",
    "dsl2",
    "rdsl",
    "rdsl-host",
    "scripting-definition",
    "scripting-host",
)
