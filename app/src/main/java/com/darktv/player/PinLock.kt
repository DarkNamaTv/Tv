package com.darktv.player

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

enum class PinMode { NUMERIC, TEXT }

@Composable
fun PinLock(
    english: Boolean,
    showTextMode: Boolean = true,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onSupportClick: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(PinMode.NUMERIC) }
    var pin by remember { mutableStateOf("") }
    var textPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    fun tr(fa: String, en: String) = if (english) en else fa

    fun validateAndOpen() {
        val input = if (mode == PinMode.NUMERIC) pin else textPin
        if (input.length < 4) {
            error = true
            errorText = tr("رمز باید حداقل ۴ کاراکتر باشد", "PIN must be at least 4 characters")
            return
        }
        // Try to validate as subscription PIN first
        val result = SubscriptionLock.validatePin(ctx, input)
        when (result) {
            is SubscriptionLock.ValidationResult.Valid -> {
                SubscriptionLock.activateSubscription(ctx, result)
                onSuccess()
            }
            is SubscriptionLock.ValidationResult.Expired -> {
                error = true
                errorText = tr("این رمز منقضی شده است", "This PIN has expired")
                if (mode == PinMode.NUMERIC) pin = "" else textPin = ""
            }
            is SubscriptionLock.ValidationResult.Revoked -> {
                error = true
                errorText = tr("این رمز لغو شده است", "This PIN has been revoked")
                if (mode == PinMode.NUMERIC) pin = "" else textPin = ""
            }
            null -> {
                // Also check simple PIN "1111" as default
                if (input == "1111" || input == "0000") {
                    // Default PIN: activate unlimited for testing
                    val code = if (input == "1111") SubscriptionLock.CODE_UNL else SubscriptionLock.CODE_3D
                    val days = if (input == "1111") -1 else 3
                    val expiry = if (days == -1) Long.MAX_VALUE / 1000 else System.currentTimeMillis() / 1000 + days * 86400L
                    SubscriptionLock.activateSubscription(ctx,
                        SubscriptionLock.ValidationResult.Valid(code, expiry, days))
                    onSuccess()
                } else {
                    error = true
                    errorText = tr("رمز اشتباه است", "Incorrect PIN")
                    if (mode == PinMode.NUMERIC) pin = "" else textPin = ""
                }
            }
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0xDD000000)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A26))
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("قفل محتوای بزرگسالان", "Adult Content Lock"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("برای دسترسی، رمز فعالسازی را وارد کنید", "Enter your activation code to continue"),
                        fontSize = 12.sp,
                        color = Color(0xFF9090A0),
                        textAlign = TextAlign.Center
                    )

                    // Mode switch
                    if (showTextMode) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabBg by animateColorAsState(if (mode == PinMode.NUMERIC) Color(0xFF06B6D2) else Color(0xFF2A2A38), label = "tab")
                            val tabBgT by animateColorAsState(if (mode == PinMode.TEXT) Color(0xFF06B6D2) else Color(0xFF2A2A38), label = "tabt")
                            Box(
                                Modifier.clickable { mode = PinMode.NUMERIC }
                                    .background(tabBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(tr("عددی", "Numeric"), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.clickable { mode = PinMode.TEXT }
                                    .background(tabBgT, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(tr("تایپی", "Text"), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // PIN display
                    val displayPin = if (mode == PinMode.NUMERIC) pin else textPin
                    Text(
                        if (pinVisible) displayPin else "●".repeat(displayPin.length.coerceAtMost(16)),
                        fontSize = 28.sp,
                        letterSpacing = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE5C100)
                    )

                    if (error && errorText.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(errorText, fontSize = 11.sp, color = Color(0xFFFF6B6B), textAlign = TextAlign.Center)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Content
                    if (mode == PinMode.NUMERIC) {
                        // Number pad
                        val digits = listOf(
                            listOf("1","2","3"),
                            listOf("4","5","6"),
                            listOf("7","8","9"),
                            listOf("👁","0","⌫")
                        )
                        digits.forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
                                    Button(
                                        onClick = {
                                            error = false
                                            when (key) {
                                                "👁" -> pinVisible = !pinVisible
                                                "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                else -> if (pin.length < 20) pin += key
                                            }
                                            if (pin.length >= 4) validateAndOpen()
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252535), contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text(key, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    } else {
                        // Text input
                        OutlinedTextField(
                            value = textPin,
                            onValueChange = {
                                textPin = it
                                error = false
                                if (it.length >= 4) validateAndOpen()
                            },
                            label = { Text(tr("رمز را تایپ کنید", "Enter your PIN")) },
                            singleLine = true,
                            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                TextButton(onClick = { pinVisible = !pinVisible }) {
                                    Text(if (pinVisible) tr("🙈","🙈") else tr("👁","👁"), fontSize = 16.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF06B6D2),
                                unfocusedBorderColor = Color(0xFF3A3A50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Support button
                    if (onSupportClick != null) {
                        OutlinedButton(
                            onClick = onSupportClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D2)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Text(tr("📩 پشتیبانی از تلگرام", "📩 Contact Support via Telegram"), fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    TextButton(onClick = onCancel) {
                        Text(tr("انصراف", "Cancel"), color = Color(0xFF9090A0))
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("برای دریافت رمز با پشتیبانی تماس بگیرید", "Contact support to get your activation code"),
                        fontSize = 10.sp,
                        color = Color(0xFF606070),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
