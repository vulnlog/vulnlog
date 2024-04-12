plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "ch.addere"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
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
