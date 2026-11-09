# ADR 0001: Architecture, Storage, and Capture Engine Selection

## Status
Accepted

## Context
The application is a high-reliability Android document scanner built with Kotlin and Jetpack Compose.
Key requirements from [BUILD_GUIDE.md](file:///C:/Users/Musid/.gemini/antigravity/scratch/document-scanner/BUILD_GUIDE.md):
- Local-first operation: scanning, editing, OCR, and export must work offline without forced account creation.
- Crash resilience: multi-page capture sessions must survive unexpected process death.
- Original assets must be immutable; user edits are recipes (non-destructive).
- Visual design: Material 3 green-black and gold palette with app-wide font scaling.

## Decisions

1. **Architecture**: Modular monolith following Clean Architecture (UI -> ViewModel -> Use Case -> Repository/Engine -> Local Data/Filesystem).
2. **Local Persistence**:
   - Metadata: Android Room (SQLite) with explicit schemas, transactions, and migration plans.
   - Assets: App-private internal storage (`context.filesDir/assets/{assetId}/original.jpg`) using staged writes (temp file -> flush -> atomic rename).
3. **Capture Strategy**:
   - Primary: Google Play Services ML Kit Document Scanner API (`play-services-mlkit-document-scanner`) for automatic edge detection, perspective correction, and high-quality scanning.
   - Custom Camera Fallback: CameraX (`androidx.camera:camera-camera2`) with manual capture and edge overlay for environments where ML Kit scanner is unavailable.
   - Gallery & File Import: Android Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia`) and SAF (`OpenDocument`) with immediate staging into private asset storage.
4. **Session Recovery**:
   - Every page captured or imported is staged immediately to an active `CaptureSession` in the database.
   - On app startup, incomplete sessions are detected and presented to the user with `Continue scanning`, `Review pages`, or `Discard session`.

## Consequences
- No network connection or account is required for any scanning, editing, or export operations.
- Zero risk of losing captured pages upon unexpected process kill.
- Original images remain untouched; users can always reset crops or filters.
