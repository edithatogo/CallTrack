import Foundation

/// Reads a user-selected regular file without ever buffering more than the parser limit.
public enum CallLogFilePreviewer {
    public static func preview(fileAt url: URL) throws -> ImportPreview {
        let hasSecurityScope = url.startAccessingSecurityScopedResource()
        defer {
            if hasSecurityScope {
                url.stopAccessingSecurityScopedResource()
            }
        }

        let values = try url.resourceValues(forKeys: [.isRegularFileKey, .fileSizeKey])
        guard values.isRegularFile == true else {
            throw ImportFailure.invalidDocument
        }
        if let size = values.fileSize, size > CallLogImporter.maximumFileBytes {
            throw ImportFailure.fileTooLarge
        }

        let handle = try FileHandle(forReadingFrom: url)
        defer { try? handle.close() }

        var data = Data()
        while data.count <= CallLogImporter.maximumFileBytes {
            let remaining = CallLogImporter.maximumFileBytes + 1 - data.count
            guard remaining > 0 else {
                throw ImportFailure.fileTooLarge
            }
            guard let chunk = try handle.read(upToCount: remaining), !chunk.isEmpty else {
                break
            }
            data.append(chunk)
        }
        guard data.count <= CallLogImporter.maximumFileBytes else {
            throw ImportFailure.fileTooLarge
        }
        return try CallLogImporter.preview(data: data)
    }
}
