package org.calltrack.benchmark

import com.markobl.calllogsync.slice.CallIdentity
import com.markobl.calllogsync.slice.CallRecord

fun main() {
    val calls =
        arrayOf(
            CallRecord("fixture", "1", 1234, 0, 1, "Cached", "000", true, "SIM"),
            CallRecord("ns😀", "id:|", 1700000000123L, 65, 2, "\u2003", " 000 ", true, null),
            CallRecord("fixture", "3", 1000, 7, 3, "Secret", "123", false, null),
            CallRecord("fixture", "4", 999, 1, 5, " \t\r\n", null, true, null),
        )
    val golden =
        arrayOf(
            "9f344402a6d4cc91192a65df83af1c9a7dd1cd04684b826b9fd33dde3e3a1bc9",
            "8d747459d88eada6087787805de80e6e5844e85bf7443669510bc1ce67d310c2",
            "49eb18c0a30e7e32f76f1eb2c1bab7a19b86c82b068abc2bffba71871226a6fa",
            "1b364cde79cd30cbd2410da6ca46f8ea4a3b87ee0cb12b936eb3a851f2708c2b",
        )
    calls.forEachIndexed { i, call -> check(CallIdentity.signature(call) == golden[i]) }
    var checksum = 0L
    repeat(10000) { checksum += CallIdentity.signature(calls[it % 4])[0].code }
    val samples =
        List(5) {
            val start = System.nanoTime()
            repeat(10000) { checksum += CallIdentity.signature(calls[it % 4])[0].code }
            System.nanoTime() - start
        }
    println(
        "{\"kind\":\"jvm\",\"goldens\":4,\"warmup\":10000,\"iterations_per_sample\":10000,\"samples_ns\":${samples},\"checksum\":$checksum}"
    )
}
