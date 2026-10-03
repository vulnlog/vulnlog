// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.InitOutcome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InitMessagesTest :
    FunSpec({

        test("renderInitFailure names the existing file and the driver's option to replace it") {
            val failure = renderInitFailure(InitOutcome.AlreadyExists, "vulnlog.yaml", "--force")

            failure shouldBe Failure("the file vulnlog.yaml already exists", "pass --force to replace it")
        }
    })
