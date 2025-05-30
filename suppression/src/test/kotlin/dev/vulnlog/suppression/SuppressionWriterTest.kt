package dev.vulnlog.suppression

import dev.vulnlog.common.DefaultReleaseBranchDataImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private class DummyWriter(var filename: String = "", var content: String = "") : OutputWriter {
    override fun writeText(data: OutputData) {
        filename = data.filename
        content = data.content.joinToString("\n")
    }
}

class SuppressionWriterTest : FunSpec({
    val simpleSuppression =
        SuppressionRecord(
            "simpleTemplate.txt",
            mapOf(DefaultReleaseBranchDataImpl to listOf("foo", "  bar", "    baz")),
        )
    val simpleTemplate =
        mapOf(
            SuppressionFileInfo("simpleTemplate", "txt") to listOf("HEAD", "  vulnlogEntries", "FOOT"),
        )

    test("empty templates and empty suppressions lead to no output") {
        val outputWriter = DummyWriter()
        val writer = SuppressionWriter(outputWriter)

        writer.writeSuppression(emptyMap(), emptySet())

        val expectedFilename = ""
        val expectedContent = ""

        outputWriter.filename shouldBe expectedFilename
        outputWriter.content shouldBe expectedContent
    }

    test("empty suppressions lead to no output") {
        val outputWriter = DummyWriter()
        val writer = SuppressionWriter(outputWriter)

        writer.writeSuppression(simpleTemplate, emptySet())

        val expectedFilename = ""
        val expectedContent = ""

        outputWriter.filename shouldBe expectedFilename
        outputWriter.content shouldBe expectedContent
    }

    test("empty templates lead to no output") {
        val outputWriter = DummyWriter()
        val writer = SuppressionWriter(outputWriter)

        writer.writeSuppression(emptyMap(), setOf(simpleSuppression))

        val expectedFilename = ""
        val expectedContent = ""

        outputWriter.filename shouldBe expectedFilename
        outputWriter.content shouldBe expectedContent
    }

    test("no output when template filename does not match") {
        val outputWriter = DummyWriter()
        val writer = SuppressionWriter(outputWriter)

        writer.writeSuppression(simpleTemplate, setOf(SuppressionRecord("simpleTemplate.txt", emptyMap())))

        val expectedFilename = ""
        val expectedContent = ""

        outputWriter.filename shouldBe expectedFilename
        outputWriter.content shouldBe expectedContent
    }

    test("correct output") {
        val outputWriter = DummyWriter()
        val writer = SuppressionWriter(outputWriter)

        writer.writeSuppression(simpleTemplate, setOf(simpleSuppression))

        val expectedFilename = "simpletemplate-default-release-branch.txt"
        val expectedContent =
            """
            |HEAD
            |  foo
            |    bar
            |      baz
            |FOOT
            """.trimMargin()

        outputWriter.filename shouldBe expectedFilename
        outputWriter.content shouldBe expectedContent
    }

    test("empty lines in template are not removed") {
        val outputWriter = DummyWriter()
        val writer = SuppressionWriter(outputWriter)

        writer.writeSuppression(
            mapOf(
                SuppressionFileInfo("simpleTemplate", "txt") to listOf("", "HEAD", "  vulnlogEntries", "FOOT", ""),
            ),
            setOf(simpleSuppression),
        )

        val expectedFilename = "simpletemplate-default-release-branch.txt"
        val expectedContent =
            """
            |
            |HEAD
            |  foo
            |    bar
            |      baz
            |FOOT
            |
            """.trimMargin()

        outputWriter.filename shouldBe expectedFilename
        outputWriter.content shouldBe expectedContent
    }
})
