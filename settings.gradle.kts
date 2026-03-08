plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "vulnlog"

include(":next")

project(":next").projectDir = file("next/cli-app")
