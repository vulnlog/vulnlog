plugins {
    id("vulnlog.app-convention")
}

description = "CLI application parsing Vulnlog files."

group = "dev.vulnlog"

dependencies {
    implementation(project(":dsl-interpreter"))
    implementation(project(":common"))
    implementation(project(":report"))
    implementation(project(":suppression"))

    implementation(libs.kotlinCoroutinesCore)
    implementation(libs.clikt)
    implementation(libs.koinCore)
    implementation(libs.kotlinxSerializationJson)
}

application {
    mainClass.set("dev.vulnlog.cli.AppKt")
    applicationName = "vl"
    // because of Java 21 warning: JNA loads a native library via System.load, which now requires enabling native access
    applicationDefaultJvmArgs += listOf("--enable-native-access=ALL-UNNAMED")

}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Version"] = version
    }
}
