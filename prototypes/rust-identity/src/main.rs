use calltrack_identity_experiment::signature;
use std::{hint::black_box, time::Instant};

fn main() {
    let calls = [
        ("fixture", "1", 1234, 0, 1, true, Some("000")),
        ("ns😀", "id:|", 1700000000123, 65, 2, true, Some(" 000 ")),
        ("fixture", "3", 1000, 7, 3, false, Some("123")),
        ("fixture", "4", 999, 1, 5, true, None),
    ];
    let mut checksum: u64 = 0;
    let mut run = |count: usize| {
        let start = Instant::now();
        for i in 0..count {
            let (ns, id, date, seconds, kind, allowed, number) = black_box(calls[i % 4]);
            let result =
                black_box(signature(ns, id, date, seconds, kind, allowed, number).unwrap());
            checksum += u64::from(result.as_bytes()[0]);
        }
        start.elapsed().as_nanos()
    };
    run(10000);
    let samples: Vec<_> = (0..5).map(|_| run(10000)).collect();
    println!(
        "{{\"kind\":\"rust\",\"warmup\":10000,\"iterations_per_sample\":10000,\"samples_ns\":{samples:?},\"checksum\":{checksum}}}"
    );
}
