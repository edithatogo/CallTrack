import CryptoKit
import Foundation

@main
struct ParserContractHarness {
    static func main() throws {
        let exportID = "00000000-0000-4000-8000-000000000001"
        let callID = "00000000-0000-4000-8000-000000000002"
        func payload(_ calls: String, version: Int = 1, count: Int? = nil) -> String {
            let actualCount = count ?? (calls.isEmpty ? 0 : calls.components(separatedBy: "},{").count)
            return "{\"schema_version\":\(version),\"export_id\":\"\(exportID)\",\"record_count\":\(actualCount),\"calls\":[\(calls)]}"
        }
        func document(_ calls: String, version: Int = 1, count: Int? = nil, checksum: String? = nil) -> Data {
            let body = payload(calls, version: version, count: count)
            let digest = SHA256.hash(data: Data(body.utf8)).map { String(format: "%02x", $0) }.joined()
            return Data((String(body.dropLast()) + ",\"content_sha256\":\"\(checksum ?? digest)\"}").utf8)
        }
        let record = "{\"id\":\"\(callID)\",\"direction\":\"missed\",\"started_at_ms\":1800000000000,\"duration_seconds\":0}"
        let preview = try CallLogImporter.preview(data: document(record))
        precondition(preview.calls.count == 1 && preview.calls[0].direction == .missed)

        if CommandLine.arguments.count > 1 {
            let golden = try Data(contentsOf: URL(fileURLWithPath: CommandLine.arguments[1]))
            let goldenPreview = try CallLogImporter.preview(data: golden)
            precondition(goldenPreview.calls.count == 2)
        }

        do {
            _ = try CallLogImporter.preview(data: document("\(record),\(record)"))
            fatalError("duplicate identity was accepted")
        } catch ImportFailure.duplicateCallID { }

        do {
            _ = try CallLogImporter.preview(data: document("", version: 2))
            fatalError("unsupported version was accepted")
        } catch ImportFailure.unsupportedVersion(2) { }

        let negativeTimestamp = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":-1,\"duration_seconds\":0}"
        do {
            _ = try CallLogImporter.preview(data: document(negativeTimestamp))
            fatalError("negative timestamp was accepted")
        } catch ImportFailure.invalidTimestamp { }

        let timestampOverflow = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":9223372036854775807,\"duration_seconds\":1}"
        do {
            _ = try CallLogImporter.preview(data: document(timestampOverflow))
            fatalError("timestamp overflow was accepted")
        } catch ImportFailure.invalidTimestamp { }

        let tooManyCalls = Array(repeating: record, count: CallLogImporter.maximumCalls + 1).joined(separator: ",")
        do {
            _ = try CallLogImporter.preview(data: document(tooManyCalls, count: CallLogImporter.maximumCalls + 1))
            fatalError("too many calls were accepted")
        } catch ImportFailure.tooManyCalls { }

        do {
            _ = try CallLogImporter.preview(data: Data(repeating: 0, count: CallLogImporter.maximumFileBytes + 1))
            fatalError("oversized document was accepted")
        } catch ImportFailure.fileTooLarge { }

        let overflow = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":9223372036854775807}"
        do {
            _ = try CallLogImporter.preview(data: document(overflow))
            fatalError("overflowing duration was accepted")
        } catch ImportFailure.invalidDuration { }

        do {
            _ = try CallLogImporter.preview(data: document(record, count: 2))
            fatalError("record-count mismatch was accepted")
        } catch ImportFailure.recordCountMismatch { }

        do {
            _ = try CallLogImporter.preview(data: document(record, checksum: String(repeating: "0", count: 64)))
            fatalError("checksum mismatch was accepted")
        } catch ImportFailure.checksumMismatch { }

        let canonical = String(decoding: document(record), as: UTF8.self)
        _ = try CallLogImporter.preview(data: Data((canonical + "\r\n").utf8))
        do {
            _ = try CallLogImporter.preview(data: Data((canonical + "\n\n").utf8))
            fatalError("multiple trailing line endings were accepted")
        } catch ImportFailure.nonCanonicalDocument { }

        let duplicateKey = canonical.replacingOccurrences(
            of: "\"schema_version\":1,",
            with: "\"schema_version\":1,\"schema_version\":1,"
        )
        do {
            _ = try CallLogImporter.preview(data: Data(duplicateKey.utf8))
            fatalError("duplicate JSON key was accepted")
        } catch ImportFailure.nonCanonicalDocument { }

        let unknownField = canonical.replacingOccurrences(
            of: "\"schema_version\":1,",
            with: "\"schema_version\":1,\"unexpected\":true,"
        )
        do {
            _ = try CallLogImporter.preview(data: Data(unknownField.utf8))
            fatalError("unknown JSON field was accepted")
        } catch ImportFailure.nonCanonicalDocument { }

        print("PASS: bounded preview, golden interchange, canonical JSON, identity, checksum, timestamp, duration, count, and size contracts")
    }
}
