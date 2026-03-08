plugins {
    id("vulnlog.app-convention")
}

description = "Next generation Vulnlog CLI application"

group = "dev.vulnlog"

application {
    mainClass.set("dev.vulnlog.cli.shell.MainKt")
    applicationName = "vulnlog"
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Version"] = version
    }
}
