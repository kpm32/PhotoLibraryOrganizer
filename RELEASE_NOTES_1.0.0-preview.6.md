# Photo Library Organizer 1.0.0-preview.6

Preview build for safer real-archive cleanup and faster startup with large libraries.

## What Changed Since 1.0.0-preview.5

- Added a local library index for fast startup after the first manual refresh.
- Startup now loads the saved index instead of scanning the external drive automatically.
- Manual `Обновить библиотеку` rebuilds and saves the index for `Library`, `Duplicates`, and `Unsupported`.
- Added inspector action to move only the currently selected library file to macOS Trash after confirmation.
- The selected-file Trash action is blocked in the import-source preview to avoid deleting source files before reviewing the import plan.
- Clarified the mass unsupported action as `В Корзину все пропущенные`.
- Kept duplicate and unsupported cleanup on macOS Trash rather than permanent deletion.
- Updated README and release test plan for the index and selected-file Trash flow.

## Safety Notes

- This is a preview build.
- Test on a copy of important archives first.
- Prefer copy mode until you trust the result on your archive.
- Move mode does not overwrite existing targets and reports per-file failures.
- Startup data comes from the last saved local index; use manual refresh when files changed outside the app.
- Duplicate, unsupported, and selected-file cleanup use macOS Trash, not permanent deletion.

## Build

DMG artifact:

```text
desktopApp/build/compose/binaries/main/dmg/PhotoLibraryOrganizer-1.0.6.dmg
```
