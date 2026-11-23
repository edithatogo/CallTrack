#!/usr/bin/env -S uv run python
"""Fail-closed independent canonical-byte audit of new Gradle checksum entries.

This is NOT a release signature, licence audit, vulnerability assessment or
approval of code. Only the difference from a previously checked-in policy is
admitted. Existing records can never be deleted or silently changed.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
from urllib.error import HTTPError

from audit_gradle_verification import module_artifact_url, rows
from audit_release_artifacts import PUBLICATION_OPENER, read_publication, repository_url


MAX_CANONICAL_ARTIFACT = 256 * 1024 * 1024
BLOCK_BYTES = 1024 * 1024


def canonical_sha256(url):
    """Hash source bytes without buffering a >64 MB compiler/tool archive."""
    if not url.startswith("https://"):
        raise ValueError("Canonical artifact URL must be HTTPS")
    digest = hashlib.sha256()
    size = 0
    with PUBLICATION_OPENER.open(url, timeout=60) as response:
        if not getattr(response, "url", url).startswith("https://"):
            raise ValueError("Non-HTTPS publication redirect")
        while True:
            block = response.read(BLOCK_BYTES)
            if not block:
                break
            size += len(block)
            if size > MAX_CANONICAL_ARTIFACT:
                raise ValueError("Oversized canonical tool archive: " + url)
            digest.update(block)
    return digest.hexdigest()


def pending(prior_bytes, candidate_bytes):
    old = {(r["coordinate"], r["name"]): set(r["sha256"]) for r in rows(prior_bytes)}
    current = {(r["coordinate"], r["name"]): set(r["sha256"]) for r in rows(candidate_bytes)}
    for key, approved in old.items():
        if key not in current or not approved <= current[key]:
            raise ValueError("Existing trust entries may not be removed or changed: " + repr(key))
        if current[key] != approved:
            raise ValueError("Conflicting hashes for previously approved artifact: " + repr(key))
    return [
        dict(coordinate=key[0], name=key[1], sha256=next(iter(hashes)))
        for key, hashes in sorted(current.items())
        if key not in old and len(hashes) == 1
    ], current


def verify_one(entry, approved):
    candidate = dict(entry)
    primary = repository_url(candidate)
    attempts = []
    try:
        actual = canonical_sha256(primary)
        attempts.append(dict(url=primary, sha256=actual))
        if actual == entry["sha256"]:
            return dict(entry, canonical_url=primary, sha256_verified=True)
        raise ValueError("Canonical hash mismatch: " + primary)
    except HTTPError as failure:
        if failure.code != 404:
            raise
    group, module, version = entry["coordinate"].split(":")
    base = primary.rsplit("/", 1)[0] + "/"
    metadata_url = base + module + "-" + version + ".module"
    metadata = read_publication(metadata_url)
    metadata_sha = hashlib.sha256(metadata).hexdigest()
    if metadata_sha not in approved.get((entry["coordinate"], module + "-" + version + ".module"), set()):
        raise ValueError("Missing independently verified variant metadata: " + metadata_url)
    variant_url = module_artifact_url(candidate, json.loads(metadata))
    actual = canonical_sha256(variant_url)
    if actual != entry["sha256"]:
        raise ValueError("Variant artifact mismatch: " + variant_url)
    return dict(entry, canonical_url=variant_url, sha256_verified=True, module_sha256=metadata_sha)


def audit(old_path, candidate_path, output_path):
    original = old_path.read_bytes()
    prospective = candidate_path.read_bytes()
    todo, map_approved = pending(original, prospective)
    with ThreadPoolExecutor(max_workers=4) as executor:
        results = list(executor.map(lambda item: verify_one(item, map_approved), todo))
    receipt = {
        "old_policy_sha256": hashlib.sha256(original).hexdigest(),
        "new_policy_sha256": hashlib.sha256(prospective).hexdigest(),
        "new_artifacts": len(results),
        "all_independently_checked": all(x["sha256_verified"] for x in results),
        "checks": results,
        "release_ready": False,
    }
    output_path.write_text(json.dumps(receipt, sort_keys=True, indent=2) + "\n")
    return receipt


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--old", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    opts = parser.parse_args()
    result = audit(opts.old, opts.candidate, opts.receipt)
    print(json.dumps({key: value for key, value in result.items() if key != "checks"}))
    if not result["all_independently_checked"]:
        raise SystemExit(1)
