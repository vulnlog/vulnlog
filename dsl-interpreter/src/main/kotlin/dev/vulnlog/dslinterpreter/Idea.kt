package dev.vulnlog.dslinterpreter

import dev.vulnlog.dsl2.Low
import dev.vulnlog.dsl2.Moderate
import dev.vulnlog.dsl2.NextRelease
import dev.vulnlog.dsl2.NotAffected
import dev.vulnlog.dsl2.Permanently
import dev.vulnlog.dsl2.RatedVulnerabilityRateContext
import dev.vulnlog.dsl2.Temporarily
import dev.vulnlog.dsl2.VlPublicationDefault
import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlVuln
import dev.vulnlog.dsl2.VlVulnlogContext
import dev.vulnlog.dsl2.atDate
import dev.vulnlog.dsl2.fixIn
import dev.vulnlog.dsl2.impl.VlVulnlogContextImpl
import dev.vulnlog.dsl2.suppress
import dev.vulnlog.dsl2.update
import dev.vulnlog.dsl2.waitAndReviewInWeeks

fun List<VlReleaseBranch>.getUpcomingRelease() =
    flatMap { it.releases }.firstOrNull { it.publication == VlPublicationDefault }
        ?: flatMap { it.releases }.last()

const val TWENTY_ONE_MONTHS: Long = 21
const val SIX_MONTHS: Long = 6

fun main() {
    val ctx = VlVulnlogContextImpl()
    val (rb1, rb2, rb3) = createReleaseBranches(ctx)

    println(rb1.release("1.1.0"))
    printReleaseBranch(rb1)

    val (semgrep, snyk, oracleCpu) = ctx.reporters("Semgrep", "Snyk", "Oracle CPU")

    ctx.vuln("CVE-2025-0")

    ctx.vuln("CVE-2025-1") {
        /*
         * 1. report
         * 2. analyse
         * 3. resolve
         * 4. plan
         */
        val (rep1, rep2) =
            report(
                snyk atDate "2024-12-28" exact rb1 andExact rb2 onVariant "Container",
                semgrep atDate "2024-12-28" from rb1 to rb3 onVariant "All",
            )
        val (anal1, anal2) =
            analyse(
                rep1 atDate "2025-01-03" asRating Low because "Lorem ipsum.",
                rep2 atDate "2025-01-03" asRating Moderate because "Lorem ipsum dolor.",
            )
        val (t1, t2, t3) =
            resolution(
                anal1 update "dependency" atLeastTo "1.23.42" onRelease rb1,
                anal1 update "dependency" atLeastTo "1.22" onRelease rb2,
                anal2 update "dependency" atLeastTo "1.12" onRelease rb1 andOn rb2 andOn rb3,
            )
        plan(
            t1 fixIn NextRelease andSuppress Temporarily,
            t2 suppress Permanently,
            t3 waitAndReviewInWeeks 2,
        )
    }

    ctx.vuln("CVE-2025-2", "CVE-2025-3") {
        report(
            snyk atDate "2024-12-28" from rb1 to rb3 onVariant "Container",
        )
    }

    ctx.vuln(
        "CVE-2023-42950",
        "CVE-2023-42956",
        "CVE-2024-23252",
        "CVE-2024-23254",
        "CVE-2024-23263",
        "CVE-2024-23280",
        "CVE-2024-23284",
        "CVE-2024-27834",
    ) {
        val report = report(oracleCpu atDate "2024-10-28" from rb1 to rb3 onVariant "All")
        val analysis = analyse(report atDate "2024-10-28" asRating NotAffected because "JavaFX is not utilised.")
        val resolution =
            resolution(analysis update "Java JDK" atLeastTo "17.0.13+11" onRelease rb1 andOn rb2 andOn rb3)
        plan(resolution fixIn NextRelease andSuppress Temporarily)
    }
}

private fun createReleaseBranches(ctx: VlVulnlogContext): Array<VlReleaseBranch> {
    return ctx.releases {
        val (sup, extSup1, extSup2) =
            lifeCycles(
                "Regular Support" months TWENTY_ONE_MONTHS,
                "Extended Support 1" months SIX_MONTHS,
                "Extended Support 2" months SIX_MONTHS,
            )
        branch("Release 1", sup, extSup1, extSup2) {
            release(
                "1.0.0" publishedAt "2024-06-30",
                "1.1.0" publishedAt "2024-10-31",
                "1.2.0" publishedAt "2024-11-30",
                "1.3.0" publishedAt "2024-12-31",
                "1.4.0" publishedAt "2025-01-31",
                "1.5.0" publishedAt "",
            )
        }
        branch("Release 2", sup, extSup1) {
            release(
                "2.0.0" publishedAt "2023-10-01",
                "2.1.0" publishedAt "2024-10-01",
                "2.2.0" publishedAt "",
            )
        }
        branch("Release 3", sup) {
            release(
                "3.0.0" publishedAt "2024-01-01",
                "3.1.0" publishedAt "2025-01-01",
                "3.2.0" publishedAt "",
            )
        }
    }
}

private fun printReleaseBranch(rb1: VlReleaseBranch) {
    println()
    val rb1SupportedMonths = rb1.supportLifetime().sumOf { it.second.months() }
    rb1.supportLifetime().forEach { println("${it.first} = ${it.second} (${it.second.months()} months)") }
    println("${rb1.name} is supported for $rb1SupportedMonths months")
}

fun printVuln(vuln: VlVuln) {
    println("=== vulnerability ${vuln.ids.joinToString(", ") { it.identifier }} === ")
    println("--- Report ---")
    vuln.reportedFor.forEach { reportedFor ->
        println("${reportedFor.reporter} at ${reportedFor.at}")
    }

    println("--- Analyse ---")
    vuln.rating.filterIsInstance<RatedVulnerabilityRateContext>().forEach { rating ->
        println("${rating.ratedAt} -- ${rating.rating.asText} -- ${rating.reasoning}")
    }

    println("--- Resolution ---")
    vuln.resolutionTask.forEach { resolution ->
        val taskDescription = resolution.resolutionTask.taskDescription
        val applyOn = resolution.resolutionTask.applyOn
        val version = resolution.version
        val releases = resolution.releaseBranches.joinToString(", ") { a -> a.name }
        println("--> task: $taskDescription$applyOn to at least $version for releases $releases")
    }

    println("--- Plan ---")
    vuln.taskPlans.forEach { taskPlan ->
        taskPlan.taskAction.forEach { taskAction -> println("  task action: $taskAction") }
    }
    println()
}
