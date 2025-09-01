
package iqbal.fahalwan

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.os.*
import android.widget.*
import android.view.*
import android.content.*
import android.net.*
import android.Manifest
import java.net.*
import java.util.*
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.provider.Settings
import java.io.File
import android.app.ActivityManager
import java.util.concurrent.TimeUnit
import android.telephony.*
import com.google.android.gms.ads.*
import com.google.android.gms.*
import com.google.android.gms.ads.rewarded.*

public class MainActivity : AppCompatActivity() {

    private lateinit var liner:LinearLayout
    private lateinit var status:ImageView
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        liner = findViewById(R.id.liner)
        status = findViewById(R.id.status)
        
        Tombol()
        IkonStatus()
        Fitur()
        Iklan()
        MobileAds.initialize(this) {}
    }
    
    private fun Iklan(){
      val IN = findViewById<AdView>(R.id.iklan_nav)
      val IU = findViewById<AdView>(R.id.iklan_utama)
      val iklan = AdRequest.Builder().build()
      IN.loadAd(iklan)
      IU.loadAd(iklan)
    }

  private fun IklanVideo() {
    if (rewardedAd == null && !isLoading) {
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this,
            "ca-aplikasi-pub-2534537144295464/7079484365",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }
            }
        )
    } else if (rewardedAd != null) {
        rewardedAd?.show(this) { 
            Toast.makeText(this, "Terima kasih sudah donasi 🙏", Toast.LENGTH_SHORT).show()
            rewardedAd = null
        }
    } else {
        Toast.makeText(this, "Iklan belum siap, coba lagi nanti", Toast.LENGTH_SHORT).show()
    }
  }
    
    private fun Keluar(){
      AlertDialog.Builder(this)
        .setTitle("Keluar")
        .setMessage("Dari Aplikasi ?")
        .setPositiveButton("Ya"){_,_-> finish()}
        .setNegativeButton("Tidak", null)
        .show()
    }
    
    private fun IkonStatus(){
      status.setImageResource(
        if(BacaIzin()){R.drawable.sd_terima}
        else{R.drawable.sd_tolak}
      )
    }
    
    override fun onResume(){
      super.onResume()
        IkonStatus()
    }
    
    override fun onBackPressed(){
      if(liner.visibility == View.VISIBLE){liner.visibility = View.GONE}
      else{ Keluar() }
    }

    private fun ModelHp(): String {
      return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    private fun TahunHp(): String {
      val waktu = Build.TIME
      val tahun = java.util.Calendar.getInstance().apply { timeInMillis = waktu }.get(java.util.Calendar.YEAR)
      return "$tahun"
    }
    
    private fun VersiHp(): String {
      return "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }
    
    private fun ModelCpu(): String {
    try {
        val cpuInfoFile = "/proc/cpuinfo"
        val bufferedReader = java.io.BufferedReader(java.io.FileReader(cpuInfoFile))
        var line: String?
        var modelName = "Unknown CPU"
        
        while (bufferedReader.readLine().also { line = it } != null) {
            line = line?.lowercase()
            if (line!!.contains("hardware") || line!!.contains("model name")) {
                modelName = line!!.split(":")[1].trim()
                break
            }
        }
        bufferedReader.close()
        return modelName
    } catch (e: Exception) {
        e.printStackTrace()
        return "Unknown CPU"
    }
  }
  
  private fun TipeCpu(): String {
    return if (Build.SUPPORTED_ABIS.isNotEmpty()) {
        Build.SUPPORTED_ABIS[0]
    } else {
        "Unknown"
    }
  }
  
  private fun KecepatanCpu(): String {
    return try {
        val path = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"
        val reader = java.io.FileReader(path)
        val buffered = java.io.BufferedReader(reader)
        val maxFreqKHz = buffered.readLine()?.toLongOrNull() ?: 0L
        buffered.close()
        reader.close()

        if (maxFreqKHz > 0) {
            val maxFreqGHz = maxFreqKHz.toDouble() / 1_000_000
            String.format("%.2f GHz", maxFreqGHz)
        } else {
            "Unknown"
        }
    } catch (e: Exception) {
        "Unknown"
    }
  }

  private fun BacaIzin(): Boolean {
    return if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
  }
  
  private fun MintaIzin() {
    val sdk = Build.VERSION.SDK_INT
    if (sdk >= 30) { IzinPengaturan() }
    else { IzinPopup() }
  }
    
    private fun IzinPengaturan() {
      val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
      intent.data = Uri.parse("package:$packageName")
      startActivity(intent)
    }
    
    private fun IzinPopup() {
      requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
    }

  private fun MemoIn(): String {
    try {
        val path: File = Environment.getDataDirectory() // Internal storage root
        val stat = StatFs(path.path)

        val blockSize: Long
        val totalBlocks: Long
        val availableBlocks: Long

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.blockSizeLong
            totalBlocks = stat.blockCountLong
            availableBlocks = stat.availableBlocksLong
        } else {
            blockSize = stat.blockSize.toLong()
            totalBlocks = stat.blockCount.toLong()
            availableBlocks = stat.availableBlocks.toLong()
        }

        val totalMB = totalBlocks * blockSize / (1024 * 1024)
        val freeMB = availableBlocks * blockSize / (1024 * 1024)

        return "Free: ${freeMB}MB / Total: ${totalMB}MB"
    } catch (e: Exception) {
        e.printStackTrace()
        return "Unknown"
    }
  }

  private fun MemoEx(): String {
    try {
        val path: File? = Environment.getExternalStorageDirectory() // root storage eksternal
        if (path == null || !path.exists()) return "Tidak ada storage eksternal"

        val stat = StatFs(path.path)

        val blockSize: Long
        val totalBlocks: Long
        val availableBlocks: Long

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.blockSizeLong
            totalBlocks = stat.blockCountLong
            availableBlocks = stat.availableBlocksLong
        } else {
            blockSize = stat.blockSize.toLong()
            totalBlocks = stat.blockCount.toLong()
            availableBlocks = stat.availableBlocks.toLong()
        }

        val totalMB = totalBlocks * blockSize / (1024 * 1024)
        val freeMB = availableBlocks * blockSize / (1024 * 1024)

        return "Free: ${freeMB}MB / Total: ${totalMB}MB"
    } catch (e: Exception) {
        e.printStackTrace()
        return "Unknown"
    }
  }

  private fun InfoRam(): String {
    try {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalMB = memInfo.totalMem / (1024 * 1024)
        val availMB = memInfo.availMem / (1024 * 1024)
        val usedMB = totalMB - availMB

        return "Total: ${totalMB}MB, Used: ${usedMB}MB, Free: ${availMB}MB"
    } catch (e: Exception) {
        e.printStackTrace()
        return "Unknown"
    }
  }
  
  private fun TipeBat(): String {
    return try {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = registerReceiver(null, ifilter)
        batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
  }

  private fun KapasitasBat(): String {
    return try {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val chargeMicroAh = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        if (chargeMicroAh > 0) {
            val chargeMah = chargeMicroAh / 1000
            "${chargeMah} mAh"
        } else {
            "Unknown"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
  }

  private fun AktifSejak(): String {
    return try {
        val millis = SystemClock.elapsedRealtime()

        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
  }

  private fun SimSatu(): String {
    return try {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val operatorName = tm.simOperatorName
        if (operatorName.isNullOrEmpty()) "Tidak ada SIM" else operatorName
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
  }

  private fun SimDua(): String {
    return try {
        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: return "Tidak ada SIM 2"
        
        if (subscriptionList.size >= 2) {
            val sim2 = subscriptionList[1]
            sim2.carrierName?.toString() ?: "Unknown"
        } else {
            "Tidak ada SIM 2"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
  }

  private fun JaringanSekarang(): String {
    return try {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Tidak ada koneksi"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Tidak ada koneksi"

        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val telephony = getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                when (telephony.networkType) {
                    android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                    android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "5G"
                    android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                    android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA,
                    android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
                    android.telephony.TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                    android.telephony.TelephonyManager.NETWORK_TYPE_GPRS,
                    android.telephony.TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                    else -> "Seluler"
                }
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth (Internet)"
            else -> "Lainnya"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
  }

  private fun AlamatIp(): String {
    return try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        val ipList = mutableListOf<String>()

        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is InetAddress) {
                    val ip = addr.hostAddress
                    if (!ip.contains(":")) {
                        ipList.add(ip)
                    }
                }
            }
        }

        if (ipList.isEmpty()) "Tidak ada IP" else ipList.joinToString(", ")
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
  }
  
  private fun BacaRoot(): String {
    return try {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )

        var isRooted = false
        for (path in paths) {
            if (java.io.File(path).exists()) {
                isRooted = true
                break
            }
        }

        if (isRooted) "IYA" else "TIDAK"
    } catch (e: Exception) {
        e.printStackTrace()
        "Tidak"
    }
  }
  
  private fun Tombol(){
       findViewById<TextView>(R.id.keluar).setOnClickListener{
        Keluar()
      }
      
      findViewById<LinearLayout>(R.id.donasi).setOnClickListener{
        IklanVideo()
      }
      
      findViewById<ImageView>(R.id.nav).setOnClickListener{
        liner.visibility = if(liner.visibility == View.GONE)View.VISIBLE else View.GONE
      }
      
      status.setOnClickListener{
        MintaIzin()
      }
      
      findViewById<LinearLayout>(R.id.berkas).setOnClickListener{
        startActivity(Intent(this, Berkas::class.java))
      }
      
      findViewById<LinearLayout>(R.id.terminal).setOnClickListener{
        startActivity(Intent(this, Terminal::class.java))
      }
      
      findViewById<LinearLayout>(R.id.ytmp3).setOnClickListener{
        startActivity(Intent(this, Konverter::class.java))
      }
      
      findViewById<LinearLayout>(R.id.musik).setOnClickListener{
        startActivity(Intent(this, Musik::class.java))
      }
    }
  
  private fun Fitur(){
      val mhp = findViewById<TextView>(R.id.m_hp)
      val thp = findViewById<TextView>(R.id.t_hp)
      val vhp = findViewById<TextView>(R.id.v_hp)
      
      val mcpu = findViewById<TextView>(R.id.m_cpu)
      val tcpu = findViewById<TextView>(R.id.t_cpu)
      val kcpu = findViewById<TextView>(R.id.k_cpu)
      
      val iin = findViewById<TextView>(R.id.i_in)
      val isd = findViewById<TextView>(R.id.i_sd)
      val iram = findViewById<TextView>(R.id.i_ram)
      
      val tbat = findViewById<TextView>(R.id.t_bat)
      val kbat = findViewById<TextView>(R.id.k_bat)
      val dbat = findViewById<TextView>(R.id.d_bat)
      
      val jsatu = findViewById<TextView>(R.id.j_satu)
      val jdua = findViewById<TextView>(R.id.j_dua)
      val js = findViewById<TextView>(R.id.j_s)
      val ji = findViewById<TextView>(R.id.j_i)
      
      val root = findViewById<TextView>(R.id.root)
      
      root.text = "ROOT : ${BacaRoot()}"
      
      mhp.text = "MODEL HP : ${ModelHp()}"
      thp.text = "TAHUN PEMBUATAN : ${TahunHp()}"
      vhp.text = "VERSI ANDROID : ${VersiHp()}"
      
      mcpu.text = "MODEL CPU : ${ModelCpu()}"
      tcpu.text = "TIPE CPU : ${TipeCpu()}"
      kcpu.text = "KECEPATAN CPU : ${KecepatanCpu()}"
      
      iin.text = "INTERNAL : \n${MemoIn()}"
      isd.text = "EXTERNAL : \n${MemoEx()}"
      iram.text = "RAM : \n${InfoRam()}"
      
      tbat.text = "TIPE : ${TipeBat()}"
      kbat.text = "KAPASITAS : ${KapasitasBat()}"
      dbat.text = "AKTIF SEJAK : ${AktifSejak()}"
      
      jsatu.text = "SIM 1 : ${SimSatu()}"
      jdua.text = "SIM 2 : ${SimDua()}"
      js.text = "JARINGAN : ${JaringanSekarang()}"
      ji.text = "ALAMAT IP : ${AlamatIp()}"
    }
  
}