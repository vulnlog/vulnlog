plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
}

group = "ch.addere"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":dsl"))
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-RC")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

// TODO used to publish DSL jar but makes problem with execution of scripting-host executable
//tasks.withType<Jar> {
//    val dependencies = configurations.runtimeClasspath.get().map(::zipTree)
//    from(dependencies)
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
