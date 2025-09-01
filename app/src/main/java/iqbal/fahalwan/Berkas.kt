package iqbal.fahalwan

import android.app.AlertDialog
import android.content.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import java.io.*
import java.util.zip.*
import kotlinx.coroutines.*
import iqbal.fahalwan.kompres.ZipKompresor
import iqbal.fahalwan.kompres.GzKompresor
import iqbal.fahalwan.kompres.TarKompresor
import com.google.android.gms.ads.*
import com.google.android.gms.*
import com.google.android.gms.ads.rewarded.*

class Berkas : AppCompatActivity() {

    private lateinit var memo: File
    private lateinit var jalur: TextView
    private lateinit var liner: LinearLayout
    private lateinit var lama: LinearLayout
    private lateinit var bersih: ImageView
    private var terpilih: MutableList<File> = mutableListOf()
    private var aksiPotongSalin = false // true = salin, false = potong
    private var buffer: MutableList<File> = mutableListOf()
    private var jobKompres: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.berkas)

        jalur = findViewById(R.id.jalur)
        liner = findViewById(R.id.liner)
        lama = findViewById(R.id.lama)
        bersih = findViewById(R.id.bersih)

        memo = Environment.getExternalStorageDirectory()
        Segarkan(memo)
        Tombol()
        Iklan()
    }
    
    private fun Iklan(){
      val IN = findViewById<AdView>(R.id.iklan_nav)
      val IU = findViewById<AdView>(R.id.iklan_utama)
      val iklan = AdRequest.Builder().build()
      IN.loadAd(iklan)
      IU.loadAd(iklan)
    }

    private fun Tombol() {
        findViewById<TextView>(R.id.keluar).setOnClickListener {
            Keluar()
        }

        findViewById<ImageView>(R.id.nav).setOnClickListener {
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        findViewById<ImageView>(R.id.apk).setOnClickListener {
            memo = filesDir
            Segarkan(memo)
            liner.visibility = View.GONE
        }

        findViewById<ImageView>(R.id.sd).setOnClickListener {
            memo = Environment.getExternalStorageDirectory()
            Segarkan(memo)
            liner.visibility = View.GONE
        }

        bersih.setOnClickListener {
            terpilih.clear()
            findViewById<GridLayout>(R.id.grid_papan).removeAllViews()
            lama.visibility = View.GONE
            bersih.visibility = View.GONE
            Segarkan(memo)
        }
        
        jalur.setOnClickListener{
          TulisJalur()
        }
        
        findViewById<ImageView>(R.id.baru).setOnClickListener {
          BuatBaru()
        }
    }

    private fun Keluar() {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Dari Berkas ?")
            .setPositiveButton("Ya") { _, _ -> finish() }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onBackPressed() {
        if (liner.visibility == View.VISIBLE) { 
            liner.visibility = View.GONE 
        } else {
            if(terpilih.isEmpty()){
                val target = memo.parentFile
                if (target == null) { 
                    Toast.makeText(this, "Root Directory", Toast.LENGTH_SHORT).show() 
                } else { 
                    Segarkan(target) 
                }
            } else {
                terpilih.clear()
                findViewById<GridLayout>(R.id.grid_papan).removeAllViews()
                lama.visibility = View.GONE
                bersih.visibility = View.GONE
                Segarkan(memo)
            }
        }
    }

    private fun Segarkan(folder: File) {
        memo = folder
        jalur.text = folder.absolutePath
        AturGrid(folder)
    }

    private fun AturGrid(folder: File) {
        val grid = findViewById<GridLayout>(R.id.grid_utama)
        grid.removeAllViews()

        val files = folder.listFiles() ?: return

        for (file in files) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_vertical, grid, false)
            val nama = item.findViewById<TextView>(R.id.nama)
            val gambar = item.findViewById<ImageView>(R.id.gambar)
            val centang = item.findViewById<TextView>(R.id.centang)

            nama.text = file.name
            centang.visibility = if (terpilih.contains(file)) View.VISIBLE else View.GONE

            val tipe = AmbilTipeFile(file)
            gambar.setImageResource(if (file.isDirectory) R.drawable.folder else tipe)

            item.setOnClickListener {
              if (file.isDirectory) Segarkan(file)
              else Toast.makeText(this, "File: ${file.name}", Toast.LENGTH_SHORT).show()
            }

            item.setOnLongClickListener {
                TogglePilih(file)
                PerbaruiTampilan()
                true
            }

            grid.addView(item)
        }
    }

    private fun AmbilTipeFile(file: File): Int {
        return when (file.extension.lowercase()) {
            "txt" -> R.drawable.txt
            "java" -> R.drawable.java
            "c", "cpp", "h" -> R.drawable.c
            "kts", "gradle" -> R.drawable.gradle
            "tar","rar","zip","gz","xz","pac","7z" -> R.drawable.zip
            "xml" -> R.drawable.xml
            "kt" -> R.drawable.kotlin
            "py" -> R.drawable.python
            else -> R.drawable.file
        }
    }

    private fun TogglePilih(target: File) {
        val grid = findViewById<GridLayout>(R.id.grid_papan)

        if (terpilih.contains(target)) {
            terpilih.remove(target)
            for (i in 0 until grid.childCount) {
                val v = grid.getChildAt(i)
                val nama = v.findViewById<TextView>(R.id.nama)
                if (nama.text == target.name) {
                    grid.removeView(v)
                    break
                }
            }
        } else {
            terpilih.add(target)
            val item = LayoutInflater.from(this).inflate(R.layout.item_horizontal, grid, false)
            val nama = item.findViewById<TextView>(R.id.nama)
            val gambar = item.findViewById<ImageView>(R.id.gambar)

            val tipe = AmbilTipeFile(target)
            gambar.setImageResource(if (target.isDirectory) R.drawable.folder else tipe)
            nama.text = target.name

            grid.addView(item)
        }

        lama.visibility = if (terpilih.isEmpty()) View.GONE else View.VISIBLE
        bersih.visibility = if (terpilih.isEmpty()) View.GONE else View.VISIBLE
        
        if (terpilih.isNotEmpty()) {
            TekanLama()
        }
    }

    private fun PerbaruiTampilan() {
        val gridUtama = findViewById<GridLayout>(R.id.grid_utama)
        for (i in 0 until gridUtama.childCount) {
            val item = gridUtama.getChildAt(i)
            val nama = item.findViewById<TextView>(R.id.nama)
            val centang = item.findViewById<TextView>(R.id.centang)
            
            val namaFile = nama.text.toString()
            val fileExists = terpilih.any { it.name == namaFile }
            centang.visibility = if (fileExists) View.VISIBLE else View.GONE
        }
    }

    private fun TekanLama() {
        val tempel = findViewById<ImageView>(R.id.tempel)
        val rename = findViewById<ImageView>(R.id.rename)
        val salin = findViewById<ImageView>(R.id.salin)
        val potong = findViewById<ImageView>(R.id.potong)

        tempel.visibility = if (buffer.isNotEmpty()) View.VISIBLE else View.GONE
        rename.visibility = if (terpilih.size == 1) View.VISIBLE else View.GONE
        
        salin.setOnClickListener { AksiSalin() }
        potong.setOnClickListener { AksiPotong() }
        tempel.setOnClickListener { Menempel() }
        rename.setOnClickListener{ NamaiUlang() }
        findViewById<ImageView>(R.id.hapus).setOnClickListener{ Menghapus() }
        findViewById<ImageView>(R.id.zip).setOnClickListener{ Kompres() }
    }

    private fun AksiPotong() {
        buffer.clear()
        buffer.addAll(terpilih)
        aksiPotongSalin = false
        Toast.makeText(this, "Terpilih Akan DiPOTONG", Toast.LENGTH_SHORT).show()
        
        val tempel = findViewById<ImageView>(R.id.tempel)
        tempel.visibility = View.VISIBLE
    }

    private fun AksiSalin() {
        buffer.clear()
        buffer.addAll(terpilih)
        aksiPotongSalin = true
        Toast.makeText(this, "Terpilih Akan DiSALIN", Toast.LENGTH_SHORT).show()
        
        val tempel = findViewById<ImageView>(R.id.tempel)
        tempel.visibility = View.VISIBLE
    }
    
    private fun Menempel() {
    if (buffer.isEmpty()) {
        Toast.makeText(this, "Tidak ada file untuk ditempel", Toast.LENGTH_SHORT).show()
        return
    }

    // Cek jika target adalah filesDir (direktori internal aplikasi)
    val isTargetInternal = memo.absolutePath == filesDir.absolutePath
    
    try {
        buffer.forEach { file ->
            val target = File(memo, file.name)
            
            // Jika mencoba menempel ke internal storage, pastikan kita punya akses
            if (isTargetInternal) {
                // Gunakan context untuk mendapatkan path internal yang benar
                val internalTarget = File(filesDir, file.name)
                if (aksiPotongSalin) {
                    SalinFile(file, internalTarget)
                } else {
                    // Untuk move ke internal, kita harus copy lalu delete
                    if (SalinFile(file, internalTarget)) {
                        file.delete() // Hapus file asli setelah berhasil copy
                    }
                }
            } else {
                if (target.exists() && target.absolutePath != file.absolutePath) {
                    val namaBase = file.nameWithoutExtension
                    val ekstensi = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
                    var counter = 1
                    var targetBaru = File(memo, "${namaBase}_copy$ekstensi")
                    
                    while (targetBaru.exists()) {
                        counter++
                        targetBaru = File(memo, "${namaBase}_copy$counter$ekstensi")
                    }
                    
                    if (aksiPotongSalin) {
                        SalinFile(file, targetBaru)
                    } else {
                        file.renameTo(targetBaru)
                    }
                } else {
                    if (aksiPotongSalin) {
                        SalinFile(file, target)
                    } else {
                        if (file.absolutePath != target.absolutePath) {
                            file.renameTo(target)
                        }
                    }
                }
            }
        }
        
        buffer.clear()
        terpilih.clear()
        findViewById<GridLayout>(R.id.grid_papan).removeAllViews()
        lama.visibility = View.GONE
        bersih.visibility = View.GONE
        findViewById<ImageView>(R.id.tempel).visibility = View.GONE
        
        Segarkan(memo)
        Toast.makeText(this, "File DILETAKAN", Toast.LENGTH_SHORT).show()
        
    } catch (e: Exception) {
        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

    private fun SalinFile(sumber: File, tujuan: File): Boolean {
    return try {
        if (sumber.isDirectory) {
            SalinFolder(sumber, tujuan)
            true
        } else {
            sumber.copyTo(tujuan, overwrite = true)
            true
        }
    } catch (e: Exception) {
        Toast.makeText(this, "Gagal menyalin ${sumber.name}: ${e.message}", Toast.LENGTH_SHORT).show()
        false
      }
    }

    private fun SalinFolder(sumber: File, tujuan: File) {
        if (!tujuan.exists()) {
            tujuan.mkdirs()
        }
        
        sumber.listFiles()?.forEach { file ->
            val targetFile = File(tujuan, file.name)
            if (file.isDirectory) {
                SalinFolder(file, targetFile)
            } else {
                file.copyTo(targetFile, overwrite = true)
            }
        }
    }
    
    private fun Menghapus(){
      AlertDialog.Builder(this)
        .setTitle("Menghapus File")
        .setMessage("FILE TERPILIH AKAN DIHAPUS ?")
        .setPositiveButton("Ya"){_,_-> SetujuiHapus()}
        .setNegativeButton("Tidak Yakin", null)
        .show()
    }
    
    private fun SetujuiHapus() {
        if (terpilih.isEmpty()) {
            Toast.makeText(this, "Tidak ada file untuk dihapus", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            var berhasil = 0
            var gagal = 0
            
            terpilih.forEach { file ->
                if (HapusFile(file)) {
                    berhasil++
                } else {
                    gagal++
                }
            }
            
            terpilih.clear()
            findViewById<GridLayout>(R.id.grid_papan).removeAllViews()
            lama.visibility = View.GONE
            bersih.visibility = View.GONE
            
            Segarkan(memo)
            
            val pesan = when {
                gagal == 0 -> "$berhasil file berhasil dihapus"
                berhasil == 0 -> "Gagal menghapus $gagal file"
                else -> "$berhasil berhasil, $gagal gagal dihapus"
            }
            
            Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun HapusFile(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                HapusFolder(file)
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun HapusFolder(folder: File): Boolean {
        return try {
            folder.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    if (!HapusFolder(file)) return false
                } else {
                    if (!file.delete()) return false
                }
            }
            folder.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun NamaiUlang() {
    if (terpilih.size != 1) {
        Toast.makeText(this, "Pilih satu file saja untuk rename", Toast.LENGTH_SHORT).show()
        return
    }

    val file = terpilih.first()
    val tulis = EditText(this).apply {
        setText(file.name)
    }

    AlertDialog.Builder(this)
        .setTitle("Mengganti Nama")
        .setView(tulis)
        .setPositiveButton("Simpan") { _, _ ->
            val namaBaru = tulis.text.toString().trim()
            if (namaBaru.isNotEmpty()) {
                val target = File(file.parent, namaBaru)
                if (file.renameTo(target)) {
                    Toast.makeText(this, "Nama berhasil diganti", Toast.LENGTH_SHORT).show()
                    terpilih.clear()
                    findViewById<GridLayout>(R.id.grid_papan).removeAllViews()
                    lama.visibility = View.GONE
                    bersih.visibility = View.GONE
                    Segarkan(memo)
                } else {
                    Toast.makeText(this, "Gagal mengganti nama", Toast.LENGTH_SHORT).show()
                }
            }
        }
        .setNegativeButton("Batal", null)
        .show()
  }
  
  private fun TulisJalur() {
    val tulis = EditText(this).apply { setText(memo.absolutePath) }

    AlertDialog.Builder(this)
        .setTitle("Menulis Jalur")
        .setView(tulis)
        .setPositiveButton("Pergi") { _, _ ->
            val target = File(tulis.text.toString().trim())
            if (!target.exists()) {
                Toast.makeText(this, "Jalur Tidak Ada", Toast.LENGTH_SHORT).show()
            } else {
              if(target.isDirectory){ Segarkan(target) }
              else{ Toast.makeText(this, "Hanya Menerima Folder",Toast.LENGTH_SHORT).show()}
            }
        }
        .setNegativeButton("Batal", null)
        .show()
  }
  
  private fun Kompres() {
    val item = arrayOf("Zip", "Gz", "7z")
    AlertDialog.Builder(this)
        .setTitle("Opsi Kompresi")
        .setItems(item) { _, which ->
            when (which) {
                0 -> PilihanZip()
                1 -> PilihanGz()
                2 -> Pilihan7z()
            }
        }
        .setNegativeButton("Batal", null)
        .show()
  }
  
  private fun PilihanZip() {
        if (terpilih.isEmpty()) {
            Toast.makeText(this, "Pilih file/folder dulu", Toast.LENGTH_SHORT).show()
            return
        }

        val output = File(memo, "arsip.zip")
        val (popup, view) = ProsesAksi()

        val statusAksi = view.findViewById<TextView>(R.id.status_aksi)
        val namaProses = view.findViewById<TextView>(R.id.nama_proses)
        val persen = view.findViewById<TextView>(R.id.persen)
        val load = view.findViewById<ProgressBar>(R.id.load)

        jobKompres = CoroutineScope(Dispatchers.IO).launch {
            val berhasil = ZipKompresor.kompres(
                terpilih,
                output,
                object : ZipKompresor.ProgressListener {
                    override fun onProgress(fileName: String, progress: Int) {
                        runOnUiThread {
                            statusAksi.text = "Mengompres..."
                            namaProses.text = fileName
                            persen.text = "$progress%"
                            load.progress = progress
                        }
                    }
                }
            )

            runOnUiThread {
                popup.dismiss()
                if (berhasil) {
                    Toast.makeText(this@Berkas, "ZIP berhasil: ${output.name}", Toast.LENGTH_SHORT).show()
                    Segarkan(memo)
                } else {
                    Toast.makeText(this@Berkas, "ZIP gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun ProsesAksi(): Pair<PopupWindow, View> {
        val view = layoutInflater.inflate(R.layout.proses_aksi, null)

        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val batal = view.findViewById<Button>(R.id.batal)
        val sembunyi = view.findViewById<Button>(R.id.sembunyi)
        val load = view.findViewById<ProgressBar>(R.id.load) //mirip load browser

        batal.setOnClickListener {
            jobKompres?.cancel()
            popup.dismiss()
            Toast.makeText(this, "Dibatalkan", Toast.LENGTH_SHORT).show()
        }

        sembunyi.setOnClickListener {
            popup.dismiss()
        }

        popup.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0)

        return popup to view
    }

    private fun PilihanGz() {
        if (terpilih.isEmpty()) {
            Toast.makeText(this, "Pilih file/folder dulu", Toast.LENGTH_SHORT).show()
            return
        }

        val outputTar = File(memo, "arsip.tar")
        val outputGz = File(memo, "arsip.tar.gz")

        val (popup, view) = ProsesAksi()
        val persen = view.findViewById<TextView>(R.id.persen)
        val load = view.findViewById<ProgressBar>(R.id.load)

        jobKompres = CoroutineScope(Dispatchers.IO).launch {
            val tarBerhasil = TarKompresor.arsipkan(terpilih, outputTar, object : TarKompresor.ProgressListener {
                override fun onProgress(fileName: String) {
                    runOnUiThread {
                        persen.text = "Tar: $fileName"
                    }
                }
            })

            if (!tarBerhasil) {
                runOnUiThread {
                    popup.dismiss()
                    Toast.makeText(this@Berkas, "Gagal bikin TAR", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val gzBerhasil = GzKompresor.kompres(outputTar, outputGz, object : GzKompresor.ProgressListener {
                override fun onProgress(progress: Int) {
                    runOnUiThread {
                        load.progress = progress
                        persen.text = "$progress%"
                    }
                }
            })

            outputTar.delete()

            runOnUiThread {
                popup.dismiss()
                if (gzBerhasil) {
                    Toast.makeText(this@Berkas, "TAR.GZ berhasil: ${outputGz.name}", Toast.LENGTH_SHORT).show()
                    Segarkan(memo)
                } else {
                    Toast.makeText(this@Berkas, "TAR.GZ gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun Pilihan7z() {
        Toast.makeText(this, "Belum Siap", Toast.LENGTH_SHORT).show()
    }
    
    private fun BuatBaru(){
        AlertDialog.Builder(this)
            .setTitle("Pilih Opsi")
            .setItems(arrayOf("Folder", "File")) { _, which ->
                when(which) {
                    0 -> MulaiBuat(true)
                    1 -> MulaiBuat(false)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun MulaiBuat(isFolder: Boolean) {
        val tulis = EditText(this).apply {
            setText(if (isFolder) "Folder Baru" else "file.txt")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Masukan nama")
            .setView(tulis)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val namaBaru = tulis.text.toString().trim()
                if (namaBaru.isNotEmpty()) {
                    val fileBaru = File(memo, namaBaru)
                    try {
                        if (isFolder) {
                            if (fileBaru.mkdirs()) {
                                Toast.makeText(this, "Folder berhasil dibuat", Toast.LENGTH_SHORT).show()
                                Segarkan(memo)
                            } else {
                                Toast.makeText(this, "Gagal membuat folder", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (fileBaru.createNewFile()) {
                                Toast.makeText(this, "File berhasil dibuat", Toast.LENGTH_SHORT).show()
                                Segarkan(memo)
                            } else {
                                Toast.makeText(this, "Gagal membuat file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: IOException) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
}