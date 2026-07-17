# Changelog

## 1.0.0-preview.6 - 2026-07-17

Preview build focused on safer real-archive cleanup and faster startup for large libraries.

### Added

- Added a local library index that is loaded on startup and rebuilt after manual refresh or library-changing operations.
- Added inspector action to move only the currently selected library file to macOS Trash after confirmation.

### Changed

- Large libraries are no longer refreshed automatically on startup; refresh is now a manual action with visible progress.
- Cleanup from `Duplicates` and `Unsupported` moves files to macOS Trash after confirmation instead of permanently deleting them.
- The unsupported cleanup button now explicitly says it moves all skipped files to Trash.
- GitHub instructions now document preview download, Gatekeeper, large-library refresh, local index, and Trash behavior.

## 1.0.0-preview.5 - 2026-07-10

Preview build that completes the scan-reliability hotfix for external and changing archives.

### Changed

- Large libraries are no longer refreshed automatically on startup; refresh is now a manual action with visible progress.
- Added a local library index that is loaded on startup and rebuilt after manual refresh or library-changing operations.
- Cleanup from `Duplicates` and `Unsupported` now moves files to macOS Trash after confirmation instead of permanently deleting them.
- Added inspector action to move only the currently selected library file to macOS Trash after confirmation.
- GitHub instructions now document preview download, Gatekeeper, large-library refresh, and Trash behavior.

### Fixed

- File traversal now uses `walkFileTree`, so an unreadable file or folder on an external drive does not terminate the scan iterator.
- Scan errors caused by disappeared files, permissions, or I/O are skipped per item while the rest of the archive continues scanning.

## 1.0.0-preview.4 - 2026-06-11

Hotfix preview build for real archive scan reliability.

### Fixed

- A single disappeared or unreadable file no longer stops the whole scan.
- Per-file `NoSuchFileException`, IO, and permission read errors are skipped while the rest of the archive continues scanning.

## 1.0.0-preview.3 - 2026-06-11

Hotfix preview build for large real photo archives.

### Added

- Added the `О программе` section with version, preview status, GitHub, license, Gatekeeper note, and safe testing guidance.

### Changed

- Library refresh no longer calculates SHA-256 hashes for every file during normal browsing.
- Library refresh now shows an `Обновляю...` state and blocks repeated refresh clicks while reading.
- AppleDouble sidecar files such as `._*.jpg` and `._*.mov` are treated as skipped service files, not media.

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
- Safe cleanup from `Duplicates` and `Unsupported` through macOS Trash.
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
