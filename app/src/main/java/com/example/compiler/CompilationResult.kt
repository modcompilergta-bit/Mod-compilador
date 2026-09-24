package com.example.compiler

/**
 * Representa los tipos de error posibles durante la compilación.
 */
enum class CompilerErrorType {
  EMPTY_SOURCE,
  UNKNOWN_OPCODE,
  INVALID_OPCODE_FORMAT,
  INVALID_PARAMETERS,
  SYNTAX_ERROR
}

/**
 * Información detallada de un error de compilación.
 */
data class CompilationError(
  val line: Int,
  val rawLine: String,
  val type: CompilerErrorType,
  val message: String,
  val suggestion: String? = null
)

/**
 * Resultado de la compilación.
 */
sealed class CompilationResult {
  data class Success(
    val bytecode: ByteArray,
    val opcodesCompiled: Int,
    val totalLines: Int,
    val hexDump: String,
    val scriptName: String = "script.csa",
    val compilationTimeMs: Long = 0L
  ) : CompilationResult() {
    val sizeBytes: Int get() = bytecode.size

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as Success
      return bytecode.contentEquals(other.bytecode) &&
          opcodesCompiled == other.opcodesCompiled &&
          totalLines == other.totalLines
    }

    override fun hashCode(): Int {
      var result = bytecode.contentHashCode()
      result = 31 * result + opcodesCompiled
      result = 31 * result + totalLines
      return result
    }
  }

  data class Failure(
    val error: CompilationError
  ) : CompilationResult()
}
