package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compiler.CleoCompiler
import com.example.compiler.CleoOpcodeDatabase
import com.example.compiler.CleoZipExporter
import com.example.compiler.CompilationResult
import com.example.compiler.GeneratedZipResult
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.i18n.getStrings
import com.example.ui.editor.CleoCodeEditor
import com.example.ui.editor.CleoScriptFormat
import com.example.ui.editor.CleoVerificationScreen
import com.example.ui.editor.CompilationFeedbackDialog
import com.example.ui.editor.CustomOpcodesScreen
import com.example.ui.editor.FormatSelectionScreen
import com.example.ui.editor.OpcodeListScreen
import com.example.ui.editor.ResultsScreen
import com.example.ui.menu.AppMenuScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreenState {
  EDITOR,
  CUSTOM_OPCODES,
  VERIFYING,
  FORMAT_SELECTION,
  COMPILING_ZIP,
  RESULTS
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    CleoOpcodeDatabase.initializeAndSyncDatabase(this)
    setContent {
      MyApplicationTheme {
        HomeScreen()
      }
    }
  }
}

/**
 * Icono de nueve rayitas ordenadas en cuadrícula de 3x3.
 */
@Composable
fun NineLinesIcon(
  modifier: Modifier = Modifier,
  color: Color = Color(0xFF475569)
) {
  Box(
    modifier = modifier.size(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.size(17.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      repeat(3) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          repeat(3) {
            Box(
              modifier = Modifier
                .size(width = 4.dp, height = 2.4.dp)
                .background(color, shape = RoundedCornerShape(1.dp))
            )
          }
        }
      }
    }
  }
}

