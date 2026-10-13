package com.apkfail.tycoon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private val Sky = Color(0xFFDDF3FF)
private val Blue = Color(0xFF168AC4)
private const val DOCS = """FGH ENGINE — ДОКУМЕНТАЦИЯ
Версия 0.1

FGH — простой декларативный язык для описания небольших игр в одном текстовом файле .fgh.

ПЕРВАЯ ИГРА
игра "Привет"
фон #DDF3FF
текст "Привет, мир!" сверху

2D-ПРИМЕР
игра "Площадка"
фон #EAF8FF
2D
круг мяч цвет #168AC4 позиция 120 120 размер 40
прямоугольник платформа цвет #315A7D позиция 40 260 размер 220 24
текст "Моя игра" сверху

3D-ПРИМЕР
игра "Мой мир"
фон #DDF3FF
3D
камера 0 2 6
свет солнце
куб герой цвет #58BFFF позиция 0 0 0
вращай герой 0 1 0.5

КОМАНДЫ
игра "Название" — заголовок.
фон #RRGGBB — цвет фона.
2D или 3D — режим сцены.
текст "Сообщение" сверху — текст вверху.
круг ИМЯ цвет #RRGGBB позиция X Y размер R — круг.
прямоугольник ИМЯ цвет #RRGGBB позиция X Y размер ШИРИНА ВЫСОТА — прямоугольник.
куб ИМЯ цвет #RRGGBB позиция X Y Z — демонстрационный куб.
камера X Y Z и свет солнце — зарезервированные команды сцены.
вращай ИМЯ X Y Z — зарезервированная команда анимации.

ПРАВИЛА
Каждая команда на отдельной строке. Текст указывается в двойных кавычках. Цвет — HEX #RRGGBB. Координаты разделяются пробелами. Строки, начинающиеся с //, считаются комментариями. Не вставляйте HTML или JavaScript: файл содержит только команды FGH.

ОТКРЫТИЕ
Создайте текстовый файл и сохраните его как game.fgh. Откройте его в файловом менеджере и выберите FGH Engine. Если приложение не появляется, используйте «Поделиться» или переименуйте файл с расширением .fgh.

СТАТУС
Это ранний прототип: фон, текст и простые 2D-фигуры работают; режим 3D показывает вращающийся демонстрационный куб. Физика, сложные события, пользовательские действия и полноценная 3D-сцена пока не реализованы.
"""

class MainActivity : ComponentActivity() {
    private var gameText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameText = readGame(intent)
        setContent {
            MaterialTheme {
                val createDocument = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("text/plain")
                ) { uri ->
                    if (uri != null) runCatching {
                        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(DOCS) }
                    }
                }
                var splash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(1000); splash = false }
                val source = gameText
                if (source != null) GameScreen(source)
                else if (splash) {
                    Box(Modifier.fillMaxSize().background(Sky), contentAlignment = Alignment.Center) {
                        Text("FGH Engine", color = Blue, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(Modifier.fillMaxSize().background(Sky), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { createDocument.launch("FGH-документация.txt") },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                        ) { Text("Скачать документацию", color = Color.White, fontSize = 17.sp) }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        gameText = readGame(intent)
    }

    private fun readGame(intent: Intent?): String? {
        if (intent == null) return null
        val uri: Uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data ?: return null
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri) ?: return null
            }
            else -> return null
        }
        return runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(500_000) }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}

@Composable
private fun GameScreen(source: String) {
    val html = remember(source) { renderFgh(source) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
    )
}

private fun escapeHtml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
private fun validColor(value: String, fallback: String): String = if (Regex("^#[0-9a-fA-F]{6}$").matches(value)) value else fallback

private fun renderFgh(source: String): String {
    var bg = "#DDF3FF"
    var title = "Моя игра"
    var header = ""
    var cube = ""
    val shapes = StringBuilder()
    source.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("//") }.forEach { line ->
        when {
            line.startsWith("игра ") -> Regex("игра\\s+\"(.*)\"").find(line)?.groupValues?.get(1)?.let { title = it }
            line.startsWith("фон ") -> bg = validColor(line.substringAfter("фон ").trim(), bg)
            line.startsWith("текст ") -> Regex("текст\\s+\"(.*)\"").find(line)?.groupValues?.get(1)?.let { header = it }
            line.startsWith("круг ") -> {
                val m = Regex("круг\\s+\\S+\\s+цвет\\s+(#[0-9a-fA-F]{6})\\s+позиция\\s+(-?\\d+)\\s+(-?\\d+)\\s+размер\\s+(\\d+)").find(line)
                if (m != null) shapes.append("<div style='position:absolute;left:${m.groupValues[2]}px;top:${m.groupValues[3]}px;width:${m.groupValues[4]}px;height:${m.groupValues[4]}px;border-radius:50%;background:${m.groupValues[1]}'></div>")
            }
            line.startsWith("прямоугольник ") -> {
                val m = Regex("прямоугольник\\s+\\S+\\s+цвет\\s+(#[0-9a-fA-F]{6})\\s+позиция\\s+(-?\\d+)\\s+(-?\\d+)\\s+размер\\s+(\\d+)\\s+(\\d+)").find(line)
                if (m != null) shapes.append("<div style='position:absolute;left:${m.groupValues[2]}px;top:${m.groupValues[3]}px;width:${m.groupValues[4]}px;height:${m.groupValues[5]}px;background:${m.groupValues[1]};border-radius:5px'></div>")
            }
            line.startsWith("куб ") -> {
                val m = Regex("куб\\s+\\S+\\s+цвет\\s+(#[0-9a-fA-F]{6})").find(line)
                val c = m?.groupValues?.get(1) ?: "#58BFFF"
                cube = "<div style='position:absolute;left:50%;top:50%;width:90px;height:90px;margin:-45px;background:$c;border:2px solid white;animation:spin 5s linear infinite;box-shadow:18px 18px 0 #247eae;'></div>"
            }
        }
    }
    val safeTitle = escapeHtml(title)
    val safeHeader = escapeHtml(header)
    return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;font-family:Arial,sans-serif;background:$bg;color:#16435c}.stage{position:relative;width:100%;height:100%;overflow:hidden;background:$bg}.title{text-align:center;padding:22px 12px;font-size:23px;font-weight:bold;overflow-wrap:anywhere}.caption{text-align:center;padding:4px 10px;font-size:18px;overflow-wrap:anywhere}@keyframes spin{from{transform:rotate(0)}to{transform:rotate(360deg)}}</style></head><body><main class='stage'><div class='title'>$safeTitle</div><div class='caption'>$safeHeader</div>$shapes$cube</main></body></html>"
}
