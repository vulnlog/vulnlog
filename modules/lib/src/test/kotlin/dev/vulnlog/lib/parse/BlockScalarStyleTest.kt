// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.VulnlogFileRaw
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

private val MIXED_STYLES_YAML =
    """
    literal_clip: |
      clip literal value
    literal_strip: |-
      strip literal value
    folded_clip: >
      folded clip value
    folded_strip: >-
      folded strip value
    plain: just a plain scalar
    quoted: "quoted scalar"
    """.trimIndent() + "\n"

class BlockScalarStyleTest :
    FunSpec({

        test("maps each block indicator to its style, keyed by the trimmed value") {
            val styles = detectBlockScalarStyles(VulnlogFileRaw(MIXED_STYLES_YAML))

            styles["clip literal value"] shouldBe BlockScalarStyle.LITERAL
            styles["strip literal value"] shouldBe BlockScalarStyle.LITERAL_STRIP
            styles["folded clip value"] shouldBe BlockScalarStyle.FOLDED
            styles["folded strip value"] shouldBe BlockScalarStyle.FOLDED_STRIP
        }

        test("ignores plain and quoted scalars") {
            val styles = detectBlockScalarStyles(VulnlogFileRaw(MIXED_STYLES_YAML))

            styles shouldNotContainKey "just a plain scalar"
            styles shouldNotContainKey "quoted scalar"
        }

        test("returns an empty map for blank content") {
            detectBlockScalarStyles(VulnlogFileRaw("")) shouldBe emptyMap()
            detectBlockScalarStyles(VulnlogFileRaw("   \n")) shouldBe emptyMap()
        }
    })
