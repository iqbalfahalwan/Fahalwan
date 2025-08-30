package iqbal.fahalwan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import android.app.ProgressDialog
import kotlinx.coroutines.*
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Berkas : AppCompatActivity() {

    private lateinit var liner: LinearLayout
    private lateinit var memo: File
    private lateinit var jalur: TextView
    private lateinit var lama: LinearLayout
    private lateinit var bersih: ImageView
    private var terpilih: MutableList<File> = mutableListOf()
    private var fileUntukTempel: MutableList<File> = mutableListOf()
    private var operasiSalin: Boolean = false
    private var zipJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.berkas)

        liner = findViewById(R.id.liner)
        memo = Environment.getExternalStorageDirectory()
        jalur = findViewById(R.id.jalur)
        lama = findViewById(R.id.lama)
        bersih = findViewById(R.id.bersih)

        Segarkan(memo)
        Tombol()
        IklanKotak()
    }

    private fun IklanKotak() {
        try {
            val IN = findViewById<AdView>(R.id.iklan_nav)
            val IU = findViewById<AdView>(R.id.iklan_utama)
            val iklan = AdRequest.Builder().build()
            IN?.loadAd(iklan)
            IU?.loadAd(iklan)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Keluar() {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Dari Berkas?")
            .setPositiveButton("Ya") { _, _ -> 
                zipJob?.cancel()
                finish() 
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onBackPressed() {
        if (liner.visibility == View.VISIBLE) {
            liner.visibility = View.GONE
        } else if (terpilih.isNotEmpty()) {
            BersihkanSeleksi()
        } else {
            val target = memo.parentFile
            if (target == null || !target.canRead()) {
                Toast.makeText(this, "Root Directory", Toast.LENGTH_SHORT).show()
            } else {
                memo = target
                Segarkan(memo)
            }
        }
    }

    private fun BersihkanSeleksi() {
        val papan = findViewById<GridLayout>(R.id.grid_papan)
        papan.removeAllViews()
        terpilih.clear()
        lama.visibility = View.GONE
        bersih.visibility = View.GONE
        Segarkan(memo)
    }

    private fun Segarkan(memo: File) {
        if (!memo.exists() || !memo.canRead()) {
            Toast.makeText(this, "Tidak dapat mengakses direktori", Toast.LENGTH_SHORT).show()
            return
        }
        jalur.text = memo.absolutePath
        AturGrid(memo)
    }

    private fun Tombol() {
        findViewById<ImageView>(R.id.nav)?.setOnClickListener {
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        findViewById<TextView>(R.id.keluar)?.setOnClickListener {
            Keluar()
        }

        findViewById<ImageView>(R.id.apk)?.setOnClickListener {
            memo = filesDir
            liner.visibility = View.GONE
            Segarkan(memo)
        }

        findViewById<ImageView>(R.id.sd)?.setOnClickListener {
            memo = Environment.getExternalStorageDirectory()
            liner.visibility = View.GONE
            Segarkan(memo)
        }

        bersih.setOnClickListener {
            BersihkanSeleksi()
        }
        
        val salin = findViewById<ImageView>(R.id.salin)
        val potong = findViewById<ImageView>(R.id.potong)
        val tempel = findViewById<ImageView>(R.id.tempel)
        val hapus = findViewById<ImageView>(R.id.hapus)
        val rename = findViewById<ImageView>(R.id.rename)
        val zip = findViewById<ImageView>(R.id.zip)

        salin?.setOnClickListener {
            Menyalin()
        }
        
        potong?.setOnClickListener {
            Memotong()
        }
        
        tempel?.setOnClickListener {
            Meletakan()
        }
        
        rename?.setOnClickListener {
            NamaiUlang()
        }
        
        hapus?.setOnClickListener {
            HapusFile()
        }
        
        zip?.setOnClickListener {
            MulaiZip()
        }
    }

    private fun BukaFile(target: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", target)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, getMimeType(target))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Tidak dapat membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "zip", "rar" -> "application/zip"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            else -> "*/*"
        }
    }

    private fun AturGrid(memo: File) {
        val grid = findViewById<GridLayout>(R.id.grid_utama)
        grid?.removeAllViews()

        try {
            val files = memo.listFiles()
            if (files == null || files.isEmpty()) {
                val empty = TextView(this)
                empty.text = "Folder kosong"
                empty.gravity = android.view.Gravity.CENTER
                empty.setPadding(50, 100, 50, 100)
                grid?.addView(empty)
                return
            }

            val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            for (file in sortedFiles) {
                val item = LayoutInflater.from(this).inflate(R.layout.item_vertical, grid, false)
                val nama = item.findViewById<TextView>(R.id.nama)
                val gambar = item.findViewById<ImageView>(R.id.gambar)
                val centang = item.findViewById<TextView>(R.id.centang)

                nama?.text = file.name
                gambar?.setImageResource(if (file.isDirectory) R.drawable.folder else R.drawable.file)
                centang?.visibility = if (terpilih.contains(file)) View.VISIBLE else View.GONE

                item.setOnClickListener {
                    if (terpilih.isEmpty()) {
                        if (file.isDirectory) {
                            Segarkan(file)
                        } else {
                            BukaFile(file)
                        }
                    } else {
                        PilihFile(file, centang)
                    }
                }

                item.setOnLongClickListener {
                    PilihFile(file, centang)
                    true
                }

                grid?.addView(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saat memuat file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        
        UpdateTampilanTombol()
    }

    private fun UpdateTampilanTombol() {
        val rename = findViewById<ImageView>(R.id.rename)
        val tempel = findViewById<ImageView>(R.id.tempel)

        rename?.visibility = if (terpilih.size == 1) View.VISIBLE else View.GONE
        tempel?.visibility = if (fileUntukTempel.isNotEmpty()) View.VISIBLE else View.GONE
        lama.visibility = if (terpilih.isNotEmpty() || fileUntukTempel.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun PilihFile(file: File, centang: TextView?) {
        val papan = findViewById<GridLayout>(R.id.grid_papan)
        
        bersih.visibility = if (papan?.childCount ?: 0 > 0) View.VISIBLE else View.GONE
        
        if (terpilih.contains(file)) {
            terpilih.remove(file)
            centang?.visibility = View.GONE
            
            // Hapus dari papan horizontal
            papan?.let { layout ->
                for (i in 0 until layout.childCount) {
                    val item = layout.getChildAt(i)
                    val nama = item?.findViewById<TextView>(R.id.nama)
                    if (nama?.text == file.name) {
                        layout.removeView(item)
                        break
                    }
                }
            }
        } else {
            terpilih.add(file)
            centang?.visibility = View.VISIBLE
            
            val item = LayoutInflater.from(this).inflate(R.layout.item_horizontal, papan, false)
            val nama = item.findViewById<TextView>(R.id.nama)
            val gambar = item.findViewById<ImageView>(R.id.gambar)

            nama?.text = file.name
            gambar?.setImageResource(
                if (file.isDirectory) R.drawable.folder else R.drawable.file
            )
            papan?.addView(item)
        }

        UpdateTampilanTombol()
    }
    
    private fun Memotong() {
        if (terpilih.isEmpty()) {
            Toast.makeText(this, "Pilih file/folder untuk dipotong", Toast.LENGTH_SHORT).show()
            return
        }
        
        fileUntukTempel.clear()
        fileUntukTempel.addAll(terpilih)
        operasiSalin = false
        
        val jumlahFile = terpilih.size
        BersihkanSeleksi()
        
        Toast.makeText(this, "$jumlahFile file dipotong.", Toast.LENGTH_SHORT).show()
        UpdateTampilanTombol()
    }
    
    private fun Menyalin() {
        if (terpilih.isEmpty()) {
            Toast.makeText(this, "Pilih file/folder untuk disalin", Toast.LENGTH_SHORT).show()
            return
        }
        
        fileUntukTempel.clear()
        fileUntukTempel.addAll(terpilih)
        operasiSalin = true
        
        val jumlahFile = terpilih.size
        BersihkanSeleksi()
        
        Toast.makeText(this, "$jumlahFile file disalin.", Toast.LENGTH_SHORT).show()
        UpdateTampilanTombol()
    }
    
    private fun Meletakan() {
        if (fileUntukTempel.isEmpty()) {
            Toast.makeText(this, "Tidak ada file untuk ditempel.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!memo.exists()) {
            if (!memo.mkdirs()) {
                Toast.makeText(this, "Gagal membuat direktori", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!memo.canWrite()) {
            Toast.makeText(this, "Tidak ada izin menulis di direktori ini", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var sukses = 0
            var gagal = 0

            for (file in fileUntukTempel) {
                try {
                    val targetFile = File(memo, file.name)
                    
                    // Cek apakah file target sudah ada
                    if (targetFile.exists()) {
                        withContext(Dispatchers.Main) {
                            val shouldOverwrite = suspendCancellableCoroutine<Boolean> { cont ->
                                AlertDialog.Builder(this@Berkas)
                                    .setTitle("File sudah ada")
                                    .setMessage("${file.name} sudah ada. Timpa?")
                                    .setPositiveButton("Ya") { _, _ -> cont.resume(true) {} }
                                    .setNegativeButton("Tidak") { _, _ -> cont.resume(false) {} }
                                    .setOnCancelListener { cont.resume(false) {} }
                                    .show()
                            }
                            
                            if (!shouldOverwrite) {
                                gagal++
                                return@withContext
                            }
                        }
                    }
                    
                    if (file.isDirectory) { copyDirectory(file, targetFile) }
                    else { file.copyTo(targetFile, overwrite = true) }
                    
                    if (!operasiSalin) {
                        if (file.isDirectory) { file.deleteRecursively() }
                        else { file.delete() }
                    }

                    sukses++
                } catch (e: Exception) {
                    e.printStackTrace()
                    gagal++
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@Berkas,
                    if (operasiSalin) "Menyalin $sukses file, gagal $gagal"
                    else "Memindahkan $sukses file, gagal $gagal",
                    Toast.LENGTH_LONG
                ).show()

                fileUntukTempel.clear()
                UpdateTampilanTombol()
                Segarkan(memo)
            }
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                throw IOException("Gagal membuat direktori: ${destination.absolutePath}")
            }
        }
        
        source.listFiles()?.forEach { file ->
            val targetFile = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectory(file, targetFile)
            } else {
                file.copyTo(targetFile, overwrite = true)
            }
        }
    }
  
    private fun NamaiUlang() {
        if (terpilih.size != 1) {
            Toast.makeText(this, "Pilih tepat satu file/folder untuk rename", Toast.LENGTH_SHORT).show()
            return
        }

        val file = terpilih[0]
        val input = EditText(this)
        input.setText(file.name)
        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle("Ganti nama")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newName == file.name) {
                    Toast.makeText(this, "Nama sama dengan sebelumnya", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Validasi karakter yang tidak diizinkan
                if (newName.contains(Regex("[<>:\"/\\\\|?*]"))) {
                    Toast.makeText(this, "Nama mengandung karakter yang tidak diizinkan", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newFile = File(file.parentFile, newName)
                if (newFile.exists()) {
                    Toast.makeText(this, "File/folder dengan nama ini sudah ada", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sukses = file.renameTo(newFile)
                if (sukses) {
                    Toast.makeText(this, "Berhasil mengganti nama menjadi $newName", Toast.LENGTH_SHORT).show()
                    BersihkanSeleksi()
                } else {
                    Toast.makeText(this, "Gagal mengganti nama", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
  
    private fun HapusFile() {
        if (terpilih.isEmpty()) {
            Toast.makeText(this, "Pilih minimal satu file/folder untuk dihapus", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Hapus")
            .setMessage("Apakah kamu yakin ingin menghapus ${terpilih.size} item?")
            .setPositiveButton("Ya") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    var sukses = 0
                    var gagal = 0

                    for (file in terpilih) {
                        try {
                            if (file.isDirectory) {
                                if (file.deleteRecursively()) sukses++ else gagal++
                            } else {
                                if (file.delete()) sukses++ else gagal++
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            gagal++
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Berkas, "Berhasil: $sukses, Gagal: $gagal", Toast.LENGTH_LONG).show()
                        BersihkanSeleksi()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
  
    private fun MulaiZip() {
        if (terpilih.isEmpty()) {
            Toast.makeText(this, "Pilih file/folder untuk dijadikan zip", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this)
        input.setText("Archive.zip")
        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle("Nama file zip")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val zipName = input.text.toString().trim()
                if (zipName.isEmpty()) {
                    Toast.makeText(this, "Nama zip tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!zipName.endsWith(".zip", ignoreCase = true)) {
                    Toast.makeText(this, "Nama file harus berakhiran .zip", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val zipFile = File(memo, zipName)
                if (zipFile.exists()) {
                    AlertDialog.Builder(this)
                        .setTitle("File sudah ada")
                        .setMessage("$zipName sudah ada. Timpa?")
                        .setPositiveButton("Ya") { _, _ -> 
                            BuatZipDenganProgress(zipFile)
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                } else {
                    BuatZipDenganProgress(zipFile)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun BuatZipDenganProgress(zipFile: File) {
        val totalFiles = terpilih.sumOf { countFilesRecursively(it) }
        var processedFiles = 0

        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Membuat zip...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.max = 100
        progressDialog.setCancelable(true)
        progressDialog.setOnCancelListener {
            zipJob?.cancel()
            Toast.makeText(this, "Pembuatan zip dibatalkan", Toast.LENGTH_SHORT).show()
        }
        progressDialog.show()

        zipJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    for (file in terpilih) {
                        if (!isActive) break
                        
                        addFileToZipWithProgress(file, file.name, zos) { processed ->
                            processedFiles += processed
                            val percent = if (totalFiles > 0) (processedFiles * 100 / totalFiles) else 0
                            
                            runOnUiThread {
                                if (progressDialog.isShowing) {
                                    progressDialog.progress = percent
                                    progressDialog.setMessage("Memproses: ${file.name} ($percent%)")
                                }
                            }
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    if (isActive) {
                        Toast.makeText(this@Berkas, "Berhasil membuat ${zipFile.name}", Toast.LENGTH_SHORT).show()
                        BersihkanSeleksi()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    if (isActive) {
                        Toast.makeText(this@Berkas, "Gagal membuat zip: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    if (zipFile.exists()) {
                        zipFile.delete()
                    }
                }
            }
        }
    }

    private fun addFileToZipWithProgress(
        file: File, 
        entryName: String, 
        zos: ZipOutputStream, 
        progressCallback: (processed: Int) -> Unit
    ) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addFileToZipWithProgress(child, "$entryName/${child.name}", zos, progressCallback)
            }
        } else {
            try {
                FileInputStream(file).use { fis ->
                    val entry = ZipEntry(entryName)
                    entry.time = file.lastModified()
                    zos.putNextEntry(entry)
                    
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (fis.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                    
                    zos.closeEntry()
                    progressCallback(1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                progressCallback(1)
            }
        }
    }

    private fun countFilesRecursively(file: File): Int {
        return if (file.isDirectory) {
            file.listFiles()?.sumOf { countFilesRecursively(it) } ?: 0
        } else 1
    }

    override fun onDestroy() {
        super.onDestroy()
        zipJob?.cancel()
    }
}