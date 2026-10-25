import CSQLite
import CallTrackImport
import Darwin
import Foundation

public enum DeliveryIntentState: String, Codable, CaseIterable, Sendable {
  case prepared
  case writeAttempted = "write_attempted"
  case reconciled
  case uncertain
  case edited
  case deleted
  case conflict
}

public struct DeliveryCall: Equatable, Sendable {
  public let id: UUID
  public let direction: CallRecord.Direction
  public let startedAtMilliseconds: Int64
  public let durationSeconds: Int64
}

public struct DeliveryIntent: Equatable, Sendable {
  public let operationID: UUID
  public let destinationCalendarID: String
  public let exportID: UUID
  public let call: DeliveryCall
  public let state: DeliveryIntentState
  public let eventIdentifier: String?
  public let createdAtMilliseconds: Int64
  public let updatedAtMilliseconds: Int64

  public var marker: String {
    "CallTrack operation: \(operationID.uuidString.lowercased())"
  }
}

public enum DeliveryLedgerFailure: Error, Equatable {
  case database(code: Int32, message: String)
  case invalidCalendarIdentifier
  case invalidEventIdentifier
  case identityPayloadMismatch
  case invalidTransition(from: DeliveryIntentState, to: DeliveryIntentState)
  case invalidStoredPayload
  case missingIntent
}

/// A process-local serial writer for durable, privacy-minimal calendar intents.
/// It records intent only; it does not access EventKit or perform calendar writes.
@MainActor
public final class DeliveryIntentLedger {
  private var database: OpaquePointer?

  public init(databaseURL: URL) throws {
    try FileManager.default.createDirectory(
      at: databaseURL.deletingLastPathComponent(),
      withIntermediateDirectories: true,
      attributes: [.posixPermissions: 0o700]
    )
    try Self.ensurePrivateDatabaseFile(at: databaseURL)

    var opened: OpaquePointer?
    let openCode = sqlite3_open_v2(
      databaseURL.path,
      &opened,
      SQLITE_OPEN_CREATE | SQLITE_OPEN_READWRITE | SQLITE_OPEN_FULLMUTEX,
      nil
    )
    guard openCode == SQLITE_OK, let opened else {
      let message = opened.map { String(cString: sqlite3_errmsg($0)) } ?? "SQLite open failed"
      if let opened { sqlite3_close_v2(opened) }
      throw DeliveryLedgerFailure.database(code: openCode, message: message)
    }
    database = opened
    sqlite3_busy_timeout(opened, 5_000)

    do {
      try execute("PRAGMA journal_mode=DELETE")
      try execute("PRAGMA synchronous=FULL")
      try execute("PRAGMA foreign_keys=ON")
      try execute(
        """
        CREATE TABLE IF NOT EXISTS delivery_intents (
            operation_id TEXT PRIMARY KEY NOT NULL,
            destination_calendar_id TEXT NOT NULL,
            export_id TEXT NOT NULL,
            call_id TEXT NOT NULL,
            direction TEXT NOT NULL CHECK(direction IN ('incoming','outgoing','missed')),
            started_at_ms INTEGER NOT NULL CHECK(started_at_ms >= 0),
            duration_seconds INTEGER NOT NULL CHECK(duration_seconds >= 0),
            state TEXT NOT NULL CHECK(state IN ('prepared','write_attempted','reconciled','uncertain','edited','deleted','conflict')),
            event_identifier TEXT,
            created_at_ms INTEGER NOT NULL,
            updated_at_ms INTEGER NOT NULL,
            UNIQUE(destination_calendar_id, export_id, call_id)
        )
        """
      )
    } catch {
      sqlite3_close_v2(opened)
      database = nil
      throw error
    }
  }

  isolated deinit {
    if let database { sqlite3_close_v2(database) }
  }

