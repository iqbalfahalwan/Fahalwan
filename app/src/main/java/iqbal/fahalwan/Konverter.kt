package iqbal.fahalwan

import android.widget.*
import androidx.appcompat.app.*
import android.os.*

class Konverter : AppCompatActivity(){
  override fun onCreate(savedInstanceState:Bundle?){
    super.onCreate(savedInstanceState)
    
    
  }
  
  private fun Keluar(){
    AlertDialog.Builder(this)
      .setMessage("Meninggalkan Konverter ?")
      .setTitle("Keluar")
      .setPositiveButton("Ya"){_,_-> finish()}
      .setNegativeButton("Tidak", null)
      .show()
  }
  
  override fun onBackPressed(){
    Keluar()
  }
}
