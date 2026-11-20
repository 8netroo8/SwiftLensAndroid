package app.swifttranslate

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val Ink = Color(0xFF18221F)
private val Green = Color(0xFFB9F27C)
private val Canvas = Color(0xFFF5F6F1)
private val Muted = Color(0xFF78817C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SwiftApp() }
    }
}

@Composable
private fun SwiftApp() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Home") }
    var selected by remember { mutableStateOf<String?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, background = Canvas, surface = Color.White)) {
        Scaffold(containerColor = Canvas, bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Home" to Icons.Default.Home, "Translate" to Icons.Default.Translate, "Chats" to Icons.Default.ChatBubbleOutline, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
                    NavigationBarItem(selected = tab == name, onClick = { tab = name; selected = null }, icon = { Icon(icon, null) }, label = { Text(name) })
                }
            }
        }) { padding ->
            when (tab) {
                "Home" -> HomeScreen(Modifier.padding(padding), onOpen = {
                    if (it == "Screen translation") {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else if (it == "Photos & voice") tab = "Translate" else selected = it
                })
                "Translate" -> TranslateScreen(Modifier.padding(padding))
                "Chats" -> ChatScreen(Modifier.padding(padding)) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                else -> SettingsScreen(Modifier.padding(padding))
            }
            selected?.let { FeatureDialog(it) { selected = null } }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, onOpen: (String) -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 22.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { Text("SwiftLens", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Your words, understood everywhere.", color = Muted, fontSize = 14.sp) }
                Box(Modifier.size(42.dp).background(Green, CircleShape), Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Ink) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(26.dp)).padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = Green); Spacer(Modifier.width(8.dp)); Text("READY WHEN YOU ARE", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                Spacer(Modifier.height(15.dp)); Text("Translate without\nleaving your app.", color = Color.White, fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp)); Text("Turn on the floating bubble to read messages, posts and games in your language.", color = Color(0xFFD2D9D4), fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(18.dp)); Button(onClick = { onOpen("Screen translation") }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink), shape = RoundedCornerShape(14.dp)) { Text("Set up screen translation", fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null) }
            }
        }
        item { Text("TRANSLATE YOUR WAY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 4.dp)) }
        item { FeatureRow("Chat apps", "Translate as you read and reply", Icons.Default.Forum, onOpen) }
        item { FeatureRow("Chat mode", "Translate individual messages", Icons.Default.SwapHoriz, onOpen) }
        item { FeatureRow("Subtitles & audio", "Translate exposed subtitle text", Icons.Default.ClosedCaption, onOpen) }
        item { FeatureRow("Photos & voice", "Recognize a chosen photo or dictate", Icons.Default.CameraAlt, onOpen) }
        item {
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, null, tint = Ink); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Translation languages", fontWeight = FontWeight.SemiBold, color = Ink); Text("English  →  中文", color = Muted, fontSize = 13.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted)
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onOpen: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).clickable { onOpen(title) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color(0xFFF0F4E9), RoundedCornerShape(13.dp)), Alignment.Center) { Icon(icon, null, tint = Ink) }
        Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink); Text(subtitle, color = Muted, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun TranslateScreen(modifier: Modifier) {
    val context = LocalContext.current
    var source by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var pair by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            result = "Recognizing selected image…"
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        source = text.text
                        result = if (text.text.isBlank()) "No readable text found in this image." else "Image text loaded. Tap Translate."
                        recognizer.close()
                    }
                    .addOnFailureListener { result = "Could not read this image."; recognizer.close() }
            } catch (_: Exception) { result = "Could not open this image." }
        }
    }
    val speechRecognizer = remember {
        if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else null
    }
    DisposableEffect(speechRecognizer) {
        if (speechRecognizer != null) speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; result = "Listening on this device…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) { listening = false; result = "On-device speech recognition failed. Check the device language pack and try again." }
            override fun onResults(results: Bundle?) {
                listening = false
                source = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                result = if (source.isBlank()) "No speech recognized." else "Speech text loaded. Tap Translate."
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { speechRecognizer?.destroy() }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (pair) "zh-CN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            speechRecognizer.startListening(intent)
        } else result = "On-device speech recognition or microphone permission is unavailable."
    }
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Translate", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(if (pair) "中文" else "English", Modifier.clickable { pair = !pair }.padding(12.dp), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.SwapHoriz, null, tint = Muted)
            Text(if (pair) "English" else "中文", Modifier.clickable { pair = !pair }.padding(12.dp), fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(source, { source = it }, Modifier.fillMaxWidth().height(170.dp), placeholder = { Text("Type, paste, or load text") }, shape = RoundedCornerShape(20.dp), trailingIcon = { IconButton(onClick = { source = "" }) { Icon(Icons.Default.Close, "Clear") } })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(6.dp)); Text("Choose photo") }
            OutlinedButton(onClick = {
                if (speechRecognizer == null) result = "This device does not provide on-device speech recognition."
                else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }, modifier = Modifier.weight(1f), enabled = !listening) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(6.dp)); Text(if (listening) "Listening…" else "Speak") }
        }
        Button(onClick = {
            if (source.isBlank()) result = "Enter some text to translate."
            else {
                val from = if (pair) TranslateLanguage.CHINESE else TranslateLanguage.ENGLISH
                val to = if (pair) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
                fun runTranslation() {
                    val translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build())
                    translator.translate(source.trim())
                        .addOnSuccessListener { result = it; translator.close() }
                        .addOnFailureListener { result = "Translation failed. Check that the offline model is installed."; translator.close() }
                }
                val zhModel = TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build()
                RemoteModelManager.getInstance().isModelDownloaded(zhModel)
                    .addOnSuccessListener { ready -> if (ready) runTranslation() else result = "Chinese offline model not installed. Download it from the Translate Lens menu while online." }
                    .addOnFailureListener { result = "Could not check offline model status." }
            }
        }, Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Translate, null); Spacer(Modifier.width(8.dp)); Text("Translate") }
        if (result.isNotEmpty()) Text(result, Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(18.dp), color = Ink, fontSize = 17.sp)
        Text("Translation runs with downloaded offline language models. Photo recognition processes only an image you select. Voice input uses on-device recognition when supported.", color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ChatScreen(modifier: Modifier, onSetup: () -> Unit) {
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Chat mode", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Translate selected messages while you read or write in another app.", color = Muted)
        Text("Drag the floating lens over a message to translate its accessibility text node. For replies, copy text into the Translate tab, translate it, then paste it back.", Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(18.dp), color = Ink, lineHeight = 22.sp)
        Button(onClick = onSetup, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(14.dp)) { Text("Set up Translate Lens") }
        Text("The lens does not read notifications or scan the whole screen. Some apps do not expose their message text to Android Accessibility.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        listOf("Languages" to "English ↔ 简体中文", "Floating bubble" to "Set up screen translation", "Privacy" to "Translation and selected-photo OCR run on device", "About" to "SwiftLens for Android · 0.2.0").forEach { (title, detail) ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(17.dp)).padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(3.dp)); Text(detail, color = Muted, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted) }
        }
    }
}

@Composable
private fun FeatureDialog(feature: String, dismiss: () -> Unit) {
    val detail = when (feature) {
        "Chat apps" -> "Open a supported app and drag Translate Lens over the message. The app must expose its text through Android Accessibility."
        "Chat mode" -> "Use the floating lens on individual messages. SwiftLens does not read notifications or monitor a conversation in the background."
        "Subtitles & audio" -> "Drag the lens over subtitles only when the video app exposes subtitle text as an accessibility node. Audio transcription is not enabled in this build."
        else -> "Choose Photos & voice on the Translate tab to recognize a selected image or dictate with on-device speech recognition. No screen image is captured."
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text(feature) }, text = { Text(detail) }, confirmButton = { TextButton(onClick = dismiss) { Text("Got it") } })
}

