package app.swifttranslate

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** A draggable accessibility lens. It reads only text nodes near the drop point. */
class TranslateLensService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var targetEnglish = true // English -> Simplified Chinese; false is the reverse.
    private var busy = false

    private val fromLanguage: String get() = if (targetEnglish) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
    private val toLanguage: String get() = if (targetEnglish) TranslateLanguage.CHINESE else TranslateLanguage.ENGLISH
    // ML Kit bundles its English model. Only Chinese needs an explicit remote model download.
    private val chineseModel: TranslateRemoteModel get() = TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        targetEnglish = getSharedPreferences("lens", MODE_PRIVATE).getBoolean("english_to_chinese", true)
        showBubble()
        refreshModelState()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showBubble() {
        if (bubble != null) return
        val view = TextView(this).apply {
            text = "译"
            textSize = 18f
            setTextColor(Color.rgb(24, 34, 31))
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(185, 242, 124), dp(30).toFloat())
            elevation = dp(8).toFloat()
            contentDescription = "Translate Lens. Drag over text to translate. Tap to open language and offline model settings."
        }
        bubbleParams = WindowManager.LayoutParams(dp(58), dp(58), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = dp(20); y = dp(220) }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var dragged = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; startX = bubbleParams.x; startY = bubbleParams.y; dragged = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                    if (abs(dx) + abs(dy) > dp(8)) dragged = true
                    val (screenW, screenH) = screenSize()
                    bubbleParams.x = (startX + dx).coerceIn(0, max(0, screenW - bubbleParams.width))
                    bubbleParams.y = (startY + dy).coerceIn(0, max(0, screenH - bubbleParams.height))
                    windowManager.updateViewLayout(view, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragged) {
                        val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                        val (screenW, screenH) = screenSize()
                        bubbleParams.x = (startX + dx).coerceIn(0, max(0, screenW - bubbleParams.width))
                        bubbleParams.y = (startY + dy).coerceIn(0, max(0, screenH - bubbleParams.height))
                        windowManager.updateViewLayout(view, bubbleParams)
                        removePanel()
                        val x = bubbleParams.x + bubbleParams.width / 2
                        val y = bubbleParams.y + bubbleParams.height / 2
                        translateTextAt(x, y)
                    } else showLensMenu()
                    true
                }
                else -> true
            }
        }
        bubble = view
        windowManager.addView(view, bubbleParams)
    }

    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        @Suppress("DEPRECATION")
        return resources.displayMetrics.widthPixels to resources.displayMetrics.heightPixels
    }

    private fun showLensMenu() {
        removePanel()
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(Color.WHITE, dp(18).toFloat())
            elevation = dp(12).toFloat()
        }
        body.addView(label("Translate Lens", 18, Color.rgb(24, 34, 31), true))
        body.addView(label("Drag the bubble onto a text node to translate it.", 13, Color.DKGRAY).apply { setPadding(0, dp(5), 0, dp(10)) })
        val direction = label("", 15, Color.rgb(24, 34, 31), true).apply {
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener { targetEnglish = !targetEnglish; getSharedPreferences("lens", MODE_PRIVATE).edit().putBoolean("english_to_chinese", targetEnglish).apply(); showLensMenu() }
        }
        body.addView(direction)
        val modelStatus = label("Checking offline models…", 12, Color.DKGRAY)
        body.addView(modelStatus)
        val action = label("Download Chinese model for offline use", 14, Color.rgb(30, 90, 45), true).apply {
            setPadding(0, dp(12), 0, dp(8))
            setOnClickListener { downloadOfflineModels(modelStatus) }
        }
        body.addView(action)
        body.addView(label("Remove downloaded Chinese model", 13, Color.DKGRAY).apply {
            setPadding(0, dp(8), 0, dp(4))
            setOnClickListener {
                RemoteModelManager.getInstance().deleteDownloadedModel(chineseModel)
                    .addOnSuccessListener { modelStatus.post { modelStatus.text = "Chinese model removed. Download it to use either direction." } }
                    .addOnFailureListener { error -> modelStatus.post { modelStatus.text = "Could not remove model: ${error.localizedMessage ?: "try again"}" } }
            }
        })
        body.addView(label("Tap this panel to close", 11, Color.GRAY).apply { setPadding(0, dp(8), 0, 0); setOnClickListener { removePanel() } })
        panel = body
        val (screenW, screenH) = screenSize()
        val x = if (bubbleParams.x < screenW / 2) bubbleParams.x else screenW - bubbleParams.x - dp(300)
        val y = (bubbleParams.y + bubbleParams.height + dp(8)).coerceIn(0, screenH - dp(260))
        val params = WindowManager.LayoutParams(dp(280), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; this.x = x.coerceAtLeast(0); this.y = y }
        windowManager.addView(body, params)
        direction.text = directionText()
        refreshModelState(modelStatus)
    }

    private fun directionText() = if (targetEnglish) "English  →  简体中文    ⇄" else "简体中文  →  English    ⇄"

    private fun label(textValue: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = textValue; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun translateTextAt(x: Int, y: Int) {
        if (busy) return
        val sourceLanguage = fromLanguage
        val targetLanguage = toLanguage
        val sourceIsEnglish = targetEnglish
        val nodeText = findTextNodeAt(x, y)
        if (nodeText.isNullOrBlank()) {
            showResult("No readable text at the lens position", "Move the lens directly over a text element and try again.", sourceIsEnglish)
            return
        }
        busy = true
        bubble?.post { bubble?.text = "…" }
        val continueTranslation = {
                val options = TranslatorOptions.Builder().setSourceLanguage(sourceLanguage).setTargetLanguage(targetLanguage).build()
                val translator = Translation.getClient(options)
                translator.translate(nodeText)
                    .addOnSuccessListener { result -> busy = false; translator.close(); showResult(nodeText, result, sourceIsEnglish) }
                    .addOnFailureListener { busy = false; translator.close(); showResult(nodeText, "Translation failed. Confirm the offline model is installed.", sourceIsEnglish) }
        }
        RemoteModelManager.getInstance().isModelDownloaded(chineseModel)
            .addOnSuccessListener { downloaded ->
                if (!downloaded) {
                    busy = false
                    bubble?.post { bubble?.text = "译" }
                    showResult(nodeText, "Chinese offline model not installed. Tap the lens and download it while online.", sourceIsEnglish)
                    return@addOnSuccessListener
                }
                continueTranslation()
            }
            .addOnFailureListener { busy = false; bubble?.post { bubble?.text = "译" }; showResult(nodeText, "Could not check offline model status.", sourceIsEnglish) }
    }

    private data class TextCandidate(val text: String, val bounds: Rect, val area: Long)

    private fun findTextNodeAt(x: Int, y: Int): String? {
        val roots = mutableListOf<AccessibilityNodeInfo>()
        val active = rootInActiveWindow
        if (active != null) {
            if (active.packageName?.toString() != packageName) roots.add(active) else active.recycle()
        }
        for (window in windows) {
            if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
            val root = window.root ?: continue
            if (root.packageName?.toString() == packageName) { root.recycle(); continue }
            if (roots.none { it.windowId == root.windowId }) roots.add(root) else root.recycle()
        }
        val candidates = mutableListOf<TextCandidate>()
        roots.forEach { root -> collectTextNodes(root, candidates, 0, intArrayOf(0)) }
        roots.forEach { runCatching { it.recycle() } }
        if (candidates.isEmpty()) return null
        val nearest = candidates.minWithOrNull(compareBy<TextCandidate> { distanceToRect(x, y, it.bounds) }.thenBy { it.area }) ?: return null
        val maxDistance = dp(64)
        if (distanceToRect(x, y, nearest.bounds) > maxDistance) return null
        return nearest.text
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo, found: MutableList<TextCandidate>, depth: Int, count: IntArray) {
        if (depth > 35 || count[0]++ > 2500 || !node.isVisibleToUser) return
        val text = node.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
            ?: node.contentDescription?.toString()?.trim().takeUnless { it.isNullOrBlank() }
        if (text != null) {
            val bounds = Rect(); node.getBoundsInScreen(bounds)
            if (!bounds.isEmpty) found.add(TextCandidate(text, bounds, bounds.width().toLong() * bounds.height()))
        }
        for (index in 0 until node.childCount) {
            val child = runCatching { node.getChild(index) }.getOrNull() ?: continue
            try { collectTextNodes(child, found, depth + 1, count) } finally { child.recycle() }
        }
    }

    private fun distanceToRect(x: Int, y: Int, rect: Rect): Int {
        if (rect.contains(x, y)) return 0
        val dx = max(max(rect.left - x, 0), x - rect.right)
        val dy = max(max(rect.top - y, 0), y - rect.bottom)
        return sqrt((dx * dx + dy * dy).toDouble()).toInt()
    }

    private fun downloadOfflineModels(status: TextView) {
        val manager = RemoteModelManager.getInstance()
        val conditions = DownloadConditions.Builder().requireWifi().build()
        status.text = "English model is built in. Downloading the Chinese model… connect to Wi-Fi."
        manager.download(chineseModel, conditions)
            .addOnSuccessListener { status.post { status.text = "Offline translation is ready in both directions." } }
            .addOnFailureListener { error -> status.post { status.text = "Model download failed: ${error.localizedMessage ?: "check Wi-Fi and retry"}" } }
    }

    private fun refreshModelState(target: TextView? = null) {
        val manager = RemoteModelManager.getInstance()
        manager.isModelDownloaded(chineseModel).addOnSuccessListener { zh ->
            val state = "English model: built in · Chinese model: ${if (zh) "ready" else "not downloaded"}"
            target?.post { target.text = state }
        }
    }

    private fun showResult(original: String, translated: String, englishToChinese: Boolean) {
        bubble?.post { bubble?.text = "译" }
        removePanel()
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)); background = rounded(Color.WHITE, dp(16).toFloat()); elevation = dp(12).toFloat()
        }
        card.addView(label(if (englishToChinese) "ENGLISH → 简体中文" else "简体中文 → ENGLISH", 11, Color.GRAY, true))
        card.addView(TextView(this).apply { text = translated; textSize = 17f; setTextColor(Color.rgb(24, 34, 31)); maxLines = 8; setPadding(0, dp(6), 0, dp(5)) })
        card.addView(TextView(this).apply { text = original; textSize = 12f; setTextColor(Color.DKGRAY); maxLines = 5 })
        card.addView(TextView(this).apply { text = "Tap to dismiss"; textSize = 10f; setTextColor(Color.GRAY); setPadding(0, dp(8), 0, 0) })
        card.setOnClickListener { removePanel() }
        panel = card
        val (screenW, screenH) = screenSize()
        val cardWidth = dp(290)
        val x = if (bubbleParams.x + bubbleParams.width + cardWidth < screenW) bubbleParams.x + bubbleParams.width + dp(8) else bubbleParams.x - cardWidth - dp(8)
        val y = (bubbleParams.y + bubbleParams.height / 2 - dp(80)).coerceIn(0, screenH - dp(180))
        val params = WindowManager.LayoutParams(cardWidth, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; this.x = x.coerceAtLeast(0); this.y = y }
        try { windowManager.addView(card, params) } catch (_: Exception) { Toast.makeText(this, translated, Toast.LENGTH_LONG).show() }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }
    private fun removePanel() { panel?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }; panel = null }
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { removePanel(); bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }; super.onDestroy() }
}
