package com.mapfre.autoanswer

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Basit ayar deposu. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("ayarlar", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** Mapfre robotunun numarası. Karşılaştırma son 10 haneye göre yapılır. */
    var targetNumber: String
        get() = sp.getString("target", "") ?: ""
        set(v) = sp.edit().putString("target", v).apply()

    /** Arama açıldıktan sonra # basmadan önce beklenecek süre (sn). */
    var hashDelaySec: Int
        get() = sp.getInt("hashDelay", 4)
        set(v) = sp.edit().putInt("hashDelay", v).apply()

    /** Kaydırma yedeği: ekran oranı olarak (yüzde) başlangıç ve bitiş noktaları. */
    var swipeStartX: Int
        get() = sp.getInt("sx", 25)
        set(v) = sp.edit().putInt("sx", v).apply()
    var swipeStartY: Int
        get() = sp.getInt("sy", 84)
        set(v) = sp.edit().putInt("sy", v).apply()
    var swipeEndX: Int
        get() = sp.getInt("ex", 25)
        set(v) = sp.edit().putInt("ex", v).apply()
    var swipeEndY: Int
        get() = sp.getInt("ey", 55)
        set(v) = sp.edit().putInt("ey", v).apply()

    fun log(msg: String) {
        val time = SimpleDateFormat("dd.MM HH:mm:ss", Locale("tr")).format(Date())
        val lines = (listOf("$time  $msg") + logText.lines()).filter { it.isNotBlank() }.take(40)
        sp.edit().putString("log", lines.joinToString("\n")).apply()
    }

    val logText: String get() = sp.getString("log", "") ?: ""

    fun clearLog() = sp.edit().remove("log").apply()

    fun matches(incoming: String?): Boolean {
        val a = incoming?.filter { it.isDigit() }?.takeLast(10).orEmpty()
        val b = targetNumber.filter { it.isDigit() }.takeLast(10)
        return a.isNotEmpty() && b.isNotEmpty() && a == b
    }
}
