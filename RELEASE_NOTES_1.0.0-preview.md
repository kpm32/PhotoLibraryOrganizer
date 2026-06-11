# Photo Library Organizer 1.0.0-preview

Early preview build for local macOS photo archive cleanup.

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
