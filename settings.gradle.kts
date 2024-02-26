plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "vulnlog"

include("dsl", "scripting-definition", "scripting-host", "cli")
