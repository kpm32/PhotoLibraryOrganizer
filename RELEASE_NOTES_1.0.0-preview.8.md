# Photo Library Organizer 1.0.0-preview.8

Hotfix preview build for selected-file Trash hangs on macOS.

## What Changed Since 1.0.0-preview.7

- Fixed a hang where Java `Desktop.moveToTrash` could block before the Finder fallback ran.
- On macOS, Trash actions now first move files directly into the correct `.Trash` or external-volume `.Trashes/<uid>` folder.
- Finder Trash remains as a fallback.
- Java Desktop Trash is now used only on non-macOS systems.

## Safety Notes

- This is a preview build.
- Test on a copy of important archives first.
- Trash actions move files to macOS Trash folders, not permanent deletion.
- If the currently installed app is stuck on `Перемещаю выбранный файл в Корзину...`, force quit it and install this build.

## Build

DMG artifact:

```text
desktopApp/build/compose/binaries/main/dmg/PhotoLibraryOrganizer-1.0.8.dmg
```
