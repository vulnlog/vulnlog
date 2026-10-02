// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.model.OutputWrite

fun renderWriteFailure(failed: OutputWrite.Failed): Failure {
    val (reason, hint) =
        when (failed) {
            is OutputWrite.MissingDirectory -> "its directory does not exist" to "create the directory first"
            is OutputWrite.Denied -> "permission denied" to "make the location writable for this user"
            is OutputWrite.Unwritable -> failed.reason to "pass a writable file path"
        }
    return Failure("cannot write ${failed.target}: $reason", hint)
}
