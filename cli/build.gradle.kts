plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "ch.addere"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":scripting-host"))
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-main-kts:1.9.22")

    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ch.addere.cli.AppKt")
}
