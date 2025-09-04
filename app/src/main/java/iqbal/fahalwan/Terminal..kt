package iqbal.fahalwan

import java.io.File
import androidx.appcompat.app.*
import android.widget.*
import android.os.*
import java.util.*
import android.util.*
import android.view.*
import android.view.inputmethod.EditorInfo
import com.google.android.gms.ads.*
import com.google.android.gms.*
import com.google.android.gms.ads.rewarded.*
import kotlinx.coroutines.*

class Terminal : AppCompatActivity() {

    companion object {
        init {
            try {
                System.loadLibrary("proses")
                Log.d("Terminal", "Library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("Terminal", "Failed to load library: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("Terminal", "Error loading library: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private lateinit var liner: LinearLayout
    private lateinit var tulis: EditText
    private lateinit var hasil: TextView
    private lateinit var scrollView: ScrollView
    
    private var isProcessing = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terminal)
        
        initViews()
        setupListeners()
        checkEnvironment()
        Iklan()
    }
    
    private fun initViews() {
        liner = findViewById(R.id.liner)
        tulis = findViewById(R.id.tulis)
        hasil = findViewById(R.id.hasil)
        scrollView = findViewById(R.id.scroll)
        
        hasil.text = "🐧 Terminal Android Ready\nType 'help' untuk bantuan\n\n"
    }
    
    private fun setupListeners() {
        findViewById<ImageView>(R.id.nav).setOnClickListener {
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        
        findViewById<TextView>(R.id.kirim).setOnClickListener {
        val mode = findViewById<Switch>(R.id.mode)
        if (mode.isChecked) {
            PakaiSistem()
            } else {
            MulaiTerminal()
            }
        }
        
        findViewById<TextView>(R.id.keluar).setOnClickListener {
            Keluar()
        }
        
        tulis.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                MulaiTerminal()
                true
            } else {
                false
            }
        }
        
        hasil.setOnClickListener {
            tulis.requestFocus()
        }
    }
    
    private fun checkEnvironment() {
        scope.launch(Dispatchers.IO) {
            try {
                val isReady = IsEnvironmentReady()
                withContext(Dispatchers.Main) {
                    if (!isReady) {
                        hasil.append("⚠️  Warning: Proot environment belum siap\n")
                        hasil.append("Pastikan file proot dan ubuntu sudah terinstall\n\n")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hasil.append("❌ Error checking environment: ${e.message}\n\n")
                }
            }
        }
    }
    
    private fun Iklan() {
        try {
            val IN = findViewById<AdView>(R.id.iklan_nav)
            val IU = findViewById<AdView>(R.id.iklan_utama)
            val iklan = AdRequest.Builder().build()
            IN.loadAd(iklan)
            IU.loadAd(iklan)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Keluar() {
        if (isProcessing) {
            AlertDialog.Builder(this)
                .setTitle("Terminal Sedang Berjalan")
                .setMessage("Ada command yang sedang berjalan. Tetap keluar?")
                .setPositiveButton("Ya") { _, _ -> 
                    InterruptCommand()
                    finish() 
                }
                .setNegativeButton("Tidak", null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Keluar")
                .setMessage("Meninggalkan Terminal ?")
                .setPositiveButton("Ya") { _, _ -> finish() }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    override fun onBackPressed() {
        if (liner.visibility == View.VISIBLE) {
            liner.visibility = View.GONE
        } else if (isProcessing) {
            AlertDialog.Builder(this)
                .setTitle("Interrupt Command")
                .setMessage("Command sedang berjalan. Stop command?")
                .setPositiveButton("Stop") { _, _ -> 
                    InterruptCommand()
                    isProcessing = false
                    hasil.append("\n🛑 Command interrupted\n\n")
                }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            Keluar()
        }
    }

    private fun MulaiTerminal() {
        val baca = tulis.text.toString().trim()
        
        if (baca.isEmpty()) return
        
        if (isProcessing) {
            Toast.makeText(this, "Command sedang berjalan...", Toast.LENGTH_SHORT).show()
            return
        }
        
        tulis.setText("")
        
        when (baca.lowercase()) {
            "clear" -> {
                hasil.text = ""
                return
            }
            "exit" -> {
                Keluar()
                return
            }
            "help" -> {
                showHelp()
                return
            }
            "interrupt" -> {
                InterruptCommand()
                hasil.append("🛑 Interrupt signal sent\n\n")
                return
            }
        }
        
        executeCommand(baca)
    }
    
    private fun showHelp() {
        val helpText = """
            📖 Built-in Commands:
            • clear     - Clear terminal screen
            • exit      - Exit terminal
            • help      - Show this help
            • interrupt - Stop running command
            
            🐧 Linux Commands:
            • ls        - List files
            • pwd       - Current directory
            • whoami    - Current user
            • date      - Current date/time
            • echo TEXT - Print text
            • cat FILE  - Show file content
            • mkdir DIR - Create directory
            • cd DIR    - Change directory
            
            All standard Linux commands available in proot environment.
            
        """.trimIndent()
        hasil.append("$helpText\n")
        scrollToBottom()
    }
    
    private fun executeCommand(command: String) {
        isProcessing = true
        
        hasil.append("$ $command\n")
        
        hasil.append("⏳ Executing...\n")
        scrollToBottom()
        
        scope.launch(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val output = Proses(command)
                val duration = System.currentTimeMillis() - startTime
                
                withContext(Dispatchers.Main) {
                    val currentText = hasil.text.toString()
                    val cleanText = currentText.replace("⏳ Executing...\n", "")
                    hasil.text = cleanText
                    
                    if (output.isNotEmpty() && output != "[No output]") {
                        hasil.append("$output\n")
                    } else {
                        hasil.append("✅ Command completed\n")
                    }
                    
                    if (duration > 2000) {
                        hasil.append("⏱️  Completed in ${duration / 1000.0}s\n")
                    }
                    
                    hasil.append("\n")
                    scrollToBottom()
                    isProcessing = false
                    
                    tulis.requestFocus()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val currentText = hasil.text.toString()
                    val cleanText = currentText.replace("⏳ Executing...\n", "")
                    hasil.text = cleanText
                    
                    hasil.append("❌ Error: ${e.message}\n\n")
                    scrollToBottom()
                    isProcessing = false
                    tulis.requestFocus()
                }
            }
        }
    }
    
    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (isProcessing) {
            InterruptCommand()
        }
    }
    external fun Proses(baca: String): String
    external fun InterruptCommand(): Unit 
    external fun IsEnvironmentReady(): Boolean
    
    private fun PakaiSistem() {
    val baca = tulis.text.toString()

    val proses = ProcessBuilder("/system/bin/sh", "-c", baca)
        .directory(filesDir)
        .redirectErrorStream(true)
        .start()

    val out = proses.inputStream.bufferedReader().readText()
    proses.waitFor()

    hasil.append("\n$baca\n$out")
    tulis.setText("")
    }
}