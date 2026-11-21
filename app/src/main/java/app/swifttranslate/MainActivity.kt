package app.swifttranslate

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

private val Blue = Color(0xFF2878F0)
private val Navy = Color(0xFF16243A)
private val Page = Color(0xFFF5F7FA)
private val Secondary = Color(0xFF768397)
private val PaleBlue = Color(0xFFEAF2FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SwiftLensApp(intent.getStringExtra("preview_page") ?: "Home") }
    }
}

@Composable
private fun SwiftLensApp(initialPage: String) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("lens", android.content.Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(initialPage) }
    var draft by remember { mutableStateOf("") }
    var englishToChinese by remember { mutableStateOf(preferences.getBoolean("english_to_chinese", true)) }
    val rootPages = setOf("Home", "Translate", "Lens", "Settings")
    BackHandler(page !in rootPages) { page = if (page in setOf("Models", "OnlineProvider")) "Settings" else "Home" }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Page, surface = Color.White)) {
        Scaffold(containerColor = Page, bottomBar = {
            if (page in rootPages) NavigationBar(containerColor = Color.White) {
                listOf("Home" to Icons.Default.Home, "Translate" to Icons.Default.Translate, "Lens" to Icons.Default.Lens, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
                    NavigationBarItem(selected = page == name, onClick = { page = name }, icon = { Icon(icon, null) }, label = { Text(name) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, selectedTextColor = Blue, indicatorColor = PaleBlue))
                }
            }
        }) { inset ->
            val modifier = Modifier.padding(inset)
            when (page) {
                "Home" -> HomePage(modifier, { page = it })
                "Translate" -> TranslatePage(modifier, draft, { draft = it }, englishToChinese, { englishToChinese = !englishToChinese; preferences.edit().putBoolean("english_to_chinese", englishToChinese).apply() }, { page = it })
                "Lens" -> LensPage(modifier, { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, { page = it })
                "Settings" -> SettingsPage(modifier, { page = it }, englishToChinese)
                "Models" -> OfflineModelsPage(modifier)
                "OnlineProvider" -> AlibabaProviderPage(modifier) { page = "Settings" }
                "Languages" -> LanguagePage(modifier, englishToChinese, { englishToChinese = it; preferences.edit().putBoolean("english_to_chinese", it).apply(); page = "Settings" })
                "Setup" -> SetupPage(modifier, { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, { page = "Lens" })
                "Photo" -> PhotoPage(modifier, { draft = it; page = "Translate" }, { page = "Home" })
                "Voice" -> VoicePage(modifier, englishToChinese, { page = "Home" }, { page = "Models" }, { page = "OnlineProvider" })
                "Chat" -> ChatPage(modifier, { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
                "Subtitles" -> SubtitlePage(modifier, { page = "Setup" })
                "Privacy" -> InfoPage(modifier, "Privacy", "Offline translation and selected-photo OCR run on this device after model setup. Online translation sends only the text you explicitly confirm to Alibaba Cloud Machine Translation. Your AccessKey is encrypted with Android Keystore on this device. The lens reads only the accessibility text node near your drop point. No display capture, notifications, ads, or billing are used.") { page = "Settings" }
                "About" -> InfoPage(modifier, "About Translate Len", "Translate Len for Android · English ↔ Simplified Chinese · Android 16 target · v0.8.0\n\nDeveloper: 8netroo8") { page = "Settings" }
                else -> HomePage(modifier, { page = it })
            }
        }
    }
}

@Composable
private fun PageFrame(modifier: Modifier, title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Navy)
                if (subtitle != null) Text(subtitle, color = Secondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
    }
}

@Composable
private fun HomePage(modifier: Modifier, open: (String) -> Unit) {
    PageFrame(modifier, "Translate Len", "Translate messages, text and speech.") {
        Column(Modifier.fillMaxWidth().background(Blue, RoundedCornerShape(24.dp)).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TRANSLATE ANYWHERE", color = Color.White.copy(alpha = .78f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(9.dp)); Text("Understand every\nconversation.", color = Color.White, fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(58.dp).background(Color.White.copy(alpha = .17f), CircleShape), Alignment.Center) { Icon(Icons.Default.Translate, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = { open("Setup") }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Blue), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Lens, null); Spacer(Modifier.width(8.dp)); Text("Set up Translate Lens", fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(4.dp)); Icon(Icons.Default.ArrowForward, null)
            }
        }
        Text("QUICK TRANSLATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Secondary, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Text", "Type or paste", Icons.Default.Translate, Modifier.weight(1f)) { open("Translate") }
            ActionCard("Photo", "Choose an image", Icons.Default.Image, Modifier.weight(1f)) { open("Photo") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Voice", "Speak to translate", Icons.Default.Mic, Modifier.weight(1f)) { open("Voice") }
            ActionCard("Chat apps", "Use the lens in chats", Icons.Default.ChatBubbleOutline, Modifier.weight(1f)) { open("Chat") }
        }
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { open("Subtitles") }) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ClosedCaption, null, tint = Blue); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text("Video subtitles", color = Navy, fontWeight = FontWeight.SemiBold); Text("Translate accessible captions", color = Secondary, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Secondary)
            }
        }
        Row(Modifier.fillMaxWidth().clickable { open("Languages") }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, null, tint = Blue); Spacer(Modifier.width(10.dp)); Text("English  ↔  简体中文", Modifier.weight(1f), color = Navy, fontWeight = FontWeight.Medium); Icon(Icons.Default.ChevronRight, null, tint = Secondary)
        }
    }
}

