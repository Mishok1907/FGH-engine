# FGH Engine

Minimal Android launcher for the FGH game-description format.

- App name: **FGH Engine**
- Minimal light-blue home screen with one **Скачать документацию** button.
- Opens text files through Android intents when the file manager provides a supported MIME type.
- The current prototype renders a small subset of FGH: background, title/text, circles, rectangles and a visual demo cube.
- Full 3D rendering, physics and game events are not implemented yet.

## Build APK

The GitHub Actions workflow at `.github/workflows/build-apk.yml` builds a debug APK on pushes to `main`, pull requests, or manual dispatch. The resulting `FGH-Engine-debug-apk` artifact is available on the workflow run.

## Sample

See `examples/first_game.fgh`.
