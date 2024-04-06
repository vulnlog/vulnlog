package ch.addere.cli

import ch.addere.cli.suppressions.OwaspDependencyCheckerSuppressor
import ch.addere.cli.suppressions.SnykSuppressor
import ch.addere.dsl.VulnLog
import ch.addere.scripting.host.Host
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) throw IllegalArgumentException("Require FILE argument")
    val script = File(args[0]).readText()

    try {
        val result: VulnLog = Host().evalScript(script)

        if (args[1].isNotBlank()) {
            val template = File(args[1])
            val suppressions = if (template.name.endsWith(".xml")) {
                val marker = "<vulnlog-marker/>"
                val suppressor = OwaspDependencyCheckerSuppressor(template, marker)
                suppressor.createSuppressions(result.vulnerabilities)
            } else {
                val marker = "vulnlog-marker"
                val suppressor = SnykSuppressor(template, marker)
                suppressor.createSuppressions(result.vulnerabilities)
            }

            println(
                suppressions.pretty(before = "\n\n", after = "\n\n", between = "\n\n", indentation = "\t")
                    .joinToString("")
            )
        } else {
            println("release branches")
            result.releaseBranch.forEach(::println)
            println("branches = ${result.branches}")
            println("vulnerabilities")
            result.vulnerabilities.forEach(::println)
        }
    } catch (e: IllegalStateException) {
        println(e.message)
    }
}
