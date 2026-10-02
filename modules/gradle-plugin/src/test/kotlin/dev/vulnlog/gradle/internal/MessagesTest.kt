// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.render.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.logging.Logger
import java.lang.reflect.Proxy

/** Gradle's [Logger] has too many methods for a hand-written fake, so a proxy records the level and the text. */
private fun recordingLogger(calls: MutableList<Pair<String, String>>): Logger =
    Proxy.newProxyInstance(Logger::class.java.classLoader, arrayOf(Logger::class.java)) { _, method, args ->
        calls += method.name to args?.firstOrNull().toString()
        null
    } as Logger

class MessagesTest :
    FunSpec({

        test("log sends each message to the Gradle level that stands for its verbosity") {
            val calls = mutableListOf<Pair<String, String>>()
            val logger = recordingLogger(calls)
            val messages =
                listOf(
                    Message.Status("Wrote: vex.json"),
                    Message.Warning("releases without purls are left out"),
                    Message.Verbose("collected 1 statement"),
                    Message.Debug("skipped CVE-2026-0001"),
                )

            messages.forEach { logger.log(it) }

            calls shouldBe
                listOf(
                    "lifecycle" to "Wrote: vex.json",
                    "warn" to "warning: releases without purls are left out",
                    "info" to "collected 1 statement",
                    "debug" to "skipped CVE-2026-0001",
                )
        }
    })
