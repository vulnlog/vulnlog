package ch.addere.dsl

data class Vulns(val vulnerabilities: List<Vulnerability>) {
    fun byReporter(scanner: Scanner): Vulns {
        val filtered = vulnerabilities.filter { it.reporter!!.scanner == scanner }.toList()
        return Vulns(filtered)
    }

    override fun toString(): String = vulnerabilities.joinToString("\n")
}
