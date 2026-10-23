// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.document.validated
import dev.vulnlog.lib.fixtures.vulnlogDocument
import dev.vulnlog.lib.render.renderSuppressionFailure
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.xml.sax.InputSource
import tools.jackson.databind.json.JsonMapper
import java.io.StringReader
import java.time.LocalDate
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class DependencyCheckSuppressionGenerationTest :
    FunSpec({

        test("accepted YAML escapes incompatible with XML fail native output and survive generic JSON") {
            val forbidden = (0..0x1F).filterNot { it in listOf(0x9, 0xA, 0xD) } + listOf(0xFFFE, 0xFFFF)
            forbidden.forEach { codePoint ->
                val escaped = "before\\u${codePoint.toString(16).padStart(4, '0')}after"

                val native = generateFromYaml(escaped)
                val generic =
                    generateFromYaml(escaped, SuppressionFormatRequest.Generic)
                        .shouldBeInstanceOf<SuppressionOutcome.Generated>()
                        .files
                        .single()

                val failed = native.shouldBeInstanceOf<SuppressionOutcome.Failed>()
                val character = "U+" + codePoint.toString(16).uppercase().padStart(4, '0')
                renderSuppressionFailure(failed, "-o", "--reporter", "--output-dir").single().message shouldBe
                    "cannot encode analysis for CVE-2026-1234 as Dependency-Check XML: " +
                    "character $character is not allowed in XML 1.0"
                generic.fileName shouldBe "dependency-check.generic.json"
                JsonMapper.shared().readTree(generic.content)["vulnerabilities"][0]["statement"].stringValue() shouldBe
                    "before${codePoint.toChar()}after"
            }
        }

        test("accepted YAML escapes for XML character boundaries survive parsing and schema validation") {
            val escaped = "before\\t\\n\\r\\u0020\\uD7FF\\uE000\\uFFFD\\U00010000\\U0001F512\\U0010FFFFafter"
            val expected =
                "before\t\n\r \uD7FF\uE000\uFFFD" +
                    String(Character.toChars(0x10000)) + "🔒" + String(Character.toChars(0x10FFFF)) + "after"

            val xml =
                generateFromYaml(escaped)
                    .shouldBeInstanceOf<SuppressionOutcome.Generated>()
                    .files
                    .single()
                    .content
            val schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            schema.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            schema.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            val resource =
                DependencyCheckSuppressionGenerationTest::class.java
                    .getResource("/suppress/dependency-suppression.1.3.xsd")!!
            schema.newSchema(resource).newValidator().validate(StreamSource(StringReader(xml)))
            val parser = DocumentBuilderFactory.newInstance()
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            val document = parser.newDocumentBuilder().parse(InputSource(StringReader(xml)))

            document.getElementsByTagName("notes").item(0).textContent shouldBe expected
            document.getElementsByTagName("suppress").length shouldBe 1
        }
    })

private fun generateFromYaml(
    escapedAnalysis: String,
    format: SuppressionFormatRequest = SuppressionFormatRequest.Auto,
): SuppressionOutcome {
    val yaml =
        vulnlogDocument(reporter = "dependency-check")
            .replace("    verdict:", "    analysis: \"$escapedAnalysis\"\n    verdict:")
    val project = validated(yaml)
    return generateSuppressions(
        project,
        SuppressionRequest(FilterRequest(), format, LocalDate.of(2026, 4, 3), singleFile = false),
    )
}
