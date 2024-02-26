package ch.addere.dsl

data class OwaspDependencyChecker(override val affected: Set<Version>) : Scanner("OWASP Dependency Checker", affected)

class OwaspDependencyCheckerScannerBuilder : ScannerBuilder() {
    override fun build(): OwaspDependencyChecker = OwaspDependencyChecker(affected)
}
