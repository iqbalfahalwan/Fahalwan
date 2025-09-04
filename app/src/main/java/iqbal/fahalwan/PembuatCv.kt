package iqbal.fahalwan

import android.widget.*
import android.os.*
import androidx.appcompat.app.*
import android.view.*
import androidx.core.widget.doAfterTextChanged
import android.text.Editable
import android.text.TextWatcher
import android.content.*
import java.io.*
import android.net.*
import android.graphics.pdf.PdfDocument
import com.google.android.gms.ads.*
import com.google.android.gms.*
import com.google.android.gms.ads.rewarded.*

class PembuatCv : AppCompatActivity(){

    private lateinit var liner:LinearLayout

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembuat_cv)
        
        liner = findViewById(R.id.liner)
        
        Tombol()
        LihatLangsung()
        Iklan()
    }
    
    private fun Iklan(){
        val IN = findViewById<AdView>(R.id.iklan_nav)
        val IU = findViewById<AdView>(R.id.iklan_utama)
        val iklan = AdRequest.Builder().build()
        IN.loadAd(iklan)
        IU.loadAd(iklan)
    }
    
    private fun Keluar(){
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Meninggalkan Cv Maker ?")
            .setNegativeButton("Tidak", null)
            .setPositiveButton("Ya"){_,_-> finish()}
            .show()
    }
    
    override fun onBackPressed(){
        if(liner.visibility == View.VISIBLE){liner.visibility = View.GONE}
        else{ Keluar() }
    }
    
    private fun Tombol(){
        findViewById<TextView>(R.id.keluar).setOnClickListener{
            Keluar()
        }
        
        findViewById<ImageView>(R.id.nav).setOnClickListener{
            liner.visibility = if(liner.visibility == View.GONE)View.VISIBLE else View.GONE
        }
        
        findViewById<ImageView>(R.id.simpan).setOnClickListener{
            SimpanPdf()
        }
        
        findViewById<ImageView>(R.id.foto).setOnClickListener{
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
            liner.visibility = View.GONE
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 100 && resultCode == RESULT_OK) {
        val uri = data?.data
        val fotoOut = findViewById<ImageView>(R.id.foto_out)
        fotoOut.setImageURI(uri)
        }
    }
    
    private fun SimpanPdf() {
    val utama = findViewById<LinearLayout>(R.id.utama)
    val tulis = EditText(this)
    tulis.hint = "Nama file"

    AlertDialog.Builder(this)
        .setTitle("Memberi nama")
        .setView(tulis)
        .setPositiveButton("Simpan") { _, _ ->
            val namaFile = tulis.text.toString().ifEmpty { "layout" }
            saveLayoutAsPdf(utama, "$namaFile.pdf")
        }
        .setNegativeButton("Batal", null)
        .show()
    }

    private fun saveLayoutAsPdf(layout: LinearLayout, fileName: String) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
            layout.width, layout.height, 1
            ).create()
        val page = pdfDocument.startPage(pageInfo)

            layout.draw(page.canvas)
            pdfDocument.finishPage(page)

        val file = java.io.File(Environment.getExternalStorageDirectory(), "/Documents/${fileName}")
        try {
            pdfDocument.writeTo(java.io.FileOutputStream(file))
            Toast.makeText(this, "File tersimpan di: ${file.path}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
        }
            pdfDocument.close()
    }
    
    private fun LihatLangsung(){
        
        val nama = findViewById<EditText>(R.id.nama)
        val wa = findViewById<EditText>(R.id.wa)
        val telp = findViewById<EditText>(R.id.telp)
        val email = findViewById<EditText>(R.id.email)
        val ig = findViewById<EditText>(R.id.ig)
        val ttl = findViewById<EditText>(R.id.ttl)
        val usia = findViewById<EditText>(R.id.usia)
        val agama = findViewById<EditText>(R.id.agama)
        val domisili = findViewById<EditText>(R.id.domisili)
        val hobi = findViewById<EditText>(R.id.hobi)
        val jurusan = findViewById<EditText>(R.id.jurusan)
        val sd = findViewById<EditText>(R.id.sd)
        val smp = findViewById<EditText>(R.id.smp)
        val sma = findViewById<EditText>(R.id.sma)
        val uni = findViewById<EditText>(R.id.uni)
        val pengalaman = findViewById<EditText>(R.id.pengalaman)
        val skill = findViewById<EditText>(R.id.skill)
        val karakter = findViewById<EditText>(R.id.karakter)
        
        
        val namaOut = findViewById<TextView>(R.id.nama_out)
        val waOut = findViewById<TextView>(R.id.wa_out)
        val telpOut = findViewById<TextView>(R.id.telp_out)
        val emailOut = findViewById<TextView>(R.id.email_out)
        val igOut = findViewById<TextView>(R.id.ig_out)
        val ttlOut = findViewById<TextView>(R.id.ttl_out)
        val usiaOut = findViewById<TextView>(R.id.usia_out)
        val agamaOut = findViewById<TextView>(R.id.agama_out)
        val domisiliOut = findViewById<TextView>(R.id.domisili_out)
        val hobiOut = findViewById<TextView>(R.id.hobi_out)
        val jurusanOut = findViewById<TextView>(R.id.jurusan_out)
        val sdOut = findViewById<TextView>(R.id.sd_out)
        val smpOut = findViewById<TextView>(R.id.smp_out)
        val smaOut = findViewById<TextView>(R.id.sma_out)
        val uniOut = findViewById<TextView>(R.id.uni_out)
        val pengalamanOut = findViewById<TextView>(R.id.pengalaman_out)
        val skillOut = findViewById<TextView>(R.id.skill_out)
        val karakterOut = findViewById<TextView>(R.id.karakter_out)

        nama.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { namaOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        wa.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { waOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        telp.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { telpOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        email.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { emailOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        ig.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { igOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        ttl.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { ttlOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        usia.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { usiaOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        agama.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { agamaOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        domisili.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { domisiliOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        hobi.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { hobiOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        jurusan.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { jurusanOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        sd.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { sdOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        smp.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { smpOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        sma.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { smaOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        uni.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { uniOut.text = ": ${s.toString()}" }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        pengalaman.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { pengalamanOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        skill.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { skillOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        karakter.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { karakterOut.text = s.toString() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
    }
    
}
