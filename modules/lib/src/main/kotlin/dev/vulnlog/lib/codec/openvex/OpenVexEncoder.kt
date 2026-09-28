// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument

/** Encodes a document to the bytes of its format version, ending in a newline. No I/O. */
object OpenVexEncoder {
    fun encode(document: OpenVexDocument): String = openVexJson.writeValueAsString(OpenVexMapper.toDto(document)) + "\n"
}
