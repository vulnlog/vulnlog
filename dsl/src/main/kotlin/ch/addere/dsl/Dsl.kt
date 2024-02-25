data class Version(val major: Int, val minor: Int, val patch: Int)
class VersionBuilder() {
    val versions = mutableListOf<Version>()
    operator fun Version.unaryPlus() = versions.add(this)
    fun build(): List<Version> = versions
}

data class ReleaseBranch(val name: String, val versions: List<Version>) {}
class ReleaseBranchBuilder() {
    var name: String? = null
    var versions: List<Version>? = null

    fun versions(block: VersionBuilder.() -> Unit): Unit {
        val version = VersionBuilder()
        version.block()
        versions = version.build()
    }

    fun build(): ReleaseBranch = ReleaseBranch(name!!, versions!!)
}

data class SupportedBranches(
    val supported: List<ReleaseBranch>,
    val unsupported: List<ReleaseBranch>
)

class SupportedBranchesBuilder() {
    val supportedBranches = mutableListOf<ReleaseBranch>()
    val unsupportedBranches = mutableListOf<ReleaseBranch>()

    operator fun ReleaseBranch.unaryPlus() = supportedBranches.add(this)
    operator fun ReleaseBranch.unaryMinus() = unsupportedBranches.add(this)

    fun build(): SupportedBranches {
        return SupportedBranches(supportedBranches, unsupportedBranches)
    }
}

@DslMarker
annotation class VulnerabilityMarker

@VulnerabilityMarker
abstract class V

data class VulnLogs(
    val branches: SupportedBranches,
    val vulnerabilities: List<Vulnerability>
)

class VulnLogsBuilder() : V() {
    var releaseBranch = mutableListOf<ReleaseBranch>()
    var branches: SupportedBranches? = null
    val vulnerabilities = mutableListOf<Vulnerability>()

    fun release(block: ReleaseBranchBuilder.() -> Unit): ReleaseBranch {
        val builder = ReleaseBranchBuilder()
        builder.block()
        val rb = builder.build()
        releaseBranch += rb
        return rb
    }

    fun branches(block: SupportedBranchesBuilder.() -> Unit): Unit {
        val supportedBranchesBuilder = SupportedBranchesBuilder()
        supportedBranchesBuilder.block()
        branches = supportedBranchesBuilder.build()
    }

    fun vulnerability(block: VulnerabilityBuilder.() -> Unit): Unit {
        val builder = VulnerabilityBuilder()
        builder.block()
        vulnerabilities.add(builder.build())
    }

    fun build(): VulnLogs = VulnLogs(branches!!, vulnerabilities)
}

fun vulnLogs(block: VulnLogsBuilder.() -> Unit): VulnLogs {
    val vulnLogs = VulnLogsBuilder()
    vulnLogs.block()
    return vulnLogs.build()
}

abstract class Scanner(val name: String, open val affected: List<Version>)
abstract class ScannerBuilder() {
    var name: String? = null
    var affected: List<Version>? = null

    fun affected(block: VersionBuilder.() -> Unit): Unit {
        val builder = VersionBuilder()
        builder.block()
        affected = builder.build()
    }

    abstract fun build(): Scanner
}

data class Snyk(val id: String, override val affected: List<Version>) : Scanner("Snyk", affected)
class SnykScannerBuilder() : ScannerBuilder() {
    var id: String? = null

    override fun build(): Snyk = Snyk(id!!, affected!!)
}

data class OwaspDependencyChecker(override val affected: List<Version>) : Scanner("OWASP Dependency Checker", affected)
class OwaspDependencyCheckerScannerBuilder() : ScannerBuilder() {
    override fun build(): OwaspDependencyChecker = OwaspDependencyChecker(affected!!)
}

data class Vulns(val vulnerabilities: List<Vulnerability>) {
    fun byReporter(scanner: Scanner): Vulns {
        val filtered = vulnerabilities.filter { it.reporter!!.scanner == scanner }.toList()
        return Vulns(filtered)
    }

    override fun toString(): String = vulnerabilities.joinToString("\n")
}

data class Reporter(val scanner: List<Scanner>)
class ReporterBuilder() : V() {
    var scanner = mutableListOf<Scanner>()

    fun snyk(block: SnykScannerBuilder.() -> Unit): Unit {
        val builder = SnykScannerBuilder()
        builder.block()
        scanner += builder.build()
    }

    fun owaspDependencyChecker(block: OwaspDependencyCheckerScannerBuilder.() -> Unit): Unit {
        val builder = OwaspDependencyCheckerScannerBuilder()
        builder.block()
        scanner += builder.build()
    }

    fun build(): Reporter = Reporter(scanner)
}

data class Vulnerability(
    val id: String,
    val reporter: Reporter?,
    val resolution: Resolution?
)

class VulnerabilityBuilder() : V() {
    var cve: String? = null
    var reporter: Reporter? = null
    var resolution: Resolution? = null

    fun reporter(block: ReporterBuilder.() -> Unit): Unit {
        val builder = ReporterBuilder()
        builder.block()
        reporter = builder.build()
    }

    fun resolution(block: ResolutionBuilder.() -> Unit): Unit {
        val builder = ResolutionBuilder()
        builder.block()
        resolution = builder.build()
    }

    fun build(): Vulnerability = Vulnerability(cve!!, reporter, resolution)
}

data class ResolutionUpdated(val versions: List<Version>)
class ResolutionUpdatedBuilder() {
    val versions = mutableListOf<Version>()

    operator fun Version.unaryPlus() = versions.add(this)

    fun build(): ResolutionUpdated = ResolutionUpdated(versions)
}

data class Resolution(
    val updated: ResolutionUpdated?,
    val suppression: ResolutionUpdated?
)

class ResolutionBuilder() {
    var updated: ResolutionUpdated? = null
    var suppression: ResolutionUpdated? = null

    fun updated(block: ResolutionUpdatedBuilder.() -> Unit) {
        val builder = ResolutionUpdatedBuilder()
        builder.block()
        updated = builder.build()
    }

    fun suppression(block: ResolutionUpdatedBuilder.() -> Unit) {
        val builder = ResolutionUpdatedBuilder()
        builder.block()
        suppression = builder.build()
    }
    // ignore

    fun build(): Resolution = Resolution(updated, suppression)
}
