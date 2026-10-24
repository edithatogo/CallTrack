// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "CallTrackMacImport",
    platforms: [.macOS(.v14)],
    products: [
        .library(name: "CallTrackImport", targets: ["CallTrackImport"]),
        .library(name: "CallTrackCompanionUI", targets: ["CallTrackCompanionUI"]),
    ],
    targets: [
        .target(name: "CallTrackImport"),
        .target(name: "CallTrackCompanionUI", dependencies: ["CallTrackImport"]),
        .testTarget(name: "CallTrackImportTests", dependencies: ["CallTrackImport"]),
    ]
)