@Composable
private fun ActionCard(title: String, note: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), color = Color.White, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.size(38.dp).background(PaleBlue, RoundedCornerShape(12.dp)), Alignment.Center) { Icon(icon, null, tint = Blue) }
            Column { Text(title, color = Navy, fontWeight = FontWeight.SemiBold); Text(note, color = Secondary, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun TranslatePage(modifier: Modifier, draft: String, onDraft: (String) -> Unit, enToZh: Boolean, swap: () -> Unit, open: (String) -> Unit) {
    val context = LocalContext.current
    var source by remember(draft) { mutableStateOf(draft) }
    var result by remember { mutableStateOf("") }
    var flowStep by remember { mutableIntStateOf(0) }
    var onlineMode by remember { mutableStateOf(false) }

    fun translateOffline() {
        result = "Translating offline…"
        val from = if (enToZh) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
        val to = if (enToZh) TranslateLanguage.CHINESE else TranslateLanguage.ENGLISH
        val enModel = TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build()
        val zhModel = TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build()
        val modelManager = RemoteModelManager.getInstance()
        modelManager.isModelDownloaded(enModel).addOnSuccessListener { englishReady ->
            modelManager.isModelDownloaded(zhModel).addOnSuccessListener { chineseReady ->
                if (!englishReady || !chineseReady) result = "Download both offline language models from Settings → Offline models first."
                else {
                    val translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build())
                    translator.translate(source.trim()).addOnSuccessListener { result = it; translator.close() }.addOnFailureListener { result = "Offline translation failed. Try again."; translator.close() }
                }
            }
        }.addOnFailureListener { result = "Could not check offline model status." }
    }

    PageFrame(modifier, "Text translation", "Type, paste, speak or choose a photo.") {
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = swap).padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(if (enToZh) "English" else "简体中文", color = Navy, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(20.dp)); Icon(Icons.Default.SwapHoriz, null, tint = Blue); Spacer(Modifier.width(20.dp))
                Text(if (enToZh) "简体中文" else "English", color = Navy, fontWeight = FontWeight.SemiBold)
            }
        }
        OutlinedTextField(source, { source = it; onDraft(it) }, Modifier.fillMaxWidth().height(180.dp), placeholder = { Text("Enter text") }, shape = RoundedCornerShape(18.dp), trailingIcon = { IconButton(onClick = { source = ""; onDraft("") }) { Icon(Icons.Default.Close, "Clear") } }, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE4E9F0), focusedBorderColor = Blue, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { open("Photo") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(5.dp)); Text("Photo") }
            OutlinedButton(onClick = { open("Voice") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(5.dp)); Text("Voice") }
        }
        Button(onClick = { if (source.isBlank()) result = "Enter text to translate." else flowStep = 1 }, Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Translate, null); Spacer(Modifier.width(8.dp)); Text("Translate", fontWeight = FontWeight.SemiBold) }
        if (result.isNotBlank()) Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text("TRANSLATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Secondary, letterSpacing = 1.sp); Spacer(Modifier.height(8.dp)); Text(result, color = Navy, fontSize = 18.sp, lineHeight = 25.sp) } }
        Text("Offline translation runs on this device. Online translation uses Alibaba Cloud only after you confirm each request.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
    }

    if (flowStep == 1) AlertDialog(
        onDismissRequest = { flowStep = 0 },
        title = { Text("Choose translation method") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onlineMode = false }) {
                    RadioButton(selected = !onlineMode, onClick = { onlineMode = false }); Column { Text("Offline · ML Kit"); Text("Runs on this device after model download.", fontSize = 12.sp, color = Secondary) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onlineMode = true }) {
                    RadioButton(selected = onlineMode, onClick = { onlineMode = true }); Column { Text("Online · Alibaba Cloud"); Text("In-app translation. You will confirm before text is sent.", fontSize = 12.sp, color = Secondary) }
                }
                if (onlineMode) Text("Alibaba Cloud may charge for API usage. Configure credentials in Settings → Online provider.", fontSize = 12.sp, color = Secondary, lineHeight = 17.sp)
            }
        },
        confirmButton = { TextButton(onClick = {
            if (onlineMode) {
                flowStep = 2
            } else { flowStep = 0; translateOffline() }
        }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { flowStep = 0 }) { Text("Cancel") } }
    )

    if (flowStep == 2) AlertDialog(
        onDismissRequest = { flowStep = 0 },
        title = { Text("Send text to Alibaba Cloud?") },
        text = { Text("Your text will be sent to Alibaba Cloud Machine Translation for processing. This API may incur charges. No website will open. Continue only if you agree.") },
        confirmButton = { TextButton(onClick = {
            flowStep = 0
            if (!AlibabaCredentialStore.hasCredentials(context)) {
                result = "Add your Alibaba Cloud AccessKey credentials in Settings → Online provider first. No text was sent."
                open("OnlineProvider")
            } else {
                result = "Translating with Alibaba Cloud…"
                AlibabaCloudTranslation.translate(context, source.trim(), enToZh) { translated, error ->
                    result = translated ?: (error ?: "Alibaba Cloud translation failed.")
                }
            }
        }) { Text("Agree and translate") } },
        dismissButton = { TextButton(onClick = { flowStep = 0 }) { Text("Cancel") } }
    )

}

