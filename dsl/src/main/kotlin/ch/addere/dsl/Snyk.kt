package ch.addere.dsl

data class Snyk(val id: String, override val affected: Set<Version>) : Scanner("Snyk", affected)

class SnykScannerBuilder : ScannerBuilder() {
    var id: String? = null

    override fun build(): Snyk = Snyk(id!!, affected)
}
