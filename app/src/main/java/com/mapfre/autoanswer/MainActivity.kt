package com.mapfre.autoanswer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

/** Ayar ekranı. Kod ile kuruldu, ek kütüphane gerekmez. */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    private val perms = arrayOf(
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildUi(): View {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun title(t: String) = col.addView(TextView(this).apply {
            text = t; textSize = 17f; setTypeface(null, Typeface.BOLD); setPadding(0, pad, 0, pad / 3)
        })
        fun button(t: String, onClick: () -> Unit) = col.addView(Button(this).apply {
            text = t; isAllCaps = false; setOnClickListener { onClick() }
        })
        fun numberField(hint: String, value: String, onChange: (String) -> Unit): EditText {
            val e = EditText(this).apply {
                this.hint = hint; setText(value)
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            col.addView(TextView(this).apply { text = hint })
            col.addView(e)
            e.setOnFocusChangeListener { _, has -> if (!has) onChange(e.text.toString()) }
            return e
        }

        // --- Durum ---
        title("Durum")
        statusText = TextView(this).apply { textSize = 15f }
        col.addView(statusText)

        col.addView(Switch(this).apply {
            text = "Otomatik cevaplama açık"
            isChecked = prefs.enabled
            setOnCheckedChangeListener { _, c -> prefs.enabled = c; refresh() }
        })

        // --- İzinler ---
        title("1) İzinler")
        button("Telefon izinlerini ver") { requestPermissions(perms, 1) }
        button("Erişilebilirlik servisini aç") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Listeden 'Mapfre Oto Cevap'ı bulup açın", Toast.LENGTH_LONG).show()
        }
        button("Pil kısıtlamasını kaldır") {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")))
        }

        // --- Numara ---
        title("2) Mapfre robot numarası")
        val numberEdit = EditText(this).apply {
            hint = "Örn: 02121234567"
            setText(prefs.targetNumber)
            inputType = InputType.TYPE_CLASS_PHONE
        }
        col.addView(numberEdit)
        val delayEdit = numberField("# basmadan önce bekleme (saniye)", prefs.hashDelaySec.toString()) {
            prefs.hashDelaySec = it.toIntOrNull()?.coerceIn(0, 30) ?: 4
        }

        // --- Kaydırma ---
        title("3) Kaydırma yedeği (Samsung A51 için ayarlı, genelde gerekmez)")
        col.addView(TextView(this).apply {
            text = "Sistem aramayı açamazsa kullanılır. Değerler ekranın yüzdesi: X soldan sağa, Y yukarıdan aşağıya."
        })
        val sx = numberField("Başlangıç X %", prefs.swipeStartX.toString()) { prefs.swipeStartX = pct(it, 25) }
        val sy = numberField("Başlangıç Y %", prefs.swipeStartY.toString()) { prefs.swipeStartY = pct(it, 84) }
        val ex = numberField("Bitiş X %", prefs.swipeEndX.toString()) { prefs.swipeEndX = pct(it, 25) }
        val ey = numberField("Bitiş Y %", prefs.swipeEndY.toString()) { prefs.swipeEndY = pct(it, 55) }

        fun save() {
            prefs.targetNumber = numberEdit.text.toString().trim()
            prefs.hashDelaySec = delayEdit.text.toString().toIntOrNull()?.coerceIn(0, 30) ?: 4
            prefs.swipeStartX = pct(sx.text.toString(), 25)
            prefs.swipeStartY = pct(sy.text.toString(), 84)
            prefs.swipeEndX = pct(ex.text.toString(), 25)
            prefs.swipeEndY = pct(ey.text.toString(), 55)
        }

        button("💾 Kaydet") {
            save(); refresh()
            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show()
        }
        button("Kaydırmayı test et (3 sn sonra)") {
            save()
            val svc = AutoAccessibilityService.instance
            if (svc == null) {
                Toast.makeText(this, "Önce erişilebilirlik servisini açın", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "3 sn sonra ekranda kaydırma yapılacak", Toast.LENGTH_SHORT).show()
                col.postDelayed({ svc.testSwipe() }, 3000)
            }
        }

        // --- Log ---
        title("Son işlemler")
        button("Logu temizle") { prefs.clearLog(); refresh() }
        logText = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 12f }
        col.addView(logText)

        return ScrollView(this).apply { addView(col, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }
    }

    private fun pct(s: String, def: Int) = s.toIntOrNull()?.coerceIn(0, 100) ?: def

    private fun refresh() {
        val permsOk = perms.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        val a11yOk = AutoAccessibilityService.instance != null
        val batteryOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        val numOk = prefs.targetNumber.filter { it.isDigit() }.length >= 7

        fun mark(ok: Boolean) = if (ok) "✅" else "❌"
        statusText.text = buildString {
            appendLine("${mark(permsOk)} Telefon izinleri")
            appendLine("${mark(a11yOk)} Erişilebilirlik servisi")
            appendLine("${mark(batteryOk)} Pil kısıtlaması kaldırıldı")
            appendLine("${mark(numOk)} Robot numarası girildi")
            append(if (prefs.enabled && permsOk && a11yOk && numOk) "\nHAZIR — sadece bu numara otomatik açılır."
                   else "\nEksikleri tamamlayın.")
        }
        logText.text = prefs.logText.ifBlank { "(henüz kayıt yok)" }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }
}
