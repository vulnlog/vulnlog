package io.vulnlog.cli.command

import io.vulnlog.cli.suppressions.OwaspDependencyCheckerSuppressor
import io.vulnlog.cli.suppressions.SnykSuppressor
import io.vulnlog.cli.suppressions.SuppressionComposition
import io.vulnlog.scriptdefinition.VulnLogScript
import io.vulnlog.scriptinghost.ScriptingHost
import java.io.File
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

interface Service {
    fun action(
        script: File,
        template: Path,
    ): List<SuppressionComposition>
}

class ServiceImpl : Service {
    override fun action(
        script: File,
        template: Path,
    ): List<SuppressionComposition> {
        val result: VulnLogScript = ScriptingHost().evalScript(script)
        val owasp: SuppressionComposition =
            template.listDirectoryEntries("*owasp*").map {
                val suppressor = OwaspDependencyCheckerSuppressor(it.toFile())
                suppressor.createSuppressions(result.allVulnerabilities)
            }.first()
        val snyk =
            template.listDirectoryEntries("*snyk*").map {
                val suppressor = SnykSuppressor(it.toFile())
                suppressor.createSuppressions(result.allVulnerabilities)
            }.first()
        return listOf(owasp, snyk)
    }
}
