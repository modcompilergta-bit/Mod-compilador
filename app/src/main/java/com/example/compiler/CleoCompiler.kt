package com.example.compiler

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Compilador de scripts CLEO para GTA San Andreas (formatos .cs y .csa).
 */
object CleoCompiler {

  /**
   * Compila el código fuente en texto a bytecode ejecutable de CLEO.
   * Si ocurre algún error de sintaxis o opcode inexistente, devuelve [CompilationResult.Failure].
   */
  fun compile(sourceCode: String): CompilationResult {
    val lines = sourceCode.lines()
    if (lines.all { it.isBlank() || it.trim().startsWith("//") }) {
      return CompilationResult.Failure(
        CompilationError(
          line = 1,
          rawLine = sourceCode.take(40),
          type = CompilerErrorType.EMPTY_SOURCE,
          message = "El código fuente está vacío. Agrega al menos una instrucción CLEO.",
          suggestion = "Ejemplo: 0001: wait 0 ms"
        )
      )
    }

    val outputStream = ByteArrayOutputStream()
    var opcodesCount = 0

    lines.forEachIndexed { index, rawLine ->
      val lineNumber = index + 1
      val cleanLine = rawLine.trim()

      // Ignorar líneas vacías o comentarios completos
      if (cleanLine.isEmpty() || cleanLine.startsWith("//") || cleanLine.startsWith(";")) {
        return@forEachIndexed
      }

      // Remover comentarios al final de la línea si existen
      val lineWithoutComment = cleanLine.split("//", ";").first().trim()
      if (lineWithoutComment.isEmpty()) {
        return@forEachIndexed
      }

      // Si es una etiqueta pura (ej. :LABEL o @LABEL)
      if (lineWithoutComment.startsWith(":") || lineWithoutComment.startsWith("@")) {
        // En un paso futuro podemos resolver offsets de etiquetas
        return@forEachIndexed
      }

      // Extraer el opcode inicial (esperado: 4 caracteres hex y dos puntos, ej. 0001:)
      val parts = lineWithoutComment.split(Regex("\\s+"), limit = 2)
      val firstToken = parts.getOrNull(0)?.trim() ?: ""

      val opcodeMatch = Regex("^([0-9A-Fa-f]{4}):?$").find(firstToken)
      if (opcodeMatch == null) {
        return CompilationResult.Failure(
          CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.INVALID_OPCODE_FORMAT,
            message = "Formato de opcode inválido: '$firstToken'. Cada instrucción debe iniciar con un código hexadecimal de 4 dígitos (ejemplo: '0001:').",
            suggestion = "Revisa que el opcode tenga 4 dígitos hexadecimales y termine en dos puntos."
          )
        )
      }

      val opcodeHex = opcodeMatch.groupValues[1].uppercase()
      val opcodeInt = opcodeHex.toInt(16)

      // Verificar si el opcode existe en la base de datos
      val opcodeDef = CleoOpcodeDatabase.findByHex(opcodeHex)
      if (opcodeDef == null) {
        return CompilationResult.Failure(
          CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.UNKNOWN_OPCODE,
            message = "Opcode no reconocido: '$opcodeHex'. Este opcode no existe en el catálogo de instrucciones de GTA San Andreas ni en CLEO Android.",
            suggestion = "Verifica la sintaxis del opcode. Opcodes comunes: 0001 (wait), 004E (end_thread), 03A4 (name_thread)."
          )
        )
      }

      // Analizar los argumentos de la instrucción
      val argumentsRest = if (parts.size > 1) parts[1].trim() else ""
      val validationError = validateAndWriteOpcode(
        opcodeDef = opcodeDef,
        argumentsRest = argumentsRest,
        lineNumber = lineNumber,
        rawLine = rawLine,
        outputStream = outputStream
      )

      if (validationError != null) {
        return CompilationResult.Failure(validationError)
      }

