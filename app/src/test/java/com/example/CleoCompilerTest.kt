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

  @Test
  fun `long cleo script compiles without errors`() {
    val script = """
      // ========================================================
      // COMPILER STUDIO - SCRIPT CLEO COMPLETO (.CS)
      // MOD: SUPER CJ - SALUD INFINITA, ARMAS Y VEHICULOS
      // ========================================================

      03A4: name_thread 'SUPERCJ'
      0001: wait 1000 ms
      0ACA: show_text_box "Compiler Studio: Super CJ Activado"

      0109: player ${'$'}PLAYER_CHAR add_money 999999
      01B6: set_weather 1
      014D: set_actor ${'$'}PLAYER_ACTOR armour 100
      01B2: give_actor ${'$'}PLAYER_ACTOR weapon 24 ammo 250
      05E2: give_actor ${'$'}PLAYER_ACTOR weapon 31 ammo 500

      :MAIN_LOOP
      0001: wait 250 ms
      014D: set_actor ${'$'}PLAYER_ACTOR armour 100
      02AB: set_actor ${'$'}PLAYER_ACTOR immunities 1 1 1 1 1
      0050: gosub @SUB_REPAIR_VEHICLE
      0002: jump @MAIN_LOOP

      :SUB_REPAIR_VEHICLE
      0001: wait 50 ms
      0229: set_car ${'$'}CAR health 1000
      0051: return

      :CLEO_TERMINATE
      0001: wait 500 ms
      0ACA: show_text_box "Mod Desactivado"
      004E: end_thread
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar exitosamente: $result", result is CompilationResult.Success)
    val success = result as CompilationResult.Success
    assertTrue("Debe generar bytecode válido", success.bytecode.isNotEmpty())
    assertTrue("Debe compilar al menos 15 opcodes", success.opcodesCompiled >= 15)
  }

  @Test
  fun `jump to label generates negative relative offset for CLEO`() {
    val script = """
      :LOOP
      0001: wait 0 ms
      0002: jump @LOOP
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar", result is CompilationResult.Success)
    val success = result as CompilationResult.Success
    // Offset de :LOOP es 0.
    // 0001 wait 0 ms: 01 00 (opcode) 04 00 (Int8 0) = 4 bytes
    // 0002 jump @LOOP: 02 00 (opcode) 01 (Int32) 00 00 00 00 (-0 = 0)
    assertEquals("01 00 04 00 02 00 01 00 00 00 00", success.hexDump)
  }

  @Test
  fun `undefined label returns syntax error`() {
    val script = """
      0001: wait 0 ms
      0002: jump @NOT_EXIST
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe fallar", result is CompilationResult.Failure)
    val failure = result as CompilationResult.Failure
    assertEquals(CompilerErrorType.SYNTAX_ERROR, failure.error.type)
  }

  @Test
  fun `missing end_thread automatically protected to avoid game crash`() {
    val script = """
      0001: wait 250 ms
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar", result is CompilationResult.Success)
    val success = result as CompilationResult.Success
    // 0001 wait 250: 01 00 05 FA 00 (250 cabe en Int16: 0x05 + 0x00FA) + terminador seguro 4E 00
    assertTrue("Debe terminar en 4E 00 para proteger el juego", success.hexDump.endsWith("4E 00"))
  }

  @Test
  fun `negated opcode with NOT prefix sets bit 15`() {
    val script = """
      0001: wait 0 ms
      NOT 00DF: is_char_in_any_car ${'$'}PLAYER_ACTOR
      004E: end_thread
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar condición negada", result is CompilationResult.Success)
    val success = result as CompilationResult.Success
    // 00DF or 0x8000 = 0x80DF -> Little endian: DF 80
    assertTrue("Debe contener DF 80", success.hexDump.contains("DF 80"))
  }

  @Test
  fun `sanny builder high level syntax compiles arithmetic and comparisons`() {
    val script = """
      wait 0 ms
      0@ = 10
      0@ += 5
      0@ -= 2
      0@ *= 3
      0@ /= 2
      0@ > 10
      0@ >= 10
      0@ == 10
      ${'$'}VAR = 100
      ${'$'}VAR += 50
      end_thread
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar sintaxis de Sanny Builder: $result", result is CompilationResult.Success)
    val success = result as CompilationResult.Success
    assertTrue("Debe contener al menos 12 instrucciones", success.opcodesCompiled >= 12)
    // Comprobar que terminó en 4E 00
    assertTrue("Debe contener end_thread", success.hexDump.endsWith("4E 00"))
  }

  @Test
  fun `sanny builder high level flow jumps and labels compile correctly`() {
    val script = """
      :LOOP
      wait 100 ms
      0@ += 1
      jump @LOOP
    """.trimIndent()

    val result = CleoCompiler.compile(script)
    assertTrue("Debe compilar bucle con etiquetas", result is CompilationResult.Success)
    val success = result as CompilationResult.Success
    assertTrue("Debe generar bytecode no vacío", success.bytecode.isNotEmpty())
  }
}
