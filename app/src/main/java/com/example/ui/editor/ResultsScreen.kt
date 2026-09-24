package com.example.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compiler.CleoZipExporter
import com.example.compiler.GeneratedZipResult
import com.example.i18n.LocalAppStrings

@Composable
fun ResultsScreen(
  zipResult: GeneratedZipResult,
  onBackToEditor: () -> Unit
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  var isSavedToDownloads by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("results_screen")
  ) {
    // Botón de regreso
    IconButton(
      onClick = onBackToEditor,
      modifier = Modifier
        .padding(12.dp)
        .align(Alignment.TopStart)
        .testTag("results_back_button")
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = strings.btnBackToEditor,
        tint = Color(0xFF64748B)
      )
    }

    // Título arriba pequeño en letras verdes
    Text(
      text = strings.resultsTitle,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = Color(0xFF16A34A),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 20.dp)
        .testTag("results_title_text")
    )

    // Contenido central limpio y directo
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Tarjeta informativa del archivo generado con su nombre inteligente
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
          .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
          .padding(14.dp)
          .testTag("result_script_info_card"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(Color(0xFFE0F2FE), shape = RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = zipResult.scriptFormat,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0284C7)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = zipResult.scriptEntryName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
          )
          Text(
            text = "${zipResult.sizeBytes} bytes • ${zipResult.fileName}",
            fontSize = 11.sp,
            color = Color(0xFF64748B)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Botón principal: Descargar / Mover a Download
      Button(
        onClick = {
          val uri = CleoZipExporter.saveToDownloads(
            context = context,
            zipFile = zipResult.zipFile,
            displayName = zipResult.fileName
          )
          if (zipResult.scriptFile != null) {
            CleoZipExporter.saveScriptToDownloads(
              context = context,
              scriptFile = zipResult.scriptFile,
              displayName = zipResult.scriptEntryName
            )
          }
          if (uri != null) {
            isSavedToDownloads = true
            Toast.makeText(context, "${strings.toastSavedToDownload}: ${zipResult.scriptEntryName}", Toast.LENGTH_LONG).show()
          } else {
            Toast.makeText(context, strings.toastErrorSaving, Toast.LENGTH_SHORT).show()
          }
        },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isSavedToDownloads) Color(0xFF16A34A) else Color(0xFF1976D2),
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("download_zip_button")
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = strings.btnDownload,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isSavedToDownloads) strings.btnDownloaded else strings.btnDownload,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Botón secundario: Abrir / Compartir archivo ZIP
      OutlinedButton(
        onClick = {
          try {
            val shareIntent = CleoZipExporter.createShareIntent(context, zipResult.zipFile)
            context.startActivity(android.content.Intent.createChooser(shareIntent, strings.btnShare))
          } catch (e: Exception) {
            Toast.makeText(context, "${strings.toastErrorOpening}: ${e.message}", Toast.LENGTH_SHORT).show()
          }
        },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("share_zip_button")
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = strings.btnShare,
          tint = Color(0xFF1976D2),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = strings.btnShare,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF1976D2)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Opción limpia para volver al editor
      Button(
        onClick = onBackToEditor,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFF1F5F9),
          contentColor = Color(0xFF475569)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .testTag("back_editor_secondary_button")
      ) {
        Text(
          text = strings.btnBackToEditor,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
