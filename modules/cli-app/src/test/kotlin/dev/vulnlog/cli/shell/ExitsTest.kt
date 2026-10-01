// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

class ExitsTest :
    FunSpec({

        test("an absent baseline is a bad flag value, an unreadable one an I/O error") {
            val unavailable = listOf(OpenVexBaselineRead.Absent, OpenVexBaselineRead.Unreadable("Permission denied"))

            val codes = unavailable.map { exitCode(it) }

            codes shouldContainExactly listOf(ExitCode.INVALID_FLAG_VALUE, ExitCode.GENERAL_ERROR)
        }
    })
