import EventKit

// Compile-only SDK surface probe. No entry point, store creation or API invocation.
// Never link this probe into the app as a permission or reconciliation adapter.
@available(macOS 14.0, *)
func checkEventKitSurface(_ store: EKEventStore) {
    let fullAccess: () async throws -> Bool = store.requestFullAccessToEvents
    let writeOnlyAccess: () async throws -> Bool = store.requestWriteOnlyAccessToEvents
    let calendars: (EKEntityType) -> [EKCalendar] = store.calendars(for:)
    let event: (String) -> EKEvent? = store.event(withIdentifier:)
    // Keep references type-checked without executing them or reading account state.
    _ = (fullAccess, writeOnlyAccess, calendars, event)
}
