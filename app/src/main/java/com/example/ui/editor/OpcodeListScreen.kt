package com.example.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compiler.CleoOpcodeDatabase
import com.example.compiler.OpcodeDef
import com.example.i18n.LocalAppStrings

@Composable
fun OpcodeListScreen(
  onClose: () -> Unit,
  onOpenCustomOpcodes: () -> Unit,
  onSelectOpcode: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  val clipboardManager = LocalClipboardManager.current
  var searchQuery by remember { mutableStateOf("") }

  val allOpcodes = remember { CleoOpcodeDatabase.getAll().sortedBy { it.hexString } }

  val filteredOpcodes = remember(searchQuery, allOpcodes) {
    if (searchQuery.isBlank()) {
      allOpcodes
    } else {
      val query = searchQuery.trim().lowercase()
      allOpcodes.filter {
        it.hexString.lowercase().contains(query) ||
          it.commandName.lowercase().contains(query) ||
          it.description.lowercase().contains(query)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding()
      .pointerInput(Unit) {
        detectHorizontalDragGestures { _, dragAmount ->
          if (dragAmount > 35) {
            onClose()
          }
        }
      }
      .testTag("opcode_list_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Cabecera superior limpia: Título arriba pequeño "Lista Opcodes" + Icono a la derecha
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier.size(38.dp).testTag("close_opcode_list_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = strings.btnBack,
            tint = Color(0xFF64748B)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = strings.opcodesListTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
          )
          Text(
            text = "${filteredOpcodes.size} ${strings.opcodesAvailableSuffix}",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
          )
        }

        // Icono a la derecha para agregar / gestionar opcodes propios
        IconButton(
          onClick = onOpenCustomOpcodes,
          modifier = Modifier.size(36.dp).testTag("header_custom_opcodes_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = strings.customActionTitle,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Barra de búsqueda minimalista sin adornos
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Buscar",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(
              fontSize = 13.sp,
              color = Color(0xFF1E293B)
            ),
            cursorBrush = SolidColor(Color(0xFF1976D2)),
            modifier = Modifier
              .weight(1f)
              .testTag("opcode_search_input"),
            decorationBox = { innerTextField ->
              if (searchQuery.isEmpty()) {
                Text(
                  text = strings.searchOpcodeHint,
                  fontSize = 13.sp,
                  color = Color(0xFF94A3B8)
                )
              }
              innerTextField()
            }
          )
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { searchQuery = "" },
              modifier = Modifier.size(18.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Limpiar",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Lista limpia de opcodes
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("opcode_lazy_column")
      ) {
        // Primera opción: Personalizados
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onOpenCustomOpcodes)
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("opcode_item_custom_action")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = strings.customActionTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1976D2)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.customActionTag,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = strings.customActionDesc,
              fontSize = 12.sp,
              color = Color(0xFF64748B)
            )
          }
          HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }

        // Resto de los opcodes
        items(filteredOpcodes, key = { "${it.hexString}_${it.isCustom}_${it.category}" }) { def ->
          OpcodeListItem(
            def = def,
            onClick = {
              val sample = def.example.ifBlank { "${def.hexString}: ${def.commandName}" }
              clipboardManager.setText(AnnotatedString(sample))
              Toast.makeText(context, "${strings.toastCopied}: $sample", Toast.LENGTH_SHORT).show()
              onSelectOpcode?.invoke(sample)
            }
          )
          HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFFF1F5F9),
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun OpcodeListItem(
  def: OpcodeDef,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 11.dp)
      .testTag("opcode_item_${def.hexString}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = def.hexString,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (def.isCustom) Color(0xFF16A34A) else Color(0xFF1976D2)
      )

      Text(
        text = ": ${def.commandName}",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1E293B)
      )
    }

    if (def.description.isNotBlank()) {
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = def.description,
        fontSize = 12.sp,
        color = Color(0xFF64748B),
        lineHeight = 16.sp
      )
    }
  }
}
