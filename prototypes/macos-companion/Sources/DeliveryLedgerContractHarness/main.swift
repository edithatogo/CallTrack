import CallTrackDelivery
import CallTrackImport
import CryptoKit
import Foundation

@main
struct DeliveryLedgerContractHarness {
  static func main() async throws {
    try await runOnMainActor()
  }

  @MainActor
  private static func runOnMainActor() async throws {
    let root = FileManager.default.temporaryDirectory
      .appending(path: "calltrack-ledger-harness-\(UUID().uuidString)")
    try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
    defer { try? FileManager.default.removeItem(at: root) }

    let databaseURL = root.appending(path: "delivery.sqlite")
    let exportID = UUID(uuidString: "00000000-0000-4000-8000-000000000201")!
    let callID = UUID(uuidString: "00000000-0000-4000-8000-000000000202")!
    let preview = try makePreview(exportID: exportID, callID: callID, duration: 0)

    var ledger: DeliveryIntentLedger? = try DeliveryIntentLedger(databaseURL: databaseURL)
    let first = try ledger!.prepare(preview: preview, destinationCalendarID: "synthetic-calendar")
    let replay = try ledger!.prepare(preview: preview, destinationCalendarID: "synthetic-calendar")
    precondition(first.count == 1 && first == replay, "Prepared identity was not idempotent")
    precondition(first[0].state == .prepared, "Initial state is not prepared")
    precondition(first[0].marker.contains(first[0].operationID.uuidString.lowercased()))

    let mode =
      try FileManager.default.attributesOfItem(atPath: databaseURL.path)[.posixPermissions]
      as? NSNumber
    precondition(mode?.intValue == 0o600, "Ledger mode is not private")
    ledger = nil

    let reopened = try DeliveryIntentLedger(databaseURL: databaseURL)
    let recovered = try reopened.unresolvedIntents()
    precondition(recovered == first, "Prepared intent did not survive reopen")
    try reopened.markWriteAttempted(operationID: first[0].operationID)
    try reopened.markUncertain(operationID: first[0].operationID)
    let uncertain = try reopened.unresolvedIntents()
    precondition(uncertain.first?.state == .uncertain, "Unknown write was not retained")
    try reopened.markReconciled(
      operationID: first[0].operationID, eventIdentifier: "synthetic-event")
    try reopened.markEdited(operationID: first[0].operationID)
    let edited = try reopened.allIntents()
    precondition(edited.first?.state == .edited, "Human edit state was not retained")
    precondition(edited.first?.eventIdentifier == "synthetic-event")
    try reopened.markDeleted(operationID: first[0].operationID)
    let afterDeletion = try reopened.unresolvedIntents()
    precondition(afterDeletion.isEmpty, "Deleted event was incorrectly replayable")

    do {
      let changed = try makePreview(exportID: exportID, callID: callID, duration: 5)
      _ = try reopened.prepare(preview: changed, destinationCalendarID: "synthetic-calendar")
      preconditionFailure("Changed content reused a foreign identity")
    } catch DeliveryLedgerFailure.identityPayloadMismatch {
      // Expected: a stable foreign identity cannot silently change payload.
    }

    let anotherDestination = try reopened.prepare(
      preview: preview,
      destinationCalendarID: "another-synthetic-calendar"
    )
    precondition(anotherDestination.first?.operationID != first[0].operationID)

    let flowPreview = try makePreview(
      exportID: UUID(uuidString: "00000000-0000-4000-8000-000000000301")!,
      callID: UUID(uuidString: "00000000-0000-4000-8000-000000000302")!,
      duration: 0
    )
    let gateway = HarnessEventCalendarGateway()
    let coordinator = CalendarDeliveryCoordinator(ledger: reopened, calendarGateway: gateway)
    let deliveryIntent = try coordinator.prepare(
      preview: flowPreview,
      destinationCalendarID: "synthetic-calendar"
    )[0]
    gateway.throwAfterInsert = true
    do {
      _ = try await coordinator.confirmAndDeliver(
        operationIDs: [deliveryIntent.operationID],
        userConfirmed: true
      )
      preconditionFailure("Uncertain provider save was not surfaced")
    } catch CalendarDeliveryFailure.outcomeUncertain {
      let uncertainIntent = try reopened.intent(operationID: deliveryIntent.operationID)
      precondition(uncertainIntent?.state == .uncertain)
    }
    gateway.throwAfterInsert = false
    let recoveredDelivery = try await coordinator.confirmAndDeliver(
      operationIDs: [deliveryIntent.operationID],
      userConfirmed: true
    )
    precondition(recoveredDelivery.reconciled == 1)
    precondition(gateway.insertCount == 1, "Uncertain save was blindly repeated")
    print(
      "PASS: durable intent, private file, dedupe, state recovery, human-edit/delete preservation, and identity conflict"
    )
  }

  private static func makePreview(exportID: UUID, callID: UUID, duration: Int64) throws
    -> ImportPreview
  {
    let call =
      "{\"id\":\"\(callID.uuidString.lowercased())\",\"direction\":\"missed\",\"started_at_ms\":1800000000000,\"duration_seconds\":\(duration)}"
    let payload =
      "{\"schema_version\":1,\"export_id\":\"\(exportID.uuidString.lowercased())\",\"record_count\":1,\"calls\":[\(call)]}"
    let digest = SHA256.hash(data: Data(payload.utf8))
      .map { String(format: "%02x", $0) }
      .joined()
    let document = String(payload.dropLast()) + ",\"content_sha256\":\"\(digest)\"}"
    return try CallLogImporter.preview(data: Data(document.utf8))
  }
}

@MainActor
private final class HarnessEventCalendarGateway: EventCalendarGateway {
  var authorizationState: EventCalendarAuthorization = .fullAccess
  var throwAfterInsert = false
  private(set) var insertCount = 0
  private var storedEvents: [String: CalendarEventSnapshot] = [:]

  func authorization() -> EventCalendarAuthorization { authorizationState }

  func requestFullAccess() async throws -> Bool {
    authorizationState = .fullAccess
    return true
  }

  func writableCalendars() throws -> [CalendarDestination] {
    guard authorizationState == .fullAccess else { throw CalendarDeliveryFailure.accessNotGranted }
    return [
      CalendarDestination(
        identifier: "synthetic-calendar",
        title: "Synthetic calendar",
        allowsModifications: true
      )
    ]
  }

  func insert(_ payload: CalendarEventPayload, into calendarID: String) throws {
    insertCount += 1
    let identifier = "synthetic-event-\(insertCount)"
    storedEvents[identifier] = CalendarEventSnapshot(
      identifier: identifier,
      calendarIdentifier: calendarID,
      marker: payload.marker,
      title: payload.title,
      startAtMilliseconds: payload.startAtMilliseconds,
      endAtMilliseconds: payload.endAtMilliseconds,
      isAllDay: false,
      attendeeCount: 0,
      alarmCount: 0
    )
    if throwAfterInsert { throw CalendarDeliveryFailure.outcomeUncertain }
  }

  func event(identifier: String) -> CalendarEventSnapshot? {
    storedEvents[identifier]
  }

  func events(
    matching marker: String,
    payload: CalendarEventPayload,
    calendarID: String
  ) throws -> [CalendarEventSnapshot] {
    guard authorizationState == .fullAccess else { throw CalendarDeliveryFailure.accessNotGranted }
    return storedEvents.values.filter {
      $0.marker == marker && $0.calendarIdentifier == calendarID
    }
  }
}
