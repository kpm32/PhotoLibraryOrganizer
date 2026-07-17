# Photo Library Organizer 1.0.0-preview.5

Preview build for reliable scanning of large, real-world archives.

## What Changed Since the Public 1.0.0-preview.2 Release

- Added the `О программе` section with version, preview status, GitHub, license, Gatekeeper guidance, and safe testing notes.
- Library refresh no longer calculates SHA-256 hashes for every file during normal browsing.
- Library refresh no longer starts automatically on app launch; use the manual refresh action for large libraries.
- Manual library refresh now shows a visible progress indicator.
- AppleDouble sidecar files such as `._*.jpg` and `._*.mov` are ignored as service files.
- Scanning continues when an individual file disappears, cannot be read, or causes a permission/I/O error.
- File traversal now uses `walkFileTree` so a problematic file or folder on an external drive does not interrupt the rest of the scan.
- Duplicate and unsupported cleanup now moves files to macOS Trash after confirmation.
- GitHub README instructions now explain preview download, Gatekeeper launch, large-library refresh, and Trash cleanup behavior.

## Safety Notes

- This is a preview build.
- Test on a copy of important archives first.
- Prefer copy mode until you trust the result on your archive.
- Move mode does not overwrite existing targets and reports per-file failures.
- Duplicate and unsupported cleanup uses macOS Trash, not permanent deletion.

## Build

DMG artifact:

```text
desktopApp/build/compose/binaries/main/dmg/PhotoLibraryOrganizer-1.0.0.dmg
```
