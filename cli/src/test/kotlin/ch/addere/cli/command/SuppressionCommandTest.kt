package ch.addere.cli.command

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe

class SuppressionCommandTest : FunSpec({

    val vulnlog = tempfile()
    val template = tempfile()

    test("test missing required template and file options") {
        val command = SuppressionCommand()

        val result = command.test("")

        result.statusCode shouldBe 1
        result.stdout shouldBe ""
        result.stderr shouldBe
            """
            Usage: supp [<options>]
            
            Error: missing option --templates
            Error: missing option --file
            
            """.trimIndent()
    }

    test("test missing required template option") {
        val command = SuppressionCommand()

        val result = command.test("--file test")

        result.statusCode shouldBe 1
        result.stdout shouldBe ""
        result.stderr shouldBe
            """
            Usage: supp [<options>]
            
            Error: missing option --templates
            
            """.trimIndent()
    }

    test("test missing required file option") {
        val command = SuppressionCommand()

        val result = command.test("--templates test")

        result.statusCode shouldBe 1
        result.stdout shouldBe ""
        result.stderr shouldBe
            """
            Usage: supp [<options>]
            
            Error: missing option --file
            
            """.trimIndent()
    }

    test("test with file and template option") {
        val command = SuppressionCommand()

        val result = command.test("--file ${vulnlog.path} --templates ${template.path}")

        result.statusCode shouldBe 0
    }
})
