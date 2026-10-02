// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import java.io.IOException
import java.nio.file.FileSystemException

/** A [FileSystemException]'s message repeats the path, which the caller already names. */
internal fun IOException.reason(): String =
    if (this is FileSystemException) reason ?: javaClass.simpleName else message ?: javaClass.simpleName
