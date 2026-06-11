# Photo Library Organizer

Photo Library Organizer is a local macOS desktop app for cleaning up large photo and video archives.

The app keeps photos as normal files on disk. It scans folders, shows an import plan, then copies or moves media into a clear year/month structure only after confirmation.

## What It Does

- Scans a large source folder recursively.
- Imports photos and videos into `Library/YYYY/YYYY-MM`.
- Supports safe modes: scan only, copy, and move.
- Reads JPEG EXIF capture dates when available.
- Reads HEIC and video dates through macOS metadata when available.
- Falls back to file modified dates when capture dates are missing.
- Recognizes common photo, RAW, camera video, and legacy video formats.
- Detects exact duplicates with SHA-256.
- Moves duplicates into `Duplicates`.
- Moves unsupported files into `Unsupported` during move import.
- Shows unsupported file groups with counts and total size.
- Opens unsupported type folders in Finder.
- Lets you review and safely delete files from `Duplicates` and `Unsupported`.
- Shows a photo grid, grouped library views, file inspector, search, filters, and basic navigation.
- Shows captured date, file date, and the date used for folder placement.
- Shows per-file import failure details when something goes wrong.
- Supports re-running import after interruption by skipping already imported target files.
- Provides a large preview overlay and keyboard navigation.
- Uses macOS QuickLook as a fallback for video thumbnails.

## Library Layout

```text
PhotoLibrary/
  Library/
    2026/
      2026-01/
      2026-02/
    2025/
      2025-11/
  Duplicates/
  Unsupported/
```

## Safety Model

The app is designed around explicit confirmation:

- Scan only never changes files.
- Copy keeps source files in place.
- Move removes source files only after moving them into the target library.
- Existing target files are skipped, not overwritten.
- Unsupported files are moved to `Unsupported`, not deleted.
- Duplicate and unsupported cleanup require confirmation.
- Empty source folder cleanup removes only empty subfolders, not files and not the source root.

## Run From Source

```bash
./gradlew :desktopApp:run
```

## Test

```bash
./gradlew :shared:allTests :desktopApp:compileKotlin
```

## Build DMG

```bash
./gradlew :desktopApp:packageDmg
```

The DMG is written to:

```text
desktopApp/build/compose/binaries/main/dmg/
```

## Release Checklist

Before publishing a DMG, run the release test plan:

```text
docs/RELEASE_TEST_PLAN.md
```

The product finish checklist is:

```text
docs/PROFESSIONAL_FINISH_CHECKLIST.md
```

## Tech Stack

- Kotlin Multiplatform
- Compose Multiplatform Desktop
- Coroutines
- kotlinx-datetime
- Clean Architecture style boundaries

## Project Status

Preview desktop app. The main workflow is usable for local archive cleanup, but public releases should still be treated as preview builds until more real-world archives are tested.

## License

MIT License. See [LICENSE](LICENSE).
