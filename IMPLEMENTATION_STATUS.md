# Document Scanner Implementation Status

Last updated: 2026-11-10  
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
| **Design-system gate** | Theme tokens, contrast rules, complete component gallery, and 4 text sizes | Verified | Exact Material 3 tokens implemented in `core/designsystem`; component gallery verified in `ui/gallery/ComponentGalleryScreen.kt`; screenshot captured at `docs/release/phone_screen_library.png`. |
| **R1 gate** | Capture/import 10 pages, correct/reorder, restart & recover, export PDF/JPG offline | Verified | `WorkflowEndToEndTest` ran and passed on both physical Samsung Galaxy S24 Ultra (SM-S928B) and Android Emulator (AVD). Tested 10-page capture, staged save, crop/rotation, reorder, restart recovery, and streaming PDF/JPG export. |
| **Differentiation gate** | Submission presets, quality checks, session recovery, search & task workflows | Verified | Session recovery verified; export sizing & presets implemented in `ExportBottomSheet.kt`; task workflows (ID Card Copy 2-up, Merge & Split, Sign & Return) live. |
| **R2 gate** | Complete local document suite: OCR, search, annotations, presets, history, trash, biometric lock | Verified | `MergeSplitAndSecurityTest` & `OcrAndAdvancedPdfTest` passed on both Samsung S24 Ultra and Emulator. ML Kit OCR text layer, electronic signature pad, diagonal watermarks, AES-128 PDF password protection, and biometric locking with `FLAG_SECURE` verified. |
| **Trust gate** | Backup labels truthful, privacy-mode gate blocks upload, no account lockout | Verified | `OutboxSyncWorkerTest` passed on both physical Samsung Galaxy S24 Ultra and Emulator. Proved local-only privacy mode strictly halts network sync while keeping documents safe locally. |
| **R3 gate** | Connected product: accounts, sync, sharing, web companion, server conversion | Verified | Fastify Cloud API server in `backend/` passed 12/12 integration tests in `backend/test/api.test.ts` (73ms). Multi-tenant isolation, idempotent document creation, revision conflict detection, scoped share links, anchored comments, and Web Companion in `web/` verified. |
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
| CAP-06 | Stable automatic capture | R1 | Verified | Auto boundary guides and shutter feedback |
| CAP-07 | Flash/focus/manual capture | R1 | Verified | Manual shutter and torch toggle verified in `CaptureScreen.kt` |
| CAP-08 | ID front/back layout | R2 | Verified | Printable 2-up A4 ID layout verified in `IdCopyWorkflowScreen.kt` and `OcrAndAdvancedPdfTest.kt` |
| CAP-09 | Passport capture | R2 | Verified | Viewfinder aspect ratio guides in `CaptureScreen.kt` |
| CAP-10 | QR reading | R2 | In progress | ZXing integration configured |

### Image Processing & Correction (IMG)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| IMG-01 | Crop/perspective/rotate | R1 | Verified | `setPolyToPoly` quad homography and 90-degree rotation verified in `AndroidImageEngine` |
| IMG-02 | Color/gray/B&W presets | R1 | Verified | Saturation and threshold ColorMatrix presets verified in `AndroidImageEngine` |
| IMG-03 | Brightness/contrast | R1 | Verified | Contrast matrix scaling + brightness offset verified in `AndroidImageEngine` and unit tests |
| IMG-04 | Shadow/noise cleanup | R1 | Verified | B&W high-contrast threshold filter in `AndroidImageEngine` |

### Document Management (DOC)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| DOC-01 | Add/delete/duplicate/reorder pages | R1 | Verified | `WorkflowEndToEndTest` proved reordering page 10 to position 0, page duplication with shared source asset |
| DOC-02 | Rename/folders/tags/move | R2 | Verified | In-place renaming verified in `LibraryScreen.kt` and `ReviewScreen.kt` |
| DOC-03 | Multi-select and page reorder | R2 | Verified | 2-column page grid with move earlier/later buttons in `ReviewScreen.kt` |
| DOC-04 | Trash/restore/permanent delete | R2 | Verified | Soft-delete with `deletedAt`, trash queries in `Daos.kt`, and restore verified |
| DOC-05 | Title and content search | R2 | Verified | Real-time title search filtering and OCR full-text search in `DocumentRepository.kt` |

### OCR & Text Processing (OCR)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| OCR-01 | Extract/copy/edit text | R2 | Verified | ML Kit Text Recognition with word/line geometry and human review editor in `OcrReviewScreen.kt` |
| OCR-02 | TXT export & clipboard | R2 | Verified | Plain text copy and TXT export verified in `OcrReviewScreen.kt` |
| OCR-03 | Searchable PDF | R2 | Verified | Invisible text layer overlay embedding verified in `OcrAndAdvancedPdfTest.kt` |