@Composable
fun HomeScreen() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var currentLanguage by remember { mutableStateOf(AppLanguage.SPANISH) }
  val strings = getStrings(currentLanguage)

  var currentScreen by remember { mutableStateOf(AppScreenState.EDITOR) }
  var showAppMenu by remember { mutableStateOf(false) }
  var showOpcodeList by remember { mutableStateOf(false) }

  var codeState by remember {
    mutableStateOf(
      TextFieldValue(
        "// Script CLEO GTA San Andreas Android\n0000: NOP\n0001: wait 0 ms\n0A93: end_custom_thread"
      )
    )
  }

  var loadingStatus by remember { mutableStateOf(strings.verifyingTitle) }
  var loadingSubStatus by remember { mutableStateOf("") }
  var verifiedResult by remember { mutableStateOf<CompilationResult.Success?>(null) }
  var errorResult by remember { mutableStateOf<CompilationResult.Failure?>(null) }
  var errorLine by remember { mutableStateOf<Int?>(null) }
  var generatedZip by remember { mutableStateOf<GeneratedZipResult?>(null) }

  // Control del botón atrás físico
  BackHandler(enabled = showAppMenu || showOpcodeList || currentScreen != AppScreenState.EDITOR) {
    if (showOpcodeList) {
      showOpcodeList = false
    } else if (showAppMenu) {
      showAppMenu = false
    } else if (currentScreen != AppScreenState.EDITOR) {
      currentScreen = AppScreenState.EDITOR
    }
  }

  CompositionLocalProvider(LocalAppStrings provides strings) {
    Box(modifier = Modifier.fillMaxSize()) {
      Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
          AppScreenState.EDITOR -> {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                  detectHorizontalDragGestures { _, dragAmount ->
                    // Gesto de derecha a izquierda abre la lista de opcodes
                    if (dragAmount < -35) {
                      showOpcodeList = true
                    }
                  }
                }
            ) {
              // Barra superior limpia: icono de nueve rayitas a la derecha
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Spacer(modifier = Modifier.weight(1f))

                // Icono de nueve rayitas
                IconButton(
                  onClick = { showAppMenu = true },
                  modifier = Modifier
                    .size(38.dp)
                    .testTag("open_nine_lines_menu_button")
                ) {
                  NineLinesIcon()
                }
              }

              CleoCodeEditor(
                code = codeState,
                onCodeChange = {
                  codeState = it
                  if (errorLine != null) {
                    errorLine = null
                  }
                },
                errorLine = errorLine,
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f)
              )

              // Botón azul pequeño con bordes redondeados
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color.White)
                  .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
              ) {
                Button(
                  onClick = {
                    errorLine = null
                    errorResult = null
                    currentScreen = AppScreenState.VERIFYING
                    loadingStatus = strings.verifyingTitle
                    loadingSubStatus = "Analizando instrucciones y etiquetas…"

                    coroutineScope.launch {
                      val check = withContext(Dispatchers.Default) {
                        CleoCompiler.compile(codeState.text)
                      }
                      if (check is CompilationResult.Failure) {
                        errorLine = check.error.line
                        errorResult = check
                        currentScreen = AppScreenState.EDITOR
                      } else {
                        verifiedResult = check as CompilationResult.Success
                        currentScreen = AppScreenState.FORMAT_SELECTION
                      }
                    }
                  },
                  shape = RoundedCornerShape(24.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White
                  ),
                  contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("compile_button")
                ) {
                  Text(
                    text = strings.btnCompile,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }

          AppScreenState.CUSTOM_OPCODES -> {
            CustomOpcodesScreen(
              onBack = { currentScreen = AppScreenState.EDITOR }
            )
          }

          AppScreenState.VERIFYING -> {
            CleoVerificationScreen(
              statusMessage = loadingStatus,
              subMessage = loadingSubStatus
            )
          }

          AppScreenState.FORMAT_SELECTION -> {
            val defaultName = verifiedResult?.scriptName ?: CleoCompiler.inferScriptName(codeState.text, "csa")
            FormatSelectionScreen(
              initialScriptName = defaultName,
              onFormatSelected = { format, chosenName ->
                val successData = verifiedResult ?: return@FormatSelectionScreen
                currentScreen = AppScreenState.COMPILING_ZIP
                loadingStatus = strings.compilingPrefix
                loadingSubStatus = strings.packagingZipSub

                coroutineScope.launch {
                  val zipPackage = withContext(Dispatchers.IO) {
                    CleoZipExporter.createZipPackage(
                      context = context,
                      bytecode = successData.bytecode,
                      sourceCode = codeState.text,
                      formatExtension = format.extension,
                      customScriptName = chosenName,
                      hexDump = successData.hexDump,
                      compilationTimeMs = successData.compilationTimeMs,
                      opcodesCount = successData.opcodesCompiled
                    )
                  }
                  generatedZip = zipPackage
                  currentScreen = AppScreenState.RESULTS
                }
              },
              onBack = {
                currentScreen = AppScreenState.EDITOR
              }
            )
          }

          AppScreenState.COMPILING_ZIP -> {
            CleoVerificationScreen(
              statusMessage = loadingStatus,
              subMessage = loadingSubStatus
            )
          }

          AppScreenState.RESULTS -> {
            generatedZip?.let { zip ->
              ResultsScreen(
                zipResult = zip,
                onBackToEditor = {
                  currentScreen = AppScreenState.EDITOR
                }
              )
            }
          }
        }
      }

      // Menú desplegable limpio (abierto desde las 9 rayitas)
      AnimatedVisibility(
        visible = showAppMenu,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
      ) {
        AppMenuScreen(
          currentLanguage = currentLanguage,
          onLanguageChanged = { newLang ->
            currentLanguage = newLang
          },
          onOpenCustomOpcodes = {
            currentScreen = AppScreenState.CUSTOM_OPCODES
          },
          onOpenOpcodeList = {
            showOpcodeList = true
          },
          onClose = {
            showAppMenu = false
          }
        )
      }

      // Panel de la lista de opcodes
      AnimatedVisibility(
        visible = showOpcodeList,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
      ) {
        OpcodeListScreen(
          onClose = { showOpcodeList = false },
          onOpenCustomOpcodes = {
            showOpcodeList = false
            currentScreen = AppScreenState.CUSTOM_OPCODES
          },
          onSelectOpcode = { selectedCode ->
            val current = codeState.text
            val separator = if (current.endsWith("\n") || current.isBlank()) "" else "\n"
            codeState = TextFieldValue(current + separator + selectedCode)
            showOpcodeList = false
          }
        )
      }
    }

    // Diálogo de error
    errorResult?.let { failure ->
      CompilationFeedbackDialog(
        result = failure,
        onDismiss = { errorResult = null }
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
  MyApplicationTheme {
    HomeScreen()
  }
}
