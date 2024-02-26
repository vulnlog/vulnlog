package ch.addere.cli

import ch.addere.dsl.VulnLog
import ch.addere.scripting.host.Host
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) throw IllegalArgumentException("Require FILE argument")
    val script = File(args[0]).readText()

    try {
        val result: VulnLog = Host().evalScript(script)

        println("release branches")
        result.releaseBranch.forEach(::println)
        println("branches = ${result.branches}")
        println("vulnerabilities")
        result.vulnerabilities.forEach(::println)
    } catch (e: IllegalStateException) {
        println(e.message)
    }
}
