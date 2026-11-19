package com.example.domain

import android.content.Context
import android.net.Uri
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExporter {

  fun writeElectronZipToUri(
    context: Context,
    targetUri: Uri,
    files: List<GeneratedElectronFile>,
    rootFolderName: String = FerizyBuildVersion.ZIP_BASENAME,
  ): Result<Int> = runCatching {
    val outputStream =
      context.contentResolver.openOutputStream(targetUri)
        ?: error("Tidak dapat membuka file tujuan untuk menulis ZIP.")

    outputStream.use { rawOut ->
      ZipOutputStream(rawOut).use { zipOut ->
        files.forEach { file ->
          val entryPath = "$rootFolderName/${file.relativePath}"
          zipOut.putNextEntry(ZipEntry(entryPath))
          zipOut.write(file.content.toByteArray(StandardCharsets.UTF_8))
          zipOut.closeEntry()
        }
      }
    }
    files.size
  }

  fun writeTextFileToUri(
    context: Context,
    targetUri: Uri,
    content: String,
  ): Result<Unit> = runCatching {
    val outputStream =
      context.contentResolver.openOutputStream(targetUri)
        ?: error("Tidak dapat membuka file tujuan untuk menulis JSON.")
    outputStream.use { out ->
      out.write(content.toByteArray(StandardCharsets.UTF_8))
    }
  }
}