      opcodesCount++
    }

    val compiledBytes = outputStream.toByteArray()
    val hexDump = compiledBytes.joinToString(" ") { "%02X".format(it) }

    return CompilationResult.Success(
      bytecode = compiledBytes,
      opcodesCompiled = opcodesCount,
      totalLines = lines.size,
      hexDump = hexDump
    )
  }

  private fun validateAndWriteOpcode(
    opcodeDef: OpcodeDef,
    argumentsRest: String,
    lineNumber: Int,
    rawLine: String,
    outputStream: ByteArrayOutputStream
  ): CompilationError? {
    // Escribir los 2 bytes del opcode en formato little-endian
    val opcodeBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    opcodeBuffer.putShort(opcodeDef.opcode.toShort())

    when (opcodeDef.opcode) {
      // 0001: wait X ms
      0x0001 -> {
        // Formato esperado: wait <tiempo> ms  o  <tiempo>
        val tokens = argumentsRest.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val timeToken = tokens.firstOrNull { it.all { char -> char.isDigit() } }
          ?: return CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.INVALID_PARAMETERS,
            message = "El opcode 0001: (wait) requiere el tiempo en milisegundos como número entero.",
            suggestion = "Ejemplo: 0001: wait 0 ms"
          )

        val timeValue = timeToken.toIntOrNull()
          ?: return CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.INVALID_PARAMETERS,
            message = "Valor de tiempo '$timeToken' no es un número entero válido.",
            suggestion = "Ejemplo: 0001: wait 250 ms"
          )

        outputStream.write(opcodeBuffer.array())
        // Tipo 0x04 para int 8-bit o 0x01 para int 32-bit
        if (timeValue in -128..127) {
          outputStream.write(byteArrayOf(0x04, timeValue.toByte()))
        } else {
          val argBuffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
          argBuffer.put(0x01.toByte())
          argBuffer.putInt(timeValue)
          outputStream.write(argBuffer.array())
        }
      }

      // 004E: end_thread (0 parámetros)
      0x004E -> {
        val nonKeywordArgs = argumentsRest.split(Regex("\\s+"))
          .filter { it.isNotEmpty() && !it.equals("end_thread", ignoreCase = true) }

        if (nonKeywordArgs.isNotEmpty()) {
          return CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.INVALID_PARAMETERS,
            message = "El opcode 004E: (end_thread) no acepta parámetros.",
            suggestion = "Usa únicamente: 004E: end_thread"
          )
        }
        outputStream.write(opcodeBuffer.array())
      }

      // 00BF: text_clear_all (0 parámetros)
      0x00BF, 0x0051, 0x038B -> {
        outputStream.write(opcodeBuffer.array())
      }

      // 03A4: name_thread 'NOMBRE'
      0x03A4 -> {
        val stringMatch = Regex("['\"]([^'\"]+)['\"]").find(argumentsRest)
        val threadName = stringMatch?.groupValues?.get(1) ?: argumentsRest.substringAfter("name_thread").trim()

        if (threadName.isEmpty()) {
          return CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.INVALID_PARAMETERS,
            message = "El opcode 03A4: (name_thread) requiere el nombre del hilo entre comillas.",
            suggestion = "Ejemplo: 03A4: name_thread 'MYMOD'"
          )
        }

        outputStream.write(opcodeBuffer.array())
        // Tag 0x09: cadena corta fija de 8 bytes con padding nulo
        outputStream.write(0x09)
        val nameBytes = ByteArray(8)
        val ascii = threadName.take(7).toByteArray(Charsets.US_ASCII)
        System.arraycopy(ascii, 0, nameBytes, 0, ascii.size)
        outputStream.write(nameBytes)
      }

      // 0ACA: show_text_box "Texto"
      0x0ACA -> {
        val stringMatch = Regex("['\"]([^'\"]+)['\"]").find(argumentsRest)
        val text = stringMatch?.groupValues?.get(1)
        if (text == null) {
          return CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.INVALID_PARAMETERS,
            message = "El opcode 0ACA: (show_text_box) requiere el texto entre comillas.",
            suggestion = "Ejemplo: 0ACA: show_text_box \"Hola mundo\""
          )
        }
        outputStream.write(opcodeBuffer.array())
        // Tag 0x0E para cadenas de longitud variable
        val textBytes = text.toByteArray(Charsets.US_ASCII)
        outputStream.write(byteArrayOf(0x0E, textBytes.size.toByte()))
        outputStream.write(textBytes)
      }

      // Para los demás opcodes registrados: validamos parámetros genéricos
      else -> {
        val tokens = argumentsRest.split(Regex("\\s+"))
          .filter { it.isNotEmpty() && !it.equals(opcodeDef.commandName, ignoreCase = true) }

        if (tokens.size < opcodeDef.minParams) {
          return CompilationError(
            line = lineNumber,
            rawLine = rawLine,
            type = CompilerErrorType.INVALID_PARAMETERS,
            message = "El opcode ${opcodeDef.hexString}: (${opcodeDef.commandName}) requiere al menos ${opcodeDef.minParams} parámetro(s).",
            suggestion = "Sintaxis recomendada: ${opcodeDef.example}"
          )
        }

        outputStream.write(opcodeBuffer.array())
        // Escribir parámetros numéricos o simples
        tokens.forEach { token ->
          val intVal = token.toIntOrNull()
          if (intVal != null) {
            outputStream.write(byteArrayOf(0x04, intVal.toByte()))
          }
        }
      }
    }

    return null
  }
}
