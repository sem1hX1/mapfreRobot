package com.mapfre.autoanswer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Ekrana dokunma işlerini yapar. Mapfre araması aktif değilken hiçbir şeye dokunmaz.
 */
class AutoAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: AutoAccessibilityService? = null
            private set

        private val ANSWER_LABELS = listOf("cevapla", "yanıtla", "yanitla", "answer", "kabul et", "accept")
        private val KEYPAD_LABELS = listOf("tuş takımı", "tus takimi", "tuş takımını", "klavye", "tuşlar",
            "keypad", "dialpad", "dial pad", "numara tuşları")
        private val HASH_DESCS = listOf("#", "pound", "hash", "kare", "diyez")
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tr = Locale("tr")

    override fun onServiceConnected() {
        instance = this
        Prefs(this).log("Erişilebilirlik servisi açık")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* olayları kullanmıyoruz */ }
    override fun onInterrupt() {}

    // ---------- Aramayı açma ----------

    fun tapAnswerButton() {
        if (!CallAutomator.isActive()) return
        val node = findNode { n ->
            val t = label(n)
            val id = n.viewIdResourceName.orEmpty().lowercase()
            ANSWER_LABELS.any { t == it || t.startsWith(it) } || id.endsWith("answer") || id.contains("answer_button")
        }
        if (node != null && click(node)) Prefs(this).log("'Cevapla' düğmesine basıldı")
    }

    fun swipeToAnswer() {
        if (!CallAutomator.isActive()) return
        val p = Prefs(this)
        val dm = resources.displayMetrics
        val path = Path().apply {
            moveTo(dm.widthPixels * p.swipeStartX / 100f, dm.heightPixels * p.swipeStartY / 100f)
            lineTo(dm.widthPixels * p.swipeEndX / 100f, dm.heightPixels * p.swipeEndY / 100f)
        }
        gesture(path, 450)
    }

    /** Ayarlardaki "Kaydırmayı test et" düğmesi için (arama olmadan). */
    fun testSwipe() {
        val p = Prefs(this)
        val dm = resources.displayMetrics
        val path = Path().apply {
            moveTo(dm.widthPixels * p.swipeStartX / 100f, dm.heightPixels * p.swipeStartY / 100f)
            lineTo(dm.widthPixels * p.swipeEndX / 100f, dm.heightPixels * p.swipeEndY / 100f)
        }
        gesture(path, 450)
    }

    // ---------- # tuşu ----------

    fun pressHash(attempt: Int) {
        if (!CallAutomator.isActive()) return
        val prefs = Prefs(this)

        val hash = findHashKey()
        if (hash != null) {
            if (click(hash)) prefs.log("# tuşuna basıldı ✔") else prefs.log("# bulundu ama basılamadı")
            return
        }

        // Tuş takımı kapalı: önce aç, sonra # ara
        val keypad = findNode { n ->
            val t = label(n)
            val id = n.viewIdResourceName.orEmpty().lowercase()
            KEYPAD_LABELS.any { t == it || t.contains(it) } || id.contains("dialpad") || id.contains("keypad")
        }
        if (keypad != null) {
            click(keypad)
            prefs.log("Tuş takımı açıldı")
            handler.postDelayed({
                val h = findHashKey()
                if (h != null && click(h)) prefs.log("# tuşuna basıldı ✔")
                else retry(attempt)
            }, 900)
        } else {
            retry(attempt)
        }
    }

    private fun retry(attempt: Int) {
        if (attempt < 3) {
            handler.postDelayed({ pressHash(attempt + 1) }, 1500)
        } else {
            Prefs(this).log("HATA: # tuşu bulunamadı (arama ekranı tanınmadı)")
        }
    }

    private fun findHashKey(): AccessibilityNodeInfo? = findNode { n ->
        val text = n.text?.toString()?.trim().orEmpty()
        val desc = n.contentDescription?.toString()?.trim()?.lowercase(tr).orEmpty()
        val id = n.viewIdResourceName.orEmpty().lowercase()
        text == "#" || desc in HASH_DESCS || desc.startsWith("#") || id.endsWith("/pound") || id.contains("pound")
    }

    // ---------- Yardımcılar ----------

    private fun label(n: AccessibilityNodeInfo): String =
        (n.text?.toString() ?: n.contentDescription?.toString() ?: "").trim().lowercase(tr)

    /** Tüm pencerelerde (arama ekranı dahil) düğüm arar; kendi uygulamamızı atlar. */
    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val roots = mutableListOf<AccessibilityNodeInfo>()
        try { windows?.forEach { w -> w.root?.let { roots.add(it) } } } catch (_: Exception) {}
        rootInActiveWindow?.let { roots.add(it) }

        for (root in roots) {
            if (root.packageName == packageName) continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val n = queue.removeFirst()
                if (n.isVisibleToUser && predicate(n)) return n
                for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun click(node: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = node
        while (n != null) {
            if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            n = n.parent
        }
        // Tıklanamıyorsa ekrandaki konumuna dokun
        val r = Rect()
        node.getBoundsInScreen(r)
        if (r.isEmpty) return false
        val path = Path().apply { moveTo(r.exactCenterX(), r.exactCenterY()) }
        return gesture(path, 80)
    }

    private fun gesture(path: Path, durationMs: Long): Boolean {
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(g, null, null)
    }
}
