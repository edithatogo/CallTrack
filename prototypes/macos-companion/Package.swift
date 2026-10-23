// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "CallTrackMacImport",
    platforms: [.macOS(.v14)],
    products: [.library(name: "CallTrackImport", targets: ["CallTrackImport"])],
    targets: [
        .target(name: "CallTrackImport"),
        .testTarget(name: "CallTrackImportTests", dependencies: ["CallTrackImport"]),
    ]
)
