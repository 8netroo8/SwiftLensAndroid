# SwiftLens Android

An Android 16-targeted Android Studio project for SwiftLens. It uses a draggable accessibility overlay and reads only visible text nodes at the lens drop position. It does not use MediaProjection, screen recording, screen sharing, screenshot APIs, or OCR. It supports English ↔ Simplified Chinese translation with ML Kit's on-device translation model. The app also has Compose screens for manual translation, chat mode, and settings.

## Open and run

1. Open this folder in an Android Studio version that supports Android Gradle Plugin 8.11.2.
2. Let Gradle sync, install Android SDK 36 if prompted, then run the `app` configuration on Android 8.0+; Android 16 is the target for compatibility testing. The project uses AGP 8.11.2, which requires Gradle 8.13 and JDK 17 or newer.
3. To create a debug APK, use **Build → Build APK(s)**. Android Studio writes it to `app/build/outputs/apk/debug/app-debug.apk`.

## Translate Lens flow

1. Launch the app and tap **Set up screen translation**.
2. Enable **Swift Translate Lens** in Android Accessibility settings.
3. Open an app with accessible text and drag the floating **译** lens onto one text element. The lens reads the closest visible text node under the drop position and shows a translation card.
4. Tap the lens to switch English → Simplified Chinese / Simplified Chinese → English, and download or remove the Chinese model. The English model is built in; the Chinese model is needed for both directions.

The model download is Wi-Fi-only. After the Chinese model is installed, both translation directions work offline. Text in canvas-rendered games, images, videos, secure screens, and apps that hide accessibility nodes cannot be read by this approach. The result appears in a floating card next to the lens; the original text is not repainted. Chat mode, live subtitles, and camera OCR are not connected.

## Device verification checklist

- On an Android 16 device, enable the accessibility service and confirm the lens bubble appears without a screen-capture prompt.
- Drag the lens over selectable English text, then switch direction and repeat over Simplified Chinese text.
- Download the Chinese model on Wi-Fi, enable airplane mode, and verify both directions still translate.
- Drag over an image-only or canvas-rendered text area and confirm it reports no accessible text node.
