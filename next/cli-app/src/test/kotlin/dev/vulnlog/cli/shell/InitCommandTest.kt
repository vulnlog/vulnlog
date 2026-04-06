package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files

class InitCommandTest : FunSpec({

    test("init outputs YAML to stdout when no output option is specified") {
        val result = InitCommand().test("--organization acme --name widget --author alice")

        result.statusCode shouldBe 0
        result.stdout shouldContain "schemaVersion:"
        result.stdout shouldContain "organization:"
        result.stdout shouldContain "acme"
    }

    test("init outputs YAML to stdout with explicit -o -") {
        val result = InitCommand().test("--organization acme --name widget --author alice -o -")

        result.statusCode shouldBe 0
        result.stdout shouldContain "schemaVersion:"
        result.stdout shouldContain "acme"
    }

    test("init does not print status message to stdout when writing to stdout") {
        val result = InitCommand().test("--organization acme --name widget --author alice")

        result.statusCode shouldBe 0
        result.stdout shouldNotContain "Vulnlog file created at:"
    }

    test("init writes to file when -o path is specified") {
        val tempFile = Files.createTempFile("vulnlog-test", ".yaml").toFile()
        try {
            val result =
                InitCommand().test(
                    "--organization acme --name widget --author alice -o ${tempFile.absolutePath}",
                )

            result.statusCode shouldBe 0
            result.stdout shouldContain "Vulnlog file created at:"
            tempFile.readText() shouldContain "acme"
        } finally {
            tempFile.delete()
        }
    }

    test("init fails when required options are missing") {
        val result = InitCommand().test("")

        result.statusCode shouldBe 1
        result.stderr shouldContain "missing"
    }
})
