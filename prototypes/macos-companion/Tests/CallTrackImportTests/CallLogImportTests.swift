import Foundation
import CryptoKit
import XCTest
@testable import CallTrackImport

final class CallLogImportTests: XCTestCase {
    private let exportID = UUID(uuidString: "00000000-0000-4000-8000-000000000001")!
    private let callID = UUID(uuidString: "00000000-0000-4000-8000-000000000002")!

    private func document(_ calls: String, version: Int = 1, count: Int? = nil, checksum: String? = nil) -> Data {
        let actualCount = count ?? (calls.isEmpty ? 0 : calls.components(separatedBy: "},{").count)
        let payload = "{\"schema_version\":\(version),\"export_id\":\"\(exportID.uuidString.lowercased())\",\"record_count\":\(actualCount),\"calls\":[\(calls)]}"
        let digest = SHA256.hash(data: Data(payload.utf8)).map { String(format: "%02x", $0) }.joined()
        return Data((String(payload.dropLast()) + ",\"content_sha256\":\"\(checksum ?? digest)\"}").utf8)
    }

    private func androidGoldenFixture() throws -> Data {
        let repoRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let fixture = repoRoot.appending(path: "app/src/test/resources/call-log-v1.json")
        return try Data(contentsOf: fixture)
    }

    func testPreviewsMinimalSyntheticRecordWithoutWriting() throws {
        let preview = try CallLogImporter.preview(data: document(
            "{\"id\":\"\(callID)\",\"direction\":\"missed\",\"started_at_ms\":1800000000000,\"duration_seconds\":0}"
        ))
        XCTAssertEqual(preview.exportID, exportID)
        XCTAssertEqual(preview.calls.count, 1)
        XCTAssertEqual(preview.calls[0].direction, .missed)
        XCTAssertEqual(preview.calls[0].durationSeconds, 0)
    }

    func testAcceptsAndroidProducerGoldenFixture() throws {
        let preview = try CallLogImporter.preview(data: androidGoldenFixture())
        XCTAssertEqual(preview.calls.count, 2)
    }

    func testRejectsEverySingleByteMutationOfAndroidGolden() throws {
        let golden = try androidGoldenFixture()
        for offset in golden.indices {
            var mutated = golden
            mutated[offset] = mutated[offset] ^ 0x01
            XCTAssertThrowsError(try CallLogImporter.preview(data: mutated), "mutation at byte \(offset)")
        }
    }

    func testRejectsRecordCountAndChecksumMismatch() {
        let record = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":1}"
        XCTAssertThrowsError(try CallLogImporter.preview(data: document(record, count: 2))) {
            XCTAssertEqual($0 as? ImportFailure, .recordCountMismatch)
        }
        XCTAssertThrowsError(try CallLogImporter.preview(data: document(record, checksum: String(repeating: "0", count: 64)))) {
            XCTAssertEqual($0 as? ImportFailure, .checksumMismatch)
        }
    }

    func testRejectsDuplicateKeysAndUnknownFields() throws {
        let record = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":1}"
        let canonical = String(decoding: document(record), as: UTF8.self)
        let duplicateKey = canonical.replacingOccurrences(
            of: "\"schema_version\":1,",
            with: "\"schema_version\":1,\"schema_version\":1,"
        )
        XCTAssertThrowsError(try CallLogImporter.preview(data: Data(duplicateKey.utf8))) {
            XCTAssertEqual($0 as? ImportFailure, .nonCanonicalDocument)
        }
        let unknownField = canonical.replacingOccurrences(
            of: "\"schema_version\":1,",
            with: "\"schema_version\":1,\"unexpected\":true,"
        )
        XCTAssertThrowsError(try CallLogImporter.preview(data: Data(unknownField.utf8))) {
            XCTAssertEqual($0 as? ImportFailure, .nonCanonicalDocument)
        }
    }

    func testAllowsOnlyOneFinalLineEnding() throws {
        let record = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":1}"
        let canonical = String(decoding: document(record), as: UTF8.self)
        XCTAssertNoThrow(try CallLogImporter.preview(data: Data((canonical + "\r\n").utf8)))
        XCTAssertThrowsError(try CallLogImporter.preview(data: Data((canonical + "\n\n").utf8))) {
            XCTAssertEqual($0 as? ImportFailure, .nonCanonicalDocument)
        }
    }

