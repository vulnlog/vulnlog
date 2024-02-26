package ch.addere.dsl

data class Reporter(val scanner: List<Scanner>)

class ReporterBuilder : V() {
    var scanner = mutableListOf<Scanner>()

    fun snyk(block: SnykScannerBuilder.() -> Unit) {
        val builder = SnykScannerBuilder()
        builder.block()
        scanner += builder.build()
    }

    fun owaspDependencyChecker(block: OwaspDependencyCheckerScannerBuilder.() -> Unit) {
        val builder = OwaspDependencyCheckerScannerBuilder()
        builder.block()
        scanner += builder.build()
    }

    fun build(): Reporter = Reporter(scanner)
}
