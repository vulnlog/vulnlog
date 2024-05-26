package ch.addere.cli.command

import ch.addere.cli.suppressions.OwaspDependencyCheckerSuppressor
import ch.addere.cli.suppressions.SnykSuppressor
import ch.addere.cli.suppressions.SuppressionComposition
import ch.addere.vulnlog.scriptdefinition.VulnLogScript
import ch.addere.vulnlog.scriptinghost.ScriptingHost
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
        val result: VulnLogScript = ScriptingHost().evalScript(script)
        return if (template.name.endsWith(".xml")) {
            val marker = "<vulnlog-marker/>"
            val suppressor = OwaspDependencyCheckerSuppressor(template, marker)
            suppressor.createSuppressions(result.allVulnerabilities)
        } else {
            val marker = "vulnlog-marker"
            val suppressor = SnykSuppressor(template, marker)
            suppressor.createSuppressions(result.allVulnerabilities)
        }
    }
}
