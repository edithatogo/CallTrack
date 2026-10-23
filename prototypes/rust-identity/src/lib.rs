/// Prototype subset: valid Unicode and the raw-type CallRecord constructor only.
/// No production/FFI use: Java unpaired-surrogate and legacy-direction parity is unproved.
pub fn signature(
    namespace: &str,
    id: &str,
    start: i64,
    seconds: i64,
    raw_type: i32,
    allowed: bool,
    number: Option<&str>,
) -> Result<String, &'static str> {
    use sha2::{Digest, Sha256};
    if namespace.is_empty() || id.is_empty() || start < 0 || seconds < 0 {
        return Err("Invalid call identity/time");
    }
    let direction = match raw_type {
        1 => "Incoming",
        2 => "Outgoing",
        3 => "Missed",
        _ => "Unknown",
    };
    // Persisted JVM keys count UTF-16 code units, not UTF-8 bytes or scalar values.
    let key = format!(
        "{}:{}{}:{}",
        namespace.encode_utf16().count(),
        namespace,
        id.encode_utf16().count(),
        id
    );
    let number = number.map_or_else(
        || "-1:".to_owned(),
        |n| format!("{}:{}", n.encode_utf16().count(), n),
    );
    let material = format!(
        "source-v1|{key}|{start}|{seconds}|{raw_type}|{}:{direction}|{allowed}|{number}",
        direction.len()
    );
    let digest = Sha256::digest(material.as_bytes());
    use std::fmt::Write;
    let mut result = String::with_capacity(64);
    for byte in digest {
        write!(&mut result, "{byte:02x}").expect("String formatting cannot fail");
    }
    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn raw_constructor_java_golden_signatures() {
        let fixtures = [
            (
                "fixture",
                "1",
                1234,
                0,
                1,
                true,
                Some("000"),
                "9f344402a6d4cc91192a65df83af1c9a7dd1cd04684b826b9fd33dde3e3a1bc9",
            ),
            (
                "ns😀",
                "id:|",
                1700000000123,
                65,
                2,
                true,
                Some(" 000 "),
                "8d747459d88eada6087787805de80e6e5844e85bf7443669510bc1ce67d310c2",
            ),
            (
                "fixture",
                "3",
                1000,
                7,
                3,
                false,
                Some("123"),
                "49eb18c0a30e7e32f76f1eb2c1bab7a19b86c82b068abc2bffba71871226a6fa",
            ),
            (
                "fixture",
                "4",
                999,
                1,
                5,
                true,
                None,
                "1b364cde79cd30cbd2410da6ca46f8ea4a3b87ee0cb12b936eb3a851f2708c2b",
            ),
        ];
        for (ns, id, start, seconds, kind, allowed, number, expected) in fixtures {
            assert_eq!(
                signature(ns, id, start, seconds, kind, allowed, number).unwrap(),
                expected
            );
        }
    }

    #[test]
    fn rejects_invalid_identity_and_time() {
        for (ns, id, start, seconds) in [
            ("", "1", 0, 0),
            ("n", "", 0, 0),
            ("n", "1", -1, 0),
            ("n", "1", 0, -1),
        ] {
            assert!(signature(ns, id, start, seconds, 1, true, None).is_err());
        }
    }

    #[test]
    fn absent_number_and_empty_number_are_distinct() {
        assert_ne!(
            signature("n", "1", 0, 0, 1, true, None),
            signature("n", "1", 0, 0, 1, true, Some(""))
        );
    }
}
