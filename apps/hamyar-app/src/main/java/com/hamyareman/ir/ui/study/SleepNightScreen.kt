package com.hamyareman.ir.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.playback.PlaybackController
import com.hamyareman.ir.platform.feature.playback.SleepPlaybackService
import com.hamyareman.ir.ui.AppTypography
import kotlinx.coroutines.launch

object SleepLaunch {
    @Volatile var pending: Boolean = false
}

data class SleepTrack(val id: String, val title: String, val subtitle: String, val uri: String)

@Composable
fun SleepNightScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val playback = remember { PlaybackController(ctx, SleepPlaybackService::class.java) }
    val state by playback.state.collectAsState()
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        onDispose { playback.release() }
    }
    LaunchedEffect(Unit) { playback.connect() }

    val audioTracks = remember {
        listOf(
            SleepTrack("h1", "آرام‌سازی بدن", "خودهیپنوتیزم ملایم — فایل بعداً در باکت", ""),
            SleepTrack("h2", "موسیقی خواب", "صدای آرام شب — فایل بعداً در باکت", ""),
        )
    }
    val stories = remember {
        listOf(
            SleepTrack("s1", "قصهٔ جنگل آرام", "حدود ۱۲ دقیقه — نمایشی تا جدول سرور", ""),
            SleepTrack("s2", "سفر روی ابرها", "حدود ۸ دقیقه — نمایشی تا باکت", ""),
        )
    }

    fun play(t: SleepTrack) {
        if (t.uri.isBlank()) return
        scope.launch {
            playback.connect()
            playback.setMedia(t.uri, t.title)
            playback.play()
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آرامش قبل خواب", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("صوت خواب", "قصهٔ شبانه", "تنفس").forEachIndexed { i, l ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l, fontFamily = AppTypography.body, fontSize = AppTypography.bump(AppTypography.body, 14)) })
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (tab) {
                0 -> audioTracks.forEach { TrackRow(it, state.playing) { play(it) } }
                1 -> stories.forEach { TrackRow(it, state.playing) { play(it) } }
                else -> {
                    Text("تمرین دم‌بازدم — فهرست بعداً می‌آید.", fontFamily = AppTypography.body, fontSize = AppTypography.bump(AppTypography.body, 14))
                    Text("اسکلت خالی: SVG + متن + صوت + شمارش در نسخهٔ بعد.", fontFamily = AppTypography.body, fontSize = AppTypography.bump(AppTypography.body, 14), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (state.playing) playback.pause() else playback.play() }, modifier = Modifier.weight(1f)) {
                Text(if (state.playing) "استوپ" else "پلی", fontFamily = AppTypography.body, fontSize = AppTypography.bump(AppTypography.body, 14))
            }
            OutlinedButton(onClick = { playback.stop() }, modifier = Modifier.weight(1f)) {
                Text("بستن", fontFamily = AppTypography.body)
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = AppTypography.bump(AppTypography.body, 12), modifier = Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
private fun TrackRow(t: SleepTrack, playing: Boolean, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = t.uri.isNotBlank(), onClick = onPlay)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title, fontFamily = AppTypography.greeting, fontSize = AppTypography.bump(AppTypography.greeting, 18))
            Text(t.subtitle, fontFamily = AppTypography.body, fontSize = AppTypography.bump(AppTypography.body, 14), style = MaterialTheme.typography.bodySmall)
            if (t.uri.isBlank()) {
                Text("فایل هنوز روی سرور نیست.", fontFamily = AppTypography.body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = AppTypography.bump(AppTypography.body, 12))
            }
        }
    }
}
