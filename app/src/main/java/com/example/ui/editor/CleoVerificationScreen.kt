package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CleoVerificationScreen(
  statusMessage: String,
  subMessage: String = ""
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("verification_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Bola giratoria azul limpia
      CircularProgressIndicator(
        color = Color(0xFF1976D2),
        strokeWidth = 3.5.dp,
        modifier = Modifier
          .size(48.dp)
          .testTag("verification_spinner")
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Mensaje de lo que el sistema está haciendo
      Text(
        text = statusMessage,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1E293B),
        modifier = Modifier.testTag("verification_status_text")
      )

      if (subMessage.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = subMessage,
          fontSize = 12.sp,
          color = Color(0xFF64748B)
        )
      }
    }
  }
}
