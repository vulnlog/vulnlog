package dev.vulnlog.suppression

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SuppressionTokenReplacerTest : FunSpec({

    val tokenReplacer = SuppressionTokenReplacer()

    test("empty template should not change with empty tokens") {
        val result = tokenReplacer.replaceTokens(SuppressionTokenData("", emptyMap()))

        result shouldBe ""
    }

    test("empty template should not change with non-empty tokens") {
        val result = tokenReplacer.replaceTokens(SuppressionTokenData("", mapOf("foo" to "bar")))

        result shouldBe ""
    }

    test("template with tokens should not change with empty tokens") {
        val result = tokenReplacer.replaceTokens(SuppressionTokenData("foo bar {{ foo }} baz", emptyMap()))

        result shouldBe "foo bar  baz"
    }

    test("template with tokens should not change with nom-matching tokens") {
        val result = tokenReplacer.replaceTokens(SuppressionTokenData("foo bar {{ baz }} baz", mapOf("foo" to "bar")))

        result shouldBe "foo bar  baz"
    }

    test("template with tokens should replace only matching token within brackets") {
        val result = tokenReplacer.replaceTokens(SuppressionTokenData("foo bar {{ foo }} baz", mapOf("foo" to "baz")))

        result shouldBe "foo bar baz baz"
    }

    test("template with tokens should replace only first matching token within brackets") {
        val template = "foo bar {{ foo }} bar {{ foo }} bar"
        val tokens = mapOf("foo" to "baz")

        val result = tokenReplacer.replaceTokens(SuppressionTokenData(template, tokens))

        result shouldBe "foo bar baz bar {{ foo }} bar"
    }

    test("multiline template with tokens should replace all matching tokens") {
        val template =
            """
            |abc {{ def }} ghi
            |  jkl mno {{ pqr }}
            |    {{ stu }} vwx
            |yz
            """.trimMargin()
        val tokens =
            mapOf(
                "foo" to "baz",
                "def" to "123",
                "pqr" to "456",
                "stu" to "789",
            )
        val expected =
            """
            |abc 123 ghi
            |  jkl mno 456
            |    789 vwx
            |yz
            """.trimMargin()

        val result = tokenReplacer.replaceTokens(SuppressionTokenData(template, tokens))

        result shouldBe expected
    }

    test("a template line with only token that is not translated should be removed") {
        val template =
            """
            |abc {{ def }} ghi
            |  {{ pqr }}
            |    {{ stu }} vwx
            |yz
            """.trimMargin()
        val tokens =
            mapOf(
                "foo" to "baz",
                "def" to "123",
                "stu" to "789",
            )
        val expected =
            """
            |abc 123 ghi
            |    789 vwx
            |yz
            """.trimMargin()

        val result = tokenReplacer.replaceTokens(SuppressionTokenData(template, tokens))

        result shouldBe expected
    }
})
