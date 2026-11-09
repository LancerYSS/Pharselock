package com.denie.phraselock

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PhraseScheduler.isEnabled(this)) {
            NotificationHelper.showCurrentPhrase(this)
        }
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PhraseLockRoot()
                }
            }
        }
    }
}

@Composable
fun PhraseLockRoot() {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("PhraseLock") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Сегодня") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Каталог") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> TodayScreen()
                1 -> CatalogScreen()
                else -> SettingsScreen()
            }
        }
    }
}

/* ---------------------------------- Сегодня ---------------------------------- */

@Composable
fun TodayScreen() {
    val context = LocalContext.current
    val favorites = remember { FavoritesStore(context) }
    val phrase = remember { PhraseRepository.phraseForHour(context, System.currentTimeMillis()) }
    var revision by remember { mutableStateOf(0) }
    val isKnown = revision.let { favorites.isKnown(phrase.id) }
    val isFavorite = revision.let { favorites.isFavorite(phrase.id) }
    val titles = remember { PhraseRepository.categoryTitles(context) }
    val categoryMeta = titles[phrase.category] ?: ("" to phrase.category)

    val nextHourText = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ФРАЗА ЧАСА",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("${categoryMeta.first} ${categoryMeta.second}".trim()) })
                    AssistChip(onClick = {}, label = { Text(phrase.level) })
                }
                Text(
                    text = phrase.en,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Divider()
                Text(
                    text = phrase.ru,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    favorites.toggleKnown(phrase.id)
                    revision++
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isKnown) "Выучено" else "Знаю")
            }
            OutlinedButton(
                onClick = {
                    favorites.toggleFavorite(phrase.id)
                    revision++
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isFavorite) "В избранном" else "В избранное")
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Новая фраза в $nextHourText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (PhraseScheduler.isEnabled(context)) {
                "Эта же фраза сейчас висит в уведомлении — на экране блокировки и в шторке."
            } else {
                "Включите уведомление во вкладке «Настройки» — и фраза появится на экране блокировки."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* --------------------------------- Каталог --------------------------------- */

@Composable
fun CatalogScreen() {
    val context = LocalContext.current
    val favorites = remember { FavoritesStore(context) }
    var revision by remember { mutableStateOf(0) }

    val all = remember { PhraseRepository.phrases(context) }
    val titles = remember { PhraseRepository.categoryTitles(context) }

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }

    val visible = revision.let {
        all.filter { p ->
            (selectedCategory == null || p.category == selectedCategory) &&
                (!onlyFavorites || favorites.isFavorite(p.id)) &&
                (query.isBlank() ||
                    p.en.lowercase().contains(query.lowercase()) ||
                    p.ru.lowercase().contains(query.lowercase()))
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск по-английски или по-русски") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Все") }
            )
            titles.forEach { (id, meta) ->
                FilterChip(
                    selected = selectedCategory == id,
                    onClick = { selectedCategory = if (selectedCategory == id) null else id },
                    label = { Text(meta.second) }
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${visible.size} фраз", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Только избранное", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                Switch(checked = onlyFavorites, onCheckedChange = { onlyFavorites = it })
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(visible, key = { it.id }) { phrase ->
                PhraseRow(
                    phrase = phrase,
                    isFavorite = favorites.isFavorite(phrase.id),
                    isKnown = favorites.isKnown(phrase.id),
                    onToggleFavorite = {
                        favorites.toggleFavorite(phrase.id)
                        revision++
                    },
                    onToggleKnown = {
                        favorites.toggleKnown(phrase.id)
                        revision++
                    }
                )
            }
        }
    }
}

@Composable
fun PhraseRow(
    phrase: Phrase,
    isFavorite: Boolean,
    isKnown: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleKnown: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(phrase.en, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(phrase.ru, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    phrase.level,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isKnown) {
                    Text(
                        "выучено",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        IconButton(onClick = onToggleKnown) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = if (isKnown) "Выучено" else "Отметить выученной",
                tint = if (isKnown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
            )
        }
    }
    Divider()
}

/* --------------------------------- Настройки --------------------------------- */

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(PhraseScheduler.isEnabled(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        enabled = granted
        PhraseScheduler.setEnabled(context, granted)
        if (granted) PhraseScheduler.enable(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Уведомление с фразой", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Постоянно висит в шторке и на экране блокировки. Фраза обновляется раз в час.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = { want ->
                    if (want) {
                        if (NotificationHelper.canPostNotifications(context)) {
                            PhraseScheduler.setEnabled(context, true)
                            PhraseScheduler.enable(context)
                            enabled = true
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        PhraseScheduler.setEnabled(context, false)
                        PhraseScheduler.disable(context)
                        enabled = false
                    }
                }
            )
        }

        Divider()

        Text("О приложении", style = MaterialTheme.typography.titleSmall)
        Text("PhraseLock 1.0 · 400 фраз · уровни A1–B2")
        Text(
            "Как установить APK: Настройки → Приложения → Специальный доступ → " +
                "Установка неизвестных приложений → разрешите для браузера или файлового менеджера, " +
                "которым открываете файл, затем откройте скачанный app-debug.apk.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Данные (избранное, «выучено») хранятся только на этом устройстве. " +
                "Фраза часа подбирается из текущего часа — одинаково всегда, без интернета.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
