// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.throwable.shouldHaveMessage

class OpenVexToolingTest :
    FunSpec({

        test("throw when the platform is blank") {
            shouldThrow<IllegalArgumentException> {
                OpenVexTooling(" ", "0.18.0")
            }.shouldHaveMessage("VEX tooling platform must not be blank")
        }

        test("throw when the version is blank") {
            shouldThrow<IllegalArgumentException> {
                OpenVexTooling("CLI", "")
            }.shouldHaveMessage("VEX tooling version must not be blank")
        }
    })
