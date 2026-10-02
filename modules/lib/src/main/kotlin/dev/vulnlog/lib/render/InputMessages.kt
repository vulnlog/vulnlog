// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.document.InputRead

fun renderInputFailure(failed: InputRead.Failed): Failure {
    val (reason, hint) =
        when (failed) {
            is InputRead.Missing -> "it does not exist" to "check the path"
            is InputRead.Denied -> "permission denied" to "make the file readable for this user"
            is InputRead.Unreadable -> failed.reason to "pass a readable Vulnlog file"
        }
    return Failure("cannot read ${failed.source}: $reason", hint)
}