  /// Durably records all validated calls before the UI can request any write.
  /// Existing identities return their original operation only when the
  /// payload is byte-for-byte equivalent at the normalized field level.
  public func prepare(preview: ImportPreview, destinationCalendarID: String) throws
    -> [DeliveryIntent]
  {
    guard !destinationCalendarID.isEmpty else {
      throw DeliveryLedgerFailure.invalidCalendarIdentifier
    }
    try execute("BEGIN IMMEDIATE")
    do {
      for call in preview.calls {
        let now = Self.nowMilliseconds()
        try insertOrRead(
          exportID: preview.exportID,
          call: call,
          destinationCalendarID: destinationCalendarID,
          now: now
        )
      }
      let intents = try readIntents(
        sql:
          "SELECT * FROM delivery_intents WHERE destination_calendar_id = ? AND export_id = ? ORDER BY call_id",
        textBindings: [destinationCalendarID, Self.uuidText(preview.exportID)]
      )
      try execute("COMMIT")
      return intents
    } catch {
      try? execute("ROLLBACK")
      throw error
    }
  }

  public func allIntents() throws -> [DeliveryIntent] {
    try readIntents(sql: "SELECT * FROM delivery_intents ORDER BY created_at_ms, operation_id")
  }

  public func intent(operationID: UUID) throws -> DeliveryIntent? {
    try readIntents(
      sql: "SELECT * FROM delivery_intents WHERE operation_id = ?",
      textBindings: [Self.uuidText(operationID)]
    ).first
  }

  public func unresolvedIntents() throws -> [DeliveryIntent] {
    try readIntents(
      sql:
        "SELECT * FROM delivery_intents WHERE state IN ('prepared','write_attempted','uncertain','conflict') ORDER BY created_at_ms, operation_id"
    )
  }

  public func markWriteAttempted(operationID: UUID) throws {
    try transition(operationID, from: [.prepared], to: .writeAttempted)
  }

  public func markReconciled(operationID: UUID, eventIdentifier: String) throws {
    guard !eventIdentifier.isEmpty else {
      throw DeliveryLedgerFailure.invalidEventIdentifier
    }
    try transition(
      operationID,
      from: [.writeAttempted, .uncertain],
      to: .reconciled,
      eventIdentifier: eventIdentifier
    )
  }

  public func markUncertain(operationID: UUID) throws {
    try transition(operationID, from: [.writeAttempted], to: .uncertain)
  }

  public func markEdited(operationID: UUID) throws {
    try transition(operationID, from: [.reconciled], to: .edited)
  }

  public func markDeleted(operationID: UUID) throws {
    try transition(operationID, from: [.reconciled, .edited], to: .deleted)
  }

  public func markConflict(operationID: UUID) throws {
    try transition(
      operationID,
      from: [.writeAttempted, .uncertain, .reconciled],
      to: .conflict
    )
  }

  private func insertOrRead(
    exportID: UUID,
    call: CallRecord,
    destinationCalendarID: String,
    now: Int64
  ) throws {
    let sql = """
      INSERT INTO delivery_intents (
          operation_id, destination_calendar_id, export_id, call_id,
          direction, started_at_ms, duration_seconds, state,
          event_identifier, created_at_ms, updated_at_ms
      ) VALUES (?, ?, ?, ?, ?, ?, ?, 'prepared', NULL, ?, ?)
      ON CONFLICT(destination_calendar_id, export_id, call_id) DO NOTHING
      """
    try withStatement(sql) { statement in
      try bind(Self.uuidText(UUID()), to: 1, in: statement)
      try bind(destinationCalendarID, to: 2, in: statement)
      try bind(Self.uuidText(exportID), to: 3, in: statement)
      try bind(Self.uuidText(call.id), to: 4, in: statement)
      try bind(call.direction.rawValue, to: 5, in: statement)
      guard sqlite3_bind_int64(statement, 6, call.startedAtMilliseconds) == SQLITE_OK,
        sqlite3_bind_int64(statement, 7, call.durationSeconds) == SQLITE_OK,
        sqlite3_bind_int64(statement, 8, now) == SQLITE_OK,
        sqlite3_bind_int64(statement, 9, now) == SQLITE_OK
      else {
        throw currentFailure()
      }
      try stepDone(statement)
    }

    let stored = try readIntents(
      sql:
        "SELECT * FROM delivery_intents WHERE destination_calendar_id = ? AND export_id = ? AND call_id = ?",
      textBindings: [destinationCalendarID, Self.uuidText(exportID), Self.uuidText(call.id)]
    )
    guard let existing = stored.first,
      existing.call.id == call.id,
      existing.call.direction == call.direction,
      existing.call.startedAtMilliseconds == call.startedAtMilliseconds,
      existing.call.durationSeconds == call.durationSeconds,
      existing.destinationCalendarID == destinationCalendarID,
      existing.exportID == exportID
    else {
      throw DeliveryLedgerFailure.identityPayloadMismatch
    }
  }

