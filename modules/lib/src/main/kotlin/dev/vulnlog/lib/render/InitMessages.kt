// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.InitOutcome

/** [target] and [forceOption] come from the driver: the use case only knows whether the target exists. */
fun renderInitFailure(
    failed: InitOutcome.Failed,
    target: String,
    forceOption: String,
): Failure =
    when (failed) {
        InitOutcome.AlreadyExists -> Failure("the file $target already exists", "pass $forceOption to replace it")
    }
