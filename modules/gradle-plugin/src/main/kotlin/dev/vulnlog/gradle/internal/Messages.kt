// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.formatMessage
import org.gradle.api.logging.Logger

/**
 * Gradle's log levels take the place of the CLI's `-q`, `-v` and `-vv`, so `gradle --info` and `--debug` show the
 * details without extra task properties, and the CLI's `verbose:` and `debug:` prefixes are left out.
 */
fun Logger.log(message: Message) =
    when (message) {
        is Message.Status -> lifecycle(message.text)
        is Message.Warning -> warn(formatMessage(FindingSeverity.WARNING, message.text))
        is Message.Verbose -> info(message.text)
        is Message.Debug -> debug(message.text)
    }
