// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument

object OpenVexWriter {
    fun write(document: OpenVexDocument): String = openVexJson.writeValueAsString(OpenVexMapper.toDto(document)) + "\n"
}
