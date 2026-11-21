# SwiftLens Android

An Android 16-targeted SwiftLens project with dedicated Home, Text, Lens, Chat, Subtitle, Photo, Voice, Language, and Settings pages. The floating Translate Lens reads visible accessibility text nodes near the user's drop point. English ↔ Simplified Chinese translation uses ML Kit's on-device translation models. Photo OCR runs on an image explicitly chosen from the photo picker; voice dictation uses Android's on-device recognizer when supported. No ads or billing are included.

## Open and run

1. Open this folder in an Android Studio version that supports Android Gradle Plugin 8.11.2.
2. Let Gradle sync, install Android SDK 36 if prompted, then run the `app` configuration on Android 8.0+; Android 16 is the target for compatibility testing. The project uses AGP 8.11.2, which requires Gradle 8.13 and JDK 17 or newer.
3. To create a debug APK, use **Build → Build APK(s)**. Android Studio writes it to `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK online

The **Build Android APK** GitHub Actions workflow runs on pushes to `main` and can also be started from the repository's **Actions** tab with **Run workflow**. On success, download `SwiftLensAndroid-debug` for the APK and `SwiftLensAndroid-ui-previews` for rendered layout previews. Both artifacts are retained for 14 days. The PNGs are generated from the screen layouts for visual review; they are not screenshots captured from an Android device.

## Translate Lens

1. Launch the app and tap **Set up screen translation**.
2. Enable **Swift Translate Lens** in Android Accessibility settings.
3. Open an app with accessible text and drag the floating **译** lens onto one text element. The lens reads the closest visible text node under the drop point and shows a translation card.
4. Tap the lens to switch English → Simplified Chinese / Simplified Chinese → English, and download or remove the Chinese translation model.

Before each text or Lens translation, choose **Offline** or **Online**. Offline mode uses the on-device English and Chinese ML Kit models. Open **Settings → Offline models** to select either language pack independently, then download or remove the selected packs in the app; downloads require Wi-Fi. Keep both packs installed to translate in either direction offline. The Chinese text-recognition model is bundled with the app. Speech dictation requires Android 12+ and an installed on-device speech language pack. Microphone permission is requested only after the user taps **Speak**.

Online mode lets you select Google Translate, DeepL, or Microsoft Bing Translator. SwiftLens shows a confirmation naming the platform and explaining that the source text may be sent to it. Only **Agree and open** proceeds; translation opens on the provider's website or app. The provider applies its own privacy terms. No text is sent to an online provider when you choose Offline or cancel.

The lens does not read notifications or scan the screen. Chat translation works by placing the lens over individual exposed message text nodes. Subtitle translation works only when the video app exposes subtitle text through Accessibility; this build does not transcribe video audio. Photo OCR processes only an image explicitly chosen by the user. It does not access screenshots or capture the display. The app contains no MediaProjection, screen recording, screen sharing, ads, or billing.

## Device verification checklist

- On an Android 16 device, enable the accessibility service and confirm the lens appears without a screen-capture prompt.
- Drag the lens over accessible English text, then switch direction and repeat over Simplified Chinese text.
- Download both language models, enable airplane mode, choose Offline, and verify both directions still translate.
- Choose Online, select a provider, cancel the confirmation, and verify the provider does not open. Then confirm and verify the selected platform opens with the source text.
- Drag over image-only or canvas-rendered screen text and confirm no accessible text node is found.
- Choose a photo in the Translate tab and confirm recognized text appears before translation.
- On Android 12+ with an offline speech pack, grant microphone permission from **Speak**, dictate a phrase, then translate it.

