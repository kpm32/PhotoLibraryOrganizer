# Photo Library Organizer 1.0.0-preview.7

Hotfix preview build for macOS Trash reliability.

## What Changed Since 1.0.0-preview.6

- Fixed selected-file Trash action when Java `Desktop.moveToTrash` returns failure.
- Duplicate and unsupported cleanup now use the same fallback path.
- The app first tries the JVM Trash API, then falls back to macOS Finder Trash through `osascript`.

## Safety Notes

- This is a preview build.
- Test on a copy of important archives first.
- Prefer copy mode until you trust the result on your archive.
- Trash actions move files to macOS Trash, not permanent deletion.

## Build

DMG artifact:

```text
desktopApp/build/compose/binaries/main/dmg/PhotoLibraryOrganizer-1.0.7.dmg
```
