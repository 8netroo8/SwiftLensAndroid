package app.swifttranslate

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.HttpsURLConnection

internal data class AlibabaCredentials(val accessKeyId: String, val accessKeySecret: String)

/** Encrypts the user's key material with an AES key held by Android Keystore. */
internal object AlibabaCredentialStore {
    private const val PREFS = "alibaba_translation_credentials"
    private const val KEY_ALIAS = "translate_len_alibaba_credentials_v1"
    private const val PAYLOAD = "encrypted_credentials"
    private const val IV = "credentials_iv"

    fun hasCredentials(context: Context): Boolean = load(context) != null

    fun save(context: Context, accessKeyId: String, accessKeySecret: String) {
        require(accessKeyId.isNotBlank() && accessKeySecret.isNotBlank())
        val plain = JSONObject()
            .put("accessKeyId", accessKeyId.trim())
            .put("accessKeySecret", accessKeySecret)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plain)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(PAYLOAD, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun load(context: Context): AlibabaCredentials? = runCatching {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val iv = Base64.decode(preferences.getString(IV, null), Base64.NO_WRAP)
        val payload = Base64.decode(preferences.getString(PAYLOAD, null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        val data = JSONObject(String(cipher.doFinal(payload), Charsets.UTF_8))
        val id = data.optString("accessKeyId").trim()
        val secret = data.optString("accessKeySecret")
        if (id.isBlank() || secret.isBlank()) null else AlibabaCredentials(id, secret)
    }.getOrNull()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        runCatching {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
        }
    }

    private fun getOrCreateKey(): java.security.Key {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? java.security.Key)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}

/** Sends one user-confirmed translation to Alibaba Cloud Machine Translation. */
internal object AlibabaCloudTranslation {
    private const val HOST = "mt.cn-hangzhou.aliyuncs.com"
    private const val PATH = "/api/translate/web/general"
    private const val CONTENT_TYPE = "application/json;charset=utf-8"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun translate(
        context: Context,
        text: String,
        englishToChinese: Boolean,
        onComplete: (translated: String?, error: String?) -> Unit
    ) {
        val credentials = AlibabaCredentialStore.load(context)
        if (credentials == null) {
            onComplete(null, "Configure Alibaba Cloud credentials in Settings → Online provider first.")
            return
        }
        if (text.isBlank()) {
            onComplete(null, "Enter text to translate.")
            return
        }
        if (text.length > 5000) {
            onComplete(null, "Alibaba Cloud accepts up to 5,000 characters per request.")
            return
        }
        executor.execute {
            val result = runCatching { send(credentials, text, englishToChinese) }
            mainHandler.post {
                result.onSuccess { onComplete(it, null) }
                    .onFailure { onComplete(null, it.message ?: "Alibaba Cloud translation failed.") }
            }
        }
    }

    private fun send(credentials: AlibabaCredentials, text: String, englishToChinese: Boolean): String {
        val requestBody = JSONObject()
            .put("FormatType", "text")
            .put("SourceLanguage", if (englishToChinese) "en" else "zh")
            .put("TargetLanguage", if (englishToChinese) "zh" else "en")
            .put("SourceText", text)
            .put("Scene", "general")
            .toString()
        val bodyBytes = requestBody.toByteArray(Charsets.UTF_8)
        val accept = "application/json"
        val date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }.format(Date())
        val nonce = UUID.randomUUID().toString()
        val contentMd5 = Base64.encodeToString(MessageDigest.getInstance("MD5").digest(bodyBytes), Base64.NO_WRAP)
        val signatureText = "POST\n$accept\n$contentMd5\n$CONTENT_TYPE\n$date\n" +
            "x-acs-signature-method:HMAC-SHA1\n" +
            "x-acs-signature-nonce:$nonce\n" +
            "x-acs-version:2019-01-02\n" + PATH
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(credentials.accessKeySecret.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val signature = Base64.encodeToString(mac.doFinal(signatureText.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

        val connection = (URL("https://$HOST$PATH").openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Accept", accept)
            setRequestProperty("Content-Type", CONTENT_TYPE)
            setRequestProperty("Content-MD5", contentMd5)
            setRequestProperty("Date", date)
            setRequestProperty("Authorization", "acs ${credentials.accessKeyId}:$signature")
            setRequestProperty("x-acs-signature-nonce", nonce)
            setRequestProperty("x-acs-signature-method", "HMAC-SHA1")
            setRequestProperty("x-acs-version", "2019-01-02")
        }
        return try {
            connection.outputStream.use { it.write(bodyBytes) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrNull()
            if (status !in 200..299) {
                val message = response?.optString("Message")?.takeIf { it.isNotBlank() }
                    ?: "HTTP $status. Check Alibaba Cloud credentials, permissions, service activation, and network."
                throw IllegalStateException(message)
            }
            val payload = response?.optJSONObject("TranslateGeneralResponse") ?: response
            val translated = payload?.optJSONObject("Data")?.optString("Translated")
                ?: payload?.optJSONObject("data")?.optString("translated")
                ?: payload?.optString("Translated")
            translated?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(payload?.optString("Message")?.takeIf { it.isNotBlank() } ?: "Alibaba Cloud returned no translation.")
        } finally {
            connection.disconnect()
        }
    }
}

