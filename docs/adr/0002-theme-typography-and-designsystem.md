# ADR 0002: Theme, Typography, and Design System

## Status
Accepted

## Context
Section 30 of [BUILD_GUIDE.md](file:///C:/Users/Musid/.gemini/antigravity/scratch/document-scanner/BUILD_GUIDE.md) specifies a Material 3 green-black and warm-gold design system.
Key constraints:
- Dark theme by default, with Light and System options persisted across sessions.
- Dynamic wallpaper color disabled to preserve identity.
- Exact tokens:
  - Dark: Background `#111411`, Surface `#1C211D`, Primary Gold `#D9AC46`, onPrimary `#111411`, onSurface `#F6F2E9`, onSurfaceVariant `#B9C0B8`, borderSubtle `#354038`, success `#75B890`, warningAccent `#E4B765`, error `#CF6679`.
  - Light: Background `#F6F7F5`, Surface `#FFFFFF`, Primary Gold `#8A6014`, onPrimary `#FFFFFF`, onSurface `#141915`, onSurfaceVariant `#47524A`, borderSubtle `#CCD4CD`, success `#1B5E20`, warningAccent `#B76E00`, error `#BA1A1A`.
- Accessibility adaptations:
  - `warningText`: Dark `#E4B765`, Light `#8A5200` (meets 4.5:1 contrast).
  - `outlineStrong`: Dark `#78877C`, Light `#718076`.
- App text sizing:
  - Four levels: Small (0.90x), Default (1.00x), Large (1.15x), Extra Large (1.30x).
  - Scaled via multiplier on top of Android system font scale (using `sp`).
  - Document image previews themselves are NEVER recolored or rescaled by app theming.

## Decisions
1. Implement `ScannerTheme` in `core/designsystem` providing both standard `MaterialTheme` colors and custom extended `ScannerColors` (for success, warning, borderSubtle, outlineStrong).
2. Store `AppearanceSettings` (`ThemeMode`, `AppTextSize`) in DataStore / Room Preferences.
3. Typography scale provides scaled `TextStyle` definitions calculated dynamically from `AppTextSize.multiplier`.
4. Faithful document rendering: preview surfaces use neutral backings with sharp page boundaries so paper color is unchanged between light and dark modes.

## Consequences
- High contrast across both themes complying with WCAG 2.2 contrast requirements.
- Full support for users requiring larger text without clipping critical buttons or metadata.
