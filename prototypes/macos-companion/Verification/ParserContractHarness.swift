import Foundation

@main
struct ParserContractHarness {
    static func main() throws {
        let exportID = "00000000-0000-4000-8000-000000000001"
        let callID = "00000000-0000-4000-8000-000000000002"
        func document(_ calls: String, version: Int = 1) -> Data {
            Data("{\"schema_version\":\(version),\"export_id\":\"\(exportID)\",\"calls\":[\(calls)]}".utf8)
        }
        let record = "{\"id\":\"\(callID)\",\"direction\":\"missed\",\"started_at_ms\":1800000000000,\"duration_seconds\":0}"
        let preview = try CallLogImporter.preview(data: document(record))
        precondition(preview.calls.count == 1 && preview.calls[0].direction == .missed)

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

        let tooManyCalls = Array(repeating: record, count: CallLogImporter.maximumCalls + 1)
            .joined(separator: ",")
        do {
            _ = try CallLogImporter.preview(data: document(tooManyCalls))
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

        print("PASS: preview, identity, version, timestamp, duration, count, and size contracts")
    }
}
