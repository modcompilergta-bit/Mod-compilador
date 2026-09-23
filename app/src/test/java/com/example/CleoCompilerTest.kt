package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.compiler.CleoCompiler
import com.example.compiler.CleoOpcodeDatabase
import com.example.compiler.CompilationResult
import com.example.compiler.CompilerErrorType
import com.example.compiler.OpcodeDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CleoCompilerTest {

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    CleoOpcodeDatabase.loadFromAssets(context)
  }

  @Test
  fun `valid cleo script compiles successfully`() {
    val script = """
      // Prueba de script
      0001: wait 0 ms
      004E: end_thread
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar con éxito", result is CompilationResult.Success)

    val success = result as CompilationResult.Success
    assertEquals(2, success.opcodesCompiled)
    assertTrue("Debe generar bytes", success.bytecode.isNotEmpty())
    // 0001 wait 0 -> 01 00 04 00 (4 bytes), 004E -> 4E 00 (2 bytes) = 6 bytes
    assertEquals("01 00 04 00 4E 00", success.hexDump)
  }

  @Test
  fun `unknown opcode returns failure with line number`() {
    val script = """
      0001: wait 0 ms
      9999: inventado
      004E: end_thread
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe fallar", result is CompilationResult.Failure)

    val failure = result as CompilationResult.Failure
    assertEquals(2, failure.error.line)
    assertEquals(CompilerErrorType.UNKNOWN_OPCODE, failure.error.type)
  }

  @Test
  fun `invalid opcode format returns failure`() {
    val script = """
      0001: wait 0 ms
      malformado: codigo
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe fallar", result is CompilationResult.Failure)

    val failure = result as CompilationResult.Failure
    assertEquals(2, failure.error.line)
    assertEquals(CompilerErrorType.INVALID_OPCODE_FORMAT, failure.error.type)
  }

  @Test
  fun `missing parameters in opcode returns failure`() {
    val script = """
      0001: wait
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe fallar", result is CompilationResult.Failure)

    val failure = result as CompilationResult.Failure
    assertEquals(1, failure.error.line)
    assertEquals(CompilerErrorType.INVALID_PARAMETERS, failure.error.type)
  }

  @Test
  fun `empty source returns empty error`() {
    val script = "   \n// solo comentario\n   "
    val result = CleoCompiler.compile(script)
    assertTrue("Debe fallar por código vacío", result is CompilationResult.Failure)
    val failure = result as CompilationResult.Failure
    assertEquals(CompilerErrorType.EMPTY_SOURCE, failure.error.type)
  }

  @Test
  fun `database contains at least 1000 valid opcodes and category checks pass`() {
    val count = com.example.compiler.CleoOpcodeDatabase.count()
    assertTrue("La base de datos debe contener al menos 1000 opcodes (actual: $count)", count >= 1000)

    val worldMissionsOpcodes = com.example.compiler.CleoOpcodeDatabase.getAll().filter {
      it.category.equals("Mundo, Interiores & Misiones", ignoreCase = true)
    }
    assertEquals(300, worldMissionsOpcodes.size)

    val cameraHudEffectsOpcodes = com.example.compiler.CleoOpcodeDatabase.getAll().filter {
      it.category.equals("Cámara, HUD, Textos & Efectos", ignoreCase = true)
    }
    assertEquals(300, cameraHudEffectsOpcodes.size)

    // Comprobar opcodes emblemáticos de GTA SA
    assertTrue("Debe contener 0001 (wait)", com.example.compiler.CleoOpcodeDatabase.findByHex("0001") != null)
    assertTrue("Debe contener 0213 (create_pickup)", com.example.compiler.CleoOpcodeDatabase.findByHex("0213") != null)
    assertTrue("Debe contener 0107 (create_object)", com.example.compiler.CleoOpcodeDatabase.findByHex("0107") != null)
    assertTrue("Debe contener 04BB (set_current_interior)", com.example.compiler.CleoOpcodeDatabase.findByHex("04BB") != null)
    assertTrue("Debe contener 01F5 (create_checkpoint)", com.example.compiler.CleoOpcodeDatabase.findByHex("01F5") != null)
    assertTrue("Debe contener 00BB (print_with_number_now)", com.example.compiler.CleoOpcodeDatabase.findByHex("00BB") != null)
    assertTrue("Debe contener 015C (set_fixed_camera_position)", com.example.compiler.CleoOpcodeDatabase.findByHex("015C") != null)
    assertTrue("Debe contener 045A (draw_rect)", com.example.compiler.CleoOpcodeDatabase.findByHex("045A") != null)
    assertTrue("Debe contener 024F (create_corona)", com.example.compiler.CleoOpcodeDatabase.findByHex("024F") != null)
    assertTrue("Debe contener 04FC (set_night_vision)", com.example.compiler.CleoOpcodeDatabase.findByHex("04FC") != null)
  }

  @Test
  fun `custom opcode is recognized and compiled`() {
    val customDef = com.example.compiler.OpcodeDef(
      opcode = 0x0A90,
      hexString = "0A90",
      commandName = "call_function",
      description = "Llama funcion interna",
      minParams = 1,
      maxParams = 1,
      example = "0A90: call_function 0x123456"
    )
    com.example.compiler.CleoOpcodeDatabase.registerCustom(customDef)

    val script = """
      0001: wait 0 ms
      0A90: call_function 100
      004E: end_thread
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar con éxito usando el opcode personalizado", result is CompilationResult.Success)
  }
}
