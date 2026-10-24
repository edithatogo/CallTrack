import Foundation
import CryptoKit

public struct CallLogDocument: Decodable, Sendable {
    public let schemaVersion: Int
    public let exportID: UUID
    public let recordCount: Int
    public let calls: [CallRecord]
    public let contentSHA256: String

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case exportID = "export_id"
        case recordCount = "record_count"
        case calls
        case contentSHA256 = "content_sha256"
    }
}

public struct CallRecord: Decodable, Equatable, Sendable {
    public enum Direction: String, Decodable, Sendable {
        case incoming
        case outgoing
        case missed
    }

    public let id: UUID
    public let direction: Direction
    /// Unix epoch milliseconds, independent of locale and current timezone.
    public let startedAtMilliseconds: Int64
    public let durationSeconds: Int64

    enum CodingKeys: String, CodingKey {
        case id
        case direction
        case startedAtMilliseconds = "started_at_ms"
        case durationSeconds = "duration_seconds"
    }
}

public struct ImportPreview: Equatable, Sendable {
    public let exportID: UUID
    public let calls: [CallRecord]
}

public enum ImportFailure: Error, Equatable {
    case fileTooLarge
    case invalidDocument
    case unsupportedVersion(Int)
    case tooManyCalls
    case recordCountMismatch
    case checksumMismatch
    case nonCanonicalDocument
    case duplicateCallID(UUID)
    case invalidTimestamp(UUID)
    case invalidDuration(UUID)
}

/// Decodes a deliberately bounded, metadata-minimal JSON interchange document.
/// This is a prototype contract; the Android producer is not integrated into the app.
public enum CallLogImporter {
    public static let maximumFileBytes = 5 * 1024 * 1024
    public static let maximumCalls = 10_000

    public static func preview(data: Data) throws -> ImportPreview {
        guard data.count <= maximumFileBytes else { throw ImportFailure.fileTooLarge }
        let document: CallLogDocument
        do {
            document = try JSONDecoder().decode(CallLogDocument.self, from: data)
        } catch {
            throw ImportFailure.invalidDocument
        }
        guard document.schemaVersion == 1 else {
            throw ImportFailure.unsupportedVersion(document.schemaVersion)
        }
        guard document.calls.count <= maximumCalls else { throw ImportFailure.tooManyCalls }
        guard document.recordCount == document.calls.count else { throw ImportFailure.recordCountMismatch }

        var seen = Set<UUID>()
        for call in document.calls {
            guard seen.insert(call.id).inserted else { throw ImportFailure.duplicateCallID(call.id) }
            guard call.startedAtMilliseconds >= 0 else { throw ImportFailure.invalidTimestamp(call.id) }
            guard call.durationSeconds >= 0 else { throw ImportFailure.invalidDuration(call.id) }
            let durationMilliseconds = call.durationSeconds.multipliedReportingOverflow(by: 1_000)
            guard !durationMilliseconds.overflow else { throw ImportFailure.invalidDuration(call.id) }
            let end = call.startedAtMilliseconds.addingReportingOverflow(durationMilliseconds.partialValue)
            guard !end.overflow else { throw ImportFailure.invalidTimestamp(call.id) }
        }
        let canonicalPayload = canonicalPayload(document)
        let digest = SHA256.hash(data: Data(canonicalPayload.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        guard digest == document.contentSHA256 else { throw ImportFailure.checksumMismatch }
        let canonicalDocument = String(canonicalPayload.dropLast())
            + ",\"content_sha256\":\"\(document.contentSHA256)\"}"
        // The producer's exact field/order/number form rejects duplicate keys,
        // unknown fields, and ambiguous spellings. A single final line ending
        // is allowed for ordinary text-file handling.
        let canonicalBytes = Data(canonicalDocument.utf8)
        let canonicalWithLF = Data((canonicalDocument + "\n").utf8)
        let canonicalWithCRLF = Data((canonicalDocument + "\r\n").utf8)
        guard data == canonicalBytes || data == canonicalWithLF || data == canonicalWithCRLF else {
            throw ImportFailure.nonCanonicalDocument
        }
        return ImportPreview(exportID: document.exportID, calls: document.calls)
    }

    private static func canonicalPayload(_ document: CallLogDocument) -> String {
        let calls = document.calls.map { call in
            "{\"id\":\"\(call.id.uuidString.lowercased())\",\"direction\":\"\(call.direction.rawValue)\",\"started_at_ms\":\(call.startedAtMilliseconds),\"duration_seconds\":\(call.durationSeconds)}"
        }.joined(separator: ",")
        return "{\"schema_version\":\(document.schemaVersion),\"export_id\":\"\(document.exportID.uuidString.lowercased())\",\"record_count\":\(document.recordCount),\"calls\":[\(calls)]}"
    }
}
