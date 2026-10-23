import Foundation
import XCTest
@testable import CallTrackImport

final class CallLogImportTests: XCTestCase {
    private let exportID = UUID(uuidString: "00000000-0000-4000-8000-000000000001")!
    private let callID = UUID(uuidString: "00000000-0000-4000-8000-000000000002")!

    private func document(_ calls: String, version: Int = 1) -> Data {
        Data("""
        {"schema_version":\(version),"export_id":"\(exportID)","calls":[\(calls)]}
        """.utf8)
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

    func testRejectsOversizedInputBeforeDecode() {
        XCTAssertThrowsError(try CallLogImporter.preview(
            data: Data(repeating: 0x20, count: CallLogImporter.maximumFileBytes + 1)
        )) {
            XCTAssertEqual($0 as? ImportFailure, .fileTooLarge)
        }
    }
}
