// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class TextWrapTest :
    FunSpec({

        context("breakWord") {
            test("cuts after the last hyphen that fits") {
                val pieces = breakWord("SNYK-JAVA-COMFASTERXMLJACKSONCORE-3038424", 24)

                pieces shouldContainExactly listOf("SNYK-JAVA-", "COMFASTERXMLJACKSONCORE-", "3038424")
            }

            test("cuts at the width when no hyphen fits") {
                val pieces = breakWord("abcdefghij", 4)

                pieces shouldContainExactly listOf("abcd", "efgh", "ij")
            }
        }

        context("fillLines") {
            test("ends a continued line with the separator without its blanks") {
                val text = fillLines(listOf("as of 1.1.0", "tags production", "reporter trivy"), "; ", 30)

                text shouldBe "as of 1.1.0; tags production;\nreporter trivy"
            }

            test("keeps a line with its end mark within the width") {
                val text = fillLines(listOf("aaaa", "bbbb", "cc"), "; ", 10)

                text shouldBe "aaaa;\nbbbb; cc"
            }
        }

        context("wrapWords") {
            test("fills lines at the blanks and breaks a word longer than the width") {
                val text = wrapWords("1.0.0, 1.1.0, 1.2.0-SNAPSHOT-BUILD", 13)

                text shouldBe "1.0.0, 1.1.0,\n1.2.0-\nSNAPSHOT-\nBUILD"
            }
        }
    })
