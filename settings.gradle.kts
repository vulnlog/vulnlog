plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "vulnlog"

// Declare logical paths
include(
    ":cli",
    ":common",
    ":dsl",
    ":dsl-interpreter",
    ":gradle-plugin",
    ":report",
    ":suppression",
)

// Map them to their actual directories under cli-app/
project(":cli").projectDir = file("cli-app/cli")
project(":common").projectDir = file("cli-app/common")
project(":dsl").projectDir = file("cli-app/dsl")
project(":dsl-interpreter").projectDir = file("cli-app/dsl-interpreter")
project(":gradle-plugin").projectDir = file("cli-app/gradle-plugin")
project(":report").projectDir = file("cli-app/report")
project(":suppression").projectDir = file("cli-app/suppression")

//includeBuild("cli-app")
