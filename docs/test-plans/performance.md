# Performance Measurement and Quality Budgets

## Reference Hardware
- Primary Physical Device: Samsung Galaxy S24 Ultra (SM-S928B, Android 15/17, Snapdragon 8 Gen 3)
- Reference Emulator: Pixel 8 API 35 (x86_64, 4GB RAM)

## Numerical Performance Budgets

| Metric | Target Budget | Maximum Allowed | Verification Method |
|---|---|---|---|
| Camera Viewfinder Startup | < 500 ms | 1000 ms | Cold activity launch to first preview frame |
| Capture to Durable Save | < 350 ms | 700 ms | Shutter press to atomic file rename + Room commit |
| Real-time Crop / Filter Preview | < 120 ms | 250 ms | Drag handle movement to preview canvas render |
| Full-Resolution Page Export | < 600 ms / page | 1200 ms / page | High-DPI perspective warp + color matrix |
| 10-Page PDF Assembly | < 4.5 s | 8.0 s | Streaming PdfDocument generation |
| Memory Ceiling during 30-Page Session | < 180 MB | 250 MB | Low-resolution thumbnail caching; no full bitmap retention |
| App Cold Start to Library | < 650 ms | 1200 ms | Process start to first Compose frame |

## Memory Bounding Rules
1. Never decode all pages into full-resolution ARGB_8888 bitmaps simultaneously.
2. Render thumbnails at max 400x400 dp; recycle immediately.
3. Stream PDF pages: render 1 page -> write to `PdfDocument` -> recycle bitmap -> proceed to next page.
4. Temporary exports are placed in `cache/temporary-share-exports` and purged upon session completion.
