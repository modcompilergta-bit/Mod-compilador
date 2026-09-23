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
  val scriptEntryName: String
)

object CleoZipExporter {

  /**
   * Crea un archivo ZIP real en el caché que contiene el script compilado y el código fuente.
   */
  fun createZipPackage(
    context: Context,
    bytecode: ByteArray,
    sourceCode: String,
    formatExtension: String // "csa" o "cs"
  ): GeneratedZipResult {
    val cleanExt = formatExtension.removePrefix(".").lowercase()
    val zipName = "script_$cleanExt.zip"
    val scriptFileName = "script.$cleanExt"

    val cacheDir = context.cacheDir
    val zipFile = File(cacheDir, zipName)
    if (zipFile.exists()) {
      zipFile.delete()
    }

    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
      // 1. Script binario compilado (.csa o .cs)
      val scriptEntry = ZipEntry(scriptFileName)
      zipOut.putNextEntry(scriptEntry)
      zipOut.write(bytecode)
      zipOut.closeEntry()

      // 2. Archivo de código fuente fuente original (.txt)
      val sourceEntry = ZipEntry("source.txt")
      zipOut.putNextEntry(sourceEntry)
      zipOut.write(sourceCode.toByteArray(Charsets.UTF_8))
      zipOut.closeEntry()
    }

    return GeneratedZipResult(
      zipFile = zipFile,
      fileName = zipName,
      sizeBytes = zipFile.length(),
      scriptFormat = cleanExt.uppercase(),
      scriptEntryName = scriptFileName
    )
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
}
