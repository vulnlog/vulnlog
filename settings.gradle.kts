plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "vulnlog"

include(
    "cli",
    "core",
    "dsl",
    "scripting-definition",
    "scripting-host",
)
