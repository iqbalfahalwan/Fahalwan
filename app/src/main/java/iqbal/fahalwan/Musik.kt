package iqbal.fahalwan

import androidx.appcompat.app.*
import android.os.*
import android.widget.*
import java.io.*
import android.content.*
import com.google.android.gms.ads.*
import com.google.android.gms.*
import com.google.android.gms.ads.rewarded.*
import android.view.*
import android.annotation.SuppressLint
import android.database.Cursor
import android.provider.MediaStore
import android.media.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class Musik: AppCompatActivity(){

    private lateinit var liner:LinearLayout
    private lateinit var nav:ImageView
    private var mediaPlayer: MediaPlayer? = null
    private var lagiJalan = false
    private lateinit var sebelum:ImageView
    private lateinit var sesudah:ImageView
    private var musikList = mutableListOf<Pair<String, String>>()
    private var currentIndex = 0
    private var currentFile: File? = null
    private var isFromNotification = false

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.musik)

        liner = findViewById(R.id.liner)
        nav = findViewById(R.id.nav)
        sebelum = findViewById(R.id.sebelum)
        sesudah = findViewById(R.id.sesudah)

        // Handle notification actions
        handleNotificationAction()
        Iklan()

        val terima = intent.getStringExtra("file")
        if(terima == null){
            AturNormal()
            nav.setImageResource(R.drawable.nav)
        }
        else{
            TerimaFile(File(terima))
            nav.setImageResource(R.drawable.x)
            sesudah.visibility = View.GONE
            sebelum.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationAction()
    }

    private fun handleNotificationAction() {
        when (intent?.action) {
            "ACTION_STOP" -> {
                stopMusic()
            }
            "ACTION_PLAY_PAUSE" -> {
                togglePlayPause()
            }
            "ACTION_NEXT" -> {
                playNextSong()
            }
            "ACTION_PREV" -> {
                playPrevSong()
            }
        }
    }

    private fun Iklan(){
        val IN = findViewById<AdView>(R.id.iklan_nav)
        val IU = findViewById<AdView>(R.id.iklan_utama)
        val iklan = AdRequest.Builder().build()
        IN.loadAd(iklan)
        IU.loadAd(iklan)
    }

    override fun onBackPressed(){
        if(liner.visibility == View.VISIBLE){liner.visibility = View.GONE}
        else{Keluar()}
    }

    private fun Keluar(){
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Dari Musik?")
            .setPositiveButton("Ya"){_,_-> 
                stopMusic()
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun TerimaFile(terima:File){
        nav.setOnClickListener{ Keluar() }
        currentFile = terima
        Jalankan(terima)
    }

    private fun AturNormal(){
        Tombol()
        AturGrid()
        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        sebelum.setOnClickListener { 
            playPrevSong()
        }
        
        sesudah.setOnClickListener { 
            playNextSong()
        }
    }

    private fun playNextSong() {
        if (musikList.isNotEmpty() && currentIndex < musikList.size - 1) {
            currentIndex++
            val nextSong = musikList[currentIndex]
            currentFile = File(nextSong.second)
            Jalankan(currentFile!!)
        }
    }

    private fun playPrevSong() {
        if (musikList.isNotEmpty() && currentIndex > 0) {
            currentIndex--
            val prevSong = musikList[currentIndex]
            currentFile = File(prevSong.second)
            Jalankan(currentFile!!)
        }
    }

    private fun Tombol(){
        nav.setOnClickListener{
            liner.visibility = if(liner.visibility == View.GONE)View.VISIBLE else View.GONE
        }

        findViewById<TextView>(R.id.keluar).setOnClickListener{
            Keluar()
        }
    }

    @SuppressLint("Range")
    private fun getAllMusic(context: Context): List<Pair<String, String>> {
        val musicList = mutableListOf<Pair<String, String>>()

        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE
        )

        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            null,
            null
        )

        cursor?.use {
            val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)

            while (it.moveToNext()) {
                val path = it.getString(dataIndex)
                val title = it.getString(titleIndex)
                musicList.add(Pair(title, path))
            }
        }

        return musicList
    }

    private fun AturGrid() {
        val grid = findViewById<GridLayout>(R.id.grid)
        grid.removeAllViews()

        musikList = getAllMusic(this).toMutableList()

        for ((index, musicPair) in musikList.withIndex()) {
            val (title, path) = musicPair
            val item = LayoutInflater.from(this).inflate(R.layout.item_horizontal, grid, false)
            val nama = item.findViewById<TextView>(R.id.nama)
            val gambar = item.findViewById<ImageView>(R.id.gambar)

            nama.text = title
            gambar.setImageResource(R.drawable.ic_musik)

            item.setOnClickListener {
                currentIndex = index
                currentFile = File(path)
                Jalankan(currentFile!!)
                liner.visibility = View.GONE
            }

            grid.addView(item)
        }
    }

    private fun Jalankan(target: File) {
        val utama = findViewById<ImageView>(R.id.gambar_utama)
        val waktu = findViewById<TextView>(R.id.waktu)
        val sek = findViewById<SeekBar>(R.id.sek_proses)
        val ps = findViewById<ImageView>(R.id.mulai_stop)
        val nama_musik = findViewById<TextView>(R.id.nama_file)

        nama_musik.text = target.nameWithoutExtension
        utama.setImageResource(R.drawable.ic_musik)

        ps.setImageResource(
            if (Berjalan()) R.drawable.stop else R.drawable.play
        )

        ps.setOnClickListener {
            if (!target.exists()) {
                Toast.makeText(this, "Silahkan Pilih Lagu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            togglePlayPause()
        }

        // Setup seekbar interaction
        sek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Auto start if not playing
        if (!Berjalan()) {
            startMusic(target)
        }
    }

    private fun togglePlayPause() {
        val ps = findViewById<ImageView>(R.id.mulai_stop)
        
        if (Berjalan()) {
            pauseMusic()
            ps.setImageResource(R.drawable.play)
        } else if (mediaPlayer != null) {
            resumeMusic()
            ps.setImageResource(R.drawable.stop)
        } else if (currentFile != null) {
            startMusic(currentFile!!)
            ps.setImageResource(R.drawable.stop)
        }
    }

    private fun startMusic(target: File) {
        try {
            stopMusic() // Stop any existing player
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(target.path)
                prepare()
                start()
                
                setOnCompletionListener {
                    // Auto play next song
                    playNextSong()
                }
            }
            
            lagiJalan = true
            updateUI()
            startProgressUpdater()
            tampilkanNotifikasi(target.nameWithoutExtension)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing music: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        lagiJalan = false
        updateNotification()
    }

    private fun resumeMusic() {
        mediaPlayer?.start()
        lagiJalan = true
        updateNotification()
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        lagiJalan = false
        
        // Clear notification
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1)
        
        updateUI()
    }

    private fun updateUI() {
        val ps = findViewById<ImageView>(R.id.mulai_stop)
        ps.setImageResource(if (Berjalan()) R.drawable.stop else R.drawable.play)
    }

    private fun startProgressUpdater() {
        val sek = findViewById<SeekBar>(R.id.sek_proses)
        val waktu = findViewById<TextView>(R.id.waktu)
        
        sek.max = mediaPlayer?.duration ?: 0
        
        Thread {
            while (Berjalan()) {
                runOnUiThread {
                    mediaPlayer?.let { mp ->
                        sek.progress = mp.currentPosition
                        val detik = mp.currentPosition / 1000
                        val menit = detik / 60
                        val sisaDetik = detik % 60
                        waktu.text = "${menit}:${String.format("%02d", sisaDetik)}"
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun Berjalan(): Boolean {
        return lagiJalan && mediaPlayer != null && mediaPlayer!!.isPlaying
    }

    @SuppressLint("RemoteViewLayout")
    private fun tampilkanNotifikasi(judul: String) {
        val channelId = "musik_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Musik Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val remoteView = RemoteViews(packageName, R.layout.musik_notif)
        remoteView.setTextViewText(R.id.nama, judul)
        
        // Set proper play/pause icon
        val playPauseIcon = if (Berjalan()) R.drawable.stop else R.drawable.play
        remoteView.setImageViewResource(R.id.stop_play, playPauseIcon)

        // Create pending intents for notification buttons
        val intentPlayPause = Intent(this, Musik::class.java).apply {
            action = "ACTION_PLAY_PAUSE"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingPlayPause = PendingIntent.getActivity(
            this, 1, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteView.setOnClickPendingIntent(R.id.stop_play, pendingPlayPause)

        val intentNext = Intent(this, Musik::class.java).apply {
            action = "ACTION_NEXT"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingNext = PendingIntent.getActivity(
            this, 2, intentNext, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteView.setOnClickPendingIntent(R.id.next, pendingNext)

        val intentPrev = Intent(this, Musik::class.java).apply {
            action = "ACTION_PREV"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingPrev = PendingIntent.getActivity(
            this, 3, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteView.setOnClickPendingIntent(R.id.prev, pendingPrev)

        // Intent to open app when notification is clicked
        val intentMain = Intent(this, Musik::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingMain = PendingIntent.getActivity(
            this, 0, intentMain, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_musik)
            .setContentIntent(pendingMain)
            .setContent(remoteView)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    private fun updateNotification() {
        currentFile?.let { file ->
            tampilkanNotifikasi(file.nameWithoutExtension)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}