package com.sam.gogotimeline.ui.timeline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sam.gogotimeline.core.time.CalendarContext
import java.time.ZoneId
import kotlinx.coroutines.delay

/** Device time zone and locale; a new context is created when the time zone changes while the app is open. */
@Composable
fun rememberCalendarContext(): CalendarContext {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var zone by remember { mutableStateOf(ZoneId.systemDefault()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                zone = ZoneId.systemDefault()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return remember(zone, locale) { CalendarContext.of(zone, locale) }
}

/**
 * Current time, refreshed at the start of every minute while the app is in the foreground and right
 * away when it returns. Read it while drawing so only the "now" line redraws.
 */
@Composable
fun rememberNow(): State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(System.currentTimeMillis(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = System.currentTimeMillis()
                delay(MINUTE_MS - System.currentTimeMillis() % MINUTE_MS)
            }
        }
    }
}

private const val MINUTE_MS = 60_000L
