package iqbal.fahalwan

import java.io.File
import androidx.appcompat.app.*
import android.widget.*
import android.os.*
import android.view.*

class Terminal : AppCompatActivity() {

  private lateinit var liner:LinearLayout
  private lateinit var tulis:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terminal)
        
        liner = findViewById(R.id.liner)
        tulis = findViewById(R.id.tulis)
        
        findViewById<ImageView>(R.id.nav).setOnClickListener{
          liner.visibility = if(liner.visibility == View.GONE)View.VISIBLE else View.GONE
        }
        
        findViewById<TextView>(R.id.kirim).setOnClickListener{
          MulaiTerminal()
          tulis.setText("")
        }
        
        findViewById<TextView>(R.id.keluar).setOnClickListener{
          Keluar()
        }
    }

    private fun Keluar() {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Meninggalkan Terminal ?")
            .setPositiveButton("Ya") { _, _ -> finish() }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onBackPressed() {
        if(liner.visibility == View.VISIBLE){liner.visibility = View.GONE}
        else{ Keluar() }
    }

    private fun MulaiTerminal(){
      val baca = tulis.text.toString()
      val hasil = findViewById<TextView>(R.id.hasil)
      
      if(baca == "clear"){hasil.text = ""}
      if(baca == "exit"){ Keluar() }
      else{
        System.loadLibrary("proses")
        hasil.append("\n${baca}\n${Proses(baca)}")
      }
    }
    
    external fun Proses(baca:String):String
}