    func testRejectsUnsupportedVersionAndMalformedInput() {
        XCTAssertThrowsError(try CallLogImporter.preview(data: document("", version: 2))) {
            XCTAssertEqual($0 as? ImportFailure, .unsupportedVersion(2))
        }
        XCTAssertThrowsError(try CallLogImporter.preview(data: Data("not json".utf8))) {
            XCTAssertEqual($0 as? ImportFailure, .invalidDocument)
        }
    }

    func testRejectsDuplicateIdentityAndNegativeDuration() {
        let record = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":1}"
        XCTAssertThrowsError(try CallLogImporter.preview(data: document("\(record),\(record)"))) {
            XCTAssertEqual($0 as? ImportFailure, .duplicateCallID(callID))
        }
        let negative = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":-1}"
        XCTAssertThrowsError(try CallLogImporter.preview(data: document(negative))) {
            XCTAssertEqual($0 as? ImportFailure, .invalidDuration(callID))
        }
        let overflow = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":9223372036854775807}"
        XCTAssertThrowsError(try CallLogImporter.preview(data: document(overflow))) {
            XCTAssertEqual($0 as? ImportFailure, .invalidDuration(callID))
        }
    }

    func testRejectsNegativeAndOverflowingTimestamps() {
        let negative = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":-1,\"duration_seconds\":0}"
        XCTAssertThrowsError(try CallLogImporter.preview(data: document(negative))) {
            XCTAssertEqual($0 as? ImportFailure, .invalidTimestamp(callID))
        }
        let overflow = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":9223372036854775807,\"duration_seconds\":1}"
        XCTAssertThrowsError(try CallLogImporter.preview(data: document(overflow))) {
            XCTAssertEqual($0 as? ImportFailure, .invalidTimestamp(callID))
        }
    }

    func testRejectsMoreThanMaximumCalls() {
        let record = "{\"id\":\"\(callID)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":1}"
        let calls = Array(repeating: record, count: CallLogImporter.maximumCalls + 1)
            .joined(separator: ",")
        XCTAssertThrowsError(try CallLogImporter.preview(data: document(calls))) {
            XCTAssertEqual($0 as? ImportFailure, .tooManyCalls)
        }
    }

    func testAcceptsMaximumCallCount() throws {
        let calls = (0..<CallLogImporter.maximumCalls).map { index in
            let id = String(format: "00000000-0000-4000-8000-%012d", index)
            return "{\"id\":\"\(id)\",\"direction\":\"incoming\",\"started_at_ms\":1800000000000,\"duration_seconds\":1}"
        }.joined(separator: ",")
        let preview = try CallLogImporter.preview(data: document(calls))
        XCTAssertEqual(preview.calls.count, CallLogImporter.maximumCalls)
    }

    func testRejectsOversizedInputBeforeDecode() {
        XCTAssertThrowsError(try CallLogImporter.preview(
            data: Data(repeating: 0x20, count: CallLogImporter.maximumFileBytes + 1)
        )) {
            XCTAssertEqual($0 as? ImportFailure, .fileTooLarge)
        }
    }

    func testPreviewsSelectedRegularFileWithoutChangingIt() throws {
        let directory = FileManager.default.temporaryDirectory
            .appending(path: "calltrack-file-preview-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let file = directory.appending(path: "synthetic.json")
        let contents = document(
            "{\"id\":\"\(callID.uuidString.lowercased())\",\"direction\":\"missed\",\"started_at_ms\":1800000000000,\"duration_seconds\":0}"
        )
        try contents.write(to: file)
        let modificationDate = try file.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate

        let preview = try CallLogFilePreviewer.preview(fileAt: file)

        XCTAssertEqual(preview.exportID, exportID)
        XCTAssertEqual(preview.calls.count, 1)
        XCTAssertEqual(preview.calls[0].direction, .missed)
        XCTAssertEqual(
            try file.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate,
            modificationDate
        )
    }

    func testRejectsDirectoriesAndOversizedFilesBeforeParsing() throws {
        let directory = FileManager.default.temporaryDirectory
            .appending(path: "calltrack-file-preview-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        XCTAssertThrowsError(try CallLogFilePreviewer.preview(fileAt: directory)) {
            XCTAssertEqual($0 as? ImportFailure, .invalidDocument)
        }

        let oversized = directory.appending(path: "oversized.json")
        try Data(repeating: 0, count: CallLogImporter.maximumFileBytes + 1).write(to: oversized)
        XCTAssertThrowsError(try CallLogFilePreviewer.preview(fileAt: oversized)) {
            XCTAssertEqual($0 as? ImportFailure, .fileTooLarge)
        }
    }
}
