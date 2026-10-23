import Foundation

public struct CallLogDocument: Decodable, Sendable {
    public let schemaVersion: Int
    public let exportID: UUID
    public let calls: [CallRecord]

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case exportID = "export_id"
        case calls
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
    case duplicateCallID(UUID)
    case invalidTimestamp(UUID)
    case invalidDuration(UUID)
}

/// Decodes a deliberately bounded, metadata-minimal JSON interchange document.
/// This is a prototype contract; no Android exporter currently claims to emit it.
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
        return ImportPreview(exportID: document.exportID, calls: document.calls)
    }
}
