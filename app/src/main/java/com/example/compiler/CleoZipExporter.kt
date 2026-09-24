package com.example.compiler

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class GeneratedZipResult(
  val zipFile: File,
  val fileName: String,
  val sizeBytes: Long,
  val scriptFormat: String,
  val scriptEntryName: String,
  val scriptFile: File? = null,
  val hexDump: String = "",
  val compilationTimeMs: Long = 0L,
  val opcodesCount: Int = 0
)

object CleoZipExporter {

  /**
   * Crea un archivo ZIP y un archivo binario directo (.csa/.csi) en el caché.
   * Asigna un nombre de script razonable y personalizado deducido o ingresado,
   * evitando colisiones y reemplazos no deseados en la carpeta de juego /data.
   */
  fun createZipPackage(
    context: Context,
    bytecode: ByteArray,
    sourceCode: String,
    formatExtension: String, // "csa" o "csi"
    customScriptName: String? = null,
    hexDump: String = "",
    compilationTimeMs: Long = 0L,
    opcodesCount: Int = 0
  ): GeneratedZipResult {
    val cleanExt = formatExtension.removePrefix(".").lowercase()
    
    // Resolver nombre del script inteligente o especificado
    val effectiveBaseName = if (!customScriptName.isNullOrBlank()) {
      customScriptName.removeSuffix(".$cleanExt").removeSuffix(".csa").removeSuffix(".csi").trim()
    } else {
      CleoCompiler.inferScriptName(sourceCode, cleanExt).removeSuffix(".$cleanExt")
    }

    val scriptFileName = "$effectiveBaseName.$cleanExt"
    val zipName = "${effectiveBaseName}_$cleanExt.zip"

    val cacheDir = context.cacheDir

    // Archivo binario directo del script (.csa o .csi)
    val directScriptFile = File(cacheDir, scriptFileName)
    directScriptFile.writeBytes(bytecode)

    // Archivo ZIP empaquetado
    val zipFile = File(cacheDir, zipName)
    if (zipFile.exists()) {
      zipFile.delete()
    }

    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
      // 1. Script binario compilado (.csa o .csi con nombre coherente)
      val scriptEntry = ZipEntry(scriptFileName)
      zipOut.putNextEntry(scriptEntry)
      zipOut.write(bytecode)
      zipOut.closeEntry()

      // 2. Archivo de código fuente original (.txt)
      val sourceEntry = ZipEntry("${effectiveBaseName}_source.txt")
      zipOut.putNextEntry(sourceEntry)
      zipOut.write(sourceCode.toByteArray(Charsets.UTF_8))
      zipOut.closeEntry()
    }

    return GeneratedZipResult(
      zipFile = zipFile,
      fileName = zipName,
      sizeBytes = zipFile.length(),
      scriptFormat = cleanExt.uppercase(),
      scriptEntryName = scriptFileName,
      scriptFile = directScriptFile,
      hexDump = hexDump,
      compilationTimeMs = compilationTimeMs,
      opcodesCount = opcodesCount
    )
  }

  /**
   * Guarda el archivo directo del script (.csa o .cs) en Downloads.
   */
  fun saveScriptToDownloads(context: Context, scriptFile: File, displayName: String): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
      }
      val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
      if (uri != null) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          scriptFile.inputStream().use { input ->
            input.copyTo(out)
          }
        }
      }
      uri
    } else {
      @Suppress("DEPRECATION")
      val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val destFile = File(downloadsDir, displayName)
      scriptFile.copyTo(destFile, overwrite = true)
      Uri.fromFile(destFile)
    }
  }

  /**
   * Guarda o mueve el archivo ZIP a la carpeta pública 'Downloads' del dispositivo.
   */
  fun saveToDownloads(context: Context, zipFile: File, displayName: String): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
      }
      val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
      if (uri != null) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          zipFile.inputStream().use { input ->
            input.copyTo(out)
          }
        }
      }
      uri
    } else {
      @Suppress("DEPRECATION")
      val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val destFile = File(downloadsDir, displayName)
      zipFile.copyTo(destFile, overwrite = true)
      Uri.fromFile(destFile)
    }
  }

  /**
   * Genera un Intent para abrir o compartir el archivo ZIP con aplicaciones del sistema.
   */
  fun createShareIntent(context: Context, zipFile: File): Intent {
    val authority = "${context.packageName}.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, zipFile)

    return Intent(Intent.ACTION_SEND).apply {
      type = "application/zip"
      putExtra(Intent.EXTRA_STREAM, contentUri)
      putExtra(Intent.EXTRA_SUBJECT, zipFile.name)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  /**
   * Genera un Intent para compartir el archivo directo (.csa o .cs).
   */
  fun createShareScriptIntent(context: Context, scriptFile: File): Intent {
    val authority = "${context.packageName}.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, scriptFile)

    return Intent(Intent.ACTION_SEND).apply {
      type = "application/octet-stream"
      putExtra(Intent.EXTRA_STREAM, contentUri)
      putExtra(Intent.EXTRA_SUBJECT, scriptFile.name)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }
}
