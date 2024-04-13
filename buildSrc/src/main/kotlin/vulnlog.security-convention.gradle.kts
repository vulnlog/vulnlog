plugins {
    id("org.owasp.dependencycheck")
}

dependencyCheck {
    format = "ALL"
    outputDirectory = "build/reports/owaspDependencyChecker"
//    suppressionFile = "build/suppressions/owaspDependencyChecker.xml"
    nvd.apiKey = System.getenv("OWASP_DEPENDENCY_CHECK")
}
