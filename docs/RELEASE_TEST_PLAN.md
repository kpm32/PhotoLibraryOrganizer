# Release Test Plan

Run this checklist before publishing a DMG.

## Environment

- macOS desktop.
- Fresh test source folder with photos, videos, duplicates, unsupported files, and nested folders.
- Separate destination library folder.
- Enough free disk space for copy mode.

## Smoke Test

1. Launch the app.
2. Confirm the initial window shows all primary controls without resizing.
3. Confirm the app follows macOS dark/light appearance.
4. Select a source folder.
5. Select a destination folder.
6. Run `Обновить библиотеку` once.
7. Restart the app.
8. Confirm the selections and library list load from the saved index without an automatic disk scan.

## Scan

1. Run scan only.
2. Confirm progress updates during scan.
3. Confirm cancel stops scan without changing files.
4. Run scan again and let it finish.
5. Confirm counts for photos, videos, unsupported files, and total size look plausible.
6. Confirm supported HEIC/video files appear as media when possible.
7. Restart the app and confirm the previous library view appears from the local index.
8. Scan a folder that contains an inaccessible or removed file, if the test environment permits it.
9. Confirm the scan completes and records the problem without losing the remaining files.

## Import Copy

1. Select copy mode.
2. Confirm import warning text says source files stay in place.
3. Run import.
4. Confirm files appear under `Library/YYYY/YYYY-MM`.
5. Confirm source files still exist.
6. Run import again.
7. Confirm existing target files are skipped, not overwritten.

## Import Move

1. Use a disposable test source folder.
2. Select move mode.
3. Confirm warning text says source files will move.
4. Run import.
5. Confirm imported media moved into `Library`.
6. Confirm unsupported files moved into `Unsupported`.
7. Confirm source folder cleanup asks for confirmation.
8. Confirm cleanup removes only empty subfolders.

## Duplicates

1. Include two identical media files.
2. Refresh library.
3. Open duplicate section.
4. Move duplicates into `Duplicates`.
5. Confirm originals remain in `Library`.
6. Confirm moving files from `Duplicates` to macOS Trash requires confirmation.

## Unsupported

1. Include unsupported types such as `aae`, `vcf`, `db`, and files without extension.
2. Open the unsupported section.
3. Confirm groups show count and total size.
4. Select a group.
5. Open the group folder in Finder.
6. Confirm moving files from `Unsupported` to macOS Trash requires confirmation.

## Viewing

1. Select several photos and videos.
2. Confirm thumbnails or placeholders render.
3. Confirm inspector shows file name, size, dates, hash, and path.
4. Confirm large preview opens and closes.
5. Confirm left/right keyboard navigation works.
6. Confirm open file and reveal in folder actions work.
7. Confirm moving one selected library file to macOS Trash requires confirmation and refreshes the list.
8. Confirm the selected-file Trash action is not allowed from the import-source preview.

## Error Handling

1. Try scanning a missing folder.
2. Try opening a missing file from inspector if possible.
3. Confirm errors appear in the Errors section.
4. Clear errors and confirm the list resets.

## Packaging

1. Run:

   ```bash
   ./gradlew :shared:allTests :desktopApp:packageDmg
   ```

2. Confirm DMG exists:

   ```text
   desktopApp/build/compose/binaries/main/dmg/PhotoLibraryOrganizer-1.0.6.dmg
   ```

3. Install from DMG on a test machine or clean app install location.
4. Launch installed app.
5. Repeat smoke test.
