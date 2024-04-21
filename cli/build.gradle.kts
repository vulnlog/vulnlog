plugins {
    id("vulnlog.app-convention")
}

dependencies {
    implementation(project(":scripting-host"))

    implementation(libs.bundles.kotlinScript)
    implementation(libs.clikt)
}

application {
    mainClass.set("ch.addere.cli.AppKt")
    applicationName = "vl"
}
