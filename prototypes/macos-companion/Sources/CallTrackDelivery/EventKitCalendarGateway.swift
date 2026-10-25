import CallTrackImport
import EventKit
import Foundation

public enum EventCalendarAuthorization: Equatable, Sendable {
  case notDetermined
  case denied
  case restricted
  case writeOnly
  case fullAccess
}

public struct CalendarDestination: Equatable, Sendable {
  public let identifier: String
  public let title: String
  public let allowsModifications: Bool

  public init(identifier: String, title: String, allowsModifications: Bool) {
    self.identifier = identifier
    self.title = title
    self.allowsModifications = allowsModifications
  }
}

public struct CalendarEventPayload: Equatable, Sendable {
  public let marker: String
  public let title: String
  public let startAtMilliseconds: Int64
  public let endAtMilliseconds: Int64

  public init(marker: String, title: String, startAtMilliseconds: Int64, endAtMilliseconds: Int64) {
    self.marker = marker
    self.title = title
    self.startAtMilliseconds = startAtMilliseconds
    self.endAtMilliseconds = endAtMilliseconds
  }
}

public struct CalendarEventSnapshot: Equatable, Sendable {
  public let identifier: String
  public let calendarIdentifier: String
  public let marker: String?
  public let title: String
  public let startAtMilliseconds: Int64
  public let endAtMilliseconds: Int64
  public let isAllDay: Bool
  public let attendeeCount: Int
  public let alarmCount: Int

  public init(
    identifier: String,
    calendarIdentifier: String,
    marker: String?,
    title: String,
    startAtMilliseconds: Int64,
    endAtMilliseconds: Int64,
    isAllDay: Bool,
    attendeeCount: Int,
    alarmCount: Int
  ) {
    self.identifier = identifier
    self.calendarIdentifier = calendarIdentifier
    self.marker = marker
    self.title = title
    self.startAtMilliseconds = startAtMilliseconds
    self.endAtMilliseconds = endAtMilliseconds
    self.isAllDay = isAllDay
    self.attendeeCount = attendeeCount
    self.alarmCount = alarmCount
  }

  public func exactlyMatches(_ payload: CalendarEventPayload, calendarID: String) -> Bool {
    identifier.isEmpty == false
      && calendarIdentifier == calendarID
      && marker == payload.marker
      && title == payload.title
      && startAtMilliseconds == payload.startAtMilliseconds
      && endAtMilliseconds == payload.endAtMilliseconds
      && !isAllDay
      && attendeeCount == 0
      && alarmCount == 0
  }
}

@MainActor
public protocol EventCalendarGateway: AnyObject {
  func authorization() -> EventCalendarAuthorization
  func requestFullAccess() async throws -> Bool
  func writableCalendars() throws -> [CalendarDestination]
  func insert(_ payload: CalendarEventPayload, into calendarID: String) throws
  func event(identifier: String) -> CalendarEventSnapshot?
  func events(matching marker: String, payload: CalendarEventPayload, calendarID: String) throws
    -> [CalendarEventSnapshot]
}

public enum CalendarDeliveryFailure: Error, Equatable {
  case accessNotGranted
  case writeOnlyAccessInsufficient
  case missingUsageDescription
  case destinationUnavailable
  case userConfirmationRequired
  case operationNotPrepared
  case outcomeUncertain
  case conflictingReadback
}

/// EventKit adapter for explicit, single-destination delivery. Construction does
/// not request access; the host must call `requestFullAccess` after user intent.
@MainActor
public final class EventKitCalendarGateway: EventCalendarGateway {
  private let store: EKEventStore

  public init(store: EKEventStore = EKEventStore()) {
    self.store = store
  }

  public func authorization() -> EventCalendarAuthorization {
    switch EKEventStore.authorizationStatus(for: .event) {
    case .notDetermined:
      .notDetermined
    case .denied:
      .denied
    case .restricted:
      .restricted
    case .writeOnly:
      .writeOnly
    case .fullAccess:
      .fullAccess
    @unknown default:
      .denied
    }
  }

