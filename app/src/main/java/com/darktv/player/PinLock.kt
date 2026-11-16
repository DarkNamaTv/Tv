package com.darktv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * PIN lock overlay for Movies section (adult content protection).
 * The default PIN is 1111. Currently stored in plaintext — can be upgraded
 * to EncryptedSharedPreferences / Keystore later.
 */
@Composable
fun PinLock(english: Boolean, onSuccess: () -> Unit, onCancel: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val correctPin = "1111"

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0xCC000000)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28))
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒", fontSize = 40.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (english) "Adult Content Lock" else "قفل محتوای بزرگسالان",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (english) "Please enter the 4-digit PIN to continue."
                        else "برای ادامه، رمز ۴ رقمی را وارد کنید.",
                        fontSize = 13.sp,
                        color = Color(0xFFB0B0C0),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (pin.isEmpty()) "••••" else pin,
                        fontSize = 36.sp,
                        letterSpacing = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE5C100)
                    )
                    Spacer(Modifier.height(12.dp))

                    // Number pad 1-9
                    val digits = listOf(
                        listOf("1","2","3"),
                        listOf("4","5","6"),
                        listOf("7","8","9"),
                        listOf("C","0","⌫")
                    )
                    digits.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { key ->
                                Button(
                                    onClick = {
                                        when (key) {
                                            "C" -> pin = ""
                                            "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            else -> if (pin.length < 4) pin += key
                                        }
                                        if (pin.length == 4) {
                                            if (pin == correctPin) {
                                                onSuccess()
                                            } else {
                                                pin = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2A2A38),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(key, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onCancel) {
                        Text(
                            if (english) "Cancel" else "انصراف",
                            color = Color(0xFFB0B0C0),
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        if (english) "Default PIN: 1111" else "رمز پیش‌فرض: ۱۱۱۱",
                        fontSize = 11.sp,
                        color = Color(0xFF70707E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
