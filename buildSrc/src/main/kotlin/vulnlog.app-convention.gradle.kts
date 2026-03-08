plugins {
    id("vulnlog.common-convention")
    application
    id("org.graalvm.buildtools.native")
}

// GraalVM Native Build Tools does not yet fully support configuration cache.
// Always pass --no-configuration-cache when running native tasks locally:
//   ./gradlew :next:nativeCompile --no-configuration-cache

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
