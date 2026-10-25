// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "CallTrackMacImport",
    platforms: [.macOS(.v14)],
    products: [
        .library(name: "CallTrackImport", targets: ["CallTrackImport"]),
        .library(name: "CallTrackCompanionUI", targets: ["CallTrackCompanionUI"]),
        .library(name: "CallTrackDelivery", targets: ["CallTrackDelivery"]),
        .executable(name: "delivery-ledger-contract", targets: ["DeliveryLedgerContractHarness"]),
    ],
    targets: [
        .systemLibrary(name: "CSQLite"),
        .target(name: "CallTrackImport"),
        .target(name: "CallTrackCompanionUI", dependencies: ["CallTrackImport"]),
        .target(name: "CallTrackDelivery", dependencies: ["CallTrackImport", "CSQLite"]),
        .executableTarget(
            name: "DeliveryLedgerContractHarness",
            dependencies: ["CallTrackDelivery", "CallTrackImport"]
        ),
        .testTarget(name: "CallTrackImportTests", dependencies: ["CallTrackImport"]),
    ]
)
