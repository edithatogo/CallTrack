import CallTrackDelivery
import CallTrackImport
import CryptoKit
import Foundation
import XCTest

@MainActor
final class DeliveryIntentLedgerTests: XCTestCase {
  private let exportID = UUID(uuidString: "00000000-0000-4000-8000-000000000101")!
  private let callID = UUID(uuidString: "00000000-0000-4000-8000-000000000102")!

  func testPrepareIsDurableAndDeduplicatedPerDestination() throws {
    let directory = temporaryDirectory()
    defer { try? FileManager.default.removeItem(at: directory) }
    let databaseURL = directory.appending(path: "delivery.sqlite")
    let preview = try preview()

    var ledger: DeliveryIntentLedger? = try DeliveryIntentLedger(databaseURL: databaseURL)
    let first = try ledger!.prepare(preview: preview, destinationCalendarID: "calendar-one")
    let repeated = try ledger!.prepare(preview: preview, destinationCalendarID: "calendar-one")
    XCTAssertEqual(first.count, 1)
    XCTAssertEqual(first, repeated)
    XCTAssertEqual(first[0].state, .prepared)
    XCTAssertTrue(first[0].marker.hasPrefix("CallTrack operation: "))

    let attributes = try FileManager.default.attributesOfItem(atPath: databaseURL.path)
    XCTAssertEqual((attributes[.posixPermissions] as? NSNumber)?.intValue, 0o600)
    ledger = nil

    let reopened = try DeliveryIntentLedger(databaseURL: databaseURL)
    XCTAssertEqual(try reopened.allIntents(), first)

    let anotherDestination = try reopened.prepare(
      preview: preview,
      destinationCalendarID: "calendar-two"
    )
    XCTAssertEqual(anotherDestination.count, 1)
    XCTAssertNotEqual(anotherDestination[0].operationID, first[0].operationID)
  }

  func testWriteIntentMustBeDurableBeforeReconciliationAndNeverBlindlyReset() throws {
    let directory = temporaryDirectory()
    defer { try? FileManager.default.removeItem(at: directory) }
    let databaseURL = directory.appending(path: "delivery.sqlite")
    let preview = try preview()
    let ledger = try DeliveryIntentLedger(databaseURL: databaseURL)
    let intent = try XCTUnwrap(
      ledger.prepare(preview: preview, destinationCalendarID: "calendar-one").first
    )

    XCTAssertThrowsError(
      try ledger.markReconciled(operationID: intent.operationID, eventIdentifier: "event-1"))
    try ledger.markWriteAttempted(operationID: intent.operationID)
    XCTAssertThrowsError(try ledger.markWriteAttempted(operationID: intent.operationID))
    try ledger.markUncertain(operationID: intent.operationID)
    try ledger.markReconciled(operationID: intent.operationID, eventIdentifier: "event-1")
    try ledger.markEdited(operationID: intent.operationID)
    try ledger.markDeleted(operationID: intent.operationID)

    let stored = try XCTUnwrap(ledger.allIntents().first)
    XCTAssertEqual(stored.state, .deleted)
    XCTAssertEqual(stored.eventIdentifier, "event-1")
    XCTAssertTrue(try ledger.unresolvedIntents().isEmpty)
    XCTAssertThrowsError(try ledger.markWriteAttempted(operationID: intent.operationID))
  }

  func testSameForeignIdentityWithChangedPayloadFailsClosed() throws {
    let directory = temporaryDirectory()
    defer { try? FileManager.default.removeItem(at: directory) }
    let ledger = try DeliveryIntentLedger(databaseURL: directory.appending(path: "delivery.sqlite"))
    _ = try ledger.prepare(preview: preview(), destinationCalendarID: "calendar-one")

    let changed = try preview(durationSeconds: 9)
    XCTAssertThrowsError(
      try ledger.prepare(preview: changed, destinationCalendarID: "calendar-one")
    ) { error in
      XCTAssertEqual(error as? DeliveryLedgerFailure, .identityPayloadMismatch)
    }
  }