  private func transition(
    _ operationID: UUID,
    from allowedStates: [DeliveryIntentState],
    to nextState: DeliveryIntentState,
    eventIdentifier: String? = nil
  ) throws {
    guard !allowedStates.isEmpty else {
      throw DeliveryLedgerFailure.invalidTransition(from: nextState, to: nextState)
    }
    let placeholders = Array(repeating: "?", count: allowedStates.count).joined(separator: ",")
    let sql = """
      UPDATE delivery_intents
      SET state = ?, event_identifier = COALESCE(?, event_identifier), updated_at_ms = ?
      WHERE operation_id = ? AND state IN (\(placeholders))
      """
    try withStatement(sql) { statement in
      try bind(nextState.rawValue, to: 1, in: statement)
      if let eventIdentifier {
        try bind(eventIdentifier, to: 2, in: statement)
      } else if sqlite3_bind_null(statement, 2) != SQLITE_OK {
        throw currentFailure()
      }
      guard sqlite3_bind_int64(statement, 3, Self.nowMilliseconds()) == SQLITE_OK else {
        throw currentFailure()
      }
      try bind(Self.uuidText(operationID), to: 4, in: statement)
      for (offset, state) in allowedStates.enumerated() {
        try bind(state.rawValue, to: Int32(offset + 5), in: statement)
      }
      try stepDone(statement)
      guard sqlite3_changes(database) == 1 else {
        let current = try readIntents(
          sql: "SELECT * FROM delivery_intents WHERE operation_id = ?",
          textBindings: [Self.uuidText(operationID)]
        ).first
        guard let current else { throw DeliveryLedgerFailure.missingIntent }
        throw DeliveryLedgerFailure.invalidTransition(from: current.state, to: nextState)
      }
    }
  }

  private func readIntents(sql: String, textBindings: [String] = []) throws -> [DeliveryIntent] {
    var results: [DeliveryIntent] = []
    try withStatement(sql) { statement in
      for (offset, value) in textBindings.enumerated() {
        try bind(value, to: Int32(offset + 1), in: statement)
      }
      while true {
        let code = sqlite3_step(statement)
        if code == SQLITE_DONE { break }
        guard code == SQLITE_ROW else { throw currentFailure(code) }
        guard
          let operationID = Self.uuid(from: Self.text(statement, 0)),
          let calendarID = Self.text(statement, 1),
          let exportID = Self.uuid(from: Self.text(statement, 2)),
          let callID = Self.uuid(from: Self.text(statement, 3)),
          let direction = CallRecord.Direction(rawValue: Self.text(statement, 4) ?? ""),
          let state = DeliveryIntentState(rawValue: Self.text(statement, 7) ?? "")
        else {
          throw DeliveryLedgerFailure.database(code: SQLITE_MISMATCH, message: "Invalid row value")
        }
        let call = DeliveryCall(
          id: callID,
          direction: direction,
          startedAtMilliseconds: sqlite3_column_int64(statement, 5),
          durationSeconds: sqlite3_column_int64(statement, 6)
        )
        results.append(
          DeliveryIntent(
            operationID: operationID,
            destinationCalendarID: calendarID,
            exportID: exportID,
            call: call,
            state: state,
            eventIdentifier: Self.text(statement, 8),
            createdAtMilliseconds: sqlite3_column_int64(statement, 9),
            updatedAtMilliseconds: sqlite3_column_int64(statement, 10)
          )
        )
      }
    }
    return results
  }

