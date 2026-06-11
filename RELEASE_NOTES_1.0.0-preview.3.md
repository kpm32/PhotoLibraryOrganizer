# Photo Library Organizer 1.0.0-preview.3

Hotfix preview build for large real photo archives.

## What Changed Since 1.0.0-preview.2

- Added an `О программе` section with version, preview status, GitHub, license, Gatekeeper note, and safe testing guidance.
- AppleDouble sidecar files such as `._IMG_0001.JPG` and `._video.mov` are no longer treated as media.
- AppleDouble files are grouped as `appledouble` in skipped files.
- Library refresh no longer calculates SHA-256 for every file during normal browsing.
- The refresh button now shows `Обновляю...` and ignores repeated clicks while the library is being read.
- Startup library loading uses the faster refresh path.

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
