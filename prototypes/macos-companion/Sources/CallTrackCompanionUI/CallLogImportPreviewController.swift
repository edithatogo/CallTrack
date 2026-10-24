import AppKit
import CallTrackImport
import UniformTypeIdentifiers

/// Native file selection and preview only. It has no calendar adapter or write action.
@MainActor
public final class CallLogImportPreviewController: NSViewController {
    private let chooseButton = NSButton(title: "Choose JSON file", target: nil, action: nil)
    private let previewLabel = NSTextField(wrappingLabelWithString: "Choose a JSON file to validate and preview.")
    private let statusLabel = NSTextField(wrappingLabelWithString: "")
    private let errorLabel = NSTextField(wrappingLabelWithString: "")
    private let deliveryButton = NSButton(title: "Calendar delivery is not available", target: nil, action: nil)

    public init() {
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("CallLogImportPreviewController does not load from a storyboard.")
    }

    public override func loadView() {
        chooseButton.target = self
        chooseButton.action = #selector(chooseFile)
        chooseButton.setAccessibilityHelp("Opens a file picker. The selected file is validated locally.")

        deliveryButton.isEnabled = false
        deliveryButton.setAccessibilityHelp("A calendar destination and recovery path have not been qualified.")

        statusLabel.stringValue = "No calendar access is requested. No events are written."
        errorLabel.textColor = .systemRed
        errorLabel.isHidden = true

        let stack = NSStackView(views: [chooseButton, previewLabel, errorLabel, deliveryButton, statusLabel])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 14
        stack.edgeInsets = NSEdgeInsets(top: 24, left: 24, bottom: 24, right: 24)
        stack.translatesAutoresizingMaskIntoConstraints = false

        let container = NSView()
        container.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            stack.trailingAnchor.constraint(lessThanOrEqualTo: container.trailingAnchor),
            stack.topAnchor.constraint(equalTo: container.topAnchor),
            stack.bottomAnchor.constraint(lessThanOrEqualTo: container.bottomAnchor),
            stack.widthAnchor.constraint(lessThanOrEqualToConstant: 560),
        ])
        view = container
    }

    @objc
    private func chooseFile() {
        guard let window = view.window else { return }
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.json]
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.prompt = "Preview"

        panel.beginSheetModal(for: window) { [weak self] response in
            guard response == .OK, let url = panel.url else { return }
            self?.loadPreview(from: url)
        }
    }

    private func loadPreview(from url: URL) {
        errorLabel.isHidden = true
        previewLabel.stringValue = "Validating selected file…"

        do {
            let preview = try CallLogFilePreviewer.preview(fileAt: url)
            let incoming = preview.calls.filter { $0.direction == .incoming }.count
            let outgoing = preview.calls.filter { $0.direction == .outgoing }.count
            let missed = preview.calls.filter { $0.direction == .missed }.count
            previewLabel.stringValue = "Review preview: \(preview.calls.count) calls\nIncoming: \(incoming)\nOutgoing: \(outgoing)\nMissed: \(missed)"
            statusLabel.stringValue = "Preview only. No call times, phone numbers, or record IDs are shown. No calendar events were added."
        } catch let failure as ImportFailure {
            previewLabel.stringValue = "No valid preview is available."
            errorLabel.stringValue = message(for: failure)
            errorLabel.isHidden = false
        } catch {
            previewLabel.stringValue = "No valid preview is available."
            errorLabel.stringValue = "The selected file could not be read."
            errorLabel.isHidden = false
        }
    }

    private func message(for failure: ImportFailure) -> String {
        switch failure {
        case .fileTooLarge:
            "The selected file exceeds the preview size limit."
        case .unsupportedVersion:
            "The selected file uses an unsupported format."
        default:
            "The selected file failed validation."
        }
    }
}
