package com.example.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.HorizontalDivider
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
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings

@Composable
fun AppMenuScreen(
  currentLanguage: AppLanguage,
  onLanguageChanged: (AppLanguage) -> Unit,
  onOpenCustomOpcodes: () -> Unit,
  onOpenOpcodeList: () -> Unit,
  onClose: () -> Unit
) {
  val strings = LocalAppStrings.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("app_menu_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Cabecera limpia
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier.size(38.dp).testTag("menu_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = strings.btnBack,
            tint = Color(0xFF64748B)
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
          text = strings.menuTitle,
          fontSize = 17.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF1E293B)
        )
      }

      HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))

      // Lista limpia de opciones: una bajo la otra sin desastres
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        // Opción 1: Personalizado (o Personalizar)
        MenuItemRow(
          title = strings.menuCustomOption,
          subtitle = strings.menuCustomDesc,
          icon = Icons.Default.Build,
          onClick = {
            onClose()
            onOpenCustomOpcodes()
          },
          testTag = "menu_item_custom"
        )

        HorizontalDivider(
          thickness = 0.5.dp,
          color = Color(0xFFF1F5F9),
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Opción 2: Inglés / Español
        val isEnglish = currentLanguage == AppLanguage.ENGLISH
        MenuItemRow(
          title = strings.menuLanguageOption,
          subtitle = strings.menuLanguageDesc,
          icon = Icons.Default.Language,
          onClick = {
            val newLang = if (isEnglish) AppLanguage.SPANISH else AppLanguage.ENGLISH
            onLanguageChanged(newLang)
          },
          testTag = "menu_item_language"
        )

        HorizontalDivider(
          thickness = 0.5.dp,
          color = Color(0xFFF1F5F9),
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Opción 3: Lista Opcodes
        MenuItemRow(
          title = strings.menuOpcodesOption,
          subtitle = strings.menuOpcodesDesc,
          icon = Icons.AutoMirrored.Filled.List,
          onClick = {
            onClose()
            onOpenOpcodeList()
          },
          testTag = "menu_item_opcodes"
        )
      }
    }
  }
}

@Composable
private fun MenuItemRow(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 13.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .background(Color(0xFFF1F5F9), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = Color(0xFF1976D2),
        modifier = Modifier.size(18.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1E293B)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        fontSize = 11.sp,
        color = Color(0xFF64748B),
        lineHeight = 15.sp
      )
    }
  }
}
