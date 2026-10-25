import CallTrackImport
import Foundation

public struct CalendarDeliverySummary: Equatable, Sendable {
  public let reconciled: Int
  public let unresolved: Int
  public let edited: Int
  public let deleted: Int
  public let conflicts: Int
  public let cancelled: Bool
}

/// Coordinates durable intent and the sole EventKit writer. Every insertion
/// requires an explicit confirmation value from the host UI. Recovery paths only
/// read EventKit and never repeat a write whose outcome is unknown.
@MainActor
public final class CalendarDeliveryCoordinator {
  private let ledger: DeliveryIntentLedger
  private let calendarGateway: any EventCalendarGateway

  public init(ledger: DeliveryIntentLedger, calendarGateway: any EventCalendarGateway) {
    self.ledger = ledger
    self.calendarGateway = calendarGateway
  }

  public func requestFullAccessAfterUserAction() async throws -> Bool {
    try await calendarGateway.requestFullAccess()
  }

  public func writableDestinations() throws -> [CalendarDestination] {
    try calendarGateway.writableCalendars()
  }

  /// Records the exact destination and operation identity without requesting
  /// EventKit access or writing events.
  public func prepare(preview: ImportPreview, destinationCalendarID: String) throws
    -> [DeliveryIntent]
  {
    guard calendarGateway.authorization() == .fullAccess else {
      throw CalendarDeliveryFailure.accessNotGranted
    }
    guard
      try calendarGateway.writableCalendars().contains(where: {
        $0.identifier == destinationCalendarID && $0.allowsModifications
      })
    else {
      throw CalendarDeliveryFailure.destinationUnavailable
    }
    return try ledger.prepare(preview: preview, destinationCalendarID: destinationCalendarID)
  }

  /// Calls that can create events require the host UI's distinct confirmation
  /// action after it displays the selected destination and aggregate preview.
  public func confirmAndDeliver(operationIDs: [UUID], userConfirmed: Bool) async throws
    -> CalendarDeliverySummary
  {
    guard userConfirmed else { throw CalendarDeliveryFailure.userConfirmationRequired }
    try requireFullAccess()

    let requested = Set(operationIDs)
    let intents = try ledger.allIntents().filter { requested.contains($0.operationID) }
    guard intents.count == requested.count else { throw DeliveryLedgerFailure.missingIntent }

    var reconciled = 0
    var unresolved = 0
    var edited = 0
    var deleted = 0
    var conflicts = 0
    var cancelled = false

    for (index, intent) in intents.enumerated() {
      if Task.isCancelled {
        cancelled = true
        unresolved += intents.count - index
        break
      }
      switch intent.state {
      case .prepared:
        try performFirstWrite(intent)
      case .writeAttempted, .uncertain:
        try recoverUncertainWrite(intent)
      case .reconciled:
        try verifyReconciledEvent(intent)
      case .edited:
        edited += 1
        continue
      case .deleted:
        deleted += 1
        continue
      case .conflict:
        conflicts += 1
        continue
      }

      switch try ledger.intent(operationID: intent.operationID)?.state {
      case .reconciled:
        reconciled += 1
      case .edited:
        edited += 1
      case .deleted:
        deleted += 1
      case .uncertain, .writeAttempted, .prepared, .none:
        unresolved += 1
      case .conflict:
        conflicts += 1
      }
      if index < intents.count - 1 { await Task.yield() }
    }

    return CalendarDeliverySummary(
      reconciled: reconciled,
      unresolved: unresolved,
      edited: edited,
      deleted: deleted,
      conflicts: conflicts,
      cancelled: cancelled
    )
  }

  private func requireFullAccess() throws {
    switch calendarGateway.authorization() {
    case .fullAccess:
      return
    case .writeOnly:
      throw CalendarDeliveryFailure.writeOnlyAccessInsufficient
    case .notDetermined, .denied, .restricted:
      throw CalendarDeliveryFailure.accessNotGranted
    }
  }

  private func performFirstWrite(_ intent: DeliveryIntent) throws {
    guard
      try calendarGateway.writableCalendars().contains(where: {
        $0.identifier == intent.destinationCalendarID && $0.allowsModifications
      })
    else {
      throw CalendarDeliveryFailure.destinationUnavailable
    }
    let payload = try Self.payload(for: intent)
    try ledger.markWriteAttempted(operationID: intent.operationID)
    do {
      try calendarGateway.insert(payload, into: intent.destinationCalendarID)
    } catch {
      try ledger.markUncertain(operationID: intent.operationID)
      throw CalendarDeliveryFailure.outcomeUncertain
    }
    try reconcileReadback(intent, payload: payload)
  }

  private func recoverUncertainWrite(_ intent: DeliveryIntent) throws {
    let payload = try Self.payload(for: intent)
    try reconcileReadback(intent, payload: payload)
  }

  private func verifyReconciledEvent(_ intent: DeliveryIntent) throws {
    guard let identifier = intent.eventIdentifier,
      let event = calendarGateway.event(identifier: identifier)
    else {
      try ledger.markDeleted(operationID: intent.operationID)
      return
    }
    guard
      event.exactlyMatches(try Self.payload(for: intent), calendarID: intent.destinationCalendarID)
    else {
      try ledger.markEdited(operationID: intent.operationID)
      return
    }
  }

  private func reconcileReadback(_ intent: DeliveryIntent, payload: CalendarEventPayload) throws {
    let matches = try calendarGateway.events(
      matching: intent.marker,
      payload: payload,
      calendarID: intent.destinationCalendarID
    )
    guard matches.count == 1, let event = matches.first else {
      if matches.isEmpty {
        let current = try ledger.allIntents().first { $0.operationID == intent.operationID }
        if current?.state == .writeAttempted {
          try ledger.markUncertain(operationID: intent.operationID)
        }
        throw CalendarDeliveryFailure.outcomeUncertain
      }
      try ledger.markConflict(operationID: intent.operationID)
      throw CalendarDeliveryFailure.conflictingReadback
    }
    guard event.exactlyMatches(payload, calendarID: intent.destinationCalendarID) else {
      try ledger.markConflict(operationID: intent.operationID)
      throw CalendarDeliveryFailure.conflictingReadback
    }
    try ledger.markReconciled(operationID: intent.operationID, eventIdentifier: event.identifier)
  }

  private static func payload(for intent: DeliveryIntent) throws -> CalendarEventPayload {
    let title: String
    switch intent.call.direction {
    case .incoming:
      title = "Incoming call"
    case .outgoing:
      title = "Outgoing call"
    case .missed:
      title = "Missed call"
    }
    let (duration, durationOverflow) = intent.call.durationSeconds.multipliedReportingOverflow(
      by: 1_000)
    let (end, endOverflow) = intent.call.startedAtMilliseconds.addingReportingOverflow(duration)
    guard !durationOverflow, !endOverflow else { throw DeliveryLedgerFailure.invalidStoredPayload }
    return CalendarEventPayload(
      marker: intent.marker,
      title: title,
      startAtMilliseconds: intent.call.startedAtMilliseconds,
      endAtMilliseconds: end
    )
  }
}
