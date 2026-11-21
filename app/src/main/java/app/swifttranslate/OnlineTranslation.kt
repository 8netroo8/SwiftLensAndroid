package app.swifttranslate

import android.content.Context
import android.content.Intent
import android.net.Uri

enum class OnlineProvider(val label: String) {
    GOOGLE("Google Translate"),
    DEEPL("DeepL"),
    BING("Microsoft Bing Translator")
}

/** Opens a user-selected translation provider only after the UI has shown its disclosure. */
object OnlineTranslation {
    fun open(context: Context, provider: OnlineProvider, englishToChinese: Boolean, text: String) {
        val from = if (englishToChinese) "en" else "zh-CN"
        val to = if (englishToChinese) "zh-CN" else "en"
        val uri = when (provider) {
            OnlineProvider.GOOGLE -> Uri.parse("https://translate.google.com/").buildUpon()
                .appendQueryParameter("sl", from)
                .appendQueryParameter("tl", to)
                .appendQueryParameter("text", text)
                .appendQueryParameter("op", "translate")
                .build()
            OnlineProvider.DEEPL -> {
                val pair = if (englishToChinese) "en/zh" else "zh/en"
                Uri.parse("https://www.deepl.com/translator#$pair/${Uri.encode(text)}")
            }
            OnlineProvider.BING -> Uri.parse("https://www.bing.com/translator/").buildUpon()
                .appendQueryParameter("from", from)
                .appendQueryParameter("to", if (englishToChinese) "zh-Hans" else "en")
                .appendQueryParameter("text", text)
                .build()
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

