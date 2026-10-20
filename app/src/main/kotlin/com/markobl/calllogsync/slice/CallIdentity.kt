package com.markobl.calllogsync.slice

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

/** Source identity is independent of mutable cached contact labels and display timezone. */
class CallIdentity private constructor() {
    companion object {
        @JvmStatic
        fun signature(call: CallRecord?): String {
            val source = call!!
            val number = source.number?.let { "${it.length}:$it" } ?: "-1:"
            val material =
                "source-v1|${source.key()}|${source.start}|${source.seconds}|${source.type}|" +
                    "${source.direction.length}:${source.direction}|${source.identityAllowed}|$number"
            try {
                val bytes =
                    MessageDigest.getInstance("SHA-256")
                        .digest(material.toByteArray(StandardCharsets.UTF_8))
                return buildString {
                    for (value in bytes) append(
                        String.format(Locale.ROOT, "%02x", value.toInt() and 255)
                    )
                }
            } catch (e: NoSuchAlgorithmException) {
                throw AssertionError(e)
            }
        }
    }
}
