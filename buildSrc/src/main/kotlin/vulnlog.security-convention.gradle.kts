plugins {
    id("io.snyk.gradle.plugin.snykplugin")
    id("org.owasp.dependencycheck")
}

snyk {
    val suppressionFile = "build/suppressions/.snyk"
    val reportFile = "build/reports/snykReport"
    println("wololo: $suppressionFile")
    setArguments("--policy-path=$suppressionFile --json-file-output=$reportFile --all-sub-projects")
    setApi(System.getenv("SNYK_API_KEY"))
    setAutoDownload(true)
    setAutoUpdate(true)
}

dependencyCheck {
    format = "ALL"
    outputDirectory = "build/reports/owaspDependencyChecker"
    suppressionFile = "build/suppressions/owaspDependencyChecker.xml"
    nvd.apiKey = System.getenv("OWASP_DEPENDENCY_CHECK")
}
