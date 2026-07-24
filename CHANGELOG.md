# Changelog

## Unreleased

### Changed

- Moved selected-file Trash validation, timeout, and repository result mapping into a dedicated domain use case with tests.
- Duplicate quarantine operations now preserve coroutine cancellation instead of mapping it to a filesystem error.
- Removed the generated desktop placeholder test.
- File actions now guard against repeated clicks while duplicate, unsupported, or selected-file Trash operations are already running.
- Selected-file Trash now has a UI timeout instead of leaving the inspector in an endless moving state.
- Library refresh now shows the current section and file counters, and can be canceled from the inspector.
- macOS Trash now uses only the Finder operation with a short timeout, avoiding slow direct `.Trashes` moves on external drives.
- macOS Trash now finishes as soon as the source file disappears, even if the AppleScript process is still waiting.
- After a successful selected-file Trash move, the UI removes the file immediately and ignores refresh failures that happen after the file is already in Trash.
- Replaced the app icon with a more polished macOS-style photo archive icon and rebuilt the `.icns` asset.
- Library index saves now use unique temporary files, avoiding `library-index.tsv.tmp` collisions during concurrent refreshes.
- Selected-file Trash no longer waits for a full library index refresh before showing success and re-enabling the inspector action.
- Added KDoc to key domain contracts, use cases, JVM filesystem adapters, and large UI entry points; removed obsolete Trash mover implementations.
- Introduced a platform-neutral Compose app state holder and moved mutable screen state out of the root `App.kt` composition.
- Moved library index refresh, navigation-file selection, file action helpers, and user-message mapping out of `App.kt` into focused files.
- Added Russian/English UI localization: Russian is used for Russian systems, while all other system languages fall back to English.
- Added Compose Multiplatform string resources for stable labels and a platform locale helper for dynamic status/error text.
- Removed UI labels from domain enums and removed unused Android lifecycle dependencies.
- Moved library index application, folder-change reset, and selected-file removal transitions into the platform-neutral app state holder with tests.
- Added a `PhotoLibraryUseCases` bundle and extracted startup/preview side effects from the root app composable.
- Extracted simple OS file/folder open actions into `PhotoLibraryFileActions` and covered failure mapping with tests.
- Replaced domain import availability messages with typed reasons, keeping domain code language-neutral while presentation localizes the text.

## 1.0.0-preview.8 - 2026-07-17

Hotfix preview build for selected-file Trash hangs on macOS.

### Fixed

- macOS Trash actions now avoid Java `Desktop.moveToTrash` on macOS and first move files directly into `.Trash` or external-volume `.Trashes/<uid>`.
- Finder Trash remains as a fallback, while Java Desktop Trash is only used on non-macOS systems.

## 1.0.0-preview.7 - 2026-07-17

Hotfix preview build for macOS Trash reliability.

### Fixed

- Selected-file, duplicate, and unsupported cleanup now fall back to macOS Finder Trash when Java `Desktop.moveToTrash` is unavailable or returns failure.

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
