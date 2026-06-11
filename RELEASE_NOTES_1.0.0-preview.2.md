# Photo Library Organizer 1.0.0-preview.2

Second preview build for local macOS photo archive cleanup.

## What Changed Since 1.0.0-preview

- Polished the first app window size so the main controls fit better on launch.
- Added a minimum window size to avoid broken layouts when the window is too small.
- Updated the window title to `Photo Library Organizer`.
- Rechecked visible app text: user actions and status messages are Russian.
- Kept English only where it is technical: folder names, paths, date templates, and standard terms such as `SHA-256` and `EXIF`.
- Improved folder action issue titles for skipped files and duplicates.

## Highlights

- Scan large folders recursively without changing source files.
- Organize photos and videos into `Library/YYYY/YYYY-MM`.
- Copy or move files after reviewing the scan plan.
- Detect exact duplicates with SHA-256 and move them into `Duplicates`.
- Move unsupported files into `Unsupported` during move import.
- Review unsupported files by type, size, and open a type folder in Finder.
- Read JPEG EXIF dates, HEIC/video dates via macOS metadata, and fall back to file date.
- Preview images and videos, inspect dates/hash/path, and open files in Finder.
- Use dark/light macOS theme and Russian UI text.

## Safety Notes

- This is a preview build.
- Test on a copy of important archives first.
- Prefer copy mode until you trust the result on your archive.
- Move mode is designed to avoid overwriting existing files and reports per-file failures.

## Build

DMG artifact:

```text
desktopApp/build/compose/binaries/main/dmg/PhotoLibraryOrganizer-1.0.0.dmg
```
