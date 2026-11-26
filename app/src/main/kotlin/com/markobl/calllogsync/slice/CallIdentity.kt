package com.markobl.calllogsync.slice

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/** Source identity is independent of mutable cached contact labels and display timezone. */
class CallIdentity private constructor() {
    companion object {
        @JvmStatic
        fun signature(call: CallRecord?): String {
            val source = call!!
            val number = source.number?.let { "${it.length}:$it" } ?: "-1:"
            val direction = "${source.direction.length}:${source.direction}"
            val app =
                if (source.source == CallRecord.PHONE) "" else "|app:${source.source.orEmpty()}"
            val material =
                "source-v1|${source.key()}|${source.start}|${source.seconds}|${source.type}|" +
                    "$direction|${source.identityAllowed}|$number$app"
            try {
                val bytes =
                    MessageDigest.getInstance("SHA-256")
                        .digest(material.toByteArray(StandardCharsets.UTF_8))
                return bytes.toHexString()
            } catch (e: NoSuchAlgorithmException) {
                throw AssertionError(e)
            }
        }
    }
}
