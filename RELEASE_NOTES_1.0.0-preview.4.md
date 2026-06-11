# Photo Library Organizer 1.0.0-preview.4

Hotfix preview build for real archive scan reliability.

## What Changed Since 1.0.0-preview.3

- A single disappeared or unreadable file no longer stops the whole scan.
- `NoSuchFileException` and per-file permission/read errors are skipped while the rest of the folder keeps scanning.
- Added a regression test for a file that becomes unreadable during scanning.

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
