# Changelog

## Unreleased

Early desktop MVP for local photo archive cleanup.

### Added

- Recursive source folder scanning.
- Scan only, copy, and move import modes.
- Import plan based on year/month folders.
- JPEG EXIF capture date reading with file date fallback.
- Exact duplicate detection with SHA-256.
- Duplicate quarantine in `Duplicates`.
- Unsupported file quarantine in `Unsupported`.
- Safe deletion from `Duplicates` and `Unsupported`.
- Empty source subfolder cleanup after move import.
- Photo grid, grouped library views, inspector, search, and filters.
- Video thumbnail fallback through macOS QuickLook.
- Import history and import summary actions.
- macOS app icon and DMG packaging.

### Notes

- This is a preview build.
- Test on a copy of important archives before using move mode on irreplaceable files.
