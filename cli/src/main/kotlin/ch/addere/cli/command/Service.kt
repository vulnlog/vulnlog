package ch.addere.cli.command

import ch.addere.cli.suppressions.OwaspDependencyCheckerSuppressor
import ch.addere.cli.suppressions.SnykSuppressor
import ch.addere.cli.suppressions.SuppressionComposition
import ch.addere.dsl.VulnLog
import ch.addere.scripting.host.ScriptingHost
import java.io.File

interface Service {
    fun action(
        script: File,
        template: File,
    ): SuppressionComposition
}

class ServiceImpl : Service {
    override fun action(
        script: File,
        template: File,
    ): SuppressionComposition {
        val result: VulnLog = ScriptingHost().evalScript(script)
        return if (template.name.endsWith(".xml")) {
            val marker = "<vulnlog-marker/>"
            val suppressor = OwaspDependencyCheckerSuppressor(template, marker)
            suppressor.createSuppressions(result.vulnerabilities)
        } else {
            val marker = "vulnlog-marker"
            val suppressor = SnykSuppressor(template, marker)
            suppressor.createSuppressions(result.vulnerabilities)
        }
    }
}
