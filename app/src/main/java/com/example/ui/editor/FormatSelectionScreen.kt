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

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

enum class CleoScriptFormat(val extension: String, val label: String, val subtitle: String) {
  CSA("csa", "CLEO SA (.csa)", "Script autónomo para GTA San Andreas Android"),
  CSI("csi", "CLEO Invoked (.csi)", "Script de invocación táctil / animación CLEO Android");

  companion object {
    @JvmField
    val CS = CSI
  }
}

@Composable
fun FormatSelectionScreen(
  initialScriptName: String = "",
  onFormatSelected: (CleoScriptFormat, String) -> Unit,
  onBack: () -> Unit
) {
  val strings = LocalAppStrings.current
  val focusManager = LocalFocusManager.current
  var scriptNameInput by remember { mutableStateOf(initialScriptName.substringBeforeLast(".")) }

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
        modifier = Modifier.padding(bottom = 16.dp)
      )

      // Campo para nombre del script (deducido automáticamente de lo que lee o editable por el usuario)
      OutlinedTextField(
        value = scriptNameInput,
        onValueChange = { scriptNameInput = it.replace(Regex("[^A-Za-z0-9_\\-]"), "") },
        label = { Text(strings.scriptNameLabel, fontSize = 12.sp) },
        placeholder = { Text(strings.scriptNameHint, fontSize = 12.sp, color = Color(0xFF94A3B8)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFF1976D2),
          unfocusedBorderColor = Color(0xFFCBD5E1),
          focusedLabelColor = Color(0xFF1976D2)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("script_name_input")
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Opción 1: .csa
      FormatOptionCard(
        title = ".CSA",
        description = strings.csaTitle,
        subDescription = strings.csaDesc,
        testTag = "format_option_csa",
        onClick = {
          val finalName = scriptNameInput.trim().ifEmpty { initialScriptName.substringBeforeLast(".") }
          onFormatSelected(CleoScriptFormat.CSA, finalName)
        }
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Opción 2: .csi
      FormatOptionCard(
        title = ".CSI",
        description = strings.csiTitle,
        subDescription = strings.csiDesc,
        testTag = "format_option_csi",
        onClick = {
          val finalName = scriptNameInput.trim().ifEmpty { initialScriptName.substringBeforeLast(".") }
          onFormatSelected(CleoScriptFormat.CSI, finalName)
        }
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