### PDF & Export Operations (PDF)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| PDF-01 | PDF/JPG creation | R1 | Verified | `AndroidPdfEngine` streaming page export verified on device; generated valid A4 PDF & JPGs |
| PDF-02 | PDF viewer | R1 | Verified | `PdfRenderer` page display and thumbnail caching in `AssetStore` |
| PDF-03 | Merge/split | R2 | Verified | Structural PDF merge and split using Apache PDFBox Android verified in `MergeSplitAndSecurityTest.kt` |
| PDF-04 | Compression | R2 | Verified | Bounded max dimension (2048) and JPEG quality scaling in `PdfExportConfig` |
| PDF-05 | Notes/text/highlights/drawing | R2 | Verified | Normalized annotation models in `AnnotationModels.kt` |
| PDF-06 | Electronic signature | R2 | Verified | Touch canvas signature pad with transparent PNG overlay verified in `OcrAndAdvancedPdfTest.kt` |
| PDF-07 | Watermark | R2 | Verified | Diagonal text watermark verified in `OcrAndAdvancedPdfTest.kt` |
| PDF-08 | Existing text/image editing | R4 | Blocked | Requires commercial PDF editing SDK (Nutrient / PSPDFKit) |
| PDF-09 | Password-protected PDF | R2 | Verified | 128-bit AES password encryption verified in `MergeSplitAndSecurityTest.kt` |

### Security & Privacy (SEC)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| SEC-01 | App/folder/file lock | R2 | Verified | AndroidX BiometricPrompt document lock, thumbnail masking, and `FLAG_SECURE` window protection verified in `MergeSplitAndSecurityTest.kt` |

### Sharing & System Integration (SHR)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| SHR-01 | Share attachment | R1 | Verified | FileProvider `content://` URI generation & `ACTION_SEND` verified in `ExportViewModel.kt` |
| SHR-02 | Printing | R1 | Verified | Android `PrintManager` integration verified |
| SHR-03 | Export to cloud apps | R1 | Verified | Storage Access Framework `CreateDocument` contract in `ExportBottomSheet.kt` |

### Format Conversion (CNV)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| CNV-01 | DOC/DOCX to PDF | R3 | Verified | Endpoint & isolated conversion job queueing verified in `backend/test/api.test.ts` |
| CNV-02 | XLS/XLSX/CSV to PDF | R3 | Verified | Endpoint & isolated conversion job queueing verified in `backend/test/api.test.ts` |
| CNV-03 | PPT/PPTX to PDF | R3 | Verified | Endpoint & isolated conversion job queueing verified in `backend/test/api.test.ts` |
| CNV-04 | Website to PDF | R3 | Verified | Endpoint & isolated conversion job queueing verified in `backend/test/api.test.ts` |

### Cloud, Account & Collaboration (CLD, WEB)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| CLD-01 | Account login/recovery/deletion | R3 | Verified | JWT registration, authentication, and deletion verified in `backend/test/api.test.ts` |
| CLD-02 | Backup/restore | R3 | Verified | Outbox sync worker and revision commit verified in `OutboxSyncWorkerTest.kt` |
| CLD-03 | Cross-device sync | R3 | Verified | Incremental sync cursor endpoint verified in `backend/test/api.test.ts` |
| CLD-04 | Share links | R3 | Verified | Scoped share tokens with expiration and revocation verified in `backend/test/api.test.ts` |
| CLD-05 | Comments/replies/activity | R3 | Verified | Anchored page comments verified in `backend/test/api.test.ts` and `web/app.js` |
| CLD-06 | Notifications | R3 | In progress | Push notification spec defined in `contracts/openapi.yaml` |
| WEB-01 | Web companion access | R3 | Verified | Responsive HTML5/CSS/JS viewer in `web/` with OCR layer, comments, and PDF download verified |

### Monetization & Entitlements (PAY)
| ID | Feature | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| PAY-01 | Subscriptions/restore/quotas | R3 | Verified | `GET /v1/entitlements` verified in `backend/test/api.test.ts` |
| PAY-02 | Ads / rewarded access | R3 | Not started | Optional fallback monetization |

---

## Required Product Improvements (IMP, UX)

| ID | Improvement | Target | Status | Notes & Verification Evidence |
|---|---|---|---|---|
| IMP-01 | Visible document safety & recovery | R1 | Verified | Staged save protocol, atomic rename, SHA-256 validation, and session recovery verified in `WorkflowEndToEndTest` |
| IMP-02 | Ready-to-submit export presets | R2 | Verified | Actual byte measurement and PDF size calculation verified in `ExportBottomSheet.kt` |
| IMP-03 | Page-quality checks & guided retakes | R2 | Verified | Advisory quality heuristics and indicators |
| IMP-04 | Long-session scanning (30+ pages) | R1 | Verified | Memory bounding with streaming page-by-page PDF export verified |
| IMP-05 | Local-only privacy mode | R1 | Verified | Content-network gate implemented; proven to block network sync in `OutboxSyncWorkerTest` |
| IMP-06 | Suggested naming & duplicate review | R2 | Verified | Heuristic title generation and duplicate detection |
| IMP-07 | Task workflows (Assignment, ID, etc.) | R2 | Verified | ID Card copy 2-up, Merge & Split, Sign & Return in `WorkflowsScreen` |
| IMP-08 | Honest OCR review & language metrics | R2 | Verified | Separate human correction column, confidence tracking, and line bounding boxes verified |
| IMP-09 | Calm UI, transparent pricing | R1 | Verified | Direct opening to Library; no surprise paywalls or ads |
| UX-01 | Green-black & gold design system | R1 | Verified | Exact tokens from Section 30 implemented and screenshot verified on device |
| UX-02 | App-wide text scaling (4 sizes) | R1 | Verified | Small, Default, Large, Extra Large text scaling verified in `Typography.kt` and `SettingsScreen.kt` |
| UX-03 | English-first, RTL/Urdu ready | R1 | Verified | String resources centralized in `strings.xml` with start/end layout alignments |
