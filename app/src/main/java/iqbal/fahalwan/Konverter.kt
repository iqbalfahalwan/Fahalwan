package iqbal.fahalwan

import android.widget.*
import androidx.appcompat.app.*
import android.os.*
import android.view.*
import com.bumptech.glide.Glide
import java.io.*

class Konverter : AppCompatActivity() {

    private lateinit var liner: LinearLayout
    private lateinit var linerLink: LinearLayout
    private lateinit var linerHasil: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ytmp3)

        liner = findViewById(R.id.liner)
        linerHasil = findViewById(R.id.liner_hasil)
        linerLink = findViewById(R.id.liner_link)

        Tombol()
        AturTulis()

        linerHasil.visibility = View.GONE
        linerLink.visibility = View.VISIBLE


    }

    private fun Keluar() {
        AlertDialog.Builder(this)
            .setMessage("Meninggalkan Konverter ?")
            .setTitle("Keluar")
            .setPositiveButton("Ya") { _, _ -> finish() }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onBackPressed() {
        if (liner.visibility == View.VISIBLE) {
            liner.visibility = View.GONE
        } else {
            Keluar()
        }
    }

    private fun Tombol() {
        findViewById<TextView>(R.id.keluar).setOnClickListener {
            Keluar()
        }

        findViewById<ImageView>(R.id.nav).setOnClickListener {
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun AturTulis() {
        val editLink = findViewById<EditText>(R.id.tulis_link)

        findViewById<TextView>(R.id.cari_link).setOnClickListener {
            val link = editLink.text.toString().trim()
            if (link.isNotEmpty()) {
                CariLink(link)
            } else {
                Toast.makeText(this, "Masukkan link dulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractVideoId(url: String): String? {
        val regex = Regex("(?:v=|\\/)([0-9A-Za-z_-]{11}).*")
        val match = regex.find(url)
        return match?.groups?.get(1)?.value
    }
    
    private fun CariLink(link: String) {
    linerLink.visibility = View.GONE
    linerHasil.visibility = View.VISIBLE

    val gambar = findViewById<ImageView>(R.id.gambar_hasil)
    val spin = findViewById<Spinner>(R.id.spin_hasil)
    val download = findViewById<TextView>(R.id.download_hasil)
    val judul = findViewById<TextView>(R.id.judul_hasil)

    val videoId = extractVideoId(link)

    if (videoId == null) {
        Toast.makeText(this, "Link tidak valid", Toast.LENGTH_SHORT).show()
        linerLink.visibility = View.VISIBLE
        linerHasil.visibility = View.GONE
        return
    }

    val thumbUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"

    Glide.with(this).load(thumbUrl).into(gambar)

    judul.text = "Video ID: $videoId"

    spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>, view: View?, position: Int, id: Long
        ) {
            download.visibility = View.VISIBLE
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            download.visibility = View.GONE
        }
    }
    
  }

}