@Composable
private fun LensPage(modifier: Modifier, setup: () -> Unit, open: (String) -> Unit) {
    PageFrame(modifier, "Translate Lens", "Translate text inside other apps with the floating lens. Manage language and model settings in the app.") {
        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(82.dp).background(PaleBlue, CircleShape), Alignment.Center) { Icon(Icons.Default.Lens, null, tint = Blue, modifier = Modifier.size(38.dp)) }
                Text("Translate Lens", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Drag the lens onto a message, post or subtitle. Translate Len reads the nearest accessibility text node and shows the translation beside it.", color = Secondary, fontSize = 13.sp, lineHeight = 19.sp)
                Button(onClick = setup, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) { Text("Enable Accessibility Service") }
                OutlinedButton(onClick = { open("Setup") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("View setup steps") }
            }
        }
        ModelCard { open("Models") }
        Text("Canvas-rendered, image-only and protected text may not be available through Accessibility.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ModelCard(openManager: () -> Unit) {
    val manager = remember { RemoteModelManager.getInstance() }
    val englishModel = remember { TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build() }
    val chineseModel = remember { TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build() }
    var status by remember { mutableStateOf("Checking model…") }
    fun refresh() {
        manager.isModelDownloaded(englishModel).addOnSuccessListener { en ->
            manager.isModelDownloaded(chineseModel).addOnSuccessListener { zh -> status = "English ${if (en) "ready" else "missing"} · Chinese ${if (zh) "ready" else "missing"}" }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudDownload, null, tint = Blue); Spacer(Modifier.width(10.dp)); Column { Text("Offline language model", color = Navy, fontWeight = FontWeight.SemiBold); Text(status, color = Secondary, fontSize = 12.sp) } }
            Text("Choose which language packs to download or remove.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
            TextButton(onClick = openManager, contentPadding = PaddingValues(0.dp)) { Text("Manage offline models") }
        }
    }
}

