package dev.msfjarvis.aps.util.extensions

import java.io.File

fun File.findTillRoot(fileName: String, rootPath: File): File? {
  val gpgFile = File(this, fileName)
  if (gpgFile.exists()) return gpgFile

  if (this.absolutePath == rootPath.absolutePath) {
    return null
  }
  val parent = parentFile
  return if (parent != null && parent.exists()) {
    parent.findTillRoot(fileName, rootPath)
  } else {
    null
  }
}
