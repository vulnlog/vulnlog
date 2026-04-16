plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "vulnlog"

include(":cli-app")
include(":lib")

project(":cli-app").projectDir = file("modules/cli-app")
project(":lib").projectDir = file("modules/lib")
