package app.swifttranslate

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                    } else selected = it
                })
                "Translate" -> TranslateScreen(Modifier.padding(padding))
                "Chats" -> ChatScreen(Modifier.padding(padding))
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
        item { FeatureRow("Chat mode", "Follow a whole conversation", Icons.Default.SwapHoriz, onOpen) }
        item { FeatureRow("Subtitles & audio", "Understand videos as they play", Icons.Default.ClosedCaption, onOpen) }
        item { FeatureRow("Photos & voice", "Translate what you see or say", Icons.Default.CameraAlt, onOpen) }
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
    var source by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var pair by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Translate", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(if (pair) "中文" else "English", Modifier.clickable { pair = !pair }.padding(12.dp), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.SwapHoriz, null, tint = Muted)
            Text(if (pair) "English" else "中文", Modifier.clickable { pair = !pair }.padding(12.dp), fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(source, { source = it }, Modifier.fillMaxWidth().height(170.dp), placeholder = { Text("Type or paste text") }, shape = RoundedCornerShape(20.dp), trailingIcon = { IconButton(onClick = { source = "" }) { Icon(Icons.Default.Mic, "Voice input") } })
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
        Text("More than 100 languages", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun ChatScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Chat mode", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Read a conversation in your language as new messages arrive.", color = Muted)
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Chat mode", fontWeight = FontWeight.SemiBold); Text("Turn on only when you need it", color = Muted, fontSize = 13.sp) }; var on by remember { mutableStateOf(false) }; Switch(on, { on = it }) }
        Text("HOW IT WORKS", color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
        listOf("1  Choose a supported chat app", "2  Grant notification access", "3  Read translated messages in the chat").forEach { Text(it, Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(15.dp)).padding(16.dp), color = Ink) }
        Text("Chat mode is off until you turn it on. Android notification access is required for incoming message translation.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        listOf("Languages" to "English → 中文", "Floating bubble" to "Set up screen translation", "Privacy" to "Your text is sent only when you translate", "About" to "Swift Translate for Android · 0.1.0").forEach { (title, detail) ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(17.dp)).padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(3.dp)); Text(detail, color = Muted, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted) }
        }
    }
}

@Composable
private fun FeatureDialog(feature: String, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(feature) }, text = { Text("This feature needs Android system access and a translation provider. This prototype maps the setup flow; connect the required service and permission screens to enable it.") }, confirmButton = { TextButton(onClick = dismiss) { Text("Got it") } })
}
