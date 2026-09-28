// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Orders the entries by what the versions mean, so the assertions do not lean on the declaration order. */
private val BY_VERSION =
    compareBy<OpenVexFormatVersion>(
        { it.version.substringBefore(".").toInt() },
        {
            it.version
                .substringAfter(".")
                .substringBefore(".")
                .toInt()
        },
        { it.version.substringAfterLast(".").toInt() },
    )

class OpenVexFormatVersionTest :
    FunSpec({

        context("LATEST") {

            test("is the newest version this build knows, whatever the declaration order") {
                val entries = OpenVexFormatVersion.entries

                val latest = OpenVexFormatVersion.LATEST

                latest shouldBe entries.maxWith(BY_VERSION)
            }
        }
    })
