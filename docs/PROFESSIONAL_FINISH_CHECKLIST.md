# Professional Finish Checklist

This checklist defines what "professionally finished" means for Photo Library Organizer.

## Product Scope

- The app has one clear primary workflow: scan, review, import, inspect, clean up.
- The physical archive layout stays understandable without the app.
- Any future photo/video split keeps the archive readable in Finder, for example `Library/Photos/YYYY/YYYY-MM` and `Library/Videos/YYYY/YYYY-MM`.
- All destructive actions require explicit confirmation.
- Move mode is treated as advanced and must remain conservative.
- Preview releases clearly warn users to test on copies first.

## Reliability

- Large folder scans show progress and can be canceled.
- Import progress is visible and can be canceled.
- Copy and move operations verify target files after the operation.
- Existing target files are skipped, never overwritten.
- Per-file import failures are collected and shown in the UI.
- Re-running import after interruption skips already imported target files.
- Empty folder cleanup removes only empty source subfolders, never source files or the source root.

## UX Polish

- The first window size must show the main controls without forcing resize.
- Sidebar labels must not wrap awkwardly.
- All visible app text must be Russian unless it is a technical folder/file name.
- Dark and light macOS themes must both be readable.
- Long paths and file names must truncate cleanly.
- Buttons that perform file operations must say exactly what they do.
- Unsupported, duplicate, and error sections must explain the current state.

## Media Support

- JPEG EXIF dates are preferred when available.
- HEIC and video metadata dates are attempted on macOS.
- File modified date is the fallback.
- Common photo, RAW, camera video, and legacy video formats are recognized.
- Photo and video routing can be separated in the archive layout without breaking existing `Library/YYYY/YYYY-MM` libraries.
- Video files are opened through macOS/default applications unless a dedicated player is intentionally added later.
- Non-media files such as contacts, sidecars, databases, and thumbnails remain unsupported.

## Release Quality

- README matches the current product behavior.
- Changelog has a dated release section.
- Release notes explain preview status and safety guidance.
- DMG builds successfully on macOS.
- Git tag is pushed for each release.
- GitHub Release contains release notes and the DMG artifact.
- The app icon is present in the bundle.

## Distribution

- Preview release can be distributed via GitHub Releases.
- Stable public release should use signed and notarized macOS builds.
- Mac App Store distribution is a later track and requires Apple Developer Program work.

## Manual Acceptance

Before a public release, run the manual test plan in `docs/RELEASE_TEST_PLAN.md`.
