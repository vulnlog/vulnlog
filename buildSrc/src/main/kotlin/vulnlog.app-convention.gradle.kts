plugins {
    id("vulnlog.common-convention")
    application
    id("org.graalvm.buildtools.native")
}

// GraalVM Native Build Tools does not yet fully support configuration cache.
// Mark affected tasks explicitly so cache still works for non-native builds.
tasks.withType<org.graalvm.buildtools.gradle.tasks.GenerateResourcesConfigFile>().configureEach {
    notCompatibleWithConfigurationCache("GraalVM Native Build Tools plugin is not yet configuration cache compatible")
}
tasks.withType<org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask>().configureEach {
    notCompatibleWithConfigurationCache("GraalVM Native Build Tools plugin is not yet configuration cache compatible")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("vulnlog")
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
            )
        }
    }
}