@Composable
private fun OfflineModelsPage(modifier: Modifier) {
    val manager = remember { RemoteModelManager.getInstance() }
    val englishModel = remember { TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build() }
    val chineseModel = remember { TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build() }
    val conditions = remember { com.google.mlkit.common.model.DownloadConditions.Builder().requireWifi().build() }
    var englishReady by remember { mutableStateOf(false) }
    var chineseReady by remember { mutableStateOf(false) }
    var englishSelected by remember { mutableStateOf(true) }
    var chineseSelected by remember { mutableStateOf(true) }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Checking installed language packs…") }

    fun refreshModels() {
        manager.isModelDownloaded(englishModel).addOnSuccessListener { englishReady = it
            manager.isModelDownloaded(chineseModel).addOnSuccessListener { chineseReady = it
                status = if (englishReady && chineseReady) "Both packs are ready. Offline translation works in either direction." else "Select one or both packs below. The pair needs both packs to translate offline."
            }.addOnFailureListener { status = "Could not check the Chinese pack." }
        }.addOnFailureListener { status = "Could not check the English pack." }
    }
    LaunchedEffect(Unit) { refreshModels() }

    fun downloadSelected() {
        val selected = buildList {
            if (englishSelected && !englishReady) add(englishModel to "English")
            if (chineseSelected && !chineseReady) add(chineseModel to "Simplified Chinese")
        }
        if (selected.isEmpty()) { status = "Select a pack that is not already installed."; return }
        working = true
        fun downloadAt(index: Int) {
            if (index >= selected.size) { working = false; refreshModels(); return }
            val (model, language) = selected[index]
            status = "Downloading $language pack over Wi-Fi…"
            manager.download(model, conditions)
                .addOnSuccessListener { status = "$language pack installed."; downloadAt(index + 1) }
                .addOnFailureListener { error -> working = false; status = "$language download failed: ${error.localizedMessage ?: "check Wi-Fi and retry"}"; refreshModels() }
        }
        downloadAt(0)
    }

    fun removeSelected() {
        val selected = buildList {
            if (englishSelected && englishReady) add(englishModel to "English")
            if (chineseSelected && chineseReady) add(chineseModel to "Simplified Chinese")
        }
        if (selected.isEmpty()) { status = "Select an installed pack to remove."; return }
        working = true
        fun removeAt(index: Int) {
            if (index >= selected.size) { working = false; refreshModels(); return }
            val (model, language) = selected[index]
            status = "Removing $language pack…"
            manager.deleteDownloadedModel(model)
                .addOnSuccessListener { status = "$language pack removed."; removeAt(index + 1) }
                .addOnFailureListener { error -> working = false; status = "$language removal failed: ${error.localizedMessage ?: "try again"}"; refreshModels() }
        }
        removeAt(0)
    }

    PageFrame(modifier, "Offline models", "Select individual language packs for offline English ↔ Simplified Chinese translation.") {
        ModelSelectionRow("English", "English model", englishReady, englishSelected, !working) { englishSelected = it }
        ModelSelectionRow("简体中文", "Simplified Chinese model", chineseReady, chineseSelected, !working) { chineseSelected = it }
        Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (englishReady && chineseReady) "Offline translation ready" else "Both packs are needed for this language pair", color = Navy, fontWeight = FontWeight.SemiBold)
                Text("Downloads require Wi-Fi. Translation runs locally after installation; downloaded packs are stored on this device.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(status, color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Button(onClick = ::downloadSelected, enabled = !working, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("Download selected packs")
        }
        OutlinedButton(onClick = ::removeSelected, enabled = !working, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Remove selected packs")
        }
        Text("You can download or remove either pack independently. Keep both installed to use offline translation in both directions.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ModelSelectionRow(label: String, detail: String, ready: Boolean, selected: Boolean, enabled: Boolean, onSelected: (Boolean) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = onSelected, enabled = enabled)
            Column(Modifier.weight(1f)) { Text(label, color = Navy, fontWeight = FontWeight.SemiBold); Text(detail, color = Secondary, fontSize = 12.sp) }
            Text(if (ready) "Downloaded" else "Not installed", color = if (ready) Color(0xFF27834A) else Secondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SetupPage(modifier: Modifier, openAccessibility: () -> Unit, done: () -> Unit) {
    PageFrame(modifier, "Set up Translate Lens", "Allow Translate Len to read only the text you point the lens at.") {
        listOf("1" to "Open Accessibility settings", "2" to "Choose Translate Len Lens and turn it on", "3" to "Return here, then drag the floating lens onto text").forEach { (number, text) ->
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).background(PaleBlue, CircleShape), Alignment.Center) { Text(number, color = Blue, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(13.dp)); Text(text, color = Navy, fontWeight = FontWeight.Medium, lineHeight = 20.sp) } }
        }
        Button(onClick = openAccessibility, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) { Text("Open Accessibility settings") }
        TextButton(onClick = done, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("I’ll do this later", color = Secondary) }
        Text("Translate Len does not use MediaProjection, screen recording or screenshots.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ChatPage(modifier: Modifier, setup: () -> Unit) {
    PageFrame(modifier, "Chat translator", "Use the floating lens inside your messaging apps.") {
        listOf("WhatsApp", "Instagram", "Telegram", "Messenger", "Discord", "Other apps").forEach { app ->
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).background(PaleBlue, RoundedCornerShape(12.dp)), Alignment.Center) { Icon(Icons.Default.ChatBubbleOutline, null, tint = Blue) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(app, color = Navy, fontWeight = FontWeight.SemiBold); Text("Drag the lens over a message", color = Secondary, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Secondary) }
            }
        }
        Button(onClick = setup, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) { Text("Set up Translate Lens") }
        Text("This build translates exposed text nodes one at a time. It does not read notifications or monitor chats in the background.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun SubtitlePage(modifier: Modifier, setup: () -> Unit) {
    PageFrame(modifier, "Subtitle translator", "Translate captions when the video app exposes their text.") {
        Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.ClosedCaption, null, tint = Blue, modifier = Modifier.size(34.dp))
                Text("Point the lens at a caption", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Open a video with accessible captions, enable Translate Lens, then drag it onto a caption. Translate Len reads the caption node exposed by that app.", color = Secondary, lineHeight = 20.sp)
                Text("Video audio transcription and screen capture are not part of this build.", color = Secondary, fontSize = 12.sp)
            }
        }
        Button(onClick = setup, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) { Text("Set up Translate Lens") }
    }
}

