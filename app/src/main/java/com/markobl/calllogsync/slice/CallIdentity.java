package com.markobl.calllogsync.slice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Source identity is independent of mutable cached contact labels and display timezone. */
public final class CallIdentity {
    private CallIdentity() {}

    public static String signature(CallRecord call) {
        String number = call.number == null ? "-1:" : call.number.length() + ":" + call.number;
        String material =
                call.key()
                        + "|"
                        + call.start
                        + "|"
                        + call.seconds
                        + "|"
                        + call.direction.length()
                        + ":"
                        + call.direction
                        + "|"
                        + call.identityAllowed
                        + "|"
                        + number;
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
