package com.erns.alertauni.screen.post

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erns.alertauni.ui.theme.MyMutedForegroundColor

/*
 * Marca visual para diferenciar las operaciones SIMULADAS de las integradas con el backend:
 * el telefono indica que el dato se guarda solo en este dispositivo (archivados, comentarios
 * cerrados, borradores). Lo que no lleva marca (publicar, enviar privado, prioridad,
 * listar anuncios, comentarios) va y viene del backend.
 */


@Composable
fun LocalOnlyNote(text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            tint = MyMutedForegroundColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = MyMutedForegroundColor)
    }
}
