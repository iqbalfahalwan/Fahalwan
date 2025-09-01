package iqbal.fahalwan

import android.content.*
import android.widget.*
import androidx.appcompat.app.*
import java.io.*
import android.os.*

class Musik : AppCompatActivity(){
  override fun onCreate(savedInstanceState:Bundle?){
    super.onCreate(savedInstanceState)
    
    val terima = intent.getStringExtra("file")
    if(terima == null ){ Keluar() }
    else{ Dialog(File(terima)) }
  }
  
  private fun Keluar(){
    AlertDialog.Builder(this)
      .setTitle("Keluar")
      .setMessage("Meninggalkan Musik ?")
      .setPositiveButton("Ya"){_,_-> finish()}
      .setNegativeButton("Tidak", null)
      .show()
  }
  
  override fun onBackPressed(){
    Keluar()
  }
  
  private fun Dialog(terima:File){
    AlertDialog.Builder(this)
      .setTitle("Belum Siap")
      .setMessage("${terima.name}")
      .setPositiveButton("Oke"){_,_-> Keluar()}
      .setNegativeButton("batal", null)
      .show()
  }
}