  private func execute(_ sql: String) throws {
    guard let database else {
      throw DeliveryLedgerFailure.database(code: SQLITE_MISUSE, message: "Ledger is closed")
    }
    var errorMessage: UnsafeMutablePointer<CChar>?
    let code = sqlite3_exec(database, sql, nil, nil, &errorMessage)
    guard code == SQLITE_OK else {
      let message =
        errorMessage.map { String(cString: $0) } ?? String(cString: sqlite3_errmsg(database))
      sqlite3_free(errorMessage)
      throw DeliveryLedgerFailure.database(code: code, message: message)
    }
  }

  private static func ensurePrivateDatabaseFile(at url: URL) throws {
    var info = stat()
    if lstat(url.path, &info) == 0 {
      guard (info.st_mode & mode_t(S_IFMT)) == mode_t(S_IFREG) else {
        throw DeliveryLedgerFailure.database(
          code: SQLITE_CANTOPEN,
          message: "Ledger path is not a regular file"
        )
      }
      let descriptor = Darwin.open(url.path, O_RDONLY | O_NOFOLLOW | O_CLOEXEC)
      guard descriptor >= 0 else {
        throw DeliveryLedgerFailure.database(
          code: SQLITE_CANTOPEN,
          message: "Unable to open ledger without following links"
        )
      }
      defer { _ = close(descriptor) }
      var openedInfo = stat()
      guard fstat(descriptor, &openedInfo) == 0,
        (openedInfo.st_mode & mode_t(S_IFMT)) == mode_t(S_IFREG),
        fchmod(descriptor, mode_t(S_IRUSR | S_IWUSR)) == 0
      else {
        throw DeliveryLedgerFailure.database(
          code: SQLITE_CANTOPEN,
          message: "Unable to secure regular ledger file"
        )
      }
    } else {
      guard errno == ENOENT else {
        throw DeliveryLedgerFailure.database(
          code: SQLITE_CANTOPEN,
          message: "Unable to inspect ledger path"
        )
      }
      let descriptor = Darwin.open(
        url.path,
        O_CREAT | O_EXCL | O_RDWR | O_NOFOLLOW | O_CLOEXEC,
        mode_t(S_IRUSR | S_IWUSR)
      )
      guard descriptor >= 0 else {
        throw DeliveryLedgerFailure.database(
          code: SQLITE_CANTOPEN,
          message: "Unable to create private ledger file"
        )
      }
      _ = close(descriptor)
    }
  }

  private func withStatement<T>(_ sql: String, _ body: (OpaquePointer) throws -> T) throws -> T {
    guard let database else {
      throw DeliveryLedgerFailure.database(code: SQLITE_MISUSE, message: "Ledger is closed")
    }
    var statement: OpaquePointer?
    let code = sqlite3_prepare_v2(database, sql, -1, &statement, nil)
    guard code == SQLITE_OK, let statement else { throw currentFailure(code) }
    defer { sqlite3_finalize(statement) }
    return try body(statement)
  }

  private func bind(_ value: String, to index: Int32, in statement: OpaquePointer) throws {
    let code = value.withCString { sqlite3_bind_text(statement, index, $0, -1, Self.transient) }
    guard code == SQLITE_OK else { throw currentFailure(code) }
  }

  private func stepDone(_ statement: OpaquePointer) throws {
    let code = sqlite3_step(statement)
    guard code == SQLITE_DONE else { throw currentFailure(code) }
  }

  private func currentFailure(_ code: Int32? = nil) -> DeliveryLedgerFailure {
    let actualCode = code ?? database.map(sqlite3_errcode) ?? SQLITE_MISUSE
    let message = database.map { String(cString: sqlite3_errmsg($0)) } ?? "Ledger is closed"
    return .database(code: actualCode, message: message)
  }

  private static func text(_ statement: OpaquePointer, _ column: Int32) -> String? {
    guard let value = sqlite3_column_text(statement, column) else { return nil }
    return String(cString: value)
  }

  private static func uuidText(_ id: UUID) -> String {
    id.uuidString.lowercased()
  }

  private static func uuid(from value: String?) -> UUID? {
    value.flatMap(UUID.init(uuidString:))
  }

  private static func nowMilliseconds() -> Int64 {
    Int64(Date().timeIntervalSince1970 * 1_000)
  }

  private static let transient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
}
