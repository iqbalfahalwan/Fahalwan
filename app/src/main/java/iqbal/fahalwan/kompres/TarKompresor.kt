package iqbal.fahalwan.kompres

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPOutputStream

object TarKompresor {

    interface ProgressListener {
        fun onProgress(fileName: String)
    }

    fun arsipkan(files: List<File>, output: File, listener: ProgressListener?): Boolean {
        return try {
            FileOutputStream(output).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    CustomTarOutputStream(bos).use { tarOut ->
                        files.forEach { file ->
                            tambahKeTar(file, file.name, tarOut, listener)
                        }
                        // Tambah dua blok 512-byte kosong untuk mengakhiri TAR
                        tarOut.writeEndOfArchive()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun tambahKeTar(file: File, entryName: String, tarOut: CustomTarOutputStream, listener: ProgressListener?) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children.isNullOrEmpty()) {
                tarOut.putNextEntry(CustomTarEntry(entryName + "/", 0, true))
                tarOut.closeEntry()
            } else {
                children.forEach { child ->
                    tambahKeTar(child, "$entryName/${child.name}", tarOut, listener)
                }
            }
        } else {
            FileInputStream(file).use { fis ->
                tarOut.putNextEntry(CustomTarEntry(entryName, file.length(), false))
                fis.copyTo(tarOut)
                tarOut.closeEntry()
            }
        }
        listener?.onProgress(entryName)
    }
}

// Custom TAR Entry class
class CustomTarEntry(
    val name: String,
    val size: Long,
    val isDirectory: Boolean
) {
    val mode: String = if (isDirectory) "0755" else "0644"
    val uid: String = "0"
    val gid: String = "0"
    val modTime: Long = System.currentTimeMillis() / 1000
}

// Custom TAR OutputStream
class CustomTarOutputStream(private val out: OutputStream) : Closeable {
    private var currentEntrySize: Long = 0
    private var currentEntryBytesWritten: Long = 0

    fun putNextEntry(entry: CustomTarEntry) {
        val header = createTarHeader(entry)
        out.write(header)
        currentEntrySize = entry.size
        currentEntryBytesWritten = 0
    }

    fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        currentEntryBytesWritten += len
    }

    fun write(b: Int) {
        out.write(b)
        currentEntryBytesWritten++
    }

    fun closeEntry() {
        // Pad to 512-byte boundary
        val padding = (512 - (currentEntryBytesWritten % 512)) % 512
        if (padding > 0) {
            val paddingBytes = ByteArray(padding.toInt())
            out.write(paddingBytes)
        }
    }

    fun writeEndOfArchive() {
        // Write two 512-byte zero blocks to end the archive
        val emptyBlock = ByteArray(512)
        out.write(emptyBlock)
        out.write(emptyBlock)
    }

    override fun close() {
        out.close()
    }

    private fun createTarHeader(entry: CustomTarEntry): ByteArray {
        val header = ByteArray(512)
        
        // Name (100 bytes)
        val nameBytes = entry.name.toByteArray(Charsets.UTF_8)
        System.arraycopy(nameBytes, 0, header, 0, minOf(nameBytes.size, 99)) // sisakan 1 byte untuk null terminator
        
        // Mode (8 bytes) - format oktal dengan null terminator
        val modeStr = String.format("%07o\u0000", entry.mode.toInt(8))
        val modeBytes = modeStr.toByteArray()
        System.arraycopy(modeBytes, 0, header, 100, modeBytes.size)
        
        // UID (8 bytes) - format oktal dengan null terminator
        val uidStr = String.format("%07o\u0000", entry.uid.toInt())
        val uidBytes = uidStr.toByteArray()
        System.arraycopy(uidBytes, 0, header, 108, uidBytes.size)
        
        // GID (8 bytes) - format oktal dengan null terminator  
        val gidStr = String.format("%07o\u0000", entry.gid.toInt())
        val gidBytes = gidStr.toByteArray()
        System.arraycopy(gidBytes, 0, header, 116, gidBytes.size)
        
        // Size (12 bytes) - format oktal dengan null terminator dan space
        val sizeStr = String.format("%011o\u0000", entry.size)
        val sizeBytes = sizeStr.toByteArray()
        System.arraycopy(sizeBytes, 0, header, 124, sizeBytes.size)
        
        // Modification time (12 bytes) - format oktal dengan null terminator dan space
        val mtimeStr = String.format("%011o\u0000", entry.modTime)
        val mtimeBytes = mtimeStr.toByteArray()
        System.arraycopy(mtimeBytes, 0, header, 136, mtimeBytes.size)
        
        // Checksum placeholder (8 bytes) - akan dihitung setelah header selesai
        val checksumSpace = "        ".toByteArray()
        System.arraycopy(checksumSpace, 0, header, 148, 8)
        
        // Type flag (1 byte)
        header[156] = if (entry.isDirectory) '5'.code.toByte() else '0'.code.toByte()
        
        // Magic number and version (POSIX TAR format)
        val magic = "ustar\u0000".toByteArray()
        System.arraycopy(magic, 0, header, 257, magic.size)
        
        // Version (2 bytes)
        header[263] = '0'.code.toByte()
        header[264] = '0'.code.toByte()
        
        // Calculate and set checksum
        val checksum = calculateChecksum(header)
        val checksumStr = String.format("%06o\u0000 ", checksum)
        val checksumBytes = checksumStr.toByteArray()
        System.arraycopy(checksumBytes, 0, header, 148, checksumBytes.size)
        
        return header
    }
    
    private fun calculateChecksum(header: ByteArray): Long {
        var checksum = 0L
        for (i in header.indices) {
            checksum += header[i].toUByte().toLong()
        }
        return checksum
    }
}

// Extension function untuk copyTo yang kompatibel dengan CustomTarOutputStream
fun InputStream.copyTo(out: CustomTarOutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied = 0L
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}