  func testDeliveryRequiresConfirmationAndReconcilesUncertainSaveWithoutReinserting() async throws {
    let directory = temporaryDirectory()
    defer { try? FileManager.default.removeItem(at: directory) }
    let ledger = try DeliveryIntentLedger(databaseURL: directory.appending(path: "delivery.sqlite"))
    let gateway = FakeEventCalendarGateway()
    let coordinator = CalendarDeliveryCoordinator(ledger: ledger, calendarGateway: gateway)
    let intent = try XCTUnwrap(
      coordinator.prepare(preview: preview(), destinationCalendarID: "calendar-one").first
    )

    do {
      _ = try await coordinator.confirmAndDeliver(
        operationIDs: [intent.operationID],
        userConfirmed: false
      )
      XCTFail("Delivery proceeded without explicit confirmation")
    } catch CalendarDeliveryFailure.userConfirmationRequired {
      XCTAssertEqual(gateway.insertCount, 0)
    }

    gateway.throwAfterInsert = true
    do {
      _ = try await coordinator.confirmAndDeliver(
        operationIDs: [intent.operationID],
        userConfirmed: true
      )
      XCTFail("An uncertain EventKit outcome was reported as reconciled")
    } catch CalendarDeliveryFailure.outcomeUncertain {
      XCTAssertEqual(gateway.insertCount, 1)
      XCTAssertEqual(try ledger.intent(operationID: intent.operationID)?.state, .uncertain)
    }

    gateway.throwAfterInsert = false
    let recovered = try await coordinator.confirmAndDeliver(
      operationIDs: [intent.operationID],
      userConfirmed: true
    )
    XCTAssertEqual(gateway.insertCount, 1, "Recovery blindly inserted a second event")
    XCTAssertEqual(recovered.reconciled, 1)
    XCTAssertEqual(try ledger.intent(operationID: intent.operationID)?.state, .reconciled)
  }

  func testPermissionLossAndHumanDeletionNeverCauseReplay() async throws {
    let directory = temporaryDirectory()
    defer { try? FileManager.default.removeItem(at: directory) }
    let ledger = try DeliveryIntentLedger(databaseURL: directory.appending(path: "delivery.sqlite"))
    let gateway = FakeEventCalendarGateway()
    let coordinator = CalendarDeliveryCoordinator(ledger: ledger, calendarGateway: gateway)
    let intent = try XCTUnwrap(
      coordinator.prepare(preview: preview(), destinationCalendarID: "calendar-one").first
    )
    _ = try await coordinator.confirmAndDeliver(
      operationIDs: [intent.operationID], userConfirmed: true)
    XCTAssertEqual(gateway.insertCount, 1)

    gateway.authorizationState = .denied
    do {
      _ = try await coordinator.confirmAndDeliver(
        operationIDs: [intent.operationID], userConfirmed: true)
      XCTFail("Delivery continued after access loss")
    } catch CalendarDeliveryFailure.accessNotGranted {
      XCTAssertEqual(gateway.insertCount, 1)
    }

    gateway.authorizationState = .fullAccess
    gateway.eventsByID.removeAll()
    let result = try await coordinator.confirmAndDeliver(
      operationIDs: [intent.operationID],
      userConfirmed: true
    )
    XCTAssertEqual(result.deleted, 1)
    XCTAssertEqual(gateway.insertCount, 1, "A human deletion was silently recreated")
    XCTAssertEqual(try ledger.intent(operationID: intent.operationID)?.state, .deleted)
  }

  private func preview(durationSeconds: Int64 = 0) throws -> ImportPreview {
    let call =
      "{\"id\":\"\(callID.uuidString.lowercased())\",\"direction\":\"missed\",\"started_at_ms\":1800000000000,\"duration_seconds\":\(durationSeconds)}"
    let payload =
      "{\"schema_version\":1,\"export_id\":\"\(exportID.uuidString.lowercased())\",\"record_count\":1,\"calls\":[\(call)]}"
    let digest = SHA256.hash(data: Data(payload.utf8))
      .map { String(format: "%02x", $0) }
      .joined()
    let document = String(payload.dropLast()) + ",\"content_sha256\":\"\(digest)\"}"
    return try CallLogImporter.preview(data: Data(document.utf8))
  }

  private func temporaryDirectory() -> URL {
    let directory = FileManager.default.temporaryDirectory
      .appending(path: "calltrack-delivery-\(UUID().uuidString)")
    try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    return directory
  }
}

@MainActor
private final class FakeEventCalendarGateway: EventCalendarGateway {
  var authorizationState: EventCalendarAuthorization = .fullAccess
  var throwAfterInsert = false
  private(set) var insertCount = 0
  var eventsByID: [String: CalendarEventSnapshot] = [:]
  private let calendar = CalendarDestination(
    identifier: "calendar-one",
    title: "Synthetic calendar",
    allowsModifications: true
  )

  func authorization() -> EventCalendarAuthorization { authorizationState }

  func requestFullAccess() async throws -> Bool {
    authorizationState = .fullAccess
    return true
  }

  func writableCalendars() throws -> [CalendarDestination] {
    guard authorizationState == .fullAccess else { throw CalendarDeliveryFailure.accessNotGranted }
    return [calendar]
  }

  func insert(_ payload: CalendarEventPayload, into calendarID: String) throws {
    insertCount += 1
    let id = "event-\(insertCount)"
    eventsByID[id] = CalendarEventSnapshot(
      identifier: id,
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
    eventsByID[identifier]
  }

  func events(
    matching marker: String,
    payload: CalendarEventPayload,
    calendarID: String
  ) throws -> [CalendarEventSnapshot] {
    guard authorizationState == .fullAccess else { throw CalendarDeliveryFailure.accessNotGranted }
    return eventsByID.values.filter {
      $0.marker == marker && $0.calendarIdentifier == calendarID
    }
  }
}
