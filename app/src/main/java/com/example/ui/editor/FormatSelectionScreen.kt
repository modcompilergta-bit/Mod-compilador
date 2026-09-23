package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.LocalAppStrings

enum class CleoScriptFormat(val extension: String, val label: String, val subtitle: String) {
  CSA("csa", "CLEO SA (.csa)", "Script autónomo para GTA San Andreas Android"),
  CS("cs", "CLEO Script (.cs)", "Script estándar de CLEO")
}

@Composable
fun FormatSelectionScreen(
  onFormatSelected: (CleoScriptFormat) -> Unit,
  onBack: () -> Unit
) {
  val strings = LocalAppStrings.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("format_selection_screen")
  ) {
    // Botón sutil para regresar al editor
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .padding(12.dp)
        .align(Alignment.TopStart)
        .testTag("back_to_editor_button")
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = strings.btnBack,
        tint = Color(0xFF64748B)
      )
    }

    // Dos opciones limpias organizadas en la mitad de la pantalla
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = strings.selectFormatTitle,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1E293B),
        modifier = Modifier.padding(bottom = 24.dp)
      )

      // Opción 1: .csa
      FormatOptionCard(
        title = ".CSA",
        description = strings.csaTitle,
        subDescription = strings.csaDesc,
        testTag = "format_option_csa",
        onClick = { onFormatSelected(CleoScriptFormat.CSA) }
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Opción 2: .cs
      FormatOptionCard(
        title = ".CS",
        description = strings.csiTitle,
        subDescription = strings.csiDesc,
        testTag = "format_option_cs",
        onClick = { onFormatSelected(CleoScriptFormat.CS) }
      )
    }
  }
}

@Composable
private fun FormatOptionCard(
  title: String,
  description: String,
  subDescription: String,
  testTag: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, shape = RoundedCornerShape(16.dp))
      .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 18.dp)
      .testTag(testTag)
  ) {
    Column {
      Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1976D2)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = description,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1E293B)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subDescription,
        fontSize = 11.sp,
        color = Color(0xFF64748B)
      )
    }
  }
}
