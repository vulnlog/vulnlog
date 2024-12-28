package dev.vulnlog.dslinterpreter

import dev.vulnlog.dsl3.All
import dev.vulnlog.dsl3.MyVulnImpl
import dev.vulnlog.dsl3.ReleaseBranch
import dev.vulnlog.dsl3.ReleaseBranch.Factory.create
import dev.vulnlog.dsl3.SCA_SCANNER
import dev.vulnlog.dsl3.allOther
import dev.vulnlog.dsl3.days
import dev.vulnlog.dsl3.nextPublication

fun main() {
    create("rb1")
    create("rb2")
    create("rb3")
    create("rb4")
    create("rb5")

    val rb1 by ReleaseBranch
    val rb2 by ReleaseBranch
    val rb3 by ReleaseBranch
    val rb5 by ReleaseBranch

    val wrapper = MyVulnImpl()

    val c =
        wrapper.vuln("c") {
            r from SCA_SCANNER at "2025-02-06" on rb1..rb5
            a analysedAt "2025-02-11" verdict "medium" because "this is a test entry"
            t update "old:dependency" atLeastTo "v23" on rb1..rb2 andUpdateAtLeastTo "v42" on rb3 andNoActionOn allOther
            e suppressOn rb1..rb2 andSuppressFor 14.days on rb3 andSuppressUntil nextPublication on allOther
        }
    println(c)

    val reason =
        """
        DoS vulnerability in Spring MVC. Affected versions are 5.3.0 - 5.3.41.
        This finding is a false positive, since 6.1.14 is reported.
        """.trimIndent()
    val x =
        wrapper.vuln("SNYK-JAVA-ORGSPRINGFRAMEWORK-8384234") {
            r from SCA_SCANNER at "2025-11-21" on rb1..rb5
            a verdict "not affected" because reason
            t noActionOn All
        }
    println(x)

    // TODO
//    val d = myVuln("d") {}
//    println(d)

    val g =
        wrapper.vuln("g") {
            r from SCA_SCANNER at "2025-02-06" on rb1..rb5
            a verdict "medium" because "this is a test entry"
            t waitOnAllFor 14.days
        }
    println(g)

    val z =
        wrapper.vuln("z") {
            r from SCA_SCANNER at "2025-02-06" on rb1..rb5
            a verdict "medium" because "this is a test entry"
            t update "old:dependency" atLeastTo "v23" on rb1..rb2 andUpdateAtLeastTo "v42" on rb3 andNoActionOn allOther
            e suppressOn rb1..rb2 andSuppressFor 14.days on rb3 andSuppressUntil nextPublication on allOther
        }
    println(z)
}