@Composable
private fun PhotoPage(modifier: Modifier, onText: (String) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Choose a photo containing text.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            status = "Recognizing selected photo…"
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                recognizer.process(image).addOnSuccessListener { result ->
                    recognizer.close()
                    if (result.text.isBlank()) status = "No text found. Try a clearer photo."
                    else { status = "Text recognized."; onText(result.text) }
                }.addOnFailureListener { recognizer.close(); status = "Could not read this photo." }
            } catch (_: Exception) { status = "Could not open this photo." }
        }
    }
    PageFrame(modifier, "Photo translator", "Select a photo and recognize its text on this device.") {
        Surface(color = Color.White, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().height(230.dp).clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(64.dp).background(PaleBlue, CircleShape), Alignment.Center) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Blue, modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.height(13.dp)); Text("Choose photo", color = Navy, fontWeight = FontWeight.SemiBold); Text("From your photo library", color = Secondary, fontSize = 12.sp)
            }
        }
        Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Select an image") }
        Text(status, color = Secondary, fontSize = 13.sp)
        Text("Only the image you choose is processed. This does not capture your display.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
        TextButton(onClick = back, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to home") }
    }
}

@Composable
private fun VoicePage(modifier: Modifier, enToZh: Boolean, back: () -> Unit, openModels: () -> Unit, openOnlineProvider: () -> Unit) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Tap the microphone and speak.") }
    var listening by remember { mutableStateOf(false) }
    var showSpeechConsent by remember { mutableStateOf(false) }
    var showTranslationMethod by remember { mutableStateOf(false) }
    var showOnlineConfirmation by remember { mutableStateOf(false) }
    var onlineMode by remember { mutableStateOf(false) }
    val onDeviceRecognition = remember { Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
    val recognizer = remember { if (onDeviceRecognition) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context) }
    val sourceLanguage = if (enToZh) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
    val targetLanguage = if (enToZh) TranslateLanguage.CHINESE else TranslateLanguage.ENGLISH
    fun translateOfflineTranscript() {
        if (transcript.isBlank()) { status = "Speak or enter text first."; return }
        status = "Checking offline language models…"
        val manager = RemoteModelManager.getInstance()
        val englishModel = TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build()
        val chineseModel = TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build()
        manager.isModelDownloaded(englishModel).addOnSuccessListener { englishReady ->
            manager.isModelDownloaded(chineseModel).addOnSuccessListener { chineseReady ->
                if (!englishReady || !chineseReady) {
                    status = "Download both language packs to translate offline."
                    return@addOnSuccessListener
                }
                status = "Translating on this device…"
                val translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(sourceLanguage).setTargetLanguage(targetLanguage).build())
                translator.translate(transcript.trim())
                    .addOnSuccessListener { translation = it; status = "Translated offline."; translator.close() }
                    .addOnFailureListener { status = "Translation failed. Check the downloaded models and try again."; translator.close() }
            }.addOnFailureListener { status = "Could not check the Chinese model." }
        }.addOnFailureListener { status = "Could not check the English model." }
    }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; translation = ""; status = if (onDeviceRecognition) "Listening on this device…" else "Listening with your device's speech service…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false; status = "Processing speech…" }
            override fun onError(error: Int) { listening = false; status = when (error) { SpeechRecognizer.ERROR_AUDIO -> "Microphone unavailable. Check app permission."; SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service needs a network connection."; SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech recognized. Try again."; SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."; else -> "Speech recognition failed ($error). Try again." } }
            override fun onResults(results: Bundle?) { listening = false; val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); if (text.isBlank()) status = "No speech recognized. Try again." else { transcript = text; status = "Speech recognized. Review the text, then translate." } }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) status = "Microphone permission is required."
        else runCatching { recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (enToZh) "en-US" else "zh-CN"); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) }) }.onFailure { status = "Could not start speech recognition: ${it.localizedMessage ?: "try again"}" }
    }
    PageFrame(modifier, "Voice translation", "Speak, review the transcript, then choose offline or Alibaba Cloud translation.") {
        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (enToZh) "English → 简体中文" else "简体中文 → English", color = Blue, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { if (listening) { recognizer.cancel(); listening = false; status = "Listening cancelled." } else if (onDeviceRecognition) permission.launch(android.Manifest.permission.RECORD_AUDIO) else showSpeechConsent = true }, colors = ButtonDefaults.buttonColors(containerColor = if (listening) Color(0xFFB3261E) else Blue), shape = RoundedCornerShape(14.dp)) {
                        Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (listening) "Stop" else "Speak")
                    }
                    OutlinedButton(onClick = { transcript = ""; translation = ""; status = "Tap the microphone and speak." }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Clear") }
                }
                OutlinedTextField(transcript, { transcript = it; translation = "" }, modifier = Modifier.fillMaxWidth().height(140.dp), label = { Text("Recognized text · editable") }, shape = RoundedCornerShape(15.dp))
                Button(onClick = { showTranslationMethod = true }, enabled = transcript.isNotBlank() && !listening, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Translate, null); Spacer(Modifier.width(8.dp)); Text("Translate") }
                if (translation.isNotBlank()) Surface(color = PaleBlue, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("TRANSLATION", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(translation, color = Navy, fontSize = 18.sp, lineHeight = 24.sp) } }
                Text(status, color = Secondary, fontSize = 13.sp)
            }
        }
        Text(if (onDeviceRecognition) "Speech recognition is on-device. Microphone permission is requested only after tapping Speak." else "On-device recognition is unavailable. The device speech service may use a network; review recognized text before translating. Microphone permission is requested only after tapping Speak.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
        TextButton(onClick = openModels, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Manage offline language models") }
        TextButton(onClick = back, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to home") }
    }
    if (showTranslationMethod) AlertDialog(
        onDismissRequest = { showTranslationMethod = false },
        title = { Text("Choose translation method") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onlineMode = false }) {
                    RadioButton(selected = !onlineMode, onClick = { onlineMode = false }); Column { Text("Offline · ML Kit"); Text("Runs on this device after model download.", fontSize = 12.sp, color = Secondary) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onlineMode = true }) {
                    RadioButton(selected = onlineMode, onClick = { onlineMode = true }); Column { Text("Online · Alibaba Cloud"); Text("Sends transcript only after your confirmation.", fontSize = 12.sp, color = Secondary) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showTranslationMethod = false; if (onlineMode) showOnlineConfirmation = true else translateOfflineTranscript() }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { showTranslationMethod = false }) { Text("Cancel") } }
    )
    if (showOnlineConfirmation) AlertDialog(
        onDismissRequest = { showOnlineConfirmation = false },
        title = { Text("Send transcript to Alibaba Cloud?") },
        text = { Text("The recognized text will be sent to Alibaba Cloud Machine Translation. API usage may incur charges. No audio is sent by this translation request.") },
        confirmButton = { TextButton(onClick = {
            showOnlineConfirmation = false
            if (!AlibabaCredentialStore.hasCredentials(context)) {
                status = "Add Alibaba Cloud credentials in Settings → Online provider first. No text was sent."
                openOnlineProvider()
            } else {
                status = "Translating with Alibaba Cloud…"
                AlibabaCloudTranslation.translate(context, transcript.trim(), enToZh) { translated, error ->
                    translation = translated.orEmpty()
                    status = if (translated != null) "Translated with Alibaba Cloud." else (error ?: "Alibaba Cloud translation failed.")
                }
            }
        }) { Text("Agree and translate") } },
        dismissButton = { TextButton(onClick = { showOnlineConfirmation = false }) { Text("Cancel") } }
    )
    if (showSpeechConsent) AlertDialog(
        onDismissRequest = { showSpeechConsent = false },
        title = { Text("Use device speech service?") },
        text = { Text("This device does not report on-device speech recognition as available. The selected speech service may send audio for recognition. Continue only if you are comfortable with that; translation remains offline.") },
        confirmButton = { TextButton(onClick = { showSpeechConsent = false; permission.launch(android.Manifest.permission.RECORD_AUDIO) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { showSpeechConsent = false }) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsPage(modifier: Modifier, open: (String) -> Unit, enToZh: Boolean) {
    PageFrame(modifier, "Settings") {
        SettingsRow("Translation languages", if (enToZh) "English → 简体中文" else "简体中文 → English", Icons.Default.Language) { open("Languages") }
        SettingsRow("Translate Lens", "Accessibility and floating bubble", Icons.Default.Lens) { open("Lens") }
        SettingsRow("Offline models", "Choose and manage language packs", Icons.Default.CloudDownload) { open("Models") }
        SettingsRow("Online provider", "Alibaba Cloud Machine Translation", Icons.Default.Cloud) { open("OnlineProvider") }
        SettingsRow("Privacy", "How Translate Len reads and translates text", Icons.Default.Security) { open("Privacy") }
        SettingsRow("About", "Translate Len · 0.8.0 · Developer 8netroo8", Icons.Default.Info) { open("About") }
        Text("No ads · No billing · No screen capture", color = Secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun AlibabaProviderPage(modifier: Modifier, back: () -> Unit) {
    val context = LocalContext.current
    var accessKeyId by remember { mutableStateOf("") }
    var accessKeySecret by remember { mutableStateOf("") }
    var configured by remember { mutableStateOf(AlibabaCredentialStore.hasCredentials(context)) }
    var status by remember { mutableStateOf(if (configured) "Alibaba Cloud credentials are saved on this device." else "No Alibaba Cloud credentials saved.") }

    PageFrame(modifier, "Online provider", "Alibaba Cloud Machine Translation · English ↔ 简体中文") {
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Configure AccessKey", color = Navy, fontWeight = FontWeight.SemiBold)
                Text("Use an Alibaba Cloud RAM user credential limited to the Machine Translation call permission. Do not use your root account key.", color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
                OutlinedTextField(accessKeyId, { accessKeyId = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("AccessKey ID") }, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(accessKeySecret, { accessKeySecret = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("AccessKey Secret") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp))
                Text("The secret is encrypted with Android Keystore and stored only on this device. The APK contains no cloud credentials. Translation text is sent only after you confirm an online request.", color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
                Text(status, color = if (configured) Color(0xFF27834A) else Secondary, fontSize = 12.sp)
            }
        }
        Button(onClick = {
            runCatching { AlibabaCredentialStore.save(context, accessKeyId, accessKeySecret) }
                .onSuccess {
                    configured = true
                    accessKeyId = ""
                    accessKeySecret = ""
                    status = "Credentials encrypted and saved on this device."
                }
                .onFailure { status = "Could not save credentials: ${it.localizedMessage ?: "try again"}" }
        }, enabled = accessKeyId.isNotBlank() && accessKeySecret.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text("Save credentials")
        }
        if (configured) OutlinedButton(onClick = {
            AlibabaCredentialStore.clear(context)
            configured = false
            accessKeyId = ""
            accessKeySecret = ""
            status = "Credentials removed from this device."
        }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Remove saved credentials")
        }
        Text("Each text, lens, or voice translation is sent only after you select Alibaba Cloud and confirm. Requests are limited to 5,000 characters.", color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
        TextButton(onClick = back, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to settings") }
    }
}

@Composable
private fun SettingsRow(title: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Blue); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, color = Navy, fontWeight = FontWeight.SemiBold); Text(detail, color = Secondary, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Secondary) }
    }
}

@Composable
private fun LanguagePage(modifier: Modifier, enToZh: Boolean, select: (Boolean) -> Unit) {
    PageFrame(modifier, "Translation languages", "Choose the direction for text, voice and the floating lens.") {
        listOf(true to "English → 简体中文", false to "简体中文 → English").forEach { (value, label) ->
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { select(value) }) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = Navy, fontWeight = FontWeight.SemiBold); if (value == enToZh) Icon(Icons.Default.CheckCircle, null, tint = Blue) }
            }
        }
    }
}

@Composable
private fun InfoPage(modifier: Modifier, title: String, body: String, back: () -> Unit) {
    PageFrame(modifier, title) {
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) { Text(body, Modifier.padding(20.dp), color = Navy, lineHeight = 23.sp) }
        TextButton(onClick = back, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to settings") }
    }
}

