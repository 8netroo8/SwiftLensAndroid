# Translate Len Android

An Android 16-targeted Translate Len project with dedicated Home, Text, Lens, Chat, Subtitle, Photo, Voice, Language, and Settings pages. The floating lens reads visible accessibility text nodes near the user's drop point. English ↔ Simplified Chinese translation uses ML Kit's on-device translation models. Photo OCR runs on an image explicitly chosen from the photo picker; voice dictation produces editable text and an in-app offline translation. No ads or billing are included.

## Open and run

1. Open this folder in an Android Studio version that supports Android Gradle Plugin 8.11.2.
2. Let Gradle sync, install Android SDK 36 if prompted, then run the `app` configuration on Android 8.0+; Android 16 is the target for compatibility testing. The project uses AGP 8.11.2, which requires Gradle 8.13 and JDK 17 or newer.
3. To create a debug APK, use **Build → Build APK(s)**. Android Studio writes it to `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK online

The **Build Android APK** GitHub Actions workflow runs on pushes to `main` and can also be started from the repository's **Actions** tab with **Run workflow**. On success, download `SwiftLensAndroid-debug` for the APK and `SwiftLensAndroid-ui-previews` for rendered layout previews. Both artifacts are retained for 14 days. The PNGs are generated from the screen layouts for visual review; they are not screenshots captured from an Android device.

## Translate Lens

1. Launch the app and tap **Set up Translate Lens**.
2. Enable **Translate Len Lens** in Android Accessibility settings.
3. Open an app with accessible text and drag the floating lens onto one text element. The lens reads the closest visible text node under the drop point and shows a translation card.
4. While idle, the lens rests as a narrow translucent white vertical tab parallel to the phone edge. Tap or drag to expand it. The bubble has no settings; configure languages and models only inside the app.

Before each text translation, choose **Offline** or **Online**. Offline mode uses the on-device English and Chinese ML Kit models. Open **Settings → Offline models** to select either language pack independently, then download or remove the selected packs in the app; downloads require Wi-Fi. Keep both packs installed to translate in either direction offline. Voice translation shows editable speech-recognition text and translates on device. The Chinese text-recognition model is bundled with the app. Microphone permission is requested only after the user taps **Speak**.

Online mode is a placeholder until the user selects an API platform in Codex. It never launches a website and does not send text. After platform selection, the provider will be integrated for in-app translation with explicit consent.

The lens does not read notifications or scan the screen. Chat translation works by placing the lens over individual exposed message text nodes. Subtitle translation works only when the video app exposes subtitle text through Accessibility; this build does not transcribe video audio. Photo OCR processes only an image explicitly chosen by the user. It does not access screenshots or capture the display. The app contains no MediaProjection, screen recording, screen sharing, ads, or billing.

## Device verification checklist

- On an Android 16 device, enable the accessibility service and confirm the lens appears without a screen-capture prompt.
- Drag the lens over accessible English text, then switch direction and repeat over Simplified Chinese text.
- Download both language models, enable airplane mode, choose Offline, and verify both directions still translate.
- Choose Online before an API provider is selected and verify no website opens and no text is sent.
- Drag over image-only or canvas-rendered screen text and confirm no accessible text node is found.
- Choose a photo in the Translate tab and confirm recognized text appears before translation.
- On Android 12+ with an on-device speech service, grant microphone permission from **Speak**, edit the recognized transcript if needed, then translate it. On devices without on-device speech recognition, the device's configured speech service may use a network.
