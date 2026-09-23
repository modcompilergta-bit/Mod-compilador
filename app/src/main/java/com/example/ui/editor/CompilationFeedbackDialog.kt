package com.example.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.compiler.CompilationResult

@Composable
fun CompilationFeedbackDialog(
  result: CompilationResult,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
        .testTag("compilation_result_dialog")
    ) {
      when (result) {
        is CompilationResult.Failure -> {
          val error = result.error
          Column(
            modifier = Modifier.padding(24.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color(0xFFDC2626)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Error en línea ${error.line}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Línea de código errónea
            if (error.rawLine.isNotBlank()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp))
                  .border(1.dp, Color(0xFFFCA5A5), shape = RoundedCornerShape(8.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp)
              ) {
                Text(
                  text = error.rawLine.trim(),
                  fontFamily = FontFamily.Monospace,
                  fontSize = 13.sp,
                  color = Color(0xFF991B1B)
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
            }

            // Explicación limpia del problema
            Text(
              text = error.message,
              fontSize = 14.sp,
              lineHeight = 20.sp,
              color = Color(0xFF334155)
            )

            // Sugerencia
            if (error.suggestion != null) {
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = error.suggestion,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
              ),
              modifier = Modifier
                .align(Alignment.End)
                .testTag("dialog_dismiss_button")
            ) {
              Text(text = "Entendido")
            }
          }
        }

        is CompilationResult.Success -> {
          Column(
            modifier = Modifier.padding(24.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Éxito",
                tint = Color(0xFF16A34A)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Compilación exitosa",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16A34A)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "${result.opcodesCompiled} opcodes compilados correctamente (${result.sizeBytes} bytes generados).",
              fontSize = 14.sp,
              color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Visor de bytecode hexadecimal limpio
            Text(
              text = "Bytecode CLEO (Hex):",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
            ) {
              val hexScroll = rememberScrollState()
              Text(
                text = result.hexDump,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A),
                modifier = Modifier.horizontalScroll(hexScroll)
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              OutlinedButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("CLEO Bytecode", result.hexDump)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "Hex copiado al portapapeles", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("copy_hex_button")
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copiar",
                  tint = Color(0xFF1976D2),
                  modifier = Modifier.padding(end = 4.dp)
                )
                Text(text = "Copiar Hex", color = Color(0xFF1976D2), fontSize = 13.sp)
              }

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF1976D2),
                  contentColor = Color.White
                ),
                modifier = Modifier.testTag("dialog_dismiss_button")
              ) {
                Text(text = "Aceptar", fontSize = 13.sp)
              }
            }
          }
        }
      }
    }
  }
}