  public func requestFullAccess() async throws -> Bool {
    switch authorization() {
    case .fullAccess:
      return true
    case .writeOnly:
      throw CalendarDeliveryFailure.writeOnlyAccessInsufficient
    case .denied, .restricted:
      throw CalendarDeliveryFailure.accessNotGranted
    case .notDetermined:
      break
    }
    guard
      let explanation = Bundle.main.object(
        forInfoDictionaryKey: "NSCalendarsFullAccessUsageDescription") as? String,
      !explanation.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    else {
      throw CalendarDeliveryFailure.missingUsageDescription
    }
    return try await withCheckedThrowingContinuation { continuation in
      store.requestFullAccessToEvents { granted, error in
        if let error {
          continuation.resume(throwing: error)
        } else {
          continuation.resume(returning: granted)
        }
      }
    }
  }

  public func writableCalendars() throws -> [CalendarDestination] {
    try requireFullAccess()
    return store.calendars(for: .event)
      .filter(\.allowsContentModifications)
      .map {
        CalendarDestination(
          identifier: $0.calendarIdentifier,
          title: $0.title,
          allowsModifications: $0.allowsContentModifications
        )
      }
      .sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
  }

  public func insert(_ payload: CalendarEventPayload, into calendarID: String) throws {
    try requireFullAccess()
    guard let calendar = store.calendar(withIdentifier: calendarID),
      calendar.allowsContentModifications
    else {
      throw CalendarDeliveryFailure.destinationUnavailable
    }
    let event = EKEvent(eventStore: store)
    event.calendar = calendar
    event.title = payload.title
    event.startDate = Self.date(payload.startAtMilliseconds)
    event.endDate = Self.date(payload.endAtMilliseconds)
    event.isAllDay = false
    event.notes = payload.marker
    event.alarms = []
    try store.save(event, span: .thisEvent)
  }

  public func event(identifier: String) -> CalendarEventSnapshot? {
    guard let event = store.event(withIdentifier: identifier) else { return nil }
    return snapshot(event)
  }

  public func events(
    matching marker: String,
    payload: CalendarEventPayload,
    calendarID: String
  ) throws -> [CalendarEventSnapshot] {
    try requireFullAccess()
    guard let calendar = store.calendar(withIdentifier: calendarID) else {
      throw CalendarDeliveryFailure.destinationUnavailable
    }
    let start = Self.date(payload.startAtMilliseconds).addingTimeInterval(-1)
    let end = Self.date(payload.endAtMilliseconds).addingTimeInterval(1)
    let predicate = store.predicateForEvents(withStart: start, end: end, calendars: [calendar])
    return store.events(matching: predicate)
      .filter { $0.notes == marker }
      .map(snapshot)
  }

  private func requireFullAccess() throws {
    switch authorization() {
    case .fullAccess:
      return
    case .writeOnly:
      throw CalendarDeliveryFailure.writeOnlyAccessInsufficient
    case .notDetermined, .denied, .restricted:
      throw CalendarDeliveryFailure.accessNotGranted
    }
  }

  private func snapshot(_ event: EKEvent) -> CalendarEventSnapshot {
    CalendarEventSnapshot(
      identifier: event.eventIdentifier ?? "",
      calendarIdentifier: event.calendar.calendarIdentifier,
      marker: event.notes,
      title: event.title ?? "",
      startAtMilliseconds: Self.milliseconds(event.startDate),
      endAtMilliseconds: Self.milliseconds(event.endDate),
      isAllDay: event.isAllDay,
      attendeeCount: event.attendees?.count ?? 0,
      alarmCount: event.alarms?.count ?? 0
    )
  }

  private static func date(_ milliseconds: Int64) -> Date {
    Date(timeIntervalSince1970: Double(milliseconds) / 1_000)
  }

  private static func milliseconds(_ date: Date) -> Int64 {
    Int64((date.timeIntervalSince1970 * 1_000).rounded())
  }
}
