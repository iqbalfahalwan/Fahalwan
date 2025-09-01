package iqbal.fahalwan.kompres

import android.os.Handler
import android.os.Looper
import java.io.*
import java.util.zip.GZIPOutputStream

object GzKompresor {

    interface ProgressListener {
        fun onProgress(progress: Int)
    }

    fun kompres(input: File, output: File, listener: ProgressListener?): Boolean {
        if (input.isDirectory) {
            throw IllegalArgumentException("GZ hanya bisa untuk 1 file, bukan folder")
        }

        val mainHandler = Handler(Looper.getMainLooper())

        return try {
            val totalSize = input.length()
            var processedSize = 0L

            FileInputStream(input).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    FileOutputStream(output).use { fos ->
                        GZIPOutputStream(BufferedOutputStream(fos)).use { gzos ->

                            val buffer = ByteArray(4096)
                            var panjang: Int
                            while (bis.read(buffer).also { panjang = it } != -1) {
                                gzos.write(buffer, 0, panjang)
                                processedSize += panjang
                                val persen = ((processedSize * 100) / totalSize).toInt()

                                mainHandler.post {
                                    listener?.onProgress(persen)
                                }
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}