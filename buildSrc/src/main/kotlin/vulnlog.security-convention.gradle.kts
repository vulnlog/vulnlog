plugins {
    id("io.snyk.gradle.plugin.snykplugin")
    id("org.owasp.dependencycheck")
}

val reportsDir: File = layout.buildDirectory.dir("reports").get().asFile
val suppressionsDir: File = layout.buildDirectory.dir("suppressions").get().asFile

snyk {
    setArguments(
        "--policy-path=$suppressionsDir/.snyk " +
            "--json-file-output=$reportsDir/snyk/report.json " +
            "--sarif-file-output=$reportsDir/snyk/report.sarif " +
            "--all-sub-projects ",
    )
    setAutoDownload(true)
    setAutoUpdate(true)
}

dependencyCheck {
    format = "ALL"
    outputDirectory = "$reportsDir/owaspDependencyChecker"
    suppressionFile = "$suppressionsDir/owasp-suppression.xml"
    nvd.apiKey = System.getenv("OWASP_DEPENDENCY_CHECK")
}
