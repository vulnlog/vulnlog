plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "vulnlog"

include(
    "cli",
    "dsl",
    "dsl-interpreter",
    "gradle-plugin",
)
