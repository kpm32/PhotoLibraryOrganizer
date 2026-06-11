# Changelog

## 1.0.0-preview.2 - 2026-06-11

Preview polish release.

### Changed

- Improved first launch window size and minimum window size.
- Updated the visible window title to `Photo Library Organizer`.
- Polished Russian UI labels and folder action issue titles.

## 1.0.0-preview - 2026-06-11

Early desktop MVP for local photo archive cleanup.

### Added

- Recursive source folder scanning.
- Scan only, copy, and move import modes.
- Import plan based on year/month folders.
- JPEG EXIF capture date reading with file date fallback.
- HEIC and video date fallback through macOS metadata.
- Legacy video format detection: `vob`, `wmv`, `flv`, `f4v`, `ogv`, `asf`, `divx`, `mod`, `tod`, `3g2`.
- Exact duplicate detection with SHA-256.
- Duplicate quarantine in `Duplicates`.
- Unsupported file quarantine in `Unsupported`.
- Safe deletion from `Duplicates` and `Unsupported`.
- Unsupported groups with counts, total size, and Finder opening by type.
- Empty source subfolder cleanup after move import.
- Import result verification and per-file failure details.
- Re-run friendly import: already existing target files are skipped.
- Photo grid, grouped library views, inspector, search, and filters.
- Large preview overlay and keyboard navigation.
- Separate captured date, file date, and folder date in the inspector.
- Video thumbnail fallback through macOS QuickLook.
- Import history and import summary actions.
- macOS app icon and DMG packaging.

### Notes

- This is a preview build.
- Test on a copy of important archives before using move mode on irreplaceable files.
