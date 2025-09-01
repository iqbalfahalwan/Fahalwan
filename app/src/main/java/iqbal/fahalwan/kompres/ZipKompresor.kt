package iqbal.fahalwan.kompres

import java.io.*
import java.util.zip.*

object ZipKompresor {

    interface ProgressListener {
        fun onProgress(fileName: String, progress: Int)
    }

    fun kompres(files: List<File>, output: File, listener: ProgressListener?): Boolean {
        return try {
            val totalSize = files.sumOf { ukuranFile(it) }
            var processedSize = 0L

            ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
                files.forEach { file ->
                    tambahKeZip(file, file.name, zos) { bytes, nama ->
                        processedSize += bytes
                        val persen = ((processedSize * 100) / totalSize).toInt()
                        listener?.onProgress(nama, persen)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun tambahKeZip(
        file: File,
        namaEntry: String,
        zos: ZipOutputStream,
        progress: (Long, String) -> Unit
    ) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children.isNullOrEmpty()) {
                zos.putNextEntry(ZipEntry("$namaEntry/"))
                zos.closeEntry()
            } else {
                children.forEach { child ->
                    tambahKeZip(child, "$namaEntry/${child.name}", zos, progress)
                }
            }
        } else {
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    val entry = ZipEntry(namaEntry)
                    zos.putNextEntry(entry)

                    val buffer = ByteArray(4096)
                    var panjang: Int
                    var totalBytes = 0L
                    while (bis.read(buffer).also { panjang = it } != -1) {
                        zos.write(buffer, 0, panjang)
                        totalBytes += panjang
                        progress(totalBytes, namaEntry)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun ukuranFile(file: File): Long {
        return if (file.isDirectory) {
            file.listFiles()?.sumOf { ukuranFile(it) } ?: 0L
        } else {
            file.length()
        }
    }
}