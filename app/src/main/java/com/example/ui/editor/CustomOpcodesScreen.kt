package com.example.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compiler.CleoOpcodeDatabase
import com.example.i18n.LocalAppStrings

@Composable
fun CustomOpcodesScreen(
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  var editorState by remember { mutableStateOf(TextFieldValue("")) }

  LaunchedEffect(Unit) {
    editorState = TextFieldValue(CleoOpcodeDatabase.getCustomOpcodesRawText(context))
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("custom_opcodes_screen")
  ) {
    // Cabecera limpia
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("custom_opcodes_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = strings.btnBack,
          tint = Color(0xFF64748B)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Box(
        modifier = Modifier
          .size(34.dp)
          .background(Color(0xFFEFF6FF), shape = RoundedCornerShape(17.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Build,
          contentDescription = strings.customScreenTitle,
          tint = Color(0xFF1976D2),
          modifier = Modifier.size(17.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = strings.customScreenTitle,
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF1E293B)
        )
        Text(
          text = strings.customScreenSubtitle,
          fontSize = 11.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }

    // Editor completo
    CleoCodeEditor(
      code = editorState,
      onCodeChange = { editorState = it },
      errorLine = null,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    )

    // Botón azul pequeño con bordes redondeados que dice "Ejecutar"
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color.White)
        .padding(horizontal = 16.dp, vertical = 18.dp),
      contentAlignment = Alignment.Center
    ) {
      Button(
        onClick = {
          val (success, message) = CleoOpcodeDatabase.saveAndExecuteCustomOpcodes(
            context = context,
            rawInput = editorState.text
          )
          Toast.makeText(context, message, Toast.LENGTH_LONG).show()
          if (success) {
            onBack()
          }
        },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF1976D2),
          contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
        modifier = Modifier.testTag("execute_custom_opcodes_button")
      ) {
        Text(
          text = strings.btnExecute,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
