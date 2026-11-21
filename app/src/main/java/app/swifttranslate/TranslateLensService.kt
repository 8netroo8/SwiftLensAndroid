package app.swifttranslate

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
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
    private var compact = false
    private var compactOnLeft = true
    private val mainHandler = Handler(Looper.getMainLooper())
    private val compactRunnable = Runnable { compactBubble() }

    private val fromLanguage: String get() = if (targetEnglish) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
    private val toLanguage: String get() = if (targetEnglish) TranslateLanguage.CHINESE else TranslateLanguage.ENGLISH
    private val englishModel: TranslateRemoteModel get() = TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build()
    private val chineseModel: TranslateRemoteModel get() = TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        targetEnglish = getSharedPreferences("lens", MODE_PRIVATE).getBoolean("english_to_chinese", true)
        showBubble()
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
            contentDescription = "Translate Len. Drag this lens over text to translate. Configure languages and translation in the app."
        }
        bubbleParams = WindowManager.LayoutParams(dp(58), dp(58), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = dp(20); y = dp(220) }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var dragged = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    expandBubble()
                    startX = bubbleParams.x; startY = bubbleParams.y; dragged = false
                    true
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
                    } else scheduleCompact(3500)
                    true
                }
                MotionEvent.ACTION_CANCEL -> { scheduleCompact(1200); true }
                else -> true
            }
        }
        bubble = view
        windowManager.addView(view, bubbleParams)
        scheduleCompact(1400)
    }

    private fun scheduleCompact(delayMs: Long = 2400) {
        mainHandler.removeCallbacks(compactRunnable)
        mainHandler.postDelayed(compactRunnable, delayMs)
    }

    private fun expandBubble() {
        mainHandler.removeCallbacks(compactRunnable)
        val view = bubble ?: return
        if (!compact) return
        val (_, screenH) = screenSize()
        val (screenW, _) = screenSize()
        val newWidth = dp(58)
        val newHeight = dp(58)
        val oldCenterY = bubbleParams.y + bubbleParams.height / 2
        compact = false
        bubbleParams.width = newWidth
        bubbleParams.height = newHeight
        bubbleParams.x = if (compactOnLeft) 0 else max(0, screenW - newWidth)
        bubbleParams.y = (oldCenterY - newHeight / 2).coerceIn(0, max(0, screenH - newHeight))
        view.text = "译"
        view.textSize = 18f
        view.alpha = 1f
        view.background = rounded(Color.rgb(185, 242, 124), dp(30).toFloat())
        view.elevation = dp(8).toFloat()
        view.contentDescription = "Translate Len. Drag this lens over text to translate. Configure languages and translation in the app."
        runCatching { windowManager.updateViewLayout(view, bubbleParams) }
    }

    private fun compactBubble() {
        val view = bubble ?: return
        if (compact) return
        val (screenW, screenH) = screenSize()
        compactOnLeft = bubbleParams.x + bubbleParams.width / 2 < screenW / 2
        // Dock as a slim vertical tab, parallel to the phone's left/right frame.
        val newWidth = dp(22)
        val newHeight = dp(74)
        val oldCenterY = bubbleParams.y + bubbleParams.height / 2
        compact = true
        bubbleParams.width = newWidth
        bubbleParams.height = newHeight
        bubbleParams.x = if (compactOnLeft) 0 else max(0, screenW - newWidth)
        bubbleParams.y = (oldCenterY - newHeight / 2).coerceIn(0, max(0, screenH - newHeight))
        view.text = ""
        view.background = rounded(Color.WHITE, dp(18).toFloat())
        view.alpha = 0.58f
        view.elevation = dp(3).toFloat()
        view.contentDescription = "Translate Len minimized as a translucent white tab at the phone edge. Drag onto text to translate."
        runCatching { windowManager.updateViewLayout(view, bubbleParams) }
    }

    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        @Suppress("DEPRECATION")
        return resources.displayMetrics.widthPixels to resources.displayMetrics.heightPixels
    }

    private fun label(textValue: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = textValue; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun translateTextAt(x: Int, y: Int) {
        if (busy) return
        targetEnglish = getSharedPreferences("lens", MODE_PRIVATE).getBoolean("english_to_chinese", true)
        val sourceIsEnglish = targetEnglish
        val nodeText = findTextNodeAt(x, y)
        if (nodeText.isNullOrBlank()) {
            showResult("No readable text at the lens position", "Move the lens directly over a text element and try again.", sourceIsEnglish)
            return
        }
        showTranslationChoice(nodeText, sourceIsEnglish)
    }

    private fun showTranslationChoice(original: String, sourceIsEnglish: Boolean) {
        removePanel()
        val body = panelContainer()
        body.addView(label("Choose translation method", 18, Color.rgb(24, 34, 31), true))
        body.addView(label("Choose offline on-device translation or send the selected text to Alibaba Cloud after confirmation.", 13, Color.DKGRAY).apply { setPadding(0, dp(5), 0, dp(8)) })
        val methods = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val offline = RadioButton(this).apply { text = "Offline · ML Kit (on device)"; id = 1; isChecked = true }
        val online = RadioButton(this).apply { text = "Online · Alibaba Cloud"; id = 2 }
        methods.addView(offline); methods.addView(online); body.addView(methods)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END; setPadding(0, dp(8), 0, 0) }
        actions.addView(actionLabel("Cancel", false) { removePanel(); scheduleCompact() })
        actions.addView(actionLabel("Continue", true) {
            if (methods.checkedRadioButtonId == online.id) showAlibabaConfirmation(original, sourceIsEnglish)
            else { removePanel(); translateOffline(original, sourceIsEnglish) }
        })
        body.addView(actions)
        showOverlayPanel(body, 320, 410)
    }

    private fun showAlibabaConfirmation(original: String, sourceIsEnglish: Boolean) {
        removePanel()
        val body = panelContainer()
        body.addView(label("Send text to Alibaba Cloud?", 18, Color.rgb(24, 34, 31), true))
        body.addView(label("This text will be sent to Alibaba Cloud Machine Translation for processing. API usage may incur charges. Nothing is sent unless you confirm here.", 14, Color.DKGRAY).apply { setPadding(0, dp(8), 0, dp(8)) })
        body.addView(label("Text: ${original.take(130)}${if (original.length > 130) "…" else ""}", 12, Color.GRAY).apply { setPadding(0, dp(4), 0, dp(8)) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END; setPadding(0, dp(8), 0, 0) }
        actions.addView(actionLabel("Cancel", false) { removePanel(); scheduleCompact() })
        actions.addView(actionLabel("Agree & translate", true) {
            removePanel()
            if (!AlibabaCredentialStore.hasCredentials(this)) {
                showResult(original, "Add Alibaba Cloud credentials in Translate Len → Settings → Online provider, then retry. No text was sent.", sourceIsEnglish)
            } else {
                busy = true
                bubble?.post { bubble?.text = "…" }
                AlibabaCloudTranslation.translate(this, original, sourceIsEnglish) { translated, error ->
                    busy = false
                    bubble?.post { bubble?.text = "译" }
                    showResult(original, translated ?: (error ?: "Alibaba Cloud translation failed."), sourceIsEnglish)
                }
            }
        })
        body.addView(actions)
        showOverlayPanel(body, 320, 370)
    }

    private fun panelContainer() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(14))
        background = rounded(Color.WHITE, dp(18).toFloat())
        elevation = dp(12).toFloat()
    }

    private fun actionLabel(value: String, primary: Boolean, action: () -> Unit) = TextView(this).apply {
        text = value; textSize = 14f; gravity = Gravity.CENTER; isClickable = true
        setTextColor(if (primary) Color.WHITE else Color.rgb(35, 87, 164))
        setPadding(dp(13), dp(10), dp(13), dp(10))
        if (primary) background = rounded(Color.rgb(40, 120, 240), dp(10).toFloat())
        setOnClickListener { action() }
    }

    private fun showOverlayPanel(body: LinearLayout, widthDp: Int, maxHeightDp: Int) {
        panel = body
        val (screenW, screenH) = screenSize()
        val x = if (bubbleParams.x < screenW / 2) bubbleParams.x else screenW - bubbleParams.x - dp(widthDp)
        val y = (bubbleParams.y + bubbleParams.height + dp(8)).coerceIn(0, max(0, screenH - dp(maxHeightDp)))
        val params = WindowManager.LayoutParams(dp(widthDp), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; this.x = x.coerceAtLeast(0); this.y = y }
        windowManager.addView(body, params)
    }

    private fun translateOffline(original: String, sourceIsEnglish: Boolean) {
        if (busy) return
        val sourceLanguage = if (sourceIsEnglish) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
        val targetLanguage = if (sourceIsEnglish) TranslateLanguage.CHINESE else TranslateLanguage.ENGLISH
        busy = true
        bubble?.post { bubble?.text = "…" }
        val continueTranslation = {
                val options = TranslatorOptions.Builder().setSourceLanguage(sourceLanguage).setTargetLanguage(targetLanguage).build()
                val translator = Translation.getClient(options)
                translator.translate(original)
                    .addOnSuccessListener { result -> busy = false; translator.close(); showResult(original, result, sourceIsEnglish) }
                    .addOnFailureListener { busy = false; translator.close(); showResult(original, "Offline translation failed. Confirm the model is installed.", sourceIsEnglish) }
        }
        val manager = RemoteModelManager.getInstance()
        manager.isModelDownloaded(englishModel).addOnSuccessListener { englishReady ->
            manager.isModelDownloaded(chineseModel).addOnSuccessListener { chineseReady ->
                if (!englishReady || !chineseReady) {
                    busy = false
                    bubble?.post { bubble?.text = "译" }
                    showResult(original, "Both English and Chinese offline models are required. Tap the lens and download them over Wi-Fi.", sourceIsEnglish)
                } else continueTranslation()
            }.addOnFailureListener { busy = false; bubble?.post { bubble?.text = "译" }; showResult(original, "Could not check Chinese model status.", sourceIsEnglish) }
        }.addOnFailureListener { busy = false; bubble?.post { bubble?.text = "译" }; showResult(original, "Could not check English model status.", sourceIsEnglish) }
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
        card.setOnClickListener { removePanel(); scheduleCompact(500) }
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
        scheduleCompact(2600)
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }
    private fun removePanel() { panel?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }; panel = null }
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { mainHandler.removeCallbacks(compactRunnable); removePanel(); bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }; super.onDestroy() }
}

