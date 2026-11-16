package com.darktv.player

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

object PinLock {
    private const val PREFS = "pin_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_ENABLED = "pin_enabled"
    private const val DEFAULT_PIN = "0000"

    // Simple hash: just base64 encode (not real security, but deters casual access)
    private fun hash(pin: String) = android.util.Base64.encodeToString(
        pin.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
    )

    private fun SharedPreferences.getPinHash() = getString(KEY_PIN_HASH, hash(DEFAULT_PIN)) ?: hash(DEFAULT_PIN)

    fun isPinEnabled(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PIN_ENABLED, false)
    }

    fun verifyPin(ctx: Context, pin: String): Boolean {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return hash(pin) == prefs.getPinHash()
    }

    fun setPin(ctx: Context, newPin: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN_HASH, hash(newPin)).apply()
    }

    fun setPinEnabled(ctx: Context, enabled: Boolean) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
    }
}

@Composable
fun PinLockDialog(
    english: Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    fun tr(fa: String, en: String) = if (english) en else fa

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try { focus.requestFocus() } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Lock, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    tr("ورود به بخش بزرگسالان", "Adult Content Access"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tr("رمز عددی خود را وارد کنید", "Enter your numeric PIN"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                // PIN display
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { idx ->
                        Box(
                            Modifier.size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (error) Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.5.dp,
                                    when {
                                        error -> Color(0xFFEF4444)
                                        idx < pin.length -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    },
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (idx < pin.length) {
                                    if (showPin) pin[idx].toString() else "●"
                                } else "",
                                color = if (error) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (error) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("رمز اشتباه!", "Wrong PIN!"),
                        color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Number pad
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        listOf("1","2","3"),
                        listOf("4","5","6"),
                        listOf("7","8","9"),
                        listOf("", "0", "⌫")
                    ).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(Modifier.size(64.dp))
                                } else if (key == "⌫") {
                                    Surface(
                                        onClick = {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                                error = false
                                            }
                                        },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Backspace, null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                } else {
                                    Surface(
                                        onClick = {
                                            if (pin.length < 4) {
                                                pin += key
                                                error = false
                                                if (pin.length == 4) {
                                                    if (PinLock.verifyPin(ctx, pin)) {
                                                        onSuccess()
                                                    } else {
                                                        error = true
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onCancel) {
                    Text(tr("انصراف", "Cancel"))
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    english: Boolean,
    onSetup: (String) -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) } // 0 = new pin, 1 = confirm
    var error by remember { mutableStateOf("") }

    fun tr(fa: String, en: String) = if (english) en else fa
    val current = if (step == 0) pin else confirm

    fun dotCount(p: String) = p.length

    @Composable fun NumberPad(onKey: (String) -> Unit, onBack: () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                listOf("1","2","3"),
                listOf("4","5","6"),
                listOf("7","8","9"),
                listOf("", "0", "⌫")
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(64.dp))
                        } else if (key == "⌫") {
                            Surface(
                                onClick = onBack,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape, modifier = Modifier.size(64.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Backspace, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                }
                            }
                        } else {
                            Surface(
                                onClick = { onKey(key) },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape, modifier = Modifier.size(64.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onKey(k: String) {
        error = ""
        if (step == 0) {
            if (pin.length < 4) pin += k
            if (pin.length == 4) step = 1
        } else {
            if (confirm.length < 4) confirm += k
            if (confirm.length == 4) {
                if (confirm == pin) onSetup(pin) else {
                    error = tr("تأیید رمز مطابقت ندارد!", "PINs do not match!")
                    confirm = ""
                }
            }
        }
    }

    fun onBack() {
        error = ""
        if (step == 1 && confirm.isNotEmpty()) {
            confirm = confirm.dropLast(1)
        } else if (step == 1 && confirm.isEmpty()) {
            step = 0
        } else if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    if (step == 0) tr("انتخاب رمز جدید","Set New PIN")
                    else tr("تأیید رمز","Confirm PIN"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (step == 0) tr("رمز ۴ رقمی جدید انتخاب کنید","Choose a new 4-digit PIN")
                    else tr("رمز را دوباره وارد کنید","Re-enter the PIN"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { idx ->
                        val v = if (step == 0) pin else confirm
                        Box(
                            Modifier.size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.5.dp,
                                    if (idx < v.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (idx < v.length) "●" else "",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))
                NumberPad(onKey = ::onKey, onBack = ::onBack)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onCancel) {
                    Text(tr("انصراف", "Cancel"))
                }
            }
        }
    }
}
