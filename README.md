# SwiftLens Android

An Android 16-targeted SwiftLens project. The floating Translate Lens reads visible accessibility text nodes near the user's drop point. English ↔ Simplified Chinese translation uses ML Kit's on-device translation models. The Translate tab also supports OCR on an image explicitly chosen from the photo picker and speech dictation with Android's on-device recognizer when the device supports it. No ads or billing are included.

## Open and run

1. Open this folder in an Android Studio version that supports Android Gradle Plugin 8.11.2.
2. Let Gradle sync, install Android SDK 36 if prompted, then run the `app` configuration on Android 8.0+; Android 16 is the target for compatibility testing. The project uses AGP 8.11.2, which requires Gradle 8.13 and JDK 17 or newer.
3. To create a debug APK, use **Build → Build APK(s)**. Android Studio writes it to `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK online

The **Build Android APK** GitHub Actions workflow runs on pushes to `main` and can also be started from the repository's **Actions** tab with **Run workflow**. When it succeeds, open that run and download the `SwiftLensAndroid-debug` artifact. The artifact contains a debug APK and is retained for 14 days.

## Translate Lens

1. Launch the app and tap **Set up screen translation**.
2. Enable **Swift Translate Lens** in Android Accessibility settings.
3. Open an app with accessible text and drag the floating **译** lens onto one text element. The lens reads the closest visible text node under the drop point and shows a translation card.
4. Tap the lens to switch English → Simplified Chinese / Simplified Chinese → English, and download or remove the Chinese translation model.

The translation model download is Wi-Fi-only. After the Chinese model is installed, both directions work offline. The Chinese text-recognition model is bundled with the app. Speech dictation requires Android 12+ and an installed on-device speech language pack. Microphone permission is requested only after the user taps **Speak**.

The lens does not read notifications or scan the screen. Chat translation works by placing the lens over individual exposed message text nodes. Subtitle translation works only when the video app exposes subtitle text through Accessibility; this build does not transcribe video audio. Photo OCR processes only an image explicitly chosen by the user. It does not access screenshots or capture the display. The app contains no MediaProjection, screen recording, screen sharing, ads, or billing.

## Device verification checklist

- On an Android 16 device, enable the accessibility service and confirm the lens appears without a screen-capture prompt.
- Drag the lens over accessible English text, then switch direction and repeat over Simplified Chinese text.
- Download the Chinese model, enable airplane mode, and verify both directions still translate.
- Drag over image-only or canvas-rendered screen text and confirm no accessible text node is found.
- Choose a photo in the Translate tab and confirm recognized text appears before translation.
- On Android 12+ with an offline speech pack, grant microphone permission from **Speak**, dictate a phrase, then translate it.

