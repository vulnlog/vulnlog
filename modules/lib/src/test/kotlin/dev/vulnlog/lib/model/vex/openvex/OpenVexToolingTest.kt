// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OpenVexToolingTest :
    FunSpec({

        test("correct tooling value for CLI and version 0.18.0") {
            OpenVexTooling("CLI", "0.18.0").value shouldBe "Vulnlog CLI version 0.18.0, https://vulnlog.dev/"
        }
    })
