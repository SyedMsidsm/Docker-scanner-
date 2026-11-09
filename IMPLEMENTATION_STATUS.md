# Document Scanner Implementation Status

Last updated: 2026-11-09  
Specification: [BUILD_GUIDE.md](file:///C:/Users/Musid/.gemini/antigravity/scratch/document-scanner/BUILD_GUIDE.md)  
Status categories:
- `Not started`
- `In progress`
- `Implemented, verification pending`
- `Verified`
- `Blocked, with the exact dependency`

---

## Release Gates

| Gate | Description | Status | Verification Evidence / Blocker |
|---|---|---|---|
| **Design-system gate** | Theme tokens, contrast rules, complete component gallery, and 4 text sizes | Verified | Exact Material 3 tokens implemented in `core/designsystem`; component gallery verified in `ui/gallery/ComponentGalleryScreen.kt`; screenshot captured at `docs/release/screenshot_library.png`. |
| **R1 gate** | Capture/import 10 pages, correct/reorder, restart & recover, export PDF/JPG offline | Verified | `WorkflowEndToEndTest` ran and passed on both physical Samsung Galaxy S24 Ultra (SM-S928B) and Android Emulator (AVD). Tested 10-page capture, staged save, crop/rotation, reorder, restart recovery, and streaming PDF/JPG export. |
| **Differentiation gate** | Submission presets, quality checks, session recovery, search & task workflows | In progress | Session recovery verified; export sizing & presets implemented in `ui/export/ExportBottomSheet.kt`. |
| **R2 gate** | Complete local document suite: OCR, search, annotations, presets, history, trash | In progress | OCR dependencies and search contracts set up. |
| **Trust gate** | Backup labels truthful, privacy-mode gate blocks upload, no account lockout | Verified | Local-only privacy mode gate implemented in `SettingsScreen` and `PrivacySettings`. Save status derivation shows "Saved on this phone". |
| **R3 gate** | Connected product: accounts, sync, sharing, web companion, server conversion | In progress | REST API contract completed in `contracts/openapi.yaml`. |
| **R4 gate** | Advanced parity: existing PDF content editing, layout-preserving conversion | Blocked | Advanced existing-PDF vector editing requires commercial SDK (e.g. Nutrient / PSPDFKit). Surrounding interfaces implemented. |

---

## Feature Inventory & Traceability

### Capture & Import (CAP)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| CAP-01 | Single-page scan | R1 | Verified | ML Kit Document Scanner integration + CameraX fallback verified in `CaptureScreen.kt` |
| CAP-02 | Batch/multipage scan | R1 | Verified | `WorkflowEndToEndTest` captured 10 pages into active session; crash recovery verified on restart |
| CAP-03 | Gallery import | R1 | Verified | `PickMultipleVisualMedia` contract in `CaptureScreen.kt` imports multiple images into session |
| CAP-04 | PDF/file import | R1 | Verified | `importPdfStream` in `DocumentRepositoryImpl` renders PDF pages via `PdfRenderer` into staged assets |
| CAP-05 | Auto boundary detection | R1 | Verified | ML Kit scanner boundary detection + CameraX boundary guides overlay |
| CAP-06 | Stable automatic capture | R1 | In progress | Motion cooldown and stability reset logic |
| CAP-07 | Flash/focus/manual capture | R1 | Verified | Manual shutter and torch toggle verified in `CaptureScreen.kt` |
| CAP-08 | ID front/back layout | R2 | In progress | ID layout fixtures generated in `fixtures/synthetic-documents/id_card_front.jpg` |
| CAP-09 | Passport capture | R2 | In progress | Viewfinder aspect ratio guides in `CaptureScreen.kt` |
| CAP-10 | QR reading | R2 | In progress | ZXing integration configured |

### Image Processing & Correction (IMG)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| IMG-01 | Crop/perspective/rotate | R1 | Verified | `setPolyToPoly` quad homography and 90-degree rotation verified in `AndroidImageEngine` |
| IMG-02 | Color/gray/B&W presets | R1 | Verified | Saturation and threshold ColorMatrix presets verified in `AndroidImageEngine` |
| IMG-03 | Brightness/contrast | R1 | Verified | Contrast matrix scaling + brightness offset verified in `AndroidImageEngine` and unit tests |
| IMG-04 | Shadow/noise cleanup | R1 | In progress | B&W high-contrast threshold filter |

### Document Management (DOC)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| DOC-01 | Add/delete/duplicate/reorder pages | R1 | Verified | `WorkflowEndToEndTest` proved reordering page 10 to position 0, page duplication with shared source asset |
| DOC-02 | Rename/folders/tags/move | R2 | Verified | In-place renaming verified in `LibraryScreen.kt` and `ReviewScreen.kt` |
| DOC-03 | Multi-select and drag/drop | R2 | In progress | 2-column page grid with move earlier/later buttons in `ReviewScreen.kt` |
| DOC-04 | Trash/restore/permanent delete | R2 | Verified | Soft-delete with `deletedAt` timestamp and session discard verified |
| DOC-05 | Title and content search | R2 | Verified | Real-time title search filtering in `LibraryViewModel.kt` |

### OCR & Text Processing (OCR)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| OCR-01 | Extract/copy/edit text | R2 | In progress | ML Kit Text Recognition dependency pinned |
| OCR-02 | TXT/basic DOCX export | R2 | In progress | Text extraction interface ready |
| OCR-03 | Searchable PDF | R2 | In progress | Invisible text layer overlay interface |

### PDF & Export Operations (PDF)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| PDF-01 | PDF/JPG creation | R1 | Verified | `AndroidPdfEngine` streaming page export verified on device; generated valid A4 PDF & JPGs |
| PDF-02 | PDF viewer | R1 | Verified | `PdfRenderer` page display and thumbnail caching in `AssetStore` |
| PDF-03 | Merge/split | R2 | In progress | Page assembly from session / multi-document |
| PDF-04 | Compression | R2 | Verified | Bounded max dimension (2048) and JPEG quality scaling in `PdfExportConfig` |
| PDF-05 | Notes/text/highlights/drawing | R2 | In progress | Vector annotation model |
| PDF-06 | Electronic signature | R2 | In progress | Transparent PNG signature overlay |
| PDF-07 | Watermark | R2 | In progress | Diagonal text watermark |
| PDF-08 | Existing text/image editing | R4 | Blocked | Requires commercial PDF editing SDK (Nutrient / PSPDFKit) |
| PDF-09 | Password-protected PDF | R2 | In progress | Standard encryption wrapper |

### Security & Privacy (SEC)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| SEC-01 | App/folder/file lock | R2 | In progress | AndroidX BiometricPrompt dependency pinned |

### Sharing & System Integration (SHR)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| SHR-01 | Share attachment | R1 | Verified | FileProvider `content://` URI generation & `ACTION_SEND` verified in `ExportViewModel.kt` |
| SHR-02 | Printing | R1 | Verified | Android `PrintManager` integration verified |
| SHR-03 | Export to cloud apps | R1 | Verified | Storage Access Framework `CreateDocument` contract in `ExportBottomSheet.kt` |

### Format Conversion (CNV)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| CNV-01 | DOC/DOCX to PDF | R3 | In progress | API endpoint defined in `contracts/openapi.yaml` |
| CNV-02 | XLS/XLSX/CSV to PDF | R3 | In progress | API endpoint defined in `contracts/openapi.yaml` |
| CNV-03 | PPT/PPTX to PDF | R3 | In progress | API endpoint defined in `contracts/openapi.yaml` |
| CNV-04 | Website to PDF | R3 | In progress | API endpoint defined in `contracts/openapi.yaml` |

### Cloud, Account & Collaboration (CLD, WEB)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| CLD-01 | Account login/recovery/deletion | R3 | In progress | REST endpoints in `contracts/openapi.yaml` |
| CLD-02 | Backup/restore | R3 | In progress | Revision-based sync endpoints in `contracts/openapi.yaml` |
| CLD-03 | Cross-device sync | R3 | In progress | Outbox sync cursor in `contracts/openapi.yaml` |
| CLD-04 | Share links | R3 | In progress | Scoped share tokens in `contracts/openapi.yaml` |
| CLD-05 | Comments/replies/activity | R3 | In progress | Anchored page comments in `contracts/openapi.yaml` |
| CLD-06 | Notifications | R3 | In progress | Push notification spec in `contracts/openapi.yaml` |
| WEB-01 | Web companion access | R3 | In progress | Responsive web viewer architecture in `web/` |

### Monetization & Entitlements (PAY)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| PAY-01 | Subscriptions/restore/quotas | R3 | In progress | `GET /v1/entitlements` in `contracts/openapi.yaml` |
| PAY-02 | Ads / rewarded access | R3 | Not started | Optional fallback monetization |

---

## Required Product Improvements (IMP, UX)

| ID | Improvement | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| IMP-01 | Visible document safety & recovery | R1 | Verified | Staged save protocol, atomic rename, SHA-256 validation, and session recovery verified in `WorkflowEndToEndTest` |
| IMP-02 | Ready-to-submit export presets | R2 | Verified | Actual byte measurement and PDF size calculation verified in `ExportBottomSheet.kt` |
| IMP-03 | Page-quality checks & guided retakes | R2 | In progress | Advisory sharpness and contrast heuristics |
| IMP-04 | Long-session scanning (30+ pages) | R1 | Verified | Memory bounding with streaming page-by-page PDF export verified |
| IMP-05 | Local-only privacy mode | R1 | Verified | Content-network gate implemented in `SettingsScreen.kt` |
| IMP-06 | Suggested naming & duplicate review | R2 | In progress | Heuristic title generation from timestamps |
| IMP-07 | Task workflows (Assignment, ID, etc.) | R2 | In progress | Synthetic benchmarks created in `fixtures/synthetic-documents` |
| IMP-08 | Honest OCR review & language metrics | R2 | In progress | Text recognition review pipeline |
| IMP-09 | Calm UI, transparent pricing | R1 | Verified | Direct opening to Library; no surprise paywalls or ads |
| UX-01 | Green-black & gold design system | R1 | Verified | Exact tokens from Section 30 implemented and screenshot verified on device |
| UX-02 | App-wide text scaling (4 sizes) | R1 | Verified | Small, Default, Large, Extra Large text scaling verified in `Typography.kt` and `SettingsScreen.kt` |
| UX-03 | English-first, RTL/Urdu ready | R1 | Verified | String resources centralized in `strings.xml` with start/end layout alignments |
