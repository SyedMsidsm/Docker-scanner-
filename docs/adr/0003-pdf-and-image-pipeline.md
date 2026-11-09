# ADR 0003: PDF and Image Processing Pipeline

## Status
Accepted

## Context
The scanner requires:
1. Fast image enhancement: crop, perspective transformation, rotation, grayscale/B&W filters, brightness/contrast.
2. Memory-bounded processing: 10 to 30+ page sessions must not cause OutOfMemory errors.
3. PDF Generation & Viewing: generating multi-page PDFs with selectable paper sizes (A4, Letter, Auto), compression, and independent verification.
4. Export options: PDF, multi-page JPG, single JPGs, with Android standard sharing and printing.

## Decisions
1. **Image Engine**:
   - Native Android `android.graphics.Matrix` and `android.graphics.Canvas` for perspective warp and rotations.
   - `android.graphics.ColorFilter` and `android.graphics.ColorMatrix` for real-time B&W, grayscale, contrast, and brightness adjustments.
   - Low-resolution thumbnails for editing preview; full-resolution rendering performed only on export or durable recipe commit.
2. **PDF Engine**:
   - Core generation: `android.graphics.pdf.PdfDocument` with streaming page creation (render page -> write to PDF -> recycle bitmap -> next page).
   - In-app viewing: `android.graphics.pdf.PdfRenderer` with page-by-page rendering on demand.
   - Sharing & Storage: Android `FileProvider` with temporary content URIs (`androidx.core.content.FileProvider`) and SAF `CreateDocument`.
   - Printing: Android `android.print.PrintManager` with custom `PrintDocumentAdapter`.

## Consequences
- No reliance on large unverified external C++ binaries for core R1 operations.
- Smooth rendering on 2GB-4GB RAM devices.
- Fully compatible with standard PDF readers (Adobe Acrobat, Chrome, Google Drive).
