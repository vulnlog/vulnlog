plugins {
    id("vulnlog.app-convention")
}

dependencies {
    implementation(project(":scripting-host"))
    implementation(libs.bundles.kotlinScript)
}

application {
    mainClass.set("ch.addere.cli.AppKt")
}
